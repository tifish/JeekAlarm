package com.tinyfish.jeekalarm.schedule

import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import com.beust.klaxon.Json
import com.tinyfish.jeekalarm.App
import com.tinyfish.jeekalarm.Config
import com.tinyfish.jeekalarm.Music
import com.tinyfish.jeekalarm.Vibration
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

    @Json(ignored = true)
    var timeConfig: String = ""
        private set

    @Json(ignored = true)
    var isValid: Boolean = false

    @Json(ignored = true)
    var minutes = mutableListOf<Int>()

    @Json(ignored = true)
    var hours = mutableListOf<Int>()

    @Json(ignored = true)
    var days = mutableListOf<Int>()

    @Json(ignored = true)
    var months = mutableListOf<Int>()

    @Json(ignored = true)
    var weekDays = mutableListOf<Int>()

    fun getNextTriggerTime(calendar: Calendar? = null): Calendar? {
        if (!isValid)
            return null

        val result =
            if (calendar == null)
                Calendar.getInstance()
            else
                calendar.clone() as Calendar
        result.clear(Calendar.SECOND)
        result.clear(Calendar.MILLISECOND)
        var minuteCompareMethod = CompareMethod.Bigger

        while (true) {
            var matchType = processTriggerCalendarPart(
                result,
                Calendar.MONTH,
                months,
                Calendar.YEAR,
                CompareMethod.EqualOrBigger
            )
            if (matchType == MatchType.NotMatch) {
                result.set(result[Calendar.YEAR], result.getMinimum(Calendar.MONTH), 1, 0, 0)
                minuteCompareMethod = CompareMethod.EqualOrBigger
                continue
            } else if (matchType == MatchType.Bigger) {
                result.set(
                    result[Calendar.YEAR],
                    result[Calendar.MONTH],
                    days[0],
                    hours[0],
                    minutes[0]
                )
                minuteCompareMethod = CompareMethod.EqualOrBigger
            }

            matchType = processTriggerCalendarPart(
                result,
                Calendar.DAY_OF_MONTH,
                days,
                Calendar.MONTH,
                CompareMethod.EqualOrBigger
            )
            if (matchType == MatchType.NotMatch) {
                minuteCompareMethod = CompareMethod.EqualOrBigger
                continue
            } else if (matchType == MatchType.Bigger) {
                result.set(
                    result[Calendar.YEAR],
                    result[Calendar.MONTH],
                    result[Calendar.DAY_OF_MONTH],
                    hours[0],
                    minutes[0]
                )
                minuteCompareMethod = CompareMethod.EqualOrBigger
            }

            matchType = processTriggerCalendarPart(
                result,
                Calendar.DAY_OF_WEEK,
                weekDays,
                Calendar.DAY_OF_MONTH,
                CompareMethod.Equal
            )
            if (matchType == MatchType.NotMatch) {
                minuteCompareMethod = CompareMethod.EqualOrBigger
                continue
            }

            matchType = processTriggerCalendarPart(
                result,
                Calendar.HOUR_OF_DAY,
                hours,
                Calendar.DAY_OF_MONTH,
                CompareMethod.EqualOrBigger
            )
            if (matchType == MatchType.NotMatch) {
                minuteCompareMethod = CompareMethod.EqualOrBigger
                continue
            } else if (matchType == MatchType.Bigger) {
                result.set(
                    result[Calendar.YEAR],
                    result[Calendar.MONTH],
                    result[Calendar.DAY_OF_MONTH],
                    result[Calendar.HOUR_OF_DAY],
                    minutes[0]
                )
                minuteCompareMethod = CompareMethod.EqualOrBigger
            }

            matchType = processTriggerCalendarPart(
                result,
                Calendar.MINUTE,
                minutes,
                Calendar.HOUR_OF_DAY,
                minuteCompareMethod
            )
            if (matchType == MatchType.NotMatch) {
                minuteCompareMethod = CompareMethod.EqualOrBigger
                continue
            }

            break
        }

        return result
    }

    private fun processTriggerCalendarPart(
        calendar: Calendar?,
        partType: Int,
        partList: List<Int>,
        parentPartType: Int,
        compareMethod: CompareMethod
    ): MatchType {
        assert(partList.isNotEmpty())
        val current = calendar!![partType]
        val min = calendar.getActualMinimum(partType)
        val max = calendar.getActualMaximum(partType)

        for (index in partList) {
            if (index < min || index > max) {
                continue
            }

            val isBigger = index > current
            val isEqual = index == current
            if (compareMethod == CompareMethod.Bigger && isBigger
                || compareMethod == CompareMethod.EqualOrBigger && (isEqual || isBigger)
                || compareMethod == CompareMethod.Equal && isEqual
            ) {
                if (partType != Calendar.DAY_OF_WEEK) {
                    calendar[partType] = index
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

        calendar.add(parentPartType, 1)
        if (partType != Calendar.DAY_OF_WEEK) {
            calendar[partType] = calendar.getMinimum(partType)
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
            Vibration.vibrate(vibrationCount)

        App.isPlaying.value = true
    }

    private fun playMusic() {
        val finalMusicFolder =
            if (musicFolder.isEmpty())
                Config.data.defaultMusicFolder
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
            Music.play(musicFiles[randomIndex])

        } else {
            val finalMusicFile =
                if (musicFile.isEmpty())
                    Config.data.defaultMusicFile
                else
                    musicFile

            if (finalMusicFile.isNotEmpty())
                Music.play(finalMusicFile)
            else
                Music.play(Settings.System.DEFAULT_ALARM_ALERT_URI)
        }
    }
}