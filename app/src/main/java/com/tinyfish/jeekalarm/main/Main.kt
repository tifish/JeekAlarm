package com.tinyfish.jeekalarm.main

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.Text
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.*
import androidx.ui.material.*
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.App
import com.tinyfish.jeekalarm.HeightSpacer
import com.tinyfish.jeekalarm.ScreenType
import com.tinyfish.jeekalarm.UI
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
        Surface {
            when (UI.screen) {
                ScreenType.MAIN -> MainScreen()
                ScreenType.EDIT -> EditScreen(App.editScheduleIndex)
            }
        }
    }
}

@Composable
fun MainScreen() {
    Scaffold(
        topAppBar = { TopBar() },
        bodyContent = { ScheduleList() },
        bottomAppBar = { BottomBar() }
    )
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

        Spacer(LayoutWidth(20.dp))

        Ripple(bounded = true) {
            Clickable(onClick = {
                App.editScheduleIndex = index
                UI.screen = ScreenType.EDIT
            }) {
                Column(LayoutFlexible(1f, true)) {
                    Text(schedule.name + if (index in UI.nextAlarmIndexes) " (Next alarm)" else "")
                    Text(schedule.timeConfig)
                    Text(App.format(schedule.getNextTriggerTime(now)))
                }
            }
        }

        Spacer(LayoutWidth(20.dp))

        var askForDeleting by state { false }
        if (askForDeleting) {
            AlertDialog(
                onCloseRequest = { },
                text = { Text("Delete this alarm?") },
                confirmButton = {
                    Button(onClick = {
                        ScheduleManager.scheduleList.removeAt(index)
                        ScheduleManager.saveConfig()

                        askForDeleting = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        askForDeleting = false
                    }) {
                        Text("Cancel")
                    }
                },
                buttonLayout = AlertDialogButtonLayout.SideBySide
            )
        } else {
            Button(
                onClick = {
                    askForDeleting = true
                }) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun BottomBar() {
    BottomAppBar(
        navigationIcon = {
            Button(onClick = {
                App.editScheduleIndex = -1
                UI.screen = ScreenType.EDIT
            }) {
                Text("Add")
            }
        },
        actionData = listOf<String>()
    ) {
    }
}
