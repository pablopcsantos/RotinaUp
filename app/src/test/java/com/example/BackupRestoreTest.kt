package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.util.*
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
class BackupRestoreTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test export and import round trip preserves all entities`() = runBlocking {
        // 1. Seed initial data
        val habitDao = db.habitDao()
        val sleepDao = db.sleepDao()
        val settingsDao = db.settingsDao()

        val habit1Id = habitDao.insertHabit(
            Habit(
                id = 101,
                name = "Foco Matinal",
                type = "FOCUS",
                targetTime = "08:30",
                durationMinutes = 45,
                daysOfWeek = "1,2,3,4,5",
                priority = 1,
                positiveConsequence = "Alta produtividade e clareza mental",
                negativeConsequence = "Sobrecarga de tarefas pendentes",
                dismissPhrase = "foco total no trabalho"
            )
        )

        habitDao.insertOrUpdateLog(
            HabitLog(
                id = 201,
                habitId = habit1Id,
                date = "2026-10-22",
                status = "DONE",
                note = "Cumprido com energia"
            )
        )

        sleepDao.insertSleepLog(
            SleepLog(
                id = 301,
                date = "2026-10-22",
                bedtimeStr = "22:45",
                wakeTimeStr = "06:45",
                durationMinutes = 480,
                quality = 5,
                awakenings = 0,
                awakeningsUnnoticed = false,
                note = "Excelente noite de descanso"
            )
        )

        sleepDao.insertNapLog(
            NapLog(
                id = 401,
                date = "2026-10-22",
                hasNap = false,
                napTime = "13:30",
                napMinutes = 0,
                note = "Vigília preservada"
            )
        )

        sleepDao.insertDailyReview(
            DailyReview(
                id = 501,
                date = "2026-10-22",
                mood = 5,
                energy = 4,
                focus = 5,
                note = "Dia impecável"
            )
        )

        settingsDao.insertAlarmEvent(
            AlarmEvent(
                id = 601,
                habitId = habit1Id,
                habitName = "Foco Matinal",
                scheduledAt = 1729600000000L,
                dismissedAt = 1729600015000L,
                dismissAttempts = 1,
                snoozed = false,
                typedText = "foco total no trabalho"
            )
        )

        settingsDao.insertOrUpdateSettings(
            UserSettings(
                id = 1,
                sleepGoalMinutes = 510,
                bedtimeTarget = "22:30",
                wakeTarget = "06:30",
                typeFocusName = "Trabalho Profundo",
                mandatoryDeadlineTime = "22:15"
            )
        )

        // 2. Export complete backup
        val exportedJson = BackupManager.exportCompleteBackup(db)
        assertNotNull(exportedJson)
        assertTrue(exportedJson.contains("Foco Matinal"))
        assertTrue(exportedJson.contains("Trabalho Profundo"))
        assertTrue(exportedJson.contains("2026-10-22"))

        // 3. Validate backup JSON
        val validation = BackupManager.validateBackupJson(exportedJson)
        assertTrue("Validation should succeed", validation is BackupValidationResult.Success)
        val success = validation as BackupValidationResult.Success
        assertEquals(1, success.preview.habitsCount)
        assertEquals(1, success.preview.habitLogsCount)
        assertEquals(1, success.preview.sleepLogsCount)
        assertEquals(1, success.preview.napLogsCount)
        assertEquals(1, success.preview.dailyReviewsCount)
        assertEquals(1, success.preview.alarmEventsCount)
        assertTrue(success.preview.hasSettings)

        // 4. Clear database completely to simulate fresh restore
        habitDao.deleteAllHabits()
        habitDao.deleteAllLogs()
        sleepDao.deleteAllSleepLogs()
        sleepDao.deleteAllNapLogs()
        sleepDao.deleteAllDailyReviews()
        settingsDao.deleteAllAlarmEvents()

        assertEquals(0, habitDao.getAllHabits().first().size)
        assertEquals(0, habitDao.getAllLogs().first().size)

        // 5. Restore from payload
        val restoreResult = BackupManager.restoreBackup(context, db, success.payload, replaceExisting = true)
        assertTrue(restoreResult.success)
        assertEquals(1, restoreResult.habitsRestored)
        assertEquals(1, restoreResult.logsRestored)
        assertEquals(1, restoreResult.sleepLogsRestored)
        assertEquals(1, restoreResult.reviewsRestored)

        // 6. Verify restored records in Room
        val restoredHabits = habitDao.getAllHabits().first()
        assertEquals(1, restoredHabits.size)
        assertEquals("Foco Matinal", restoredHabits[0].name)
        assertEquals("08:30", restoredHabits[0].targetTime)
        assertEquals("foco total no trabalho", restoredHabits[0].dismissPhrase)

        val restoredLogs = habitDao.getAllLogs().first()
        assertEquals(1, restoredLogs.size)
        assertEquals("DONE", restoredLogs[0].status)
        assertEquals("2026-10-22", restoredLogs[0].date)

        val restoredSleep = sleepDao.getAllSleepLogs().first()
        assertEquals(1, restoredSleep.size)
        assertEquals(5, restoredSleep[0].quality)
        assertEquals("22:45", restoredSleep[0].bedtimeStr)

        val restoredReviews = sleepDao.getAllDailyReviews().first()
        assertEquals(1, restoredReviews.size)
        assertEquals("Dia impecável", restoredReviews[0].note)

        val restoredSettings = settingsDao.getSettings().first()
        assertNotNull(restoredSettings)
        assertEquals("Trabalho Profundo", restoredSettings!!.typeFocusName)
        assertEquals("22:15", restoredSettings.mandatoryDeadlineTime)
    }

    @Test
    fun `failed replacement restore rolls back all database changes`() = runBlocking {
        val habitDao = db.habitDao()
        val sleepDao = db.sleepDao()
        val settingsDao = db.settingsDao()

        val localHabitId = habitDao.insertHabit(
            Habit(id = 101, name = "Hábito local preservado", targetTime = "07:30")
        )
        habitDao.insertOrUpdateLog(
            HabitLog(
                id = 201,
                habitId = localHabitId,
                date = "2026-10-20",
                status = "DONE",
                note = "Histórico local que não pode desaparecer"
            )
        )
        sleepDao.insertSleepLog(
            SleepLog(id = 301, date = "2026-10-20", note = "Sono local preservado")
        )
        settingsDao.insertOrUpdateSettings(UserSettings(themeMode = "dark"))

        // Force a real SQLite failure after replacement has deleted local rows and
        // inserted the imported habit. This exercises transaction rollback rather than
        // relying on duplicate rows, which Room may replace due to its conflict strategy.
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_backup_sleep_insert
            BEFORE INSERT ON sleep_logs
            BEGIN
                SELECT RAISE(ABORT, 'forced backup restore failure');
            END
            """.trimIndent()
        )

        val invalidPayload = BackupPayload(
            habits = listOf(
                Habit(id = 1, name = "Hábito importado", targetTime = "09:00")
            ),
            sleepLogs = listOf(
                SleepLog(id = 0, date = "2026-10-21", note = "Este registro não deve ser inserido")
            )
        )

        val result = BackupManager.restoreBackup(
            context = context,
            db = db,
            payload = invalidPayload,
            replaceExisting = true
        )

        assertFalse("The invalid restore must report failure", result.success)

        val habitsAfterFailure = habitDao.getAllHabits().first()
        assertEquals(1, habitsAfterFailure.size)
        assertEquals(localHabitId, habitsAfterFailure.single().id)
        assertEquals("Hábito local preservado", habitsAfterFailure.single().name)

        val logsAfterFailure = habitDao.getAllLogs().first()
        assertEquals(1, logsAfterFailure.size)
        assertEquals("Histórico local que não pode desaparecer", logsAfterFailure.single().note)

        val sleepAfterFailure = sleepDao.getAllSleepLogs().first()
        assertEquals(1, sleepAfterFailure.size)
        assertEquals("Sono local preservado", sleepAfterFailure.single().note)

        val settingsAfterFailure = settingsDao.getSettings().first()
        assertNotNull(settingsAfterFailure)
        assertEquals("dark", settingsAfterFailure!!.themeMode)
    }

    @Test
    fun `test validation rejects corrupted or invalid JSON`() {
        // Empty content
        val emptyResult = BackupManager.validateBackupJson("")
        assertTrue(emptyResult is BackupValidationResult.Error)
        assertEquals("O conteúdo do backup está vazio.", (emptyResult as BackupValidationResult.Error).message)

        // Corrupted JSON syntax
        val syntaxError = BackupManager.validateBackupJson("{ \"schemaVersion\": 1, \"habits\": [")
        assertTrue(syntaxError is BackupValidationResult.Error)

        // Habit with empty name
        val emptyHabitName = """
            {
              "habits": [
                { "name": "", "targetTime": "08:00" }
              ]
            }
        """.trimIndent()
        val emptyNameResult = BackupManager.validateBackupJson(emptyHabitName)
        assertTrue(emptyNameResult is BackupValidationResult.Error)

        // Habit with invalid time format
        val invalidTime = """
            {
              "habits": [
                { "name": "Corrida", "targetTime": "25:99" }
              ]
            }
        """.trimIndent()
        val invalidTimeResult = BackupManager.validateBackupJson(invalidTime)
        assertTrue(invalidTimeResult is BackupValidationResult.Error)

        // Non-recognized structure
        val randomObj = """{ "some_random_key": 123 }"""
        val randomResult = BackupManager.validateBackupJson(randomObj)
        assertTrue(randomResult is BackupValidationResult.Error)

        // A malformed alarm section must not be silently discarded.
        val malformedAlarmSection = """{ "habits": [], "alarmEvents": "not-an-array" }"""
        assertTrue(
            BackupManager.validateBackupJson(malformedAlarmSection) is BackupValidationResult.Error
        )

        val malformedAlarmItem = """{ "habits": [], "alarmEvents": [null] }"""
        assertTrue(
            BackupManager.validateBackupJson(malformedAlarmItem) is BackupValidationResult.Error
        )

        val stringAlarmHabitId = """
            {
              "habits": [
                { "id": 7, "name": "Leitura", "type": "FOCUS", "targetTime": "08:00" }
              ],
              "alarmEvents": [
                { "habitId": "7", "habitName": "Leitura", "scheduledAt": 1792569600000, "dismissedAt": 1792569615000 }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Alarm habit IDs must be numeric rather than silently coerced from strings",
            BackupManager.validateBackupJson(stringAlarmHabitId) is BackupValidationResult.Error
        )

        val stringAlarmSnoozed = """
            {
              "habits": [
                { "id": 7, "name": "Leitura", "type": "FOCUS", "targetTime": "08:00" }
              ],
              "alarmEvents": [
                { "habitId": 7, "habitName": "Leitura", "scheduledAt": 1792569600000, "dismissedAt": 1792569615000, "snoozed": "false" }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Alarm snoozed must be a JSON boolean",
            BackupManager.validateBackupJson(stringAlarmSnoozed) is BackupValidationResult.Error
        )

        val invalidAlarmAttempts = """
            {
              "habits": [
                { "id": 7, "name": "Leitura", "type": "FOCUS", "targetTime": "08:00" }
              ],
              "alarmEvents": [
                { "habitId": 7, "habitName": "Leitura", "scheduledAt": 1792569600000, "dismissedAt": 1792569615000, "dismissAttempts": 0 }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Alarm dismissAttempts must be a positive integer",
            BackupManager.validateBackupJson(invalidAlarmAttempts) is BackupValidationResult.Error
        )

        // Settings have a defined object shape; invalid settings must be reported.
        val malformedSettings = """{ "habits": [], "settings": [] }"""
        assertTrue(
            BackupManager.validateBackupJson(malformedSettings) is BackupValidationResult.Error
        )

        // Settings objects also need semantically valid values, not just the right JSON shape.
        val invalidSettingsTime = """{ "habits": [], "settings": { "bedtimeTarget": "25:90" } }"""
        assertTrue(
            BackupManager.validateBackupJson(invalidSettingsTime) is BackupValidationResult.Error
        )

        val invalidSettingsTheme = """{ "habits": [], "settings": { "themeMode": "ultraviolet" } }"""
        assertTrue(
            BackupManager.validateBackupJson(invalidSettingsTheme) is BackupValidationResult.Error
        )

        val invalidSettingsInterval = """{ "habits": [], "settings": { "mandatoryIntervalMinutes": 0 } }"""
        assertTrue(
            BackupManager.validateBackupJson(invalidSettingsInterval) is BackupValidationResult.Error
        )

        // Present values with the wrong JSON primitive type must not be silently coerced/defaulted.
        val stringSettingsInterval = """{ "habits": [], "settings": { "mandatoryIntervalMinutes": "abc" } }"""
        assertTrue(
            BackupManager.validateBackupJson(stringSettingsInterval) is BackupValidationResult.Error
        )

        val stringSettingsBoolean = """{ "habits": [], "settings": { "notificationsEnabled": "false" } }"""
        assertTrue(
            BackupManager.validateBackupJson(stringSettingsBoolean) is BackupValidationResult.Error
        )

        val numericSettingsTime = """{ "habits": [], "settings": { "bedtimeTarget": 2300 } }"""
        assertTrue(
            BackupManager.validateBackupJson(numericSettingsTime) is BackupValidationResult.Error
        )
    }

    @Test
    fun `backup validation rejects out of range sleep and daily review ratings`() {
        val invalidSleepQuality = """
            {
              "schemaVersion": 1,
              "sleepLogs": [
                { "date": "2026-10-21", "quality": 9 }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Sleep quality outside 1..5 must be rejected instead of clamped",
            BackupManager.validateBackupJson(invalidSleepQuality) is BackupValidationResult.Error
        )

        val invalidReviewRating = """
            {
              "schemaVersion": 1,
              "dailyReviews": [
                { "date": "2026-10-21", "mood": 0 }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Daily review ratings outside 1..5 must be rejected",
            BackupManager.validateBackupJson(invalidReviewRating) is BackupValidationResult.Error
        )

        val stringRating = """
            {
              "schemaVersion": 1,
              "dailyReviews": [
                { "date": "2026-10-21", "focus": "4" }
              ]
            }
        """.trimIndent()
        assertTrue(
            "A rating encoded as text must be rejected rather than coerced",
            BackupManager.validateBackupJson(stringRating) is BackupValidationResult.Error
        )
    }

    @Test
    fun `backup validation rejects duplicate logical history records`() {
        val duplicateHabitLogDate = """
            {
              "schemaVersion": 1,
              "habits": [
                { "id": 7, "name": "Leitura", "type": "FOCUS", "targetTime": "08:00" }
              ],
              "habitLogs": [
                { "id": 10, "habitId": 7, "date": "2026-10-21", "status": "DONE" },
                { "id": 11, "habitId": 7, "date": "2026-10-21", "status": "PARTIAL" }
              ]
            }
        """.trimIndent()
        assertTrue(
            "A habit must not have two history records for the same date",
            BackupManager.validateBackupJson(duplicateHabitLogDate) is BackupValidationResult.Error
        )

        val duplicateSleepDate = """
            {
              "schemaVersion": 1,
              "sleepLogs": [
                { "id": 1, "date": "2026-10-21", "bedtimeStr": "23:00", "wakeTimeStr": "07:00" },
                { "id": 2, "date": "2026-10-21", "bedtimeStr": "22:30", "wakeTimeStr": "06:30" }
              ]
            }
        """.trimIndent()
        assertTrue(
            "A backup must not contain multiple sleep logs for the same date",
            BackupManager.validateBackupJson(duplicateSleepDate) is BackupValidationResult.Error
        )
    }

    @Test
    fun `backup validation rejects duplicate habit IDs and orphaned habit logs`() {
        val duplicateHabitIds = """
            {
              "schemaVersion": 1,
              "habits": [
                { "id": 7, "name": "Leitura", "type": "FOCUS", "targetTime": "08:00" },
                { "id": 7, "name": "Caminhada", "type": "EXERCISE", "targetTime": "09:00" }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Duplicate habit IDs make imported log references ambiguous",
            BackupManager.validateBackupJson(duplicateHabitIds) is BackupValidationResult.Error
        )

        val orphanedHabitLog = """
            {
              "schemaVersion": 1,
              "habits": [
                { "id": 7, "name": "Leitura", "type": "FOCUS", "targetTime": "08:00" }
              ],
              "habitLogs": [
                { "id": 10, "habitId": 99, "date": "2026-10-21", "status": "DONE" }
              ]
            }
        """.trimIndent()
        assertTrue(
            "A log referencing a habit absent from the backup must be rejected",
            BackupManager.validateBackupJson(orphanedHabitLog) is BackupValidationResult.Error
        )

        val invalidHabitLogStatus = """
            {
              "schemaVersion": 1,
              "habits": [
                { "id": 7, "name": "Leitura", "type": "FOCUS", "targetTime": "08:00" }
              ],
              "habitLogs": [
                { "id": 10, "habitId": 7, "date": "2026-10-21", "status": "TYPO" }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Unknown habit-log statuses must be rejected instead of being restored as DONE",
            BackupManager.validateBackupJson(invalidHabitLogStatus) is BackupValidationResult.Error
        )

        val logsWithoutHabits = """
            {
              "schemaVersion": 1,
              "habitLogs": [
                { "id": 10, "habitId": 99, "date": "2026-10-21", "status": "DONE" }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Habit logs cannot be restored without their referenced habits",
            BackupManager.validateBackupJson(logsWithoutHabits) is BackupValidationResult.Error
        )
    }

    @Test
    fun `backup validation rejects alarm events referencing missing habits`() {
        val orphanedAlarmEvent = """
            {
              "schemaVersion": 1,
              "habits": [
                { "id": 7, "name": "Leitura", "type": "FOCUS", "targetTime": "08:00" }
              ],
              "alarmEvents": [
                {
                  "id": 4,
                  "habitId": 99,
                  "habitName": "Hábito ausente",
                  "scheduledAt": 1792569600000,
                  "dismissedAt": 1792569615000,
                  "dismissAttempts": 1,
                  "snoozed": false,
                  "typedText": ""
                }
              ]
            }
        """.trimIndent()
        assertTrue(
            "An alarm event referencing a habit absent from the backup must be rejected",
            BackupManager.validateBackupJson(orphanedAlarmEvent) is BackupValidationResult.Error
        )

        val alarmEventsWithoutHabits = """
            {
              "schemaVersion": 1,
              "alarmEvents": [
                {
                  "id": 4,
                  "habitId": 99,
                  "habitName": "Hábito ausente",
                  "scheduledAt": 1792569600000,
                  "dismissedAt": 1792569615000,
                  "dismissAttempts": 1,
                  "snoozed": false,
                  "typedText": ""
                }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Alarm events cannot be restored without their referenced habits",
            BackupManager.validateBackupJson(alarmEventsWithoutHabits) is BackupValidationResult.Error
        )
    }

    @Test
    fun `backup validation rejects impossible calendar dates`() {
        val invalidHabitLogDate = """
            {
              "schemaVersion": 1,
              "habits": [
                { "id": 1, "name": "Leitura", "type": "FOCUS", "targetTime": "08:00" }
              ],
              "habitLogs": [
                { "id": 1, "habitId": 1, "date": "2026-02-30", "status": "DONE" }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Impossible habit-log dates must be rejected",
            BackupManager.validateBackupJson(invalidHabitLogDate) is BackupValidationResult.Error
        )

        val invalidSleepDate = """
            {
              "schemaVersion": 1,
              "sleepLogs": [
                {
                  "id": 1,
                  "date": "2026-13-01",
                  "bedtimeStr": "23:00",
                  "wakeTimeStr": "07:00",
                  "durationMinutes": 480,
                  "quality": 4
                }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Impossible sleep-log dates must be rejected",
            BackupManager.validateBackupJson(invalidSleepDate) is BackupValidationResult.Error
        )

        val invalidNapDate = """
            {
              "schemaVersion": 1,
              "napLogs": [
                { "id": 1, "date": "2026-04-31", "hasNap": true, "napTime": "13:30", "napMinutes": 20 }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Impossible nap-log dates must be rejected",
            BackupManager.validateBackupJson(invalidNapDate) is BackupValidationResult.Error
        )

        val invalidReviewDate = """
            {
              "schemaVersion": 1,
              "dailyReviews": [
                { "id": 1, "date": "2026-02-29", "mood": 3, "energy": 3, "focus": 3 }
              ]
            }
        """.trimIndent()
        assertTrue(
            "Impossible daily-review dates must be rejected",
            BackupManager.validateBackupJson(invalidReviewDate) is BackupValidationResult.Error
        )
    }

    @Test
    fun `test schedule format validation and restoration`() = runBlocking {
        val scheduleJson = """
            [
              {
                "name": "Leitura Noturna",
                "type": "FOCUS",
                "targetTime": "21:30",
                "durationMinutes": 20,
                "daysOfWeek": "1,2,3,4,5,6,7",
                "priority": 2,
                "positiveConsequence": "Mente relaxada antes de dormir",
                "negativeConsequence": "Uso excessivo de telas",
                "dismissPhrase": "leitura concluida"
              }
            ]
        """.trimIndent()

        val validation = BackupManager.validateBackupJson(scheduleJson)
        assertTrue(validation is BackupValidationResult.Success)
        val success = validation as BackupValidationResult.Success
        assertEquals(1, success.preview.habitsCount)
        assertTrue(success.preview.isScheduleOnly)

        val restoreResult = BackupManager.restoreBackup(context, db, success.payload, replaceExisting = true)
        assertTrue(restoreResult.success)

        val habits = db.habitDao().getAllHabits().first()
        assertEquals(1, habits.size)
        assertEquals("Leitura Noturna", habits[0].name)
        assertEquals("21:30", habits[0].targetTime)
    }

    @Test
    fun `test restore strictly preserves existing local security PIN credentials`() = runBlocking {
        val settingsDao = db.settingsDao()

        // Local device has a strong PIN configured
        val localSecuritySettings = UserSettings(
            id = 1,
            requireAuth = true,
            pinHash = "PBKDF2\$100000\$securehash123",
            pinSalt = "securesalt456",
            themeMode = "light"
        )
        settingsDao.insertOrUpdateSettings(localSecuritySettings)

        // External backup without PIN credentials or attempting to tamper
        val backupWithSettings = """
            {
              "schemaVersion": 1,
              "appName": "RotinaUp",
              "habits": [
                { "name": "Respiração", "targetTime": "09:00" }
              ],
              "settings": {
                "themeMode": "dark",
                "sleepGoalMinutes": 450,
                "typeSleepName": "Repouso"
              }
            }
        """.trimIndent()

        val validation = BackupManager.validateBackupJson(backupWithSettings)
        assertTrue(validation is BackupValidationResult.Success)

        val restoreResult = BackupManager.restoreBackup(
            context,
            db,
            (validation as BackupValidationResult.Success).payload,
            replaceExisting = false
        )
        assertTrue(restoreResult.success)

        // Verify that preferences updated, but PIN credentials remain 100% UNTOUCHED
        val currentSettings = settingsDao.getSettings().first()
        assertNotNull(currentSettings)
        assertTrue("requireAuth MUST remain true", currentSettings!!.requireAuth)
        assertEquals("PBKDF2\$100000\$securehash123", currentSettings.pinHash)
        assertEquals("securesalt456", currentSettings.pinSalt)
        assertEquals("dark", currentSettings.themeMode)
        assertEquals("Repouso", currentSettings.typeSleepName)
    }

    @Test
    fun `schedule import preserves existing habits and tracking history even in replace mode`() = runBlocking {
        val habitDao = db.habitDao()
        val existingId = habitDao.insertHabit(
            Habit(id = 101, name = "Hábito local", targetTime = "08:00")
        )
        habitDao.insertOrUpdateLog(
            HabitLog(id = 201, habitId = existingId, date = "2026-10-22", status = "DONE")
        )

        val scheduleJson = """
            [
              { "name": "Hábito importado", "targetTime": "09:30", "daysOfWeek": "1,2,3" }
            ]
        """.trimIndent()
        val validation = BackupManager.validateBackupJson(scheduleJson) as BackupValidationResult.Success
        assertTrue(validation.payload.isScheduleOnly)

        // Even if the caller asks to replace, schedule-only imports must be non-destructive.
        val result = BackupManager.restoreBackup(context, db, validation.payload, replaceExisting = true)
        assertTrue(result.success)

        val habits = habitDao.getAllHabits().first()
        assertEquals(2, habits.size)
        assertTrue(habits.any { it.id == existingId && it.name == "Hábito local" })
        assertTrue(habits.any { it.name == "Hábito importado" && it.id != existingId })
        val logs = habitDao.getAllLogs().first()
        assertEquals(1, logs.size)
        assertEquals(existingId, logs.single().habitId)
    }

    @Test
    fun `legacy habits-only backup preserves existing tracking history`() = runBlocking {
        val habitDao = db.habitDao()
        val existingId = habitDao.insertHabit(
            Habit(id = 301, name = "Hábito com histórico", targetTime = "07:00")
        )
        habitDao.insertOrUpdateLog(
            HabitLog(id = 401, habitId = existingId, date = "2026-10-21", status = "DONE")
        )

        val legacyJson = """
            {
              "app": "Rotina Viva",
              "timestamp": 1729600000000,
              "habits": [
                { "id": 1, "name": "Hábito do backup antigo", "targetTime": "10:00" }
              ]
            }
        """.trimIndent()
        val validation = BackupManager.validateBackupJson(legacyJson) as BackupValidationResult.Success
        assertTrue(validation.payload.isScheduleOnly)
        assertTrue(validation.preview.isScheduleOnly)

        val result = BackupManager.restoreBackup(context, db, validation.payload, replaceExisting = true)
        assertTrue(result.success)
        assertEquals(2, habitDao.getAllHabits().first().size)
        assertEquals(1, habitDao.getAllLogs().first().size)
        assertEquals(existingId, habitDao.getAllLogs().first().single().habitId)
    }


    @Test
    fun `merge remaps colliding IDs and preserves existing records`() = runBlocking {
        val habitDao = db.habitDao()
        val sleepDao = db.sleepDao()
        val localHabitId = habitDao.insertHabit(
            Habit(id = 1, name = "Hábito local", type = "FOCUS", targetTime = "08:00")
        )
        habitDao.insertOrUpdateLog(
            HabitLog(id = 1, habitId = localHabitId, date = "2026-10-20", status = "DONE", note = "Registro local")
        )
        sleepDao.insertSleepLog(
            SleepLog(id = 1, date = "2026-10-20", bedtimeStr = "22:00", wakeTimeStr = "06:00", note = "Sono local")
        )

        val backupJson = """
            {
              "schemaVersion": 1,
              "appName": "RotinaUp",
              "habits": [
                { "id": 1, "name": "Hábito importado", "type": "FOCUS", "targetTime": "09:00" }
              ],
              "habitLogs": [
                { "id": 1, "habitId": 1, "date": "2026-10-21", "status": "DONE", "note": "Registro importado" }
              ],
              "sleepLogs": [
                { "id": 1, "date": "2026-10-20", "bedtimeStr": "23:00", "wakeTimeStr": "07:00", "note": "Sono importado" }
              ]
            }
        """.trimIndent()
        val validation = BackupManager.validateBackupJson(backupJson) as BackupValidationResult.Success

        val result = BackupManager.restoreBackup(context, db, validation.payload, replaceExisting = false)
        assertTrue(result.success)

        val habits = habitDao.getAllHabits().first()
        assertEquals(2, habits.size)
        val importedHabit = habits.single { it.name == "Hábito importado" }
        assertNotEquals("Imported habit must receive a fresh local ID", localHabitId, importedHabit.id)

        val logs = habitDao.getAllLogs().first()
        assertEquals(2, logs.size)
        assertTrue(logs.any { it.habitId == localHabitId && it.note == "Registro local" })
        assertTrue(logs.any { it.habitId == importedHabit.id && it.note == "Registro importado" })

        val sleepLogs = sleepDao.getAllSleepLogs().first()
        assertEquals(1, sleepLogs.size)
        assertEquals("Sono local", sleepLogs.single().note)
    }


    @Test
    fun `merge does not collapse duplicate imported habits into one local habit`() = runBlocking {
        val habitDao = db.habitDao()
        val localId = habitDao.insertHabit(
            Habit(name = "Hábito repetido", type = "FOCUS", targetTime = "08:00")
        )
        val backupJson = """
            {
              "schemaVersion": 1,
              "appName": "RotinaUp",
              "habits": [
                { "id": 101, "name": "Hábito repetido", "type": "FOCUS", "targetTime": "08:00" },
                { "id": 102, "name": "Hábito repetido", "type": "FOCUS", "targetTime": "09:00" }
              ],
              "habitLogs": [
                { "id": 201, "habitId": 101, "date": "2026-10-20", "status": "DONE" },
                { "id": 202, "habitId": 102, "date": "2026-10-21", "status": "DONE" }
              ]
            }
        """.trimIndent()
        val validation = BackupManager.validateBackupJson(backupJson) as BackupValidationResult.Success

        val result = BackupManager.restoreBackup(context, db, validation.payload, replaceExisting = false)
        assertTrue(result.success)

        val habits = habitDao.getAllHabits().first()
        assertEquals(2, habits.size)
        val logs = habitDao.getAllLogs().first()
        assertEquals(2, logs.size)
        assertTrue(logs.any { it.date == "2026-10-20" && it.habitId == localId })
        val secondHabitId = logs.single { it.date == "2026-10-21" }.habitId
        assertNotEquals(localId, secondHabitId)
        assertTrue(habits.any { it.id == secondHabitId })
    }


    @Test
    fun `merge does not duplicate alarm events already present locally`() = runBlocking {
        val habitDao = db.habitDao()
        val settingsDao = db.settingsDao()
        val localHabitId = habitDao.insertHabit(
            Habit(name = "Hábito com alarme", type = "FOCUS", targetTime = "08:00")
        )
        val scheduledAt = 1_729_600_000_000L
        val dismissedAt = scheduledAt + 15_000L
        settingsDao.insertAlarmEvent(
            AlarmEvent(
                habitId = localHabitId,
                habitName = "Hábito com alarme",
                scheduledAt = scheduledAt,
                dismissedAt = dismissedAt,
                dismissAttempts = 2,
                snoozed = false,
                typedText = "confirmado"
            )
        )

        val backupJson = """
            {
              "schemaVersion": 1,
              "appName": "RotinaUp",
              "habits": [
                { "id": 77, "name": "Hábito com alarme", "type": "FOCUS", "targetTime": "08:00" }
              ],
              "alarmEvents": [
                {
                  "id": 900,
                  "habitId": 77,
                  "habitName": "Hábito com alarme",
                  "scheduledAt": $scheduledAt,
                  "dismissedAt": $dismissedAt,
                  "dismissAttempts": 2,
                  "snoozed": false,
                  "typedText": "confirmado"
                }
              ]
            }
        """.trimIndent()
        val validation = BackupManager.validateBackupJson(backupJson) as BackupValidationResult.Success

        val firstImport = BackupManager.restoreBackup(context, db, validation.payload, replaceExisting = false)
        assertTrue(firstImport.success)
        val secondImport = BackupManager.restoreBackup(context, db, validation.payload, replaceExisting = false)
        assertTrue(secondImport.success)

        val events = settingsDao.getAllAlarmEvents().first()
        assertEquals(1, events.size)
        assertEquals(localHabitId, events.single().habitId)
        assertEquals(scheduledAt, events.single().scheduledAt)
    }

}
