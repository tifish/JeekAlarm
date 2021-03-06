package com.tinyfish.jeekalarm

import com.tinyfish.jeekalarm.schedule.ScheduleParser
import org.junit.Assert
import org.junit.Test

class ScheduleParserTest {
    @Test
    fun parseLine1() {
        val cronSchedule = ScheduleParser.parseTextLine("name * * * * * {}")!!
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals((0..59).toList(), cronSchedule.minutes)
        Assert.assertEquals((0..23).toList(), cronSchedule.hours)
        Assert.assertEquals((1..31).toList(), cronSchedule.days)
        Assert.assertEquals((0..11).toList(), cronSchedule.months)
        Assert.assertEquals((1..7).toList(), cronSchedule.weekDays)
    }

    @Test
    fun parseLine2() {
        val cronSchedule = ScheduleParser.parseTextLine("name 50 23 1 4 1 {}")!!
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(50), cronSchedule.minutes)
        Assert.assertEquals(listOf(23), cronSchedule.hours)
        Assert.assertEquals(listOf(1), cronSchedule.days)
        Assert.assertEquals(listOf(3), cronSchedule.months)
        Assert.assertEquals(listOf(2), cronSchedule.weekDays)
    }

    @Test
    fun parseLine3() {
        val cronSchedule = ScheduleParser.parseTextLine("name 40 23 30 4,5 2,3 {}")!!
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(40), cronSchedule.minutes)
        Assert.assertEquals(listOf(23), cronSchedule.hours)
        Assert.assertEquals(listOf(30), cronSchedule.days)
        Assert.assertEquals(listOf(3, 4), cronSchedule.months)
        Assert.assertEquals(listOf(3, 4), cronSchedule.weekDays)
    }

    @Test
    fun parseLine4() {
        val cronSchedule = ScheduleParser.parseTextLine("name 40 23 30 4-6 2-4 {}")!!
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(40), cronSchedule.minutes)
        Assert.assertEquals(listOf(23), cronSchedule.hours)
        Assert.assertEquals(listOf(30), cronSchedule.days)
        Assert.assertEquals(listOf(3, 4, 5), cronSchedule.months)
        Assert.assertEquals(listOf(3, 4, 5), cronSchedule.weekDays)
    }

    @Test
    fun parseLineSunday1() {
        val cronSchedule = ScheduleParser.parseTextLine("name 40 23 30 4-6 5-7 {}")!!
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(40), cronSchedule.minutes)
        Assert.assertEquals(listOf(23), cronSchedule.hours)
        Assert.assertEquals(listOf(30), cronSchedule.days)
        Assert.assertEquals(listOf(3, 4, 5), cronSchedule.months)
        Assert.assertEquals(listOf(1, 6, 7), cronSchedule.weekDays)
    }

    @Test
    fun parseLineSunday2() {
        val cronSchedule = ScheduleParser.parseTextLine("name 40 23 30 4-6 0-2 {}")!!
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(40), cronSchedule.minutes)
        Assert.assertEquals(listOf(23), cronSchedule.hours)
        Assert.assertEquals(listOf(30), cronSchedule.days)
        Assert.assertEquals(listOf(3, 4, 5), cronSchedule.months)
        Assert.assertEquals(listOf(1, 2, 3), cronSchedule.weekDays)
    }

    @Test
    fun parseLineInterval() {
        val cronSchedule = ScheduleParser.parseTextLine("name */10 1 1 1 1 {}")!!
        Assert.assertEquals("name", cronSchedule.name)
        Assert.assertEquals(listOf(0, 10, 20, 30, 40, 50), cronSchedule.minutes)
        Assert.assertEquals(listOf(1), cronSchedule.hours)
        Assert.assertEquals(listOf(1), cronSchedule.days)
        Assert.assertEquals(listOf(0), cronSchedule.months)
        Assert.assertEquals(listOf(2), cronSchedule.weekDays)
    }

}
