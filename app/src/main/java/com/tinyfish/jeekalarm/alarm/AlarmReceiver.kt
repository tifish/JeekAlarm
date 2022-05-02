package com.tinyfish.jeekalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tinyfish.jeekalarm.schedule.ScheduleService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationService.showAlarm(ScheduleService.nextAlarmIds)
    }
}
