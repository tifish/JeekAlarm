package com.tinyfish.jeekalarm.schedule

import com.tinyfish.jeekalarm.*
import com.tinyfish.jeekalarm.alarm.Alarm
import com.tinyfish.jeekalarm.alarm.Notification
import java.io.File
import java.util.*

object ScheduleManager {
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
        try {
            Alarm.cancelAlarm()
            if (scheduleList.size == 0) {
                if (nextAlarmIndexes.size > 0)
                    nextAlarmIndexes = mutableListOf()
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
                return
            }

            Alarm.setAlarm(minTriggerTime)
            Config.save()
        } finally {
            Notification.showInformation()
        }
    }

    fun stopPlaying() {
        Music.stop()
        Vibration.stop()

        App.isPlaying.value = false
    }

    fun pausePlaying() {
        Music.pause()
        Vibration.stop()

        App.isPlaying.value = false
    }

    fun resumePlaying() {
        Music.resume()

        App.isPlaying.value = true
    }
}