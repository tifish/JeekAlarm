package com.tinyfish.jeekalarm.ui

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.state
import androidx.ui.core.Text
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.material.Button
import androidx.ui.res.vectorResource
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.WidthSpacer
import kotlin.reflect.KMutableProperty0

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
    text: String,
    booleanProp: KMutableProperty0<Boolean>,
    textStyle: TextStyle? = null,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    SimpleCheckbox(
        text = text,
        booleanProp = booleanProp,
        textStyle = textStyle,
        onCheckedChange = onCheckedChange,
        textModifier = LayoutHeight(36.dp) + LayoutWidth(200.dp)
    )
}

@Composable
fun MyTextField(
    hint: String,
    textProp: KMutableProperty0<String>,
    isTimeConfig: Boolean = false
) {
    Container(LayoutHeight(36.dp)) {
        Row {
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
                    Button(onClick = {
                        textProp.set("*")
                        recompose()
                    }) {
                        Text("*")
                    }

                    WidthSpacer()

                    Button(onClick = {
                        textProp.set("0")
                        recompose()
                    }) {
                        Text("0")
                    }
                }
            }
        }
    }
}
