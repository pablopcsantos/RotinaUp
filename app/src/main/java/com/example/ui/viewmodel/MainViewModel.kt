package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.util.AudioPlayer
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

sealed interface AuthState {
    data object Loading : AuthState
    data object Locked : AuthState
    data object Unlocked : AuthState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val habitDao = db.habitDao()
    private val sleepDao = db.sleepDao()
    private val settingsDao = db.settingsDao()

    val habitoRepository = com.example.data.repository.HabitoRepository(habitDao)

    val audioPlayer = AudioPlayer()

    private val _currentDateStr = MutableStateFlow(DateUtils.getTodayDateString(0))
    val currentDateStr: StateFlow<String> = _currentDateStr.asStateFlow()
    val todayDateStr: String get() = _currentDateStr.value

    // Current time string updated periodically or on demand
    private val _currentTimeStr = MutableStateFlow(DateUtils.getNowTimeStr())
    val currentTimeStr: StateFlow<String> = _currentTimeStr.asStateFlow()

    // Database Flows via HabitoRepository
    val allHabits: StateFlow<List<Habit>> = habitoRepository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeHabits: StateFlow<List<Habit>> = habitoRepository.activeHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<HabitLog>> = habitoRepository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayLogs: StateFlow<List<HabitLog>> = _currentDateStr
        .flatMapLatest { date -> habitoRepository.getLogsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSleepLogs: StateFlow<List<SleepLog>> = sleepDao.getAllSleepLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todaySleepLog: StateFlow<SleepLog?> = _currentDateStr
        .flatMapLatest { date -> sleepDao.getSleepLog(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allNapLogs: StateFlow<List<NapLog>> = sleepDao.getAllNapLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayNapLog: StateFlow<NapLog?> = _currentDateStr
        .flatMapLatest { date -> sleepDao.getNapLog(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allDailyReviews: StateFlow<List<DailyReview>> = sleepDao.getAllDailyReviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayDailyReview: StateFlow<DailyReview?> = _currentDateStr
        .flatMapLatest { date -> sleepDao.getDailyReview(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userSettings: StateFlow<UserSettings> = settingsDao.getSettings()
        .map { it ?: UserSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    val authState: StateFlow<AuthState> = combine(
        settingsDao.getSettings(),
        _isSessionUnlocked
    ) { settings, isUnlocked ->
        if (settings == null) {
            AuthState.Loading
        } else if (settings.requireAuth && settings.hasPin() && !isUnlocked) {
            AuthState.Locked
        } else {
            AuthState.Unlocked
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)

    fun unlockSession() {
        _isSessionUnlocked.value = true
    }

    // Snooze until timestamp for overdue reminder
    private val _snoozeUntil = MutableStateFlow(0L)
    val snoozeUntil: StateFlow<Long> = _snoozeUntil.asStateFlow()

    // Snooze until timestamp for mandatory steps reminder
    private val _mandatorySnoozeUntil = MutableStateFlow(0L)
    val mandatorySnoozeUntil: StateFlow<Long> = _mandatorySnoozeUntil.asStateFlow()

    // Toast feedback message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Consequence dialog state (positive or reflection)
    private val _consequence = MutableStateFlow<Pair<String, String>?>(null)
    val consequence: StateFlow<Pair<String, String>?> = _consequence.asStateFlow()

    init {
        viewModelScope.launch {
            val current = settingsDao.getSettings().firstOrNull()
            if (current == null || !current.hasSeededInitialData) {
                seedInitialData()
                val updated = (current ?: UserSettings()).copy(hasSeededInitialData = true)
                settingsDao.insertOrUpdateSettings(updated)
            }
        }
    }

    fun updateCurrentTime() {
        _currentTimeStr.value = DateUtils.getNowTimeStr()
        val today = DateUtils.getTodayDateString(0)
        if (_currentDateStr.value != today) {
            _currentDateStr.value = today
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearConsequence() {
        _consequence.value = null
    }

    fun snoozeOverdue(minutes: Int) {
        _snoozeUntil.value = System.currentTimeMillis() + minutes * 60 * 1000L
        showToast("Lembrete adiado por ${if (minutes >= 60) "${minutes / 60}h" else "$minutes min"}")
    }

    fun snoozeMandatoryOverdue(minutes: Int) {
        _mandatorySnoozeUntil.value = System.currentTimeMillis() + minutes * 60 * 1000L
        showToast("Lembrete de etapas adiado por ${if (minutes >= 60) "${minutes / 60}h" else "$minutes min"}")
    }

    fun dismissDummyPrompt() {
        viewModelScope.launch {
            val current = userSettings.value
            settingsDao.insertOrUpdateSettings(current.copy(hasSeenDummyPrompt = true))
        }
    }

    fun checkInHabit(
        habitId: Long,
        status: String, // DONE, PARTIAL, SKIPPED
        reason: String? = null,
        note: String? = null
    ) {
        viewModelScope.launch {
            val habit = allHabits.value.find { it.id == habitId }
            habitDao.insertOrUpdateLog(
                HabitLog(
                    habitId = habitId,
                    date = todayDateStr,
                    status = status,
                    reason = reason,
                    note = note
                )
            )
            if (habit != null) {
                if (status == "DONE" && habit.positiveConsequence.isNotBlank()) {
                    _consequence.value = Pair(habit.name, habit.positiveConsequence)
                } else if (status == "SKIPPED" && habit.negativeConsequence.isNotBlank()) {
                    _consequence.value = Pair(habit.name, habit.negativeConsequence)
                } else {
                    val msg = when (status) {
                        "DONE" -> "Hábito concluído com sucesso!"
                        "PARTIAL" -> "Hábito parcial registrado!"
                        else -> "Hábito pulado."
                    }
                    showToast(msg)
                }
            }
            com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            if (habit.id == 0L) {
                habitoRepository.insertHabit(habit)
                showToast("Hábito cadastrado!")
            } else {
                habitoRepository.updateHabit(habit.copy(updatedAt = System.currentTimeMillis()))
                showToast("Hábito atualizado!")
            }
            com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitoRepository.deleteHabit(habit)
            showToast("Hábito excluído.")
            com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleHabitActive(habit: Habit) {
        viewModelScope.launch {
            val updated = habit.copy(isActive = !habit.isActive, updatedAt = System.currentTimeMillis())
            habitoRepository.updateHabit(updated)
            showToast("Hábito ${if (updated.isActive) "ativado" else "desativado"}")
            com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun setAllHabitsShowInWidget(show: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val habits = habitoRepository.allHabits.firstOrNull() ?: emptyList()
            habits.forEach { habit ->
                if (habit.showInWidget != show) {
                    habitoRepository.updateHabit(habit.copy(showInWidget = show, updatedAt = System.currentTimeMillis()))
                }
            }
            com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(getApplication())
            showToast("Todos os hábitos foram ativados no widget!")
        }
    }

    fun saveSleepLog(
        bedtimeStr: String,
        wakeTimeStr: String,
        quality: Int,
        awakenings: Int,
        note: String?
    ) {
        viewModelScope.launch {
            var diff = 480
            try {
                val (bH, bM) = bedtimeStr.split(":").map { it.toInt() }
                val (wH, wM) = wakeTimeStr.split(":").map { it.toInt() }
                var m = (wH * 60 + wM) - (bH * 60 + bM)
                if (m <= 0) m += 24 * 60
                diff = m
            } catch (_: Exception) {}

            sleepDao.insertSleepLog(
                SleepLog(
                    date = todayDateStr,
                    bedtimeStr = bedtimeStr,
                    wakeTimeStr = wakeTimeStr,
                    durationMinutes = diff,
                    quality = quality,
                    awakenings = awakenings,
                    awakeningsUnnoticed = awakenings == -1,
                    note = note?.trim()?.ifBlank { null }
                )
            )
            showToast("Registro de sono noturno salvo com sucesso!")
        }
    }

    fun saveNapLog(
        hasNap: Boolean,
        napTime: String,
        napMinutes: Int,
        note: String?
    ) {
        viewModelScope.launch {
            sleepDao.insertNapLog(
                NapLog(
                    date = todayDateStr,
                    hasNap = hasNap,
                    napTime = napTime,
                    napMinutes = if (hasNap) napMinutes else 0,
                    note = note?.trim()?.ifBlank { null }
                )
            )
            showToast("Registro de cochilo salvo com sucesso!")
        }
    }

    fun saveDailyReview(
        mood: Int,
        energy: Int,
        focus: Int,
        note: String?
    ) {
        viewModelScope.launch {
            sleepDao.insertDailyReview(
                DailyReview(
                    date = todayDateStr,
                    mood = mood,
                    energy = energy,
                    focus = focus,
                    note = note?.trim()?.ifBlank { null }
                )
            )
            showToast("Diário de hoje salvo com sucesso!")
        }
    }

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            settingsDao.insertOrUpdateSettings(settings)
        }
    }

    fun setPin(pin: String) {
        val current = userSettings.value
        val updated = com.example.util.SecurityUtils.createHashedSettings(pin, current)
        updateSettings(updated)
    }

    fun removePin() {
        val current = userSettings.value
        updateSettings(current.copy(requireAuth = false, pinHash = null, pinSalt = null, pinCode = null))
    }

    fun verifyPin(enteredPin: String): Boolean {
        val current = userSettings.value
        val isValid = com.example.util.SecurityUtils.verifyPin(enteredPin, current)
        if (isValid) {
            com.example.util.SecurityUtils.resetFailedAttempts(getApplication())
            unlockSession()
            // Upgrade legacy plain-text pinCode or legacy SHA-256 to modern PBKDF2
            val isLegacy = !current.pinCode.isNullOrBlank() || (current.pinHash?.startsWith("PBKDF2$") != true)
            if (isLegacy) {
                viewModelScope.launch {
                    val upgraded = com.example.util.SecurityUtils.createHashedSettings(enteredPin, current)
                    settingsDao.insertOrUpdateSettings(upgraded)
                }
            }
        } else {
            com.example.util.SecurityUtils.recordFailedAttempt(getApplication())
        }
        return isValid
    }

    fun clearDummyContent() {
        viewModelScope.launch {
            habitDao.deleteAllLogs()
            sleepDao.deleteAllSleepLogs()
            sleepDao.deleteAllNapLogs()
            sleepDao.deleteAllDailyReviews()
            settingsDao.deleteAllAlarmEvents()
            val current = userSettings.value
            settingsDao.insertOrUpdateSettings(current.copy(isDummyActive = false, hasSeenDummyPrompt = true))
            showToast("Conteúdo de teste removido! Pronto para seus registros reais.")
        }
    }

    fun recordAlarmEvent(
        habitId: Long,
        habitName: String,
        attempts: Int,
        snoozed: Boolean,
        typedText: String
    ) {
        viewModelScope.launch {
            settingsDao.insertAlarmEvent(
                AlarmEvent(
                    habitId = habitId,
                    habitName = habitName,
                    scheduledAt = System.currentTimeMillis(),
                    dismissedAt = System.currentTimeMillis(),
                    dismissAttempts = attempts,
                    snoozed = snoozed,
                    typedText = typedText
                )
            )
        }
    }

    // Circadian streak check (3 late nights)
    fun checkLateBedtimeStreak(sleepLogs: List<SleepLog>, targetBedtime: String): Boolean {
        try {
            val (targetHour, targetMin) = targetBedtime.split(":").map { it.toInt() }
            var lateCount = 0
            for (i in 1..5) {
                val checkDate = DateUtils.getTodayDateString(-i)
                val log = sleepLogs.find { it.date == checkDate }
                if (log != null) {
                    val (bHour, bMin) = log.bedtimeStr.split(":").map { it.toInt() }
                    var isLate = false
                    if (targetHour >= 20) {
                        isLate = (bHour in 0..6) || (bHour > targetHour) || (bHour == targetHour && bMin > targetMin)
                    } else {
                        isLate = (bHour > targetHour) || (bHour == targetHour && bMin > targetMin)
                    }
                    if (isLate) {
                        lateCount++
                        if (lateCount >= 3) return true
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
        } catch (_: Exception) {}
        return false
    }

    // Weekly Report Generation
    fun computeWeeklyReport(
        habits: List<Habit>,
        logs: List<HabitLog>,
        sleepLogs: List<SleepLog>,
        napLogs: List<NapLog>,
        dailyReviews: List<DailyReview>
    ): WeeklyReportData {
        val dates = (6 downTo 0).map { DateUtils.getTodayDateString(-it) }
        val activeHabitsList = habits.filter { it.isActive }
        val weeklyLogs = logs.filter { dates.contains(it.date) }
        val daysWithData = dates.count { d ->
            weeklyLogs.any { it.date == d } || sleepLogs.any { it.date == d }
        }

        var totalScheduledChecks = 0
        var totalWeightedDone = 0f

        val matrices = activeHabitsList.map { habit ->
            val hLogs = weeklyLogs.filter { it.habitId == habit.id }
            val createdDateStr = DateUtils.formatTimestampToIso(habit.createdAt)

            // Strictly eligible scheduled dates: not before habit creation, and scheduled for that day
            val eligibleScheduledDates = dates.filter { dStr ->
                dStr >= createdDateStr && habit.isScheduledOnDate(dStr)
            }
            val scheduledDays = eligibleScheduledDates.size

            // Numerator strictly counts completions on eligible scheduled dates
            val eligibleLogs = hLogs.filter { eligibleScheduledDates.contains(it.date) }
            val done = eligibleLogs.count { it.status == "DONE" }
            val partial = eligibleLogs.count { it.status == "PARTIAL" }

            val weighted = done + (partial * 0.5f)
            val rate = if (scheduledDays > 0) {
                ((weighted / scheduledDays) * 100f).toInt().coerceIn(0, 100)
            } else {
                100 // No scheduled days in this week window
            }

            if (scheduledDays > 0) {
                totalScheduledChecks += scheduledDays
                totalWeightedDone += weighted
            }

            val dayStatuses = dates.map { dStr ->
                val log = hLogs.find { it.date == dStr }
                HabitWeeklyDayStatus(
                    date = dStr,
                    dayInitial = DateUtils.getDayInitial(dStr),
                    status = log?.status ?: "NONE",
                    isToday = dStr == dates.last()
                )
            }

            HabitWeeklyMatrix(
                habit = habit,
                rate = rate,
                days = dayStatuses,
                completedDaysCount = done
            )
        }

        val hasEnoughData = daysWithData >= 5 && activeHabitsList.isNotEmpty() && totalScheduledChecks >= 5

        val overallAdherence = if (totalScheduledChecks > 0) {
            ((totalWeightedDone / totalScheduledChecks) * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val weeklySleep = sleepLogs.filter { dates.contains(it.date) && it.durationMinutes > 0 }
        val avgSleep: Double? = if (weeklySleep.isNotEmpty()) {
            val totalHours = weeklySleep.sumOf { it.durationMinutes.toDouble() } / 60.0
            (Math.round((totalHours / weeklySleep.size) * 10.0) / 10.0)
        } else null

        // Real bedtime variation calculation (standard deviation in hours)
        val bedtimeVar: Double? = if (weeklySleep.size >= 2) {
            val minutesList = weeklySleep.map { log ->
                val parts = log.bedtimeStr.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 23
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                if (h >= 12) (h - 24) * 60 + m else h * 60 + m
            }
            val mean = minutesList.average()
            val variance = minutesList.map { (it - mean) * (it - mean) }.average()
            val stdDevMinutes = Math.sqrt(variance)
            (Math.round((stdDevMinutes / 60.0) * 10.0) / 10.0)
        } else null
        val weeklyNaps = napLogs.filter { dates.contains(it.date) && it.hasNap }
        val napCount = weeklyNaps.size

        val skipCounts = mutableMapOf<String, Int>()
        weeklyLogs.filter { it.status == "SKIPPED" && !it.reason.isNullOrBlank() }.forEach { l ->
            val r = l.reason!!
            skipCounts[r] = (skipCounts[r] ?: 0) + 1
        }
        val topSkips = skipCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { Pair(it.key, it.value) }

        val suggestion = when {
            !hasEnoughData -> "Continue registrando seus hábitos diariamente. São necessários pelo menos 5 dias de registros na semana para gerar insights precisos."
            overallAdherence < 60 -> "Considere reduzir a quantidade de hábitos ou começar com blocos menores para consolidar o ritmo antes de expandir."
            napCount >= 3 -> "Cochilos frequentes foram registrados. Procure fortalecer o sono noturno para evitar sonolência diurna excessiva."
            else -> "Excelente consistência na rotina! Mantenha os horários regulares de deitar e despertar para consolidar seus ciclos circadianos."
        }

        // Consistency Tier (1 to 5)
        val tier = when {
            overallAdherence >= 95 -> ConsistencyTier(
                id = "diamond",
                levelNumber = 5,
                name = "Nível 5: Diamante Supremo",
                badge = "💎",
                minAdherence = 95,
                maxAdherence = 100,
                colorHex = 0xFF22C55E,
                description = "Mestre da Consistência! Execução quase perfeita dos hábitos com disciplina inabalável.",
                xpPoints = 1500
            )
            overallAdherence >= 85 -> ConsistencyTier(
                id = "platinum",
                levelNumber = 4,
                name = "Nível 4: Platina Elite",
                badge = "🏆",
                minAdherence = 85,
                maxAdherence = 94,
                colorHex = 0xFF0284C7,
                description = "Desempenho de Alta Performance! Consistência firme em mais de 85% dos hábitos semanais.",
                xpPoints = 1100,
                nextLevelName = "Nível 5: Diamante Supremo",
                pointsToNextLevel = 95 - overallAdherence
            )
            overallAdherence >= 70 -> ConsistencyTier(
                id = "gold",
                levelNumber = 3,
                name = "Nível 3: Ouro Consistente",
                badge = "🥇",
                minAdherence = 70,
                maxAdherence = 84,
                colorHex = 0xFFEAB308,
                description = "Rotina Blindada! Mais de 70% dos hábitos cumpridos com perseverança e ritmo estabelecido.",
                xpPoints = 800,
                nextLevelName = "Nível 4: Platina Elite",
                pointsToNextLevel = 85 - overallAdherence
            )
            overallAdherence >= 50 -> ConsistencyTier(
                id = "silver",
                levelNumber = 2,
                name = "Nível 2: Prata em Construção",
                badge = "🥈",
                minAdherence = 50,
                maxAdherence = 69,
                colorHex = 0xFF94A3B8,
                description = "Hábito em Construção! Bom ritmo estabelecido na maioria dos dias da semana.",
                xpPoints = 500,
                nextLevelName = "Nível 3: Ouro Consistente",
                pointsToNextLevel = 70 - overallAdherence
            )
            else -> ConsistencyTier(
                id = "bronze",
                levelNumber = 1,
                name = "Nível 1: Bronze Iniciante",
                badge = "🥉",
                minAdherence = 0,
                maxAdherence = 49,
                colorHex = 0xFFD97706,
                description = "Iniciante Focado! Primeiros passos dados rumo a uma rotina de hábitos transformadora.",
                xpPoints = 250,
                nextLevelName = "Nível 2: Prata em Construção",
                pointsToNextLevel = 50 - overallAdherence
            )
        }

        // Medals
        var maxGreenStreak = 0
        matrices.forEach { m ->
            var current = 0
            m.days.forEach { d ->
                if (d.status == "DONE") {
                    current++
                    if (current > maxGreenStreak) maxGreenStreak = current
                } else {
                    current = 0
                }
            }
        }

        val wakeHabit = activeHabitsList.find { it.name.contains("despertar", ignoreCase = true) }
        val wakeDoneCount = if (wakeHabit != null) weeklyLogs.count { it.habitId == wakeHabit.id && it.status == "DONE" } else 0

        val noNapHabit = activeHabitsList.find { it.name.contains("cochilo", ignoreCase = true) }
        val noNapDoneCount = if (noNapHabit != null) weeklyLogs.count { it.habitId == noNapHabit.id && it.status == "DONE" } else 0

        val waterHabit = activeHabitsList.find { it.name.contains("hidrata", ignoreCase = true) }
        val waterDoneCount = if (waterHabit != null) weeklyLogs.count { it.habitId == waterHabit.id && it.status == "DONE" } else 0

        val focusHabits = activeHabitsList.filter { it.type == "FOCUS" }
        val focusSkips = weeklyLogs.count { l -> focusHabits.any { it.id == l.habitId } && l.status == "SKIPPED" }
        val reviewsInWeek = dailyReviews.count { dates.contains(it.date) }

        val medals = listOf(
            WeeklyMedal(
                id = "guardiao_sono",
                title = "Guardião do Sono",
                category = "sono",
                icon = "🌙",
                unlocked = avgSleep != null && avgSleep >= 7.0 && (bedtimeVar == null || bedtimeVar <= 1.0),
                description = "Média de sono noturno ≥ 7h com horário de deitar regular.",
                requirement = "≥ 7h de sono e desvio ≤ 1h",
                currentProgress = avgSleep?.toFloat() ?: 0f,
                targetProgress = 7.0f,
                progressText = if (avgSleep != null) "${avgSleep}h / 7h média" else "Sem registros de sono"
            ),
            WeeklyMedal(
                id = "semana_imparavel",
                title = "Semana de Ferro",
                category = "consistencia",
                icon = "⚡",
                unlocked = hasEnoughData && overallAdherence >= 80,
                description = "Atingiu 80% ou mais de adesão geral em todos os hábitos da semana.",
                requirement = "Adesão semanal ≥ 80%",
                currentProgress = overallAdherence.toFloat(),
                targetProgress = 80f,
                progressText = if (hasEnoughData) "$overallAdherence% / 80%" else "Dados insuficientes"
            ),
            WeeklyMedal(
                id = "transformacao_verde",
                title = "Transformação Verde",
                category = "consistencia",
                icon = "🟢",
                unlocked = maxGreenStreak >= 5,
                description = "Sequência consistente de bolinhas verdes consecutivas em um hábito.",
                requirement = "≥ 5 bolinhas verdes consecutivas",
                currentProgress = maxGreenStreak.toFloat(),
                targetProgress = 5f,
                progressText = "$maxGreenStreak de 5 dias seguidos"
            ),
            WeeklyMedal(
                id = "despertar_vitorioso",
                title = "Despertar Vitorioso",
                category = "disciplina",
                icon = "🌅",
                unlocked = wakeDoneCount >= 5,
                description = "Acordou no horário e cumpriu o despertar em pelo menos 5 dias da semana.",
                requirement = "Despertar concluído em ≥ 5 dias",
                currentProgress = wakeDoneCount.toFloat(),
                targetProgress = 5f,
                progressText = "$wakeDoneCount de 5 dias"
            ),
            WeeklyMedal(
                id = "foco_inabalavel",
                title = "Foco Inabalável",
                category = "foco",
                icon = "🎯",
                unlocked = focusSkips == 0 && focusHabits.isNotEmpty(),
                description = "Nenhum bloco de foco profundo foi pulado ao longo da semana.",
                requirement = "Zero faltas em blocos de foco",
                currentProgress = if (focusSkips == 0) 1f else 0f,
                targetProgress = 1f,
                progressText = if (focusSkips == 0) "Nenhum foco pulado" else "$focusSkips pulados"
            ),
            WeeklyMedal(
                id = "guerreiro_anti_cochilo",
                title = "Vigília Ativa",
                category = "disciplina",
                icon = "🛡️",
                unlocked = noNapDoneCount >= 5 || napCount <= 1,
                description = "Preservou a pressão homeostática de sono sem cochilos excessivos.",
                requirement = "Sem cochilo diurno em ≥ 5 dias",
                currentProgress = minOf(5f, maxOf(noNapDoneCount.toFloat(), (7 - napCount).toFloat())),
                targetProgress = 5f,
                progressText = "${minOf(5, maxOf(noNapDoneCount, 7 - napCount))} de 5 dias"
            ),
            WeeklyMedal(
                id = "diario_sagrado",
                title = "Mestre do Diário",
                category = "disciplina",
                icon = "📖",
                unlocked = reviewsInWeek >= 5,
                description = "Registrou sua energia, foco e reflexões em 5 ou mais dias da semana.",
                requirement = "Preencheu o diário em ≥ 5 dias",
                currentProgress = reviewsInWeek.toFloat(),
                targetProgress = 5f,
                progressText = "$reviewsInWeek de 5 dias"
            ),
            WeeklyMedal(
                id = "fonte_vitalidade",
                title = "Fonte de Vitalidade",
                category = "consistencia",
                icon = "💧",
                unlocked = waterDoneCount >= 5,
                description = "Hidratação matinal cumprida com rigor em pelo menos 5 dias.",
                requirement = "Hidratação em ≥ 5 dias",
                currentProgress = waterDoneCount.toFloat(),
                targetProgress = 5f,
                progressText = "$waterDoneCount de 5 dias"
            )
        )

        return WeeklyReportData(
            hasEnoughData = daysWithData >= 5,
            daysCount = daysWithData,
            overallAdherence = overallAdherence,
            habitMatrices = matrices,
            avgSleepHours = avgSleep,
            bedtimeVariationHours = bedtimeVar,
            napCount = napCount,
            topSkipReasons = topSkips,
            suggestion = suggestion,
            tier = tier,
            medals = medals
        )
    }

    // Seed 6 default habits and 30 days dummy content
    private suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        val initialHabits = listOf(
            Habit(
                id = 1,
                name = "Despertar Disposto",
                type = "SLEEP",
                targetTime = "07:00",
                durationMinutes = 15,
                daysOfWeek = "1,2,3,4,5,6,7",
                priority = 1,
                positiveConsequence = "Você começa o dia no comando da sua rotina e com a mente clara!",
                negativeConsequence = "Atrasar o despertar desregula seu ritmo circadiano e aumenta a pressa.",
                dismissPhrase = "eu escolho acordar agora"
            ),
            Habit(
                id = 2,
                name = "Hidratação Matinal",
                type = "WATER",
                targetTime = "07:15",
                durationMinutes = 5,
                daysOfWeek = "1,2,3,4,5,6,7",
                priority = 2,
                positiveConsequence = "Metabolismo ativado e energia revitalizada para a manhã inteira.",
                negativeConsequence = "Desidratação matinal causa fadiga precoce e dor de cabeça leve.",
                dismissPhrase = "beber agua faz bem"
            ),
            Habit(
                id = 3,
                name = "Não tirar cochilos durante o dia",
                type = "FOCUS",
                targetTime = "14:00",
                durationMinutes = 0,
                daysOfWeek = "1,2,3,4,5,6,7",
                priority = 1,
                positiveConsequence = "Preserva a homeostase do sono para você adormecer rápido e sem interrupção à noite!",
                negativeConsequence = "Cochilar tarde rouba o sono profundo noturno e fragmenta seus ciclos biológicos.",
                dismissPhrase = "foco em ficar acordado ate a noite"
            ),
            Habit(
                id = 4,
                name = "Bloco de Foco Profundo",
                type = "FOCUS",
                targetTime = "09:00",
                durationMinutes = 60,
                daysOfWeek = "1,2,3,4,5",
                priority = 1,
                positiveConsequence = "Tarefas mais difíceis liquidadas no pico de atenção, sem culpa!",
                negativeConsequence = "Procrastinar agora acumula estresse e reduz seu tempo livre à noite.",
                dismissPhrase = "vou estudar sem distracao"
            ),
            Habit(
                id = 5,
                name = "Movimento e Exercício",
                type = "EXERCISE",
                targetTime = "17:30",
                durationMinutes = 40,
                daysOfWeek = "1,2,3,4,5",
                priority = 2,
                positiveConsequence = "Endorfina liberada, sono mais profundo e corpo resistente.",
                negativeConsequence = "O sedentarismo aumenta o cansaço mental e prejudica a postura.",
                dismissPhrase = "meu corpo agradece o movimento"
            ),
            Habit(
                id = 6,
                name = "Preparação para o Sono",
                type = "SLEEP",
                targetTime = "22:30",
                durationMinutes = 30,
                daysOfWeek = "1,2,3,4,5,6,7",
                priority = 1,
                positiveConsequence = "Transição suave para o sono reparador e regeneração celular completa.",
                negativeConsequence = "Telas e agitação tardia atrasam a produção de melatonina.",
                dismissPhrase = "vou dormir para descansar"
            )
        )
        habitDao.insertAllHabits(initialHabits)

        // Seed 30 days dummy content (from day -30 up to yesterday day -1)
        val dummyLogs = mutableListOf<HabitLog>()
        val dummySleep = mutableListOf<SleepLog>()
        val dummyNaps = mutableListOf<NapLog>()
        val dummyReviews = mutableListOf<DailyReview>()

        val sleepNotes = listOf(
            "Dormi muito bem e acordei com a mente limpa.",
            "Quarto com temperatura ideal e escuro, sem interrupções.",
            "Leve demora para pegar no sono, mas descanso foi restaurador.",
            "Sono contínuo, acordei antes mesmo do despertador tocar.",
            "Dormi no horário certo da meta circadiana.",
            "Boa noite de descanso, meditação antes de deitar ajudou bastante."
        )

        val reviewNotes = listOf(
            "Dia de alto rendimento no trabalho e foco mantido.",
            "Muita energia pela manhã. Consegui finalizar tarefas prioritárias.",
            "Rotina cumprida com disciplina, me sinto no comando do meu tempo.",
            "Dia produtivo. Praticar exercícios no fim da tarde fez toda a diferença.",
            "Excelente equilíbrio entre concentração e pausas estratégicas.",
            "Venci a procrastinação pela manhã e o restante do dia fluiu leve."
        )

        for (i in 30 downTo 1) {
            val pastDate = DateUtils.getTodayDateString(-i)

            initialHabits.forEachIndexed { hIdx, habit ->
                var status = "DONE"
                var reason: String? = null
                if ((i * 3 + hIdx) % 11 == 0) {
                    status = "SKIPPED"
                    reason = listOf("cansaço", "ansiedade", "falta de tempo", "esqueci")[hIdx % 4]
                } else if ((i * 2 + hIdx) % 7 == 0) {
                    status = "PARTIAL"
                }
                dummyLogs.add(
                    HabitLog(
                        habitId = habit.id,
                        date = pastDate,
                        status = status,
                        reason = reason,
                        note = if (status == "DONE") "Executado com foco" else null
                    )
                )
            }

            val bedHour = if (i % 5 == 0) "23:30" else if (i % 3 == 0) "22:45" else "23:00"
            val wakeHour = if (i % 7 == 0) "07:30" else "07:00"
            dummySleep.add(
                SleepLog(
                    date = pastDate,
                    bedtimeStr = bedHour,
                    wakeTimeStr = wakeHour,
                    durationMinutes = 480,
                    quality = if (i % 4 == 0) 3 else if (i % 6 == 0) 5 else 4,
                    awakenings = -1,
                    awakeningsUnnoticed = true,
                    note = sleepNotes[i % sleepNotes.size]
                )
            )

            val hadNap = (i % 6 == 2)
            val napDur = if (hadNap) listOf(20, 35, 45, 60, 25, 90)[i % 6] else 0
            dummyNaps.add(
                NapLog(
                    date = pastDate,
                    hasNap = hadNap,
                    napTime = "13:30",
                    napMinutes = napDur,
                    note = if (hadNap) {
                        if (napDur > 30) "Cochilo de ${napDur} min após almoço pesado" else "Cochilo restaurador de ${napDur} min"
                    } else "Resisti ao cochilo e mantive a vigília ativa até a noite"
                )
            )

            dummyReviews.add(
                DailyReview(
                    date = pastDate,
                    mood = if (i % 5 == 0) 3 else if (i % 7 == 0) 5 else 4,
                    energy = if (i % 4 == 0) 3 else 4,
                    focus = if (i % 3 == 0) 4 else 5,
                    note = reviewNotes[i % reviewNotes.size]
                )
            )
        }

        habitDao.insertAllLogs(dummyLogs)
        sleepDao.insertAllSleepLogs(dummySleep)
        sleepDao.insertAllNapLogs(dummyNaps)
        sleepDao.insertAllDailyReviews(dummyReviews)
        settingsDao.insertOrUpdateSettings(UserSettings(isDummyActive = true, hasSeenDummyPrompt = false))
        com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(getApplication())
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
