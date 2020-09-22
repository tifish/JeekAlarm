package com.tinyfish.jeekalarm.schedule

import com.squareup.moshi.JsonAdapter
import com.tinyfish.jeekalarm.start.App
import java.io.File
import java.io.IOException
import java.util.*

internal object ScheduleParser {
    private val scheduleMoshiAdapter: JsonAdapter<Schedule> = App.moshi.adapter(Schedule::class.java)

    fun parseJsonLine(line: String): Schedule? {
        return try {
            scheduleMoshiAdapter.fromJson(line).also {
                it?.timeConfigChanged()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun parseTextLine(line: String): Schedule? {
        val parts = line.split(' ')
        return Schedule(
            name = parts[0],
            minuteConfig = parts[1],
            hourConfig = parts[2],
            dayConfig = parts[3],
            monthConfig = parts[4],
            weekDayConfig = parts[5]
        )
    }

    fun parseTimeConfig(schedule: Schedule) {
        parseIndexes(
            Calendar.MINUTE,
            schedule.minuteConfig,
            schedule.minutes
        )
        parseIndexes(
            Calendar.HOUR_OF_DAY,
            schedule.hourConfig,
            schedule.hours
        )
        parseIndexes(
            Calendar.DAY_OF_MONTH,
            schedule.dayConfig,
            schedule.days
        )
        parseIndexes(
            Calendar.MONTH,
            schedule.monthConfig,
            schedule.months
        )
        parseIndexes(
            Calendar.DAY_OF_WEEK,
            schedule.weekDayConfig,
            schedule.weekDays
        )
    }

    private fun normalizeMonths(months: MutableList<Int>) {
        // Crontab's month is 1-based
        // Calendar's month is 0-based
        for (i in months.indices) {
            months[i] = months[i] - 1
        }
    }

    private fun normalizeWeekDays(weekDays: MutableList<Int>) {
        // Crontab's weekday: 0-7, 0 and 7 are both Sunday
        // Calendar's weekday: Sunday: 1, Monday: 2
        for (i in weekDays.indices) {
            weekDays[i] = (weekDays[i] % 7) + 1
        }
    }

    @Throws(IOException::class)
    fun loadFromFile(configFile: File): MutableList<Schedule> {
        val result = mutableListOf<Schedule>()
        if (!configFile.exists())
            return result

        configFile.forEachLine {
            val line = it.trim()
            if (line.isNotEmpty()) {
                val schedule = parseJsonLine(line)
                if (schedule != null) {
                    result.add(schedule)
                }
            }
        }

        return result
    }

    fun saveToFile(configFile: File, schedules: MutableList<Schedule>) {
        if (!configFile.exists())
            configFile.createNewFile()
        configFile.bufferedWriter().use {
            for (schedule in schedules) {
                it.write(scheduleMoshiAdapter.toJson(schedule))
                it.newLine()
            }
        }
    }

    private fun parseIndexes(
        indexType: Int,
        content: String,
        result: MutableList<Int>
    ) {
        // content format:
        // 1-3,5,6 or */5
        result.clear()
        val calendar = Calendar.getInstance()

        if (content.startsWith("*/")) {
            val interval = content.substring(2).toInt()
            for (i in calendar.getMinimum(indexType)..calendar.getMaximum(indexType) step interval) {
                result.add(i)
            }
            return
        } else {
            for (part in content.split(",")) {
                when {
                    part == "*" -> {
                        for (i in calendar.getMinimum(indexType)..calendar.getMaximum(indexType)) {
                            result.add(i)
                        }
                        return
                    }
                    part.contains("-") -> {
                        val beginEnd = part.split("-")
                        assert(beginEnd.size == 2)
                        val begin = beginEnd[0].toInt()
                        val end = beginEnd[1].toInt()
                        for (i in begin..end) {
                            result.add(i)
                        }
                    }
                    else -> {
                        result.add(part.toInt())
                    }
                }
            }
        }

        if (indexType == Calendar.MONTH)
            normalizeMonths(result)
        else if (indexType == Calendar.DAY_OF_WEEK)
            normalizeWeekDays(result)

        result.sort()
    }
}