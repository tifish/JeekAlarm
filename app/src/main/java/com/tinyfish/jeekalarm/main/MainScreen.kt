package com.tinyfish.jeekalarm.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.ConfigService
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.alarm.NotificationScreen
import com.tinyfish.jeekalarm.edit.EditScreen
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.settings.SettingsScreen
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType
import com.tinyfish.ui.*
import java.util.*

@Composable
fun MainUI() {
    App.themeColorsChangeTrigger

    MaterialTheme(colors = GetThemeFromConfig()) {
        when (App.screen) {
            ScreenType.MAIN -> MainScreen()
            ScreenType.EDIT -> EditScreen()
            ScreenType.SETTINGS -> SettingsScreen()
            ScreenType.NOTIFICATION -> NotificationScreen()
        }
    }
}

@Composable
fun GetThemeFromConfig(): Colors {
    return when (ConfigService.data.theme) {
        "Auto" -> if (isSystemInDarkTheme()) DarkColorPalette else LightColorPalette
        "Dark" -> DarkColorPalette
        "Light" -> LightColorPalette
        else -> LightColorPalette
    }
}

@Composable
fun MainScreen() {
    Column {
        MyTopBar(R.drawable.ic_alarm, "JeekAlarm")
        Surface(
            Modifier
                .weight(1f, true)
                .fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            ScheduleList()
        }
        BottomBar()
    }
}

@Composable
private fun ScheduleList() {
    App.scheduleChangeTrigger

    if (ScheduleService.scheduleList.size == 0) {
        Box(Modifier.wrapContentSize()) {
            SimpleVectorButton(
                ImageVector.vectorResource(R.drawable.ic_add),
                "Add"
            ) {
                App.editScheduleIndex = -1
                App.screen = ScreenType.EDIT
            }
        }
    } else {
        Column(
            Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())) {
            App.scheduleChangeTrigger
            val now = Calendar.getInstance()
            for (index in ScheduleService.scheduleList.indices) {
                val schedule = ScheduleService.scheduleList[index]
                HeightSpacer()
                ScheduleItem(index, schedule, now)
                Divider(color = Color.DarkGray)
            }
        }
    }
}

@Composable
private fun ScheduleItem(index: Int, schedule: Schedule, now: Calendar) {
    Row {
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                val boxScope = currentRecomposeScope
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = {
                        schedule.enabled = it
                        ScheduleService.saveConfig()
                        boxScope.invalidate()
                    }
                )
            }

        Spacer(Modifier.width(20.dp))

        Column(
            Modifier
                .weight(1f, true)
                .clickable(onClick = {
                    if (App.removingIndex == -1) {
                        App.editScheduleIndex = index
                        App.screen = ScreenType.EDIT
                    }
                })) {
            Text(schedule.name + if (index in App.nextAlarmIndexes) " (Next alarm)" else "")
            Text(
                schedule.timeConfig,
                style = TextStyle(color = Color.Gray)
            )
            Text(
                App.format(schedule.getNextTriggerTime(now)),
                style = TextStyle(color = Color.Gray)
            )
        }

        Row(Modifier.align(Alignment.CenterVertically)) {
            if (App.removingIndex == -1) {
                SimpleVectorButton(ImageVector.vectorResource(R.drawable.ic_remove)) {
                    App.removingIndex = index
                }
                WidthSpacer()
            } else if (App.removingIndex == index) {
                SimpleVectorButton(
                    ImageVector.vectorResource(R.drawable.ic_done),
                    "Remove"
                ) {
                    App.removingIndex = -1
                    ScheduleService.scheduleList.removeAt(index)
                    ScheduleService.saveConfig()
                }

                Spacer(Modifier.width(20.dp))
                SimpleVectorButton(
                    ImageVector.vectorResource(R.drawable.ic_back),
                    "Cancel"
                ) {
                    App.removingIndex = -1
                }
            }
        }
    }
}

@Composable
private fun BottomBar() {
    MyBottomBar {
        App.scheduleChangeTrigger

        if (ScheduleService.scheduleList.size > 0) {
            SimpleVectorButton(
                ImageVector.vectorResource(R.drawable.ic_add),
                "Add"
            ) {
                App.editScheduleIndex = -1
                App.screen = ScreenType.EDIT
            }
            ToolButtonWidthSpacer()
        }

        SimpleVectorButton(
            ImageVector.vectorResource(R.drawable.ic_settings),
            "Settings"
        ) {
            App.screen = ScreenType.SETTINGS
        }
    }
}