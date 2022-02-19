package com.tinyfish.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorPalette = darkColors(
    primary = MaterialColor.LightBlue400, // TopAppBar, BottomAppBar, Button
    primaryVariant = MaterialColor.LightBlue400, // top status bar
    secondary = MaterialColor.LightBlue400, // Switch on, Radio button on, floating action button, slider, selected text, progress bar, link and headline
    surface = Color.LightGray, // Surface, Switch off, disabled button background, card, sheet, menu
    // onPrimary // text on primary
    // onSurface // text on Surface, Switch off background
    // background
)

val LightColorPalette = lightColors(
    primary = MaterialColor.LightBlue600,
    primaryVariant = MaterialColor.LightBlue600,
    secondary = MaterialColor.LightBlue600,
    secondaryVariant = MaterialColor.LightBlue600,
    surface = Color.LightGray

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun ComposeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}