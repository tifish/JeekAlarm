package com.tinyfish.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.reflect.KMutableProperty0

@Composable
fun Observe(body: @Composable () -> Unit) = body()

@Composable
fun SimpleSwitch(
    hint: String,
    booleanProp: KMutableProperty0<Boolean>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    textModifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val rowScope = currentRecomposeScope

        Switch(
            checked = booleanProp.get(),
            onCheckedChange = {
                booleanProp.set(it)
                onCheckedChange(it)
                rowScope.invalidate()
            }
        )

        WidthSpacer(15.dp)

        Text(
            hint,
            style = textStyle,
            modifier = textModifier.clickable(
                onClick = {
                    booleanProp.set(!booleanProp.get())
                    rowScope.invalidate()
                })
        )
    }
}

@Composable
fun SimpleSwitch(
    hint: String,
    value: Boolean,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    textModifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = value,
            onCheckedChange = {
                onCheckedChange(it)
            }
        )

        WidthSpacer(15.dp)

        Text(
            hint,
            style = textStyle,
            modifier = textModifier.clickable(
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
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = hint, modifier = Modifier.wrapContentSize(Alignment.Center))
        }
    }
}

@Composable
fun SimpleTextField(
    hint: String,
    textFieldValue: TextFieldValue,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    onTextFieldFocused: (Boolean) -> Unit = {},
    onTextChanged: (TextFieldValue) -> Unit = {}
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val focusRequester = remember { FocusRequester() }

        Text(hint, Modifier.clickable { focusRequester.requestFocus() })
        WidthSpacer()

        var lastFocusState by remember { mutableStateOf(false) }

        TextField(
            modifier = textFieldModifier
                .onFocusChanged { state ->
                    if (lastFocusState != state.hasFocus) {
                        onTextFieldFocused(state.hasFocus)
                    }
                    lastFocusState = state.hasFocus
                }
                .focusRequester(focusRequester),
            value = textFieldValue,
            onValueChange = {
                onTextChanged(it)
            },
            textStyle = textStyle,
        )
    }
}

@Composable
fun SimpleTextField(
    hint: String,
    value: String,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    onTextFieldFocused: (Boolean) -> Unit = {},
    onTextChanged: (String) -> Unit = {}
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val focusRequester = remember { FocusRequester() }

        Text(hint, Modifier.clickable { focusRequester.requestFocus() })
        WidthSpacer()

        var lastFocusState by remember { mutableStateOf(false) }

        TextField(
            modifier = textFieldModifier
                .onFocusChanged { state ->
                    if (lastFocusState != state.hasFocus) {
                        onTextFieldFocused(state.hasFocus)
                    }
                    lastFocusState = state.hasFocus
                }
                .focusRequester(focusRequester),
            value = value,
            onValueChange = {
                onTextChanged(it)
            },
            textStyle = textStyle,
        )
    }
}

@Composable
fun SimpleIntField(
    hint: String,
    textFieldValue: TextFieldValue,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    onTextFieldFocused: (Boolean) -> Unit = {},
    onTextChanged: (TextFieldValue) -> Unit = {}
) {
    Row {
        Text(hint)
        WidthSpacer()

        var lastFocusState by remember { mutableStateOf(false) }

        TextField(
            modifier = modifier.onFocusChanged { state ->
                if (lastFocusState != state.hasFocus) {
                    onTextFieldFocused(state.hasFocus)
                }
                lastFocusState = state.hasFocus
            },
            value = textFieldValue,
            onValueChange = {
                onTextChanged(it)
            },
            textStyle = textStyle,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun SimpleVectorButton(
    vectorAsset: ImageVector,
    text: String = "",
    onClick: () -> Unit
) {
    SimpleVectorButton(vectorAsset, Modifier, text, onClick)
}

@Composable
fun SimpleVectorButton(
    vectorAsset: ImageVector,
    modifier: Modifier = Modifier,
    text: String = "",
    onClick: () -> Unit
) {
    Column(
        modifier.clickable(onClick = onClick),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        Box {
            Icon(vectorAsset, null)
        }

        if (text != "") {
            Spacer(Modifier.height(vectorAsset.defaultHeight / 8))
            Text(text)
        }
    }
}

@Composable
fun SimpleImageButton(
    imageAsset: ImageBitmap,
    modifier: Modifier = Modifier,
    text: String = "",
    imageSize: Dp? = null,
    onClick: () -> Unit
) {
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageModifier =
            if (imageSize != null) Modifier.size(imageSize) else Modifier
        Image(imageAsset, null, imageModifier)

        if (text != "") {
            if (imageSize != null)
                Spacer(Modifier.height(2.dp))
            Text(text, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun SimpleTextButton(
    text: String,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        modifier
            .clickable(onClick = onClick)
            .size(width, height),
        shape = shape,
        color = backgroundColor
    ) {
        Text(text, Modifier.wrapContentSize(Alignment.Center))
    }
}
