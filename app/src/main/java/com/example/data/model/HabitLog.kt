package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "date"], unique = true)]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val date: String, // "YYYY-MM-DD"
    val status: String, // "DONE", "PARTIAL", "SKIPPED"
    val reason: String? = null,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
