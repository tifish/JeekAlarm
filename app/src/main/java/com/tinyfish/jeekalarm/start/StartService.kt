package com.tinyfish.jeekalarm.start

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.tinyfish.jeekalarm.alarm.NotificationService

class StartService : Service() {
    override fun onCreate() {
        super.onCreate()

        startForeground(
            NotificationService.InfoId,
            NotificationService.getInfoNotification()
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
