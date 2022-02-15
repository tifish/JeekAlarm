package com.tinyfish.jeekalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App

class NotificationPauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (App.isPlaying)
            ScheduleService.pausePlaying()
        else
            ScheduleService.resumePlaying()

        NotificationService.updateAlarm()
    }
}
