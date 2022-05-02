package com.tinyfish.jeekalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tinyfish.jeekalarm.home.MainActivity
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.ScreenType

class NotificationClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmIds = intent.getIntArrayExtra("alarmIds")!!
        App.notificationAlarmIds.addAll(alarmIds.toList())

        if (App.screen != ScreenType.NOTIFICATION) {
            App.screenBeforeNotification = App.screen
            App.screen = ScreenType.NOTIFICATION
        }

        val openIntent = Intent(App.context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        if (MainActivity.instance != null) {
            MainActivity.instance!!.startActivity(openIntent)
        } else {
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            App.context.startActivity(openIntent)
        }
    }
}
