package com.tinyfish.jeekalarm.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.tinyfish.ui.WidthSpacer
import com.tinyfish.ui.theme.JeekAlarmTheme
import java.util.Calendar

@Composable
fun MainUI() {
    JeekAlarmTheme(SettingsService.theme) {
        when (App.screen) {
            ScreenType.HOME -> HomeScreen()
            ScreenType.EDIT -> EditScreen()
            ScreenType.SETTINGS -> SettingsScreen()
            ScreenType.NOTIFICATION -> NotificationScreen()
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = { MyTopBar(R.drawable.ic_alarm, "JeekAlarm") },
        bottomBar = { NavigationBottomBar(ScreenType.HOME) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    EditViewModel.startEditing(-1)
                    App.screen = ScreenType.EDIT

                    IFly.showDialog(context) { recognizedName ->
                        EditViewModel.update { it.copy(name = recognizedName) }
                        EditViewModel.guessFromName()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_add), "Add alarm")
            }
        },
    ) { padding ->
        ScheduleList(Modifier.padding(padding))
    }
}

@Composable
private fun ScheduleList(modifier: Modifier = Modifier) {
    val schedules = ScheduleService.scheduleList

    if (schedules.isEmpty()) {
        EmptyState(modifier)
        return
    }

    val now = Calendar.getInstance()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(schedules, key = { it.id }) { schedule ->
            ScheduleItem(schedule, now)
        }
        // 给悬浮按钮留出底部空间，避免遮挡最后一项
        item { HeightSpacer(80.dp) }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            ImageVector.vectorResource(R.drawable.ic_alarm),
            null,
            Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        )
        HeightSpacer(20.dp)
        Text("No alarms yet", style = MaterialTheme.typography.titleLarge)
        HeightSpacer(4.dp)
        Text(
            "Tap + to add your first alarm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        HeightSpacer(24.dp)
        Button(onClick = {
            EditViewModel.startEditing(-1)
            App.screen = ScreenType.EDIT
        }) {
            Icon(ImageVector.vectorResource(R.drawable.ic_add), null)
            WidthSpacer(8.dp)
            Text("Add alarm")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleItem(schedule: Schedule, now: Calendar) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isNext = schedule.id in App.nextAlarmIds

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    EditViewModel.startEditing(schedule.id)
                    App.screen = ScreenType.EDIT
                },
                onLongClick = { menuExpanded = true },
            )
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        schedule.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (schedule.enabled)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isNext) {
                        WidthSpacer(8.dp)
                        NextBadge()
                    }
                }
                HeightSpacer(2.dp)
                Text(
                    App.format(schedule.getNextTriggerTime(now)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    schedule.timeConfig,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = schedule.enabled,
                onCheckedChange = { checked -> ScheduleService.setEnabled(schedule.id, checked) },
            )

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Remove") },
                    leadingIcon = { Icon(ImageVector.vectorResource(R.drawable.ic_remove), null) },
                    onClick = {
                        menuExpanded = false
                        ScheduleService.scheduleList.removeIf { it.id == schedule.id }
                        ScheduleService.saveAndRefresh()
                    },
                )
            }
        }
    }
}

@Composable
private fun NextBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            "NEXT",
            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun NavigationBottomBar(currentScreen: ScreenType) {
    NavigationBar {
        val items = listOf(ScreenType.HOME, ScreenType.SETTINGS)
        val icons = listOf(R.drawable.ic_home, R.drawable.ic_settings)

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = item == currentScreen,
                onClick = { App.screen = item },
                label = { Text(getScreenName(item)) },
                icon = { Icon(ImageVector.vectorResource(icons[index]), getScreenName(item)) },
            )
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    if (ScheduleService.scheduleList.isEmpty()) {
        ScheduleService.scheduleList.addAll(
            listOf(Schedule(name = "Alarm1"), Schedule(name = "Alarm2"))
        )
    }
    JeekAlarmTheme("Dark") {
        HomeScreen()
    }
}
