package com.tinyfish.jeekalarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.home.MainActivity
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType
import java.util.Calendar


object NotificationService {
    private val notificationManager: NotificationManager by lazy {
        App.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    const val InfoChannel = "Info"
    const val AlarmChannel = "Alarm"

    private fun initOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                InfoChannel, InfoChannel, NotificationManager.IMPORTANCE_LOW
            )
            createNotificationChannel(AlarmChannel, AlarmChannel, NotificationManager.IMPORTANCE_HIGH)
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String, importance: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val channel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(channel)
    }

    const val InfoId = 1
    const val AlarmId = 2

    fun updateInfo() {
        notificationManager.notify(InfoId, getInfoNotification())
    }

    fun getInfoNotification(): Notification {
        initOnce()

        val openIntent = Intent(App.context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val openPendingIntent = PendingIntent.getActivity(
            App.context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var infoText = ""
        if (ScheduleService.nextAlarmIds.size > 0) {
            val alarmNames = getAlarmNames(ScheduleService.nextAlarmIds)
            val schedule = ScheduleService.scheduleList.filter { it.id in ScheduleService.nextAlarmIds }[0]
            val nextAlarmDateString =
                App.format(schedule.getNextTriggerTime(Calendar.getInstance()))
            infoText = "Next: ${alarmNames.joinToString("; ")} $nextAlarmDateString"
        }

        return NotificationCompat.Builder(App.context, InfoChannel).run {
            setContentTitle("JeekAlarm standby:")
            setContentText(infoText)
            setOngoing(true)
            setAutoCancel(false)
            priority = NotificationCompat.PRIORITY_LOW
            setCategory(NotificationCompat.CATEGORY_STATUS)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentIntent(openPendingIntent)
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

        val openIntent = Intent(App.context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("alarmIds", alarmIds.toIntArray())
        }
        val openPendingIntent = PendingIntent.getActivity(
            App.context, System.currentTimeMillis().toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(App.context, NotificationPauseReceiver::class.java)
        val pausePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            App.context, 0, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(App.context, NotificationDismissReceiver::class.java)
        val dismissPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            App.context, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmNames = getAlarmNames(alarmIds)
        val notification = NotificationCompat.Builder(App.context, AlarmChannel).run {
            setContentTitle("JeekAlarm triggered:")
            setContentText(alarmNames.joinToString("\n"))
            setOngoing(true)
            setAutoCancel(false)
            priority = NotificationCompat.PRIORITY_HIGH
            setCategory(NotificationCompat.CATEGORY_ALARM)
            setSmallIcon(R.drawable.ic_launcher_foreground)
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
                ScheduleService.saveAndRefresh()
            else
                ScheduleService.setNextAlarm()

            ScheduleService.sort()
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
        if (App.isPlaying)
            ScheduleService.stopPlaying()

        notificationManager.cancel(AlarmId)

        if (App.screen == ScreenType.NOTIFICATION)
            App.screen = App.screenBeforeNotification

        App.notificationAlarmIds.clear()
    }

}
