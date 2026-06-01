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
import com.tinyfish.ui.SimpleTextField
import java.util.Calendar

@Composable
fun EditScreen() {
    Scaffold(topBar = { MyTopBar(R.drawable.ic_edit, if (EditViewModel.isAdding) "Add" else "Edit") }, content = {
        Surface(Modifier.padding(it)) {
            Editor()
        }
    }, bottomBar = { BottomBar() })
}

@Composable
private fun Editor() {
    val schedule = EditViewModel.editing

    Column(
        Modifier
            .padding(start = 15.dp, end = 15.dp)
            .verticalScroll(rememberScrollState())
    ) {
        HeightSpacer()

        MyGroupBox {
            MySwitch("Enabled", schedule.enabled, { checked -> EditViewModel.update { it.copy(enabled = checked) } })
            MySwitch("Only Once", schedule.onlyOnce, { checked -> EditViewModel.update { it.copy(onlyOnce = checked) } })
        }

        HeightSpacer()

        MyGroupBox {
            SimpleTextField("Name: ", schedule.name, onTextChanged = { name ->
                EditViewModel.update { it.copy(name = name) }
            })
            HeightSpacer()

            if (SettingsService.openAiApiKey != "") {
                Button(onClick = { EditViewModel.guessFromName() }) {
                    Text("Guess time from name")
                }
                HeightSpacer()
            }

            // 改任意时间字段时，顺带启用该闹钟
            CronTimeTextField("Hour: ", schedule.hourConfig) { value ->
                EditViewModel.update { it.copy(hourConfig = value, enabled = true) }
            }

            HeightSpacer()

            CronTimeTextField("Minute: ", schedule.minuteConfig) { value ->
                EditViewModel.update { it.copy(minuteConfig = value, enabled = true) }
            }

            HeightSpacer()

            CronTimeTextField("WeekDay: ", schedule.weekDayConfig) { value ->
                EditViewModel.update { it.copy(weekDayConfig = value, enabled = true) }
            }

            HeightSpacer()

            CronTimeTextField("Month: ", schedule.monthConfig) { value ->
                EditViewModel.update { it.copy(monthConfig = value, enabled = true) }
            }

            HeightSpacer()

            CronTimeTextField("Day: ", schedule.dayConfig) { value ->
                EditViewModel.update { it.copy(dayConfig = value, enabled = true) }
            }

            HeightSpacer()

            CronTimeTextField("Year: ", schedule.yearConfig) { value ->
                EditViewModel.update { it.copy(yearConfig = value, enabled = true) }
            }
        }

        HeightSpacer()

        MyGroupBox {
            MySwitch(hint = "Play Music:", schedule.playMusic, { checked -> EditViewModel.update { it.copy(playMusic = checked) } })

            if (schedule.playMusic) {
                Column(Modifier.padding(start = 20.dp)) {
                    HeightSpacer()

                    MyFileSelector("Music File:", schedule.musicFile, onSelect = {
                        FileSelector.openMusicFile { uri ->
                            EditViewModel.update { it.copy(musicFile = uri.toString()) }
                        }
                    }, onClear = {
                        EditViewModel.update { it.copy(musicFile = "") }
                    })

                    HeightSpacer()

                    MyFileSelector("Music Folder:", schedule.musicFolder, onSelect = {
                        FileSelector.openFolder { uri ->
                            EditViewModel.update { it.copy(musicFolder = uri.toString()) }
                        }
                    }, onClear = {
                        EditViewModel.update { it.copy(musicFolder = "") }
                    })
                }
            }

            HeightSpacer()

            MySwitch("Vibration", schedule.vibration, { checked -> EditViewModel.update { it.copy(vibration = checked) } })

            if (schedule.vibration) {
                HeightSpacer()

                Text(
                    "${schedule.vibrationCount} times", modifier = Modifier.padding(start = 20.dp)
                )
                Slider(
                    value = schedule.vibrationCount.toFloat(),
                    onValueChange = { value ->
                        EditViewModel.update { it.copy(vibrationCount = value.toInt()) }
                    },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                    steps = 30,
                    valueRange = 1f..30f,
                )
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
                    ScheduleService.saveAndRefresh()
                    App.screen = ScreenType.HOME
                }.setNegativeButton("No", null).show()
            }, label = { Text("Remove") }, icon = { Icon(ImageVector.vectorResource(R.drawable.ic_remove), null) })
        }

        NavigationBarItem(
            selected = false,
            onClick = { onEditScreenPressBack() },
            label = { Text(if (EditViewModel.isAdding) "Add" else "Apply") },
            icon = { Icon(ImageVector.vectorResource(R.drawable.ic_done), null) })

        NavigationBarItem(
            selected = false,
            onClick = { EditViewModel.setEditingScheduleTime(Calendar.getInstance()) },
            label = { Text("Now") },
            icon = { Icon(ImageVector.vectorResource(R.drawable.ic_access_time), null) },
        )

        val isPlaying = App.isPlaying
        NavigationBarItem(
            selected = false,
            onClick = {
                if (isPlaying) ScheduleService.stopPlaying()
                else EditViewModel.play()
            },
            label = { Text(if (isPlaying) "Stop" else "Play") },
            icon = {
                Icon(
                    ImageVector.vectorResource(if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play_arrow),
                    null
                )
            })
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
