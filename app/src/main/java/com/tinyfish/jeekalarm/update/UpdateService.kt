package com.tinyfish.jeekalarm.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.tinyfish.jeekalarm.BuildConfig
import com.tinyfish.jeekalarm.PermissionsService
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.alarm.NotificationService
import com.tinyfish.jeekalarm.start.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object UpdateService {
    private const val TAG = "UpdateService"
    private const val GitHubOwner = "tifish"
    private const val GitHubRepo = "JeekAlarm"
    private const val LatestReleaseApiUrl = "https://api.github.com/repos/$GitHubOwner/$GitHubRepo/releases/latest"
    const val ReleasesPageUrl = "https://github.com/$GitHubOwner/$GitHubRepo/releases/latest"

    private const val AutoCheckIntervalMs = 24L * 60L * 60L * 1000L
    private const val StaleApkMaxAgeMs = 60L * 60L * 1000L
    private const val MaxRedirects = 5
    private const val ApiConnectTimeoutMs = 10_000
    private const val ApiReadTimeoutMs = 15_000
    private const val DownloadConnectTimeoutMs = 15_000
    private const val DownloadReadTimeoutMs = 30_000
    private const val ProgressThrottleMs = 100L
    private const val ProgressThrottleBytes = 64L * 1024L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var checkJob: Job? = null
    private var downloadJob: Job? = null
    private var checkedOnStartup = false

    val currentVersionName: String
        get() = BuildConfig.VERSION_NAME

    val currentVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    var state by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    var showDialog by mutableStateOf(false)
        private set

    var pendingInstallPermission by mutableStateOf(false)
        private set

    fun formattedCurrentVersion(): String {
        return currentVersionName.trim()
    }

    /** 仅在主界面启动时调用，避免后台响铃/开机广播唤醒进程时发起网络请求。 */
    fun checkOnStartupIfNeeded() {
        if (NotificationService.currentAlarmIds.isNotEmpty()) return
        val now = System.currentTimeMillis()
        if (!UpdateParser.shouldAutoCheck(
                autoCheckEnabled = SettingsService.autoCheckUpdate,
                hasCheckedThisSession = checkedOnStartup,
                lastCheckTimeMs = SettingsService.lastUpdateCheckTimeMs,
                nowMs = now,
                intervalMs = AutoCheckIntervalMs,
            )
        ) {
            return
        }
        checkedOnStartup = true
        checkForUpdate(manual = false)
    }

    fun checkForUpdateManually() {
        checkForUpdate(manual = true)
    }

    private fun checkForUpdate(manual: Boolean) {
        if (state is UpdateState.Checking || state is UpdateState.Downloading) return

        checkJob?.cancel()
        if (manual) {
            state = UpdateState.Checking
        }

        checkJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchLatestRelease()
            }

            when (result) {
                is FetchResult.Success -> {
                    SettingsService.lastUpdateCheckTimeMs = System.currentTimeMillis()
                    val release = result.release
                    if (release != null && AppVersion.isNewer(release.versionName, currentVersionName)) {
                        val shouldPrompt = manual || UpdateParser.shouldPromptAutoUpdate(
                            remoteVersion = release.versionName,
                            currentVersion = currentVersionName,
                            skippedVersion = SettingsService.skippedUpdateVersion,
                        )
                        if (shouldPrompt) {
                            state = UpdateState.Available(release = release, fromAutoCheck = !manual)
                            showDialog = true
                        }
                    } else if (manual) {
                        state = UpdateState.UpToDate(checkedManually = true)
                    }
                }

                is FetchResult.NoRelease -> {
                    SettingsService.lastUpdateCheckTimeMs = System.currentTimeMillis()
                    if (manual) {
                        state = UpdateState.UpToDate(checkedManually = true)
                    }
                }

                is FetchResult.Failure -> {
                    Log.w(TAG, "Check update failed: ${result.message}")
                    if (manual) {
                        state = UpdateState.Error(message = result.message)
                    }
                }
            }
        }
    }

    fun openUpdateDialog() {
        if (state is UpdateState.Available ||
            state is UpdateState.Downloading ||
            state is UpdateState.ReadyToInstall ||
            state is UpdateState.Error
        ) {
            showDialog = true
        }
    }

    fun dismissDialog() {
        showDialog = false
    }

    fun skipVersion(versionName: String) {
        SettingsService.skippedUpdateVersion = AppVersion.normalizeVersionName(versionName)
        showDialog = false
    }

    fun startDownload(context: Context, release: ReleaseInfo) {
        val appContext = context.applicationContext
        val asset = release.apkAsset
        if (asset == null) {
            openReleasePage(appContext, release.htmlUrl)
            showDialog = false
            return
        }

        // 若该版本 APK 已下载且完整有效，直接进入安装流程。
        val existingReady = state as? UpdateState.ReadyToInstall
        if (existingReady != null &&
            existingReady.release.versionName == release.versionName &&
            existingReady.apkFile.exists()
        ) {
            installDownloadedApk(appContext)
            return
        }

        if (state is UpdateState.Downloading) return

        downloadJob?.cancel()
        state = UpdateState.Downloading(
            release = release,
            downloadedBytes = 0L,
            totalBytes = asset.sizeBytes,
        )
        showDialog = true

        downloadJob = scope.launch {
            try {
                val apkFile = withContext(Dispatchers.IO) {
                    downloadAndVerifyApk(release, asset) { downloaded, total ->
                        scope.launch {
                            val current = state as? UpdateState.Downloading
                            if (current != null && current.release.versionName == release.versionName) {
                                state = UpdateState.Downloading(release, downloaded, total)
                            }
                        }
                    }
                }
                state = UpdateState.ReadyToInstall(release = release, apkFile = apkFile)
                showDialog = true
                // 已有安装权限时直接拉起系统安装器；未授权时先保留提示弹窗说明，由用户点击“授权并安装”。
                if (PermissionsService.canRequestPackageInstalls(appContext)) {
                    installDownloadedApk(appContext)
                }
            } catch (_: CancellationException) {
                state = UpdateState.Available(release = release, fromAutoCheck = false)
                showDialog = false
            } catch (e: Exception) {
                Log.e(TAG, "Download update failed", e)
                val localized = App.localizedContext()
                val msg = e.message?.takeIf { it.isNotBlank() }
                    ?: localized.getString(R.string.update_error_download_failed)
                state = UpdateState.Error(message = msg, release = release)
                showDialog = true
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    fun installDownloadedApk(context: Context): Boolean {
        val ready = state as? UpdateState.ReadyToInstall ?: return false
        if (!ready.apkFile.exists()) {
            state = UpdateState.Available(release = ready.release, fromAutoCheck = false)
            return false
        }

        if (!PermissionsService.canRequestPackageInstalls(context)) {
            pendingInstallPermission = true
            PermissionsService.openInstallUnknownAppsSettings(context)
            return false
        }

        pendingInstallPermission = false
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                ready.apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer", e)
            val localized = App.localizedContext()
            state = UpdateState.Error(
                message = e.message?.takeIf { it.isNotBlank() }
                    ?: localized.getString(R.string.update_error_install_failed),
                release = ready.release,
            )
            showDialog = true
            false
        }
    }

    /** 从“安装未知应用”系统权限页返回前台时自动续接安装。 */
    fun onAppResumed(context: Context) {
        if (!pendingInstallPermission) return
        pendingInstallPermission = false

        val ready = state as? UpdateState.ReadyToInstall ?: return
        if (ready.apkFile.exists() && PermissionsService.canRequestPackageInstalls(context)) {
            installDownloadedApk(context)
        }
    }

    fun openReleasePage(context: Context, url: String = ReleasesPageUrl) {
        val safeUrl = url.takeIf { UpdateParser.isSafeHttpsUrl(it) } ?: ReleasesPageUrl
        val intent = Intent(Intent.ACTION_VIEW, safeUrl.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No browser available to open $safeUrl", e)
        }
    }

    /** 清理上次遗留的过期 APK 缓存文件，避免占用存储。 */
    fun cleanupOldApksAsync() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val dir = updatesCacheDir()
                if (!dir.exists()) return@runCatching
                val now = System.currentTimeMillis()
                dir.listFiles()?.forEach { file ->
                    val isPart = file.name.endsWith(".part")
                    val isOld = now - file.lastModified() > StaleApkMaxAgeMs
                    val isAlreadyInstalled = extractApkVersionFromFileName(file.name)?.let { cachedVer ->
                        !AppVersion.isNewer(cachedVer, currentVersionName)
                    } ?: false
                    if (isPart || isOld || isAlreadyInstalled) {
                        file.delete()
                    }
                }
            }
        }
    }

    private fun extractApkVersionFromFileName(fileName: String): String? {
        if (!fileName.startsWith("JeekAlarm-") || !fileName.endsWith(".apk")) return null
        return fileName.removePrefix("JeekAlarm-").removeSuffix(".apk")
    }

    private fun updatesCacheDir(): File {
        return File(App.context.cacheDir, "updates")
    }

    private sealed interface FetchResult {
        data class Success(val release: ReleaseInfo?) : FetchResult
        data object NoRelease : FetchResult
        data class Failure(val message: String) : FetchResult
    }

    private fun fetchLatestRelease(): FetchResult {
        val localized = App.localizedContext()
        var connection: HttpURLConnection? = null
        return try {
            connection = openHttpsConnection(
                url = LatestReleaseApiUrl,
                connectTimeoutMs = ApiConnectTimeoutMs,
                readTimeoutMs = ApiReadTimeoutMs,
                headers = mapOf(
                    "Accept" to "application/vnd.github+json",
                    "X-GitHub-Api-Version" to "2022-11-28",
                    "User-Agent" to "JeekAlarm/$currentVersionName",
                ),
            )
            val code = connection.responseCode
            when (code) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val release = UpdateParser.parseGitHubRelease(
                        responseBody = body,
                        supportedAbis = Build.SUPPORTED_ABIS.toList(),
                        fallbackHtmlUrl = ReleasesPageUrl,
                    )
                    FetchResult.Success(release)
                }

                HttpURLConnection.HTTP_NOT_FOUND -> FetchResult.NoRelease
                HttpURLConnection.HTTP_FORBIDDEN, 429 -> FetchResult.Failure(
                    localized.getString(R.string.update_error_rate_limited)
                )

                else -> FetchResult.Failure(
                    localized.getString(R.string.update_error_http, code)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FetchResult.Failure(
                e.message?.takeIf { it.isNotBlank() }
                    ?: localized.getString(R.string.update_error_network)
            )
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun downloadAndVerifyApk(
        release: ReleaseInfo,
        asset: ApkAsset,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File {
        val localized = App.localizedContext()
        val dir = updatesCacheDir()
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException(localized.getString(R.string.update_error_storage))
        }
        // 下载新包前清理旧的缓存文件
        dir.listFiles()?.forEach { it.delete() }

        val safeVersion = release.versionName.replace(Regex("[^0-9A-Za-z._-]"), "_")
        val partFile = File(dir, "JeekAlarm-$safeVersion.apk.part")
        val targetFile = File(dir, "JeekAlarm-$safeVersion.apk")

        var connection: HttpURLConnection? = null
        try {
            connection = openHttpsConnection(
                url = asset.downloadUrl,
                connectTimeoutMs = DownloadConnectTimeoutMs,
                readTimeoutMs = DownloadReadTimeoutMs,
                headers = mapOf(
                    "Accept" to "application/octet-stream",
                    "User-Agent" to "JeekAlarm/$currentVersionName",
                ),
            )
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException(localized.getString(R.string.update_error_http, code))
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: asset.sizeBytes
            val sha256Digest = if (asset.sha256 != null) MessageDigest.getInstance("SHA-256") else null

            var downloadedBytes = 0L
            var lastReportedBytes = 0L
            var lastReportTimeMs = 0L
            val buffer = ByteArray(16 * 1024)

            connection.inputStream.use { input ->
                FileOutputStream(partFile).use { output ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        sha256Digest?.update(buffer, 0, read)
                        downloadedBytes += read

                        val now = System.currentTimeMillis()
                        if (now - lastReportTimeMs >= ProgressThrottleMs ||
                            downloadedBytes - lastReportedBytes >= ProgressThrottleBytes
                        ) {
                            lastReportTimeMs = now
                            lastReportedBytes = downloadedBytes
                            onProgress(downloadedBytes, totalBytes)
                        }
                    }
                    output.flush()
                }
            }
            onProgress(downloadedBytes, totalBytes)

            if (asset.sizeBytes > 0L && downloadedBytes != asset.sizeBytes) {
                throw IOException(localized.getString(R.string.update_error_verification_failed))
            }

            if (asset.sha256 != null && sha256Digest != null) {
                val actualHash = UpdateParser.toHex(sha256Digest.digest())
                if (!actualHash.equals(asset.sha256, ignoreCase = true)) {
                    throw IOException(localized.getString(R.string.update_error_verification_failed))
                }
            }

            if (!verifyApkPackage(partFile)) {
                throw IOException(localized.getString(R.string.update_error_invalid_apk))
            }

            if (targetFile.exists()) targetFile.delete()
            if (!partFile.renameTo(targetFile)) {
                partFile.copyTo(targetFile, overwrite = true)
                partFile.delete()
            }
            return targetFile
        } catch (e: Throwable) {
            partFile.delete()
            targetFile.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    private fun verifyApkPackage(apkFile: File): Boolean {
        val pm = App.context.packageManager
        val info: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
        }
        return info != null && info.packageName == App.context.packageName
    }

    /** 手动跟踪重定向并强制要求所有跳转目标均为 HTTPS，避免协议降级。 */
    private fun openHttpsConnection(
        url: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        headers: Map<String, String>,
    ): HttpURLConnection {
        var currentUrl = url
        for (hop in 0..MaxRedirects) {
            if (!UpdateParser.isSafeHttpsUrl(currentUrl)) {
                throw IOException("Unsupported update URL scheme")
            }
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                requestMethod = "GET"
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = conn.responseCode
            if (code in listOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307,
                    308,
                )
            ) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IOException("Redirect without Location header")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
                continue
            }
            return conn
        }
        throw IOException("Too many redirects")
    }
}
