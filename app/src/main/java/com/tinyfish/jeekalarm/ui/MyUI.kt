package com.tinyfish.jeekalarm.ui

import androidx.annotation.DrawableRes
import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.state
import androidx.ui.core.Text
import androidx.ui.foundation.Icon
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.layout.*
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TopAppBar
import androidx.ui.material.surface.Surface
import androidx.ui.res.vectorResource
import androidx.ui.text.TextStyle
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.tinyfish.jeekalarm.R
import kotlin.reflect.KMutableProperty0

enum class ScreenType {
    MAIN,
    EDIT,
    SETTINGS,
    NOTIFICATION,
}

@Composable
fun HeightSpacer(height: Dp = 10.dp) {
    Spacer(LayoutHeight(height))
}

@Composable
fun WidthSpacer(width: Dp = 10.dp) {
    Spacer(LayoutWidth(width))
}

@Composable
fun MyFileSelect(hint: String, text: String, onSelect: () -> Unit, onClear: () -> Unit) {
    Row {
        Text(hint, LayoutWidth(100.dp))
        Text(text, LayoutFlexible(1f, true))

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
fun MyCheckbox(
    hint: String,
    booleanProp: KMutableProperty0<Boolean>,
    textStyle: TextStyle? = null,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    SimpleCheckbox(
        hint = hint,
        booleanProp = booleanProp,
        textStyle = textStyle,
        onCheckedChange = onCheckedChange,
        textModifier = LayoutHeight(36.dp) + LayoutWidth(200.dp)
    )
}

@Composable
fun MyCheckbox(
    hint: String,
    value: Boolean,
    textStyle: TextStyle? = null,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    SimpleCheckbox(
        hint = hint,
        value = value,
        textStyle = textStyle,
        onCheckedChange = onCheckedChange,
        textModifier = LayoutHeight(36.dp) + LayoutWidth(200.dp)
    )
}

@Composable
fun MyCronTimeTextField(
    hint: String,
    textProp: KMutableProperty0<String>,
    isTimeConfig: Boolean = false
) {
    Row(LayoutHeight(36.dp)) {
        val focusedState = state { false }

        Recompose { recompose ->
            SimpleTextField(
                hint = hint,
                textProp = textProp,
                hintModifier = LayoutWidth(80.dp),
                onFocus = { focusedState.value = true },
                onBlur = { focusedState.value = false },
                textModifier = LayoutWidth(160.dp),
                textStyle = TextStyle(fontSize = (20.sp)),
                modifier = LayoutFlexible(1f, true)
            )

            if (isTimeConfig && focusedState.value) {
                MyTextButton("*") {
                    textProp.set("*")
                    recompose()
                }

                WidthSpacer()

                MyTextButton("0") {
                    textProp.set("0")
                    recompose()
                }

                WidthSpacer()

                MyTextButton("1-3") {
                    textProp.set("1-3")
                    recompose()
                }

                WidthSpacer()

                MyTextButton("1,3") {
                    textProp.set("1,3")
                    recompose()
                }

                WidthSpacer()

                MyTextButton("*/3") {
                    textProp.set("*/3")
                    recompose()
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
fun MyTopBar(@DrawableRes iconID: Int, title: String) {
    TopAppBar(
        title = {
            Row {
                Icon(vectorResource(iconID), LayoutGravity.Center)
                WidthSpacer()
                Text(title)
            }
        }
    )
}

@Composable
fun MyBottomBar(buttons: @Composable() () -> Unit) {
    Surface(elevation = 2.dp, color = MaterialTheme.colors().background) {
        Container(modifier = LayoutHeight(100.dp), expanded = true) {
            Row(arrangement = Arrangement.Center) {
                buttons()
            }
        }
    }
}
