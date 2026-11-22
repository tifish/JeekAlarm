package com.tinyfish.jeekalarm.schedule

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import com.tinyfish.jeekalarm.MusicService
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.VibrationService
import com.tinyfish.jeekalarm.start.App
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.util.Calendar
import kotlin.random.Random

// A single cron schedule
@Serializable
data class Schedule(
    var id: Int = 0,
    var name: String = DefaultName,
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
    var vibrationCount: Int = 10,
    // 进入回收站的时间戳（epoch 毫秒）；0 表示是活动闹钟、不在回收站。
    var deletedAt: Long = 0L
) {
    companion object {
        /** 新建闹钟时的默认名字。 */
        const val DefaultName = "Alarm"

        /**
         * 同时触发多个闹钟时合并播放策略：
         * 音乐取第一个启用播放音乐的闹钟；振动若任意闹钟启用则取最大振动次数。
         */
        fun playCombined(schedules: List<Schedule>) {
            if (schedules.isEmpty())
                return

            val musicSchedule = schedules.firstOrNull { it.playMusic }
            val maxVibrationCount = schedules.filter { it.vibration }.maxOfOrNull { it.vibrationCount }

            val token = ScheduleService.beginPlayback()

            // 选曲和 MediaPlayer.prepare 都可能耗好几秒，必须离开主线程。
            MusicService.runSerially {
                if (!ScheduleService.isPlaybackActive(token))
                    return@runSerially

                if (musicSchedule != null) {
                    runCatching { musicSchedule.playMusic() }
                        .onFailure { Log.e(Schedule::class.java.name, "play music failed", it) }
                }

                if (!ScheduleService.isPlaybackActive(token))
                    return@runSerially

                if (maxVibrationCount != null)
                    VibrationService.vibrate(maxVibrationCount)
            }
        }
    }

    fun timeConfigChanged() {
        timeConfig = "$hourConfig:$minuteConfig W $weekDayConfig M $monthConfig D $dayConfig Y $yearConfig"
        if (onlyOnce) {
            timeConfig = "Once $timeConfig"
        }
        try {
            ScheduleParser.parseTimeConfig(this)
            isValid = true
        } catch (ex: Exception) {
            runCatching { Log.e(this.javaClass.name, ex.message ?: "parse error") }
            isValid = false
            hours.clear()
            minutes.clear()
            weekDays.clear()
            months.clear()
            days.clear()
            years.clear()
        }
    }

    /// Time config to display in UI
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

        // Gregorian calendar repeats identically every 400 years, so any valid schedule
        // matches within 400 years (or before the last configured year).
        val maxSearchYear = if (years.isNotEmpty()) years.last() else (resultTime[Calendar.YEAR] + 400).coerceAtMost(9999)

        // Prepare handler of all cases
        val minTriggerMonth = if (months.size == 0) 0 else months[0]
        val minTriggerHour = if (hours.size == 0) 0 else hours[0]
        val minTriggerMinute = if (minutes.size == 0) 0 else minutes[0]

        var minuteCompareMethod = CompareMethod.Bigger

        val setMinValueBelow = fun(timePart: Int) {
            when (timePart) {
                Calendar.YEAR -> {
                    // Reset day to 1 first so changing month never overflows (e.g. Jan 31 -> Feb -> Mar 3).
                    resultTime.set(Calendar.DAY_OF_MONTH, 1)
                    resultTime.set(Calendar.MONTH, minTriggerMonth)
                    resultTime.set(Calendar.HOUR_OF_DAY, minTriggerHour)
                    resultTime.set(Calendar.MINUTE, minTriggerMinute)
                }

                Calendar.MONTH -> {
                    resultTime.set(Calendar.DAY_OF_MONTH, 1)
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
                    resultTime.set(Calendar.DAY_OF_MONTH, 1)
                    resultTime.set(Calendar.MONTH, 0)
                    resultTime.add(Calendar.YEAR, 1)
                    setMinValueBelow(Calendar.YEAR)
                }

                Calendar.DAY_OF_MONTH -> {
                    resultTime.set(Calendar.DAY_OF_MONTH, 1)
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

            if (resultTime[Calendar.YEAR] > maxSearchYear)
                return Int.MIN_VALUE

            minuteCompareMethod = CompareMethod.EqualOrBigger

            return when {
                orgTime[Calendar.YEAR] != resultTime[Calendar.YEAR] -> Calendar.YEAR
                orgTime[Calendar.MONTH] != resultTime[Calendar.MONTH] -> Calendar.MONTH
                orgTime[Calendar.DAY_OF_MONTH] != resultTime[Calendar.DAY_OF_MONTH] -> Calendar.DAY_OF_MONTH
                orgTime[Calendar.HOUR_OF_DAY] != resultTime[Calendar.HOUR_OF_DAY] -> Calendar.HOUR_OF_DAY
                else -> Calendar.HOUR_OF_DAY
            }
        }

        val biggerHandler = fun(currentTimePart: Int): Int {
            minuteCompareMethod = CompareMethod.EqualOrBigger

            setMinValueBelow(currentTimePart)

            return when (currentTimePart) {
                // Re-check DAY_OF_MONTH so days like 29/30/31 are validated against the new month's actual length.
                Calendar.YEAR -> Calendar.DAY_OF_MONTH
                Calendar.MONTH -> Calendar.DAY_OF_MONTH
                Calendar.DAY_OF_MONTH -> Calendar.DAY_OF_WEEK
                Calendar.HOUR_OF_DAY -> Int.MAX_VALUE
                else -> throw Exception("not match")
            }
        }

        // Match from year to minute
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
                    check(partType == Calendar.MINUTE)
                    if (time[Calendar.MINUTE] >= time.getActualMaximum(Calendar.MINUTE)) {
                        MatchResult.NotMatch
                    } else {
                        time.add(Calendar.MINUTE, 1)
                        MatchResult.Bigger
                    }
                }

                else -> MatchResult.Equals
            }
        }

        val current = time[partType]
        val min = time.getActualMinimum(partType)
        val max = time.getActualMaximum(partType)

        for (value in partTriggerList) {
            if (value < min || value > max) {
                if (partType == Calendar.DAY_OF_MONTH)
                    continue
                throw Exception("invalid trigger time part $value")
            }

            val isBigger = value > current
            val isEqual = value == current
            if (compareMethod == CompareMethod.Bigger && isBigger
                || compareMethod == CompareMethod.EqualOrBigger && (isEqual || isBigger)
                || compareMethod == CompareMethod.Equal && isEqual
            ) {
                if (partType != Calendar.DAY_OF_WEEK) {
                    if (isBigger && (partType == Calendar.YEAR || partType == Calendar.MONTH))
                        time.set(Calendar.DAY_OF_MONTH, 1)
                    time[partType] = value
                }
                return if (isBigger) MatchResult.Bigger else MatchResult.Equals
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
        playCombined(listOf(this))
    }

    /**
     * 从 SAF 文件夹里随机挑一个音频文件。
     * 一次 ContentResolver 查询就把子项的 id 和 MIME 全拿回来——DocumentFile.listFiles() 之后再读 .isFile/.type
     * 是每个文件各发一次跨进程查询，几十首歌就是好几秒，主线程上直接 ANR。
     */
    private fun randomAudioInTree(treeUri: Uri): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )

        val documentIds = mutableListOf<String>()
        App.context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1)?.startsWith("audio/") == true)
                    documentIds.add(cursor.getString(0))
            }
        }

        if (documentIds.isEmpty())
            return null

        return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentIds[Random.nextInt(documentIds.size)])
    }

    private fun resolveFolderAudio(folderPath: String): Uri? {
        if (folderPath.startsWith("content://")) {
            return randomAudioInTree(Uri.parse(folderPath))
        }

        val folder = File(Environment.getExternalStorageDirectory().path, folderPath)
        val musicFiles = mutableListOf<File>()
        for (file in folder.listFiles() ?: return null) {
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            if (mimeType != null && mimeType.startsWith("audio")) {
                musicFiles.add(file)
            }
        }
        if (musicFiles.isEmpty())
            return null

        return Uri.fromFile(musicFiles[Random.nextInt(musicFiles.size)])
    }

    private fun resolveFileAudio(musicPath: String): Uri {
        return if (musicPath.startsWith("content://") || musicPath.startsWith("file://"))
            Uri.parse(musicPath)
        else
            Uri.fromFile(File(Environment.getExternalStorageDirectory().path, musicPath))
    }

    private fun playMusic() {
        // Per-alarm music settings take precedence over global defaults.
        val uriToPlay = when {
            musicFolder.isNotEmpty() -> resolveFolderAudio(musicFolder)
            musicFile.isNotEmpty() -> resolveFileAudio(musicFile)
            SettingsService.defaultMusicFolder.isNotEmpty() -> resolveFolderAudio(SettingsService.defaultMusicFolder)
            SettingsService.defaultMusicFile.isNotEmpty() -> resolveFileAudio(SettingsService.defaultMusicFile)
            else -> null
        }

        if (uriToPlay != null) {
            try {
                MusicService.play(uriToPlay)
                return
            } catch (ex: Exception) {
                Log.e(javaClass.name, "play custom music failed, falling back to default alarm", ex)
            }
        }

        MusicService.play(Settings.System.DEFAULT_ALARM_ALERT_URI)
    }
}
