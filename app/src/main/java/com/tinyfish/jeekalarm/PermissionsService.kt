package com.tinyfish.jeekalarm

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tinyfish.jeekalarm.alarm.AlarmService


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
        if (canManageExternalStorage())
            return

        val uri = Uri.parse("package:${context.packageName}")
        val appSettingsIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
        if (startSettingsActivity(context, appSettingsIntent))
            return

        startSettingsActivity(context, Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }

    fun canManageExternalStorage(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }

    fun checkAndRequestPermissions(permissions: Array<String>, activity: Activity) {
        val toApplyList = ArrayList<String>()

        for (perm in permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity, perm)) {
                toApplyList.add(perm)
            }
        }
        val tmpList = arrayOfNulls<String>(toApplyList.size)
        if (toApplyList.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, toApplyList.toArray(tmpList), 123)
        }
    }

    fun requestExactAlarmPermissionIfNeeded(activity: Activity) {
        AlarmService.requestExactAlarmPermission(activity)
    }

    fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications(activity)) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 124)
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return AlarmService.canScheduleExactAlarms()
    }

    fun requestExactAlarmPermission(context: Context) {
        AlarmService.requestExactAlarmPermission(context)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isIgnoringBatteryOptimizations(context))
            return

        val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        if (startSettingsActivity(context, requestIntent))
            return

        openBatteryOptimizationSettings(context)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        startSettingsActivity(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    fun openAppNotificationSettings(context: Context) {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                appDetailsIntent(context)
            }

        startSettingsActivity(context, intent)
    }

    fun openAppDetailsSettings(context: Context) {
        startSettingsActivity(context, appDetailsIntent(context))
    }

    private fun appDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    private fun startSettingsActivity(context: Context, intent: Intent): Boolean {
        val finalIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (finalIntent.resolveActivity(context.packageManager) == null)
            return false

        context.startActivity(finalIntent)
        return true
    }
}
