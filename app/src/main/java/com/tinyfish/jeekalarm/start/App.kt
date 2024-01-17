package com.tinyfish.jeekalarm.start

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tinyfish.jeekalarm.SettingsService
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
        // Only 1 instance, leak is not a problem
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        private val dateFormat = SimpleDateFormat("MM-dd", Locale.US)
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        private val weekFormat = SimpleDateFormat("EEE", Locale.US)

        fun format(calendar: Calendar?): String {
            if (calendar == null)
                return ""

            val now = Calendar.getInstance()
            val calendarYear = calendar.get(Calendar.YEAR)
            val sameYear = now.get(Calendar.YEAR) == calendarYear
            val today = now.get(Calendar.DAY_OF_YEAR)
            val tomorrow = today + 1
            val dat = tomorrow + 1
            val calendarDay = calendar.get(Calendar.DAY_OF_YEAR)
            val isToday = sameYear && calendarDay == today
            val isTomorrow = sameYear && calendarDay == tomorrow
            val isDat = sameYear && calendarDay == dat

            var dateString = dateFormat.format(calendar.time)
            if (isToday)
                dateString = "Today"
            else if (isTomorrow)
                dateString = "Tmr"
            else if (isDat)
                dateString = "DAT"
            else if (!sameYear)
                dateString = "$calendarYear-$dateString"

            val weekString = weekFormat.format(calendar.time)

            val timeString = timeFormat.format(calendar.time)

            return "$dateString $timeString $weekString"
        }

        var screenBeforeNotification = ScreenType.HOME

        var screen by mutableStateOf(ScreenType.HOME)
        var nextAlarmIds by mutableStateOf(listOf<Int>())
        var scheduleChangedTrigger by mutableIntStateOf(0)
        var isPlaying by mutableStateOf(false)

        fun startServiceAndUpdateInfo() {
            val serviceIntent = Intent(context, StartService::class.java)
            context.startForegroundService(serviceIntent)

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

        SettingsService.load()
        ScheduleService.load()
        ScheduleService.sort()
        ScheduleService.setNextAlarm()
    }
}
