package com.tinyfish.jeekalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tinyfish.jeekalarm.*
import com.tinyfish.jeekalarm.schedule.ScheduleManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notification.showAlarm(ScheduleManager.nextAlarmIndexes)
    }
}
