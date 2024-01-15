package com.tinyfish.jeekalarm.schedule

import android.os.Environment
import com.tinyfish.jeekalarm.MusicService
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.VibrationService
import com.tinyfish.jeekalarm.alarm.AlarmService
import com.tinyfish.jeekalarm.start.App
import java.io.File
import java.util.Calendar

object ScheduleService {
    var scheduleList = mutableListOf<Schedule>()
    var nextScheduleId = 1

    val configFile: File
        get() {
            val dir = if (SettingsService.settingsDir == "")
                App.context.filesDir
            else
                File(Environment.getExternalStorageDirectory().path, SettingsService.settingsDir)

            return File(dir, "schedule.cron")
        }

    fun load() {
        if (!configFile.exists())
            return

        scheduleList = ScheduleParser.loadFromFile(configFile)
    }

    fun loadAndRefresh() {
        load()

        setNextAlarm()
        App.scheduleChangedTrigger++
    }

    fun save() {
        ScheduleParser.saveToFile(configFile, scheduleList)
    }

    fun saveAndRefresh() {
        save()

        setNextAlarm()
        App.scheduleChangedTrigger++
    }

    fun sort() {
        val now = Calendar.getInstance()
        scheduleList.sortWith { s1, s2 -> s1.getNextTriggerTime(now)!!.compareTo(s2.getNextTriggerTime(now)!!) }
    }

    var nextAlarmIds = mutableListOf<Int>()
        private set(value) {
            field = value
            App.nextAlarmIds = value.toList()
        }

    // Find and set next alarm, and save the ids to nextAlarmIds
    fun setNextAlarm() {
        AlarmService.cancelAlarm()
        if (scheduleList.size == 0) {
            if (nextAlarmIds.size > 0)
                nextAlarmIds = mutableListOf()
            App.stopService()
            return
        }

        var minTriggerTime = Calendar.getInstance().apply { set(9999, 12, 30) }
        val minScheduleIds = mutableListOf<Int>()
        val now = Calendar.getInstance()

        for (index in scheduleList.indices) {
            val schedule = scheduleList[index]
            if (!schedule.enabled || !schedule.isValid)
                continue

            val currentTriggerTime = schedule.getNextTriggerTime(now) ?: continue
            if (currentTriggerTime == minTriggerTime) {
                minScheduleIds.add(schedule.id)
            } else if (currentTriggerTime < minTriggerTime) {
                minTriggerTime = currentTriggerTime
                minScheduleIds.clear()
                minScheduleIds.add(schedule.id)
            }
        }

        if (nextAlarmIds != minScheduleIds)
            nextAlarmIds = minScheduleIds

        if (minScheduleIds.isEmpty()) {
            App.stopService()
            return
        }

        AlarmService.setAlarm(minTriggerTime)
        App.startServiceAndUpdateInfo()
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