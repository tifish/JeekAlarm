package com.tinyfish.jeekalarm.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.platform.setContent
import com.tinyfish.jeekalarm.PermissionsHome
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.edit.onEditScreenPressBack
import com.tinyfish.jeekalarm.settings.onSettingsScreenPressBack
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType

class MainActivity : AppCompatActivity() {
    companion object {
        var instance: MainActivity? = null
        var onFileSelect: (uri: Uri?) -> Unit = {}
    }

    @ExperimentalFoundationApi
    @ExperimentalFocus
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        PermissionsHome.checkExternalStoragePermission(this)

        setContent {
            MainUI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        instance = null
    }

    override fun onBackPressed() {
        if (App.screen == ScreenType.MAIN) {
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

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?
    ) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FileSelector.PICK_MP3_FILE ||
                requestCode == FileSelector.PICK_FOLDER
            ) {
                onFileSelect(resultData?.data)
            }
        }

        super.onActivityResult(requestCode, resultCode, resultData)
    }

}