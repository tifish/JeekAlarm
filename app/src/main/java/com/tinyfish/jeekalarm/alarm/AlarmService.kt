package com.tinyfish.jeekalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import com.tinyfish.jeekalarm.start.App
import java.util.Calendar

object AlarmService {
    private val alarmManager: AlarmManager = App.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private var alarmIntent: PendingIntent? = null

    private fun initOnce() {
        if (alarmIntent != null)
            return

        val intent = Intent(App.context, AlarmReceiver::class.java)
        alarmIntent =
            PendingIntent.getBroadcast(
                App.context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
    }

    fun setAlarm(calendar: Calendar) {
        initOnce()
        AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent!!)
    }

    fun cancelAlarm() {
        if (alarmIntent != null) {
            alarmManager.cancel(alarmIntent)
        }
    }
}
