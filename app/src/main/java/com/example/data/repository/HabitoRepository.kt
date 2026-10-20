package com.example.data.repository

import com.example.data.db.HabitoDao
import com.example.data.model.HabitLog
import com.example.data.model.Habito
import kotlinx.coroutines.flow.Flow

class HabitoRepository(
    private val habitoDao: HabitoDao
) {
    val allHabits: Flow<List<Habito>> = habitoDao.getAllHabits()
    val activeHabits: Flow<List<Habito>> = habitoDao.getAllActiveHabits()
    val widgetHabits: Flow<List<Habito>> = habitoDao.getWidgetHabits()
    val allLogs: Flow<List<HabitLog>> = habitoDao.getAllLogs()

    fun getHabitById(id: Long): Flow<Habito?> = habitoDao.getHabitById(id)

    fun getLogsForDate(date: String): Flow<List<HabitLog>> = habitoDao.getLogsForDate(date)

    fun getLogsSince(startDate: String): Flow<List<HabitLog>> = habitoDao.getLogsSince(startDate)

    suspend fun insertHabit(habit: Habito): Long = habitoDao.insertHabit(habit)

    suspend fun insertAllHabits(habits: List<Habito>) = habitoDao.insertAllHabits(habits)

    suspend fun updateHabit(habit: Habito) = habitoDao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habito) = habitoDao.deleteHabitAndLogs(habit)

    suspend fun deleteAllHabits() = habitoDao.deleteAllHabits()

    suspend fun insertOrUpdateLog(log: HabitLog): Long = habitoDao.insertOrUpdateLog(log)

    suspend fun insertAllLogs(logs: List<HabitLog>) = habitoDao.insertAllLogs(logs)

    suspend fun deleteLog(habitId: Long, date: String) = habitoDao.deleteLog(habitId, date)

    suspend fun deleteAllLogs() = habitoDao.deleteAllLogs()
}
