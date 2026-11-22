package com.tinyfish.jeekalarm

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.tinyfish.jeekalarm.alarm.AlarmService

object PermissionsService {
    fun checkAndRequestPermissions(permissions: Array<String>, activity: Activity) {
        val toApplyList = permissions.filter { perm ->
            ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED
        }
        if (toApplyList.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, toApplyList.toTypedArray(), 123)
        }
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
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (isIgnoringBatteryOptimizations(context))
            return

        val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }
        if (startSettingsActivity(context, requestIntent))
            return

        openBatteryOptimizationSettings(context)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        startSettingsActivity(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        if (!startSettingsActivity(context, intent))
            openAppDetailsSettings(context)
    }

    fun openAppDetailsSettings(context: Context) {
        startSettingsActivity(context, appDetailsIntent(context))
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    fun openInstallUnknownAppsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = "package:${context.packageName}".toUri()
        }
        if (!startSettingsActivity(context, intent))
            openAppDetailsSettings(context)
    }

    private fun appDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
    }

    private fun startSettingsActivity(context: Context, intent: Intent): Boolean {
        val finalIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(finalIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
