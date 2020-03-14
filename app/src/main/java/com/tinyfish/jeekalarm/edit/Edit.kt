package com.tinyfish.jeekalarm.edit

import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.Recompose
import androidx.compose.state
import androidx.ui.core.Text
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TopAppBar
import androidx.ui.material.surface.Surface
import androidx.ui.res.vectorResource
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.tinyfish.jeekalarm.*
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleManager
import com.tinyfish.jeekalarm.ui.SimpleCheckbox
import com.tinyfish.jeekalarm.ui.SimpleTextField
import com.tinyfish.jeekalarm.ui.SimpleVectorButton
import com.tinyfish.jeekalarm.ui.Use
import java.util.*
import kotlin.reflect.KMutableProperty0

private lateinit var originalSchedule: Schedule
private lateinit var editingSchedule: Schedule

private lateinit var uiTimeConfigChanged: MutableState<Int>

@Composable
fun EditScreen(scheduleIndex: Int) {
    uiTimeConfigChanged = state { 0 }

    if (scheduleIndex == -1) {
        editingSchedule = Schedule()
    } else {
        originalSchedule = ScheduleManager.scheduleList[scheduleIndex]
        editingSchedule = originalSchedule.copy()
    }

    Column {
        TopBar()
        Surface(
            color = MaterialTheme.colors().background,
            modifier = LayoutFlexible(1f, true)
        ) {
            Editor()
        }
        BottomBar(scheduleIndex == -1)
    }
}

@Composable
private fun TopBar() {
    TopAppBar(
        title = { Text(text = "Edit Schedule") }
    )
}

@Composable
private fun Editor() {
    Column(LayoutPadding(20.dp)) {
        MyCheckbox("Enabled", editingSchedule::enabled)
        HeightSpacer()
        MyCheckbox("Only Once", editingSchedule::onlyOnce)

        HeightSpacer()
        MyTextField("Name: ", editingSchedule::name)
        Column {
            Use(uiTimeConfigChanged.value)
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
                        modifier = LayoutPadding(start = 20.dp)
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
    Surface(elevation = 2.dp, color = MaterialTheme.colors().background) {
        Container(modifier = LayoutHeight(100.dp), expanded = true) {
            Row(arrangement = Arrangement.Center) {
                SimpleVectorButton(vectorResource(R.drawable.ic_cancel), "Cancel") {
                    ScheduleManager.stopPlaying()
                    UI.screen.value = ScreenType.MAIN
                }

                WidthSpacer(36.dp)
                SimpleVectorButton(
                    vectorResource(if (isAdding) R.drawable.ic_add else R.drawable.ic_done),
                    if (isAdding) "Add" else "Save"
                ) {
                    if (isAdding) {
                        editingSchedule.timeConfigChanged()
                        ScheduleManager.scheduleList.add(editingSchedule)
                    } else {
                        editingSchedule.copyTo(originalSchedule)
                        originalSchedule.timeConfigChanged()
                    }
                    ScheduleManager.saveConfig()

                    ScheduleManager.stopPlaying()
                    UI.screen.value = ScreenType.MAIN
                }

                WidthSpacer(36.dp)
                SimpleVectorButton(vectorResource(R.drawable.ic_access_time), "Now") {
                    Calendar.getInstance().apply {
                        editingSchedule.minuteConfig = get(Calendar.MINUTE).toString()
                        editingSchedule.hourConfig = get(Calendar.HOUR).toString()
                        editingSchedule.dayConfig = get(Calendar.DAY_OF_MONTH).toString()
                        editingSchedule.monthConfig = (get(Calendar.MONTH) + 1).toString()
                        uiTimeConfigChanged.value++
                    }
                }

                WidthSpacer(36.dp)
                Recompose { recompose ->
                    val text = if (UI.isPlaying.value) "Stop" else "Play"
                    val onClick = {
                        if (UI.isPlaying.value)
                            ScheduleManager.stopPlaying()
                        else
                            editingSchedule.play()
                    }

                    if (UI.isPlaying.value)
                        SimpleVectorButton(vectorResource(R.drawable.ic_stop), text, onClick)
                    else
                        SimpleVectorButton(vectorResource(R.drawable.ic_play_arrow), text, onClick)
                }
            }
        }
    }
}
