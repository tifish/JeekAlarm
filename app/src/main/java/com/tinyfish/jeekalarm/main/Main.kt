package com.tinyfish.jeekalarm.main

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.remember
import androidx.ui.core.Text
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Switch
import androidx.ui.material.TopAppBar
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.text.TextStyle
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.*
import com.tinyfish.jeekalarm.edit.EditScreen
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleManager
import java.util.*

@Preview
@Composable
private fun MainPreview() {
    MainScreen()
}

@Composable
fun Main() {
    MaterialTheme(colors = DarkColorPalette) {
        when (UI.screen) {
            ScreenType.MAIN -> MainScreen()
            ScreenType.EDIT -> EditScreen(App.editScheduleIndex)
        }
    }
}

@Composable
fun MainScreen() {
    Column {
        TopBar()
        Surface(
            color = DarkColorPalette.background,
            modifier = LayoutFlexible(1f, true)
        ) {
            ScheduleList()
        }
        BottomBar()
    }
}

@Composable
private fun TopBar() {
    TopAppBar(
        title = { Text("JeekAlarm") }
    )
}

@Composable
private fun ScheduleList() {
    VerticalScroller {
        Column(LayoutPadding(20.dp)) {
            remember { UI.scheduleChangeTrigger }

            val now = Calendar.getInstance()
            for ((index, schedule) in ScheduleManager.scheduleList.withIndex()) {
                HeightSpacer()
                ScheduleItem(index, schedule, now)
            }
        }
    }
}

@Composable
private fun ScheduleItem(index: Int, schedule: Schedule, now: Calendar) {
    Row {
        if (!UI.isDeleting) {
            Recompose { recompose ->
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

        Spacer(LayoutWidth(20.dp))

        Ripple(bounded = true) {
            Clickable(onClick = {
                App.editScheduleIndex = index
                UI.screen = ScreenType.EDIT
            }) {
                Column(LayoutFlexible(1f, true)) {
                    Text(schedule.name + if (index in UI.nextAlarmIndexes) " (Next alarm)" else "")
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

        if (UI.isDeleting) {
            Spacer(LayoutWidth(20.dp))
            Button(
                onClick = {
                    ScheduleManager.scheduleList.removeAt(index)
                    ScheduleManager.saveConfig()
                }) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun BottomBar() {
    Surface(elevation = 2.dp, color = DarkColorPalette.background) {
        Container(modifier = LayoutHeight(56.dp), expanded = true) {
            Row(arrangement = Arrangement.Center) {
                if (UI.isDeleting) {
                    Button(onClick = {
                        UI.isDeleting = false
                    }) {
                        Text("Finish")
                    }
                } else {
                    Button(onClick = {
                        App.editScheduleIndex = -1
                        UI.screen = ScreenType.EDIT
                    }) {
                        Text("Add")
                    }

                    WidthSpacer()
                    Button(onClick = {
                        UI.isDeleting = true
                    }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
