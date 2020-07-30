package com.tinyfish.ui

import androidx.compose.Composable
import androidx.compose.invalidate
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.*
import androidx.ui.graphics.Color
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.Shape
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.input.TextFieldValue
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
fun Recompose(body: @Composable() (recompose: () -> Unit) -> Unit) = body(invalidate)

@Composable
fun Observe(body: @Composable() () -> Unit) = body()

@Composable
fun SimpleSwitch(
    hint: String,
    booleanProp: KMutableProperty0<Boolean>,
    textStyle: TextStyle = TextStyle.Default,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Row(modifier) {
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
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Row(modifier) {
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
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Column(modifier) {
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
    modifier: Modifier = Modifier,
    hintModifier: Modifier = Modifier,
    hintStyle: TextStyle = TextStyle.Default,
    textModifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    onChange: () -> Unit = {}
) {
    Row(modifier) {
        Text(hint, hintModifier, style = hintStyle)

        val textRange = state { TextRange(0, 0) }

        Recompose { recompose ->
            TextField(
                modifier = textModifier,
                value = TextFieldValue(textProp.get(), textRange.value),
                onValueChange = {
                    if (textProp.get() != it.text) {
                        textProp.set(it.text)
                        onChange()
                    }
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
    SimpleVectorButton(vectorAsset, text, Modifier, onClick)
}

@Composable
fun SimpleVectorButton(
    vectorAsset: VectorAsset,
    text: String = "",
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier.clickable(onClick = onClick), Arrangement.Center, Alignment.CenterHorizontally) {
        Box {
            Icon(vectorAsset)
        }

        if (text != "") {
            Spacer(Modifier.preferredHeight(vectorAsset.defaultHeight / 8))
            Text(text)
        }
    }
}

@Composable
fun SimpleImageButton(
    imageAsset: ImageAsset,
    text: String = "",
    imageSize: Dp? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.clickable(onClick = onClick),
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier.clickable(onClick = onClick).preferredSize(width, height),
        shape = shape,
        color = backgroundColor
    ) {
        Text(text, Modifier.wrapContentSize(Alignment.Center))
    }
}
