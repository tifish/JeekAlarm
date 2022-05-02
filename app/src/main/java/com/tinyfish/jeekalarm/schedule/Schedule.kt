package com.tinyfish.jeekalarm.schedule

import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import com.tinyfish.jeekalarm.ConfigService
import com.tinyfish.jeekalarm.MusicService
import com.tinyfish.jeekalarm.VibrationService
import com.tinyfish.jeekalarm.start.App
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.util.*
import kotlin.random.Random

// A single cron schedule
@Serializable
data class Schedule(
    var name: String = "Alarm",
    var minuteConfig: String = "*",
    var hourConfig: String = "*",
    var dayConfig: String = "*",
    var monthConfig: String = "*",
    var weekDayConfig: String = "*",
    var yearConfig: String = "*",
    var enabled: Boolean = true,
    var onlyOnce: Boolean = false,
    var playMusic: Boolean = true,
    var musicFile: String = "",
    var musicFolder: String = "",
    var vibration: Boolean = true,
    var vibrationCount: Int = 10
) {
    fun copyTo(dest: Schedule) {
        dest.name = name
        dest.hourConfig = hourConfig
        dest.minuteConfig = minuteConfig
        dest.weekDayConfig = weekDayConfig
        dest.dayConfig = dayConfig
        dest.monthConfig = monthConfig
        dest.yearConfig = yearConfig
        dest.enabled = enabled
        dest.onlyOnce = onlyOnce
        dest.playMusic = playMusic
        dest.musicFile = musicFile
        dest.musicFolder = musicFolder
        dest.vibration = vibration
        dest.vibrationCount = vibrationCount
    }

    fun timeConfigChanged() {
        timeConfig = "$hourConfig:$minuteConfig W $weekDayConfig M $monthConfig D $dayConfig Y $yearConfig"
        try {
            ScheduleParser.parseTimeConfig(this)
            isValid = true
        } catch (ex: Exception) {
            Log.e(this.javaClass.name, ex.message ?: "parse error")
            isValid = false
            hours.clear()
            minutes.clear()
            weekDays.clear()
            months.clear()
            days.clear()
            years.clear()
        }
    }

    @Transient
    var timeConfig: String = ""
        private set

    @Transient
    var isValid: Boolean = false

    @Transient
    var hours = mutableListOf<Int>()

    @Transient
    var minutes = mutableListOf<Int>()

    @Transient
    var weekDays = mutableListOf<Int>()

    @Transient
    var days = mutableListOf<Int>()

    @Transient
    var months = mutableListOf<Int>()

    @Transient
    var years = mutableListOf<Int>()

    init {
        timeConfigChanged()
    }

    fun getNextTriggerTime(now: Calendar? = null): Calendar? {
        if (!isValid)
            return null

        val resultTime =
            if (now == null)
                Calendar.getInstance()
            else
                now.clone() as Calendar
        resultTime.clear(Calendar.SECOND)
        resultTime.clear(Calendar.MILLISECOND)

        val minTriggerMonth = if (months.size == 0) 0 else months[0]
        val minTriggerDay = if (days.size == 0) 1 else days[0]
        val minTriggerHour = if (hours.size == 0) 0 else hours[0]
        val minTriggerMinute = if (minutes.size == 0) 0 else minutes[0]

        var minuteCompareMethod = CompareMethod.Bigger

        val setMinValueBelow = fun(timePart: Int) {
            when (timePart) {
                Calendar.YEAR -> {
                    resultTime.set(Calendar.MONTH, minTriggerMonth)
                    resultTime.set(Calendar.DAY_OF_MONTH, minTriggerDay)
                    resultTime.set(Calendar.HOUR_OF_DAY, minTriggerHour)
                    resultTime.set(Calendar.MINUTE, minTriggerMinute)
                }
                Calendar.MONTH -> {
                    resultTime.set(Calendar.DAY_OF_MONTH, minTriggerDay)
                    resultTime.set(Calendar.HOUR_OF_DAY, minTriggerHour)
                    resultTime.set(Calendar.MINUTE, minTriggerMinute)
                }
                Calendar.DAY_OF_MONTH -> {
                    resultTime.set(Calendar.HOUR_OF_DAY, minTriggerHour)
                    resultTime.set(Calendar.MINUTE, minTriggerMinute)
                }
                Calendar.HOUR_OF_DAY -> {
                    resultTime.set(Calendar.MINUTE, minTriggerMinute)
                }
                else -> throw Exception("not match")
            }
        }

        val notMatchHandler = fun(currentTimePart: Int): Int {
            val orgTime = resultTime.clone() as Calendar

            when (currentTimePart) {
                Calendar.YEAR -> {
                    return Int.MIN_VALUE
                }
                Calendar.MONTH -> {
                    resultTime.add(Calendar.YEAR, 1)
                    setMinValueBelow(Calendar.YEAR)
                }
                Calendar.DAY_OF_MONTH -> {
                    resultTime.add(Calendar.MONTH, 1)
                    setMinValueBelow(Calendar.MONTH)
                }
                Calendar.DAY_OF_WEEK -> {
                    resultTime.add(Calendar.DAY_OF_MONTH, 1)
                    setMinValueBelow(Calendar.DAY_OF_MONTH)
                }
                Calendar.HOUR_OF_DAY -> {
                    resultTime.add(Calendar.DAY_OF_MONTH, 1)
                    setMinValueBelow(Calendar.DAY_OF_MONTH)
                }
                Calendar.MINUTE -> {
                    resultTime.add(Calendar.HOUR_OF_DAY, 1)
                    setMinValueBelow(Calendar.HOUR_OF_DAY)
                }
            }

            minuteCompareMethod = CompareMethod.EqualOrBigger

            return when {
                orgTime[Calendar.YEAR] != resultTime[Calendar.YEAR] -> Calendar.MONTH
                orgTime[Calendar.MONTH] != resultTime[Calendar.MONTH] -> Calendar.MONTH
                orgTime[Calendar.DAY_OF_MONTH] != resultTime[Calendar.DAY_OF_MONTH] -> Calendar.DAY_OF_MONTH
                orgTime[Calendar.HOUR_OF_DAY] != resultTime[Calendar.HOUR_OF_DAY] -> Calendar.HOUR_OF_DAY
                else -> throw Exception("not match")
            }
        }

        val biggerHandler = fun(currentTimePart: Int): Int {
            minuteCompareMethod = CompareMethod.EqualOrBigger

            setMinValueBelow(currentTimePart)

            return when (currentTimePart) {
                Calendar.YEAR -> Calendar.DAY_OF_WEEK
                Calendar.MONTH -> Calendar.DAY_OF_WEEK
                Calendar.DAY_OF_MONTH -> Calendar.DAY_OF_WEEK
                Calendar.HOUR_OF_DAY -> Int.MAX_VALUE
                else -> throw Exception("not match")
            }
        }

        // Match from month to minute.
        var matchingPart = Calendar.YEAR

        while (true) {
            when (matchingPart) {
                Calendar.YEAR -> {
                    matchingPart = when (findNextTriggerTimePart(
                        resultTime,
                        Calendar.YEAR,
                        years,
                        CompareMethod.EqualOrBigger
                    )) {
                        MatchResult.NotMatch -> notMatchHandler(Calendar.YEAR)
                        MatchResult.Bigger -> biggerHandler(Calendar.YEAR)
                        MatchResult.Equals -> Calendar.MONTH
                    }
                }
                Calendar.MONTH -> {
                    matchingPart = when (findNextTriggerTimePart(
                        resultTime,
                        Calendar.MONTH,
                        months,
                        CompareMethod.EqualOrBigger
                    )) {
                        MatchResult.NotMatch -> notMatchHandler(Calendar.MONTH)
                        MatchResult.Bigger -> biggerHandler(Calendar.MONTH)
                        MatchResult.Equals -> Calendar.DAY_OF_MONTH
                    }
                }

                Calendar.DAY_OF_MONTH -> {
                    matchingPart = when (findNextTriggerTimePart(
                        resultTime,
                        Calendar.DAY_OF_MONTH,
                        days,
                        CompareMethod.EqualOrBigger
                    )) {
                        MatchResult.NotMatch -> notMatchHandler(Calendar.DAY_OF_MONTH)
                        MatchResult.Bigger -> biggerHandler(Calendar.DAY_OF_MONTH)
                        MatchResult.Equals -> Calendar.DAY_OF_WEEK
                    }
                }

                Calendar.DAY_OF_WEEK -> {
                    matchingPart = when (findNextTriggerTimePart(
                        resultTime,
                        Calendar.DAY_OF_WEEK,
                        weekDays,
                        CompareMethod.Equal
                    )) {
                        MatchResult.NotMatch -> notMatchHandler(Calendar.DAY_OF_WEEK)
                        MatchResult.Bigger -> throw Exception("unexpected match type")
                        MatchResult.Equals -> Calendar.HOUR_OF_DAY
                    }
                }

                Calendar.HOUR_OF_DAY -> {
                    matchingPart = when (findNextTriggerTimePart(
                        resultTime,
                        Calendar.HOUR_OF_DAY,
                        hours,
                        CompareMethod.EqualOrBigger
                    )) {
                        MatchResult.NotMatch -> notMatchHandler(Calendar.HOUR_OF_DAY)
                        MatchResult.Bigger -> biggerHandler(Calendar.HOUR_OF_DAY)
                        MatchResult.Equals -> Calendar.MINUTE
                    }
                }

                Calendar.MINUTE -> {
                    matchingPart = when (findNextTriggerTimePart(
                        resultTime,
                        Calendar.MINUTE,
                        minutes,
                        minuteCompareMethod
                    )) {
                        MatchResult.NotMatch -> notMatchHandler(Calendar.MINUTE)
                        else -> break
                    }
                }

                Int.MAX_VALUE -> break
                Int.MIN_VALUE -> return null
            }
        }

        return resultTime
    }

    /// Find the next month/day/hour/minute should be triggered.
    private fun findNextTriggerTimePart(
        time: Calendar,
        partType: Int,
        partTriggerList: List<Int>,
        compareMethod: CompareMethod
    ): MatchResult {
        if (partTriggerList.isEmpty()) {
            return when (compareMethod) {
                CompareMethod.Bigger -> {
                    assert(partType == Calendar.MINUTE)
                    time.add(Calendar.MINUTE, 1)
                    MatchResult.Bigger
                }
                else -> MatchResult.Equals
            }
        }

        val current = time[partType]
        val min = time.getActualMinimum(partType)
        val max = time.getActualMaximum(partType)

        for (value in partTriggerList) {
            if (value < min || value > max) {
                throw Exception("invalid trigger time part $value")
            }

            val isBigger = value > current
            val isEqual = value == current
            if (compareMethod == CompareMethod.Bigger && isBigger
                || compareMethod == CompareMethod.EqualOrBigger && (isEqual || isBigger)
                || compareMethod == CompareMethod.Equal && isEqual
            ) {
                if (partType != Calendar.DAY_OF_WEEK) {
                    time[partType] = value
                }
                when {
                    isBigger -> {
                        return MatchResult.Bigger
                    }
                    isEqual -> {
                        return MatchResult.Equals
                    }
                    else -> {
                        assert(false)
                    }
                }
            }
        }

        return MatchResult.NotMatch
    }

    private enum class MatchResult {
        NotMatch, Equals, Bigger
    }

    private enum class CompareMethod {
        EqualOrBigger, Bigger, Equal
    }

    fun play() {
        if (playMusic) {
            playMusic()
        }

        if (vibration)
            VibrationService.vibrate(vibrationCount)

        App.isPlaying = true
    }

    private fun playMusic() {
        val finalMusicFolder =
            if (musicFolder.isEmpty())
                ConfigService.data.defaultMusicFolder
            else
                musicFolder

        if (finalMusicFolder.isNotEmpty()) {
            val folder = File(Environment.getExternalStorageDirectory().path, finalMusicFolder)
            val musicFiles = mutableListOf<File>()
            for (file in folder.listFiles() ?: return) {
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                if (mimeType != null && mimeType.startsWith("audio")) {
                    musicFiles.add(file)
                }
            }
            if (musicFiles.size == 0)
                return

            val randomIndex = Random.nextInt(musicFiles.size)
            MusicService.play(musicFiles[randomIndex])

        } else {
            val finalMusicFile =
                if (musicFile.isEmpty())
                    ConfigService.data.defaultMusicFile
                else
                    musicFile

            if (finalMusicFile.isNotEmpty())
                MusicService.play(finalMusicFile)
            else
                MusicService.play(Settings.System.DEFAULT_ALARM_ALERT_URI)
        }
    }
}