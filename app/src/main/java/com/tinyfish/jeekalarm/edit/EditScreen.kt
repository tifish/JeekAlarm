package com.tinyfish.jeekalarm.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType
import com.tinyfish.ui.HeightSpacer
import com.tinyfish.ui.MyBottomBar
import com.tinyfish.ui.MyFileSelector
import com.tinyfish.ui.MyGroupBox
import com.tinyfish.ui.MySwitch
import com.tinyfish.ui.MyTopBar
import com.tinyfish.ui.Observe
import com.tinyfish.ui.SimpleVectorButton
import com.tinyfish.ui.ToolButtonWidthSpacer
import java.util.Calendar

private lateinit var editingSchedule: Schedule
private var isAdding = false

@Composable
fun EditScreen() {
    isAdding = App.editScheduleId == -1
    editingSchedule =
        if (isAdding)
            Schedule()
        else
            ScheduleService.scheduleList.filter { it.id == App.editScheduleId }[0]

//    Scaffold(
//        topBar = { MyTopBar(R.drawable.ic_edit, if (isAdding) "Add" else "Edit") },
//        content = {
//            Surface(
//                color = MaterialTheme.colors.background,
//            ) {
//                Editor()
//            }
//        },
//        bottomBar = { BottomBar() }
//    )

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
            .padding(start = 15.dp, end = 15.dp)
            .verticalScroll(rememberScrollState())
    ) {
        HeightSpacer()

        MyGroupBox {
            Observe {
                App.editEnabledChangeTrigger
                MySwitch("Enabled", editingSchedule::enabled)
            }
            MySwitch("Only Once", editingSchedule::onlyOnce)
        }

        HeightSpacer()

        MyGroupBox {
            val onChange = { _: String ->
                if (!editingSchedule.enabled) {
                    editingSchedule.enabled = true
                    App.editEnabledChangeTrigger++
                }
            }

            CronTimeTextField(
                "Name: ",
                editingSchedule::name,
                false,
                onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "Hour: ",
                editingSchedule::hourConfig,
                true,
                onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "Minute: ",
                editingSchedule::minuteConfig,
                true,
                onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "WeekDay: ",
                editingSchedule::weekDayConfig,
                true,
                onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "Month: ",
                editingSchedule::monthConfig,
                true,
                onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "Day: ",
                editingSchedule::dayConfig,
                true,
                onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "Year: ",
                editingSchedule::yearConfig,
                true,
                onChange
            )
        }

        HeightSpacer()

        MyGroupBox {
            Observe {
                val playScope = currentRecomposeScope

                MySwitch(
                    hint = "Play Music:",
                    booleanProp = editingSchedule::playMusic,
                    onCheckedChange = { playScope.invalidate() })

                if (editingSchedule.playMusic) {
                    Column(Modifier.padding(start = 20.dp)) {
                        HeightSpacer()

                        Observe {
                            val fileSelectScope = currentRecomposeScope
                            MyFileSelector("Music File:",
                                editingSchedule.musicFile,
                                onSelect = {
                                    FileSelector.openMusicFile {
                                        editingSchedule.musicFile =
                                            it.path?.substringAfter(':')!!
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
                            MyFileSelector("Music Folder:",
                                editingSchedule.musicFolder,
                                onSelect = {
                                    FileSelector.openFolder {
                                        editingSchedule.musicFolder =
                                            it.path?.substringAfter(':')!!
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

                HeightSpacer()

                Observe {
                    val vibrationScope = currentRecomposeScope
                    MySwitch(
                        "Vibration",
                        editingSchedule::vibration,
                        onCheckedChange = { vibrationScope.invalidate() })

                    if (editingSchedule.vibration) {
                        HeightSpacer()

                        var vibrationCount by remember { mutableStateOf(editingSchedule.vibrationCount.toFloat()) }

                        Text(
                            vibrationCount.toInt().toString() + " times",
                            modifier = Modifier.padding(start = 20.dp)
                        )
                        Slider(
                            value = vibrationCount,
                            onValueChange = {
                                vibrationCount = it
                                editingSchedule.vibrationCount = vibrationCount.toInt()
                                vibrationScope.invalidate()
                            },
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                            steps = 30,
                            valueRange = 1f..30f,
                        )
                    }
                }
            }
        }

        HeightSpacer()
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
                App.screen = ScreenType.HOME
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
                editingSchedule.yearConfig = (get(Calendar.YEAR)).toString()
                App.editTimeConfigChanged++
            }
        }

        ToolButtonWidthSpacer()
        Observe {
            val text = if (App.isPlaying) "Stop" else "Play"
            val onClick = {
                if (App.isPlaying)
                    ScheduleService.stopPlaying()
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
    editingSchedule.timeConfigChanged()
    if (isAdding) {
        editingSchedule.id = ScheduleService.nextScheduleId++
        ScheduleService.scheduleList.add(editingSchedule)
    }
    ScheduleService.sort()
    ScheduleService.saveAndRefresh()

    ScheduleService.stopPlaying()
    App.screen = ScreenType.HOME
}
