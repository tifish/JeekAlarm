package com.tinyfish.jeekalarm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App

class AlarmRingingService : Service() {
    companion object {
        private const val ExtraAlarmIds = "com.tinyfish.jeekalarm.extra.RINGING_ALARM_IDS"
        private const val ExtraIsTest = "com.tinyfish.jeekalarm.extra.IS_TEST_ALARM"

        fun start(context: Context, alarmIds: List<Int>, isTest: Boolean = false) {
            if (alarmIds.isEmpty())
                return

            val intent = Intent(context, AlarmRingingService::class.java).apply {
                putExtra(ExtraAlarmIds, alarmIds.toIntArray())
                putExtra(ExtraIsTest, isTest)
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

        val isTest = intent?.getBooleanExtra(ExtraIsTest, false) == true
        // 只更新响铃状态；通知浮层由 UI 层观察 currentAlarmIds 自动弹出。
        // 非测试闹钟连续触发时合并保留未关闭的旧闹钟，避免先前的闹钟（尤其是一次性闹钟）被静默顶掉。
        val activeAlarmIds = NotificationService.setCurrentAlarmIds(alarmIds, isTest = isTest)
        startForeground(NotificationService.AlarmId, NotificationService.getAlarmNotification(activeAlarmIds))

        if (App.isPlaying)
            ScheduleService.stopPlaying()

        val schedules = alarmIds.mapNotNull { ScheduleService.findSchedule(it) }
        if (schedules.isEmpty()) {
            NotificationService.cancelAlarm()
            stopSelf(startId)
            return START_NOT_STICKY
        }

        Schedule.playCombined(schedules)
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
