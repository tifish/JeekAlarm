package com.tinyfish.jeekalarm.schedule

import com.tinyfish.jeekalarm.ConfigHome
import com.tinyfish.jeekalarm.MusicHome
import com.tinyfish.jeekalarm.VibrationHome
import com.tinyfish.jeekalarm.alarm.AlarmHome
import com.tinyfish.jeekalarm.start.App
import java.io.File
import java.util.*

object ScheduleHome {
    var scheduleList = mutableListOf<Schedule>()
    private val configFile: File by lazy {
        File(App.context.getExternalFilesDir(null), "schedule.cron")
    }

    fun loadConfig() {
        if (!configFile.exists())
            return

        scheduleList = ScheduleParser.loadFromFile(configFile)
    }

    fun saveConfig() {
        ScheduleParser.saveToFile(configFile, scheduleList)

        App.scheduleChangeTrigger.value++
        setNextAlarm()
    }

    var nextAlarmIndexes = mutableListOf<Int>()
        private set(value) {
            field = value
            App.nextAlarmIndexes.value = value.toList()
        }

    fun setNextAlarm() {
        AlarmHome.cancelAlarm()
        if (scheduleList.size == 0) {
            if (nextAlarmIndexes.size > 0)
                nextAlarmIndexes = mutableListOf()
            App.stopService()
            return
        }

        var minTriggerTime = Calendar.getInstance().apply { set(9999, 12, 30) }
        val minScheduleIndexes = mutableListOf<Int>()

        for ((index, schedule) in scheduleList.withIndex()) {
            if (!schedule.enabled || !schedule.isValid)
                continue

            val currentTriggerTime = schedule.getNextTriggerTime()!!
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

        AlarmHome.setAlarm(minTriggerTime)
        ConfigHome.save()
        App.startService()
    }

    fun stopPlaying() {
        MusicHome.stop()
        VibrationHome.stop()

        App.isPlaying.value = false
    }

    fun pausePlaying() {
        MusicHome.pause()
        VibrationHome.stop()

        App.isPlaying.value = false
    }

    fun resumePlaying() {
        MusicHome.resume()

        App.isPlaying.value = true
    }
}