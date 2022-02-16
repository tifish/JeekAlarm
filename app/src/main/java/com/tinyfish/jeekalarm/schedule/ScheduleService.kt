package com.tinyfish.jeekalarm.schedule

import com.tinyfish.jeekalarm.ConfigService
import com.tinyfish.jeekalarm.MusicService
import com.tinyfish.jeekalarm.VibrationService
import com.tinyfish.jeekalarm.alarm.AlarmService
import com.tinyfish.jeekalarm.start.App
import java.io.File
import java.util.*

object ScheduleService {
    var scheduleList = mutableListOf<Schedule>()
    private val configFile: File by lazy {
        File(App.context.filesDir, "schedule.cron")
    }

    fun loadConfig() {
        if (!configFile.exists())
            return

        scheduleList = ScheduleParser.loadFromFile(configFile)
    }

    fun saveConfig() {
        ScheduleParser.saveToFile(configFile, scheduleList)

        App.scheduleChangeTrigger++
        setNextAlarm()
    }

    var nextAlarmIndexes = mutableListOf<Int>()
        private set(value) {
            field = value
            App.nextAlarmIndexes = value.toList()
        }

    fun setNextAlarm() {
        AlarmService.cancelAlarm()
        if (scheduleList.size == 0) {
            if (nextAlarmIndexes.size > 0)
                nextAlarmIndexes = mutableListOf()
            App.stopService()
            return
        }

        var minTriggerTime = Calendar.getInstance().apply { set(9999, 12, 30) }
        val minScheduleIndexes = mutableListOf<Int>()

        for (index in scheduleList.indices) {
            val schedule = scheduleList[index]
            if (!schedule.enabled || !schedule.isValid)
                continue

            val currentTriggerTime = schedule.getNextTriggerTime() ?: continue
            if (currentTriggerTime == minTriggerTime) {
                minScheduleIndexes.add(index)
            } else if (currentTriggerTime < minTriggerTime) {
                minTriggerTime = currentTriggerTime
                minScheduleIndexes.clear()
                minScheduleIndexes.add(index)
            }
        }

        if (nextAlarmIndexes != minScheduleIndexes)
            nextAlarmIndexes = minScheduleIndexes

        if (minScheduleIndexes.isEmpty()) {
            App.stopService()
            return
        }

        AlarmService.setAlarm(minTriggerTime)
        ConfigService.save()
        App.startService()
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