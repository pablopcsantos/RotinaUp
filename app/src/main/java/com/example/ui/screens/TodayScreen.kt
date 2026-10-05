package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.MandatoryProcessBanner
import com.example.ui.components.OverdueHabitsBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils

@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    onNavigateToSleep: () -> Unit,
    onNavigateToDailyReview: () -> Unit,
    onNavigateToAddHabit: () -> Unit,
    onStartFocusMode: (Habit) -> Unit,
    onOpenDetailedCheckIn: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.allHabits.collectAsState()
    val activeHabits by viewModel.activeHabits.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val todayLogs by viewModel.todayLogs.collectAsState()
    val sleepLogs by viewModel.allSleepLogs.collectAsState()
    val todaySleepLog by viewModel.todaySleepLog.collectAsState()
    val todayNapLog by viewModel.todayNapLog.collectAsState()
    val todayDailyReview by viewModel.todayDailyReview.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val currentTimeStr by viewModel.currentTimeStr.collectAsState()
    val snoozeUntil by viewModel.snoozeUntil.collectAsState()
    val mandatorySnoozeUntil by viewModel.mandatorySnoozeUntil.collectAsState()

    val isDark = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val todayDateStr = viewModel.todayDateStr
    val todayIsoDay = remember(todayDateStr) { DateUtils.getIsoDayOfWeek(todayDateStr) }

    val todayLogsMap = remember(todayLogs) { todayLogs.associateBy { it.habitId } }

    // Filter today's habits
    val todayHabits = remember(activeHabits, todayIsoDay) {
        activeHabits.filter { h ->
            val days = h.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            days.isEmpty() || days.contains(todayIsoDay)
        }.sortedBy { it.targetTime }
    }

    val doneCount = remember(todayHabits, todayLogsMap) {
        todayHabits.count { todayLogsMap[it.id]?.status == "DONE" }
    }

    // Overdue habits
    val isSnoozed = System.currentTimeMillis() < snoozeUntil
    val overdueHabits = remember(todayHabits, todayLogsMap, currentTimeStr, settings.overdueRemindersEnabled, isSnoozed) {
        if (!settings.overdueRemindersEnabled || isSnoozed) emptyList()
        else {
            todayHabits.filter { h ->
                val log = todayLogsMap[h.id]
                val isDoneOrSkipped = log?.status == "DONE" || log?.status == "SKIPPED"
                !isDoneOrSkipped && h.targetTime <= currentTimeStr
            }
        }
    }

    // Overdue mandatory steps (Night Sleep + Daily Review)
    val isMandatorySnoozed = System.currentTimeMillis() < mandatorySnoozeUntil
    val isMandatoryOverdue = remember(
        todaySleepLog,
        todayDailyReview,
        currentTimeStr,
        settings.mandatoryRemindersEnabled,
        settings.mandatoryDeadlineTime,
        isMandatorySnoozed
    ) {
        if (!settings.mandatoryRemindersEnabled || isMandatorySnoozed) false
        else {
            val pending = (todaySleepLog == null) || (todayDailyReview == null)
            pending && currentTimeStr >= settings.mandatoryDeadlineTime
        }
    }

    // Circadian late bedtime streak check
    val hasLateBedtimeStreak = remember(sleepLogs, settings.bedtimeTarget) {
        viewModel.checkLateBedtimeStreak(sleepLogs, settings.bedtimeTarget)
    }

    // 7 past days for adherence dots
    val last7Days = remember {
        (6 downTo 0).map { offset ->
            val dStr = DateUtils.getTodayDateString(-offset)
            Triple(dStr, DateUtils.getDayInitial(dStr), offset == 0)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddHabit,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Hábito", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
        ) {
            // Circadian Late Streak Alert
            if (hasLateBedtimeStreak) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusPartial.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StatusPartial.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StatusPartial.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bedtime, contentDescription = null, tint = StatusPartial)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Alerta Circadiano: 3 noites atrasado!",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = StatusPartial
                                )
                                Text(
                                    text = "Você foi dormir após o horário planejado por 3 dias seguidos. Sugerimos ajustar sua meta nas configurações.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Overdue Habits Banner
            if (overdueHabits.isNotEmpty()) {
                item {
                    OverdueHabitsBanner(
                        overdueHabits = overdueHabits,
                        intervalMinutes = settings.overdueIntervalMinutes,
                        soundEnabled = settings.overdueAlarmSound,
                        onOpenFocusMode = onStartFocusMode,
                        onOpenCheckIn = onOpenDetailedCheckIn,
                        onSnooze = { viewModel.snoozeOverdue(settings.overdueIntervalMinutes) },
                        onTriggerSound = {
                            viewModel.audioPlayer.playOverdueChime()
                            viewModel.showToast("Som de lembrete testado!")
                        }
                    )
                }
            }

            // Mandatory Process Banner (Disappears completely once both completed!)
            item {
                MandatoryProcessBanner(
                    nightSleepLogged = todaySleepLog != null,
                    dailyReviewLogged = todayDailyReview != null,
                    onNavigateToSleep = onNavigateToSleep,
                    onNavigateToDailyReview = onNavigateToDailyReview,
                    isOverdue = isMandatoryOverdue,
                    deadlineTime = settings.mandatoryDeadlineTime,
                    intervalMinutes = settings.mandatoryIntervalMinutes,
                    onSnooze = { viewModel.snoozeMandatoryOverdue(settings.mandatoryIntervalMinutes) }
                )
            }

            // Resumo de Hoje (MD3 Surface Container Card)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 1.dp else 0.dp),
                    border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Resumo de Hoje",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = onNavigateToDailyReview,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Diário", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                FilledTonalButton(
                                    onClick = onNavigateToSleep,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Sono", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Energia
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(StatusPartial.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = StatusPartial, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (todayDailyReview != null) "${todayDailyReview?.energy}/5" else "-",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Text("Energia", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Foco
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (todayDailyReview != null) "${todayDailyReview?.focus}/5" else "-",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Text("Foco", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Sono Noite
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Nightlight, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val durationText = todaySleepLog?.let {
                                    val h = it.durationMinutes / 60
                                    val m = it.durationMinutes % 60
                                    if (m > 0) "${h}h${m}m" else "${h}h"
                                } ?: "-"
                                Text(
                                    text = durationText,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Text("Sono Noite", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Cochilo
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(StatusDone.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = StatusDone, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val napText = when {
                                    todayNapLog == null -> "-"
                                    todayNapLog?.hasNap == true -> "${todayNapLog?.napMinutes}m"
                                    else -> "Não"
                                }
                                Text(
                                    text = napText,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Text("Cochilo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Timeline Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Linha do Tempo de Hábitos",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusDone)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$doneCount/${todayHabits.size} feitos hoje",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = StatusDone
                        )
                    }
                }
            }

            // Empty state if no habits
            if (todayHabits.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhum hábito programado para hoje",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toque no botão + para cadastrar seus hábitos da rotina.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Habits List with Alternating Depth Steps in Dark Mode
            itemsIndexed(todayHabits, key = { _, h -> h.id }) { index, habit ->
                val log = todayLogsMap[habit.id]
                val isDone = log?.status == "DONE"
                val isPartial = log?.status == "PARTIAL"
                val isSkipped = log?.status == "SKIPPED"

                // Alternating depth container color
                val cardBg = if (isDark) {
                    if (index % 2 == 1) DarkSurfaceCardStep1 else DarkSurface
                } else {
                    MaterialTheme.colorScheme.surface
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 2.dp else 0.dp),
                    border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Habit Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDetailedCheckIn(habit) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (habit.type.uppercase()) {
                                            "SLEEP" -> Icons.Default.Nightlight
                                            "EXERCISE" -> Icons.Default.FitnessCenter
                                            "WATER" -> Icons.Default.WaterDrop
                                            "FOCUS" -> Icons.Default.Psychology
                                            else -> Icons.Default.Star
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = habit.name,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Horário: ${habit.targetTime}${if (habit.durationMinutes != null) " • ${habit.durationMinutes}min" else ""}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Shortcut action: Focus Mode
                            IconButton(
                                onClick = { onStartFocusMode(habit) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = "Foco Total",
                                    tint = StatusDone,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 7-day Adherence Dots Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aderência recente:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                last7Days.forEach { (dStr, dayInitial, isToday) ->
                                    val pastLog = allLogs.find { it.habitId == habit.id && it.date == dStr }
                                    val dotDone = pastLog?.status == "DONE"
                                    val dotPartial = pastLog?.status == "PARTIAL"
                                    val dotSkipped = pastLog?.status == "SKIPPED"

                                    // Check if habit is scheduled for this day
                                    val isoDay = DateUtils.getIsoDayOfWeek(dStr)
                                    val isScheduled = habit.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(isoDay)

                                    val dotColor = when {
                                        dotDone -> StatusDone
                                        dotPartial -> StatusPartial
                                        dotSkipped -> StatusSkipped
                                        !isScheduled -> Color.Transparent
                                        !isToday -> StatusSkipped.copy(alpha = 0.35f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                                .border(
                                                    1.dp,
                                                    if (!isScheduled) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                    else if (dotDone) StatusDone
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (dotDone) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(9.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = dayInitial,
                                            fontSize = 9.sp,
                                            fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal,
                                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 Quick Action Buttons: Feito, Parcial, Pular
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // FEITO
                            Button(
                                onClick = { viewModel.checkInHabit(habit.id, "DONE") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDone) StatusDone else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isDone) Color.White else StatusDone
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Feito", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // PARCIAL
                            Button(
                                onClick = { viewModel.checkInHabit(habit.id, "PARTIAL") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPartial) StatusPartial else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isPartial) Color.White else StatusPartial
                                )
                            ) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Parcial", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // PULAR
                            Button(
                                onClick = { viewModel.checkInHabit(habit.id, "SKIPPED") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSkipped) StatusSkipped else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSkipped) Color.White else StatusSkipped
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pular", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
