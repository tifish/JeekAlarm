package com.tinyfish.jeekalarm.schedule

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

internal object ScheduleParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseJsonLine(line: String): Schedule? {
        return try {
            json.decodeFromString<Schedule>(line).apply { timeConfigChanged() }
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

    fun isValidField(indexType: Int, rawContent: String): Boolean {
        return runCatching { parseIndexes(indexType, rawContent) }.isSuccess
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

    fun loadFromLines(lines: List<String>): MutableList<Schedule> {
        val result = mutableListOf<Schedule>()
        val usedIds = mutableSetOf<Int>()
        var nextId = 1

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isNotEmpty()) {
                val schedule = parseJsonLine(line)
                if (schedule != null) {
                    if (schedule.id <= 0 || schedule.id in usedIds) {
                        while (nextId in usedIds)
                            nextId++
                        schedule.id = nextId++
                    }
                    usedIds.add(schedule.id)
                    if (schedule.id >= nextId)
                        nextId = schedule.id + 1
                    result.add(schedule)
                }
            }
        }

        ScheduleService.nextScheduleId = nextId
        return result
    }

    fun saveToString(schedules: List<Schedule>): String {
        return buildString {
            for (schedule in schedules) {
                append(json.encodeToString(schedule))
                appendLine()
            }
        }
    }

    private fun parseIndexes(
        indexType: Int,
        rawContent: String,
    ): MutableList<Int> {
        // content format:
        // 1-3,5,6 or */5
        val content = rawContent.trim()
        require(content.isNotEmpty()) { "Empty cron field" }

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
            val interval = content.substring(2).trim().toInt()
            require(interval > 0) { "Step interval must be positive: $interval" }
            for (i in minCronValue..maxCronValue step interval) {
                result.add(i)
            }
        } else {
            for (rawPart in content.split(",")) {
                val part = rawPart.trim()
                when {
                    part == "*" -> {
                        result.clear()
                        return result
                    }

                    part.contains("-") -> {
                        val beginEnd = part.split("-")
                        require(beginEnd.size == 2) { "Invalid range: $part" }
                        val begin = parseInRange(beginEnd[0], minCronValue, maxCronValue)
                        val end = parseInRange(beginEnd[1], minCronValue, maxCronValue)
                        require(begin <= end) { "Reversed range: $part" }
                        for (i in begin..end) {
                            result.add(i)
                        }
                    }

                    else -> {
                        result.add(parseInRange(part, minCronValue, maxCronValue))
                    }
                }
            }
        }

        when (indexType) {
            Calendar.MONTH -> toCalendarMonth(result)
            Calendar.DAY_OF_WEEK -> toCalendarWeekDay(result)
        }

        // Deduplicate after weekday mapping so 0 and 7 (both Sunday) don't duplicate.
        result = result.distinct().toMutableList()
        result.sort()

        return result
    }

    private fun parseInRange(token: String, min: Int, max: Int): Int {
        val value = token.trim().toInt()
        require(value in min..max) { "Value $value out of range $min..$max" }
        return value
    }
}
