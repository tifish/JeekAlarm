package com.tinyfish.jeekalarm

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat

object PermissionsHome {
    private const val REQUEST_EXTERNAL_PERMISSION_CODE = 666

    private val PERMISSIONS_EXTERNAL_STORAGE = arrayOf(
        READ_EXTERNAL_STORAGE,
    )

    fun checkExternalStoragePermission(activity: Activity?): Boolean {
        val readStoragePermissionState = ContextCompat.checkSelfPermission(activity!!, READ_EXTERNAL_STORAGE)
        val externalStoragePermissionGranted = readStoragePermissionState == PackageManager.PERMISSION_GRANTED
        if (!externalStoragePermissionGranted) {
            requestPermissions(activity, PERMISSIONS_EXTERNAL_STORAGE, REQUEST_EXTERNAL_PERMISSION_CODE)
        }
        return externalStoragePermissionGranted
    }

}
