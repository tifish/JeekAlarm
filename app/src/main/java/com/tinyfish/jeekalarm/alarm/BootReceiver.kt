package com.tinyfish.jeekalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tinyfish.jeekalarm.App
import com.tinyfish.jeekalarm.schedule.ScheduleManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "onReceive ${App.nowString}")
        ScheduleManager.setNextAlarm()
    }
}
