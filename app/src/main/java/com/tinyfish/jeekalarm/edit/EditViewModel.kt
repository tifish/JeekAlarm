package com.tinyfish.jeekalarm.edit

import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.ai.DeepSeek
import com.tinyfish.jeekalarm.ai.Gemini
import com.tinyfish.jeekalarm.globalStateOf
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import java.util.Calendar

object EditViewModel : ViewModel() {
    var isAdding = false

    var editScheduleId = -1
    private lateinit var editingSchedule: Schedule

    fun initEditingSchedule() {
        editingSchedule =
            if (isAdding)
                Schedule()
            else
                ScheduleService.scheduleList.filter { it.id == editScheduleId }[0]

        editingScheduleName = editingSchedule.name
        editingScheduleMinuteConfig = editingSchedule.minuteConfig
        editingScheduleHourConfig = editingSchedule.hourConfig
        editingScheduleDayConfig = editingSchedule.dayConfig
        editingScheduleMonthConfig = editingSchedule.monthConfig
        editingScheduleWeekDayConfig = editingSchedule.weekDayConfig
        editingScheduleYearConfig = editingSchedule.yearConfig
        editingScheduleEnabled = editingSchedule.enabled
        editingScheduleOnlyOnce = editingSchedule.onlyOnce
        editingSchedulePlayMusic = editingSchedule.playMusic
        editingScheduleMusicFile = editingSchedule.musicFile
        editingScheduleMusicFolder = editingSchedule.musicFolder
        editingScheduleVibration = editingSchedule.vibration
        editingScheduleVibrationCount = editingSchedule.vibrationCount
    }

    var editingScheduleName by globalStateOf("") { editingSchedule.name = it }
    var editingScheduleMinuteConfig by globalStateOf("") { editingSchedule.minuteConfig = it }
    var editingScheduleHourConfig by globalStateOf("") { editingSchedule.hourConfig = it }
    var editingScheduleDayConfig by globalStateOf("") { editingSchedule.dayConfig = it }
    var editingScheduleMonthConfig by globalStateOf("") { editingSchedule.monthConfig = it }
    var editingScheduleWeekDayConfig by globalStateOf("") { editingSchedule.weekDayConfig = it }
    var editingScheduleYearConfig by globalStateOf("") { editingSchedule.yearConfig = it }
    var editingScheduleEnabled by globalStateOf(true) { editingSchedule.enabled = it }
    var editingScheduleOnlyOnce by globalStateOf(false) { editingSchedule.onlyOnce = it }
    var editingSchedulePlayMusic by globalStateOf(true) { editingSchedule.playMusic = it }
    var editingScheduleMusicFile by globalStateOf("") { editingSchedule.musicFile = it }
    var editingScheduleMusicFolder by globalStateOf("") { editingSchedule.musicFolder = it }
    var editingScheduleVibration by globalStateOf(true) { editingSchedule.vibration = it }
    var editingScheduleVibrationCount by globalStateOf(10) { editingSchedule.vibrationCount = it }

    fun saveEditingSchedule() {
        editingSchedule.timeConfigChanged()
        if (isAdding) {
            editingSchedule.id = ScheduleService.nextScheduleId++
            ScheduleService.scheduleList.add(editingSchedule)
        }
        ScheduleService.sort()
        ScheduleService.saveAndRefresh()
    }

    suspend fun guessEditingScheduleFromName() {
        val schedule: Schedule?
        if (SettingsService.defaultAi == "Gemini" && SettingsService.geminiKey.isNotEmpty()) {
            schedule = Gemini.getAnswer(editingSchedule.name)
        } else if (SettingsService.defaultAi == "DeepSeek" && SettingsService.deepSeekApiKey.isNotEmpty()) {
            schedule = DeepSeek.getAnswer(editingSchedule.name)
        } else {
            return
        }

        if (schedule != null) {
            editingScheduleMinuteConfig = schedule.minuteConfig
            editingScheduleHourConfig = schedule.hourConfig
            editingScheduleDayConfig = schedule.dayConfig
            editingScheduleMonthConfig = schedule.monthConfig
            editingScheduleWeekDayConfig = schedule.weekDayConfig
            editingScheduleYearConfig = schedule.yearConfig
            editingScheduleEnabled = schedule.enabled
            editingScheduleOnlyOnce = schedule.onlyOnce
            editingSchedule.timeConfigChanged()

            Toast.makeText(App.context, "Filled time automatically", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(App.context, "No time found in name", Toast.LENGTH_SHORT).show()
        }
    }

    fun setEditingScheduleTime(time: Calendar) {
        time.apply {
            editingScheduleMinuteConfig = get(Calendar.MINUTE).toString()
            editingScheduleHourConfig = get(Calendar.HOUR_OF_DAY).toString()
            editingScheduleDayConfig = get(Calendar.DAY_OF_MONTH).toString()
            editingScheduleMonthConfig = (get(Calendar.MONTH) + 1).toString()
            editingScheduleWeekDayConfig = "*"
            editingScheduleYearConfig = (get(Calendar.YEAR)).toString()
        }
    }

    fun play() {
        editingSchedule.play()
    }
}