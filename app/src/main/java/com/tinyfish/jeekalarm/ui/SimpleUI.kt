package com.tinyfish.jeekalarm.ui

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.*
import androidx.ui.graphics.Color
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.Shape
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.layout.*
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.material.Switch
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import kotlin.reflect.KMutableProperty0

@Composable
fun SimpleSwitch(
    hint: String,
    booleanProp: KMutableProperty0<Boolean>,
    textStyle: TextStyle = TextStyle.Default,
    onCheckedChange: (Boolean) -> Unit = {},
    textModifier: Modifier = Modifier.Companion
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

            Spacer(Modifier.preferredWidth(10.dp))

            Text(
                hint, style = textStyle, modifier = textModifier.clickable(
                    indication = null,
                    onClick = {
                        booleanProp.set(!booleanProp.get())
                        recompose()
                    })
            )
        }
    }
}

@Composable
fun SimpleSwitch(
    hint: String,
    value: Boolean,
    textStyle: TextStyle = TextStyle.Default,
    onCheckedChange: (Boolean) -> Unit = {},
    textModifier: Modifier = Modifier.Companion
) {
    Row {
        Switch(
            checked = value,
            onCheckedChange = {
                onCheckedChange(it)
            }
        )

        Spacer(Modifier.preferredWidth(10.dp))

        Text(
            hint, style = textStyle, modifier = textModifier.clickable(
                indication = null,
                onClick = {
                    onCheckedChange(!value)
                })
        )
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
            Spacer(modifier = Modifier.preferredHeight(3.dp))
            Text(text = hint, modifier = Modifier.wrapContentSize(Alignment.Center))
        }
    }
}

@Composable
fun SimpleTextField(
    hint: String,
    textProp: KMutableProperty0<String>,
    onFocus: () -> Unit = {},
    onBlur: () -> Unit = {},
    modifier: Modifier = Modifier.Companion,
    hintModifier: Modifier = Modifier.Companion,
    hintStyle: TextStyle = TextStyle.Default,
    textModifier: Modifier = Modifier.Companion,
    textStyle: TextStyle = TextStyle.Default
) {
    Row(modifier) {
        Text(hint, hintModifier, style = hintStyle)

        val textRange = state { TextRange(0, 0) }

        Recompose { recompose ->
            TextField(
                modifier = textModifier,
                value = TextFieldValue(textProp.get(), textRange.value),
                onValueChange = {
                    textProp.set(it.text)
                    textRange.value = it.selection
                    recompose()
                },
                onFocusChange = {
                    if (it)
                        onFocus()
                    else
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
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.gravity(Alignment.CenterHorizontally)
                .preferredSize(vectorAsset.defaultWidth, vectorAsset.defaultHeight)
        ) {
            Icon(vectorAsset)
        }

        if (text != "") {
            Spacer(Modifier.preferredHeight(vectorAsset.defaultHeight / 8))
            Text(text, Modifier.gravity(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun SimpleImageButton(
    imageAsset: ImageAsset,
    text: String = "",
    imageSize: Dp? = null,
    onClick: () -> Unit
) {
    Column(
        Modifier.clickable(onClick = onClick),
        horizontalGravity = Alignment.CenterHorizontally
    ) {
        val imageModifier =
            if (imageSize != null) Modifier.preferredSize(imageSize) else Modifier
        Image(imageAsset, imageModifier)

        if (text != "") {
            if (imageSize != null)
                Spacer(Modifier.preferredHeight(2.dp))
            Text(text, Modifier.gravity(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun SimpleTextButton(
    text: String = "",
    width: Dp,
    height: Dp,
    shape: Shape = MaterialTheme.shapes.small,
    backgroundColor: Color = MaterialTheme.colors.primary,
    onClick: () -> Unit
) {
    Surface(
        Modifier.clickable(onClick = onClick).preferredSize(width, height),
        shape = shape,
        color = backgroundColor
    ) {
        Text(text, Modifier.wrapContentSize(Alignment.Center))
    }
}
