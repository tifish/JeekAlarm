package com.tinyfish.jeekalarm.schedule

import android.os.Environment
import androidx.compose.runtime.mutableStateListOf
import com.tinyfish.jeekalarm.MusicService
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.VibrationService
import com.tinyfish.jeekalarm.alarm.AlarmService
import com.tinyfish.jeekalarm.alarm.NotificationService
import com.tinyfish.jeekalarm.start.App
import java.io.File
import java.util.Calendar

object ScheduleService {
    private const val ScheduleFileName = "schedule.cron"

    val scheduleList = mutableStateListOf<Schedule>()
    var nextScheduleId = 1

    val configFile: File
        get() {
            val dir = if (SettingsService.settingsDir == "" || SettingsService.settingsDir.startsWith("content://"))
                App.context.filesDir
            else
                File(Environment.getExternalStorageDirectory().path, SettingsService.settingsDir)

            return File(dir, ScheduleFileName)
        }

    fun load() {
        val loaded =
            if (SettingsService.configExists(ScheduleFileName))
                ScheduleParser.loadFromLines(SettingsService.readConfigLines(ScheduleFileName))
            else
                emptyList()
        scheduleList.clear()
        scheduleList.addAll(loaded)
        sort()
    }

    fun loadAndRefresh() {
        load()
        setNextAlarm()
    }

    fun save() {
        ensureStableIds()
        SettingsService.writeConfigText(ScheduleFileName, ScheduleParser.saveToString(scheduleList))
    }

    fun saveAndRefresh() {
        save()
        setNextAlarm()
    }

    fun sort() {
        val now = Calendar.getInstance()
        scheduleList.sortWith(
            compareBy<Schedule> { schedule ->
                if (schedule.enabled && schedule.isValid)
                    schedule.getNextTriggerTime(now)?.timeInMillis ?: Long.MAX_VALUE
                else
                    Long.MAX_VALUE
            }.thenBy { it.id }
        )
    }

    fun createScheduleId(): Int {
        // 也避开回收站里的 id，否则恢复/再次删除时回收站列表可能出现重复 id。
        val usedIds = (scheduleList.map { it.id } + RecycleBinService.recycleList.map { it.id }).toSet()
        while (nextScheduleId <= 0 || nextScheduleId in usedIds)
            nextScheduleId++
        return nextScheduleId++
    }

    fun findSchedule(id: Int): Schedule? {
        return scheduleList.firstOrNull { it.id == id }
    }

    fun setEnabled(id: Int, enabled: Boolean) {
        val index = scheduleList.indexOfFirst { it.id == id }
        if (index < 0) return
        scheduleList[index] = scheduleList[index].copy(enabled = enabled)
        saveAndRefresh()
    }

    private fun ensureStableIds() {
        val usedIds = mutableSetOf<Int>()
        for (schedule in scheduleList) {
            if (schedule.id <= 0 || schedule.id in usedIds)
                schedule.id = createScheduleId()
            usedIds.add(schedule.id)
        }
    }

    var nextAlarmIds = mutableListOf<Int>()
        private set(value) {
            field = value
            App.nextAlarmIds = value.toList()
        }

    private data class NextAlarm(
        val triggerTime: Calendar,
        val scheduleIds: MutableList<Int>,
    )

    private fun findNextAlarm(now: Calendar = Calendar.getInstance()): NextAlarm? {
        var minTriggerTime: Calendar? = null
        val minScheduleIds = mutableListOf<Int>()

        for (schedule in scheduleList) {
            if (!schedule.enabled || !schedule.isValid)
                continue

            val currentTriggerTime = schedule.getNextTriggerTime(now) ?: continue
            val currentMinTriggerTime = minTriggerTime
            when {
                currentMinTriggerTime == null || currentTriggerTime < currentMinTriggerTime -> {
                    minTriggerTime = currentTriggerTime
                    minScheduleIds.clear()
                    minScheduleIds.add(schedule.id)
                }

                currentTriggerTime.timeInMillis == currentMinTriggerTime.timeInMillis -> {
                    minScheduleIds.add(schedule.id)
                }
            }
        }

        val triggerTime = minTriggerTime ?: return null
        return NextAlarm(triggerTime, minScheduleIds)
    }

    fun setNextAlarm() {
        AlarmService.cancelAlarm()

        val nextAlarm = findNextAlarm()
        if (nextAlarm == null) {
            if (nextAlarmIds.isNotEmpty())
                nextAlarmIds = mutableListOf()
            NotificationService.updateInfo()
            return
        }

        if (nextAlarmIds != nextAlarm.scheduleIds)
            nextAlarmIds = nextAlarm.scheduleIds

        AlarmService.setAlarm(nextAlarm.triggerTime, nextAlarm.scheduleIds)
        NotificationService.updateInfo()
    }

    fun completeTriggeredAlarms(alarmIds: List<Int>) {
        var modified = false
        for (alarmId in alarmIds) {
            val schedule = findSchedule(alarmId) ?: continue
            if (schedule.onlyOnce && schedule.enabled) {
                schedule.enabled = false
                modified = true
            }
        }

        if (modified)
            save()

        sort()
        setNextAlarm()
    }

    /** 把闹钟移入回收站（手工删除走这里）。 */
    fun recycle(schedule: Schedule) {
        scheduleList.removeIf { it.id == schedule.id }
        saveAndRefresh()
        RecycleBinService.add(schedule)
    }

    /** 响铃关闭时调用：触发结束的一次性闹钟自动进回收站。 */
    fun recycleTriggeredOnceAlarms(alarmIds: List<Int>) {
        var modified = false
        for (alarmId in alarmIds) {
            val schedule = findSchedule(alarmId) ?: continue
            if (schedule.onlyOnce) {
                scheduleList.removeIf { it.id == schedule.id }
                RecycleBinService.add(schedule)
                modified = true
            }
        }
        if (modified)
            saveAndRefresh()
    }

    /** 从回收站恢复：清掉删除标记、重新分配 id 避免冲突，加回活动列表。 */
    fun restoreFromRecycleBin(schedule: Schedule) {
        schedule.deletedAt = 0L
        schedule.id = createScheduleId()
        scheduleList.add(schedule)
        sort()
        saveAndRefresh()
    }

    fun stopPlaying() {
        MusicService.stop()
        VibrationService.stop()

        App.isPlaying = false
    }

    fun pausePlaying() {
        MusicService.pause()
        VibrationService.stop()

        App.isPlaying = false
    }

    fun resumePlaying() {
        MusicService.resume()

        App.isPlaying = true
    }
}
