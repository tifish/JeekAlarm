package com.tinyfish.jeekalarm.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
        Observe {
            val themeScope = currentRecomposeScope
            Text("Theme:")
            Row(modifier = Modifier.padding(start = 20.dp)) {
                val options = listOf("Auto", "Dark", "Light")
                options.forEach {
                    val onClick = {
                        ConfigHome.data.theme = it
                        ConfigHome.save()
                        App.themeColorsChangeTrigger++
                        themeScope.invalidate()
                    }
                    RadioButton(selected = ConfigHome.data.theme == it, onClick = onClick)
                    Text(it, Modifier.clickable(onClick = onClick))
                    WidthSpacer()
                }
            }
        }

        HeightSpacer()
        Observe {
            val fileSelectScope = currentRecomposeScope
            MyFileSelect("Music File:",
                ConfigHome.data.defaultMusicFile,
                onSelect = {
                    FileSelector.openMusicFile {
                        ConfigHome.data.defaultMusicFile = it?.path?.substringAfter(':')!!
                        fileSelectScope.invalidate()
                    }
                },
                onClear = {
                    ConfigHome.data.defaultMusicFile = ""
                    fileSelectScope.invalidate()
                }
            )
        }

        HeightSpacer()
        Observe {
            val fileSelectScope = currentRecomposeScope
            MyFileSelect("Music Folder:",
                ConfigHome.data.defaultMusicFolder,
                onSelect = {
                    FileSelector.openFolder {
                        ConfigHome.data.defaultMusicFolder = it?.path?.substringAfter(':')!!
                        fileSelectScope.invalidate()
                    }
                },
                onClear = {
                    ConfigHome.data.defaultMusicFolder = ""
                    fileSelectScope.invalidate()
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
            ImageVector.vectorResource(backResId),
            "Back"
        ) {
            onSettingsScreenPressBack()
        }
    }
}

fun onSettingsScreenPressBack() {
    ConfigHome.save()
    App.screen = ScreenType.MAIN
}
