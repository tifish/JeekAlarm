package com.tinyfish.jeekalarm

import com.tinyfish.jeekalarm.schedule.ScheduleParser
import com.tinyfish.jeekalarm.start.App
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

    @Test
    fun pastFixedYearReturnsNullInsteadOfRollingOver() {
        val cron = ScheduleParser.parseTextLine("name 8 0 * 1 1 2026")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2026, Calendar.JUNE, 1, 10, 0) }
        )
        Assert.assertNull(result)
    }

    @Test
    fun pastFixedYearSameDayAfterHourReturnsNull() {
        val cron = ScheduleParser.parseTextLine("name 8 0 * 31 12 2026")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2026, Calendar.DECEMBER, 31, 9, 0) }
        )
        Assert.assertNull(result)
    }

    @Test
    fun day31SkipsShorterMonthsWithoutCrashing() {
        val cron = ScheduleParser.parseTextLine("name 8 0 * 31 * *")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2026, Calendar.APRIL, 10, 8, 0) }
        )!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2026, Calendar.MAY, 31, 8, 0) }.time,
            result.time
        )
    }

    @Test
    fun feb29SkipsNonLeapYearsInsteadOfMarch1() {
        val cron = ScheduleParser.parseTextLine("name 8 0 * 29 2 *")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2026, Calendar.JANUARY, 31, 0, 0) }
        )!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2028, Calendar.FEBRUARY, 29, 8, 0) }.time,
            result.time
        )
    }

    @Test
    fun impossibleDateReturnsNull() {
        val feb31 = ScheduleParser.parseTextLine("name 8 0 * 31 2 *")
        Assert.assertNull(
            feb31.getNextTriggerTime(
                Calendar.getInstance().apply { clear(); set(2026, Calendar.JANUARY, 1, 0, 0) }
            )
        )

        val apr31 = ScheduleParser.parseTextLine("name 8 0 * 31 4 *")
        Assert.assertNull(
            apr31.getNextTriggerTime(
                Calendar.getInstance().apply { clear(); set(2026, Calendar.JANUARY, 1, 0, 0) }
            )
        )
    }

    @Test
    fun wildcardMinuteAt59RespectsHourConstraint() {
        val cron = ScheduleParser.parseTextLine("name 23 * * * * *")
        val result = cron.getNextTriggerTime(
            Calendar.getInstance().apply { clear(); set(2026, Calendar.MARCH, 1, 23, 59) }
        )!!
        Assert.assertEquals(
            Calendar.getInstance().apply { clear(); set(2026, Calendar.MARCH, 2, 23, 0) }.time,
            result.time
        )
    }

    @Test
    fun dayDiffAcrossYearBoundary() {
        val dec31 = Calendar.getInstance().apply { clear(); set(2026, Calendar.DECEMBER, 31, 23, 0) }
        val jan1 = Calendar.getInstance().apply { clear(); set(2027, Calendar.JANUARY, 1, 8, 0) }
        val jan2 = Calendar.getInstance().apply { clear(); set(2027, Calendar.JANUARY, 2, 8, 0) }
        Assert.assertEquals(1L, App.dayDiff(dec31, jan1))
        Assert.assertEquals(2L, App.dayDiff(dec31, jan2))
    }

}
