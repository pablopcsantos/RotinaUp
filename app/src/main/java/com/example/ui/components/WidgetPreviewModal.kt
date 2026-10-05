package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Habito
import com.example.data.model.HabitLog
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.StatusSkipped
import com.example.util.DateUtils
import com.example.widget.RotinaUpWidgetProvider

@Composable
fun WidgetPreviewModal(
    habits: List<Habito>,
    logs: List<HabitLog>,
    allLogs: List<HabitLog> = logs,
    onDismiss: () -> Unit,
    onQuickCheckIn: (Habito, String) -> Unit
) {
    val context = LocalContext.current
    val activeHabits = habits.filter { it.isActive && it.showInWidget }
    val hiddenCount = habits.count { it.isActive && !it.showInWidget }
    val todayLogsMap = logs.associateBy { it.habitId }
    val last7Days = DateUtils.getLast7Days()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Widget da Homescreen",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Aderência dos últimos 7 dias",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Solution Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lista de hábitos e bolinhas de aderência:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hiddenCount > 0) {
                                "Exibindo ${activeHabits.size} hábito(s) configurado(s) ($hiddenCount oculto(s) do widget)."
                            } else {
                                "O widget na tela inicial exibe os hábitos com histórico visual dos últimos 7 dias atualizado em tempo real."
                            },
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Widget Simulator Frame (Dark card for the launcher widget look)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFF0284C7), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Widget Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                        .border(1.dp, Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ROTINAUP WIDGET",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = DateUtils.getNowTimeStr(),
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days of week header above dots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HÁBITO",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                last7Days.forEach { (_, initial, isToday) ->
                                    Text(
                                        text = initial,
                                        color = if (isToday) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                        fontSize = 9.sp,
                                        fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
                                        modifier = Modifier.width(14.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Habit items with 7-day adherence dots
                        activeHabits.take(4).forEach { habit ->
                            val todayLog = todayLogsMap[habit.id]
                            val isTodayDone = todayLog?.status == "DONE"
                            val isTodayPartial = todayLog?.status == "PARTIAL"

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = habit.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = habit.targetTime,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.sp
                                        )
                                    }

                                    // 7-day adherence dots
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        last7Days.forEach { (dStr, _, isToday) ->
                                            val pastLog = allLogs.find { it.habitId == habit.id && it.date == dStr }
                                            val dotDone = pastLog?.status == "DONE"
                                            val dotPartial = pastLog?.status == "PARTIAL"
                                            val dotSkipped = pastLog?.status == "SKIPPED"

                                            val isoDay = DateUtils.getIsoDayOfWeek(dStr)
                                            val isScheduled = habit.daysOfWeek.split(",")
                                                .mapNotNull { it.trim().toIntOrNull() }
                                                .contains(isoDay)

                                            val dotColor = when {
                                                dotDone -> Color(0xFF22C55E)
                                                dotPartial -> Color(0xFFF59E0B)
                                                dotSkipped -> Color(0xFFEF4444)
                                                !isScheduled -> Color.Transparent
                                                !isToday -> Color(0xFFEF4444).copy(alpha = 0.6f)
                                                else -> Color(0xFF475569)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                                    .border(
                                                        1.dp,
                                                        if (dotDone) Color(0xFF22C55E)
                                                        else if (isScheduled) Color(0xFF64748B)
                                                        else Color(0xFF334155),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (dotDone) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🟢 Feito  🟠 Parcial  🔴 Não feito / Pular",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val pinned = RotinaUpWidgetProvider.requestPinWidget(context)
                            if (pinned) {
                                RotinaUpWidgetProvider.updateAllWidgets(context)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Adicionar à Homescreen", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Fechar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
