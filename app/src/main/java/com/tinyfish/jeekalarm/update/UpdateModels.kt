package com.tinyfish.jeekalarm.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
internal data class GitHubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    val size: Long = 0L,
    val digest: String? = null,
)

data class ApkAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val sha256: String? = null,
)

data class ReleaseInfo(
    val versionName: String,
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkAsset: ApkAsset?,
    val publishedAt: String,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val checkedManually: Boolean) : UpdateState
    data class Available(val release: ReleaseInfo, val fromAutoCheck: Boolean) : UpdateState
    data class Downloading(
        val release: ReleaseInfo,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : UpdateState {
        val progress: Float?
            get() = if (totalBytes > 0L) {
                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }
    }

    data class ReadyToInstall(val release: ReleaseInfo, val apkFile: File) : UpdateState
    data class Error(val message: String, val release: ReleaseInfo? = null) : UpdateState
}

/**
 * 语义化/点分版本号解析与比较：
 * 支持 "v1.2.3"、"1.0"、"1.2.0-beta.1"、"1.2.3+10" 等格式。
 */
data class AppVersion(
    val numbers: List<Int>,
    val preRelease: String? = null,
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val maxLen = maxOf(numbers.size, other.numbers.size)
        for (i in 0 until maxLen) {
            val a = numbers.getOrElse(i) { 0 }
            val b = other.numbers.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }

        // 正式版 > 同版本号的预发布版
        val aPre = preRelease
        val bPre = other.preRelease
        if (aPre == null && bPre == null) return 0
        if (aPre == null) return 1
        if (bPre == null) return -1

        return comparePreRelease(aPre, bPre)
    }

    companion object {
        private val PreReleaseDelimiter = Regex("[.-]")

        fun normalizeVersionName(raw: String): String {
            val trimmed = raw.trim()
            return if (trimmed.length > 1 && trimmed[0] in charArrayOf('v', 'V', 'r', 'R') && trimmed[1].isDigit()) {
                trimmed.substring(1).trim()
            } else {
                trimmed
            }
        }

        fun parse(raw: String): AppVersion? {
            val cleaned = normalizeVersionName(raw).substringBefore('+').trim()
            if (cleaned.isEmpty()) return null

            val dashIndex = cleaned.indexOf('-')
            val corePart = if (dashIndex >= 0) cleaned.substring(0, dashIndex) else cleaned
            val prePart = if (dashIndex >= 0) cleaned.substring(dashIndex + 1).ifBlank { null } else null

            val segments = corePart.split('.')
            if (segments.isEmpty()) return null

            val numbers = ArrayList<Int>(segments.size)
            for (seg in segments) {
                if (seg.isEmpty() || !seg.all { it.isDigit() }) return null
                val num = seg.toIntOrNull() ?: return null
                numbers.add(num)
            }
            return AppVersion(numbers = numbers, preRelease = prePart)
        }

        fun isNewer(remoteVersion: String, currentVersion: String): Boolean {
            val remote = parse(remoteVersion) ?: return false
            val current = parse(currentVersion) ?: return false
            return remote > current
        }

        private fun comparePreRelease(a: String, b: String): Int {
            val aParts = a.split(PreReleaseDelimiter).filter { it.isNotEmpty() }
            val bParts = b.split(PreReleaseDelimiter).filter { it.isNotEmpty() }
            val minLen = minOf(aParts.size, bParts.size)
            for (i in 0 until minLen) {
                val p1 = aParts[i]
                val p2 = bParts[i]
                val n1 = p1.toIntOrNull()
                val n2 = p2.toIntOrNull()
                val cmp = when {
                    n1 != null && n2 != null -> n1.compareTo(n2)
                    n1 != null -> -1 // 按 SemVer 规范，纯数字标识符优先级低于非数字标识符
                    n2 != null -> 1
                    else -> p1.lowercase(Locale.ROOT).compareTo(p2.lowercase(Locale.ROOT))
                }
                if (cmp != 0) return cmp
            }
            return aParts.size.compareTo(bParts.size)
        }
    }
}

