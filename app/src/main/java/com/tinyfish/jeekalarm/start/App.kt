package com.tinyfish.jeekalarm.start

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.schedule.RecycleBinService
import com.tinyfish.jeekalarm.schedule.ScheduleService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class App : Application() {
    companion object {
        // Only 1 instance, leak is not a problem
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        private val dateFormat = SimpleDateFormat("MM-dd", Locale.US)
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        private val weekFormat = SimpleDateFormat("EEE", Locale.US)
        private val monthDayFormat = SimpleDateFormat("MMM d", Locale.US)

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

        /**
         * 主列表主角行的“哪天”标签：今明后→相对词，更远→月日，跨年再带年份。
         * useWeekday 为 true（按星期重复的闹钟）时，落在一周内会用星期几（如 "下周二" 的语感）；
         * 月/年/一次性的闹钟关心的是日期，不该以星期几开头。
         */
        fun nextTriggerDay(calendar: Calendar?, useWeekday: Boolean = false): String {
            if (calendar == null)
                return ""
            val now = Calendar.getInstance()
            if (now.get(Calendar.YEAR) != calendar.get(Calendar.YEAR))
                return "${monthDayFormat.format(calendar.time)} ${calendar.get(Calendar.YEAR)}"

            val diff = calendar.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR)
            return when {
                diff == 0 -> "Today"
                diff == 1 -> "Tmr"
                diff == 2 -> "DAT"
                diff in 3..6 && useWeekday -> weekFormat.format(calendar.time)
                else -> monthDayFormat.format(calendar.time)
            }
        }

        var nextAlarmIds by mutableStateOf(listOf<Int>())
        var isPlaying by mutableStateOf(false)
    }

    override fun onCreate() {
        super.onCreate()

        context = applicationContext

        SettingsService.load()
        ScheduleService.load()
        RecycleBinService.load()
        ScheduleService.sort()
        ScheduleService.setNextAlarm()
    }
}
