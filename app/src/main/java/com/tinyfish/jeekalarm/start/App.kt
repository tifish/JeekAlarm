package com.tinyfish.jeekalarm.start

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tinyfish.jeekalarm.ConfigService
import com.tinyfish.jeekalarm.alarm.NotificationService
import com.tinyfish.jeekalarm.schedule.ScheduleService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ScreenType {
    HOME,
    SETTINGS,
    EDIT,
    NOTIFICATION,
}

fun getScreenName(screenType: ScreenType): String {
    return when (screenType) {
        ScreenType.HOME -> "Home"
        ScreenType.SETTINGS -> "Settings"
        ScreenType.EDIT -> "Edit"
        ScreenType.NOTIFICATION -> "Notification"
    }
}

class App : Application() {
    companion object {
        lateinit var context: Context

        private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        fun format(calendar: Calendar?): String {
            return if (calendar == null) "" else simpleDateFormat.format(calendar.time)
        }

        var editScheduleId = -1
        var screenBeforeNotification = ScreenType.HOME

        var themeColorsChangeTrigger by mutableStateOf(0)

        var screen by mutableStateOf(ScreenType.HOME)
        var nextAlarmIds by mutableStateOf(listOf<Int>())
        var scheduleChangeTrigger by mutableStateOf(0)
        var isPlaying by mutableStateOf(false)
        var removingIndex by mutableStateOf(-1)
        var editOptionsChangeTrigger by mutableStateOf(0)
        var editTimeConfigChanged by mutableStateOf(0)

        fun startServiceAndUpdateInfo() {
            val serviceIntent = Intent(context, StartService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(serviceIntent)
            else
                context.startService(serviceIntent)

            NotificationService.updateInfo()
        }

        fun stopService() {
            val serviceIntent = Intent(context, StartService::class.java)
            context.stopService(serviceIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        context = applicationContext
        ConfigService.load()
        ScheduleService.load()
        ScheduleService.sort()
        ScheduleService.setNextAlarm()
    }
}
