package com.tinyfish.jeekalarm

import com.beust.klaxon.Klaxon
import org.junit.Assert
import org.junit.Test

data class Options(
    var enabled: Boolean = true,
    var onlyOnce: Boolean = false,
    var musicFile: String = "",
    var musicFolder: String = "",
    var vibration: Boolean = true
)

class JsonTest {
    @Test
    fun stringToJson1() {
        val result = Klaxon().parse<Options>("""{"enabled":false,"onlyOnce":false,"musicFile":"a","musicFolder":"f","vibration":true,"none":2}""")
        Assert.assertEquals(false, result?.enabled)
        Assert.assertEquals(false, result?.onlyOnce)
        Assert.assertEquals("a", result?.musicFile)
    }
}