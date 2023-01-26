package com.tinyfish.jeekalarm.home

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.tinyfish.jeekalarm.PermissionsService
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.edit.onEditScreenPressBack
import com.tinyfish.jeekalarm.settings.onSettingsScreenPressBack
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType

class MainActivity : AppCompatActivity() {
    companion object {
        var instance: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            PermissionsService.checkAndRequestAllFileAccessPermission(this)
        else
            PermissionsService.checkAndRequestExternalStoragePermission(this)

        FileSelector.init(this)

        val alarmIds = intent.getIntArrayExtra("alarmIds")
        if (alarmIds != null) {
            App.notificationAlarmIds.addAll(alarmIds.toList())

            if (App.screen != ScreenType.NOTIFICATION) {
                App.screenBeforeNotification = App.screen
                App.screen = ScreenType.NOTIFICATION
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

    override fun onBackPressed() {
        if (App.screen == ScreenType.HOME) {
            if (App.removingIndex > -1)
                App.removingIndex = -1
            else
                super.onBackPressed()
        } else if (App.screen == ScreenType.EDIT) {
            onEditScreenPressBack()
        } else if (App.screen == ScreenType.SETTINGS) {
            onSettingsScreenPressBack()
        }
    }

}