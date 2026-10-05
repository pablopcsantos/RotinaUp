package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Habit
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.StatusSkipped

private val SKIP_REASONS = listOf(
    "cansaço",
    "ansiedade",
    "tédio",
    "esqueci",
    "falta de tempo",
    "outro"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onConfirm: (status: String, reason: String?, note: String?) -> Unit,
    onStartFocusMode: () -> Unit
) {
    var selectedStatus by remember { mutableStateOf("DONE") }
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CHECK-IN DO HÁBITO",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = habit.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Horário: ${habit.targetTime}${if (habit.durationMinutes != null) " • ${habit.durationMinutes}min" else ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Shortcut to Focus Mode
                Surface(
                    onClick = onStartFocusMode,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Entrar em Modo Foco Total com Cronômetro",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Como foi a execução hoje?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // 3 Status Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // FEITO
                    Surface(
                        onClick = { selectedStatus = "DONE" },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedStatus == "DONE") StatusDone else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (selectedStatus == "DONE") Color.White else StatusDone,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Feito",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedStatus == "DONE") Color.White else StatusDone
                            )
                        }
                    }

                    // PARCIAL
                    Surface(
                        onClick = { selectedStatus = "PARTIAL" },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedStatus == "PARTIAL") StatusPartial else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = if (selectedStatus == "PARTIAL") Color.White else StatusPartial,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Parcial",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedStatus == "PARTIAL") Color.White else StatusPartial
                            )
                        }
                    }

                    // PULAR
                    Surface(
                        onClick = { selectedStatus = "SKIPPED" },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedStatus == "SKIPPED") StatusSkipped else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = if (selectedStatus == "SKIPPED") Color.White else StatusSkipped,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pular",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedStatus == "SKIPPED") Color.White else StatusSkipped
                            )
                        }
                    }
                }

                // If SKIPPED, show reason chips
                if (selectedStatus == "SKIPPED") {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Qual foi o motivo principal?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SKIP_REASONS.forEach { r ->
                                    val isSelected = selectedReason == r
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedReason = if (isSelected) null else r },
                                        label = { Text(r, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StatusSkipped,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Free-form note
                Text(
                    text = "Nota ou observação (opcional):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Ex: Executei com facilidade, senti boa clareza...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Save button
                Button(
                    onClick = {
                        onConfirm(
                            selectedStatus,
                            if (selectedStatus == "SKIPPED") selectedReason else null,
                            note.trim().ifBlank { null }
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Check-in", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
