package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WeeklyReportTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = MainViewModel(app)
    }

    @Test
    fun `test empty weekly report produces zero adherence and null sleep metrics`() {
        val report = viewModel.computeWeeklyReport(
            habits = emptyList(),
            logs = emptyList(),
            sleepLogs = emptyList(),
            napLogs = emptyList(),
            dailyReviews = emptyList()
        )

        assertEquals(0, report.overallAdherence)
        assertEquals(emptyList<HabitWeeklyMatrix>(), report.habitMatrices)
        assertNull("Empty sleep logs should not produce an invented average", report.avgSleepHours)
        assertNull("Empty sleep logs should not produce bedtime variation", report.bedtimeVariationHours)
        assertEquals(0, report.napCount)
        assertFalse(report.hasEnoughData)
    }

    @Test
    fun `test habit adherence with DONE and PARTIAL weights`() {
        val today = DateUtils.getTodayDateString(0)
        val habit = Habit(
            id = 10,
            name = "Leitura Matinal",
            type = "FOCUS",
            targetTime = "08:00",
            daysOfWeek = "1,2,3,4,5,6,7", // all 7 days
            isActive = true
        )

        // 3 DONE days and 2 PARTIAL days
        val dates = (6 downTo 0).map { DateUtils.getTodayDateString(-it) }
        val logs = listOf(
            HabitLog(habitId = 10, date = dates[0], status = "DONE"),
            HabitLog(habitId = 10, date = dates[1], status = "DONE"),
            HabitLog(habitId = 10, date = dates[2], status = "DONE"),
            HabitLog(habitId = 10, date = dates[3], status = "PARTIAL"),
            HabitLog(habitId = 10, date = dates[4], status = "PARTIAL")
        )

        val report = viewModel.computeWeeklyReport(
            habits = listOf(habit),
            logs = logs,
            sleepLogs = emptyList(),
            napLogs = emptyList(),
            dailyReviews = emptyList()
        )

        // 3 * 1.0 + 2 * 0.5 = 4.0 weighted checks out of 7 scheduled days -> 4 / 7 * 100 = 57%
        assertEquals(1, report.habitMatrices.size)
        val matrix = report.habitMatrices.first()
        assertEquals(57, matrix.rate)
        assertEquals(3, matrix.completedDaysCount)
        assertEquals(57, report.overallAdherence)
    }

    @Test
    fun `test sleep average and bedtime variation calculation`() {
        val dates = (6 downTo 0).map { DateUtils.getTodayDateString(-it) }
        val sleepLogs = listOf(
            SleepLog(
                date = dates[0],
                bedtimeStr = "23:00",
                wakeTimeStr = "07:00",
                durationMinutes = 480 // 8.0h
            ),
            SleepLog(
                date = dates[1],
                bedtimeStr = "23:30",
                wakeTimeStr = "07:30",
                durationMinutes = 480 // 8.0h
            )
        )

        val report = viewModel.computeWeeklyReport(
            habits = emptyList(),
            logs = emptyList(),
            sleepLogs = sleepLogs,
            napLogs = emptyList(),
            dailyReviews = emptyList()
        )

        assertNotNull(report.avgSleepHours)
        assertEquals(8.0, report.avgSleepHours!!, 0.01)
        assertNotNull(report.bedtimeVariationHours)
        // Bedtimes 23:00 (-60m) and 23:30 (-30m) -> diff is 30m, stdDev is 15m -> ~0.3h
        assertTrue(report.bedtimeVariationHours!! > 0.0)
    }
}
