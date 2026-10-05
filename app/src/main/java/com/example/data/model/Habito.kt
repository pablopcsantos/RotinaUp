package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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
)

typealias Habit = Habito
