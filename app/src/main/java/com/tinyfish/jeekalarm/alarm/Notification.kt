package com.tinyfish.jeekalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tinyfish.jeekalarm.App
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.UI
import com.tinyfish.jeekalarm.schedule.ScheduleManager

object Notification {
    private val notificationManager: NotificationManager by lazy { App.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private fun initOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createNotificationChannel("Alarm", "Alarm", NotificationManager.IMPORTANCE_HIGH)
        }
    }

    fun showAlarm(alarmIndexes: List<Int>) {
        Log.d("Notification", "showAlarm: $alarmIndexes")

        initOnce()

        val intent = Intent(App.context, NotificationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra("alarmIndexes", alarmIndexes.toIntArray())
        }
        val pendingIntent = PendingIntent.getActivity(App.context, 0, intent, 0)

        val alarmNames = mutableListOf<String>()
        for (alarmIndex in alarmIndexes) {
            alarmNames.add(ScheduleManager.scheduleList[alarmIndex].name)
        }

        val notification =
            NotificationCompat.Builder(App.context, "Alarm").run {
                setContentTitle("Alarm")
                setContentText(alarmNames.joinToString("\n"))
                setOngoing(true)
                setAutoCancel(true)
                priority = NotificationCompat.PRIORITY_HIGH
                setCategory(NotificationCompat.CATEGORY_ALARM)
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setContentIntent(pendingIntent)
                build()
            }

        notificationManager.notify(alarmIndexes[0], notification)

        onNotify()
    }

    private fun onNotify() {
        ScheduleManager.scheduleList[ScheduleManager.nextAlarmIndexes[0]].play()

        var modified = false
        for (alarmIndex in ScheduleManager.nextAlarmIndexes) {
            val schedule = ScheduleManager.scheduleList[alarmIndex]
            if (schedule.onlyOnce) {
                schedule.enabled = false
                modified = true
            }
        }
        if (modified)
            ScheduleManager.saveConfig()
        else
            ScheduleManager.setNextAlarm()
    }

    private fun createNotificationChannel(channelId: String, channelName: String, importance: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val channel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(channel)
    }
}