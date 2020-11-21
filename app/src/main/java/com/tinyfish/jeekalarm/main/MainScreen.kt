package com.tinyfish.jeekalarm.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.ConfigHome
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.alarm.NotificationScreen
import com.tinyfish.jeekalarm.edit.EditScreen
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import com.tinyfish.jeekalarm.settings.SettingsScreen
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType
import com.tinyfish.ui.*
import java.util.*

@ExperimentalFoundationApi
@ExperimentalFocus
@Composable
fun MainUI() {
    App.themeColorsChangeTrigger.value

    MaterialTheme(colors = GetThemeFromConfig()) {
        when (App.screen.value) {
            ScreenType.MAIN -> MainScreen()
            ScreenType.EDIT -> EditScreen()
            ScreenType.SETTINGS -> SettingsScreen()
            ScreenType.NOTIFICATION -> NotificationScreen()
        }
    }
}

@Composable
fun GetThemeFromConfig(): Colors {
    return when (ConfigHome.data.theme) {
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
            Modifier.weight(1f, true).fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            ScheduleList()
        }
        BottomBar()
    }
}

@Composable
private fun ScheduleList() {
    App.scheduleChangeTrigger.value

    if (ScheduleHome.scheduleList.size == 0) {
        Box(Modifier.wrapContentSize()) {
            SimpleVectorButton(
                vectorResource(R.drawable.ic_add),
                "Add"
            ) {
                App.editScheduleIndex = -1
                App.screen.value = ScreenType.EDIT
            }
        }
    } else {
        ScrollableColumn(Modifier.padding(20.dp)) {
            val now = Calendar.getInstance()
            for (index in ScheduleHome.scheduleList.indices) {
                val schedule = ScheduleHome.scheduleList[index]
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
        Recompose { recompose ->
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = {
                        schedule.enabled = it
                        ScheduleHome.saveConfig()
                        recompose()
                    }
                )
            }
        }

        Spacer(Modifier.preferredWidth(20.dp))

        Column(Modifier.weight(1f, true).clickable(onClick = {
            if (App.removingIndex.value == -1) {
                App.editScheduleIndex = index
                App.screen.value = ScreenType.EDIT
            }
        })) {
            Text(schedule.name + if (index in App.nextAlarmIndexes.value) " (Next alarm)" else "")
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
            if (App.removingIndex.value == -1) {
                SimpleVectorButton(vectorResource(R.drawable.ic_remove)) {
                    App.removingIndex.value = index
                }
                WidthSpacer()
            } else if (App.removingIndex.value == index) {
                SimpleVectorButton(
                    vectorResource(R.drawable.ic_done),
                    "Remove"
                ) {
                    App.removingIndex.value = -1
                    ScheduleHome.scheduleList.removeAt(index)
                    ScheduleHome.saveConfig()
                }

                Spacer(Modifier.preferredWidth(20.dp))
                SimpleVectorButton(
                    vectorResource(R.drawable.ic_back),
                    "Cancel"
                ) {
                    App.removingIndex.value = -1
                }
            }
        }
    }
}

@Composable
private fun BottomBar() {
    MyBottomBar {
        App.scheduleChangeTrigger.value

        if (ScheduleHome.scheduleList.size > 0) {
            SimpleVectorButton(
                vectorResource(R.drawable.ic_add),
                "Add"
            ) {
                App.editScheduleIndex = -1
                App.screen.value = ScreenType.EDIT
            }
            ToolButtonWidthSpacer()
        }

        SimpleVectorButton(
            vectorResource(R.drawable.ic_settings),
            "Settings"
        ) {
            App.screen.value = ScreenType.SETTINGS
        }
    }
}