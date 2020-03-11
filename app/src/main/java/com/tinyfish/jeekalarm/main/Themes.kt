package com.tinyfish.jeekalarm.main

import androidx.ui.graphics.Color
import androidx.ui.material.ColorPalette

// 0.1.0dev6
object DarkColorPalette : ColorPalette {
    override var primary = Color(0xFF039BE5) // TopAppBar, BottomAppBar, Button
    override var primaryVariant = Color.Magenta
    override var secondary = Color.Magenta
    override var secondaryVariant = Color(0xFF039BE5) // Switch on
    override var background = Color.Black // Scaffold's content background
    override var surface = Color.White // Surface, Switch off
    override var error = Color.Red
    override var onPrimary = Color.White // text on primary
    override var onSecondary = Color.White
    override var onBackground = Color.White
    override var onSurface = Color.White // text on Surface, Switch off background
    override var onError = Color.Magenta
    override val isLight = true
}
