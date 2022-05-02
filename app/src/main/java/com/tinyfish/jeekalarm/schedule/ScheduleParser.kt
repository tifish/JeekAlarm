package com.tinyfish.jeekalarm.schedule

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.*

internal object ScheduleParser {
    fun parseJsonLine(line: String): Schedule? {
        return try {
            Json.decodeFromString<Schedule>(line).apply { timeConfigChanged() }
        } catch (e: Exception) {
            null
        }
    }

    fun parseTextLine(line: String): Schedule {
        val parts = line.split(' ')
        return Schedule(
            name = parts[0],
            hourConfig = parts[1],
            minuteConfig = parts[2],
            weekDayConfig = if (parts.size > 3) parts[3] else "*",
            dayConfig = if (parts.size > 4) parts[4] else "*",
            monthConfig = if (parts.size > 5) parts[5] else "*",
            yearConfig = if (parts.size > 6) parts[6] else "*",
        )
    }

    fun parseTimeConfig(schedule: Schedule) {
        schedule.hours = parseIndexes(Calendar.HOUR_OF_DAY, schedule.hourConfig)
        schedule.minutes = parseIndexes(Calendar.MINUTE, schedule.minuteConfig)
        schedule.weekDays = parseIndexes(Calendar.DAY_OF_WEEK, schedule.weekDayConfig)
        schedule.days = parseIndexes(Calendar.DAY_OF_MONTH, schedule.dayConfig)
        schedule.months = parseIndexes(Calendar.MONTH, schedule.monthConfig)
        schedule.years = parseIndexes(Calendar.YEAR, schedule.yearConfig)
    }

    private fun toCalendarMonth(months: MutableList<Int>) {
        // Crontab's month is 1-based
        // Calendar's month is 0-based
        for (i in months.indices) {
            months[i] = months[i] - 1
        }
    }

    private fun toCalendarWeekDay(weekDays: MutableList<Int>) {
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
                    schedule.id = ScheduleService.nextScheduleId++
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
                it.write(Json.encodeToString(schedule))
                it.newLine()
            }
        }
    }

    private fun parseIndexes(
        indexType: Int,
        content: String,
    ): MutableList<Int> {
        // content format:
        // 1-3,5,6 or */5

        val minCronValue = when (indexType) {
            Calendar.HOUR_OF_DAY -> 0
            Calendar.MINUTE -> 0
            Calendar.DAY_OF_WEEK -> 0
            Calendar.DAY_OF_MONTH -> 1
            Calendar.MONTH -> 1
            Calendar.YEAR -> 1
            else -> throw IllegalArgumentException("Invalid index type $indexType")
        }
        val maxCronValue = when (indexType) {
            Calendar.HOUR_OF_DAY -> 23
            Calendar.MINUTE -> 59
            Calendar.DAY_OF_WEEK -> 7
            Calendar.DAY_OF_MONTH -> 31
            Calendar.MONTH -> 12
            Calendar.YEAR -> 9999
            else -> throw IllegalArgumentException("Invalid index type $indexType")
        }

        var result = mutableListOf<Int>()

        if (content.startsWith("*/")) {
            val interval = content.substring(2).toInt()
            for (i in minCronValue..maxCronValue step interval) {
                result.add(i)
            }
        } else {
            for (part in content.split(",")) {
                when {
                    part == "*" -> {
                        result.clear()
                        return result
                    }
                    part.contains("-") -> {
                        val beginEnd = part.split("-")
                        assert(beginEnd.size == 2)
                        val begin = ensureRange(beginEnd[0].toInt(), minCronValue, maxCronValue)
                        val end = ensureRange(beginEnd[1].toInt(), minCronValue, maxCronValue)
                        for (i in begin..end) {
                            result.add(i)
                        }
                    }
                    else -> {
                        result.add(ensureRange(part.toInt(), minCronValue, maxCronValue))
                    }
                }
            }
        }

        result = result.distinct().toMutableList()

        when (indexType) {
            Calendar.MONTH -> toCalendarMonth(result)
            Calendar.DAY_OF_WEEK -> toCalendarWeekDay(result)
        }

        result.sort()

        return result
    }

    private fun ensureRange(value: Int, min: Int, max: Int): Int {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
}