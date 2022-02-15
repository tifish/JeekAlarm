package com.tinyfish.jeekalarm

import com.tinyfish.jeekalarm.schedule.ScheduleParser
import org.junit.Assert
import org.junit.Test
import java.util.*

class CronScheduleTest {
    @Test
    fun nextTriggerTime0() {
        val cron = ScheduleParser.parseTextLine("name * * * * *")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2017, 2, 1, 23, 44) })!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2017, 2, 1, 23, 45) }.time, result.time
        )
    }

    @Test
    fun nextTriggerTime0a() {
        val cron = ScheduleParser.parseTextLine("name * 1 * * *")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2017, 2, 1, 23, 44) })!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2017, 2, 2, 0, 1) }.time, result.time
        )
    }

    @Test
    fun nextTriggerTime1() {
        val cron = ScheduleParser.parseTextLine("name 23 50 * 1 3")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2017, 2, 1, 23, 44) })!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2017, 2, 1, 23, 50) }.time, result.time
        )
    }

    @Test
    fun nextTriggerTime2() {
        val cron = ScheduleParser.parseTextLine("name 23 40 * 1 3")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2017, 2, 1, 23, 44) })!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2018, 2, 1, 23, 40) }.time, result.time
        )
    }

    @Test
    fun nextTriggerTime3() {
        val cron = ScheduleParser.parseTextLine("name 23 44 * 1 3")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2017, 2, 1, 23, 44) })!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2018, 2, 1, 23, 44) }.time, result.time
        )
    }

    @Test
    fun nextTriggerTime4() {
        val cron = ScheduleParser.parseTextLine("name 23 40 0 30 3,4")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2017, 1, 30, 0, 0) })!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2017, 3, 30, 23, 40) }.time, result.time
        )
    }

    @Test
    fun nextTriggerTime5() {
        val cron = ScheduleParser.parseTextLine("name 0 0 5 1 3")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2017, 2, 1, 23, 44) })!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2019, 2, 1, 0, 0) }.time, result.time
        )
    }

    @Test
    fun nextTriggerTimeYear() {
        val cron = ScheduleParser.parseTextLine("name 1 1 * 1 1 2038-2040")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2017, 2, 1, 23, 44) })!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2038, 0, 1, 1, 1) }.time, result.time
        )
    }

}
