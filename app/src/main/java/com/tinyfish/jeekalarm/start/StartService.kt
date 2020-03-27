package com.tinyfish.jeekalarm.start

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.tinyfish.jeekalarm.alarm.NotificationHome

class StartService : Service() {
    var isCreating = true

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NotificationHome.InformationId,
            NotificationHome.getInformationNotification()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isCreating) {
            isCreating = false
            return super.onStartCommand(intent, flags, startId)
        }

        if (intent?.action == "Start") {
            NotificationHome.showInformation()
        }
        if (intent?.action == "Stop") {
            stopForeground(true)
            stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
