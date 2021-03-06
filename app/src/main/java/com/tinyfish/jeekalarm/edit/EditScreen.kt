package com.tinyfish.jeekalarm.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType
import com.tinyfish.ui.*
import java.util.*

private lateinit var editingSchedule: Schedule
private var isAdding = false
private lateinit var uiTimeConfigChanged: MutableState<Int>

@Composable
fun EditScreen() {
    uiTimeConfigChanged = remember { mutableStateOf(0) }

    isAdding = App.editScheduleIndex == -1
    editingSchedule =
        if (isAdding)
            Schedule()
        else
            ScheduleHome.scheduleList[App.editScheduleIndex]


    Column {
        MyTopBar(
            R.drawable.ic_edit,
            if (isAdding) "Add" else "Edit"
        )
        Surface(
            color = MaterialTheme.colors.background,
            modifier = Modifier.weight(1f, true)
        ) {
            Editor()
        }
        BottomBar()
    }
}

@Composable
private fun Editor() {
    Column(
        Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())) {
        Observe {
            App.editEnabledChangeTrigger
            MySwitch("Enabled", editingSchedule::enabled)
        }
        HeightSpacer()
        MySwitch("Only Once", editingSchedule::onlyOnce)

        val onChange = { _: String ->
            if (!editingSchedule.enabled) {
                editingSchedule.enabled = true
                App.editEnabledChangeTrigger++
            }
        }

        HeightSpacer()
        MyCronTimeTextField(
            "Name: ",
            editingSchedule::name,
            false,
            onChange
        )
        Column(Modifier.padding(start = 20.dp)) {
            uiTimeConfigChanged.value
            HeightSpacer()
            MyCronTimeTextField(
                "Minute: ",
                editingSchedule::minuteConfig,
                true,
                onChange
            )
            HeightSpacer()
            MyCronTimeTextField(
                "Hour: ",
                editingSchedule::hourConfig,
                true,
                onChange
            )
            HeightSpacer()
            MyCronTimeTextField(
                "Day: ",
                editingSchedule::dayConfig,
                true,
                onChange
            )
            HeightSpacer()
            MyCronTimeTextField(
                "Month: ",
                editingSchedule::monthConfig,
                true,
                onChange
            )
            HeightSpacer()
            MyCronTimeTextField(
                "WeekDay: ",
                editingSchedule::weekDayConfig,
                true,
                onChange
            )
        }

        Observe {
            val playScope = currentRecomposeScope
            HeightSpacer()
            MySwitch(
                hint = "Play Music:",
                booleanProp = editingSchedule::playMusic,
                onCheckedChange = { playScope.invalidate() })

            if (editingSchedule.playMusic) {
                Column(Modifier.padding(start = 20.dp)) {
                    HeightSpacer()
                    Observe {
                        val fileSelectScope = currentRecomposeScope
                        MyFileSelect("Music File:",
                            editingSchedule.musicFile,
                            onSelect = {
                                FileSelector.openMusicFile {
                                    editingSchedule.musicFile = it?.path?.substringAfter(':')!!
                                    fileSelectScope.invalidate()
                                }
                            },
                            onClear = {
                                editingSchedule.musicFile = ""
                                fileSelectScope.invalidate()
                            }
                        )
                    }

                    HeightSpacer()
                    Observe {
                        val fileSelectScope = currentRecomposeScope
                        MyFileSelect("Music Folder:",
                            editingSchedule.musicFolder,
                            onSelect = {
                                FileSelector.openFolder {
                                    editingSchedule.musicFolder =
                                        it?.path?.substringAfter(':')!!
                                    fileSelectScope.invalidate()
                                }
                            },
                            onClear = {
                                editingSchedule.musicFolder = ""
                                fileSelectScope.invalidate()
                            }
                        )
                    }
                }
            }

            Observe {
                val vibrationScope = currentRecomposeScope
                HeightSpacer()
                MySwitch(
                    "Vibration",
                    editingSchedule::vibration,
                    onCheckedChange = { vibrationScope.invalidate() })

                if (editingSchedule.vibration) {
                    HeightSpacer()
                    Text(
                        editingSchedule.vibrationCount.toString(),
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomBar() {
    MyBottomBar {
        SimpleVectorButton(
            ImageVector.vectorResource(R.drawable.ic_back),
            if (isAdding) "Add" else "Back"
        ) {
            onEditScreenPressBack()
        }

        if (isAdding) {
            ToolButtonWidthSpacer()
            SimpleVectorButton(
                ImageVector.vectorResource(R.drawable.ic_cancel),
                "Cancel"
            ) {
                App.screen = ScreenType.MAIN
            }
        }

        ToolButtonWidthSpacer()
        SimpleVectorButton(
            ImageVector.vectorResource(R.drawable.ic_access_time),
            "Now"
        ) {
            Calendar.getInstance().apply {
                editingSchedule.minuteConfig = get(Calendar.MINUTE).toString()
                editingSchedule.hourConfig = get(Calendar.HOUR_OF_DAY).toString()
                editingSchedule.dayConfig = get(Calendar.DAY_OF_MONTH).toString()
                editingSchedule.monthConfig = (get(Calendar.MONTH) + 1).toString()
                uiTimeConfigChanged.value++
            }
        }

        ToolButtonWidthSpacer()
        Observe {
            val text = if (App.isPlaying) "Stop" else "Play"
            val onClick = {
                if (App.isPlaying)
                    ScheduleHome.stopPlaying()
                else
                    editingSchedule.play()
            }

            if (App.isPlaying)
                SimpleVectorButton(
                    ImageVector.vectorResource(R.drawable.ic_stop),
                    text,
                    onClick
                )
            else
                SimpleVectorButton(
                    ImageVector.vectorResource(R.drawable.ic_play_arrow),
                    text,
                    onClick
                )
        }
    }
}

fun onEditScreenPressBack() {
    if (isAdding)
        ScheduleHome.scheduleList.add(editingSchedule)
    editingSchedule.timeConfigChanged()
    ScheduleHome.saveConfig()

    ScheduleHome.stopPlaying()
    App.screen = ScreenType.MAIN
}
