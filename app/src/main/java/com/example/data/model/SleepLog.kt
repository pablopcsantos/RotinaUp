package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_logs",
    indices = [Index(value = ["date"], unique = true)]
)
data class SleepLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val bedtimeStr: String = "23:00", // "HH:mm"
    val wakeTimeStr: String = "07:00", // "HH:mm"
    val durationMinutes: Int = 480,
    val quality: Int = 4, // 1 to 5
    val awakenings: Int = -1, // -1 means "Não percebi" (apnea safety default), 0, 1, 2, 3, 4+
    val awakeningsUnnoticed: Boolean = true,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
