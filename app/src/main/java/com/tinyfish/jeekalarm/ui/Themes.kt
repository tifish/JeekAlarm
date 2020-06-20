package com.tinyfish.jeekalarm.ui

import androidx.ui.graphics.Color
import androidx.ui.material.ColorPalette

object DarkColorPalette : ColorPalette {
    override var primary = Color(0xFF039BE5) // TopAppBar, BottomAppBar, Button
    override var primaryVariant = Color.Magenta
    override var secondary = Color(0xFF039BE5) // Radio button
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

object LightColorPalette : ColorPalette {
    override var primary = Color(0xFF039BE5) // TopAppBar, BottomAppBar, Button
    override var primaryVariant = Color.Magenta
    override var secondary = Color(0xFF039BE5) // Radio button
    override var secondaryVariant = Color(0xFF039BE5) // Switch on
    override var background = Color.White // Scaffold's content background
    override var surface = Color.Gray // Surface, Switch off
    override var error = Color.Red
    override var onPrimary = Color.White // text on primary
    override var onSecondary = Color.Black
    override var onBackground = Color.Black
    override var onSurface = Color.Black // text on Surface, Switch off background
    override var onError = Color.Magenta
    override val isLight = true
}
