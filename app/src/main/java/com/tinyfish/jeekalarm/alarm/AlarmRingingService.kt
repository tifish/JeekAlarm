package com.tinyfish.jeekalarm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType

class AlarmRingingService : Service() {
    companion object {
        private const val ExtraAlarmIds = "com.tinyfish.jeekalarm.extra.RINGING_ALARM_IDS"

        fun start(context: Context, alarmIds: List<Int>) {
            if (alarmIds.isEmpty())
                return

            val intent = Intent(context, AlarmRingingService::class.java).apply {
                putExtra(ExtraAlarmIds, alarmIds.toIntArray())
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context = App.context) {
            context.stopService(Intent(context, AlarmRingingService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmIds = intent?.getIntArrayExtra(ExtraAlarmIds)?.toList() ?: emptyList()
        if (alarmIds.isEmpty()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        NotificationService.setCurrentAlarmIds(alarmIds)
        if (App.screen != ScreenType.NOTIFICATION) {
            App.screenBeforeNotification = App.screen
            App.screen = ScreenType.NOTIFICATION
        }
        startForeground(NotificationService.AlarmId, NotificationService.getAlarmNotification(alarmIds))

        if (App.isPlaying)
            ScheduleService.stopPlaying()

        val schedule = alarmIds.asSequence().mapNotNull { ScheduleService.findSchedule(it) }.firstOrNull()
        if (schedule == null) {
            NotificationService.cancelAlarm()
            stopSelf(startId)
            return START_NOT_STICKY
        }

        schedule.play()
        NotificationService.updateAlarm()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        if (App.isPlaying)
            ScheduleService.stopPlaying()
        super.onDestroy()
    }
}
