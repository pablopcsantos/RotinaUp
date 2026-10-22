package com.example.util

import android.content.Context
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserSettingsExport(
    val notificationsEnabled: Boolean = true,
    val sleepGoalMinutes: Int = 480,
    val bedtimeTarget: String = "23:00",
    val wakeTarget: String = "07:00",
    val themeMode: String = "light",
    val overdueRemindersEnabled: Boolean = true,
    val overdueIntervalMinutes: Int = 15,
    val overdueAlarmSound: Boolean = true,
    val typeSleepName: String = "Sono",
    val typeFocusName: String = "Foco",
    val typeExerciseName: String = "Exercício",
    val typeWaterName: String = "Água",
    val typeCustomName: String = "Personalizado",
    val customHabitTypes: String = "Personalizado",
    val mandatoryRemindersEnabled: Boolean = true,
    val mandatoryDeadlineTime: String = "22:00",
    val mandatoryIntervalMinutes: Int = 15,
    val mandatoryAlarmSound: Boolean = true
)

data class BackupPayload(
    val schemaVersion: Int = 1,
    val appName: String = "RotinaUp",
    val exportedAt: Long = System.currentTimeMillis(),
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    val sleepLogs: List<SleepLog> = emptyList(),
    val napLogs: List<NapLog> = emptyList(),
    val dailyReviews: List<DailyReview> = emptyList(),
    val alarmEvents: List<AlarmEvent> = emptyList(),
    val settings: UserSettingsExport? = null
)

data class BackupPreview(
    val schemaVersion: Int,
    val appName: String,
    val exportedAt: Long,
    val formattedExportDate: String,
    val habitsCount: Int,
    val habitLogsCount: Int,
    val sleepLogsCount: Int,
    val napLogsCount: Int,
    val dailyReviewsCount: Int,
    val alarmEventsCount: Int,
    val hasSettings: Boolean,
    val isScheduleOnly: Boolean = false
)

sealed interface BackupValidationResult {
    data class Success(val preview: BackupPreview, val payload: BackupPayload) : BackupValidationResult
    data class Error(val message: String) : BackupValidationResult
}

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val habitsRestored: Int,
    val logsRestored: Int,
    val sleepLogsRestored: Int,
    val reviewsRestored: Int
)

object BackupManager {

    private val TIME_REGEX = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
    private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    /**
     * Exports a complete backup containing habits, all tracking logs, reviews, alarm events,
     * and safe application settings (excluding security credentials).
     */
    suspend fun exportCompleteBackup(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val habits = db.habitDao().getAllHabits().firstOrNull() ?: emptyList()
        val habitLogs = db.habitDao().getAllLogs().firstOrNull() ?: emptyList()
        val sleepLogs = db.sleepDao().getAllSleepLogs().firstOrNull() ?: emptyList()
        val napLogs = db.sleepDao().getAllNapLogs().firstOrNull() ?: emptyList()
        val dailyReviews = db.sleepDao().getAllDailyReviews().firstOrNull() ?: emptyList()
        val alarmEvents = db.settingsDao().getAllAlarmEvents().firstOrNull() ?: emptyList()
        val currentSettings = db.settingsDao().getSettings().firstOrNull() ?: UserSettings()

        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("appName", "RotinaUp")
        val now = System.currentTimeMillis()
        root.put("exportedAt", now)
        root.put("exportedDateStr", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(now)))

