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
import com.tinyfish.jeekalarm.App
import com.tinyfish.jeekalarm.Config
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.alarm.Notification
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.schedule.ScheduleManager
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
        Recompose { recomposeFileSelect ->
            MyFileSelect("Music File:", Config.data.defaultMusicFile,
                onSelect = {
                    FileSelector.openMusicFile {
                        Config.data.defaultMusicFile = it?.path?.substringAfter(':')!!
                        recomposeFileSelect()
                    }
                },
                onClear = {
                    Config.data.defaultMusicFile = ""
                    recomposeFileSelect()
                }
            )
        }

        HeightSpacer()
        Recompose { recomposeFileSelect ->
            MyFileSelect("Music Folder:", Config.data.defaultMusicFolder,
                onSelect = {
                    FileSelector.openFolder {
                        Config.data.defaultMusicFolder = it?.path?.substringAfter(':')!!
                        recomposeFileSelect()
                    }
                },
                onClear = {
                    Config.data.defaultMusicFolder = ""
                    recomposeFileSelect()
                }
            )
        }

        HeightSpacer()

        Button(onClick = {
            Notification.showAlarm(ScheduleManager.nextAlarmIndexes)
        }) {
            Text("Test Next Alarm")
        }
    }
}

@Composable
private fun BottomBar() {
    MyBottomBar {
        SimpleVectorButton(vectorResource(R.drawable.ic_done), "OK") {
            onSettingsScreenPressOK()
        }
    }
}

fun onSettingsScreenPressOK() {
    Config.save()
    App.screen.value = ScreenType.MAIN
}
