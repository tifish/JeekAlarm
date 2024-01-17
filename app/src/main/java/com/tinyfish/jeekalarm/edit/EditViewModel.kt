package com.tinyfish.jeekalarm.edit

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.ai.Gemini
import com.tinyfish.jeekalarm.ai.OpenAI
import com.tinyfish.jeekalarm.globalStateOf
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App

object EditViewModel : ViewModel() {
    var isAdding = false

    var editScheduleId = -1
    lateinit var editingSchedule: Schedule

    fun initEditingSchedule() {
        editingSchedule =
            if (isAdding)
                Schedule()
            else
                ScheduleService.scheduleList.filter { it.id == editScheduleId }[0]

        editingScheduleName = editingSchedule.name
        editingScheduleEnabled = editingSchedule.enabled
        editingScheduleOnlyOnce = editingSchedule.onlyOnce
        editingSchedulePlayMusic = editingSchedule.playMusic
        editingScheduleMusicFile = editingSchedule.musicFile
        editingScheduleMusicFolder = editingSchedule.musicFolder
        editingScheduleVibration = editingSchedule.vibration
        editingScheduleVibrationCount = editingSchedule.vibrationCount
    }

    var editingScheduleName by globalStateOf("") { editingSchedule.name = it }
    var editingScheduleEnabled by globalStateOf(true) { editingSchedule.enabled = it }
    var editingScheduleOnlyOnce by globalStateOf(false) { editingSchedule.onlyOnce = it }
    var editingSchedulePlayMusic by globalStateOf(true) { editingSchedule.playMusic = it }
    var editingScheduleMusicFile by globalStateOf("") { editingSchedule.musicFile = it }
    var editingScheduleMusicFolder by globalStateOf("") { editingSchedule.musicFolder = it }
    var editingScheduleVibration by globalStateOf(true) { editingSchedule.vibration = it }
    var editingScheduleVibrationCount by globalStateOf(10) { editingSchedule.vibrationCount = it }
    var editingTimeConfigChangedTrigger by mutableIntStateOf(0)

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
        var schedule: Schedule? = null
        if (SettingsService.defaultAi == "Gemini" && SettingsService.geminiKey.isNotEmpty()) {
            schedule = Gemini.getAnswer(editingSchedule.name)
        } else if (SettingsService.defaultAi == "OpenAI" && SettingsService.openAiApiKey.isNotEmpty()) {
            OpenAI.getAnswer(editingSchedule.name)
        } else {
            return
        }

        if (schedule != null) {
            schedule.name = editingSchedule.name
            schedule.copyTo(editingSchedule)
            editingSchedule.timeConfigChanged()

            editingScheduleEnabled = editingSchedule.enabled
            editingScheduleOnlyOnce = editingSchedule.onlyOnce
            editingTimeConfigChangedTrigger++

            Toast.makeText(App.context, "Filled time automatically", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(App.context, "No time found in name", Toast.LENGTH_SHORT).show()
        }
    }
}