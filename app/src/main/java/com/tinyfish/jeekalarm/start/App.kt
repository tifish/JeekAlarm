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
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

class App : Application() {
    companion object {
        // Only 1 instance, leak is not a problem
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        // 星期名称跟随当前语言。
        private fun weekFormat(calendar: Calendar) =
            SimpleDateFormat("EEE", currentLocale()).apply { timeZone = calendar.timeZone }

        // 月日/年月日：按当前 Locale 取最合适的排版，中文带“月/日”（如 6月5日、2027年6月5日），
        // 英文为 Jun 5 / Jun 5, 2027——用 skeleton 而非固定 "MMM d"，避免中文丢掉“日”。
        private fun monthDayFormat(calendar: Calendar) =
            SimpleDateFormat(
                android.text.format.DateFormat.getBestDateTimePattern(currentLocale(), "MMMd"),
                currentLocale()
            ).apply { timeZone = calendar.timeZone }

        private fun yearMonthDayFormat(calendar: Calendar) =
            SimpleDateFormat(
                android.text.format.DateFormat.getBestDateTimePattern(currentLocale(), "yMMMd"),
                currentLocale()
            ).apply { timeZone = calendar.timeZone }

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

        internal fun dayDiff(from: Calendar, to: Calendar): Long {
            val fromDate = LocalDate.of(
                from.get(Calendar.YEAR),
                from.get(Calendar.MONTH) + 1,
                from.get(Calendar.DAY_OF_MONTH),
            )
            val toDate = LocalDate.of(
                to.get(Calendar.YEAR),
                to.get(Calendar.MONTH) + 1,
                to.get(Calendar.DAY_OF_MONTH),
            )
            return ChronoUnit.DAYS.between(fromDate, toDate)
        }

        fun format(calendar: Calendar?, now: Calendar = Calendar.getInstance()): String {
            if (calendar == null)
                return ""

            val diff = dayDiff(now, calendar)
            val calendarYear = calendar.get(Calendar.YEAR)
            val sameYear = now.get(Calendar.YEAR) == calendarYear

            val dateString = when (diff) {
                0L -> localizedContext().getString(R.string.date_today)
                1L -> localizedContext().getString(R.string.date_tomorrow)
                2L -> localizedContext().getString(R.string.date_day_after_tomorrow)
                else -> {
                    val mmdd = "%02d-%02d".format(
                        Locale.US,
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH),
                    )
                    if (sameYear) mmdd else "$calendarYear-$mmdd"
                }
            }

            val weekString = weekFormat(calendar).format(calendar.time)
            val timeString = "%02d:%02d".format(
                Locale.US,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
            )

            return "$dateString $timeString $weekString"
        }

        /**
         * 主列表主角行的“哪天”标签：今明后→相对词，更远→月日，跨年再带年份。
         * useWeekday 为 true（按星期重复的闹钟）时，落在一周内会用星期几（如 "下周二" 的语感）；
         * 月/年/一次性的闹钟关心的是日期，不该以星期几开头。
         */
        fun nextTriggerDay(
            calendar: Calendar?,
            useWeekday: Boolean = false,
            now: Calendar = Calendar.getInstance(),
        ): String {
            if (calendar == null)
                return ""

            val diff = dayDiff(now, calendar)
            return when {
                diff == 0L -> localizedContext().getString(R.string.date_today)
                diff == 1L -> localizedContext().getString(R.string.date_tomorrow)
                diff == 2L -> localizedContext().getString(R.string.date_day_after_tomorrow)
                diff in 3L..6L && useWeekday -> weekFormat(calendar).format(calendar.time)
                now.get(Calendar.YEAR) != calendar.get(Calendar.YEAR) -> yearMonthDayFormat(calendar).format(calendar.time)
                else -> monthDayFormat(calendar).format(calendar.time)
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
        ScheduleService.setNextAlarm()
    }
}
