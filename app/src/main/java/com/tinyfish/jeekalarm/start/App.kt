package com.tinyfish.jeekalarm.start

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import com.tinyfish.jeekalarm.R
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

        // 数字格式与语言无关，固定用 US 即可。
        private val dateFormat = SimpleDateFormat("MM-dd", Locale.US)
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

        // 星期名称跟随当前语言。
        private fun weekFormat() = SimpleDateFormat("EEE", currentLocale())

        // 月日/年月日：按当前 Locale 取最合适的排版，中文带“月/日”（如 6月5日、2027年6月5日），
        // 英文为 Jun 5 / Jun 5, 2027——用 skeleton 而非固定 "MMM d"，避免中文丢掉“日”。
        private fun monthDayFormat() =
            SimpleDateFormat(
                android.text.format.DateFormat.getBestDateTimePattern(currentLocale(), "MMMd"),
                currentLocale()
            )

        private fun yearMonthDayFormat() =
            SimpleDateFormat(
                android.text.format.DateFormat.getBestDateTimePattern(currentLocale(), "yMMMd"),
                currentLocale()
            )

        /** 当前生效的 Locale：应用内选了具体语言就用它，否则跟随系统。 */
        private fun currentLocale(): Locale {
            val tags = SettingsService.language
            return if (tags.isEmpty()) Locale.getDefault() else Locale.forLanguageTag(tags)
        }

        /**
         * 跟随应用内语言设置的 Context，供非 Composable 代码（通知、Toast、列表副标题等）取字符串。
         * Android 13 以下，AppCompat 的按应用语言不会改写 application context，这里手动套一层配置补齐。
         */
        fun localizedContext(): Context {
            val tags = SettingsService.language
            if (tags.isEmpty())
                return context
            val config = Configuration(context.resources.configuration)
            config.setLocale(Locale.forLanguageTag(tags))
            return context.createConfigurationContext(config)
        }

        /** 把存下来的语言设置应用到 AppCompat（空 = 跟随系统）。 */
        fun applyStoredLanguage() {
            val tags = SettingsService.language
            val locales = if (tags.isEmpty())
                LocaleListCompat.getEmptyLocaleList()
            else
                LocaleListCompat.forLanguageTags(tags)
            AppCompatDelegate.setApplicationLocales(locales)
        }

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
                dateString = localizedContext().getString(R.string.date_today)
            else if (isTomorrow)
                dateString = localizedContext().getString(R.string.date_tomorrow)
            else if (isDat)
                dateString = localizedContext().getString(R.string.date_day_after_tomorrow)
            else if (!sameYear)
                dateString = "$calendarYear-$dateString"

            val weekString = weekFormat().format(calendar.time)

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
                return yearMonthDayFormat().format(calendar.time)

            val diff = calendar.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR)
            return when {
                diff == 0 -> localizedContext().getString(R.string.date_today)
                diff == 1 -> localizedContext().getString(R.string.date_tomorrow)
                diff == 2 -> localizedContext().getString(R.string.date_day_after_tomorrow)
                diff in 3..6 && useWeekday -> weekFormat().format(calendar.time)
                else -> monthDayFormat().format(calendar.time)
            }
        }

        var nextAlarmIds by mutableStateOf(listOf<Int>())
        var isPlaying by mutableStateOf(false)
    }

    override fun onCreate() {
        super.onCreate()

        context = applicationContext

        SettingsService.load()
        applyStoredLanguage()
        ScheduleService.load()
        RecycleBinService.load()
        ScheduleService.sort()
        ScheduleService.setNextAlarm()
    }
}
