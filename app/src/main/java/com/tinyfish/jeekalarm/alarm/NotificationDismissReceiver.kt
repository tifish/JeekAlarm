package com.tinyfish.jeekalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tinyfish.jeekalarm.schedule.ScheduleManager

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ScheduleManager.stopPlaying()
        Notification.cancelAlarm()
    }
}
