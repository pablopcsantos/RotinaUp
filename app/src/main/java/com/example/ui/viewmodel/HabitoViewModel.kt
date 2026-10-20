package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.HabitLog
import com.example.data.model.Habito
import com.example.data.repository.HabitoRepository
import com.example.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitoViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository: HabitoRepository = HabitoRepository(AppDatabase.getDatabase(application).habitDao())

    private val _currentDateStr = MutableStateFlow(DateUtils.getTodayDateString(0))
    val currentDateStr: StateFlow<String> = _currentDateStr.asStateFlow()

    val todayDateStr: String get() = _currentDateStr.value

    fun updateCurrentDate() {
        val today = DateUtils.getTodayDateString(0)
        if (_currentDateStr.value != today) {
            _currentDateStr.value = today
        }
    }

    val allHabits: StateFlow<List<Habito>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeHabits: StateFlow<List<Habito>> = repository.activeHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayLogs: StateFlow<List<HabitLog>> = _currentDateStr
        .flatMapLatest { date -> repository.getLogsForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<HabitLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveHabit(habit: Habito, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (habit.id == 0L) {
                repository.insertHabit(habit)
            } else {
                repository.updateHabit(habit.copy(updatedAt = System.currentTimeMillis()))
            }
            com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(getApplication())
            onSaved?.invoke()
        }
    }

    fun deleteHabit(habit: Habito, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(getApplication())
            onDeleted?.invoke()
        }
    }

    fun toggleHabitActive(habit: Habito) {
        viewModelScope.launch {
            val updated = habit.copy(isActive = !habit.isActive, updatedAt = System.currentTimeMillis())
            repository.updateHabit(updated)
            com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun checkIn(
        habitId: Long,
        date: String = todayDateStr,
        status: String, // DONE, PARTIAL, SKIPPED
        reason: String? = null,
        note: String? = null
    ) {
        viewModelScope.launch {
            repository.insertOrUpdateLog(
                HabitLog(
                    habitId = habitId,
                    date = date,
                    status = status,
                    reason = reason,
                    note = note
                )
            )
            com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(getApplication())
        }
    }
}
