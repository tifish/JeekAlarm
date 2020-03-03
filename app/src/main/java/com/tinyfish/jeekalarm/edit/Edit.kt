package com.tinyfish.jeekalarm.edit

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.Text
import androidx.ui.layout.*
import androidx.ui.material.BottomAppBar
import androidx.ui.material.Button
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar
import androidx.ui.text.TextStyle
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.tinyfish.jeekalarm.HeightSpacer
import com.tinyfish.jeekalarm.ScreenType
import com.tinyfish.jeekalarm.UI
import com.tinyfish.jeekalarm.WidthSpacer
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleManager
import com.tinyfish.jeekalarm.ui.SimpleCheckbox
import com.tinyfish.jeekalarm.ui.SimpleTextField
import java.util.*
import kotlin.reflect.KMutableProperty0

private lateinit var originalSchedule: Schedule
private lateinit var editingSchedule: Schedule

private var uiTimeConfigChanged by state { 0 }

@Preview
@Composable
private fun EditPreview() {
    EditScreen(-1)
}

@Composable
fun EditScreen(scheduleIndex: Int) {
    if (scheduleIndex == -1) {
        editingSchedule = Schedule()
    } else {
        originalSchedule = ScheduleManager.scheduleList[scheduleIndex]
        editingSchedule = originalSchedule.copy()
    }

    Scaffold(
        topAppBar = { TopBar() },
        bodyContent = { Editor() },
        bottomAppBar = { BottomBar(scheduleIndex == -1) }
    )
}

@Composable
private fun TopBar() {
    TopAppBar(
        title = { Text(text = "Edit Schedule") }
    )
}

class BarButtonData(
    var text: String,
    var onClick: () -> Unit
)

@Composable
private fun Editor() {
    Column {
        MyCheckbox("Enabled", editingSchedule::enabled)
        HeightSpacer()
        MyCheckbox("Only Once", editingSchedule::onlyOnce)

        HeightSpacer()
        MyTextField("Name: ", editingSchedule::name)
        Column {
            remember { uiTimeConfigChanged }
            MyTextField("Minute: ", editingSchedule::minuteConfig, true)
            MyTextField("Hour: ", editingSchedule::hourConfig, true)
            MyTextField("Day: ", editingSchedule::dayConfig, true)
            MyTextField("Month: ", editingSchedule::monthConfig, true)
            MyTextField("WeekDay: ", editingSchedule::weekDayConfig, true)
        }

        Recompose { recompose ->
            HeightSpacer()
            MyCheckbox(
                text = "Play Music:",
                booleanProp = editingSchedule::playMusic,
                onCheckedChange = { recompose() })

            if (editingSchedule.playMusic) {
                HeightSpacer()
                Recompose { recomposeFileSelect ->
                    FileSelect("Music File:", editingSchedule.musicFile,
                        onSelect = {
                            FileSelector.openMusicFile {
                                editingSchedule.musicFile = it?.path?.substringAfter(':')!!
                                recomposeFileSelect()
                            }
                        },
                        onClear = {
                            editingSchedule.musicFile = ""
                            recomposeFileSelect()
                        }
                    )
                }

                HeightSpacer()
                Recompose { recomposeFileSelect ->
                    FileSelect("Music Folder:", editingSchedule.musicFolder,
                        onSelect = {
                            FileSelector.openFolder {
                                editingSchedule.musicFolder = it?.path?.substringAfter(':')!!
                                recomposeFileSelect()
                            }
                        },
                        onClear = {
                            editingSchedule.musicFolder = ""
                            recomposeFileSelect()
                        }
                    )
                }
            }

            Recompose { recomposeVibration ->
                HeightSpacer()
                MyCheckbox(
                    "Vibration",
                    editingSchedule::vibration,
                    onCheckedChange = { recomposeVibration() })

                if (editingSchedule.vibration) {
                    HeightSpacer()
                    Text(
                        editingSchedule.vibrationCount.toString(),
                        modifier = LayoutPadding(left = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FileSelect(hint: String, text: String, onSelect: () -> Unit, onClear: () -> Unit) {
    Row {
        Text(hint, LayoutWidth(100.dp))
        Text(text, LayoutFlexible(1f, true))

        Button(onClick = onSelect) {
            Text("Select")
        }
        WidthSpacer()
        Button(onClick = onClear) {
            Text("Clear")
        }
    }
}

@Composable
private fun MyCheckbox(
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
private fun MyTextField(
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

@Composable
private fun BottomBar(isAdding: Boolean) {
    BottomAppBar(
        navigationIcon = {
            Button(onClick = {
                if (isAdding) {
                    editingSchedule.timeConfigChanged()
                    ScheduleManager.scheduleList.add(editingSchedule)
                } else {
                    editingSchedule.copyTo(originalSchedule)
                    originalSchedule.timeConfigChanged()
                }
                ScheduleManager.saveConfig()

                ScheduleManager.stopPlaying()
                UI.screen = ScreenType.MAIN
            }) {
                Text(if (isAdding) "Add" else "Save")
            }
        },
        actionData = listOf(
            BarButtonData("Cancel", onClick = {
                ScheduleManager.stopPlaying()
                UI.screen = ScreenType.MAIN
            }),
            BarButtonData("Now", onClick = {
                Calendar.getInstance().apply {
                    editingSchedule.minuteConfig = get(Calendar.MINUTE).toString()
                    editingSchedule.hourConfig = get(Calendar.HOUR).toString()
                    editingSchedule.dayConfig = get(Calendar.DAY_OF_MONTH).toString()
                    editingSchedule.monthConfig = (get(Calendar.MONTH) + 1).toString()
                    uiTimeConfigChanged++
                }
            }),
            BarButtonData(if (UI.isPlaying) "Stop" else "Play", onClick = {
                if (UI.isPlaying)
                    ScheduleManager.stopPlaying()
                else
                    editingSchedule.play()
            })
        )
    ) { buttonData ->
        Row {
            WidthSpacer()
            Button(onClick = buttonData.onClick) {
                Text(buttonData.text)
            }
        }
    }
}
