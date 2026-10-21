package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.DateUtils

@Entity(tableName = "habitos")
data class Habito(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String = "FOCUS", // SLEEP, FOCUS, EXERCISE, WATER, CUSTOM
    val targetTime: String, // "HH:mm"
    val durationMinutes: Int? = 30,
    val daysOfWeek: String = "1,2,3,4,5,6,7", // 1=Mon, 7=Sun
    val priority: Int = 1, // 1=Alta, 2=Média, 3=Baixa
    val isActive: Boolean = true,
    val showInWidget: Boolean = true,
    val positiveConsequence: String = "",
    val negativeConsequence: String = "",
    val dismissPhrase: String = "eu assumo o compromisso",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Formal, centralized parsing for daysOfWeek.
     * Empty string strictly means NO days scheduled (emptySet).
     */
    fun getScheduledDaysOfWeek(): Set<Int> {
        if (daysOfWeek.isBlank()) return emptySet()
        return daysOfWeek.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
    }

    /**
     * Checks if the habit is scheduled on a specific ISO day (1=Mon ... 7=Sun).
     */
    fun isScheduledOn(isoDay: Int): Boolean {
        return getScheduledDaysOfWeek().contains(isoDay)
    }

    /**
     * Checks if the habit is scheduled on a given ISO date (YYYY-MM-DD).
     */
    fun isScheduledOnDate(dateStr: String): Boolean {
        val isoDay = DateUtils.getIsoDayOfWeek(dateStr)
        return isScheduledOn(isoDay)
    }

    /**
     * Checks if the habit is eligible on a given ISO date:
     * Must not be prior to the habit's creation date AND must be scheduled on that date.
     */
    fun isEligibleOnDate(dateStr: String): Boolean {
        val createdDateStr = DateUtils.formatTimestampToIso(createdAt)
        return dateStr >= createdDateStr && isScheduledOnDate(dateStr)
    }
}

typealias Habit = Habito
