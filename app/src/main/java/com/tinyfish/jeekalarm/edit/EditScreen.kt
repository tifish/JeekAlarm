package com.tinyfish.jeekalarm.edit

import androidx.compose.*
import androidx.ui.core.Text
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutPadding
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.res.vectorResource
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.*
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.ui.*
import java.util.*

private lateinit var originalSchedule: Schedule
private lateinit var editingSchedule: Schedule

private lateinit var uiTimeConfigChanged: MutableState<Int>

@Composable
fun EditScreen() {
    uiTimeConfigChanged = state { 0 }

    if (App.editScheduleIndex == -1) {
        editingSchedule = Schedule()
    } else {
        originalSchedule = ScheduleHome.scheduleList[App.editScheduleIndex]
        editingSchedule = originalSchedule.copy()
    }

    Column {
        MyTopBar(R.drawable.ic_edit, "Edit")
        Surface(
            color = MaterialTheme.colors().background,
            modifier = LayoutFlexible(1f, true)
        ) {
            Editor()
        }
        BottomBar(App.editScheduleIndex == -1)
    }
}

@Composable
private fun Editor() {
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

@Composable
private fun BottomBar(isAdding: Boolean) {
    MyBottomBar {
        SimpleVectorButton(vectorResource(R.drawable.ic_cancel), "Cancel") {
            ScheduleHome.stopPlaying()
            App.screen.value = ScreenType.MAIN
        }

        WidthSpacer(36.dp)
        SimpleVectorButton(
            vectorResource(if (isAdding) R.drawable.ic_add else R.drawable.ic_done),
            if (isAdding) "Add" else "Save"
        ) {
            if (isAdding) {
                editingSchedule.timeConfigChanged()
                ScheduleHome.scheduleList.add(editingSchedule)
            } else {
                editingSchedule.copyTo(originalSchedule)
                originalSchedule.timeConfigChanged()
            }
            ScheduleHome.saveConfig()

            ScheduleHome.stopPlaying()
            App.screen.value = ScreenType.MAIN
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
