package com.tinyfish.jeekalarm

import com.tinyfish.jeekalarm.ai.OpenAi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.Calendar
import java.util.TimeZone

class OpenAiTest {
    @Test
    fun yearlyReminderDoesNotRequireSunday() {
        val schedule = OpenAi.parseGptResult("* 10 30 20 0 0 0 0")!!

        assertTrue(schedule.isValid)
        assertFalse(schedule.onlyOnce)
        assertEquals("*", schedule.weekDayConfig)
        val now = calendar(2026, Calendar.NOVEMBER, 14, 22, 32)
        val expected = calendar(2027, Calendar.OCTOBER, 30, 20, 0)
        assertEquals(expected.time, schedule.getNextTriggerTime(now)?.time)
    }

    @Test
    fun dailyAndMonthlyRemindersDoNotRequireSunday() {
        for (result in listOf("0 0 * 6 0 0 0 0", "0 * 10 20 0 0 0 0")) {
            val schedule = OpenAi.parseGptResult(result)!!

            assertTrue(schedule.isValid)
            assertEquals("*", schedule.weekDayConfig)
            assertTrue(schedule.weekDays.isEmpty())
        }
    }

    @Test
    fun weeklyRemindersKeepExplicitWeekdays() {
        for (weekday in 1..7) {
            val schedule = OpenAi.parseGptResult("0 0 0 20 0 0 * $weekday")!!

            assertTrue(schedule.isValid)
            assertEquals(weekday.toString(), schedule.weekDayConfig)
            assertEquals(listOf(weekday % 7 + 1), schedule.weekDays)
        }
    }

    @Test
    fun wildcardWeekdayIsUnrestricted() {
        val schedule = OpenAi.parseGptResult("* 10 30 20 0 0 0 *")!!

        assertEquals("*", schedule.weekDayConfig)
        assertTrue(schedule.weekDays.isEmpty())
    }

    @Test
    fun absoluteHourDoesNotUseWildcardMinute() {
        val schedule = OpenAi.parseGptResult("0 0 0 8 0 0 0 0")!!
        assertEquals("8", schedule.hourConfig)
        assertEquals("0", schedule.minuteConfig)
        assertTrue(schedule.onlyOnce)

        val midnightHalf = OpenAi.parseGptResult("0 0 0 0 30 0 0 0")!!
        assertEquals("0", midnightHalf.hourConfig)
        assertEquals("30", midnightHalf.minuteConfig)
    }

    @Test
    fun eveningTwelveHourIsNormalizedTo24Hour() {
        val fixedNow = LocalDateTime.of(2026, 11, 22, 10, 0) // Sunday
        val schedule = OpenAi.parseGptResult("0 0 0 6 0 4 n1 3") { fixedNow }!!
        assertEquals("18", schedule.hourConfig)
        assertEquals("0", schedule.minuteConfig)
    }

    @Test
    fun relativeDaysWithFixedHourZeroesMinute() {
        val fixedNow = LocalDateTime.of(2026, 11, 22, 10, 47)
        val schedule = OpenAi.parseGptResult("0 0 +3 8 0 0 0 0") { fixedNow }!!
        assertEquals("2026", schedule.yearConfig)
        assertEquals("11", schedule.monthConfig)
        assertEquals("25", schedule.dayConfig)
        assertEquals("8", schedule.hourConfig)
        assertEquals("0", schedule.minuteConfig)
    }

    @Test
    fun sameWeekdayInPastAdvancesToNextWeek() {
        // 2026-11-25 is Wednesday (dayOfWeek = 3), 15:00
        val fixedNow = LocalDateTime.of(2026, 11, 25, 15, 0)
        val schedule = OpenAi.parseGptResult("0 0 0 9 0 1 0 3") { fixedNow }!!
        assertEquals("2026", schedule.yearConfig)
        assertEquals("12", schedule.monthConfig)
        assertEquals("2", schedule.dayConfig)
        assertEquals("9", schedule.hourConfig)
        assertEquals("0", schedule.minuteConfig)
    }

    @Test
    fun malformedResultReturnsNull() {
        assertNull(OpenAi.parseGptResult("not a valid schedule output"))
        assertNull(OpenAi.parseGptResult("2026年 1 1 8 0 0 0 0"))
    }

    private fun calendar(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
            clear()
            set(year, month, day, hour, minute)
        }
}
