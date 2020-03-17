package com.tinyfish.jeekalarm

import android.app.Application
import android.content.Context
import android.util.Log
import com.beust.klaxon.Klaxon
import com.tinyfish.jeekalarm.schedule.ScheduleManager
import com.tinyfish.jeekalarm.ui.GlobalState
import com.tinyfish.jeekalarm.ui.ScreenType
import java.text.SimpleDateFormat
import java.util.*


class App : Application() {
    companion object {
        lateinit var context: Context
        var json = Klaxon()

        private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        fun format(calendar: Calendar?): String {
            return if (calendar == null) "" else simpleDateFormat.format(calendar.time)
        }

        var editScheduleIndex = -1
        var screenBeforeNotification = ScreenType.MAIN
        val notificationAlarmIndexes = mutableListOf<Int>()

        val screen = GlobalState(ScreenType.MAIN)
        val nextAlarmIndexes = GlobalState(listOf<Int>())
        val scheduleChangeTrigger = GlobalState(0)
        val isPlaying = GlobalState(false)
        val isRemoving = GlobalState(false)

        fun bindComposer() {
            screen.createState()
            nextAlarmIndexes.createState()
            scheduleChangeTrigger.createState()
            isPlaying.createState()
            isRemoving.createState()
        }

        fun unbindComposer() {
            screen.destroyState()
            nextAlarmIndexes.destroyState()
            scheduleChangeTrigger.destroyState()
            isPlaying.destroyState()
            isRemoving.destroyState()
        }
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
