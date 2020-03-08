package com.tinyfish.jeekalarm

import android.app.Application
import android.content.Context
import android.util.Log
import com.beust.klaxon.Klaxon
import com.tinyfish.jeekalarm.alarm.Notification
import com.tinyfish.jeekalarm.schedule.ScheduleManager
import java.text.SimpleDateFormat
import java.util.*


class App : Application() {
    companion object {
        lateinit var context: Context
        var json = Klaxon()

        private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.CHINA)
        fun format(calendar: Calendar?): String {
            return if (calendar == null) "" else simpleDateFormat.format(calendar.time)
        }

        var editScheduleIndex = -1
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("App", "onCreate")

        context = applicationContext
        Config.load()
        ScheduleManager.loadConfig()
        ScheduleManager.setNextAlarm()

        //startService(Intent(this, PlayerService::class.java))
    }

    override fun onTerminate() {
        super.onTerminate()
        //stopService(Intent(this, com.tinyfish.jeekalarm.alarm.PlayerService::class.java))
    }

}
