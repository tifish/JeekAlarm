package com.tinyfish.jeekalarm.settings

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.layout.padding
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.RadioGroup
import androidx.ui.material.Surface
import androidx.ui.res.vectorResource
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.ConfigHome
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.alarm.NotificationHome
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.ui.*

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
            RadioGroup(
                options = listOf("Auto", "Dark", "Light"),
                selectedOption = ConfigHome.data.theme,
                onSelectedChange = {
                    ConfigHome.data.theme = it
                    ConfigHome.save()
                    App.themeColorsChangeTrigger.value++
                    recompose()
                },
                modifier = Modifier.padding(start = 20.dp)
            )
        }

        HeightSpacer()
        Recompose { recomposeFileSelect ->
            MyFileSelect("Music File:", ConfigHome.data.defaultMusicFile,
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
            MyFileSelect("Music Folder:", ConfigHome.data.defaultMusicFolder,
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
        SimpleVectorButton(vectorResource(R.drawable.ic_back), "Back") {
            onSettingsScreenPressBack()
        }
    }
}

fun onSettingsScreenPressBack() {
    ConfigHome.save()
    App.screen.value = ScreenType.MAIN
}
