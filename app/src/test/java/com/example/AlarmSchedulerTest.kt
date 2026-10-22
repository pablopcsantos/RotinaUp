package com.example

import com.example.util.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmSchedulerTest {

    private fun localTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `next trigger uses ISO weekday and returns the selected future time`() {
        // 19 October 2026 is a Monday. Thursday is ISO weekday 4.
        val now = localTime(2026, Calendar.OCTOBER, 19, 9, 15)
        val expected = localTime(2026, Calendar.OCTOBER, 22, 8, 0)

        assertEquals(expected, AlarmScheduler.calculateNextTriggerTime("08:00", "4", now))
    }

    @Test
    fun `next trigger skips a scheduled time that has already passed today`() {
        val now = localTime(2026, Calendar.OCTOBER, 19, 9, 15)
        val expected = localTime(2026, Calendar.OCTOBER, 26, 8, 0)

        assertEquals(expected, AlarmScheduler.calculateNextTriggerTime("08:00", "1", now))
    }

    @Test
    fun `invalid times are rejected instead of normalized into another time`() {
        val now = localTime(2026, Calendar.OCTOBER, 19, 9, 15)

        assertNull(AlarmScheduler.calculateNextTriggerTime("25:00", "1", now))
        assertNull(AlarmScheduler.calculateNextTriggerTime("08:60", "1", now))
        assertNull(AlarmScheduler.calculateNextTriggerTime("08:00:30", "1", now))
        assertNull(AlarmScheduler.calculateNextTriggerTime("invalid", "1", now))
    }

    @Test
    fun `empty or invalid weekday lists do not create an alarm`() {
        val now = localTime(2026, Calendar.OCTOBER, 19, 9, 15)

        assertNull(AlarmScheduler.calculateNextTriggerTime("08:00", "", now))
        assertNull(AlarmScheduler.calculateNextTriggerTime("08:00", "0,8,invalid", now))
    }
    @Test
    fun `mandatory deadline schedules later today when deadline has not passed`() {
        val now = localTime(2026, Calendar.OCTOBER, 19, 21, 30)
        val expected = localTime(2026, Calendar.OCTOBER, 19, 22, 0)

        assertEquals(expected, AlarmScheduler.calculateTodayOrTomorrowTime("22:00", now))
    }

    @Test
    fun `mandatory deadline schedules tomorrow when deadline has passed`() {
        val now = localTime(2026, Calendar.OCTOBER, 19, 22, 0)
        val expected = localTime(2026, Calendar.OCTOBER, 20, 22, 0)

        assertEquals(expected, AlarmScheduler.calculateTodayOrTomorrowTime("22:00", now))
    }

    @Test
    fun `invalid mandatory deadline falls back to 22 00 instead of calendar normalization`() {
        val now = localTime(2026, Calendar.OCTOBER, 19, 21, 0)
        val expected = localTime(2026, Calendar.OCTOBER, 19, 22, 0)

        assertEquals(expected, AlarmScheduler.calculateTodayOrTomorrowTime("99:99", now))
        assertEquals(expected, AlarmScheduler.calculateTodayOrTomorrowTime("not-a-time", now))
    }
}
