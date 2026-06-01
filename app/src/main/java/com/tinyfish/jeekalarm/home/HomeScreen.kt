package com.tinyfish.jeekalarm.home

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.alarm.NotificationScreen
import com.tinyfish.jeekalarm.edit.EditScreen
import com.tinyfish.jeekalarm.edit.EditViewModel
import com.tinyfish.jeekalarm.ifly.IFly
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.settings.SettingsScreen
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType
import com.tinyfish.jeekalarm.start.getScreenName
import com.tinyfish.ui.HeightSpacer
import com.tinyfish.ui.MyTopBar
import com.tinyfish.ui.SimpleVectorButton
import com.tinyfish.ui.WidthSpacer
import java.util.Calendar

@Composable
fun MainUI() {
    MaterialTheme(colorScheme = getThemeFromConfig(SettingsService.theme)) {
        when (App.screen) {
            ScreenType.HOME -> HomeScreen()
            ScreenType.EDIT -> EditScreen()
            ScreenType.SETTINGS -> SettingsScreen()
            ScreenType.NOTIFICATION -> NotificationScreen()
        }
    }
}

@Composable
fun getThemeFromConfig(theme: String): ColorScheme {
    val darkTheme = when (theme) {
        "Auto" -> isSystemInDarkTheme()
        "Dark" -> true
        "Light" -> false
        else -> throw Exception("Unexpected theme")
    }

    // Dynamic color is available on Android 12+
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    return when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
}

@Composable
fun HomeScreen() {
    Scaffold(
        topBar = { MyTopBar(R.drawable.ic_alarm, "JeekAlarm") },
        content = { padding ->
            Surface(Modifier.padding(padding)) {
                ScheduleList()
            }
        },
        bottomBar = { NavigationBottomBar(ScreenType.HOME) },
    )
}

@Composable
private fun ScheduleList() {
    val schedules = ScheduleService.scheduleList

    if (schedules.isEmpty()) {
        Box(Modifier.wrapContentSize()) {
            SimpleVectorButton(
                ImageVector.vectorResource(R.drawable.ic_add),
                "Add"
            ) {
                EditViewModel.startEditing(-1)
                App.screen = ScreenType.EDIT
            }
        }
    } else {
        val now = Calendar.getInstance()
        Column(Modifier.verticalScroll(rememberScrollState())) {
            schedules.forEach { schedule ->
                key(schedule.id) {
                    HeightSpacer()
                    ScheduleItem(schedule, now)
                    HorizontalDivider(color = Color.DarkGray)
                }
            }
            HeightSpacer(100.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleItem(schedule: Schedule, now: Calendar) {
    Row(
        Modifier.padding(start = 10.dp, end = 10.dp)
    ) {
        Box(modifier = Modifier.align(Alignment.Top)) {
            Switch(
                checked = schedule.enabled,
                onCheckedChange = { checked ->
                    ScheduleService.setEnabled(schedule.id, checked)
                }
            )
        }

        WidthSpacer(15.dp)

        Box(modifier = Modifier.weight(1f, true)) {
            var dropdownMenuExpanded by remember { mutableStateOf(false) }

            Column(
                Modifier
                    .combinedClickable(
                        onClick = {
                            EditViewModel.startEditing(schedule.id)
                            App.screen = ScreenType.EDIT
                        },
                        onLongClick = {
                            dropdownMenuExpanded = true
                        })
            ) {
                Text(schedule.name + if (schedule.id in App.nextAlarmIds) " (Next)" else "", Modifier.fillMaxWidth())
                Text(schedule.timeConfig)
                Text(App.format(schedule.getNextTriggerTime(now)))
            }

            DropdownMenu(
                expanded = dropdownMenuExpanded,
                onDismissRequest = { dropdownMenuExpanded = false },
                offset = DpOffset(50.dp, (-10).dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Remove") },
                    onClick = {
                        dropdownMenuExpanded = false
                        ScheduleService.scheduleList.removeIf { it.id == schedule.id }
                        ScheduleService.saveAndRefresh()
                    }
                )
            }
        }
    }
}

@Composable
fun NavigationBottomBar(currentScreen: ScreenType) {
    BottomAppBar(
        actions = {
            val items = listOf(ScreenType.HOME, ScreenType.SETTINGS)
            val icons = listOf(R.drawable.ic_home, R.drawable.ic_settings)

            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = item == currentScreen,
                    onClick = { App.screen = item },
                    label = { Text(getScreenName(item)) },
                    icon = { Icon(ImageVector.vectorResource(icons[index]), getScreenName(item)) }
                )
            }
        },
        floatingActionButton = {
            val context = LocalContext.current

            FloatingActionButton(onClick = {
                EditViewModel.startEditing(-1)
                App.screen = ScreenType.EDIT

                IFly.showDialog(context) { recognizedName ->
                    EditViewModel.update { it.copy(name = recognizedName) }
                    EditViewModel.guessFromName()
                }
            }) {
                Icon(ImageVector.vectorResource(R.drawable.ic_add), null)
            }
        },
    )
}

@Preview
@Composable
fun HomeScreenPreview() {
    if (ScheduleService.scheduleList.isEmpty()) {
        ScheduleService.scheduleList.addAll(
            listOf(Schedule(name = "Alarm1"), Schedule(name = "Alarm2"))
        )
    }
    MaterialTheme(colorScheme = darkColorScheme()) {
        HomeScreen()
    }
}
