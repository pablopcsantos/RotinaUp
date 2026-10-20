package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.ui.theme.StatusPartial

@Composable
fun OverdueHabitsBanner(
    overdueHabits: List<Habit>,
    intervalMinutes: Int,
    soundEnabled: Boolean,
    onOpenFocusMode: (Habit) -> Unit,
    onOpenCheckIn: (Habit) -> Unit,
    onSnooze: () -> Unit,
    onTriggerSound: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (overdueHabits.isEmpty()) return

    val firstHabit = overdueHabits.first()
    val intervalText = if (intervalMinutes >= 60) {
        val h = intervalMinutes / 60
        val m = intervalMinutes % 60
        if (m > 0) "${h}h ${m}min" else "${h}h"
    } else {
        "$intervalMinutes minutos"
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = StatusPartial.copy(alpha = 0.12f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, StatusPartial.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = StatusPartial,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LEMBRETE: ${overdueHabits.size} HÁBITO(S) ATRASADO(S)!",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = StatusPartial
                    )
                }
                Badge(
                    containerColor = StatusPartial.copy(alpha = 0.2f),
                    contentColor = StatusPartial
                ) {
                    Text(
                        text = "${overdueHabits.size} pendente${if (overdueHabits.size > 1) "s" else ""}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "O horário programado já passou e os hábitos abaixo estão pendentes. Lembretes a cada $intervalText:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Habit chips
            overdueHabits.take(3).forEach { habit ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = habit.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Programado para às ${habit.targetTime}",
                                fontSize = 11.sp,
                                color = StatusPartial,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { onOpenFocusMode(habit) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = "Foco Total",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { onOpenCheckIn(habit) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Check-in",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSnooze,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Adiar lembrete ($intervalText)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { onOpenFocusMode(firstHabit) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Focar no primeiro",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = StatusPartial
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = StatusPartial
                    )
                }
            }
        }
    }
}