        // 1. Habits
        val habitsArr = JSONArray()
        habits.forEach { h ->
            val hObj = JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("type", h.type)
                put("targetTime", h.targetTime)
                put("durationMinutes", h.durationMinutes ?: 30)
                put("daysOfWeek", h.daysOfWeek)
                put("priority", h.priority)
                put("isActive", h.isActive)
                put("showInWidget", h.showInWidget)
                put("positiveConsequence", h.positiveConsequence)
                put("negativeConsequence", h.negativeConsequence)
                put("dismissPhrase", h.dismissPhrase)
                put("createdAt", h.createdAt)
                put("updatedAt", h.updatedAt)
            }
            habitsArr.put(hObj)
        }
        root.put("habits", habitsArr)

        // 2. Habit Logs
        val logsArr = JSONArray()
        habitLogs.forEach { log ->
            val lObj = JSONObject().apply {
                put("id", log.id)
                put("habitId", log.habitId)
                put("date", log.date)
                put("status", log.status)
                log.reason?.let { put("reason", it) }
                log.note?.let { put("note", it) }
                put("timestamp", log.timestamp)
            }
            logsArr.put(lObj)
        }
        root.put("habitLogs", logsArr)

        // 3. Sleep Logs
        val sleepArr = JSONArray()
        sleepLogs.forEach { s ->
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("date", s.date)
                put("bedtimeStr", s.bedtimeStr)
                put("wakeTimeStr", s.wakeTimeStr)
                put("durationMinutes", s.durationMinutes)
                put("quality", s.quality)
                put("awakenings", s.awakenings)
                put("awakeningsUnnoticed", s.awakeningsUnnoticed)
                s.note?.let { put("note", it) }
                put("createdAt", s.createdAt)
            }
            sleepArr.put(sObj)
        }
        root.put("sleepLogs", sleepArr)

        // 4. Nap Logs
        val napArr = JSONArray()
        napLogs.forEach { n ->
            val nObj = JSONObject().apply {
                put("id", n.id)
                put("date", n.date)
                put("hasNap", n.hasNap)
                put("napTime", n.napTime)
                put("napMinutes", n.napMinutes)
                n.note?.let { put("note", it) }
                put("createdAt", n.createdAt)
            }
            napArr.put(nObj)
        }
        root.put("napLogs", napArr)

        // 5. Daily Reviews
        val reviewsArr = JSONArray()
        dailyReviews.forEach { r ->
            val rObj = JSONObject().apply {
                put("id", r.id)
                put("date", r.date)
                put("mood", r.mood)
                put("energy", r.energy)
                put("focus", r.focus)
                r.note?.let { put("note", it) }
                put("createdAt", r.createdAt)
            }
            reviewsArr.put(rObj)
        }
        root.put("dailyReviews", reviewsArr)

        // 6. Alarm Events
        val alarmArr = JSONArray()
        alarmEvents.forEach { a ->
            val aObj = JSONObject().apply {
                put("id", a.id)
                put("habitId", a.habitId)
                put("habitName", a.habitName)
                put("scheduledAt", a.scheduledAt)
                put("dismissedAt", a.dismissedAt)
                put("dismissAttempts", a.dismissAttempts)
                put("snoozed", a.snoozed)
                put("typedText", a.typedText)
            }
            alarmArr.put(aObj)
        }
        root.put("alarmEvents", alarmArr)

        // 7. Safe Settings (no PIN/security credentials)
        val setObj = JSONObject().apply {
            put("notificationsEnabled", currentSettings.notificationsEnabled)
            put("sleepGoalMinutes", currentSettings.sleepGoalMinutes)
            put("bedtimeTarget", currentSettings.bedtimeTarget)
            put("wakeTarget", currentSettings.wakeTarget)
            put("themeMode", currentSettings.themeMode)
            put("overdueRemindersEnabled", currentSettings.overdueRemindersEnabled)
            put("overdueIntervalMinutes", currentSettings.overdueIntervalMinutes)
            put("overdueAlarmSound", currentSettings.overdueAlarmSound)
            put("typeSleepName", currentSettings.typeSleepName)
            put("typeFocusName", currentSettings.typeFocusName)
            put("typeExerciseName", currentSettings.typeExerciseName)
            put("typeWaterName", currentSettings.typeWaterName)
            put("typeCustomName", currentSettings.typeCustomName)
            put("customHabitTypes", currentSettings.customHabitTypes)
            put("mandatoryRemindersEnabled", currentSettings.mandatoryRemindersEnabled)
            put("mandatoryDeadlineTime", currentSettings.mandatoryDeadlineTime)
            put("mandatoryIntervalMinutes", currentSettings.mandatoryIntervalMinutes)
            put("mandatoryAlarmSound", currentSettings.mandatoryAlarmSound)
        }
        root.put("settings", setObj)

        root.toString(2)
    }

    /**
     * Exports a schedule-only JSON array (.rvschedule).
     */
    suspend fun exportSchedule(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val habits = db.habitDao().getAllHabits().firstOrNull() ?: emptyList()
        val jsonArr = JSONArray()
        habits.forEach { h ->
            val obj = JSONObject().apply {
                put("name", h.name)
                put("type", h.type)
                put("targetTime", h.targetTime)
                put("durationMinutes", h.durationMinutes ?: 30)
                put("daysOfWeek", h.daysOfWeek)
                put("priority", h.priority)
                put("positiveConsequence", h.positiveConsequence)
                put("negativeConsequence", h.negativeConsequence)
                put("dismissPhrase", h.dismissPhrase)
            }
            jsonArr.put(obj)
        }
        jsonArr.toString(2)
    }

    /**
     * Validates a backup JSON string (handles full .rvbackup, legacy habits format, and .rvschedule).
     * Returns either a Success with structured payload and preview or a descriptive Error.
     */
    fun validateBackupJson(jsonString: String): BackupValidationResult {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            return BackupValidationResult.Error("O conteúdo do backup está vazio.")
        }

        try {
            if (trimmed.startsWith("[")) {
                // Schedule or list of habits
                val arr = JSONArray(trimmed)
                if (arr.length() == 0) {
                    return BackupValidationResult.Error("A lista de hábitos do cronograma está vazia.")
                }
                val habits = mutableListOf<Habit>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)
                        ?: return BackupValidationResult.Error("O item ${i + 1} do cronograma não é um objeto válido.")
                    val habit = parseHabit(item, i)
                        ?: return BackupValidationResult.Error("Hábito no índice ${i + 1} possui nome vazio ou horário inválido.")
                    habits.add(habit)
                }

                val now = System.currentTimeMillis()
                val payload = BackupPayload(
                    schemaVersion = 1,
                    appName = "RotinaUp",
                    exportedAt = now,
                    habits = habits
                )
                val preview = BackupPreview(
                    schemaVersion = 1,
                    appName = "Cronograma RotinaUp",
                    exportedAt = now,
                    formattedExportDate = "Formato Cronograma (.rvschedule)",
                    habitsCount = habits.size,
                    habitLogsCount = 0,
                    sleepLogsCount = 0,
                    napLogsCount = 0,
                    dailyReviewsCount = 0,
                    alarmEventsCount = 0,
                    hasSettings = false,
                    isScheduleOnly = true
                )
                return BackupValidationResult.Success(preview, payload)
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)

                // Must contain recognizable keys
                val hasHabits = root.has("habits")
                val hasLogs = root.has("habitLogs")
                val hasSleep = root.has("sleepLogs")
                val hasNaps = root.has("napLogs")
                val hasReviews = root.has("dailyReviews")

                if (!hasHabits && !hasLogs && !hasSleep && !hasNaps && !hasReviews) {
                    return BackupValidationResult.Error(
                        "Estrutura de dados não reconhecida. O arquivo deve conter hábitos ou registros de rotina."
                    )
                }

                val schemaVersion = root.optInt("schemaVersion", 1)
                val appName = root.optString("appName", root.optString("app", "RotinaUp"))
                val exportedAt = root.optLong("exportedAt", root.optLong("timestamp", System.currentTimeMillis()))
                val formattedDate = root.optString("exportedDateStr").ifBlank {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(exportedAt))
                }

                // Parse Habits
                val parsedHabits = mutableListOf<Habit>()
                if (hasHabits) {
                    val hArr = root.optJSONArray("habits")
                        ?: return BackupValidationResult.Error("O campo 'habits' precisa ser uma lista válida.")
                    for (i in 0 until hArr.length()) {
                        val item = hArr.optJSONObject(i)
                            ?: return BackupValidationResult.Error("O hábito no índice ${i + 1} está malformado.")
                        val habit = parseHabit(item, i)
                            ?: return BackupValidationResult.Error("O hábito '${item.optString("name", "sem nome")}' possui horário inválido (${item.optString("targetTime")}) ou nome em branco.")
                        parsedHabits.add(habit)
                    }
                }

                // Parse Habit Logs
                val parsedLogs = mutableListOf<HabitLog>()
                if (hasLogs) {
                    val lArr = root.optJSONArray("habitLogs")
                        ?: return BackupValidationResult.Error("O campo 'habitLogs' precisa ser uma lista.")
                    for (i in 0 until lArr.length()) {
                        val item = lArr.optJSONObject(i)
                            ?: return BackupValidationResult.Error("Registro de hábito no índice ${i + 1} inválido.")
                        val habitId = item.optLong("habitId", -1L)
                        val date = item.optString("date")
                        if (habitId <= 0 || !DATE_REGEX.matches(date)) {
                            return BackupValidationResult.Error("Registro de hábito no índice ${i + 1} possui data ou habitId inválido.")
                        }
                        val status = item.optString("status", "DONE").uppercase()
                        val validStatus = if (status in listOf("DONE", "PARTIAL", "SKIPPED")) status else "DONE"
                        parsedLogs.add(
                            HabitLog(
                                id = item.optLong("id", 0L),
                                habitId = habitId,
                                date = date,
                                status = validStatus,
                                reason = if (item.isNull("reason")) null else item.optString("reason"),
                                note = if (item.isNull("note")) null else item.optString("note"),
                                timestamp = item.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Parse Sleep Logs
                val parsedSleep = mutableListOf<SleepLog>()
                if (hasSleep) {
                    val sArr = root.optJSONArray("sleepLogs")
                        ?: return BackupValidationResult.Error("O campo 'sleepLogs' precisa ser uma lista.")
                    for (i in 0 until sArr.length()) {
                        val item = sArr.optJSONObject(i)
                            ?: return BackupValidationResult.Error("Registro de sono no índice ${i + 1} inválido.")
                        val date = item.optString("date")
                        if (!DATE_REGEX.matches(date)) {
                            return BackupValidationResult.Error("Registro de sono no índice ${i + 1} possui data inválida: '$date'.")
                        }
                        parsedSleep.add(
                            SleepLog(
                                id = item.optLong("id", 0L),
                                date = date,
                                bedtimeStr = item.optString("bedtimeStr", "23:00"),
                                wakeTimeStr = item.optString("wakeTimeStr", "07:00"),
                                durationMinutes = item.optInt("durationMinutes", 480),
                                quality = item.optInt("quality", 4).coerceIn(1, 5),
                                awakenings = item.optInt("awakenings", -1),
                                awakeningsUnnoticed = item.optBoolean("awakeningsUnnoticed", true),
                                note = if (item.isNull("note")) null else item.optString("note"),
                                createdAt = item.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Parse Nap Logs
                val parsedNaps = mutableListOf<NapLog>()
                if (hasNaps) {
                    val nArr = root.optJSONArray("napLogs")
                        ?: return BackupValidationResult.Error("O campo 'napLogs' precisa ser uma lista.")
                    for (i in 0 until nArr.length()) {
                        val item = nArr.optJSONObject(i)
                            ?: return BackupValidationResult.Error("Registro de cochilo no índice ${i + 1} inválido.")
                        val date = item.optString("date")
                        if (!DATE_REGEX.matches(date)) {
                            return BackupValidationResult.Error("Registro de cochilo no índice ${i + 1} possui data inválida.")
                        }
                        parsedNaps.add(
                            NapLog(
                                id = item.optLong("id", 0L),
                                date = date,
                                hasNap = item.optBoolean("hasNap", false),
                                napTime = item.optString("napTime", "13:30"),
                                napMinutes = item.optInt("napMinutes", 0),
                                note = if (item.isNull("note")) null else item.optString("note"),
                                createdAt = item.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Parse Daily Reviews
                val parsedReviews = mutableListOf<DailyReview>()
                if (hasReviews) {
                    val rArr = root.optJSONArray("dailyReviews")
                        ?: return BackupValidationResult.Error("O campo 'dailyReviews' precisa ser uma lista.")
                    for (i in 0 until rArr.length()) {
                        val item = rArr.optJSONObject(i)
                            ?: return BackupValidationResult.Error("Diário noturno no índice ${i + 1} inválido.")
                        val date = item.optString("date")
                        if (!DATE_REGEX.matches(date)) {
                            return BackupValidationResult.Error("Diário noturno no índice ${i + 1} possui data inválida.")
                        }
                        parsedReviews.add(
                            DailyReview(
                                id = item.optLong("id", 0L),
                                date = date,
                                mood = item.optInt("mood", 3).coerceIn(1, 5),
                                energy = item.optInt("energy", 3).coerceIn(1, 5),
                                focus = item.optInt("focus", 3).coerceIn(1, 5),
                                note = if (item.isNull("note")) null else item.optString("note"),
                                createdAt = item.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Parse Alarm Events
                val parsedAlarms = mutableListOf<AlarmEvent>()
                if (root.has("alarmEvents")) {
                    val aArr = root.optJSONArray("alarmEvents")
                    if (aArr != null) {
                        for (i in 0 until aArr.length()) {
                            val item = aArr.optJSONObject(i) ?: continue
                            parsedAlarms.add(
                                AlarmEvent(
                                    id = item.optLong("id", 0L),
                                    habitId = item.optLong("habitId", 0L),
                                    habitName = item.optString("habitName", "Hábito"),
                                    scheduledAt = item.optLong("scheduledAt", System.currentTimeMillis()),
                                    dismissedAt = item.optLong("dismissedAt", System.currentTimeMillis()),
                                    dismissAttempts = item.optInt("dismissAttempts", 1),
                                    snoozed = item.optBoolean("snoozed", false),
                                    typedText = item.optString("typedText", "")
                                )
                            )
                        }
                    }
                }

                // Parse Settings
                val parsedSettings: UserSettingsExport? = if (root.has("settings")) {
                    val sObj = root.optJSONObject("settings")
                    if (sObj != null) {
                        UserSettingsExport(
                            notificationsEnabled = sObj.optBoolean("notificationsEnabled", true),
                            sleepGoalMinutes = sObj.optInt("sleepGoalMinutes", 480),
                            bedtimeTarget = sObj.optString("bedtimeTarget", "23:00"),
                            wakeTarget = sObj.optString("wakeTarget", "07:00"),
                            themeMode = sObj.optString("themeMode", "light"),
                            overdueRemindersEnabled = sObj.optBoolean("overdueRemindersEnabled", true),
                            overdueIntervalMinutes = sObj.optInt("overdueIntervalMinutes", 15),
                            overdueAlarmSound = sObj.optBoolean("overdueAlarmSound", true),
                            typeSleepName = sObj.optString("typeSleepName", "Sono"),
                            typeFocusName = sObj.optString("typeFocusName", "Foco"),
                            typeExerciseName = sObj.optString("typeExerciseName", "Exercício"),
                            typeWaterName = sObj.optString("typeWaterName", "Água"),
                            typeCustomName = sObj.optString("typeCustomName", "Personalizado"),
                            customHabitTypes = sObj.optString("customHabitTypes", "Personalizado"),
                            mandatoryRemindersEnabled = sObj.optBoolean("mandatoryRemindersEnabled", true),
                            mandatoryDeadlineTime = sObj.optString("mandatoryDeadlineTime", "22:00"),
                            mandatoryIntervalMinutes = sObj.optInt("mandatoryIntervalMinutes", 15),
                            mandatoryAlarmSound = sObj.optBoolean("mandatoryAlarmSound", true)
                        )
                    } else null
                } else null

                val payload = BackupPayload(
                    schemaVersion = schemaVersion,
                    appName = appName,
                    exportedAt = exportedAt,
                    habits = parsedHabits,
                    habitLogs = parsedLogs,
                    sleepLogs = parsedSleep,
                    napLogs = parsedNaps,
                    dailyReviews = parsedReviews,
                    alarmEvents = parsedAlarms,
                    settings = parsedSettings
                )

                val preview = BackupPreview(
                    schemaVersion = schemaVersion,
                    appName = appName,
                    exportedAt = exportedAt,
                    formattedExportDate = formattedDate,
                    habitsCount = parsedHabits.size,
                    habitLogsCount = parsedLogs.size,
                    sleepLogsCount = parsedSleep.size,
                    napLogsCount = parsedNaps.size,
                    dailyReviewsCount = parsedReviews.size,
                    alarmEventsCount = parsedAlarms.size,
                    hasSettings = parsedSettings != null,
                    isScheduleOnly = false
                )

                return BackupValidationResult.Success(preview, payload)
            } else {
                return BackupValidationResult.Error("Formato de backup inválido: o texto deve começar com '{' ou '['.")
            }
        } catch (e: Exception) {
            return BackupValidationResult.Error("Erro ao processar JSON: ${e.localizedMessage ?: "formato corrompido"}")
        }
    }

    private fun parseHabit(obj: JSONObject, index: Int): Habit? {
        val name = obj.optString("name").trim()
        if (name.isEmpty()) return null

        val targetTime = obj.optString("targetTime").trim()
        if (!TIME_REGEX.matches(targetTime)) return null

        val rawDays = obj.optString("daysOfWeek", "1,2,3,4,5,6,7").trim()
        val cleanedDays = if (rawDays.isBlank()) "" else {
            rawDays.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .joinToString(",")
        }

        val type = obj.optString("type", "FOCUS").trim().ifBlank { "FOCUS" }
        val duration = if (obj.has("durationMinutes")) obj.optInt("durationMinutes", 30) else 30
        val priority = obj.optInt("priority", 1).coerceIn(1, 3)
        val isActive = obj.optBoolean("isActive", true)
        val showInWidget = obj.optBoolean("showInWidget", true)
        val pos = obj.optString("positiveConsequence", "")
        val neg = obj.optString("negativeConsequence", "")
        val phrase = obj.optString("dismissPhrase", "eu assumo o compromisso").ifBlank { "eu assumo o compromisso" }
        val id = obj.optLong("id", (index + 1).toLong())
        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())

        return Habit(
            id = id,
            name = name,
            type = type,
            targetTime = targetTime,
            durationMinutes = duration,
            daysOfWeek = cleanedDays,
            priority = priority,
            isActive = isActive,
            showInWidget = showInWidget,
            positiveConsequence = pos,
            negativeConsequence = neg,
            dismissPhrase = phrase,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Executes the restoration of the validated payload into the Room Database.
     * Guaranteed to preserve existing PIN credentials and security lockout states.
     * Automatically reschedules alarms and updates home screen widgets upon completion.
     */
    suspend fun restoreBackup(
        context: Context,
        db: AppDatabase,
        payload: BackupPayload,
        replaceExisting: Boolean
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            db.withTransaction {
                if (replaceExisting) {
                    // 1. Wipe old records
                    db.habitDao().deleteAllHabits()
                    db.habitDao().deleteAllLogs()
                    db.sleepDao().deleteAllSleepLogs()
                    db.sleepDao().deleteAllNapLogs()
                    db.sleepDao().deleteAllDailyReviews()
                    db.settingsDao().deleteAllAlarmEvents()

                    // 2. Insert payload data
                    if (payload.habits.isNotEmpty()) {
                        db.habitDao().insertAllHabits(payload.habits)
                    }
                    if (payload.habitLogs.isNotEmpty()) {
                        db.habitDao().insertAllLogs(payload.habitLogs)
                    }
                    if (payload.sleepLogs.isNotEmpty()) {
                        db.sleepDao().insertAllSleepLogs(payload.sleepLogs)
                    }
                    if (payload.napLogs.isNotEmpty()) {
                        db.sleepDao().insertAllNapLogs(payload.napLogs)
                    }
                    if (payload.dailyReviews.isNotEmpty()) {
                        db.sleepDao().insertAllDailyReviews(payload.dailyReviews)
                    }
                    if (payload.alarmEvents.isNotEmpty()) {
                        db.settingsDao().insertAllAlarmEvents(payload.alarmEvents)
                    }
                } else {
                    // Merge mode: preserve existing, insert or update matching
                    if (payload.habits.isNotEmpty()) {
                        db.habitDao().insertAllHabits(payload.habits)
                    }
                    if (payload.habitLogs.isNotEmpty()) {
                        db.habitDao().insertAllLogs(payload.habitLogs)
                    }
                    if (payload.sleepLogs.isNotEmpty()) {
                        db.sleepDao().insertAllSleepLogs(payload.sleepLogs)
                    }
                    if (payload.napLogs.isNotEmpty()) {
                        db.sleepDao().insertAllNapLogs(payload.napLogs)
                    }
                    if (payload.dailyReviews.isNotEmpty()) {
                        db.sleepDao().insertAllDailyReviews(payload.dailyReviews)
                    }
                    if (payload.alarmEvents.isNotEmpty()) {
                        db.settingsDao().insertAllAlarmEvents(payload.alarmEvents)
                    }
                }

                // 3. Settings update (Strictly preserve existing PIN, salt, hash, and requireAuth to prevent bypass)
                val currentSettings = db.settingsDao().getSettings().firstOrNull() ?: UserSettings()
                val updatedSettings = if (payload.settings != null) {
                    currentSettings.copy(
                        notificationsEnabled = payload.settings.notificationsEnabled,
                        sleepGoalMinutes = payload.settings.sleepGoalMinutes,
                        bedtimeTarget = payload.settings.bedtimeTarget,
                        wakeTarget = payload.settings.wakeTarget,
                        themeMode = payload.settings.themeMode,
                        overdueRemindersEnabled = payload.settings.overdueRemindersEnabled,
                        overdueIntervalMinutes = payload.settings.overdueIntervalMinutes,
                        overdueAlarmSound = payload.settings.overdueAlarmSound,
                        typeSleepName = payload.settings.typeSleepName,
                        typeFocusName = payload.settings.typeFocusName,
                        typeExerciseName = payload.settings.typeExerciseName,
                        typeWaterName = payload.settings.typeWaterName,
                        typeCustomName = payload.settings.typeCustomName,
                        customHabitTypes = payload.settings.customHabitTypes,
                        mandatoryRemindersEnabled = payload.settings.mandatoryRemindersEnabled,
                        mandatoryDeadlineTime = payload.settings.mandatoryDeadlineTime,
                        mandatoryIntervalMinutes = payload.settings.mandatoryIntervalMinutes,
                        mandatoryAlarmSound = payload.settings.mandatoryAlarmSound,
                        lastBackupAt = System.currentTimeMillis(),
                        hasSeededInitialData = true
                        // IMPORTANT: pinCode, pinHash, pinSalt, requireAuth are deliberately KEPT untouched!
                    )
                } else {
                    currentSettings.copy(
                        lastBackupAt = System.currentTimeMillis(),
                        hasSeededInitialData = true
                    )
                }
                db.settingsDao().insertOrUpdateSettings(updatedSettings)
            }

            // Reschedule alarms and refresh widgets
            AlarmScheduler.rescheduleAllAlarmsSuspend(context)
            com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(context)

            RestoreResult(
                success = true,
                message = "Backup restaurado com sucesso!",
                habitsRestored = payload.habits.size,
                logsRestored = payload.habitLogs.size,
                sleepLogsRestored = payload.sleepLogs.size,
                reviewsRestored = payload.dailyReviews.size
            )
        } catch (t: Throwable) {
            RestoreResult(
                success = false,
                message = "Falha ao restaurar dados: ${t.localizedMessage ?: "erro desconhecido"}",
                habitsRestored = 0,
                logsRestored = 0,
                sleepLogsRestored = 0,
                reviewsRestored = 0
            )
        }
    }
}
