package com.tinyfish.jeekalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tinyfish.jeekalarm.App
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.UI
import com.tinyfish.jeekalarm.main.MainActivity
import com.tinyfish.jeekalarm.schedule.ScheduleManager
import java.util.*

object Notification {
    private val notificationManager: NotificationManager by lazy {
        App.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun initOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                "Information", "Information", NotificationManager.IMPORTANCE_LOW
            )
            createNotificationChannel("Alarm", "Alarm", NotificationManager.IMPORTANCE_HIGH)
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String, importance: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val channel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(channel)
    }

    fun showInformation() {
        initOnce()

        val intent = Intent(App.context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val pendingIntent = PendingIntent.getActivity(App.context, 0, intent, 0)

        var infoText = ""
        if (ScheduleManager.nextAlarmIndexes.size > 0) {
            val alarmNames = getAlarmNames(ScheduleManager.nextAlarmIndexes)
            val schedule = ScheduleManager.scheduleList[ScheduleManager.nextAlarmIndexes[0]]
            val nextAlarmDateString =
                App.format(schedule.getNextTriggerTime(Calendar.getInstance()))
            infoText = "Next: ${alarmNames.joinToString("; ")} $nextAlarmDateString"
        }

        val notification =
            NotificationCompat.Builder(App.context, "Information").run {
                setContentTitle("JeekAlarm standby:")
                setContentText(infoText)
                setOngoing(true)
                setAutoCancel(false)
                priority = NotificationCompat.PRIORITY_LOW
                setCategory(NotificationCompat.CATEGORY_STATUS)
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setContentIntent(pendingIntent)
                build()
            }

        notificationManager.notify(1, notification)
    }

    var lastAlarmIndexes = mutableListOf<Int>()

    fun showAlarm(alarmIndexes: List<Int>, isUpdating: Boolean = false) {
        initOnce()

        if (!isUpdating) {
            lastAlarmIndexes.clear()
            lastAlarmIndexes.addAll(alarmIndexes)

            ScheduleManager.scheduleList[ScheduleManager.nextAlarmIndexes[0]].play()
        }

        val openIntent = Intent(App.context, NotificationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra("alarmIndexes", alarmIndexes.toIntArray())
        }
        val openPendingIntent = PendingIntent.getActivity(App.context, 0, openIntent, 0)

        val pauseIntent = Intent(App.context, NotificationPauseReceiver::class.java)
        val pausePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(App.context, 0, pauseIntent, 0)

        val dismissIntent = Intent(App.context, NotificationDismissReceiver::class.java)
        val dismissPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(App.context, 0, dismissIntent, 0)

        val alarmNames = getAlarmNames(alarmIndexes)
        val notification = NotificationCompat.Builder(App.context, "Alarm").run {
            setContentTitle("JeekAlarm triggered:")
            setContentText(alarmNames.joinToString("\n"))
            setOngoing(true)
            setAutoCancel(true)
            priority = NotificationCompat.PRIORITY_HIGH
            setCategory(NotificationCompat.CATEGORY_ALARM)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentIntent(openPendingIntent)
            addAction(
                R.drawable.ic_pause,
                if (UI.isPlaying.value) "Pause" else "Play",
                pausePendingIntent
            )
            addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
            build()
        }

        notificationManager.notify(2, notification)

        if (!isUpdating) {
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
    }

    fun updateAlarm() {
        showAlarm(lastAlarmIndexes, true)
    }

    private fun getAlarmNames(alarmIndexes: List<Int>): List<String> {
        val alarmNames = mutableListOf<String>()
        for (alarmIndex in alarmIndexes) {
            alarmNames.add(ScheduleManager.scheduleList[alarmIndex].name)
        }
        return alarmNames
    }

    fun cancelAlarm() {
        notificationManager.cancel(2)
    }

}