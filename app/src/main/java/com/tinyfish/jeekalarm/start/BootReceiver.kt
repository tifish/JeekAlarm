package com.tinyfish.jeekalarm.start

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.schedule.RecycleBinService
import com.tinyfish.jeekalarm.schedule.ScheduleService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SettingsService.load()
        ScheduleService.load()
        RecycleBinService.load()
        ScheduleService.setNextAlarm()
    }
}
