package com.tinyfish.jeekalarm.ui

import androidx.compose.Composable
import androidx.ui.foundation.isSystemInDarkTheme
import androidx.ui.graphics.Color
import androidx.ui.material.MaterialTheme
import androidx.ui.material.darkColorPalette
import androidx.ui.material.lightColorPalette

val DarkColorPalette = darkColorPalette(
    primary = MaterialColor.LightBlue400, // TopAppBar, BottomAppBar, Button
    primaryVariant = MaterialColor.LightBlue400,
    secondary = MaterialColor.LightBlue400, // Radio button
    surface = Color.White // Surface, Switch off
    // onPrimary // text on primary
    // onSurface // text on Surface, Switch off background
)

val LightColorPalette = lightColorPalette(
    primary = MaterialColor.LightBlue600, // TopAppBar, BottomAppBar, Button
    primaryVariant = MaterialColor.LightBlue600,
    secondary = MaterialColor.LightBlue600, // Radio button
    secondaryVariant = MaterialColor.LightBlue600, // Switch on
    surface = Color.Gray // Surface, Switch off
    // onPrimary // text on primary
    // onSurface // text on Surface, Switch off background
)

@Composable
fun Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}