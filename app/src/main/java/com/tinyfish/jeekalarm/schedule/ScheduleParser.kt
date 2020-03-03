package com.tinyfish.jeekalarm.schedule

import com.beust.klaxon.KlaxonException
import com.tinyfish.jeekalarm.App
import java.io.File
import java.io.IOException
import java.util.*

internal object ScheduleParser {
    fun parseLine(line: String): Schedule? {
        return try {
            App.json.parse<Schedule>(line).also {
                it?.timeConfigChanged()
            }
        } catch (e: KlaxonException) {
            null
        }
    }

    fun parseTimeConfig(schedule: Schedule) {
        parseIndexes(
            schedule.minuteConfig,
            schedule.minutes,
            Calendar.MINUTE
        )
        parseIndexes(
            schedule.hourConfig,
            schedule.hours,
            Calendar.HOUR_OF_DAY
        )
        parseIndexes(
            schedule.dayConfig,
            schedule.days,
            Calendar.DAY_OF_MONTH
        )
        parseIndexes(
            schedule.monthConfig,
            schedule.months,
            Calendar.MONTH
        )
        parseIndexes(
            schedule.weekDayConfig,
            schedule.weekDays,
            Calendar.DAY_OF_WEEK
        )
    }

    private fun normalizeMonths(months: MutableList<Int>) {
        // Month is 0-based
        for (i in months.indices) {
            months[i] = months[i] - 1
        }
    }

    private fun normalizeWeekDays(weekDays: MutableList<Int>) {
        // Sunday: 1, Monday: 2
        for (i in weekDays.indices) {
            weekDays[i] = (weekDays[i] + 1) % 7
        }
    }

    @Throws(IOException::class)
    fun loadFromFile(configFile: File): MutableList<Schedule> {
        val result = mutableListOf<Schedule>()
        if (!configFile.exists())
            return result

        configFile.forEachLine() {
            val line = it.trim()
            if (line.isNotEmpty()) {
                val schedule = parseLine(line)
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
                it.write(App.json.toJsonString(schedule))
                it.newLine()
            }
        }
    }

    private fun parseIndexes(
        content: String,
        result: MutableList<Int>,
        timePart: Int
    ) {
        // content format:
        // 1-3,5,6
        result.clear()
        val calendar = Calendar.getInstance()

        for (part in content.split(",")) {
            when {
                part == "*" -> {
                    for (i in calendar.getMinimum(timePart)..calendar.getMaximum(timePart)) {
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

        if (timePart == Calendar.MONTH)
            normalizeMonths(result)
        else if (timePart == Calendar.DAY_OF_WEEK)
            normalizeWeekDays(result)

        result.sort()
    }

    fun toLine(schedule: Schedule): String {
        return App.json.toJsonString(schedule)
    }
}