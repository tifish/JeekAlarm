package com.tinyfish.jeekalarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
    const val InfoId = 1
    const val AlarmId = 2

    private fun initOnce() {
        val infoChannel = NotificationChannel(InfoChannel, InfoChannel, NotificationManager.IMPORTANCE_LOW).apply {
            description = "Next alarm status"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(infoChannel)

        val alarmChannel = NotificationChannel(AlarmChannel, AlarmChannel, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Ringing alarm notifications"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(alarmChannel)
    }

    fun updateInfo() {
        val notification = getInfoNotification()
        if (notification == null) {
            notificationManager.cancel(InfoId)
            return
        }

        notificationManager.notify(InfoId, notification)
    }

    private fun getInfoNotification(): Notification? {
        initOnce()

        val alarmIds = ScheduleService.nextAlarmIds
        if (alarmIds.isEmpty())
            return null

        val schedules = alarmIds.mapNotNull { ScheduleService.findSchedule(it) }
        if (schedules.isEmpty())
            return null

        val openPendingIntent = PendingIntent.getActivity(
            App.context,
            2,
            Intent(App.context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextAlarmDateString = App.format(schedules.first().getNextTriggerTime(Calendar.getInstance()))
        val alarmNames = schedules.joinToString("; ") { it.name }
        val infoText = "Next: $alarmNames $nextAlarmDateString"
        return NotificationCompat.Builder(App.context, InfoChannel).run {
            setContentTitle("JeekAlarm standby")
            setContentText(infoText)
            setStyle(NotificationCompat.BigTextStyle().bigText(infoText))
            setOngoing(true)
            setAutoCancel(false)
            setOnlyAlertOnce(true)
            priority = NotificationCompat.PRIORITY_LOW
            setCategory(NotificationCompat.CATEGORY_STATUS)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentIntent(openPendingIntent)
            build()
        }
    }

    val currentAlarmIds = mutableListOf<Int>()

    fun setCurrentAlarmIds(alarmIds: List<Int>) {
        currentAlarmIds.clear()
        currentAlarmIds.addAll(alarmIds)
    }

    fun getAlarmNotification(alarmIds: List<Int> = currentAlarmIds): Notification {
        initOnce()

        val openIntent = Intent(App.context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val openPendingIntent = PendingIntent.getActivity(
            App.context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pausePendingIntent = PendingIntent.getBroadcast(
            App.context,
            0,
            Intent(App.context, NotificationPauseReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPendingIntent = PendingIntent.getBroadcast(
            App.context,
            1,
            Intent(App.context, NotificationDismissReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmNames = getAlarmNames(alarmIds)
        val alarmText = alarmNames.joinToString("\n")
        return NotificationCompat.Builder(App.context, AlarmChannel).run {
            setContentTitle("JeekAlarm")
            setContentText(alarmNames.joinToString("; "))
            setStyle(NotificationCompat.BigTextStyle().bigText(alarmText))
            setOngoing(true)
            setAutoCancel(false)
            setOnlyAlertOnce(true)
            priority = NotificationCompat.PRIORITY_HIGH
            setCategory(NotificationCompat.CATEGORY_ALARM)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentIntent(openPendingIntent)
            setFullScreenIntent(openPendingIntent, true)
            setDeleteIntent(dismissPendingIntent)
            addAction(R.drawable.ic_pause, if (App.isPlaying) "Pause" else "Play", pausePendingIntent)
            addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
            build()
        }
    }

    fun updateAlarm() {
        if (currentAlarmIds.isEmpty())
            return

        notificationManager.notify(AlarmId, getAlarmNotification(currentAlarmIds))
    }

    private fun getAlarmNames(alarmIds: List<Int>): List<String> {
        return alarmIds.map { alarmId ->
            ScheduleService.findSchedule(alarmId)?.name ?: "Alarm #$alarmId"
        }
    }

    fun cancelAlarm() {
        if (App.isPlaying)
            ScheduleService.stopPlaying()

        notificationManager.cancel(AlarmId)

        if (App.screen == ScreenType.NOTIFICATION)
            App.screen = App.screenBeforeNotification

        currentAlarmIds.clear()
        AlarmRingingService.stop()
    }
}
