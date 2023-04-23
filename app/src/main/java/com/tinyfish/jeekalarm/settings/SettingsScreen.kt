package com.tinyfish.jeekalarm.settings

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.ConfigService
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.alarm.NotificationService
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
    Scaffold(
        topBar = { MyTopBar(R.drawable.ic_settings, "Settings") },
        content = {
            Surface(
                modifier = Modifier.padding(it),
                color = MaterialTheme.colors.background,
            ) {
                Editor()
            }
        },
        bottomBar = { NavigationBottomBar(ScreenType.SETTINGS) }
    )
}

@Composable
private fun Editor() {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 15.dp, end = 15.dp)
    ) {
        HeightSpacer()

        MyGroupBox()
        {
            Text("Theme:")
            Observe {
                val themeScope = currentRecomposeScope
                Row(
                    modifier = Modifier.padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val options = listOf("Auto", "Dark", "Light")
                    options.forEach {
                        val onClick = {
                            ConfigService.data.theme = it
                            ConfigService.save()
                            App.themeColorsChangeTrigger++
                            themeScope.invalidate()
                        }
                        RadioButton(selected = ConfigService.data.theme == it, onClick = onClick)
                        Text(it, Modifier.clickable(onClick = onClick))
                        WidthSpacer()
                    }
                }
            }
        }

        HeightSpacer()

        MyGroupBox {
            Observe {
                val fileSelectScope = currentRecomposeScope
                MyFileSelector("Music File:",
                    ConfigService.data.defaultMusicFile,
                    onSelect = {
                        FileSelector.openMusicFile {
                            ConfigService.data.defaultMusicFile = it.path?.substringAfter(':')!!
                            ConfigService.save()
                            fileSelectScope.invalidate()
                        }
                    },
                    onClear = {
                        ConfigService.data.defaultMusicFile = ""
                        ConfigService.save()
                        fileSelectScope.invalidate()
                    }
                )
            }

            HeightSpacer()
            Observe {
                val fileSelectScope = currentRecomposeScope
                MyFileSelector("Music Folder:",
                    ConfigService.data.defaultMusicFolder,
                    onSelect = {
                        FileSelector.openFolder {
                            ConfigService.data.defaultMusicFolder = it.path?.substringAfter(':')!!
                            ConfigService.save()
                            fileSelectScope.invalidate()
                        }
                    },
                    onClear = {
                        ConfigService.data.defaultMusicFolder = ""
                        ConfigService.save()
                        fileSelectScope.invalidate()
                    }
                )
            }
        }

        HeightSpacer()

        Button(
            onClick = {
                NotificationService.showAlarm(ScheduleService.nextAlarmIds)
            },
            Modifier.padding(5.dp)
        ) {
            Text("Test Next Alarm")
        }

        HeightSpacer()

        MyGroupBox {
            var openAIApiKey by remember { mutableStateOf(TextFieldValue(ConfigService.data.openAIApiKey)) }
            SimpleTextField("OpenAI API key: ", openAIApiKey, onTextChanged = {
                openAIApiKey = it
                ConfigService.data.openAIApiKey = it.text
                ConfigService.save()
            })
            HeightSpacer()
        }

        MyGroupBox {
            Observe {
                val fileSelectScope = currentRecomposeScope
                val context = LocalContext.current

                MyFileSelector("Config Folder:",
                    ConfigService.configDir,
                    onSelect = {
                        FileSelector.openFolder {
                            ConfigService.configDir = it.path?.substringAfter(':')!!
                            fileSelectScope.invalidate()
                            onConfigDirChanged(context)
                        }
                    },
                    onClear = {
                        ConfigService.configDir = ""
                        fileSelectScope.invalidate()
                        onConfigDirChanged(context)
                    }
                )
            }
        }

        HeightSpacer()
    }
}

private fun onConfigDirChanged(context: Context) {
    if (ConfigService.configFile.exists() || ScheduleService.configFile.exists()) {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    ConfigService.load()
                    ScheduleService.loadAndRefresh()
                }

                DialogInterface.BUTTON_NEGATIVE -> {
                    ConfigService.save()
                    ScheduleService.save()
                }
            }
        }

        AlertDialog.Builder(context).setMessage("Found config file in new location, load or overwrite them?")
            .setPositiveButton("Load", dialogClickListener)
            .setNegativeButton("Overwrite", dialogClickListener)
            .show()
    } else {
        ConfigService.save()
        ScheduleService.save()
    }
}

fun onSettingsScreenPressBack() {
    ConfigService.save()
    App.screen = ScreenType.HOME
}
