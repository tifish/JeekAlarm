package com.tinyfish.jeekalarm.edit

import androidx.compose.*
import androidx.ui.core.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutPadding
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.res.vectorResource
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.ui.*
import java.util.*

private lateinit var editingSchedule: Schedule
private var isAdding = false
private lateinit var uiTimeConfigChanged: MutableState<Int>

@Composable
fun EditScreen() {
    uiTimeConfigChanged = state { 0 }

    isAdding = App.editScheduleIndex == -1
    editingSchedule =
        if (isAdding)
            Schedule()
        else
            ScheduleHome.scheduleList[App.editScheduleIndex]


    Column {
        MyTopBar(R.drawable.ic_edit, if (isAdding) "Add" else "Edit")
        Surface(
            color = MaterialTheme.colors().background,
            modifier = LayoutFlexible(1f, true)
        ) {
            Editor()
        }
        BottomBar()
    }
}

@Composable
private fun Editor() {
    VerticalScroller {
        Column(LayoutPadding(20.dp)) {
            MyCheckbox("Enabled", editingSchedule::enabled)
            HeightSpacer()
            MyCheckbox("Only Once", editingSchedule::onlyOnce)

            HeightSpacer()
            MyCronTimeTextField("Name: ", editingSchedule::name)
            Column {
                uiTimeConfigChanged.value
                MyCronTimeTextField("Minute: ", editingSchedule::minuteConfig, true)
                MyCronTimeTextField("Hour: ", editingSchedule::hourConfig, true)
                MyCronTimeTextField("Day: ", editingSchedule::dayConfig, true)
                MyCronTimeTextField("Month: ", editingSchedule::monthConfig, true)
                MyCronTimeTextField("WeekDay: ", editingSchedule::weekDayConfig, true)
            }

            Recompose { recompose ->
                HeightSpacer()
                MyCheckbox(
                    hint = "Play Music:",
                    booleanProp = editingSchedule::playMusic,
                    onCheckedChange = { recompose() })

                if (editingSchedule.playMusic) {
                    HeightSpacer()
                    Recompose { recomposeFileSelect ->
                        MyFileSelect("Music File:", editingSchedule.musicFile,
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
                        MyFileSelect("Music Folder:", editingSchedule.musicFolder,
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
}


@Composable
private fun BottomBar() {
    MyBottomBar {
        SimpleVectorButton(vectorResource(R.drawable.ic_back), if (isAdding) "Add" else "Back") {
            onEditScreenPressBack()
        }

        if (isAdding) {
            WidthSpacer(36.dp)
            SimpleVectorButton(vectorResource(R.drawable.ic_cancel), "Cancel") {
                App.screen.value = ScreenType.MAIN
            }
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
        Observe {
            val text = if (App.isPlaying.value) "Stop" else "Play"
            val onClick = {
                if (App.isPlaying.value)
                    ScheduleHome.stopPlaying()
                else
                    editingSchedule.play()
            }

            if (App.isPlaying.value)
                SimpleVectorButton(vectorResource(R.drawable.ic_stop), text, onClick)
            else
                SimpleVectorButton(vectorResource(R.drawable.ic_play_arrow), text, onClick)
        }
    }
}

fun onEditScreenPressBack() {
    if (isAdding)
        ScheduleHome.scheduleList.add(editingSchedule)
    editingSchedule.timeConfigChanged()
    ScheduleHome.saveConfig()

    ScheduleHome.stopPlaying()
    App.screen.value = ScreenType.MAIN
}
