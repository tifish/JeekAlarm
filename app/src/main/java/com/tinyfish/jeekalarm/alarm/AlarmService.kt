package com.tinyfish.jeekalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tinyfish.jeekalarm.start.App
import java.util.*

object AlarmService {
    private var alarmManager: AlarmManager? = null
    private var alarmIntent: PendingIntent? = null

    private fun initOnce() {
        if (alarmManager != null)
            return

        alarmManager = App.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(App.context, AlarmReceiver::class.java)
        alarmIntent =
            PendingIntent.getBroadcast(
                App.context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
    }

    fun setAlarm(calendar: Calendar) {
        initOnce()
        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            alarmIntent
        )
    }

    fun cancelAlarm() {
        if (alarmIntent != null) {
            alarmManager?.cancel(alarmIntent)
        }
    }
}
