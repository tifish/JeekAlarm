package com.tinyfish.jeekalarm

import android.content.SharedPreferences
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.tinyfish.jeekalarm.start.App
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.Executors
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object SettingsService {
    private const val SaveDebounceMs = 400L

    private val ioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SettingsIO")
    }
    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
    private var pendingSaveRunnable: Runnable? = null

    var defaultMusicFile by SettingsItemState("")
    var defaultMusicFolder by SettingsItemState("")
    var theme by SettingsItemState("Dark")
    // 应用内语言：""=跟随系统，"en"=English，"zh"=中文。
    var language by SettingsItemState("")
    var openAiApiUrl by SettingsItemState("")
    var openAiApiKey by SettingsItemState("")
    var openAiApiModel by SettingsItemState("")
    var iFlyAppId by SettingsItemState("")
    var autoCheckUpdate by SettingsItemState(true)

    class SettingsItemState<T>(initial: T) : ReadWriteProperty<SettingsService, T> {
        private val _state = mutableStateOf(initial)

        override fun getValue(thisRef: SettingsService, property: KProperty<*>): T {
            return _state.value
        }

        override fun setValue(thisRef: SettingsService, property: KProperty<*>, value: T) {
            if (_state.value == value)
                return
            _state.value = value
            scheduleSave()
        }
    }

    private val sharedPrefs: SharedPreferences by lazy {
        App.context.getSharedPreferences("Config", 0)
    }

    var settingsDir: String
        get() = sharedPrefs.getString("ConfigDir", "") ?: ""
        set(value) {
            sharedPrefs.edit().apply {
                putString("ConfigDir", value)
                apply()
            }
        }

    var lastUpdateCheckTimeMs: Long
        get() = sharedPrefs.getLong("LastUpdateCheckTimeMs", 0L)
        set(value) {
            sharedPrefs.edit().putLong("LastUpdateCheckTimeMs", value).apply()
        }

    var skippedUpdateVersion: String
        get() = sharedPrefs.getString("SkippedUpdateVersion", "") ?: ""
        set(value) {
            sharedPrefs.edit().putString("SkippedUpdateVersion", value).apply()
        }

    private fun isContentTree(uri: String = settingsDir): Boolean {
        return uri.startsWith("content://")
    }

    private fun usesLegacyExternalConfigDir(): Boolean {
        return settingsDir.isNotEmpty() && !isContentTree()
    }

    private fun legacyConfigFile(fileName: String): File {
        val dir =
            if (settingsDir == "")
                App.context.filesDir
            else
                File(Environment.getExternalStorageDirectory().path, settingsDir)

        return File(dir, fileName)
    }

    private fun internalConfigFile(fileName: String): File {
        return File(App.context.filesDir, fileName)
    }

    private fun fileConfigCandidates(fileName: String): List<File> {
        val legacyFile = legacyConfigFile(fileName)
        if (!usesLegacyExternalConfigDir())
            return listOf(legacyFile)

        val internalFile = internalConfigFile(fileName)
        return if (legacyFile.exists())
            listOf(legacyFile, internalFile)
        else
            listOf(internalFile, legacyFile)
    }

    private fun configDocument(fileName: String, create: Boolean): DocumentFile? {
        if (!isContentTree())
            return null

        val dir = DocumentFile.fromTreeUri(App.context, settingsDir.toUri()) ?: return null
        val existing = dir.findFile(fileName)
        if (existing != null || !create)
            return existing

        val mimeType =
            if (fileName.endsWith(".json"))
                "application/json"
            else
                "text/plain"
        return dir.createFile(mimeType, fileName)
    }

    fun configExists(fileName: String): Boolean {
        return if (isContentTree())
            configDocument(fileName, false)?.exists() == true
        else
            fileConfigCandidates(fileName).any { it.exists() }
    }

    fun readConfigText(fileName: String): String? {
        return runCatching {
            if (isContentTree()) {
                val document = configDocument(fileName, false) ?: return null
                if (!document.exists())
                    return null
                return App.context.contentResolver.openInputStream(document.uri)?.bufferedReader()?.use { it.readText() }
            }

            for (file in fileConfigCandidates(fileName)) {
                if (!file.exists())
                    continue

                val text = runCatching { file.readText() }.getOrNull()
                if (text != null)
                    return text
            }

            null
        }.getOrNull()
    }

    fun readConfigLines(fileName: String): List<String> {
        return readConfigText(fileName)?.lineSequence()?.toList() ?: emptyList()
    }

    fun writeConfigText(fileName: String, text: String) {
        runCatching {
            if (!isContentTree()) {
                for (file in fileConfigCandidates(fileName)) {
                    val wrote = runCatching {
                        file.parentFile?.mkdirs()
                        file.writeText(text)
                    }.isSuccess
                    if (wrote)
                        return
                }
                return
            }

            val document = configDocument(fileName, true) ?: return
            App.context.contentResolver.openOutputStream(document.uri, "wt")?.bufferedWriter()?.use {
                it.write(text)
            }
        }
    }

    @Serializable
    private data class SettingsData(
        var defaultMusicFile: String = "",
        var defaultMusicFolder: String = "",
        var theme: String = "Dark",
        var language: String = "",
        var openAiApiUrl: String = "",
        var openAiApiKey: String = "",
        var openAiApiModel: String = "",
        var iFlyAppId: String = "",
        var autoCheckUpdate: Boolean = true,
    )

    private var _isLoading = false
    private val json = Json { ignoreUnknownKeys = true }

    fun load() {
        cancelPendingSave()
        // Wait for any in-flight background write to finish before reading.
        runCatching { ioExecutor.submit {}.get() }

        val text = readConfigText("config.json")
        if (text == null)
            return

        _isLoading = true
        try {

            val data = runCatching { json.decodeFromString<SettingsData>(text) }.getOrNull() ?: return

            defaultMusicFile = data.defaultMusicFile
            defaultMusicFolder = data.defaultMusicFolder
            theme = data.theme
            language = data.language

            openAiApiUrl = data.openAiApiUrl
            openAiApiModel = data.openAiApiModel

            if (data.openAiApiKey != "")
                data.openAiApiKey = CryptoService.decrypt(data.openAiApiKey)
            openAiApiKey = data.openAiApiKey

            if (data.iFlyAppId != "")
                data.iFlyAppId = CryptoService.decrypt(data.iFlyAppId)
            iFlyAppId = data.iFlyAppId
            autoCheckUpdate = data.autoCheckUpdate
        } finally {
            _isLoading = false
        }
    }

    private fun capturePlainSnapshot(): SettingsData {
        return SettingsData(
            defaultMusicFile = defaultMusicFile,
            defaultMusicFolder = defaultMusicFolder,
            theme = theme,
            language = language,
            openAiApiUrl = openAiApiUrl,
            openAiApiKey = openAiApiKey,
            openAiApiModel = openAiApiModel,
            iFlyAppId = iFlyAppId,
            autoCheckUpdate = autoCheckUpdate,
        )
    }

    private fun writeSnapshot(plainSnapshot: SettingsData) {
        val data = plainSnapshot.copy(
            openAiApiKey = if (plainSnapshot.openAiApiKey != "") CryptoService.encrypt(plainSnapshot.openAiApiKey) else "",
            iFlyAppId = if (plainSnapshot.iFlyAppId != "") CryptoService.encrypt(plainSnapshot.iFlyAppId) else "",
        )
        writeConfigText("config.json", json.encodeToString(data))
    }

    private fun cancelPendingSave() {
        pendingSaveRunnable?.let {
            mainHandler.removeCallbacks(it)
            pendingSaveRunnable = null
        }
    }

    private fun scheduleSave() {
        if (_isLoading)
            return

        cancelPendingSave()
        val runnable = Runnable {
            pendingSaveRunnable = null
            val snapshot = capturePlainSnapshot()
            ioExecutor.execute { writeSnapshot(snapshot) }
        }
        pendingSaveRunnable = runnable
        mainHandler.postDelayed(runnable, SaveDebounceMs)
    }

    /** 立即把尚未落盘的防抖改动提交到后台线程（例如切后台时调用）。 */
    fun flushSave() {
        if (_isLoading || pendingSaveRunnable == null)
            return

        cancelPendingSave()
        val snapshot = capturePlainSnapshot()
        ioExecutor.execute { writeSnapshot(snapshot) }
    }

    /** 显式立即保存（例如切换配置文件夹覆盖写入）。 */
    fun save() {
        if (_isLoading)
            return

        cancelPendingSave()
        val snapshot = capturePlainSnapshot()
        runCatching { ioExecutor.submit { writeSnapshot(snapshot) }.get() }
    }
}
