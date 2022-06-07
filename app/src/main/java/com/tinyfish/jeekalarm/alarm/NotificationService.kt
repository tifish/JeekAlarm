package com.tinyfish.jeekalarm.alarm

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.home.MainActivity
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import java.util.*


object NotificationService {
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

    const val InformationId = 1
    const val AlarmId = 2

    fun showInformation() {
        notificationManager.notify(InformationId, getInformationNotification())
    }

    fun getInformationNotification(): Notification {
        initOnce()

        val intent = Intent(App.context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val pendingIntent = PendingIntent.getActivity(
            App.context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        var infoText = ""
        if (ScheduleService.nextAlarmIds.size > 0) {
            val alarmNames = getAlarmNames(ScheduleService.nextAlarmIds)
            val schedule = ScheduleService.scheduleList.filter { it.id in ScheduleService.nextAlarmIds }[0]
            val nextAlarmDateString =
                App.format(schedule.getNextTriggerTime(Calendar.getInstance()))
            infoText = "Next: ${alarmNames.joinToString("; ")} $nextAlarmDateString"
        }

        val bitmap = AppCompatResources.getDrawable(App.context, R.drawable.ic_launcher_foreground)
            ?.toBitmap()

        return NotificationCompat.Builder(App.context, "Information").run {
            setContentTitle("JeekAlarm standby:")
            setContentText(infoText)
            setOngoing(true)
            setAutoCancel(false)
            priority = NotificationCompat.PRIORITY_LOW
            setCategory(NotificationCompat.CATEGORY_STATUS)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setLargeIcon(bitmap)
            setContentIntent(pendingIntent)
            build()
        }
    }

    private var lastAlarmIds = mutableListOf<Int>()

    fun showAlarm(alarmIds: List<Int>, isUpdating: Boolean = false) {
        initOnce()

        if (!isUpdating) {
            lastAlarmIds.clear()
            lastAlarmIds.addAll(alarmIds)

            ScheduleService.scheduleList.filter { it.id in ScheduleService.nextAlarmIds }[0].play()
        }

        val openIntent = Intent(App.context, NotificationClickReceiver::class.java).apply {
            putExtra("alarmIds", alarmIds.toIntArray())
        }
        val openPendingIntent = PendingIntent.getBroadcast(
            App.context, 0, openIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val pauseIntent = Intent(App.context, NotificationPauseReceiver::class.java)
        val pausePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            App.context, 0, pauseIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val dismissIntent = Intent(App.context, NotificationDismissReceiver::class.java)
        val dismissPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            App.context, 0, dismissIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val bitmap = AppCompatResources.getDrawable(App.context, R.drawable.ic_launcher_foreground)?.toBitmap()
        val alarmNames = getAlarmNames(alarmIds)
        val notification = NotificationCompat.Builder(App.context, "Alarm").run {
            setContentTitle("JeekAlarm triggered:")
            setContentText(alarmNames.joinToString("\n"))
            setOngoing(true)
            setAutoCancel(true)
            priority = NotificationCompat.PRIORITY_HIGH
            setCategory(NotificationCompat.CATEGORY_ALARM)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setLargeIcon(bitmap)
            setContentIntent(openPendingIntent)
            addAction(R.drawable.ic_pause, if (App.isPlaying) "Pause" else "Play", pausePendingIntent)
            addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
            build()
        }

        notificationManager.notify(AlarmId, notification)

        if (!isUpdating) {
            var modified = false
            for (alarmId in ScheduleService.nextAlarmIds) {
                val schedule = ScheduleService.scheduleList.filter { schedule -> schedule.id == alarmId }[0]
                if (schedule.onlyOnce) {
                    schedule.enabled = false
                    modified = true
                }
            }

            if (modified)
                ScheduleService.saveConfig()

            ScheduleService.setNextAlarm()
        }
    }

    fun updateAlarm() {
        showAlarm(lastAlarmIds, true)
    }

    private fun getAlarmNames(alarmIds: List<Int>): List<String> {
        val alarmNames = mutableListOf<String>()
        for (alarmId in alarmIds) {
            val schedule = ScheduleService.scheduleList.filter { schedule -> schedule.id == alarmId }[0]
            alarmNames.add(schedule.name)
        }
        return alarmNames
    }

    fun cancelAlarm() {
        notificationManager.cancel(AlarmId)
    }

}