internal object UpdateParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val KnownAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
    private val Sha256HexRegex = Regex("^[0-9a-fA-F]{64}$")

    fun parseGitHubRelease(
        responseBody: String,
        supportedAbis: List<String>,
        fallbackHtmlUrl: String,
    ): ReleaseInfo? {
        val dto = json.decodeFromString<GitHubReleaseDto>(responseBody)
        if (dto.draft || dto.prerelease) return null

        val tag = dto.tagName.trim()
        if (tag.isEmpty()) return null

        val normalizedVersion = AppVersion.normalizeVersionName(tag)
        if (AppVersion.parse(normalizedVersion) == null) return null

        val title = dto.name?.trim().orEmpty().ifEmpty { tag }
        val notes = dto.body?.trim().orEmpty()
        val htmlUrl = dto.htmlUrl.trim().takeIf { isSafeHttpsUrl(it) } ?: fallbackHtmlUrl
        val apkAsset = selectBestApkAsset(dto.assets, supportedAbis)

        return ReleaseInfo(
            versionName = normalizedVersion,
            tagName = tag,
            title = title,
            releaseNotes = notes,
            htmlUrl = htmlUrl,
            apkAsset = apkAsset,
            publishedAt = dto.publishedAt.orEmpty(),
        )
    }

    internal fun selectBestApkAsset(
        assets: List<GitHubAssetDto>,
        supportedAbis: List<String>,
    ): ApkAsset? {
        val apkAssets = assets.filter { asset ->
            asset.name.endsWith(".apk", ignoreCase = true) &&
                isSafeHttpsUrl(asset.browserDownloadUrl)
        }
        if (apkAssets.isEmpty()) return null
        if (apkAssets.size == 1) return apkAssets.first().toApkAsset()

        // 1. 优先匹配设备支持顺序中最靠前的 ABI（如 arm64-v8a）
        for (abi in supportedAbis) {
            val normalizedAbi = abi.lowercase(Locale.ROOT)
            val matched = apkAssets
                .filter { it.name.lowercase(Locale.ROOT).contains(normalizedAbi) }
                .preferRelease()
            if (matched != null) return matched.toApkAsset()
        }

        // 2. 其次选择显式 universal 或不带其他 ABI 标识的通用包
        val universal = apkAssets
            .filter { asset ->
                val lower = asset.name.lowercase(Locale.ROOT)
                lower.contains("universal") || KnownAbis.none { lower.contains(it) }
            }
            .preferRelease()
        if (universal != null) return universal.toApkAsset()

        return apkAssets.preferRelease()?.toApkAsset() ?: apkAssets.first().toApkAsset()
    }

    private fun List<GitHubAssetDto>.preferRelease(): GitHubAssetDto? {
        if (isEmpty()) return null
        return firstOrNull { !it.name.lowercase(Locale.ROOT).contains("debug") } ?: first()
    }

    private fun GitHubAssetDto.toApkAsset(): ApkAsset {
        return ApkAsset(
            name = name,
            downloadUrl = browserDownloadUrl.trim(),
            sizeBytes = size.coerceAtLeast(0L),
            sha256 = parseSha256Digest(digest),
        )
    }

    internal fun parseSha256Digest(digest: String?): String? {
        val raw = digest?.trim() ?: return null
        val hex = when {
            raw.startsWith("sha256:", ignoreCase = true) -> raw.substring(7).trim()
            else -> return null
        }
        return hex.lowercase(Locale.ROOT).takeIf { Sha256HexRegex.matches(it) }
    }

    internal fun isSafeHttpsUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        return runCatching {
            val uri = URI(trimmed)
            uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }

    internal fun shouldAutoCheck(
        autoCheckEnabled: Boolean,
        hasCheckedThisSession: Boolean,
        lastCheckTimeMs: Long,
        nowMs: Long,
        intervalMs: Long,
    ): Boolean {
        if (!autoCheckEnabled || hasCheckedThisSession) return false
        if (lastCheckTimeMs <= 0L) return true
        return nowMs - lastCheckTimeMs >= intervalMs
    }

    internal fun shouldPromptAutoUpdate(
        remoteVersion: String,
        currentVersion: String,
        skippedVersion: String,
    ): Boolean {
        if (!AppVersion.isNewer(remoteVersion, currentVersion)) return false
        if (skippedVersion.isBlank()) return true
        return AppVersion.normalizeVersionName(remoteVersion) !=
            AppVersion.normalizeVersionName(skippedVersion)
    }

    internal fun formatFileSize(bytes: Long): String {
        if (bytes < 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    internal fun toHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format(Locale.US, "%02x", b.toInt() and 0xff))
        }
        return sb.toString()
    }

    internal fun sha256Of(bytes: ByteArray): String {
        return toHex(MessageDigest.getInstance("SHA-256").digest(bytes))
    }
}
