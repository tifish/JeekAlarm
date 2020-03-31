package com.tinyfish.jeekalarm.ui

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.Text
import androidx.ui.core.TextField
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.Icon
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shape
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.layout.*
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Switch
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.text.TextStyle
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import kotlin.reflect.KMutableProperty0

@Composable
fun SimpleSwitch(
    hint: String,
    booleanProp: KMutableProperty0<Boolean>,
    textStyle: TextStyle? = null,
    onCheckedChange: (Boolean) -> Unit = {},
    textModifier: Modifier = Modifier.None
) {
    Row {
        Recompose { recompose ->
            Switch(
                checked = booleanProp.get(),
                onCheckedChange = {
                    booleanProp.set(it)
                    onCheckedChange(it)
                    recompose()
                }
            )

            Spacer(LayoutWidth(10.dp))

            Clickable(onClick = {
                booleanProp.set(!booleanProp.get())
                recompose()
            }) {
                Text(hint, style = textStyle, modifier = textModifier)
            }
        }
    }
}

@Composable
fun SimpleSwitch(
    hint: String,
    value: Boolean,
    textStyle: TextStyle? = null,
    onCheckedChange: (Boolean) -> Unit = {},
    textModifier: Modifier = Modifier.None
) {
    Row {
        Switch(
            checked = value,
            onCheckedChange = {
                onCheckedChange(it)
                // recompose()
            }
        )

        Spacer(LayoutWidth(10.dp))

        Clickable(onClick = {
            onCheckedChange(!value)
        }) {
            Text(hint, style = textStyle, modifier = textModifier)
        }
    }
}

@Composable
fun SimpleSwitchOnText(
    hint: String,
    value: Boolean,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Column {
        Switch(
            checked = value,
            onCheckedChange = {
                onCheckedChange(it)
            }
        )

        if (hint != "") {
            Spacer(modifier = LayoutHeight(3.dp))
            Text(text = hint, modifier = LayoutGravity.Center)
        }
    }
}

@Composable
fun SimpleTextField(
    hint: String,
    textProp: KMutableProperty0<String>,
    onFocus: () -> Unit = {},
    onBlur: () -> Unit = {},
    modifier: Modifier = Modifier.None,
    hintModifier: Modifier = Modifier.None,
    hintStyle: TextStyle? = null,
    textModifier: Modifier = Modifier.None,
    textStyle: TextStyle? = null
) {
    Row(modifier) {
        Text(hint, hintModifier, style = hintStyle)

        // var textRange by state { TextRange(0, 0) }

        Recompose { recompose ->
            TextField(
                modifier = textModifier,
                value = textProp.get(),
                onValueChange = {
                    textProp.set(it)
                    recompose()
                },
                onFocus = {
                    // textRange = TextRange(0, textProp.get().length)

                    onFocus()
                },
                onBlur = {
                    // textRange = TextRange(0, 0)

                    onBlur()
                },
                textStyle = textStyle
            )
        }
    }
}

@Composable
fun SimpleVectorButton(
    vectorAsset: VectorAsset,
    text: String = "",
    onClick: () -> Unit
) {
    Ripple(bounded = false) {
        Clickable(onClick = onClick) {
            Column {
                Container(
                    width = vectorAsset.defaultWidth,
                    height = vectorAsset.defaultHeight,
                    modifier = LayoutGravity.Center
                ) {
                    Icon(vectorAsset)
                }

                if (text != "") {
                    Spacer(modifier = LayoutHeight(vectorAsset.defaultHeight / 8))
                    Text(text = text, modifier = LayoutGravity.Center)
                }
            }
        }
    }
}

@Composable
fun SimpleTextButton(
    text: String = "",
    width: Dp? = null,
    height: Dp? = null,
    shape: Shape = MaterialTheme.shapes().button,
    backgroundColor: Color = MaterialTheme.colors().primary,
    onClick: () -> Unit
) {
    Container(width = width, height = height) {
        Surface(shape = shape, color = backgroundColor) {
            Ripple(bounded = false) {
                Clickable(onClick = onClick) {
                    Align(Alignment.Center) {
                        Text(text = text)
                    }
                }
            }
        }
    }
}
