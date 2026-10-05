package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPartial

@Composable
fun MandatoryProcessBanner(
    nightSleepLogged: Boolean,
    dailyReviewLogged: Boolean,
    onNavigateToSleep: () -> Unit,
    onNavigateToDailyReview: () -> Unit,
    isOverdue: Boolean = false,
    deadlineTime: String = "22:00",
    intervalMinutes: Int = 15,
    onSnooze: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Disappears COMPLETELY when both are logged today!
    if (nightSleepLogged && dailyReviewLogged) return

    val pendingCount = (if (nightSleepLogged) 0 else 1) + (if (dailyReviewLogged) 0 else 1)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else StatusPartial.copy(alpha = 0.35f),
                RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isOverdue) MaterialTheme.colorScheme.error else StatusPartial,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOverdue) "ETAPAS OBRIGATÓRIAS ATRASADAS" else "ETAPAS OBRIGATÓRIAS DE HOJE",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else StatusPartial
                    )
                }

                if (isOverdue && onSnooze != null) {
                    FilledTonalButton(
                        onClick = onSnooze,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Adiar ${intervalMinutes}m", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Badge(
                        containerColor = StatusPartial.copy(alpha = 0.2f),
                        contentColor = StatusPartial
                    ) {
                        Text(
                            text = "$pendingCount pendente${if (pendingCount > 1) "s" else ""}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isOverdue) {
                    "O horário limite configurado ($deadlineTime) foi ultrapassado. Preencha as etapas obrigatórias para calibrar seu ritmo circadiano."
                } else {
                    "O preenchimento diário do Sono Noturno e do Diário é essencial para calibrar seu ritmo circadiano."
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Night sleep button
                Button(
                    onClick = onNavigateToSleep,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (nightSleepLogged) StatusDone else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (nightSleepLogged) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        if (nightSleepLogged) Icons.Default.CheckCircle else Icons.Default.Nightlight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (nightSleepLogged) "Sono Registrado" else "Sono da Noite",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Daily review button
                Button(
                    onClick = onNavigateToDailyReview,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dailyReviewLogged) StatusDone else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (dailyReviewLogged) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        if (dailyReviewLogged) Icons.Default.CheckCircle else Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (dailyReviewLogged) "Diário Feito" else "Diário Noturno",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
