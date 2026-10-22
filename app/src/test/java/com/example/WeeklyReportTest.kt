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
        assertTrue(report.suggestion.contains("pelo menos 5 dias"))
    }

    @Test
    fun `test habit adherence with DONE and PARTIAL weights`() {
        val habit = Habit(
            id = 10,
            name = "Leitura Matinal",
            type = "FOCUS",
            targetTime = "08:00",
            daysOfWeek = "1,2,3,4,5,6,7", // all 7 days
            isActive = true,
            createdAt = System.currentTimeMillis() - (10L * 24 * 3600 * 1000) // created 10 days ago
        )

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
    fun `test habit created mid-week does not count prior days against adherence`() {
        val dates = (6 downTo 0).map { DateUtils.getTodayDateString(-it) }

        // Habit was created 2 days ago (only dates[-2], dates[-1], dates[0] are eligible)
        val twoDaysAgoTimestamp = System.currentTimeMillis() - (2L * 24 * 3600 * 1000)
        val midWeekHabit = Habit(
            id = 20,
            name = "Corrida",
            type = "EXERCISE",
            targetTime = "18:00",
            daysOfWeek = "1,2,3,4,5,6,7",
            isActive = true,
            createdAt = twoDaysAgoTimestamp
        )

        // User completed all 3 days since habit was created
        val logs = listOf(
            HabitLog(habitId = 20, date = dates[4], status = "DONE"),
            HabitLog(habitId = 20, date = dates[5], status = "DONE"),
            HabitLog(habitId = 20, date = dates[6], status = "DONE")
        )

        val report = viewModel.computeWeeklyReport(
            habits = listOf(midWeekHabit),
            logs = logs,
            sleepLogs = emptyList(),
            napLogs = emptyList(),
            dailyReviews = emptyList()
        )

        // Habit has 3 eligible scheduled days, and all 3 are DONE -> 100% adherence, NOT 3/7 (42%)!
        val matrix = report.habitMatrices.first()
        assertEquals(100, matrix.rate)
        assertEquals(3, matrix.completedDaysCount)
        assertEquals(100, report.overallAdherence)
    }

    @Test
    fun `test habit adherence excludes logs recorded on unscheduled days`() {
        val dates = (6 downTo 0).map { DateUtils.getTodayDateString(-it) }

        // Habit is only scheduled on a day that does NOT match dates[0]
        val isoDayToday = DateUtils.getIsoDayOfWeek(dates.last())
        // Habit is scheduled ONLY today
        val singleDayHabit = Habit(
            id = 30,
            name = "Faxina Semanal",
            type = "CUSTOM",
            targetTime = "10:00",
            daysOfWeek = "$isoDayToday",
            isActive = true,
            createdAt = System.currentTimeMillis() - (10L * 24 * 3600 * 1000)
        )

        // Log done today, but also log recorded on an unscheduled yesterday
        val logs = listOf(
            HabitLog(habitId = 30, date = dates[5], status = "DONE"), // unscheduled day
            HabitLog(habitId = 30, date = dates[6], status = "DONE")  // scheduled today
        )

        val report = viewModel.computeWeeklyReport(
            habits = listOf(singleDayHabit),
            logs = logs,
            sleepLogs = emptyList(),
            napLogs = emptyList(),
            dailyReviews = emptyList()
        )

        // Only 1 scheduled day in the week, 1 done on that scheduled day -> 100%
        val matrix = report.habitMatrices.first()
        assertEquals(100, matrix.rate)
        assertEquals(1, matrix.completedDaysCount)
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
        assertTrue(report.bedtimeVariationHours!! > 0.0)
    }

    @Test
    fun `test habit with no scheduled days in window produces null rate and zero scheduled days`() {
        val habitWithoutDays = Habit(
            id = 40,
            name = "Hábito Futuro",
            type = "FOCUS",
            targetTime = "14:00",
            daysOfWeek = "", // empty
            isActive = true,
            createdAt = System.currentTimeMillis()
        )

        val report = viewModel.computeWeeklyReport(
            habits = listOf(habitWithoutDays),
            logs = emptyList(),
            sleepLogs = emptyList(),
            napLogs = emptyList(),
            dailyReviews = emptyList()
        )

        assertEquals(1, report.habitMatrices.size)
        val matrix = report.habitMatrices.first()
        assertNull("Rate should be null when there are no scheduled days", matrix.rate)
        assertEquals(0, matrix.scheduledDaysCount)
        assertEquals(0, matrix.completedDaysCount)
        assertEquals(0, report.overallAdherence)
    }

    @Test
    fun `test hasEnoughData requires habits and scheduled checks and is consistent in report return`() {
        val dates = (6 downTo 0).map { DateUtils.getTodayDateString(-it) }
        // 5 days of sleep logs, but NO active habits
        val sleepLogs = (0..4).map { idx ->
            SleepLog(
                date = dates[idx],
                bedtimeStr = "23:00",
                wakeTimeStr = "07:00",
                durationMinutes = 480
            )
        }

        val reportWithoutHabits = viewModel.computeWeeklyReport(
            habits = emptyList(),
            logs = emptyList(),
            sleepLogs = sleepLogs,
            napLogs = emptyList(),
            dailyReviews = emptyList()
        )
        // Even though daysCount == 5, hasEnoughData must be false because there are no active habits or scheduled checks
        assertEquals(5, reportWithoutHabits.daysCount)
        assertFalse(reportWithoutHabits.hasEnoughData)

        // Now with active habit scheduled 7 days and 5 days logged
        val habit = Habit(
            id = 50,
            name = "Meditação",
            type = "FOCUS",
            targetTime = "07:30",
            daysOfWeek = "1,2,3,4,5,6,7",
            isActive = true,
            createdAt = System.currentTimeMillis() - (10L * 24 * 3600 * 1000)
        )
        val logs = (0..4).map { idx ->
            HabitLog(habitId = 50, date = dates[idx], status = "DONE")
        }

        val reportWithHabits = viewModel.computeWeeklyReport(
            habits = listOf(habit),
            logs = logs,
            sleepLogs = sleepLogs,
            napLogs = emptyList(),
            dailyReviews = emptyList()
        )
        assertTrue(reportWithHabits.hasEnoughData)
    }

    @Test
    fun `test medals evaluate only scheduled eligible days`() {
        val dates = (6 downTo 0).map { DateUtils.getTodayDateString(-it) }
        val todayIso = DateUtils.getIsoDayOfWeek(dates.last())

        // Wake habit scheduled ONLY 1 day a week (today)
        val wakeHabit = Habit(
            id = 60,
            name = "Despertar com Luz Natural",
            type = "SLEEP",
            targetTime = "06:30",
            daysOfWeek = "$todayIso",
            isActive = true,
            createdAt = System.currentTimeMillis() - (10L * 24 * 3600 * 1000)
        )

        // 5 DONE logs, but 4 are on unscheduled days
        val logs = (0..4).map { idx ->
            HabitLog(habitId = 60, date = dates[idx], status = "DONE")
        }

        val report = viewModel.computeWeeklyReport(
            habits = listOf(wakeHabit),
            logs = logs,
            sleepLogs = emptyList(),
            napLogs = emptyList(),
            dailyReviews = emptyList()
        )

        val wakeMedal = report.medals.find { it.id == "despertar_vitorioso" }
        assertNotNull(wakeMedal)
        assertFalse("Medal must not unlock when completions are on unscheduled days", wakeMedal!!.unlocked)
        // Since only the last day (if matches) is eligible, current progress is < 5
        assertTrue(wakeMedal.currentProgress < 5f)
    }
}
