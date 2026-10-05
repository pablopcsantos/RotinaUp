package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_events")
data class AlarmEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val habitName: String,
    val scheduledAt: Long = System.currentTimeMillis(),
    val dismissedAt: Long = System.currentTimeMillis(),
    val dismissAttempts: Int = 1,
    val snoozed: Boolean = false,
    val typedText: String = ""
)
