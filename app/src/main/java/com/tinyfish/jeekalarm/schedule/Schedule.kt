package com.tinyfish.jeekalarm.schedule

import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import com.tinyfish.jeekalarm.ConfigHome
import com.tinyfish.jeekalarm.MusicHome
import com.tinyfish.jeekalarm.VibrationHome
import com.tinyfish.jeekalarm.start.App
import java.io.File
import java.util.*
import kotlin.random.Random

// A single cron schedule
data class Schedule(
    var name: String = "Alarm",
    var minuteConfig: String = "*",
    var hourConfig: String = "*",
    var dayConfig: String = "*",
    var monthConfig: String = "*",
    var weekDayConfig: String = "*",
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
        dest.minuteConfig = minuteConfig
        dest.hourConfig = hourConfig
        dest.dayConfig = dayConfig
        dest.monthConfig = monthConfig
        dest.weekDayConfig = weekDayConfig
        dest.enabled = enabled
        dest.onlyOnce = onlyOnce
        dest.playMusic = playMusic
        dest.musicFile = musicFile
        dest.musicFolder = musicFolder
        dest.vibration = vibration
        dest.vibrationCount = vibrationCount
    }

    fun timeConfigChanged() {
        timeConfig = "$minuteConfig $hourConfig $dayConfig $monthConfig $weekDayConfig"
        try {
            ScheduleParser.parseTimeConfig(this)
            isValid = true
        } catch (ex: Exception) {
            Log.e(this.javaClass.name, ex.message ?: "parse error")
            isValid = false
            months.clear()
            days.clear()
            hours.clear()
            minutes.clear()
            weekDays.clear()
        }
    }

    @Transient
    var timeConfig: String = ""
        private set

    @Transient
    var isValid: Boolean = false

    @Transient
    var minutes = mutableListOf<Int>()

    @Transient
    var hours = mutableListOf<Int>()

    @Transient
    var days = mutableListOf<Int>()

    @Transient
    var months = mutableListOf<Int>()

    @Transient
    var weekDays = mutableListOf<Int>()

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
        var minuteCompareMethod = CompareMethod.Bigger

        // Match from month to minute.
        // May loop once or twice.
        while (true) {
            var matchType = findNextTriggerTimePart(
                resultTime,
                Calendar.MONTH,
                months,
                Calendar.YEAR,
                CompareMethod.EqualOrBigger
            )
            when (matchType) {
                MatchType.NotMatch -> {
                    resultTime.set(resultTime[Calendar.YEAR], resultTime.getMinimum(Calendar.MONTH), 1, 0, 0)
                    minuteCompareMethod = CompareMethod.EqualOrBigger
                    continue
                }
                MatchType.Bigger -> {
                    resultTime.set(
                        resultTime[Calendar.YEAR],
                        resultTime[Calendar.MONTH],
                        days[0],
                        hours[0],
                        minutes[0]
                    )
                    minuteCompareMethod = CompareMethod.EqualOrBigger
                }
                MatchType.Equals -> {}
            }

            matchType = findNextTriggerTimePart(
                resultTime,
                Calendar.DAY_OF_MONTH,
                days,
                Calendar.MONTH,
                CompareMethod.EqualOrBigger
            )
            when (matchType) {
                MatchType.NotMatch -> {
                    minuteCompareMethod = CompareMethod.EqualOrBigger
                    continue
                }
                MatchType.Bigger -> {
                    resultTime.set(
                        resultTime[Calendar.YEAR],
                        resultTime[Calendar.MONTH],
                        resultTime[Calendar.DAY_OF_MONTH],
                        hours[0],
                        minutes[0]
                    )
                    minuteCompareMethod = CompareMethod.EqualOrBigger
                }
                MatchType.Equals -> {}
            }

            matchType = findNextTriggerTimePart(
                resultTime,
                Calendar.DAY_OF_WEEK,
                weekDays,
                Calendar.DAY_OF_MONTH,
                CompareMethod.Equal
            )
            when (matchType) {
                MatchType.NotMatch -> {
                    minuteCompareMethod = CompareMethod.EqualOrBigger
                    continue
                }
                MatchType.Equals -> {}
                MatchType.Bigger -> throw Exception("unexpected match type")
            }

            matchType = findNextTriggerTimePart(
                resultTime,
                Calendar.HOUR_OF_DAY,
                hours,
                Calendar.DAY_OF_MONTH,
                CompareMethod.EqualOrBigger
            )
            when (matchType) {
                MatchType.NotMatch -> {
                    minuteCompareMethod = CompareMethod.EqualOrBigger
                    continue
                }
                MatchType.Bigger -> {
                    resultTime.set(
                        resultTime[Calendar.YEAR],
                        resultTime[Calendar.MONTH],
                        resultTime[Calendar.DAY_OF_MONTH],
                        resultTime[Calendar.HOUR_OF_DAY],
                        minutes[0]
                    )
                    minuteCompareMethod = CompareMethod.EqualOrBigger
                }
                MatchType.Equals -> {}
            }

            matchType = findNextTriggerTimePart(
                resultTime,
                Calendar.MINUTE,
                minutes,
                Calendar.HOUR_OF_DAY,
                minuteCompareMethod
            )
            when (matchType) {
                MatchType.NotMatch -> {
                    minuteCompareMethod = CompareMethod.EqualOrBigger
                    continue
                }
                else -> break;
            }
        }

        return resultTime
    }

    /// Find the next month/day/hour/minute should be triggered.
    private fun findNextTriggerTimePart(
        time: Calendar,
        partType: Int,
        partTriggerList: List<Int>,
        parentPartType: Int,
        compareMethod: CompareMethod
    ): MatchType {
        if (partTriggerList.isEmpty()) {
            return when (compareMethod) {
                CompareMethod.Bigger -> {
                    assert(partType == Calendar.MINUTE)
                    time.add(Calendar.MINUTE, 1)
                    MatchType.Bigger
                }
                else -> MatchType.Equals
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
                        return MatchType.Bigger
                    }
                    isEqual -> {
                        return MatchType.Equals
                    }
                    else -> {
                        assert(false)
                    }
                }
            }
        }

        time.add(parentPartType, 1)
        if (partType != Calendar.DAY_OF_WEEK) {
            time[partType] = time.getMinimum(partType)
        }
        return MatchType.NotMatch
    }

    private enum class MatchType {
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
            VibrationHome.vibrate(vibrationCount)

        App.isPlaying = true
    }

    private fun playMusic() {
        val finalMusicFolder =
            if (musicFolder.isEmpty())
                ConfigHome.data.defaultMusicFolder
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
            MusicHome.play(musicFiles[randomIndex])

        } else {
            val finalMusicFile =
                if (musicFile.isEmpty())
                    ConfigHome.data.defaultMusicFile
                else
                    musicFile

            if (finalMusicFile.isNotEmpty())
                MusicHome.play(finalMusicFile)
            else
                MusicHome.play(Settings.System.DEFAULT_ALARM_ALERT_URI)
        }
    }
}