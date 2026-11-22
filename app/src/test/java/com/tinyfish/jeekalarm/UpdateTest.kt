package com.tinyfish.jeekalarm

import com.tinyfish.jeekalarm.update.AppVersion
import com.tinyfish.jeekalarm.update.GitHubAssetDto
import com.tinyfish.jeekalarm.update.UpdateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTest {
    @Test
    fun versionComparisonHandlesCommitCountVersions() {
        assertTrue(AppVersion.isNewer("238", "237"))
        assertTrue(AppVersion.isNewer("v238", "237"))
        assertTrue(AppVersion.isNewer("r238", "237"))
        assertTrue(AppVersion.isNewer("100", "99"))
        assertFalse(AppVersion.isNewer("237", "237"))
        assertFalse(AppVersion.isNewer("v237", "237"))
        assertFalse(AppVersion.isNewer("236", "237"))
    }

    @Test
    fun versionComparisonHandlesNumericSegmentsAndPrefixes() {
        assertEquals(0, AppVersion.parse("1.0")!!.compareTo(AppVersion.parse("1.0.0")!!))
        assertEquals(0, AppVersion.parse("v1.2.0")!!.compareTo(AppVersion.parse("V1.2")!!))
        assertTrue(AppVersion.isNewer("v1.0.1", "1.0"))
        assertTrue(AppVersion.isNewer("1.10.0", "1.2.9"))
        assertFalse(AppVersion.isNewer("1.0.0", "1.0"))
        assertFalse(AppVersion.isNewer("0.9.9", "1.0"))
    }

    @Test
    fun versionComparisonHandlesPreReleaseAndBuildMetadata() {
        assertTrue(AppVersion.isNewer("1.2.0", "1.2.0-beta.1"))
        assertFalse(AppVersion.isNewer("1.2.0-beta.1", "1.2.0"))
        assertTrue(AppVersion.isNewer("1.2.0-beta.10", "1.2.0-beta.2"))
        assertTrue(AppVersion.isNewer("1.2.0-rc.1", "1.2.0-beta.9"))
        assertEquals(0, AppVersion.parse("1.2.0+10")!!.compareTo(AppVersion.parse("1.2.0+99")!!))
    }

    @Test
    fun invalidVersionsAreRejectedSafely() {
        assertNull(AppVersion.parse(""))
        assertNull(AppVersion.parse("v"))
        assertNull(AppVersion.parse("latest"))
        assertNull(AppVersion.parse("1..0"))
        assertFalse(AppVersion.isNewer("nightly", "1.0"))
    }

    @Test
    fun parseGitHubReleaseExtractsReleaseAndSha256() {
        val digestHex = "a".repeat(64)
        val payload = """
            {
              "tag_name": "v1.3.0",
              "name": "JeekAlarm 1.3.0",
              "body": "- Added auto-update\n- Bug fixes",
              "html_url": "https://github.com/tifish/JeekAlarm/releases/tag/v1.3.0",
              "draft": false,
              "prerelease": false,
              "published_at": "2026-11-20T08:00:00Z",
              "assets": [
                {
                  "name": "checksums.txt",
                  "browser_download_url": "https://github.com/tifish/JeekAlarm/releases/download/v1.3.0/checksums.txt",
                  "size": 128
                },
                {
                  "name": "JeekAlarm-v1.3.0-release.apk",
                  "browser_download_url": "https://github.com/tifish/JeekAlarm/releases/download/v1.3.0/JeekAlarm-v1.3.0-release.apk",
                  "size": 5242880,
                  "digest": "sha256:$digestHex"
                }
              ]
            }
        """.trimIndent()

        val release = UpdateParser.parseGitHubRelease(
            responseBody = payload,
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
            fallbackHtmlUrl = "https://github.com/tifish/JeekAlarm/releases/latest",
        )
        assertNotNull(release)
        release!!
        assertEquals("1.3.0", release.versionName)
        assertEquals("v1.3.0", release.tagName)
        assertEquals("JeekAlarm 1.3.0", release.title)
        assertEquals("- Added auto-update\n- Bug fixes", release.releaseNotes)
        assertNotNull(release.apkAsset)
        assertEquals("JeekAlarm-v1.3.0-release.apk", release.apkAsset!!.name)
        assertEquals(5242880L, release.apkAsset!!.sizeBytes)
        assertEquals(digestHex, release.apkAsset!!.sha256)
    }

    @Test
    fun parseGitHubReleaseSkipsDraftAndPrerelease() {
        val draftJson = """{"tag_name":"v2.0.0","draft":true,"prerelease":false}"""
        val preJson = """{"tag_name":"v2.0.0-beta.1","draft":false,"prerelease":true}"""

        assertNull(UpdateParser.parseGitHubRelease(draftJson, listOf("arm64-v8a"), "https://example.com"))
        assertNull(UpdateParser.parseGitHubRelease(preJson, listOf("arm64-v8a"), "https://example.com"))
    }

    @Test
    fun selectBestApkAssetPrefersMatchingAbiThenUniversal() {
        val assets = listOf(
            GitHubAssetDto("app-armeabi-v7a-release.apk", "https://example.com/v7a.apk", 100),
            GitHubAssetDto("app-arm64-v8a-debug.apk", "https://example.com/v8a-debug.apk", 100),
            GitHubAssetDto("app-arm64-v8a-release.apk", "https://example.com/v8a.apk", 100),
            GitHubAssetDto("app-universal-release.apk", "https://example.com/universal.apk", 100),
            GitHubAssetDto("app-insecure.apk", "http://example.com/insecure.apk", 100),
        )

        val arm64 = UpdateParser.selectBestApkAsset(assets, listOf("arm64-v8a", "armeabi-v7a"))
        assertEquals("app-arm64-v8a-release.apk", arm64?.name)

        val arm32 = UpdateParser.selectBestApkAsset(assets, listOf("armeabi-v7a"))
        assertEquals("app-armeabi-v7a-release.apk", arm32?.name)

        val x86 = UpdateParser.selectBestApkAsset(assets, listOf("x86_64"))
        assertEquals("app-universal-release.apk", x86?.name)
    }

    @Test
    fun urlAndDigestValidationRejectUnsafeInputs() {
        assertTrue(UpdateParser.isSafeHttpsUrl("https://github.com/tifish/JeekAlarm/releases"))
        assertFalse(UpdateParser.isSafeHttpsUrl("http://github.com/tifish/JeekAlarm/releases"))
        assertFalse(UpdateParser.isSafeHttpsUrl("file:///sdcard/evil.apk"))
        assertFalse(UpdateParser.isSafeHttpsUrl(""))

        assertNull(UpdateParser.parseSha256Digest("md5:1234"))
        assertNull(UpdateParser.parseSha256Digest("sha256:not-hex"))
        assertEquals("0".repeat(64), UpdateParser.parseSha256Digest("SHA256:${"0".repeat(64)}"))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            UpdateParser.sha256Of(ByteArray(0)),
        )
    }

    @Test
    fun autoCheckThrottleAndSkipVersionLogic() {
        val dayMs = 24L * 60 * 60 * 1000
        assertTrue(UpdateParser.shouldAutoCheck(true, false, 0L, 1_000L, dayMs))
        assertFalse(UpdateParser.shouldAutoCheck(false, false, 0L, 1_000L, dayMs))
        assertFalse(UpdateParser.shouldAutoCheck(true, true, 0L, dayMs * 2, dayMs))
        assertFalse(UpdateParser.shouldAutoCheck(true, false, 10_000L, 10_000L + dayMs - 1, dayMs))
        assertTrue(UpdateParser.shouldAutoCheck(true, false, 10_000L, 10_000L + dayMs, dayMs))

        assertTrue(UpdateParser.shouldPromptAutoUpdate("v1.2.0", "1.0", ""))
        assertFalse(UpdateParser.shouldPromptAutoUpdate("v1.2.0", "1.0", "1.2.0"))
        assertFalse(UpdateParser.shouldPromptAutoUpdate("1.2.0", "1.0", "v1.2.0"))
        assertTrue(UpdateParser.shouldPromptAutoUpdate("v1.3.0", "1.0", "1.2.0"))
        assertFalse(UpdateParser.shouldPromptAutoUpdate("1.0", "1.0", ""))
    }

    @Test
    fun formatFileSizeFormatsUnitsCleanly() {
        assertEquals("512 B", UpdateParser.formatFileSize(512))
        assertEquals("10 KB", UpdateParser.formatFileSize(10 * 1024))
        assertEquals("5.5 MB", UpdateParser.formatFileSize((5.5 * 1024 * 1024).toLong()))
    }
}
