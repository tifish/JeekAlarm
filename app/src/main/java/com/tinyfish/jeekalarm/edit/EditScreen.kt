package com.tinyfish.jeekalarm.edit

import android.app.AlertDialog
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.ui.LabeledTextField
import com.tinyfish.ui.MyFileSelector
import com.tinyfish.ui.MyTopBar
import com.tinyfish.ui.SectionCard
import com.tinyfish.ui.SettingSwitchRow
import com.tinyfish.ui.WidthSpacer
import com.tinyfish.ui.theme.JeekAlarmTheme
import java.util.Calendar

@Composable
fun EditScreen(onNavigateBack: () -> Unit) {
    // 离开编辑页（FAB 应用 / 系统返回键）都先保存再返回，保持原有行为。
    val leaveWithSave = {
        EditViewModel.saveEditingSchedule()
        ScheduleService.stopPlaying()
        onNavigateBack()
    }

    BackHandler { leaveWithSave() }

    Scaffold(
        topBar = {
            MyTopBar(
                R.drawable.ic_edit,
                if (EditViewModel.isAdding) "Add alarm" else "Edit alarm",
            )
        },
        bottomBar = {
            BottomBar(
                onApply = leaveWithSave,
                onCancel = onNavigateBack,
                onRemoved = onNavigateBack,
            )
        },
    ) { padding ->
        Editor(Modifier.padding(padding))
    }
}

@Composable
private fun Editor(modifier: Modifier = Modifier) {
    val schedule = EditViewModel.editing

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard {
            SettingSwitchRow(
                "Enabled",
                schedule.enabled,
                { checked -> EditViewModel.update { it.copy(enabled = checked) } },
                subtitle = "Turn this alarm on or off",
            )
            SettingSwitchRow(
                "Only once",
                schedule.onlyOnce,
                { checked -> EditViewModel.update { it.copy(onlyOnce = checked) } },
                subtitle = "Auto-disable after it rings once",
            )
        }

        SectionCard(title = "Name") {
            LabeledTextField(
                label = "Alarm name",
                value = schedule.name,
                onValueChange = { name -> EditViewModel.update { it.copy(name = name) } },
            )

            if (SettingsService.openAiApiKey != "") {
                FilledTonalButton(onClick = { EditViewModel.guessFromName() }) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_access_time), null)
                    WidthSpacer(8.dp)
                    Text("Guess time from name")
                }
            }
        }

        SectionCard(title = "Schedule", icon = R.drawable.ic_access_time) {
            Text(
                "Cron syntax — *: any · 0: fixed · 1-3: range · 1,3: list · */3: step",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 改任意时间字段时，顺带启用该闹钟
            CronTimeField("Hour", schedule.hourConfig) { value ->
                EditViewModel.update { it.copy(hourConfig = value, enabled = true) }
            }
            CronTimeField("Minute", schedule.minuteConfig) { value ->
                EditViewModel.update { it.copy(minuteConfig = value, enabled = true) }
            }
            CronTimeField("Weekday", schedule.weekDayConfig) { value ->
                EditViewModel.update { it.copy(weekDayConfig = value, enabled = true) }
            }
            CronTimeField("Month", schedule.monthConfig) { value ->
                EditViewModel.update { it.copy(monthConfig = value, enabled = true) }
            }
            CronTimeField("Day", schedule.dayConfig) { value ->
                EditViewModel.update { it.copy(dayConfig = value, enabled = true) }
            }
            CronTimeField("Year", schedule.yearConfig) { value ->
                EditViewModel.update { it.copy(yearConfig = value, enabled = true) }
            }
        }

        SectionCard(title = "Sound & vibration") {
            SettingSwitchRow(
                "Play music",
                schedule.playMusic,
                { checked -> EditViewModel.update { it.copy(playMusic = checked) } },
            )

            if (schedule.playMusic) {
                MyFileSelector("Music file", schedule.musicFile, onSelect = {
                    FileSelector.openMusicFile { uri ->
                        EditViewModel.update { it.copy(musicFile = uri.toString()) }
                    }
                }, onClear = {
                    EditViewModel.update { it.copy(musicFile = "") }
                })

                MyFileSelector("Music folder", schedule.musicFolder, onSelect = {
                    FileSelector.openFolder { uri ->
                        EditViewModel.update { it.copy(musicFolder = uri.toString()) }
                    }
                }, onClear = {
                    EditViewModel.update { it.copy(musicFolder = "") }
                })
            }

            SettingSwitchRow(
                "Vibration",
                schedule.vibration,
                { checked -> EditViewModel.update { it.copy(vibration = checked) } },
            )

            if (schedule.vibration) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Vibrate ${schedule.vibrationCount} times",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = schedule.vibrationCount.toFloat(),
                        onValueChange = { value ->
                            EditViewModel.update { it.copy(vibrationCount = value.toInt()) }
                        },
                        valueRange = 1f..30f,
                        steps = 28,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(
    onApply: () -> Unit,
    onCancel: () -> Unit,
    onRemoved: () -> Unit,
) {
    val context = LocalContext.current

    BottomAppBar(
        actions = {
            if (EditViewModel.isAdding) {
                LabeledAction(R.drawable.ic_cancel, "Cancel") {
                    onCancel()
                }
            } else {
                LabeledAction(R.drawable.ic_remove, "Remove") {
                    AlertDialog.Builder(context)
                        .setTitle("Remove")
                        .setMessage("Remove this schedule?")
                        .setPositiveButton("Yes") { _, _ ->
                            ScheduleService.findSchedule(EditViewModel.editScheduleId)
                                ?.let { ScheduleService.recycle(it) }
                            onRemoved()
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }

            LabeledAction(R.drawable.ic_access_time, "Now") {
                EditViewModel.setEditingScheduleTime(Calendar.getInstance())
            }

            val isPlaying = App.isPlaying
            LabeledAction(
                if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play_arrow,
                if (isPlaying) "Stop" else "Play",
            ) {
                if (isPlaying) ScheduleService.stopPlaying()
                else EditViewModel.play()
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(if (EditViewModel.isAdding) "Add" else "Apply") },
                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_done), null) },
                onClick = onApply,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        },
    )
}

/** 底部栏的图标 + 文字按钮，文字在图标下方，便于理解按钮含义。 */
@Composable
private fun LabeledAction(
    @DrawableRes iconID: Int,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(ImageVector.vectorResource(iconID), null)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Preview
@Composable
fun EditScreenPreview() {
    JeekAlarmTheme("Dark") {
        EditScreen(onNavigateBack = {})
    }
}
