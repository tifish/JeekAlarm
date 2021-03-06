package com.tinyfish.jeekalarm.start

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tinyfish.jeekalarm.ConfigHome
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import java.text.SimpleDateFormat
import java.util.*

enum class ScreenType {
    MAIN,
    EDIT,
    SETTINGS,
    NOTIFICATION,
}

class App : Application() {
    companion object {
        lateinit var context: Context

        val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        fun format(calendar: Calendar?): String {
            return if (calendar == null) "" else simpleDateFormat.format(calendar.time)
        }

        var editScheduleIndex = -1
        var screenBeforeNotification = ScreenType.MAIN
        val notificationAlarmIndexes = mutableListOf<Int>()

        var themeColorsChangeTrigger by mutableStateOf(0)

        var screen by mutableStateOf(ScreenType.MAIN)
        var nextAlarmIndexes by mutableStateOf(listOf<Int>())
        var scheduleChangeTrigger by mutableStateOf(0)
        var isPlaying by mutableStateOf(false)
        var removingIndex by mutableStateOf(-1)
        var editEnabledChangeTrigger by mutableStateOf(0)

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
