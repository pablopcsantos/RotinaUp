package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseOperationsTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test deleteHabitAndLogs removes habit and its associated logs`() = runBlocking {
        val habitDao = db.habitDao()
        val habitId = habitDao.insertHabit(
            Habit(name = "Meditação", targetTime = "06:30")
        )

        habitDao.insertOrUpdateLog(HabitLog(habitId = habitId, date = "2026-10-20", status = "DONE"))
        habitDao.insertOrUpdateLog(HabitLog(habitId = habitId, date = "2026-10-19", status = "PARTIAL"))

        val initialLogs = habitDao.getAllLogs().first()
        assertEquals(2, initialLogs.size)

        val habit = habitDao.getHabitById(habitId).first()
        assertNotNull(habit)

        // Perform cascading deletion
        habitDao.deleteHabitAndLogs(habit!!)

        val remainingHabits = habitDao.getAllHabits().first()
        assertTrue(remainingHabits.isEmpty())

        val remainingLogs = habitDao.getAllLogs().first()
        assertTrue(remainingLogs.isEmpty())
    }

    @Test
    fun `test settings persistence with hashed PIN credentials and seed flag`() = runBlocking {
        val settingsDao = db.settingsDao()
        val settings = UserSettings(
            id = 1,
            requireAuth = true,
            pinHash = "PBKDF2\$10000\$e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            pinSalt = "f8a7b6c5d4e3f201",
            hasSeededInitialData = true
        )

        settingsDao.insertOrUpdateSettings(settings)

        val retrieved = settingsDao.getSettings().first()
        assertNotNull(retrieved)
        assertTrue(retrieved!!.requireAuth)
        assertEquals("PBKDF2\$10000\$e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", retrieved.pinHash)
        assertEquals("f8a7b6c5d4e3f201", retrieved.pinSalt)
        assertNull(retrieved.pinCode)
        assertTrue(retrieved.hasPin())
        assertTrue(retrieved.hasSeededInitialData)
    }

    @Test
    fun `test isScheduledOn domain semantics`() {
        // Default: all 7 days
        val allDaysHabit = Habit(name = "Hidratação", targetTime = "07:00", daysOfWeek = "1,2,3,4,5,6,7")
        for (day in 1..7) {
            assertTrue("Default habit should be scheduled on day $day", allDaysHabit.isScheduledOn(day))
        }

        // Empty string: strictly no days scheduled
        val emptyDaysHabit = Habit(name = "Pausa", targetTime = "12:00", daysOfWeek = "")
        for (day in 1..7) {
            assertFalse("Empty daysOfWeek habit should NOT be scheduled on day $day", emptyDaysHabit.isScheduledOn(day))
        }

        // Weekdays only: 1 to 5
        val weekdaysHabit = Habit(name = "Trabalho", targetTime = "09:00", daysOfWeek = "1,2,3,4,5")
        assertTrue(weekdaysHabit.isScheduledOn(1))
        assertTrue(weekdaysHabit.isScheduledOn(5))
        assertFalse(weekdaysHabit.isScheduledOn(6)) // Saturday
        assertFalse(weekdaysHabit.isScheduledOn(7)) // Sunday
    }
}
