package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.data.model.SleepLog
import com.example.data.model.UserSettings
import com.example.util.DateUtils
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExampleUnitTest {

    @Test
    fun `test date rollover and ISO day calculation consistency`() {
        val todayStr = DateUtils.getTodayDateString(0)
        assertTrue(todayStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))

        val tomorrowStr = DateUtils.getTodayDateString(1)
        val yesterdayStr = DateUtils.getTodayDateString(-1)
        assertNotEquals(todayStr, tomorrowStr)
        assertNotEquals(todayStr, yesterdayStr)

        val isoDay = DateUtils.getIsoDayOfWeek(todayStr)
        assertTrue("ISO day must be between 1 (Monday) and 7 (Sunday)", isoDay in 1..7)

        val widgetDate = DateUtils.formatWidgetDate(todayStr)
        assertTrue(widgetDate.isNotBlank())
    }

    @Test
    fun `test getLast7Days ordering and structure`() {
        val days = DateUtils.getLast7Days()
        assertEquals(7, days.size)
        // Last item must be today
        assertTrue(days.last().isToday)
        // All previous 6 items must not be today
        for (i in 0..5) {
            assertFalse(days[i].isToday)
        }
    }

    @Test
    fun `test user settings dummy content flags integrity`() {
        val settings = UserSettings(
            hasSeededInitialData = true,
            isDummyActive = false,
            hasSeenDummyPrompt = true
        )
        assertTrue(settings.hasSeededInitialData)
        assertFalse(settings.isDummyActive)
        assertTrue(settings.hasSeenDummyPrompt)

        // Custom types parser
        val customTypes = settings.getCustomTypesList()
        assertTrue(customTypes.isNotEmpty())

        val allTypes = settings.getAllHabitTypes()
        assertTrue(allTypes.any { it.first == "SLEEP" })
        assertTrue(allTypes.any { it.first == "FOCUS" })
    }

    @Test
    fun `test bedtime target time diff calculation with cross-midnight`() {
        // Bedtime 23:30 to Wake 07:00 -> 7.5 hours = 450 minutes
        val (bH, bM) = "23:30".split(":").map { it.toInt() }
        val (wH, wM) = "07:00".split(":").map { it.toInt() }
        var diff = (wH * 60 + wM) - (bH * 60 + bM)
        if (diff <= 0) diff += 24 * 60
        assertEquals(450, diff)
    }

    @Test
    fun `test bedtime standard deviation mathematical edge cases`() {
        // Case 1: Identical bedtimes -> 0.0 variance
        val list1 = listOf(23 * 60, 23 * 60, 23 * 60)
        val mean1 = list1.average()
        val variance1 = list1.map { (it - mean1) * (it - mean1) }.average()
        val stdDev1 = Math.sqrt(variance1)
        assertEquals(0.0, stdDev1, 0.001)

        // Case 2: Cross-midnight adjustment (e.g. 23:30 and 00:30)
        // 23:30 -> (23 - 24)*60 + 30 = -30 min
        // 00:30 -> 30 min
        val list2 = listOf(-30.0, 30.0)
        val mean2 = list2.average() // 0.0
        val variance2 = list2.map { (it - mean2) * (it - mean2) }.average() // 900.0
        val stdDevMinutes2 = Math.sqrt(variance2) // 30.0 min
        val stdDevHours2 = Math.round((stdDevMinutes2 / 60.0) * 10.0) / 10.0
        assertEquals(0.5, stdDevHours2, 0.01)
    }

    @Test
    fun `test habit schedule on ISO day verification`() {
        val habit = Habit(
            id = 1,
            name = "Treino",
            targetTime = "18:00",
            daysOfWeek = "1,3,5" // Mon, Wed, Fri
        )
        assertTrue(habit.isScheduledOn(1))
        assertFalse(habit.isScheduledOn(2))
        assertTrue(habit.isScheduledOn(3))
        assertFalse(habit.isScheduledOn(4))
        assertTrue(habit.isScheduledOn(5))
        assertFalse(habit.isScheduledOn(6))
        assertFalse(habit.isScheduledOn(7))
    }
}
