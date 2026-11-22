package com.tinyfish.jeekalarm.edit

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tinyfish.jeekalarm.R
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

    // 每次进入/离开编辑页递增，防止在途的 AI 请求回写到后续打开的其他闹钟。
    private var editSessionId = 0L
    private var activeSession = false
    private var initialEditing = Schedule()

    // 单一真相源：正在编辑的副本。所有界面读它、改它，Compose 自动重组。
    var editing by mutableStateOf(Schedule())
        private set

    val hasChanges: Boolean
        get() = editing != initialEditing

    fun startEditing(scheduleId: Int) {
        editSessionId++
        activeSession = true
        editScheduleId = scheduleId
        isAdding = scheduleId == -1
        val initial =
            if (isAdding)
                Schedule()
            else
                ScheduleService.findSchedule(scheduleId)?.copy() ?: Schedule()
        initialEditing = initial
        editing = initial
    }

    /** 进程被系统回收后从 NavBackStack 恢复 EditRoute 时，按 route 中的 scheduleId 重新载入编辑态。 */
    fun ensureEditing(scheduleId: Int) {
        if (!activeSession || editScheduleId != scheduleId) {
            startEditing(scheduleId)
        }
    }

    fun cancelEditing() {
        editSessionId++
        activeSession = false
    }

    /** 以不可变方式更新编辑副本：editing = transform(editing) */
    fun update(transform: (Schedule) -> Schedule) {
        editing = transform(editing)
    }

    fun saveEditingSchedule() {
        editSessionId++
        activeSession = false
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
        val sessionId = editSessionId
        val queryName = editing.name
        scope.launch { guess(sessionId, queryName) }
    }

    private suspend fun guess(sessionId: Long, queryName: String) {
        if (SettingsService.openAiApiKey.isEmpty())
            return

        val schedule = try {
            OpenAi.getAnswer(queryName)
        } catch (ex: Exception) {
            if (sessionId == editSessionId) {
                Toast.makeText(App.context, App.localizedContext().getString(R.string.edit_ai_request_failed, ex.message ?: ""), Toast.LENGTH_LONG).show()
            }
            return
        }

        if (sessionId != editSessionId)
            return

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
            Toast.makeText(App.context, App.localizedContext().getString(R.string.edit_filled_time_auto), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(App.context, App.localizedContext().getString(R.string.edit_no_time_found), Toast.LENGTH_SHORT).show()
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
            enabled = true,
        )
    }

    fun play() {
        editing.play()
    }
}
