package com.tinyfish.jeekalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.tinyfish.jeekalarm.home.MainActivity
import com.tinyfish.jeekalarm.start.App
import java.util.Calendar

object AlarmService {
    private const val RequestAlarm = 100
    private const val RequestShowAlarm = 101

    const val ActionFireAlarm = "com.tinyfish.jeekalarm.action.FIRE_ALARM"
    private const val ExtraAlarmIds = "com.tinyfish.jeekalarm.extra.ALARM_IDS"
    private const val ExtraTriggerAtMillis = "com.tinyfish.jeekalarm.extra.TRIGGER_AT_MILLIS"

    private val alarmManager: AlarmManager by lazy {
        App.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    fun setAlarm(calendar: Calendar, alarmIds: List<Int>): Boolean {
        if (alarmIds.isEmpty() || !canScheduleExactAlarms())
            return false

        val triggerAtMillis = calendar.timeInMillis
        val operation = alarmPendingIntent(
            alarmIds,
            triggerAtMillis,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val showIntent = Intent(App.context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            App.context,
            RequestShowAlarm,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                operation,
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(
            App.context,
            RequestAlarm,
            Intent(App.context, AlarmReceiver::class.java).setAction(ActionFireAlarm),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun getAlarmIds(intent: Intent): List<Int> {
        return intent.getIntArrayExtra(ExtraAlarmIds)?.toList() ?: emptyList()
    }

    fun getTriggerAtMillis(intent: Intent): Long {
        return intent.getLongExtra(ExtraTriggerAtMillis, 0L)
    }

    fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms())
            return

        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        if (intent.resolveActivity(context.packageManager) != null)
            context.startActivity(intent)
    }

    private fun alarmPendingIntent(
        alarmIds: List<Int>,
        triggerAtMillis: Long,
        flags: Int,
    ): PendingIntent {
        val intent = Intent(App.context, AlarmReceiver::class.java).apply {
            action = ActionFireAlarm
            putExtra(ExtraAlarmIds, alarmIds.toIntArray())
            putExtra(ExtraTriggerAtMillis, triggerAtMillis)
        }

        return PendingIntent.getBroadcast(App.context, RequestAlarm, intent, flags)
    }
}
