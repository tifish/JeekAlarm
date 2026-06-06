package com.tinyfish.jeekalarm.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.tinyfish.jeekalarm.PermissionsService
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.alarm.AlarmRingingService
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.home.BottomTab
import com.tinyfish.jeekalarm.home.NavigationBottomBar
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.ui.LabeledTextField
import com.tinyfish.ui.MyFileSelector
import com.tinyfish.ui.MyTopBar
import com.tinyfish.ui.SectionCard
import com.tinyfish.ui.WidthSpacer
import com.tinyfish.ui.theme.JeekAlarmTheme

@Composable
fun SettingsScreen(
    onAdd: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    onOpenRecycleBin: () -> Unit,
) {
    Scaffold(
        topBar = { MyTopBar(R.drawable.ic_settings, stringResource(R.string.label_settings)) },
        bottomBar = {
            NavigationBottomBar(
                selected = BottomTab.SETTINGS,
                onNavigateHome = onNavigateHome,
                onNavigateSettings = onNavigateSettings,
                onAdd = onAdd,
            )
        },
    ) { padding ->
        Editor(Modifier.padding(padding), onOpenRecycleBin = onOpenRecycleBin)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Editor(modifier: Modifier = Modifier, onOpenRecycleBin: () -> Unit = {}) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(title = stringResource(R.string.settings_appearance)) {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyMedium)
            // 存储值固定用英文（参与持久化与比较），只本地化显示标签。
            val themes = listOf("Auto", "Dark", "Light")
            val themeLabels = listOf(
                stringResource(R.string.theme_auto),
                stringResource(R.string.theme_dark),
                stringResource(R.string.theme_light),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                themes.forEachIndexed { index, value ->
                    SegmentedButton(
                        selected = SettingsService.theme == value,
                        onClick = { SettingsService.theme = value },
                        shape = SegmentedButtonDefaults.itemShape(index, themes.size),
                    ) { Text(themeLabels[index]) }
                }
            }
        }

        SectionCard(title = stringResource(R.string.settings_language)) {
            // 存储值：""=跟随系统，"en"，"zh"；切换后立即生效（AppCompat 会重建界面）。
            val languages = listOf("", "en", "zh")
            val languageLabels = listOf(
                stringResource(R.string.language_system),
                stringResource(R.string.language_english),
                stringResource(R.string.language_chinese),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                languages.forEachIndexed { index, value ->
                    SegmentedButton(
                        selected = SettingsService.language == value,
                        onClick = {
                            SettingsService.language = value
                            App.applyStoredLanguage()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, languages.size),
                    ) { Text(languageLabels[index]) }
                }
            }
        }

        FilledTonalButton(
            onClick = onOpenRecycleBin,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(ImageVector.vectorResource(R.drawable.ic_delete), null)
            WidthSpacer(8.dp)
            Text(stringResource(R.string.label_recycle_bin))
        }

        PermissionHealthGroup()

        SectionCard(title = stringResource(R.string.settings_default_sound)) {
            MyFileSelector(stringResource(R.string.label_music_file), SettingsService.defaultMusicFile, onSelect = {
                FileSelector.openMusicFile { SettingsService.defaultMusicFile = it.toString() }
            }, onClear = {
                SettingsService.defaultMusicFile = ""
            })

            MyFileSelector(stringResource(R.string.label_music_folder), SettingsService.defaultMusicFolder, onSelect = {
                FileSelector.openFolder { SettingsService.defaultMusicFolder = it.toString() }
            }, onClear = {
                SettingsService.defaultMusicFolder = ""
            })
        }

        FilledTonalButton(
            onClick = {
                AlarmRingingService.start(context = App.context, alarmIds = ScheduleService.nextAlarmIds)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(ImageVector.vectorResource(R.drawable.ic_play_arrow), null)
            WidthSpacer(8.dp)
            Text(stringResource(R.string.action_test_alarm))
        }

        SectionCard(title = stringResource(R.string.settings_ai_assistant)) {
            LabeledTextField(stringResource(R.string.settings_openai_url), SettingsService.openAiApiUrl) {
                SettingsService.openAiApiUrl = it.trim()
            }
            LabeledTextField(stringResource(R.string.settings_openai_model), SettingsService.openAiApiModel) {
                SettingsService.openAiApiModel = it.trim()
            }
            LabeledTextField(stringResource(R.string.settings_openai_key), SettingsService.openAiApiKey) {
                SettingsService.openAiApiKey = it.trim()
            }
            LabeledTextField(stringResource(R.string.settings_ifly_appid), SettingsService.iFlyAppId) {
                SettingsService.iFlyAppId = it.trim()
            }
        }

        ConfigFolderGroup()
    }
}

