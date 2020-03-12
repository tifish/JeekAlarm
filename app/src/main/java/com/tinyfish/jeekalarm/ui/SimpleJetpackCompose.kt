package com.tinyfish.jeekalarm.ui

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.Text
import androidx.ui.core.TextField
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.DrawBackground
import androidx.ui.graphics.vector.DrawVector
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.layout.*
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Switch
import androidx.ui.material.ripple.Ripple
import androidx.ui.text.TextFieldValue
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import kotlin.reflect.KMutableProperty0

@Composable
fun <T> Use(observeValue: T) {
}

@Composable
fun SimpleCheckbox(
    text: String,
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
                Text(text, style = textStyle, modifier = textModifier)
            }
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

        var textRange by state { TextRange(0, 0) }

        TextField(
            modifier = textModifier,
            value = TextFieldValue(textProp.get(), textRange),
            onValueChange = { textProp.set(it.text); textRange = it.selection },
            onFocus = {
                textRange = TextRange(0, textProp.get().length)

                onFocus()
            },
            onBlur = {
                textRange = TextRange(0, 0)

                onBlur()
            },
            textStyle = textStyle
        )
    }
}

@Composable
fun SimpleVectorButton(vectorAsset: VectorAsset, text: String = "", onClick: () -> Unit) {
    Ripple(bounded = false) {
        Clickable(onClick = onClick) {
            Column {
                Container(
                    width = vectorAsset.defaultWidth,
                    height = vectorAsset.defaultHeight,
                    modifier = LayoutGravity.Center
                ) {
                    DrawBackground(color = MaterialTheme.colors().background)
                    DrawVector(vectorAsset)
                }

                if (text != "") {
                    Spacer(modifier = LayoutHeight(vectorAsset.defaultHeight / 8))
                    Text(text = text, modifier = LayoutGravity.Center)
                }
            }
        }
    }
}