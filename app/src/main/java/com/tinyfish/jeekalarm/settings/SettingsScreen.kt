package com.tinyfish.jeekalarm.settings

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.ConfigHome
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.alarm.NotificationHome
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType
import com.tinyfish.ui.*

@Composable
fun SettingsScreen() {
    Column {
        MyTopBar(R.drawable.ic_settings, "Settings")
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
    Column(Modifier.padding(20.dp)) {
        Recompose { recompose ->
            Text("Theme:")
            Row(modifier = Modifier.padding(start = 20.dp)) {
                val options = listOf("Auto", "Dark", "Light")
                options.forEach {
                    RadioButton(selected = ConfigHome.data.theme == it, onClick = {
                        ConfigHome.data.theme = it
                        ConfigHome.save()
                        App.themeColorsChangeTrigger.value++
                        recompose()
                    })
                    Text(it)
                }
            }
        }

        HeightSpacer()
        Recompose { recomposeFileSelect ->
            MyFileSelect("Music File:",
                ConfigHome.data.defaultMusicFile,
                onSelect = {
                    FileSelector.openMusicFile {
                        ConfigHome.data.defaultMusicFile = it?.path?.substringAfter(':')!!
                        recomposeFileSelect()
                    }
                },
                onClear = {
                    ConfigHome.data.defaultMusicFile = ""
                    recomposeFileSelect()
                }
            )
        }

        HeightSpacer()
        Recompose { recomposeFileSelect ->
            MyFileSelect("Music Folder:",
                ConfigHome.data.defaultMusicFolder,
                onSelect = {
                    FileSelector.openFolder {
                        ConfigHome.data.defaultMusicFolder = it?.path?.substringAfter(':')!!
                        recomposeFileSelect()
                    }
                },
                onClear = {
                    ConfigHome.data.defaultMusicFolder = ""
                    recomposeFileSelect()
                }
            )
        }

        HeightSpacer()

        Button(onClick = {
            NotificationHome.showAlarm(ScheduleHome.nextAlarmIndexes)
        }) {
            Text("Test Next Alarm")
        }
    }
}

@Composable
private fun BottomBar() {
    MyBottomBar {
        val backResId = R.drawable.ic_back
        SimpleVectorButton(
            vectorResource(backResId),
            "Back"
        ) {
            onSettingsScreenPressBack()
        }
    }
}

fun onSettingsScreenPressBack() {
    ConfigHome.save()
    App.screen.value = ScreenType.MAIN
}
