package com.example.data.db

import androidx.room.*
import com.example.data.model.Habito
import com.example.data.model.HabitLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitoDao {
    @Query("SELECT * FROM habitos WHERE isActive = 1 ORDER BY targetTime ASC")
    fun getAllActiveHabits(): Flow<List<Habito>>

    @Query("SELECT * FROM habitos WHERE isActive = 1 AND showInWidget = 1 ORDER BY targetTime ASC")
    fun getWidgetHabits(): Flow<List<Habito>>

    @Query("SELECT * FROM habitos ORDER BY targetTime ASC")
    fun getAllHabits(): Flow<List<Habito>>

    @Query("SELECT * FROM habitos WHERE id = :id LIMIT 1")
    fun getHabitById(id: Long): Flow<Habito?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habito): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHabits(habits: List<Habito>)

    @Update
    suspend fun updateHabit(habit: Habito)

    @Delete
    suspend fun deleteHabit(habit: Habito)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: Long)

    @Transaction
    suspend fun deleteHabitAndLogs(habit: Habito) {
        deleteLogsForHabit(habit.id)
        deleteHabit(habit)
    }

    @Query("DELETE FROM habitos")
    suspend fun deleteAllHabits()

    // HabitLog queries
    @Query("SELECT * FROM habit_logs ORDER BY date DESC, timestamp DESC")
    fun getAllLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getLogsForDate(date: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE date >= :startDate ORDER BY date ASC")
    fun getLogsSince(startDate: String): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: HabitLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<HabitLog>)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLog(habitId: Long, date: String)

    @Query("DELETE FROM habit_logs")
    suspend fun deleteAllLogs()
}

typealias HabitDao = HabitoDao
