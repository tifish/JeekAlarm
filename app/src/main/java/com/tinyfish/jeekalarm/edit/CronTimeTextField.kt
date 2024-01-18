package com.tinyfish.jeekalarm.edit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.sp
import com.tinyfish.ui.MyTextButton
import com.tinyfish.ui.Observe
import com.tinyfish.ui.SimpleTextField
import com.tinyfish.ui.WidthSpacer

@Composable
fun CronTimeTextField(
    hint: String,
    text: String,
    onChange: (String) -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        val cronTimeScope = currentRecomposeScope

        var focusedState by remember { mutableStateOf(false) }
        var textRange by remember { mutableStateOf(TextRange(text.length)) }
        var keepWholeSelection by remember { mutableStateOf(false) }
        if (keepWholeSelection) {
            // in case onValueChange was not called immediately after onFocusChanged
            // the selection will be transferred correctly, so we don't need to redefine it anymore
            SideEffect {
                keepWholeSelection = false
            }
        }

        Observe {
            SimpleTextField(
                hint = hint,
                textFieldValue = TextFieldValue(text, textRange),
                onTextFieldFocused = { focused ->
                    if (focusedState != focused) {
                        focusedState = focused
                        if (focused) {
                            textRange = TextRange(0, text.length)
                            keepWholeSelection = true
                        }
                    }
                },
                textFieldModifier = Modifier.weight(1f, true),
                textStyle = TextStyle(fontSize = (20.sp)),
                modifier = Modifier.weight(1f, true),
                onTextChanged = {
                    if (text != it.text) {
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

        if (focusedState) {
            MyTextButton("*") {
                if (text != "*") {
                    onChange("*")
                    textRange = TextRange(1, 1)
                    cronTimeScope.invalidate()
                }
            }

            WidthSpacer()

            MyTextButton("0") {
                if (text != "0") {
                    textRange = TextRange(1, 1)
                    onChange("0")
                    cronTimeScope.invalidate()
                }
            }

            WidthSpacer()

            MyTextButton("1-3") {
                if (text != "1-3") {
                    textRange = TextRange(3, 3)
                    onChange("0")
                    onChange("1-3")
                    cronTimeScope.invalidate()
                }
            }

            WidthSpacer()

            MyTextButton("1,3") {
                if (text != "1,3") {
                    textRange = TextRange(3, 3)
                    onChange("1,3")
                    cronTimeScope.invalidate()
                }
            }

            WidthSpacer()

            MyTextButton("*/3") {
                if (text != "*/3") {
                    textRange = TextRange(3, 3)
                    onChange("*/3")
                    cronTimeScope.invalidate()
                }
            }
        }
    }
}
