package com.tinyfish.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Icon
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.state
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinyfish.jeekalarm.R
import kotlin.reflect.KMutableProperty0

@Composable
fun HeightSpacer(height: Dp = 10.dp) {
    Spacer(Modifier.preferredHeight(height))
}

@Composable
fun WidthSpacer(width: Dp = 10.dp) {
    Spacer(Modifier.preferredWidth(width))
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
            vectorResource(R.drawable.ic_location_searching),
            "Select",
            onClick = onSelect
        )
        WidthSpacer()
        SimpleVectorButton(
            vectorResource(R.drawable.ic_clear),
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
    onChange: () -> Unit = {}
) {
    Row {
        val focusedState = remember { mutableStateOf(false) }

        Recompose { recompose ->
            SimpleTextField(
                hint = hint,
                textProp = textProp,
                onFocus = { focusedState.value = true },
                onBlur = { focusedState.value = false },
                textModifier = Modifier.preferredWidth(160.dp),
                textStyle = TextStyle(fontSize = (20.sp)),
                modifier = Modifier.weight(1f, true),
                onChange = onChange
            )

            if (isTimeConfig && focusedState.value) {
                MyTextButton("*") {
                    if (textProp.get() != "*") {
                        textProp.set("*")
                        onChange()
                        recompose()
                    }
                }

                WidthSpacer()

                MyTextButton("0") {
                    if (textProp.get() != "0") {
                        textProp.set("0")
                        onChange()
                        recompose()
                    }
                }

                WidthSpacer()

                MyTextButton("1-3") {
                    if (textProp.get() != "1-3") {
                        textProp.set("1-3")
                        onChange()
                        recompose()
                    }
                }

                WidthSpacer()

                MyTextButton("1,3") {
                    if (textProp.get() != "1,3") {
                        textProp.set("1,3")
                        onChange()
                        recompose()
                    }
                }

                WidthSpacer()

                MyTextButton("*/3") {
                    if (textProp.get() != "*/3") {
                        textProp.set("*/3")
                        onChange()
                        recompose()
                    }
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
fun MyTopBar(title: @Composable() () -> Unit) {
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
                Icon(vectorResource(iconID), Modifier.gravity(Alignment.CenterVertically))
                WidthSpacer()
                Text(title)
            }
        },
        backgroundColor = MaterialTheme.colors.primary
    )
}

@Composable
fun MyBottomBar(buttons: @Composable() () -> Unit) {
    Surface(Modifier.fillMaxWidth(), elevation = 2.dp, color = MaterialTheme.colors.background) {
        Row(Modifier.preferredHeight(80.dp), Arrangement.Center, Alignment.CenterVertically) {
            buttons()
        }
    }
}
