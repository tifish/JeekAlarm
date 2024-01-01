package com.tinyfish.jeekalarm

import android.content.SharedPreferences
import android.os.Environment
import com.tinyfish.jeekalarm.start.App
import kotlinx.serialization.Serializable
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
        var defaultAi: String = "OpenAI",
        var openAiApiKey: String = "",
        var geminiKey: String = "",
        var iFlyAppId: String = "",
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
            val dir =
                if (configDir == "")
                    App.context.filesDir
                else
                    File(Environment.getExternalStorageDirectory().path, configDir)

            return File(dir, "config.json")
        }

    fun load() {
        if (!configFile.exists())
            return

        data = Json.decodeFromString(configFile.readText())
        if (data.openAiApiKey != "")
            data.openAiApiKey = CryptoService.decrypt(data.openAiApiKey)
        if (data.geminiKey != "")
            data.geminiKey = CryptoService.decrypt(data.geminiKey)
        if (data.iFlyAppId != "")
            data.iFlyAppId = CryptoService.decrypt(data.iFlyAppId)

        App.openAiApiKeyChangedTrigger++
        App.geminiApiKeyChangedTrigger++
        App.iFlyAppIdChangedTrigger++
    }

    fun save() {
        val originalOpenAiApiKey = data.openAiApiKey
        if (data.openAiApiKey != "")
            data.openAiApiKey = CryptoService.encrypt(data.openAiApiKey)
        val originalGeminiKey = data.geminiKey
        if (data.geminiKey != "")
            data.geminiKey = CryptoService.encrypt(data.geminiKey)
        val originalIFlyAppId = data.iFlyAppId
        if (data.iFlyAppId != "")
            data.iFlyAppId = CryptoService.encrypt(data.iFlyAppId)

        configFile.writeText(Json.encodeToString(data))

        if (data.openAiApiKey != "")
            data.openAiApiKey = originalOpenAiApiKey
        if (data.geminiKey != "")
            data.geminiKey = originalGeminiKey
        if (data.iFlyAppId != "")
            data.iFlyAppId = originalIFlyAppId
    }
}