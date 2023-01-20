package com.tinyfish.jeekalarm.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Colors
import androidx.compose.material.Divider
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
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
import com.tinyfish.jeekalarm.start.getScreenName
import com.tinyfish.ui.DarkColorPalette
import com.tinyfish.ui.HeightSpacer
import com.tinyfish.ui.LightColorPalette
import com.tinyfish.ui.MyTopBar
import com.tinyfish.ui.SimpleVectorButton
import com.tinyfish.ui.WidthSpacer
import java.util.Calendar

@Composable
fun MainUI() {
    App.themeColorsChangeTrigger

    MaterialTheme(colors = getThemeFromConfig()) {
        when (App.screen) {
            ScreenType.HOME -> HomeScreen()
            ScreenType.EDIT -> EditScreen()
            ScreenType.SETTINGS -> SettingsScreen()
            ScreenType.NOTIFICATION -> NotificationScreen()
        }
    }
}

@Composable
fun getThemeFromConfig(): Colors {
    return when (ConfigService.data.theme) {
        "Auto" -> if (isSystemInDarkTheme()) DarkColorPalette else LightColorPalette
        "Dark" -> DarkColorPalette
        "Light" -> LightColorPalette
        else -> throw Exception("Unexpected theme")
    }
}

@Composable
fun HomeScreen() {
    Scaffold(
        topBar = { MyTopBar(R.drawable.ic_alarm, "JeekAlarm") },
        content = {
            Surface(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(it),
                color = MaterialTheme.colors.background,
            ) {
                ScheduleList()
            }
        },
        bottomBar = { NavigationBottomBar(ScreenType.HOME) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                App.editScheduleId = -1
                App.screen = ScreenType.EDIT
            }) {
                Icon(ImageVector.vectorResource(R.drawable.ic_add), null)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        isFloatingActionButtonDocked = true,
    )
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
                App.editScheduleId = -1
                App.screen = ScreenType.EDIT
            }
        }
    } else {
        Column {
            App.scheduleChangeTrigger

            val now = Calendar.getInstance()
            for (index in ScheduleService.scheduleList.indices) {
                val schedule = ScheduleService.scheduleList[index]
                HeightSpacer()
                ScheduleItem(index, schedule, now)
                Divider(color = Color.DarkGray)
            }

            HeightSpacer(100.dp)
        }
    }
}

@Composable
private fun ScheduleItem(index: Int, schedule: Schedule, now: Calendar) {
    Row(
        Modifier.padding(start = 10.dp, end = 10.dp)
    ) {
        Box(modifier = Modifier.align(Alignment.Top)) {
            val boxScope = currentRecomposeScope
            Switch(
                checked = schedule.enabled,
                onCheckedChange = {
                    schedule.enabled = it
                    ScheduleService.saveAndRefresh()
                    boxScope.invalidate()
                }
            )
        }

        WidthSpacer()

        Column(
            Modifier
                .weight(1f, true)
                .clickable(onClick = {
                    if (App.removingIndex == -1) {
                        App.editScheduleId = schedule.id
                        App.screen = ScreenType.EDIT
                    }
                })
        ) {
            Text(schedule.name + if (schedule.id in App.nextAlarmIds) " (Next)" else "")
            Text(
                schedule.timeConfig,
                style = TextStyle(color = Color.Gray)
            )
            Text(
                App.format(schedule.getNextTriggerTime(now)),
                style = TextStyle(color = Color.Gray)
            )
        }

        Row(Modifier.align(Alignment.Bottom)) {
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
                    ScheduleService.saveAndRefresh()
                }

                WidthSpacer()
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
fun NavigationBottomBar(currentScreen: ScreenType) {
    BottomNavigation(
        backgroundColor = MaterialTheme.colors.primary,
    ) {
        val items = listOf(ScreenType.HOME, ScreenType.SETTINGS)
        val icons = listOf(R.drawable.ic_home, R.drawable.ic_settings)

        items.forEachIndexed { index, item ->
            BottomNavigationItem(
                selected = item == currentScreen,
                onClick = { App.screen = item },
                label = { Text(getScreenName(item)) },
                icon = { Icon(ImageVector.vectorResource(icons[index]), getScreenName(item)) }
            )
        }
    }
}