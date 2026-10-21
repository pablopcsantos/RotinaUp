package com.example.data.model

data class ConsistencyTier(
    val id: String, // bronze, silver, gold, platinum, diamond
    val levelNumber: Int, // 1 to 5
    val name: String,
    val badge: String,
    val minAdherence: Int,
    val maxAdherence: Int,
    val colorHex: Long,
    val description: String,
    val xpPoints: Int,
    val nextLevelName: String? = null,
    val pointsToNextLevel: Int? = null
)

data class WeeklyMedal(
    val id: String,
    val title: String,
    val category: String, // sono, consistencia, foco, disciplina
    val icon: String,
    val unlocked: Boolean,
    val description: String,
    val requirement: String,
    val currentProgress: Float = 0f,
    val targetProgress: Float = 0f,
    val progressText: String? = null
)

data class HabitWeeklyDayStatus(
    val date: String,
    val dayInitial: String,
    val status: String, // DONE, PARTIAL, SKIPPED, NONE
    val isToday: Boolean,
    val isEligible: Boolean = true
)

data class HabitWeeklyMatrix(
    val habit: Habit,
    val rate: Int?,
    val days: List<HabitWeeklyDayStatus>,
    val completedDaysCount: Int,
    val scheduledDaysCount: Int
)

data class ConsequenceEvent(
    val habitName: String,
    val message: String,
    val isPositive: Boolean
)

data class WeeklyReportData(
    val hasEnoughData: Boolean,
    val daysCount: Int,
    val overallAdherence: Int,
    val habitMatrices: List<HabitWeeklyMatrix>,
    val avgSleepHours: Double?,
    val bedtimeVariationHours: Double?,
    val napCount: Int,
    val topSkipReasons: List<Pair<String, Int>>,
    val suggestion: String,
    val tier: ConsistencyTier,
    val medals: List<WeeklyMedal>
)
