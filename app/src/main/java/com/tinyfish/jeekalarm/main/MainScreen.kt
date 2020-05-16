package com.tinyfish.jeekalarm.main

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.Divider
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.material.Switch
import androidx.ui.material.ripple.ripple
import androidx.ui.res.vectorResource
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.alarm.NotificationScreen
import com.tinyfish.jeekalarm.edit.EditScreen
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import com.tinyfish.jeekalarm.settings.SettingsScreen
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.ui.*
import java.util.*

@Composable
fun MainUI() {
    MaterialTheme(colors = App.themeColors.value) {
        when (App.screen.value) {
            ScreenType.MAIN -> MainScreen()
            ScreenType.EDIT -> EditScreen()
            ScreenType.SETTINGS -> SettingsScreen()
            ScreenType.NOTIFICATION -> NotificationScreen()
        }
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
            SimpleVectorButton(vectorResource(R.drawable.ic_add), "Add") {
                App.editScheduleIndex = -1
                App.screen.value = ScreenType.EDIT
            }
        }
    } else {
        VerticalScroller {
            Column(Modifier.padding(20.dp)) {
                val now = Calendar.getInstance()
                for ((index, schedule) in ScheduleHome.scheduleList.withIndex()) {
                    HeightSpacer()
                    ScheduleItem(index, schedule, now)
                    Divider(color = Color.DarkGray)
                }
            }
        }
    }
}

@Composable
private fun ScheduleItem(index: Int, schedule: Schedule, now: Calendar) {
    Row {
        Recompose { recompose ->
            Box(modifier = Modifier.gravity(Alignment.CenterVertically)) {
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

        Clickable(
            {
                if (App.removingIndex.value == -1) {
                    App.editScheduleIndex = index
                    App.screen.value = ScreenType.EDIT
                }
            },
            Modifier.ripple(true)
        ) {
            Column(Modifier.weight(1f, true)) {
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
        }

        Row(Modifier.gravity(Alignment.CenterVertically)) {
            if (App.removingIndex.value == -1) {
                SimpleVectorButton(vectorResource(R.drawable.ic_remove)) {
                    App.removingIndex.value = index
                }
                WidthSpacer()
            } else if (App.removingIndex.value == index) {
                SimpleVectorButton(vectorResource(R.drawable.ic_done), "Remove") {
                    App.removingIndex.value = -1
                    ScheduleHome.scheduleList.removeAt(index)
                    ScheduleHome.saveConfig()
                }

                Spacer(Modifier.preferredWidth(20.dp))
                SimpleVectorButton(vectorResource(R.drawable.ic_back), "Cancel") {
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
            SimpleVectorButton(vectorResource(R.drawable.ic_add), "Add") {
                App.editScheduleIndex = -1
                App.screen.value = ScreenType.EDIT
            }
            ToolButtonWidthSpacer()
        }

        SimpleVectorButton(vectorResource(R.drawable.ic_settings), "Settings") {
            App.screen.value = ScreenType.SETTINGS
        }
    }
}