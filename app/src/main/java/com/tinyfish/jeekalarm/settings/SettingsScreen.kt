package com.tinyfish.jeekalarm.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.ConfigService
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.alarm.NotificationService
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.home.NavigationBottomBar
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType
import com.tinyfish.ui.*

@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = { MyTopBar(R.drawable.ic_settings, "Settings") },
        content = {
            Surface(
                modifier = Modifier.padding(it),
                color = MaterialTheme.colors.background,
            ) {
                Editor()
            }
        },
        bottomBar = { NavigationBottomBar(ScreenType.SETTINGS) }
    )
}

@Composable
private fun Editor() {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 15.dp, end = 15.dp)
    ) {
        HeightSpacer()

        MyGroupBox()
        {
            Text("Theme:")
            Observe {
                val themeScope = currentRecomposeScope
                Row(
                    modifier = Modifier.padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val options = listOf("Auto", "Dark", "Light")
                    options.forEach {
                        val onClick = {
                            ConfigService.data.theme = it
                            ConfigService.save()
                            App.themeColorsChangeTrigger++
                            themeScope.invalidate()
                        }
                        RadioButton(selected = ConfigService.data.theme == it, onClick = onClick)
                        Text(it, Modifier.clickable(onClick = onClick))
                        WidthSpacer()
                    }
                }
            }
        }

        HeightSpacer()

        MyGroupBox {
            Observe {
                val fileSelectScope = currentRecomposeScope
                MyFileSelector("Music File:",
                    ConfigService.data.defaultMusicFile,
                    onSelect = {
                        FileSelector.openMusicFile {
                            ConfigService.data.defaultMusicFile = it.path?.substringAfter(':')!!
                            fileSelectScope.invalidate()
                        }
                    },
                    onClear = {
                        ConfigService.data.defaultMusicFile = ""
                        fileSelectScope.invalidate()
                    }
                )
            }

            HeightSpacer()
            Observe {
                val fileSelectScope = currentRecomposeScope
                MyFileSelector("Music Folder:",
                    ConfigService.data.defaultMusicFolder,
                    onSelect = {
                        FileSelector.openFolder {
                            ConfigService.data.defaultMusicFolder = it.path?.substringAfter(':')!!
                            fileSelectScope.invalidate()
                        }
                    },
                    onClear = {
                        ConfigService.data.defaultMusicFolder = ""
                        fileSelectScope.invalidate()
                    }
                )
            }
        }

        HeightSpacer()

        Button(
            onClick = {
                NotificationService.showAlarm(ScheduleService.nextAlarmIds)
            },
            Modifier.padding(5.dp)
        ) {
            Text("Test Next Alarm")
        }

        HeightSpacer()
    }
}

fun onSettingsScreenPressBack() {
    ConfigService.save()
    App.screen = ScreenType.HOME
}
