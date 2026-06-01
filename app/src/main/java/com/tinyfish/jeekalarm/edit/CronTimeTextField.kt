package com.tinyfish.jeekalarm.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val cronPresets = listOf("*", "0", "1-3", "1,3", "*/3")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CronTimeField(
    label: String,
    text: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    var textRange by remember { mutableStateOf(TextRange(text.length)) }
    var keepWholeSelection by remember { mutableStateOf(false) }
    if (keepWholeSelection) {
        // 聚焦后让 onValueChange 把全选状态正确传递一次即可
        SideEffect { keepWholeSelection = false }
    }

    // 外部（如"现在"/"按名字猜测"）可能改变 text，需把选区限制在合法范围内
    val safeRange = TextRange(
        textRange.start.coerceIn(0, text.length),
        textRange.end.coerceIn(0, text.length),
    )

    Column(modifier.fillMaxWidth()) {
        // chip 放在输入框上方：聚焦后字段会被顶到键盘上方，此时 chip 仍可见
        AnimatedVisibility(visible = focused) {
            FlowRow(
                Modifier.padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                cronPresets.forEach { preset ->
                    SuggestionChip(
                        onClick = {
                            if (text != preset) {
                                onChange(preset)
                                textRange = TextRange(preset.length)
                            }
                        },
                        label = { Text(preset) },
                    )
                }
            }
        }

        OutlinedTextField(
            value = TextFieldValue(text, safeRange),
            onValueChange = {
                if (text != it.text)
                    onChange(it.text)

                textRange = if (keepWholeSelection) {
                    keepWholeSelection = false
                    TextRange(0, it.text.length)
                } else {
                    it.selection
                }
            },
            label = { Text(label) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (focused != state.isFocused) {
                        focused = state.isFocused
                        if (state.isFocused) {
                            textRange = TextRange(0, text.length)
                            keepWholeSelection = true
                        }
                    }
                },
        )
    }
}
