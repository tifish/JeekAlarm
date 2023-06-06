package com.tinyfish.jeekalarm.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.tinyfish.jeekalarm.PermissionsService
import com.tinyfish.jeekalarm.alarm.NotificationService
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.edit.onEditScreenPressBack
import com.tinyfish.jeekalarm.settings.onSettingsScreenPressBack
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType


class MainActivity : AppCompatActivity() {
    companion object {
        var instance: MainActivity? = null
    }

    private fun showCurrentAlarms() {
        if (NotificationService.currentAlarmIds.isNotEmpty()) {
            if (App.screen != ScreenType.NOTIFICATION) {
                App.screenBeforeNotification = App.screen
                App.screen = ScreenType.NOTIFICATION
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        showCurrentAlarms()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        val permissions = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        PermissionsService.checkAndRequestPermissions(permissions, this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            PermissionsService.checkAndRequestAllFileAccessPermission(this)
        else
            PermissionsService.checkAndRequestExternalStoragePermission(this)

        FileSelector.init(this)

        showCurrentAlarms()

        setContent {
            MainUI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        instance = null
    }

    override fun onBackPressed() {
        when (App.screen) {
            ScreenType.HOME -> {
                super.onBackPressed()
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

}