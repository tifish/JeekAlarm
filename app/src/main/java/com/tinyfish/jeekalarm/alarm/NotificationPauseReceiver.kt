package com.tinyfish.jeekalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.schedule.ScheduleHome

class NotificationPauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (App.isPlaying.value)
            ScheduleHome.pausePlaying()
        else
            ScheduleHome.resumePlaying()

        NotificationHome.updateAlarm()
    }
}
