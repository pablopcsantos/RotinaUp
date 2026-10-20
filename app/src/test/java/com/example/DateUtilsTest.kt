package com.example

import com.example.util.DateUtils
import org.junit.Assert.*
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `test getTodayDateString format`() {
        val today = DateUtils.getTodayDateString(0)
        assertTrue(today.matches(Regex("""\d{4}-\d{2}-\d{2}""")))

        val yesterday = DateUtils.getTodayDateString(-1)
        assertTrue(yesterday.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
        assertNotEquals(today, yesterday)
    }

    @Test
    fun `test getIsoDayOfWeek`() {
        // 2026-10-19 is Monday (ISO 1), 2026-10-20 is Tuesday (ISO 2), 2026-10-25 is Sunday (ISO 7)
        assertEquals(1, DateUtils.getIsoDayOfWeek("2026-10-19"))
        assertEquals(2, DateUtils.getIsoDayOfWeek("2026-10-20"))
        assertEquals(7, DateUtils.getIsoDayOfWeek("2026-10-25"))
    }

    @Test
    fun `test getDayInitial`() {
        // Monday -> S (Segunda), Sunday -> D (Domingo)
        assertEquals("S", DateUtils.getDayInitial("2026-10-19"))
        assertEquals("D", DateUtils.getDayInitial("2026-10-25"))
    }

    @Test
    fun `test formatWidgetDate`() {
        val widgetDate = DateUtils.formatWidgetDate("2026-10-20")
        assertNotNull(widgetDate)
        assertTrue(widgetDate.contains("20/10"))
    }

    @Test
    fun `test getLast7Days`() {
        val days = DateUtils.getLast7Days()
        assertEquals(7, days.size)
        assertTrue(days.last().isToday)
        assertFalse(days.first().isToday)
    }
}
