package com.tinyfish.jeekalarm

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionsService {
    fun checkAndRequestExternalStoragePermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                READ_EXTERNAL_STORAGE,
                WRITE_EXTERNAL_STORAGE,
            ),
            666
        )
    }

    fun checkAndRequestAllFileAccessPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            return
        if (Environment.isExternalStorageManager())
            return

        val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                uri
            )
        )
    }
}
