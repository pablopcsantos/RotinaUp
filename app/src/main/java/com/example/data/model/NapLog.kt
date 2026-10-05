package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nap_logs",
    indices = [Index(value = ["date"], unique = true)]
)
data class NapLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val hasNap: Boolean = false,
    val napTime: String = "13:30",
    val napMinutes: Int = 0, // Records real duration without restrictions (e.g. 20, 45, 90)
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
