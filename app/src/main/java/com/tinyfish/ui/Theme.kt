package com.tinyfish.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorPalette = darkColors(
    primary = MaterialColor.LightBlue900, // TopAppBar, BottomAppBar, Button, selected text, cursor
    primaryVariant = MaterialColor.Grey900, // MyGroupBox
    onPrimary = Color.White,
    secondary = MaterialColor.LightBlue400, // Radio button on, floating action button, slider, progress bar, link and headline
    secondaryVariant = MaterialColor.LightBlue400, // Switch on
    onSecondary = Color.White,
    surface = Color.LightGray, // Surface, Switch off, disabled button background, card, sheet, menu
    onSurface = Color.White, // text on Surface, Switch off background, Radio button circle
    background = Color.Black, // My background
    onBackground = Color.White,
)

val LightColorPalette = lightColors(
    primary = MaterialColor.LightBlue100, // TopAppBar, BottomAppBar, Button, selected text, cursor
    primaryVariant = MaterialColor.Grey100, // MyGroupBox
    onPrimary = Color.Black,
    secondary = MaterialColor.LightBlue600, // Radio button on, floating action button, slider, progress bar, link and headline
    secondaryVariant = MaterialColor.LightBlue600, // Switch on
    onSecondary = Color.Black,
    surface = Color.LightGray, // Surface, Switch off, disabled button background, card, sheet, menu
    onSurface = Color.Black, // text on Surface, Switch off background, Radio button circle
    background = Color.White, // My background
    onBackground = Color.Black,
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