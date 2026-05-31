package com.tinyfish.jeekalarm.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.tinyfish.jeekalarm.PermissionsService
import com.tinyfish.jeekalarm.alarm.NotificationService
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.edit.onEditScreenPressBack
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.settings.onSettingsScreenPressBack
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType


class MainActivity : AppCompatActivity() {
    companion object {
        var instance: MainActivity? = null
    }

    private fun showCurrentAlarms() {
        if (NotificationService.currentAlarmIds.isNotEmpty()) {
            allowAlarmOverLockScreen()
            if (App.screen != ScreenType.NOTIFICATION) {
                App.screenBeforeNotification = App.screen
                App.screen = ScreenType.NOTIFICATION
            }
        }
    }

    private fun allowAlarmOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        showCurrentAlarms()
    }

    override fun onResume() {
        super.onResume()

        App.permissionChangedTrigger++
        ScheduleService.setNextAlarm()
        showCurrentAlarms()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)

        PermissionsService.checkAndRequestPermissions(permissions.toTypedArray(), this)
        PermissionsService.requestExactAlarmPermissionIfNeeded(this)

        FileSelector.init(this)

        showCurrentAlarms()

        onBackPressedDispatcher.addCallback(this) {
            when (App.screen) {
                ScreenType.HOME -> {
                    isEnabled = false
                    try {
                        onBackPressedDispatcher.onBackPressed()
                    } finally {
                        isEnabled = true
                    }
                }

                ScreenType.EDIT -> {
                    onEditScreenPressBack()
                }

                ScreenType.SETTINGS -> {
                    onSettingsScreenPressBack()
                }

                ScreenType.NOTIFICATION -> {
                }
            }
        }

        setContent {
            MainUI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        instance = null
    }
}
