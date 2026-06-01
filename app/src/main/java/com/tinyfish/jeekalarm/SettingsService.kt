package com.tinyfish.jeekalarm

import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import androidx.documentfile.provider.DocumentFile
import com.tinyfish.jeekalarm.start.App
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object SettingsService {
    var defaultMusicFile by SettingsItemState("")
    var defaultMusicFolder by SettingsItemState("")
    var theme by SettingsItemState("Dark")
    var openAiApiUrl by SettingsItemState("")
    var openAiApiKey by SettingsItemState("")
    var openAiApiModel by SettingsItemState("")
    var iFlyAppId by SettingsItemState("")

    class SettingsItemState<T>(initial: T) : ReadWriteProperty<SettingsService, T> {
        private val _state = mutableStateOf(initial)

        override fun getValue(thisRef: SettingsService, property: KProperty<*>): T {
            return _state.value
        }

        override fun setValue(thisRef: SettingsService, property: KProperty<*>, value: T) {
            _state.value = value
            save()
        }
    }

    private val sharedPrefs: SharedPreferences by lazy {
        App.context.getSharedPreferences("Config", 0)
    }

    var settingsDir: String
        get() = sharedPrefs.getString("ConfigDir", "")!!
        set(value) {
            sharedPrefs.edit().apply {
                putString("ConfigDir", value)
                apply()
            }
        }

    val settingsFile: File
        get() {
            val dir =
                if (settingsDir == "" || settingsDir.startsWith("content://"))
                    App.context.filesDir
                else
                    File(Environment.getExternalStorageDirectory().path, settingsDir)

            return File(dir, "config.json")
        }

    private fun isContentTree(uri: String = settingsDir): Boolean {
        return uri.startsWith("content://")
    }

    fun usesLegacyExternalConfigDir(): Boolean {
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

        val dir = DocumentFile.fromTreeUri(App.context, Uri.parse(settingsDir)) ?: return null
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
                if (!configExists(fileName))
                    return null

                val document = configDocument(fileName, false) ?: return null
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
        var openAiApiUrl: String = "",
        var openAiApiKey: String = "",
        var openAiApiModel: String = "",
        var iFlyAppId: String = "",
    )

    private var _isLoading = false
    private val json = Json { ignoreUnknownKeys = true }

    fun load() {
        val text = readConfigText("config.json")
        if (text == null)
            return

        _isLoading = true
        try {

            val data = runCatching { json.decodeFromString<SettingsData>(text) }.getOrNull() ?: return

            defaultMusicFile = data.defaultMusicFile
            defaultMusicFolder = data.defaultMusicFolder
            theme = data.theme

            openAiApiUrl = data.openAiApiUrl
            openAiApiModel = data.openAiApiModel

            if (data.openAiApiKey != "")
                data.openAiApiKey = CryptoService.decrypt(data.openAiApiKey)
            openAiApiKey = data.openAiApiKey

            if (data.iFlyAppId != "")
                data.iFlyAppId = CryptoService.decrypt(data.iFlyAppId)
            iFlyAppId = data.iFlyAppId
        } finally {
            _isLoading = false
        }
    }

    fun save() {
        if (_isLoading)
            return

        val data = SettingsData()

        data.defaultMusicFile = defaultMusicFile
        data.defaultMusicFolder = defaultMusicFolder
        data.theme = theme
        data.openAiApiUrl = openAiApiUrl
        data.openAiApiModel = openAiApiModel

        if (openAiApiKey != "")
            data.openAiApiKey = CryptoService.encrypt(openAiApiKey)

        if (iFlyAppId != "")
            data.iFlyAppId = CryptoService.encrypt(iFlyAppId)

        writeConfigText("config.json", json.encodeToString(data))
    }
}
