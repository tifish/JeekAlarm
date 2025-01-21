package com.tinyfish.jeekalarm

import android.content.SharedPreferences
import android.os.Environment
import androidx.compose.runtime.mutableStateOf
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
    var defaultAi by SettingsItemState("DeepSeek")
    var deepSeekApiKey by SettingsItemState("")
    var geminiKey by SettingsItemState("")
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
                if (settingsDir == "")
                    App.context.filesDir
                else
                    File(Environment.getExternalStorageDirectory().path, settingsDir)

            return File(dir, "config.json")
        }

    @Serializable
    private data class SettingsData(
        var defaultMusicFile: String = "",
        var defaultMusicFolder: String = "",
        var theme: String = "Dark",
        var defaultAi: String = "DeepSeek",
        var deepSeekApiKey: String = "",
        var geminiKey: String = "",
        var iFlyAppId: String = "",
    )

    private var _isLoading = false
    private val json = Json { ignoreUnknownKeys = true }

    fun load() {
        if (!settingsFile.exists())
            return

        _isLoading = true
        try {

            val data = json.decodeFromString<SettingsData>(settingsFile.readText())

            defaultMusicFile = data.defaultMusicFile
            defaultMusicFolder = data.defaultMusicFolder
            theme = data.theme
            defaultAi = data.defaultAi

            if (data.deepSeekApiKey != "")
                data.deepSeekApiKey = CryptoService.decrypt(data.deepSeekApiKey)
            deepSeekApiKey = data.deepSeekApiKey

            if (data.geminiKey != "")
                data.geminiKey = CryptoService.decrypt(data.geminiKey)
            geminiKey = data.geminiKey

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
        data.defaultAi = defaultAi

        if (deepSeekApiKey != "")
            data.deepSeekApiKey = CryptoService.encrypt(deepSeekApiKey)

        if (geminiKey != "")
            data.geminiKey = CryptoService.encrypt(geminiKey)

        if (iFlyAppId != "")
            data.iFlyAppId = CryptoService.encrypt(iFlyAppId)

        settingsFile.writeText(json.encodeToString(data))
    }
}