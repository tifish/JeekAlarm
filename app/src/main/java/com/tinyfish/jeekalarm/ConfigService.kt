package com.tinyfish.jeekalarm

import com.squareup.moshi.JsonAdapter
import com.tinyfish.jeekalarm.start.App
import java.io.File

object ConfigService {
    var data = ConfigData()
    private val configDataMoshiAdapter: JsonAdapter<ConfigData> = App.moshi.adapter(ConfigData::class.java)

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
            data = configDataMoshiAdapter.fromJson(configFile.readText()) ?: ConfigData()
    }

    fun save() {
        configFile.writeText(configDataMoshiAdapter.toJson(data))
    }
}