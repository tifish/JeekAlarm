package com.tinyfish.jeekalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tinyfish.jeekalarm.App
import java.util.*

object Alarm {
    private var alarmManager: AlarmManager? = null
    private var alarmIntent: PendingIntent? = null

    private fun initOnce() {
        if (alarmManager != null)
            return

        alarmManager = App.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(App.context, AlarmReceiver::class.java)
        alarmIntent = PendingIntent.getBroadcast(App.context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    fun setAlarm(calendar: Calendar) {
        initOnce()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
        } else {
            alarmManager?.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
        }
    }

    fun cancelAlarm() {
        if (alarmIntent != null) {
            alarmManager?.cancel(alarmIntent)
        }
    }
}
