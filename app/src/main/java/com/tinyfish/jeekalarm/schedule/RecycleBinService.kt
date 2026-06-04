package com.tinyfish.jeekalarm.schedule

import androidx.compose.runtime.mutableStateListOf
import com.tinyfish.jeekalarm.SettingsService

// 回收站：手工删除的闹钟、以及触发结束的一次性闹钟都会进这里，保留 30 天后自动清除。
// 单独存一个文件，和活动闹钟列表互不干扰；回收站里的闹钟不会被调度。
object RecycleBinService {
    private const val RecycleBinFileName = "recyclebin.cron"
    private const val RetentionMillis = 30L * 24 * 60 * 60 * 1000 // 30 天

    // 按删除时间倒序（最近删除的在最前）。
    val recycleList = mutableStateListOf<Schedule>()

    fun load() {
        val loaded =
            if (SettingsService.configExists(RecycleBinFileName))
                SettingsService.readConfigLines(RecycleBinFileName).mapNotNull { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) null else ScheduleParser.parseJsonLine(trimmed)
                }
            else
                emptyList()
        recycleList.clear()
        recycleList.addAll(loaded)
        sort()
        purgeExpired()
    }

    private fun save() {
        SettingsService.writeConfigText(RecycleBinFileName, ScheduleParser.saveToString(recycleList))
    }

    private fun sort() {
        recycleList.sortByDescending { it.deletedAt }
    }

    /** 把一个闹钟放进回收站（调用方负责先把它从活动列表移除）。 */
    fun add(schedule: Schedule) {
        schedule.deletedAt = System.currentTimeMillis()
        recycleList.add(schedule)
        sort()
        save()
    }

    /** 从回收站恢复为活动闹钟。 */
    fun restore(schedule: Schedule) {
        recycleList.removeIf { it.id == schedule.id }
        save()
        ScheduleService.restoreFromRecycleBin(schedule)
    }

    /** 彻底删除一条。 */
    fun deleteForever(schedule: Schedule) {
        recycleList.removeIf { it.id == schedule.id }
        save()
    }

    /** 清空回收站。 */
    fun clearAll() {
        recycleList.clear()
        save()
    }

    /** 移除保留期已过的条目。 */
    fun purgeExpired() {
        val deadline = System.currentTimeMillis() - RetentionMillis
        val removed = recycleList.removeIf { it.deletedAt in 1 until deadline }
        if (removed)
            save()
    }

    /** 剩余天数（向上取整，至少 0），用于 UI 展示。 */
    fun daysUntilRemoval(schedule: Schedule): Int {
        val elapsed = System.currentTimeMillis() - schedule.deletedAt
        val remaining = RetentionMillis - elapsed
        if (remaining <= 0) return 0
        return ((remaining + (24L * 60 * 60 * 1000 - 1)) / (24L * 60 * 60 * 1000)).toInt()
    }
}
