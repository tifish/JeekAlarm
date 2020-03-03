package com.tinyfish.jeekalarm.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.ui.core.setContent
import com.tinyfish.jeekalarm.Permissions
import com.tinyfish.jeekalarm.ScreenType
import com.tinyfish.jeekalarm.UI
import com.tinyfish.jeekalarm.edit.FileSelector

class MainActivity : AppCompatActivity() {
    companion object {
        var instance: MainActivity? = null
        var onFileSelect: (uri: Uri?) -> Unit = {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        Permissions.checkExternalStoragePermission(this)

        setContent {
            UI.init()

            Main()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        instance = null
    }

    override fun onBackPressed() {
        if (UI.screen == ScreenType.MAIN)
            super.onBackPressed()
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