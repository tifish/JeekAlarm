package com.tinyfish.jeekalarm

import java.io.File

object Config {
    var data = ConfigData()

    data class ConfigData(
        var defaultMusicFile: String = "",
        var defaultMusicFolder: String = ""
    )

    private val configFile: File by lazy {
        File(App.context.getExternalFilesDir(null), "config.json")
    }

    fun load() {
        if (configFile.exists())
            data = App.json.parse<ConfigData>(configFile) ?: ConfigData()
    }

    fun save() {
        configFile.writeText(App.json.toJsonString(data))
    }
}