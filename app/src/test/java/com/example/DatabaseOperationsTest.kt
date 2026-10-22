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

    @Test
    fun `test all database migrations execute cleanly and preserve schemas`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
            .create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(null) // in-memory SQLite database
                    .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            // Version 1 tables
                            db.execSQL("CREATE TABLE IF NOT EXISTS habitos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, targetTime TEXT NOT NULL, daysOfWeek TEXT NOT NULL, durationMinutes INTEGER NOT NULL, streakCount INTEGER NOT NULL, reminderSound TEXT NOT NULL, ringDurationMinutes INTEGER NOT NULL, dismissPhrase TEXT NOT NULL, isActive INTEGER NOT NULL, positiveConsequence TEXT NOT NULL, negativeConsequence TEXT NOT NULL, lastCompletedDate TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS user_settings (id INTEGER PRIMARY KEY NOT NULL, notificationsEnabled INTEGER NOT NULL, sleepGoalMinutes INTEGER NOT NULL, bedtimeTarget TEXT NOT NULL, wakeTarget TEXT NOT NULL, requireAuth INTEGER NOT NULL, pinCode TEXT, themeMode TEXT NOT NULL, lastBackupAt INTEGER, overdueRemindersEnabled INTEGER NOT NULL, overdueIntervalMinutes INTEGER NOT NULL, overdueAlarmSound INTEGER NOT NULL, isDummyActive INTEGER NOT NULL, hasSeenDummyPrompt INTEGER NOT NULL, typeSleepName TEXT NOT NULL, typeFocusName TEXT NOT NULL, typeExerciseName TEXT NOT NULL, typeWaterName TEXT NOT NULL, typeCustomName TEXT NOT NULL)")
                        }
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    })
                    .build()
            )
        val sDb = helper.writableDatabase

        // Seed a representative pre-migration record to ensure schema changes preserve user data.
        sDb.execSQL(
            "INSERT INTO habitos (id, name, type, targetTime, daysOfWeek, durationMinutes, streakCount, " +
                "reminderSound, ringDurationMinutes, dismissPhrase, isActive, positiveConsequence, " +
                "negativeConsequence, lastCompletedDate, createdAt, updatedAt) VALUES " +
                "(42, 'Hábito legado', 'FOCUS', '08:30', '1,2,3,4,5', 20, 3, 'default', 5, " +
                "'Concluído', 1, 'Pausa', 'Retomar', NULL, 1000, 2000)"
        )

        // Test Migration 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(sDb)
        val c1 = sDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='alarm_events'")
        assertTrue("alarm_events table must exist after MIGRATION_1_2", c1.moveToFirst())
        c1.close()

        // Test Migration 2 -> 3
        AppDatabase.MIGRATION_2_3.migrate(sDb)
        val c2 = sDb.query("PRAGMA table_info(habitos)")
        var hasShowInWidget = false
        while (c2.moveToNext()) {
            if (c2.getString(1) == "showInWidget") hasShowInWidget = true
        }
        c2.close()
        assertTrue("habitos must have showInWidget column after MIGRATION_2_3", hasShowInWidget)

        // Test Migration 3 -> 4
        AppDatabase.MIGRATION_3_4.migrate(sDb)
        val c3 = sDb.query("PRAGMA table_info(user_settings)")
        var hasCustomHabitTypes = false
        while (c3.moveToNext()) {
            if (c3.getString(1) == "customHabitTypes") hasCustomHabitTypes = true
        }
        c3.close()
        assertTrue("user_settings must have customHabitTypes after MIGRATION_3_4", hasCustomHabitTypes)

        // Test Migration 4 -> 5
        AppDatabase.MIGRATION_4_5.migrate(sDb)
        val c4 = sDb.query("PRAGMA table_info(user_settings)")
        var hasMandatoryReminders = false
        while (c4.moveToNext()) {
            if (c4.getString(1) == "mandatoryRemindersEnabled") hasMandatoryReminders = true
        }
        c4.close()
        assertTrue("user_settings must have mandatoryRemindersEnabled after MIGRATION_4_5", hasMandatoryReminders)

        // Test Migration 5 -> 6
        AppDatabase.MIGRATION_5_6.migrate(sDb)
        val c5 = sDb.query("PRAGMA table_info(user_settings)")
        var hasPinHash = false
        var hasPinSalt = false
        while (c5.moveToNext()) {
            val col = c5.getString(1)
            if (col == "pinHash") hasPinHash = true
            if (col == "pinSalt") hasPinSalt = true
        }
        c5.close()
        assertTrue("user_settings must have pinHash and pinSalt after MIGRATION_5_6", hasPinHash && hasPinSalt)

        // Test Migration 6 -> 7
        AppDatabase.MIGRATION_6_7.migrate(sDb)
        val c6 = sDb.query("PRAGMA table_info(user_settings)")
        var hasSeeded = false
        while (c6.moveToNext()) {
            if (c6.getString(1) == "hasSeededInitialData") hasSeeded = true
        }
        c6.close()
        assertTrue("user_settings must have hasSeededInitialData after MIGRATION_6_7", hasSeeded)

        val preservedHabit = sDb.query("SELECT id, name, targetTime, daysOfWeek FROM habitos WHERE id = 42")
        assertTrue("Legacy habit must remain after all migrations", preservedHabit.moveToFirst())
        assertEquals(42L, preservedHabit.getLong(0))
        assertEquals("Hábito legado", preservedHabit.getString(1))
        assertEquals("08:30", preservedHabit.getString(2))
        assertEquals("1,2,3,4,5", preservedHabit.getString(3))
        preservedHabit.close()

        sDb.close()
    }
}
