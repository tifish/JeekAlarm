package com.tinyfish.jeekalarm

import android.content.SharedPreferences
import android.os.Environment
import com.tinyfish.jeekalarm.start.App
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ConfigService {
    var data = ConfigData()

    @Serializable
    data class ConfigData(
        var defaultMusicFile: String = "",
        var defaultMusicFolder: String = "",
        var theme: String = "Dark",
        var openAIApiKey: String = "",
    )

    private val sharedPrefs: SharedPreferences by lazy {
        App.context.getSharedPreferences("Config", 0)
    }

    var configDir: String
        get() = sharedPrefs.getString("ConfigDir", "")!!
        set(value) {
            sharedPrefs.edit().apply {
                putString("ConfigDir", value)
                apply()
            }
        }

    val configFile: File
        get() {
            val dir = if (configDir == "")
                App.context.filesDir
            else
                File(Environment.getExternalStorageDirectory().path, configDir)

            return File(dir, "config.json")
        }

    fun load() {
        if (!configFile.exists())
            return

        data = Json.decodeFromString(configFile.readText())
        if (data.openAIApiKey != "")
            data.openAIApiKey = CryptoService.decrypt(data.openAIApiKey)
    }

    fun save() {
        val originalKey = data.openAIApiKey
        if (data.openAIApiKey != "")
            data.openAIApiKey = CryptoService.encrypt(data.openAIApiKey)

        configFile.writeText(Json.encodeToString(data))

        if (data.openAIApiKey != "")
            data.openAIApiKey = originalKey
    }
}