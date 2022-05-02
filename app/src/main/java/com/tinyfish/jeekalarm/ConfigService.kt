package com.tinyfish.jeekalarm

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
        var theme: String = "Dark"
    )

    private val configFile: File by lazy {
        File(App.context.filesDir, "config.json")
    }

    fun load() {
        if (configFile.exists())
            data = Json.decodeFromString(configFile.readText())
    }

    fun save() {
        configFile.writeText(Json.encodeToString(data))
    }
}