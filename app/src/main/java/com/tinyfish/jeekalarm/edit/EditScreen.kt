package com.tinyfish.jeekalarm.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.ConfigService
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
import com.tinyfish.ui.SimpleTextField
import com.tinyfish.ui.SimpleVectorButton
import com.tinyfish.ui.ToolButtonWidthSpacer
import java.util.Calendar

private var isAdding = false

@Composable
fun EditScreen() {
    isAdding = App.editScheduleId == -1
    App.editingSchedule = if (isAdding) Schedule()
    else ScheduleService.scheduleList.filter { it.id == App.editScheduleId }[0]

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
            R.drawable.ic_edit, if (isAdding) "Add" else "Edit"
        )
        Surface(
            color = MaterialTheme.colors.background, modifier = Modifier.weight(1f, true)
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
                App.editOptionsChangedTrigger
                MySwitch("Enabled", App.editingSchedule::enabled)
                MySwitch("Only Once", App.editingSchedule::onlyOnce)
            }
        }

        HeightSpacer()

        MyGroupBox {
            Observe {
                App.editTimeConfigChangedTrigger

                var textRange by remember { mutableStateOf(TextRange(App.editingSchedule.name.length)) }

                SimpleTextField("Name: ", TextFieldValue(App.editingSchedule.name, textRange), onTextChanged = {
                    App.editingSchedule.name = it.text
                    textRange = it.selection
                })
            }
            HeightSpacer()

            if (ConfigService.data.openAiApiKey != "") {
                Button(onClick = {
                    App.guessEditingScheduleFromName()
                }) {
                    Text("Guess time from name")
                }
                HeightSpacer()
            }

            val onChange = { _: String ->
                if (!App.editingSchedule.enabled) {
                    App.editingSchedule.enabled = true
                    App.editOptionsChangedTrigger++
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

                        var vibrationCount by remember { mutableStateOf(App.editingSchedule.vibrationCount.toFloat()) }

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
    MyBottomBar {
        SimpleVectorButton(
            ImageVector.vectorResource(R.drawable.ic_back), if (isAdding) "Add" else "Back"
        ) {
            onEditScreenPressBack()
        }

        if (isAdding) {
            ToolButtonWidthSpacer()
            SimpleVectorButton(
                ImageVector.vectorResource(R.drawable.ic_cancel), "Cancel"
            ) {
                App.screen = ScreenType.HOME
            }
        }

        ToolButtonWidthSpacer()
        SimpleVectorButton(
            ImageVector.vectorResource(R.drawable.ic_access_time), "Now"
        ) {
            Calendar.getInstance().apply {
                App.editingSchedule.minuteConfig = get(Calendar.MINUTE).toString()
                App.editingSchedule.hourConfig = get(Calendar.HOUR_OF_DAY).toString()
                App.editingSchedule.dayConfig = get(Calendar.DAY_OF_MONTH).toString()
                App.editingSchedule.monthConfig = (get(Calendar.MONTH) + 1).toString()
                App.editingSchedule.yearConfig = (get(Calendar.YEAR)).toString()
                App.editTimeConfigChangedTrigger++
            }
        }

        ToolButtonWidthSpacer()
        Observe {
            val text = if (App.isPlaying) "Stop" else "Play"
            val onClick = {
                if (App.isPlaying) ScheduleService.stopPlaying()
                else App.editingSchedule.play()
            }

            if (App.isPlaying) SimpleVectorButton(
                ImageVector.vectorResource(R.drawable.ic_stop), text, onClick
            )
            else SimpleVectorButton(
                ImageVector.vectorResource(R.drawable.ic_play_arrow), text, onClick
            )
        }
    }
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
