package com.tinyfish.jeekalarm.start

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.ui.material.ColorPalette
import com.beust.klaxon.Klaxon
import com.tinyfish.jeekalarm.ConfigHome
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import com.tinyfish.jeekalarm.ui.DarkColorPalette
import com.tinyfish.jeekalarm.ui.GlobalState
import com.tinyfish.jeekalarm.ui.LightColorPalette
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

        val theme = GlobalState<ColorPalette>(DarkColorPalette)
        fun setThemeFromConfig() {
            theme.value =
                when (ConfigHome.data.theme) {
                    "Dark" -> DarkColorPalette
                    "Light" -> LightColorPalette
                    else -> LightColorPalette
                }
        }

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

        fun startService() {
            val startIntent = Intent(context, StartService::class.java).apply {
                action = "Start"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(startIntent)
            else
                context.startService(startIntent)
        }

        fun stopService() {
            val startIntent = Intent(context, StartService::class.java).apply {
                action = "Stop"
            }
            context.startService(startIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        context = applicationContext
        ConfigHome.load()

        ScheduleHome.loadConfig()
        ScheduleHome.setNextAlarm()
    }

}
