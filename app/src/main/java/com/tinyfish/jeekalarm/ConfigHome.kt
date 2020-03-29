package com.tinyfish.jeekalarm

import androidx.ui.material.ColorPalette
import com.tinyfish.jeekalarm.start.App
import java.io.File

object ConfigHome {
    var data = ConfigData()

    data class ConfigData(
        var defaultMusicFile: String = "",
        var defaultMusicFolder: String = "",
        var theme: String = "Dark"
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