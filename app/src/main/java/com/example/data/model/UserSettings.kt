package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,
    val notificationsEnabled: Boolean = true,
    val sleepGoalMinutes: Int = 480, // 8h
    val bedtimeTarget: String = "23:00",
    val wakeTarget: String = "07:00",
    val requireAuth: Boolean = false,
    val pinCode: String? = null, // Kept nullable for backwards-compatible migration; cleared upon PBKDF2 upgrade
    val pinHash: String? = null, // PBKDF2WithHmacSHA256 salted hash (format: PBKDF2$<iterations>$<hexKey>, 100,000 iterations); supports legacy SHA-256 for migration
    val pinSalt: String? = null, // Hex-encoded 16-byte cryptographically secure salt
    val themeMode: String = "light", // "light", "dark", "system"
    val lastBackupAt: Long? = null,
    val overdueRemindersEnabled: Boolean = true,
    val overdueIntervalMinutes: Int = 15,
    val overdueAlarmSound: Boolean = true,
    val isDummyActive: Boolean = true,
    val hasSeenDummyPrompt: Boolean = false,
    // Customizable Habit Types Names (Requirement 6)
    val typeSleepName: String = "Sono",
    val typeFocusName: String = "Foco",
    val typeExerciseName: String = "Exercício",
    val typeWaterName: String = "Água",
    val typeCustomName: String = "Personalizado",
    val customHabitTypes: String = "Personalizado",
    // Mandatory Steps Overdue Reminders
    val mandatoryRemindersEnabled: Boolean = true,
    val mandatoryDeadlineTime: String = "22:00",
    val mandatoryIntervalMinutes: Int = 15,
    val mandatoryAlarmSound: Boolean = true,
    // First run initialization flag (prevents re-seeding when user deletes all habits)
    val hasSeededInitialData: Boolean = false
) {
    fun hasPin(): Boolean {
        return !pinHash.isNullOrBlank() || !pinCode.isNullOrBlank()
    }

    fun getCustomTypesList(): List<String> {
        return customHabitTypes.split("||")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(typeCustomName.ifBlank { "Personalizado" }) }
    }

    fun getAllHabitTypes(): List<Pair<String, String>> {
        val standard = listOf(
            Pair("SLEEP", typeSleepName.ifBlank { "Sono" }),
            Pair("FOCUS", typeFocusName.ifBlank { "Foco" }),
            Pair("EXERCISE", typeExerciseName.ifBlank { "Exercício" }),
            Pair("WATER", typeWaterName.ifBlank { "Água" })
        )
        val customs = getCustomTypesList().map { customName ->
            val key = if (customName.equals("Personalizado", ignoreCase = true)) "CUSTOM" else customName
            Pair(key, customName)
        }
        return standard + customs
    }

    fun getTypeLabel(type: String): String {
        return when (type.uppercase()) {
            "SLEEP" -> typeSleepName.ifBlank { "Sono" }
            "FOCUS" -> typeFocusName.ifBlank { "Foco" }
            "EXERCISE" -> typeExerciseName.ifBlank { "Exercício" }
            "WATER" -> typeWaterName.ifBlank { "Água" }
            "CUSTOM" -> typeCustomName.ifBlank { "Personalizado" }
            else -> {
                getCustomTypesList().find { it.equals(type, ignoreCase = true) } ?: type
            }
        }
    }
}
