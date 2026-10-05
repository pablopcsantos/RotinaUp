package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_reviews",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyReview(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val mood: Int = 3, // 1 to 5
    val energy: Int = 3, // 1 to 5
    val focus: Int = 3, // 1 to 5
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
