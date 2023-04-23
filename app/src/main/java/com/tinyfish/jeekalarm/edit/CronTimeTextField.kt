package com.tinyfish.jeekalarm.edit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.ui.MyTextButton
import com.tinyfish.ui.Observe
import com.tinyfish.ui.SimpleTextField
import com.tinyfish.ui.WidthSpacer
import kotlin.reflect.KMutableProperty0

@Composable
fun CronTimeTextField(
    hint: String,
    textProp: KMutableProperty0<String>,
    isTimeConfig: Boolean = false,
    onChange: (String) -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        App.editTimeConfigChangedTrigger

        val cronTimeScope = currentRecomposeScope

        var focusedState by remember { mutableStateOf(false) }
        var textRange by remember { mutableStateOf(TextRange(textProp.get().length)) }
        var keepWholeSelection by remember { mutableStateOf(false) }
        if (keepWholeSelection) {
            // in case onValueChange was not called immediately after onFocusChanged
            // the selection will be transferred correctly, so we don't need to redefine it anymore
            SideEffect {
                keepWholeSelection = false
            }
        }

        Observe {
            val textModifier = if (isTimeConfig)
                Modifier.width(160.dp)
            else
                Modifier.weight(1f, true)

            SimpleTextField(
                hint = hint,
                textFieldValue = TextFieldValue(textProp.get(), textRange),
                onTextFieldFocused = { focused ->
                    if (focusedState != focused) {
                        focusedState = focused
                        if (focused) {
                            textRange = TextRange(0, textProp.get().length)
                            keepWholeSelection = true
                        }
                    }
                },
                textModifier = textModifier,
                textStyle = TextStyle(fontSize = (20.sp)),
                modifier = Modifier.weight(1f, true),
                onTextChanged = {
                    if (textProp.get() != it.text) {
                        textProp.set(it.text)
                        onChange(it.text)
                    }

                    if (keepWholeSelection) {
                        keepWholeSelection = false
                        textRange = TextRange(0, it.text.length)
                    } else {
                        textRange = it.selection
                    }
                }
            )
        }

        if (isTimeConfig && focusedState) {
            MyTextButton("*") {
                if (textProp.get() != "*") {
                    textProp.set("*")
                    textRange = TextRange(1, 1)
                    onChange("*")
                    cronTimeScope.invalidate()
                }
            }

            WidthSpacer()

            MyTextButton("0") {
                if (textProp.get() != "0") {
                    textProp.set("0")
                    textRange = TextRange(1, 1)
                    onChange("0")
                    cronTimeScope.invalidate()
                }
            }

            WidthSpacer()

            MyTextButton("1-3") {
                if (textProp.get() != "1-3") {
                    textProp.set("1-3")
                    textRange = TextRange(3, 3)
                    onChange("0")
                    onChange("1-3")
                    cronTimeScope.invalidate()
                }
            }

            WidthSpacer()

            MyTextButton("1,3") {
                if (textProp.get() != "1,3") {
                    textProp.set("1,3")
                    textRange = TextRange(3, 3)
                    onChange("1,3")
                    cronTimeScope.invalidate()
                }
            }

            WidthSpacer()

            MyTextButton("*/3") {
                if (textProp.get() != "*/3") {
                    textProp.set("*/3")
                    textRange = TextRange(3, 3)
                    onChange("*/3")
                    cronTimeScope.invalidate()
                }
            }
        }
    }
}
