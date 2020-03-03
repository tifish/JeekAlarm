package com.tinyfish.jeekalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tinyfish.jeekalarm.*
import com.tinyfish.jeekalarm.schedule.ScheduleManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "onReceive ${App.nowString}")

        Notification.showAlarm(ScheduleManager.nextAlarmIndexes)
    }
}
