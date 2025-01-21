package com.tinyfish.jeekalarm.edit

import android.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType
import com.tinyfish.ui.HeightSpacer
import com.tinyfish.ui.MyFileSelector
import com.tinyfish.ui.MyGroupBox
import com.tinyfish.ui.MySwitch
import com.tinyfish.ui.MyTopBar
import com.tinyfish.ui.Observe
import com.tinyfish.ui.SimpleTextField
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun EditScreen() {
    EditViewModel.isAdding = EditViewModel.editScheduleId == -1
    EditViewModel.initEditingSchedule()

    Scaffold(topBar = { MyTopBar(R.drawable.ic_edit, if (EditViewModel.isAdding) "Add" else "Edit") }, content = {
        Surface(Modifier.padding(it)) {
            Editor()
        }
    }, bottomBar = { BottomBar() })
}

@OptIn(DelicateCoroutinesApi::class)
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
                MySwitch("Enabled", EditViewModel.editingScheduleEnabled, { EditViewModel.editingScheduleEnabled = it })
                MySwitch("Only Once", EditViewModel.editingScheduleOnlyOnce, { EditViewModel.editingScheduleOnlyOnce = it })
            }
        }

        HeightSpacer()

        MyGroupBox {
            Observe {
                SimpleTextField("Name: ", EditViewModel.editingScheduleName, onTextChanged = {
                    EditViewModel.editingScheduleName = it
                })
            }
            HeightSpacer()

            if (SettingsService.deepSeekApiKey != "") {
                Button(onClick = {
                    GlobalScope.launch(Dispatchers.Main) {
                        EditViewModel.guessEditingScheduleFromName()
                    }
                }) {
                    Text("Guess time from name")
                }
                HeightSpacer()
            }

            val onChange = { _: String ->
                if (!EditViewModel.editingScheduleEnabled) {
                    EditViewModel.editingScheduleEnabled = true
                }
            }

            CronTimeTextField("Hour: ", EditViewModel.editingScheduleHourConfig) {
                EditViewModel.editingScheduleHourConfig = it
                onChange(it)
            }

            HeightSpacer()

            CronTimeTextField("Minute: ", EditViewModel.editingScheduleMinuteConfig) {
                EditViewModel.editingScheduleMinuteConfig = it
                onChange(it)
            }

            HeightSpacer()

            CronTimeTextField("WeekDay: ", EditViewModel.editingScheduleWeekDayConfig) {
                EditViewModel.editingScheduleWeekDayConfig = it
                onChange(it)
            }

            HeightSpacer()

            CronTimeTextField("Month: ", EditViewModel.editingScheduleMonthConfig) {
                EditViewModel.editingScheduleMonthConfig = it
                onChange(it)
            }

            HeightSpacer()

            CronTimeTextField("Day: ", EditViewModel.editingScheduleDayConfig) {
                EditViewModel.editingScheduleDayConfig = it
                onChange(it)
            }

            HeightSpacer()

            CronTimeTextField("Year: ", EditViewModel.editingScheduleYearConfig) {
                EditViewModel.editingScheduleYearConfig = it
                onChange(it)
            }
        }

        HeightSpacer()

        MyGroupBox {
            Observe {
                MySwitch(hint = "Play Music:", EditViewModel.editingSchedulePlayMusic, { EditViewModel.editingSchedulePlayMusic = it })

                if (EditViewModel.editingSchedulePlayMusic) {
                    Column(Modifier.padding(start = 20.dp)) {
                        HeightSpacer()

                        Observe {
                            MyFileSelector("Music File:", EditViewModel.editingScheduleMusicFile, onSelect = {
                                FileSelector.openMusicFile {
                                    EditViewModel.editingScheduleMusicFile = it.path?.substringAfter(':')!!
                                }
                            }, onClear = {
                                EditViewModel.editingScheduleMusicFile = ""
                            })
                        }

                        HeightSpacer()

                        Observe {
                            MyFileSelector("Music Folder:", EditViewModel.editingScheduleMusicFolder, onSelect = {
                                FileSelector.openFolder {
                                    EditViewModel.editingScheduleMusicFolder = it.path?.substringAfter(':')!!
                                }
                            }, onClear = {
                                EditViewModel.editingScheduleMusicFolder = ""
                            })
                        }
                    }
                }

                HeightSpacer()

                Observe {
                    MySwitch(
                        "Vibration",
                        EditViewModel.editingScheduleVibration,
                        onCheckedChange = { EditViewModel.editingScheduleVibration = it })

                    if (EditViewModel.editingScheduleVibration) {
                        HeightSpacer()

                        var vibrationCount by remember { mutableFloatStateOf(EditViewModel.editingScheduleVibrationCount.toFloat()) }

                        Text(
                            vibrationCount.toInt().toString() + " times", modifier = Modifier.padding(start = 20.dp)
                        )
                        Slider(
                            value = vibrationCount,
                            onValueChange = {
                                vibrationCount = it
                                EditViewModel.editingScheduleVibrationCount = vibrationCount.toInt()
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
    val context = LocalContext.current

    BottomAppBar(actions = {
        if (EditViewModel.isAdding) {
            NavigationBarItem(selected = false, onClick = {
                App.screen = ScreenType.HOME
            }, label = { Text("Cancel") }, icon = { Icon(ImageVector.vectorResource(R.drawable.ic_cancel), null) })
        } else {
            NavigationBarItem(selected = false, onClick = {
                AlertDialog.Builder(context).setTitle("Remove").setMessage("Remove this schedule?").setPositiveButton("Yes") { _, _ ->
                    ScheduleService.scheduleList.removeIf { it.id == EditViewModel.editScheduleId }
                    App.scheduleChangedTrigger++
                    App.screen = ScreenType.HOME
                }.setNegativeButton("No", null).show()
            }, label = { Text("Remove") }, icon = { Icon(ImageVector.vectorResource(R.drawable.ic_remove), null) })
        }

        NavigationBarItem(
            selected = false,
            onClick = {
                onEditScreenPressBack()
            },
            label = { Text(if (EditViewModel.isAdding) "Add" else "Apply") },
            icon = { Icon(ImageVector.vectorResource(R.drawable.ic_done), null) })

        NavigationBarItem(
            selected = false,
            onClick = { EditViewModel.setEditingScheduleTime(Calendar.getInstance()) },
            label = { Text("Now") },
            icon = { Icon(ImageVector.vectorResource(R.drawable.ic_access_time), null) },
        )

        Observe {
            val text = if (App.isPlaying) "Stop" else "Play"
            val onClick = {
                if (App.isPlaying) ScheduleService.stopPlaying()
                else EditViewModel.play()
            }

            if (App.isPlaying) NavigationBarItem(selected = false,
                onClick = onClick,
                label = { Text(text) },
                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_stop), null) })
            else NavigationBarItem(selected = false,
                onClick = onClick,
                label = { Text(text) },
                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_play_arrow), null) })
        }
    })
}

fun onEditScreenPressBack() {
    EditViewModel.saveEditingSchedule()

    ScheduleService.stopPlaying()
    App.screen = ScreenType.HOME
}

@Preview
@Composable
fun EditScreenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        EditScreen()
    }
}
