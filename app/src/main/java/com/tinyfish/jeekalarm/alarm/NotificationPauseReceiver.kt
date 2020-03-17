package com.tinyfish.jeekalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tinyfish.jeekalarm.App
import com.tinyfish.jeekalarm.schedule.ScheduleManager

class NotificationPauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (App.isPlaying.value)
            ScheduleManager.pausePlaying()
        else
            ScheduleManager.resumePlaying()

        Notification.updateAlarm()
    }
}
