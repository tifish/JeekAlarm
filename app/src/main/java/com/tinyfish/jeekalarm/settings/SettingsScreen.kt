package com.tinyfish.jeekalarm.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.PermissionsService
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.alarm.AlarmRingingService
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.home.NavigationBottomBar
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType
import com.tinyfish.ui.HeightSpacer
import com.tinyfish.ui.MyFileSelector
import com.tinyfish.ui.MyGroupBox
import com.tinyfish.ui.MyTopBar
import com.tinyfish.ui.Observe
import com.tinyfish.ui.SimpleTextField
import com.tinyfish.ui.WidthSpacer

@Composable
fun SettingsScreen() {
    Scaffold(topBar = { MyTopBar(R.drawable.ic_settings, "Settings") }, content = {
        Surface(
            modifier = Modifier.padding(it),
        ) {
            Editor()
        }
    }, bottomBar = { NavigationBottomBar(ScreenType.SETTINGS) })
}

@Composable
private fun Editor() {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 15.dp, end = 15.dp)
    ) {
        HeightSpacer()

        MyGroupBox {
            Text("Theme:")
            Observe {
                Row(
                    modifier = Modifier.padding(start = 20.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    val options = listOf("Auto", "Dark", "Light")
                    options.forEach {
                        val onClick = {
                            SettingsService.theme = it
                        }
                        RadioButton(selected = SettingsService.theme == it, onClick = onClick)
                        Text(it, Modifier.clickable(onClick = onClick))
                        WidthSpacer()
                    }
                }
            }
        }

        HeightSpacer()

        PermissionHealthGroup()

        HeightSpacer()

        MyGroupBox {
            Observe {
                MyFileSelector("Music File:", SettingsService.defaultMusicFile, onSelect = {
                    FileSelector.openMusicFile {
                        SettingsService.defaultMusicFile = it.toString()
                        SettingsService.save()
                    }
                }, onClear = {
                    SettingsService.defaultMusicFile = ""
                })
            }

            HeightSpacer()
            Observe {
                MyFileSelector("Music Folder:", SettingsService.defaultMusicFolder, onSelect = {
                    FileSelector.openFolder {
                        SettingsService.defaultMusicFolder = it.toString()
                    }
                }, onClear = {
                    SettingsService.defaultMusicFolder = ""
                })
            }
        }

        HeightSpacer()

        Button(
            onClick = {
                AlarmRingingService.start(context = App.context, alarmIds = ScheduleService.nextAlarmIds)
            }, Modifier.padding(5.dp)
        ) {
            Text("Test Next Alarm")
        }

        HeightSpacer()

        MyGroupBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Default AI: ")
                Observe {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = SettingsService.defaultAi == "OpenAI",
                            onClick = {
                                SettingsService.defaultAi = "OpenAI"
                            }
                        )
                        Text("OpenAI")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = SettingsService.defaultAi == "Gemini",
                            onClick = {
                                SettingsService.defaultAi = "Gemini"
                            }
                        )
                        Text("Gemini")
                    }
                }
            }

            Observe {
                SimpleTextField("OpenAI API URL: ", SettingsService.openAiApiUrl, onTextChanged = {
                    SettingsService.openAiApiUrl = it.trim()
                })
            }
            HeightSpacer()

            Observe {
                SimpleTextField("OpenAI API Model: ", SettingsService.openAiApiModel, onTextChanged = {
                    SettingsService.openAiApiModel = it.trim()
                })
            }
            HeightSpacer()

            Observe {
                SimpleTextField("OpenAI API key: ", SettingsService.openAiApiKey, onTextChanged = {
                    SettingsService.openAiApiKey = it.trim()
                })
            }
            HeightSpacer()

            Observe {
                SimpleTextField("Gemini API key: ", SettingsService.geminiKey, onTextChanged = {
                    SettingsService.geminiKey = it.trim()
                })
            }

            Observe {
                SimpleTextField("IFly APP ID: ", SettingsService.iFlyAppId, onTextChanged = {
                    SettingsService.iFlyAppId = it.trim()
                })
            }
            HeightSpacer()
        }

        HeightSpacer()

        MyGroupBox {
            Observe {
                val fileSelectScope = currentRecomposeScope
                val context = LocalContext.current

                val configDir =
                    if (LocalInspectionMode.current)
                        ""
                    else
                        SettingsService.settingsDir

                MyFileSelector("Config Folder:", configDir, onSelect = {
                    FileSelector.openFolder {
                        SettingsService.settingsDir = it.toString()
                        fileSelectScope.invalidate()
                        onConfigDirChanged(context)
                    }
                }, onClear = {
                    SettingsService.settingsDir = ""
                    fileSelectScope.invalidate()
                    onConfigDirChanged(context)
                })
            }
        }

        HeightSpacer()
    }
}

@Composable
private fun PermissionHealthGroup() {
    App.permissionChangedTrigger

    val context = LocalContext.current
    val activity = context as? Activity
    val inspectionMode = LocalInspectionMode.current
    val notificationAllowed = inspectionMode || PermissionsService.canPostNotifications(context)
    val exactAlarmAllowed = inspectionMode || PermissionsService.canScheduleExactAlarms()
    val batteryUnrestricted = inspectionMode || PermissionsService.isIgnoringBatteryOptimizations(context)

    MyGroupBox {
        Text("Permissions:")
        HeightSpacer()

        PermissionStatusRow(
            name = "Notifications",
            status = if (notificationAllowed) "Allowed" else "Required",
            actionText = if (notificationAllowed) "Settings" else "Allow",
            onClick = {
                if (activity != null && !notificationAllowed)
                    PermissionsService.requestNotificationPermission(activity)
                else
                    PermissionsService.openAppNotificationSettings(context)
            }
        )

        HeightSpacer()

        PermissionStatusRow(
            name = "Exact alarms",
            status = if (exactAlarmAllowed) "Allowed" else "Required",
            actionText = if (exactAlarmAllowed) "Settings" else "Allow",
            onClick = {
                if (exactAlarmAllowed)
                    PermissionsService.openAppDetailsSettings(context)
                else
                    PermissionsService.requestExactAlarmPermission(context)
            }
        )

        HeightSpacer()

        PermissionStatusRow(
            name = "Battery optimization",
            status = if (batteryUnrestricted) "Unrestricted" else "May be restricted",
            actionText = if (batteryUnrestricted) "Settings" else "Allow",
            onClick = {
                if (batteryUnrestricted)
                    PermissionsService.openBatteryOptimizationSettings(context)
                else
                    PermissionsService.requestIgnoreBatteryOptimizations(context)
            }
        )

        HeightSpacer()

        PermissionStatusRow(
            name = "Autostart / lock screen",
            status = "Device setting",
            actionText = "Open",
            onClick = {
                PermissionsService.openAppDetailsSettings(context)
            }
        )
    }
}

@Composable
private fun PermissionStatusRow(
    name: String,
    status: String,
    actionText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f, true)) {
            Text(name)
            Text(status, style = MaterialTheme.typography.bodySmall)
        }

        WidthSpacer()

        Button(onClick = onClick) {
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

        AlertDialog.Builder(context).setMessage("Found config file in new location, load or overwrite them?")
            .setPositiveButton("Load", dialogClickListener).setNegativeButton("Overwrite", dialogClickListener).show()
    } else {
        SettingsService.save()
        ScheduleService.save()
    }
}

fun onSettingsScreenPressBack() {
    App.screen = ScreenType.HOME
}

@Preview
@Composable
fun SettingsScreenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        SettingsScreen()
    }
}
