package com.example.data.db

import androidx.room.*
import com.example.data.model.AlarmEvent
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<UserSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: UserSettings): Long

    // Alarm Events
    @Query("SELECT * FROM alarm_events ORDER BY scheduledAt DESC LIMIT 50")
    fun getAlarmEvents(): Flow<List<AlarmEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarmEvent(event: AlarmEvent): Long

    @Query("DELETE FROM alarm_events")
    suspend fun deleteAllAlarmEvents()
}
