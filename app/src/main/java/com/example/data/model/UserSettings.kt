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
    val pinCode: String? = null,
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
    val typeCustomName: String = "Personalizado"
) {
    fun getTypeLabel(type: String): String {
        return when (type.uppercase()) {
            "SLEEP" -> typeSleepName
            "FOCUS" -> typeFocusName
            "EXERCISE" -> typeExerciseName
            "WATER" -> typeWaterName
            "CUSTOM" -> typeCustomName
            else -> typeCustomName
        }
    }
}
