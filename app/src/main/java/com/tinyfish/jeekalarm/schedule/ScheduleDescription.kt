package com.tinyfish.jeekalarm.schedule

import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.start.App

// 周日=1 … 周六=7（Calendar 约定）。索引 = weekday - 1。跟随当前语言。
private fun weekDayShortNames(): Array<String> =
    App.localizedContext().resources.getStringArray(R.array.weekday_short_names)

// 索引 = Calendar.MONTH（0 起）。跟随当前语言。
private fun monthShortNames(): Array<String> =
    App.localizedContext().resources.getStringArray(R.array.month_short_names)

private fun local(resId: Int): String = App.localizedContext().getString(resId)

/**
 * 把重复规则归纳成一句人话，用于主列表的副标题。
 *
 * 只覆盖闹钟里最常见的几种模式，命中则返回短语，否则返回 null——
 * 由调用方退回到“下次触发时间”这个对任意 cron 都成立的兜底显示。
 * 基于解析后的整型集合（空集合 = 通配 *），不碰原始 config 串。
 */
fun Schedule.describeRecurrence(): String? {
    if (!isValid) return null
    if (onlyOnce) return local(R.string.recur_once)

    val anyWeekDay = weekDays.isEmpty()
    val anyDay = days.isEmpty()
    val anyMonth = months.isEmpty()
    val anyYear = years.isEmpty()

    return when {
        // 每天：星期/日/月/年都不限定
        anyWeekDay && anyDay && anyMonth && anyYear -> local(R.string.recur_daily)

        // 按星期重复：限定星期，日/月/年都不限定
        !anyWeekDay && anyDay && anyMonth && anyYear -> describeWeekDays(weekDays)

        // 每月某天：限定单个日，不按星期，月/年不限定
        anyWeekDay && !anyDay && anyMonth && anyYear && days.size == 1 ->
            App.localizedContext().getString(R.string.recur_monthly_day, days[0])

        // 每年某月某日：限定单月单日，不按星期、年份不限定
        anyWeekDay && !anyDay && !anyMonth && anyYear && days.size == 1 && months.size == 1 ->
            App.localizedContext().getString(R.string.recur_yearly_date, monthShortNames()[months[0]], days[0])

        // 其余任意组合：交给兜底
        else -> null
    }
}

private fun describeWeekDays(weekDays: List<Int>): String {
    val set = weekDays.toSet()
    return when {
        set == setOf(2, 3, 4, 5, 6) -> local(R.string.recur_weekdays)
        set == setOf(1, 7) -> local(R.string.recur_weekends)
        else -> {
            val names = weekDayShortNames()
            local(R.string.recur_weekly_prefix) + weekDays.sorted().joinToString(", ") { names[it - 1] }
        }
    }
}
