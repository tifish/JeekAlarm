package com.tinyfish.jeekalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tinyfish.jeekalarm.schedule.ScheduleService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmIds = AlarmService.getAlarmIds(intent)
        if (alarmIds.isEmpty()) {
            ScheduleService.setNextAlarm()
            return
        }

        AlarmRingingService.start(context, alarmIds)
        ScheduleService.completeTriggeredAlarms(alarmIds)
    }
}