@Composable
private fun ConfigFolderGroup() {
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current

    var configDir by remember {
        mutableStateOf(if (inspectionMode) "" else SettingsService.settingsDir)
    }

    SectionCard(title = stringResource(R.string.settings_config_folder)) {
        MyFileSelector(stringResource(R.string.label_folder), configDir, onSelect = {
            FileSelector.openFolder {
                SettingsService.settingsDir = it.toString()
                configDir = it.toString()
                onConfigDirChanged(context)
            }
        }, onClear = {
            SettingsService.settingsDir = ""
            configDir = ""
            onConfigDirChanged(context)
        })
    }
}

@Composable
private fun PermissionHealthGroup() {
    val context = LocalContext.current
    val activity = context as? Activity
    val inspectionMode = LocalInspectionMode.current

    var refreshKey by remember { mutableIntStateOf(0) }
    if (!inspectionMode) {
        // 每次回到前台重新查询系统权限状态（用户可能刚从系统设置返回）
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            refreshKey++
        }
    }

    val notificationAllowed = remember(refreshKey) {
        inspectionMode || PermissionsService.canPostNotifications(context)
    }
    val exactAlarmAllowed = remember(refreshKey) {
        inspectionMode || PermissionsService.canScheduleExactAlarms()
    }
    val batteryUnrestricted = remember(refreshKey) {
        inspectionMode || PermissionsService.isIgnoringBatteryOptimizations(context)
    }

    SectionCard(title = stringResource(R.string.settings_permissions)) {
        PermissionStatusRow(
            name = stringResource(R.string.perm_notifications),
            allowed = notificationAllowed,
            status = if (notificationAllowed) stringResource(R.string.perm_status_allowed) else stringResource(R.string.perm_status_required),
            actionText = if (notificationAllowed) stringResource(R.string.label_settings) else stringResource(R.string.action_allow),
            onClick = {
                if (activity != null && !notificationAllowed)
                    PermissionsService.requestNotificationPermission(activity)
                else
                    PermissionsService.openAppNotificationSettings(context)
            },
        )

        PermissionStatusRow(
            name = stringResource(R.string.perm_exact_alarms),
            allowed = exactAlarmAllowed,
            status = if (exactAlarmAllowed) stringResource(R.string.perm_status_allowed) else stringResource(R.string.perm_status_required),
            actionText = if (exactAlarmAllowed) stringResource(R.string.label_settings) else stringResource(R.string.action_allow),
            onClick = {
                if (exactAlarmAllowed)
                    PermissionsService.openAppDetailsSettings(context)
                else
                    PermissionsService.requestExactAlarmPermission(context)
            },
        )

        PermissionStatusRow(
            name = stringResource(R.string.perm_battery_optimization),
            allowed = batteryUnrestricted,
            status = if (batteryUnrestricted) stringResource(R.string.perm_status_unrestricted) else stringResource(R.string.perm_status_may_restricted),
            actionText = if (batteryUnrestricted) stringResource(R.string.label_settings) else stringResource(R.string.action_allow),
            onClick = {
                if (batteryUnrestricted)
                    PermissionsService.openBatteryOptimizationSettings(context)
                else
                    PermissionsService.requestIgnoreBatteryOptimizations(context)
            },
        )

        PermissionStatusRow(
            name = stringResource(R.string.perm_autostart_lockscreen),
            allowed = null,
            status = stringResource(R.string.perm_status_device_setting),
            actionText = stringResource(R.string.action_open),
            onClick = {
                PermissionsService.openAppDetailsSettings(context)
            },
        )
    }
}

@Composable
private fun PermissionStatusRow(
    name: String,
    allowed: Boolean?,
    status: String,
    actionText: String,
    onClick: () -> Unit,
) {
    val statusColor = when (allowed) {
        true -> MaterialTheme.colorScheme.primary
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (allowed == true) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_done),
                        null,
                        Modifier.size(16.dp),
                        tint = statusColor,
                    )
                    WidthSpacer(4.dp)
                }
                Text(status, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
        }

        WidthSpacer()

        TextButton(onClick = onClick) {
            Text(actionText)
        }
    }
}

private fun onConfigDirChanged(context: Context) {
    if (SettingsService.configExists("config.json") || SettingsService.configExists("schedule.cron")) {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    SettingsService.load()
                    ScheduleService.loadAndRefresh()
                }

                DialogInterface.BUTTON_NEGATIVE -> {
                    SettingsService.save()
                    ScheduleService.save()
                }
            }
        }

        AlertDialog.Builder(context).setMessage(context.getString(R.string.dialog_config_found))
            .setPositiveButton(context.getString(R.string.action_load), dialogClickListener)
            .setNegativeButton(context.getString(R.string.action_overwrite), dialogClickListener).show()
    } else {
        SettingsService.save()
        ScheduleService.save()
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    JeekAlarmTheme("Dark") {
        SettingsScreen(onAdd = {}, onNavigateHome = {}, onNavigateSettings = {}, onOpenRecycleBin = {})
    }
}
