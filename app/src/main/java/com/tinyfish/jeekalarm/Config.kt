package com.tinyfish.jeekalarm

import java.io.File

object Config {
    var data = ConfigData()

    data class ConfigData(
        var DefaultMusicFile: String? = "Song/散曲/2CELLOS - Pirates of the Caribbean.mp3"
    )

    private val configFile: File by lazy {
        File(App.context.getExternalFilesDir(null), "config.json")
    }

    fun load() {
        if (!configFile.exists())
            return

        data = App.json.parse<ConfigData>(configFile) ?: ConfigData()
    }

    fun save() {
        configFile.writeText(App.json.toJsonString(data))
    }
}