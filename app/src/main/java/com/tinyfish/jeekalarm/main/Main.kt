package com.tinyfish.jeekalarm.main

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.remember
import androidx.ui.core.Text
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.*
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.res.vectorResource
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.*
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.edit.EditScreen
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleManager
import com.tinyfish.jeekalarm.ui.SimpleVectorButton
import java.util.*

@Composable
fun MainUI() {
    MaterialTheme(colors = DarkColorPalette) {
        when (UI.screen.value) {
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
            color = MaterialTheme.colors().background,
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
                Divider(color = Color.DarkGray)
            }
        }
    }
}

@Composable
private fun ScheduleItem(index: Int, schedule: Schedule, now: Calendar) {
    Row {
        if (!UI.isRemoving.value) {
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
                if (!UI.isRemoving.value) {
                    App.editScheduleIndex = index
                    UI.screen.value = ScreenType.EDIT
                }
            }) {
                Column(LayoutFlexible(1f, true)) {
                    Text(schedule.name + if (index in UI.nextAlarmIndexes.value) " (Next alarm)" else "")
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

        if (UI.isRemoving.value) {
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
    Surface(elevation = 2.dp, color = MaterialTheme.colors().background) {
        Container(modifier = LayoutHeight(100.dp), expanded = true) {
            Row(arrangement = Arrangement.Center) {
                if (UI.isRemoving.value) {
                    SimpleVectorButton(vectorResource(R.drawable.ic_done), "Done") {
                        UI.isRemoving.value = false
                    }
                } else {
                    SimpleVectorButton(vectorResource(R.drawable.ic_add), "Add") {
                        App.editScheduleIndex = -1
                        UI.screen.value = ScreenType.EDIT
                    }

                    WidthSpacer(36.dp)

                    SimpleVectorButton(vectorResource(R.drawable.ic_remove), "Remove") {
                        UI.isRemoving.value = true
                    }
                }
            }
        }
    }
}
