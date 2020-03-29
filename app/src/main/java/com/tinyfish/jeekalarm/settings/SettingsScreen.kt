package com.tinyfish.jeekalarm.settings

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.ui.core.Text
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutPadding
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
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
    Column(LayoutPadding(20.dp)) {
        Recompose { recompose ->
            MyCheckbox(
                hint = "Dark theme",
                value = ConfigHome.data.theme == "Dark"
            ) {
                ConfigHome.data.theme = if (it) "Dark" else "Light"
                App.setThemeFromConfig()
                recompose()
            }
        }

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
