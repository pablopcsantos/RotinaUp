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
    fun `test settings persistence with hashed PIN credentials`() = runBlocking {
        val settingsDao = db.settingsDao()
        val settings = UserSettings(
            id = 1,
            requireAuth = true,
            pinHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            pinSalt = "f8a7b6c5d4e3f201"
        )

        settingsDao.insertOrUpdateSettings(settings)

        val retrieved = settingsDao.getSettings().first()
        assertNotNull(retrieved)
        assertTrue(retrieved!!.requireAuth)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", retrieved.pinHash)
        assertEquals("f8a7b6c5d4e3f201", retrieved.pinSalt)
        assertNull(retrieved.pinCode)
        assertTrue(retrieved.hasPin())
    }
}
