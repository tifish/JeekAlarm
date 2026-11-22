package com.tinyfish.jeekalarm.edit

import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.alarm.NotificationService
import com.tinyfish.jeekalarm.schedule.ScheduleParser
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
import kotlin.math.roundToInt

@Composable
fun EditScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val stopPreviewIfNotRinging = {
        if (NotificationService.currentAlarmIds.isEmpty())
            ScheduleService.stopPlaying()
    }
    val leaveWithSave = {
        if (EditViewModel.editing.isValid) {
            EditViewModel.saveEditingSchedule()
            stopPreviewIfNotRinging()
            onNavigateBack()
        } else {
            Toast.makeText(context, context.getString(R.string.edit_invalid_schedule), Toast.LENGTH_SHORT).show()
        }
    }
    val leaveWithoutSave = {
        EditViewModel.cancelEditing()
        stopPreviewIfNotRinging()
        onNavigateBack()
    }

    // 返回键：新建且未改动、或当前时间配置非法时直接放弃返回，避免生成空白/损坏闹钟；否则保存并返回。
    BackHandler {
        if ((EditViewModel.isAdding && !EditViewModel.hasChanges) || !EditViewModel.editing.isValid) {
            leaveWithoutSave()
        } else {
            leaveWithSave()
        }
    }

    Scaffold(
        topBar = {
            MyTopBar(
                R.drawable.ic_edit,
                if (EditViewModel.isAdding) stringResource(R.string.add_alarm) else stringResource(R.string.edit_title_edit),
            )
        },
        bottomBar = {
            BottomBar(
                onApply = leaveWithSave,
                onCancel = leaveWithoutSave,
                onRemoved = leaveWithoutSave,
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
                stringResource(R.string.edit_enabled),
                schedule.enabled,
                { checked -> EditViewModel.update { it.copy(enabled = checked) } },
                subtitle = stringResource(R.string.edit_enabled_subtitle),
            )
            SettingSwitchRow(
                stringResource(R.string.edit_only_once),
                schedule.onlyOnce,
                { checked -> EditViewModel.update { it.copy(onlyOnce = checked) } },
                subtitle = stringResource(R.string.edit_only_once_subtitle),
            )
        }

        SectionCard(title = stringResource(R.string.edit_section_name)) {
            LabeledTextField(
                label = stringResource(R.string.edit_alarm_name),
                value = schedule.name,
                onValueChange = { name -> EditViewModel.update { it.copy(name = name) } },
            )

            if (SettingsService.openAiApiKey != "") {
                FilledTonalButton(onClick = { EditViewModel.guessFromName() }) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_access_time), null)
                    WidthSpacer(8.dp)
                    Text(stringResource(R.string.edit_guess_time))
                }
            }
        }

        SectionCard(title = stringResource(R.string.edit_section_schedule), icon = R.drawable.ic_access_time) {
            Text(
                stringResource(R.string.edit_cron_syntax),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!schedule.isValid) {
                Text(
                    stringResource(R.string.edit_invalid_schedule),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // 改任意时间字段时，顺带启用该闹钟
            CronTimeField(
                label = stringResource(R.string.edit_cron_hour),
                text = schedule.hourConfig,
                isError = !ScheduleParser.isValidField(Calendar.HOUR_OF_DAY, schedule.hourConfig),
            ) { value ->
                EditViewModel.update { it.copy(hourConfig = value, enabled = true) }
            }
            CronTimeField(
                label = stringResource(R.string.edit_cron_minute),
                text = schedule.minuteConfig,
                isError = !ScheduleParser.isValidField(Calendar.MINUTE, schedule.minuteConfig),
            ) { value ->
                EditViewModel.update { it.copy(minuteConfig = value, enabled = true) }
            }
            CronTimeField(
                label = stringResource(R.string.edit_cron_weekday),
                text = schedule.weekDayConfig,
                isError = !ScheduleParser.isValidField(Calendar.DAY_OF_WEEK, schedule.weekDayConfig),
            ) { value ->
                EditViewModel.update { it.copy(weekDayConfig = value, enabled = true) }
            }
            CronTimeField(
                label = stringResource(R.string.edit_cron_month),
                text = schedule.monthConfig,
                isError = !ScheduleParser.isValidField(Calendar.MONTH, schedule.monthConfig),
            ) { value ->
                EditViewModel.update { it.copy(monthConfig = value, enabled = true) }
            }
            CronTimeField(
                label = stringResource(R.string.edit_cron_day),
                text = schedule.dayConfig,
                isError = !ScheduleParser.isValidField(Calendar.DAY_OF_MONTH, schedule.dayConfig),
            ) { value ->
                EditViewModel.update { it.copy(dayConfig = value, enabled = true) }
            }
            CronTimeField(
                label = stringResource(R.string.edit_cron_year),
                text = schedule.yearConfig,
                isError = !ScheduleParser.isValidField(Calendar.YEAR, schedule.yearConfig),
            ) { value ->
                EditViewModel.update { it.copy(yearConfig = value, enabled = true) }
            }
        }

        SectionCard(title = stringResource(R.string.edit_section_sound)) {
            SettingSwitchRow(
                stringResource(R.string.edit_play_music),
                schedule.playMusic,
                { checked -> EditViewModel.update { it.copy(playMusic = checked) } },
            )

            if (schedule.playMusic) {
                MyFileSelector(stringResource(R.string.label_music_file), schedule.musicFile, onSelect = {
                    FileSelector.openMusicFile { uri ->
                        EditViewModel.update { it.copy(musicFile = uri.toString()) }
                    }
                }, onClear = {
                    EditViewModel.update { it.copy(musicFile = "") }
                })

                MyFileSelector(stringResource(R.string.label_music_folder), schedule.musicFolder, onSelect = {
                    FileSelector.openFolder { uri ->
                        EditViewModel.update { it.copy(musicFolder = uri.toString()) }
                    }
                }, onClear = {
                    EditViewModel.update { it.copy(musicFolder = "") }
                })
            }

            SettingSwitchRow(
                stringResource(R.string.edit_vibration),
                schedule.vibration,
                { checked -> EditViewModel.update { it.copy(vibration = checked) } },
            )

            if (schedule.vibration) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        pluralStringResource(R.plurals.edit_vibrate_count, schedule.vibrationCount, schedule.vibrationCount),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = schedule.vibrationCount.toFloat(),
                        onValueChange = { value ->
                            EditViewModel.update { it.copy(vibrationCount = value.roundToInt()) }
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
    var showRemoveConfirm by remember { mutableStateOf(false) }

    BottomAppBar(
        actions = {
            if (EditViewModel.isAdding) {
                LabeledAction(R.drawable.ic_cancel, stringResource(R.string.action_cancel)) {
                    onCancel()
                }
            } else {
                LabeledAction(R.drawable.ic_remove, stringResource(R.string.action_remove)) {
                    showRemoveConfirm = true
                }
            }

            LabeledAction(R.drawable.ic_access_time, stringResource(R.string.edit_action_now)) {
                EditViewModel.setEditingScheduleTime(Calendar.getInstance())
            }

            val isPlaying = App.isPlaying
            LabeledAction(
                if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play_arrow,
                if (isPlaying) stringResource(R.string.action_stop) else stringResource(R.string.action_play),
            ) {
                if (isPlaying) ScheduleService.stopPlaying()
                else EditViewModel.play()
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(if (EditViewModel.isAdding) stringResource(R.string.edit_fab_add) else stringResource(R.string.edit_fab_apply)) },
                icon = { Icon(ImageVector.vectorResource(R.drawable.ic_done), null) },
                onClick = onApply,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        },
    )

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text(stringResource(R.string.action_remove)) },
            text = { Text(stringResource(R.string.edit_dialog_remove_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveConfirm = false
                    ScheduleService.findSchedule(EditViewModel.editScheduleId)
                        ?.let { ScheduleService.recycle(it) }
                    onRemoved()
                }) { Text(stringResource(R.string.action_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text(stringResource(R.string.action_no)) }
            },
        )
    }
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
