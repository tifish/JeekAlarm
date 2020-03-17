package com.tinyfish.jeekalarm.main

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.ui.core.Text
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.Divider
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Switch
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.res.vectorResource
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.App
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.alarm.NotificationScreen
import com.tinyfish.jeekalarm.edit.EditScreen
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleManager
import com.tinyfish.jeekalarm.settings.SettingsScreen
import com.tinyfish.jeekalarm.ui.*
import java.util.*

@Composable
fun MainUI() {
    MaterialTheme(colors = DarkColorPalette) {
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
            color = MaterialTheme.colors().background,
            modifier = LayoutFlexible(1f, true)
        ) {
            ScheduleList()
        }
        BottomBar()
    }
}

@Composable
private fun ScheduleList() {
    VerticalScroller {
        Column(LayoutPadding(20.dp)) {
            App.scheduleChangeTrigger.value

            val now = Calendar.getInstance()
            for ((index, schedule) in ScheduleManager.scheduleList.withIndex()) {
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
        if (!App.isRemoving.value) {
            Recompose { recompose ->
                Container(modifier = LayoutGravity.Center) {
                    Switch(
                        checked = schedule.enabled,
                        onCheckedChange = {
                            schedule.enabled = it
                            ScheduleManager.saveConfig()
                            recompose()
                        }
                    )
                }
            }
        }

        Spacer(LayoutWidth(20.dp))

        Ripple(bounded = true) {
            Clickable(onClick = {
                if (!App.isRemoving.value) {
                    App.editScheduleIndex = index
                    App.screen.value = ScreenType.EDIT
                }
            }) {
                Column(LayoutFlexible(1f, true)) {
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
        }

        if (App.isRemoving.value) {
            Spacer(LayoutWidth(20.dp))
            SimpleVectorButton(vectorResource(R.drawable.ic_remove)) {
                ScheduleManager.scheduleList.removeAt(index)
                ScheduleManager.saveConfig()
            }
        }
    }
}

@Composable
private fun BottomBar() {
    MyBottomBar {
        if (App.isRemoving.value) {
            SimpleVectorButton(vectorResource(R.drawable.ic_done), "Done") {
                App.isRemoving.value = false
            }
        } else {
            SimpleVectorButton(vectorResource(R.drawable.ic_add), "Add") {
                App.editScheduleIndex = -1
                App.screen.value = ScreenType.EDIT
            }

            WidthSpacer(36.dp)
            SimpleVectorButton(vectorResource(R.drawable.ic_remove), "Remove") {
                App.isRemoving.value = true
            }

            WidthSpacer(24.dp)
            SimpleVectorButton(vectorResource(R.drawable.ic_settings), "Settings") {
                App.screen.value = ScreenType.SETTINGS
            }
        }
    }
}