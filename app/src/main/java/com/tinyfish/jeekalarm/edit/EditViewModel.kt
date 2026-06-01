package com.tinyfish.jeekalarm.edit

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.ai.OpenAi
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

object EditViewModel {
    // App 级作用域：用于"按名字猜时间"，不随屏幕切换被取消（取代 GlobalScope）
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var isAdding by mutableStateOf(false)
        private set

    var editScheduleId = -1
        private set

    // 单一真相源：正在编辑的副本。所有界面读它、改它，Compose 自动重组。
    var editing by mutableStateOf(Schedule())
        private set

    fun startEditing(scheduleId: Int) {
        editScheduleId = scheduleId
        isAdding = scheduleId == -1
        editing =
            if (isAdding)
                Schedule()
            else
                ScheduleService.findSchedule(scheduleId)?.copy() ?: Schedule()
    }

    /** 以不可变方式更新编辑副本：editing = transform(editing) */
    fun update(transform: (Schedule) -> Schedule) {
        editing = transform(editing)
    }

    fun saveEditingSchedule() {
        editing.timeConfigChanged()
        if (isAdding) {
            editing.id = ScheduleService.createScheduleId()
            ScheduleService.scheduleList.add(editing)
        } else {
            val index = ScheduleService.scheduleList.indexOfFirst { it.id == editScheduleId }
            if (index >= 0)
                ScheduleService.scheduleList[index] = editing
        }
        ScheduleService.sort()
        ScheduleService.saveAndRefresh()
    }

    fun guessFromName() {
        scope.launch { guess() }
    }

    private suspend fun guess() {
        if (SettingsService.openAiApiKey.isEmpty())
            return

        val schedule = try {
            OpenAi.getAnswer(editing.name)
        } catch (ex: Exception) {
            Toast.makeText(App.context, "AI request failed: ${ex.message}", Toast.LENGTH_LONG).show()
            return
        }

        if (schedule != null) {
            editing = editing.copy(
                minuteConfig = schedule.minuteConfig,
                hourConfig = schedule.hourConfig,
                dayConfig = schedule.dayConfig,
                monthConfig = schedule.monthConfig,
                weekDayConfig = schedule.weekDayConfig,
                yearConfig = schedule.yearConfig,
                enabled = schedule.enabled,
                onlyOnce = schedule.onlyOnce,
            )
            Toast.makeText(App.context, "Filled time automatically", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(App.context, "No time found in name", Toast.LENGTH_SHORT).show()
        }
    }

    fun setEditingScheduleTime(time: Calendar) {
        editing = editing.copy(
            minuteConfig = time.get(Calendar.MINUTE).toString(),
            hourConfig = time.get(Calendar.HOUR_OF_DAY).toString(),
            dayConfig = time.get(Calendar.DAY_OF_MONTH).toString(),
            monthConfig = (time.get(Calendar.MONTH) + 1).toString(),
            weekDayConfig = "*",
            yearConfig = time.get(Calendar.YEAR).toString(),
        )
    }

    fun play() {
        editing.play()
    }
}
