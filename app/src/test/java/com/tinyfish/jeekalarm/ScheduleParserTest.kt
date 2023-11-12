package com.tinyfish.jeekalarm

import com.tinyfish.jeekalarm.schedule.ScheduleParser
import org.junit.Assert
import org.junit.Test

class ScheduleParserTest {
    @Test
    fun parseLine1() {
        val cronSchedule = ScheduleParser.parseTextLine("name * * * * * *")
        Assert.assertEquals("name", cronSchedule.name)
        val emptyList = mutableListOf<Int>()
        Assert.assertEquals(emptyList, cronSchedule.minutes)
        Assert.assertEquals(emptyList, cronSchedule.hours)
        Assert.assertEquals(emptyList, cronSchedule.days)
        Assert.assertEquals(emptyList, cronSchedule.months)
        Assert.assertEquals(emptyList, cronSchedule.weekDays)
        Assert.assertEquals(emptyList, cronSchedule.years)
    }

    @Test
    fun parseLine2() {
        val cronSchedule = ScheduleParser.parseTextLine("name 23 50 1 1 4")
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(50), cronSchedule.minutes)
        Assert.assertEquals(listOf(23), cronSchedule.hours)
        Assert.assertEquals(listOf(1), cronSchedule.days)
        Assert.assertEquals(listOf(3), cronSchedule.months)
        Assert.assertEquals(listOf(2), cronSchedule.weekDays)
    }

    @Test
    fun parseLine3() {
        val cronSchedule = ScheduleParser.parseTextLine("name 23 40 2,3 30 4,5 *")
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(40), cronSchedule.minutes)
        Assert.assertEquals(listOf(23), cronSchedule.hours)
        Assert.assertEquals(listOf(30), cronSchedule.days)
        Assert.assertEquals(listOf(3, 4), cronSchedule.months)
        Assert.assertEquals(listOf(3, 4), cronSchedule.weekDays)
    }

    @Test
    fun parseLine4() {
        val cronSchedule = ScheduleParser.parseTextLine("name 23 40 2-4 30 4-6 *")
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(40), cronSchedule.minutes)
        Assert.assertEquals(listOf(23), cronSchedule.hours)
        Assert.assertEquals(listOf(30), cronSchedule.days)
        Assert.assertEquals(listOf(3, 4, 5), cronSchedule.months)
        Assert.assertEquals(listOf(3, 4, 5), cronSchedule.weekDays)
    }

    @Test
    fun parseLineSunday1() {
        val cronSchedule = ScheduleParser.parseTextLine("name 23 40 5-7 30 4-6 *")
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(40), cronSchedule.minutes)
        Assert.assertEquals(listOf(23), cronSchedule.hours)
        Assert.assertEquals(listOf(30), cronSchedule.days)
        Assert.assertEquals(listOf(3, 4, 5), cronSchedule.months)
        Assert.assertEquals(listOf(1, 6, 7), cronSchedule.weekDays)
    }

    @Test
    fun parseLineSunday2() {
        val cronSchedule = ScheduleParser.parseTextLine("name 23 40 0-2 30 4-6 *")
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(40), cronSchedule.minutes)
        Assert.assertEquals(listOf(23), cronSchedule.hours)
        Assert.assertEquals(listOf(30), cronSchedule.days)
        Assert.assertEquals(listOf(3, 4, 5), cronSchedule.months)
        Assert.assertEquals(listOf(1, 2, 3), cronSchedule.weekDays)
    }

    @Test
    fun parseLineInterval() {
        val cronSchedule = ScheduleParser.parseTextLine("name 1 */10 1 1 1 *")
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(0, 10, 20, 30, 40, 50), cronSchedule.minutes)
        Assert.assertEquals(listOf(1), cronSchedule.hours)
        Assert.assertEquals(listOf(1), cronSchedule.days)
        Assert.assertEquals(listOf(0), cronSchedule.months)
        Assert.assertEquals(listOf(2), cronSchedule.weekDays)
    }

    @Test
    fun parseLineYear() {
        val cronSchedule = ScheduleParser.parseTextLine("name 1 1 * 1 1 2038-2040")
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(1), cronSchedule.minutes)
        Assert.assertEquals(listOf(1), cronSchedule.hours)
        Assert.assertEquals(listOf(1), cronSchedule.days)
        Assert.assertEquals(listOf(0), cronSchedule.months)
        Assert.assertEquals(listOf<Int>(), cronSchedule.weekDays)
        Assert.assertEquals(listOf(2038, 2039, 2040), cronSchedule.years)
    }

}
