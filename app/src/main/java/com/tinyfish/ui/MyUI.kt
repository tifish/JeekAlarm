package com.tinyfish.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinyfish.jeekalarm.R
import kotlin.reflect.KMutableProperty0

@Composable
fun HeightSpacer(height: Dp = 10.dp) {
    Spacer(Modifier.height(height))
}

@Composable
fun WidthSpacer(width: Dp = 10.dp) {
    Spacer(Modifier.width(width))
}

@Composable
fun ToolButtonWidthSpacer() {
    WidthSpacer(36.dp)
}

@Composable
fun MyFileSelect(hint: String, text: String, onSelect: () -> Unit, onClear: () -> Unit) {
    Row {
        Text(hint)
        WidthSpacer()
        Text(text, Modifier.weight(1f, true))

        SimpleVectorButton(
            ImageVector.vectorResource(R.drawable.ic_location_searching),
            "Select",
            onClick = onSelect
        )
        WidthSpacer()
        SimpleVectorButton(
            ImageVector.vectorResource(R.drawable.ic_clear),
            "Clear",
            onClick = onClear
        )
    }
}

@Composable
fun MySwitch(
    hint: String,
    booleanProp: KMutableProperty0<Boolean>,
    textStyle: TextStyle = TextStyle.Default,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    SimpleSwitch(
        hint = hint,
        booleanProp = booleanProp,
        textStyle = textStyle,
        onCheckedChange = onCheckedChange,
        textModifier = Modifier.size(200.dp, 36.dp)
    )
}

@Composable
fun MySwitch(
    hint: String,
    value: Boolean,
    textStyle: TextStyle = TextStyle.Default,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    SimpleSwitch(
        hint = hint,
        value = value,
        textStyle = textStyle,
        onCheckedChange = onCheckedChange,
        textModifier = Modifier.size(200.dp, 36.dp)
    )
}

@Composable
fun MyCronTimeTextField(
    hint: String,
    textProp: KMutableProperty0<String>,
    isTimeConfig: Boolean = false,
    onChange: (String) -> Unit = {}
) {
    Row {
        val focusedState = remember { mutableStateOf(false) }
        val textValue = remember { mutableStateOf(TextFieldValue(textProp.get())) }

        SimpleTextField(
            hint = hint,
            textFieldValue = textValue.value,
            onTextFieldFocused = { focused ->
                focusedState.value = focused
            },
            textModifier = Modifier.width(160.dp),
            textStyle = TextStyle(fontSize = (20.sp)),
            modifier = Modifier.weight(1f, true),
            onTextChanged = {
                if (textValue.value.text != it.text) {
                    textProp.set(it.text)
                    onChange(it.text)
                }
                textValue.value = it
            }
        )

        if (isTimeConfig && focusedState.value) {
            MyTextButton("*") {
                if (textProp.get() != "*") {
                    textProp.set("*")
                    textValue.value = TextFieldValue("*", TextRange(0, 1))
                    onChange("*")
                }
            }

            WidthSpacer()

            MyTextButton("0") {
                if (textProp.get() != "0") {
                    textProp.set("0")
                    textValue.value = TextFieldValue("0", TextRange(0, 1))
                    onChange("0")
                }
            }

            WidthSpacer()

            MyTextButton("1-3") {
                if (textProp.get() != "1-3") {
                    textProp.set("1-3")
                    textValue.value = TextFieldValue("1-3", TextRange(3, 3))
                    onChange("0")
                    onChange("1-3")
                }
            }

            WidthSpacer()

            MyTextButton("1,3") {
                if (textProp.get() != "1,3") {
                    textProp.set("1,3")
                    textValue.value = TextFieldValue("1,3", TextRange(3, 3))
                    onChange("1,3")
                }
            }

            WidthSpacer()

            MyTextButton("*/3") {
                if (textProp.get() != "*/3") {
                    textProp.set("*/3")
                    textValue.value = TextFieldValue("*/3", TextRange(3, 3))
                    onChange("*/3")
                }
            }
        }
    }
}

@Composable
fun MyTextButton(
    text: String = "",
    onClick: () -> Unit
) {
    SimpleTextButton(
        text = text,
        width = 30.dp,
        height = 30.dp,
        shape = CircleShape,
        onClick = onClick
    )
}

@Composable
fun MyTopBar(title: @Composable () -> Unit) {
    TopAppBar(
        title = title,
        backgroundColor = MaterialTheme.colors.primary
    )
}

@Composable
fun MyTopBar(@DrawableRes iconID: Int, title: String) {
    TopAppBar(
        title = {
            Row {
                Icon(ImageVector.vectorResource(iconID), null, Modifier.align(Alignment.CenterVertically))
                WidthSpacer()
                Text(title)
            }
        },
        backgroundColor = MaterialTheme.colors.primary
    )
}

@Composable
fun MyBottomBar(buttons: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), elevation = 2.dp, color = MaterialTheme.colors.background) {
        Row(Modifier.height(80.dp), Arrangement.Center, Alignment.CenterVertically) {
            buttons()
        }
    }
}
