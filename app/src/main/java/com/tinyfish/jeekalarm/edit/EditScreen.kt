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
import androidx.compose.runtime.currentRecomposeScope
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
import com.tinyfish.jeekalarm.schedule.Schedule
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

private var isAdding = false

@Composable
fun EditScreen() {
    isAdding = App.editScheduleId == -1
    App.editingSchedule = if (isAdding)
        Schedule()
    else
        ScheduleService.scheduleList.filter { it.id == App.editScheduleId }[0]

    Scaffold(topBar = { MyTopBar(R.drawable.ic_edit, if (isAdding) "Add" else "Edit") }, content = {
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
                App.editingOptionsChangedTrigger
                MySwitch("Enabled", App.editingSchedule::enabled)
                MySwitch("Only Once", App.editingSchedule::onlyOnce)
            }
        }

        HeightSpacer()

        MyGroupBox {
            Observe {
                App.editingNameChangedTrigger

                SimpleTextField("Name: ", App.editingSchedule.name, onTextChanged = {
                    App.editingSchedule.name = it
                    App.editingNameChangedTrigger++
                })
            }
            HeightSpacer()

            if (SettingsService.openAiApiKey != "") {
                Button(onClick = {
                    GlobalScope.launch(Dispatchers.Main) {
                        App.guessEditingScheduleFromName()
                    }
                }) {
                    Text("Guess time from name")
                }
                HeightSpacer()
            }

            val onChange = { _: String ->
                if (!App.editingSchedule.enabled) {
                    App.editingSchedule.enabled = true
                    App.editingOptionsChangedTrigger++
                }
            }

            CronTimeTextField(
                "Hour: ", App.editingSchedule::hourConfig, true, onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "Minute: ", App.editingSchedule::minuteConfig, true, onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "WeekDay: ", App.editingSchedule::weekDayConfig, true, onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "Month: ", App.editingSchedule::monthConfig, true, onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "Day: ", App.editingSchedule::dayConfig, true, onChange
            )
            HeightSpacer()
            CronTimeTextField(
                "Year: ", App.editingSchedule::yearConfig, true, onChange
            )
        }

        HeightSpacer()

        MyGroupBox {
            Observe {
                val playScope = currentRecomposeScope

                MySwitch(hint = "Play Music:", booleanProp = App.editingSchedule::playMusic, onCheckedChange = { playScope.invalidate() })

                if (App.editingSchedule.playMusic) {
                    Column(Modifier.padding(start = 20.dp)) {
                        HeightSpacer()

                        Observe {
                            val fileSelectScope = currentRecomposeScope
                            MyFileSelector("Music File:", App.editingSchedule.musicFile, onSelect = {
                                FileSelector.openMusicFile {
                                    App.editingSchedule.musicFile = it.path?.substringAfter(':')!!
                                    fileSelectScope.invalidate()
                                }
                            }, onClear = {
                                App.editingSchedule.musicFile = ""
                                fileSelectScope.invalidate()
                            })
                        }

                        HeightSpacer()

                        Observe {
                            val fileSelectScope = currentRecomposeScope
                            MyFileSelector("Music Folder:", App.editingSchedule.musicFolder, onSelect = {
                                FileSelector.openFolder {
                                    App.editingSchedule.musicFolder = it.path?.substringAfter(':')!!
                                    fileSelectScope.invalidate()
                                }
                            }, onClear = {
                                App.editingSchedule.musicFolder = ""
                                fileSelectScope.invalidate()
                            })
                        }
                    }
                }

                HeightSpacer()

                Observe {
                    val vibrationScope = currentRecomposeScope
                    MySwitch("Vibration", App.editingSchedule::vibration, onCheckedChange = { vibrationScope.invalidate() })

                    if (App.editingSchedule.vibration) {
                        HeightSpacer()

                        var vibrationCount by remember { mutableFloatStateOf(App.editingSchedule.vibrationCount.toFloat()) }

                        Text(
                            vibrationCount.toInt().toString() + " times", modifier = Modifier.padding(start = 20.dp)
                        )
                        Slider(
                            value = vibrationCount,
                            onValueChange = {
                                vibrationCount = it
                                App.editingSchedule.vibrationCount = vibrationCount.toInt()
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
    val context = LocalContext.current

    BottomAppBar(actions = {
        if (isAdding) {
            NavigationBarItem(selected = false, onClick = {
                App.screen = ScreenType.HOME
            }, label = { Text("Cancel") }, icon = { Icon(ImageVector.vectorResource(R.drawable.ic_cancel), null) })
        } else {
            NavigationBarItem(selected = false, onClick = {
                AlertDialog.Builder(context).setTitle("Remove").setMessage("Remove this schedule?").setPositiveButton("Yes") { _, _ ->
                    ScheduleService.scheduleList.removeIf { it.id == App.editScheduleId }
                    App.scheduleChangedTrigger++
                    App.screen = ScreenType.HOME
                }.setNegativeButton("No", null).show()
            }, label = { Text("Remove") }, icon = { Icon(ImageVector.vectorResource(R.drawable.ic_remove), null) })
        }

        NavigationBarItem(selected = false, onClick = {
            onEditScreenPressBack()
        }, label = { Text(if (isAdding) "Add" else "Apply") }, icon = { Icon(ImageVector.vectorResource(R.drawable.ic_done), null) })

        NavigationBarItem(selected = false, onClick = {
            Calendar.getInstance().apply {
                App.editingSchedule.minuteConfig = get(Calendar.MINUTE).toString()
                App.editingSchedule.hourConfig = get(Calendar.HOUR_OF_DAY).toString()
                App.editingSchedule.dayConfig = get(Calendar.DAY_OF_MONTH).toString()
                App.editingSchedule.monthConfig = (get(Calendar.MONTH) + 1).toString()
                App.editingSchedule.yearConfig = (get(Calendar.YEAR)).toString()
                App.editingTimeConfigChangedTrigger++
            }
        }, label = { Text("Now") }, icon = { Icon(ImageVector.vectorResource(R.drawable.ic_access_time), null) })

        Observe {
            val text = if (App.isPlaying) "Stop" else "Play"
            val onClick = {
                if (App.isPlaying) ScheduleService.stopPlaying()
                else App.editingSchedule.play()
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
    App.editingSchedule.timeConfigChanged()
    if (isAdding) {
        App.editingSchedule.id = ScheduleService.nextScheduleId++
        ScheduleService.scheduleList.add(App.editingSchedule)
    }
    ScheduleService.sort()
    ScheduleService.saveAndRefresh()

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
