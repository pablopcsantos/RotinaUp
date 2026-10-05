package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import com.example.data.model.UserSettings

private val HABIT_TYPES = listOf(
    Pair("SLEEP", "Sono"),
    Pair("FOCUS", "Foco"),
    Pair("EXERCISE", "Exercício"),
    Pair("WATER", "Água"),
    Pair("CUSTOM", "Personalizado")
)

private val DAYS_OF_WEEK = listOf(
    Pair(1, "Seg"),
    Pair(2, "Ter"),
    Pair(3, "Qua"),
    Pair(4, "Qui"),
    Pair(5, "Sex"),
    Pair(6, "Sáb"),
    Pair(7, "Dom")
)

@Composable
fun HabitEditDialog(
    habitToEdit: Habit?,
    settings: UserSettings? = null,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit
) {
    var name by remember { mutableStateOf(habitToEdit?.name ?: "") }
    var type by remember { mutableStateOf(habitToEdit?.type ?: "FOCUS") }
    var targetTime by remember { mutableStateOf(habitToEdit?.targetTime ?: "08:00") }
    var durationText by remember { mutableStateOf(habitToEdit?.durationMinutes?.toString() ?: "30") }
    var selectedDays by remember {
        mutableStateOf(
            habitToEdit?.daysOfWeek?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
                ?: setOf(1, 2, 3, 4, 5)
        )
    }
    var priority by remember { mutableIntStateOf(habitToEdit?.priority ?: 1) }
    var positiveConsequence by remember { mutableStateOf(habitToEdit?.positiveConsequence ?: "") }
    var negativeConsequence by remember { mutableStateOf(habitToEdit?.negativeConsequence ?: "") }
    var dismissPhrase by remember { mutableStateOf(habitToEdit?.dismissPhrase ?: "vou estudar sem distracao") }

    var nameError by remember { mutableStateOf(false) }

    fun updateDefaultPhrase(newType: String) {
        if (habitToEdit == null || dismissPhrase.isBlank()) {
            dismissPhrase = when (newType) {
                "SLEEP" -> "eu escolho acordar agora"
                "FOCUS" -> "vou estudar sem distracao"
                "EXERCISE" -> "meu corpo agradece o movimento"
                "WATER" -> "beber agua faz bem"
                else -> "eu assumo o controle da minha rotina"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (habitToEdit == null) "Cadastrar Hábito" else "Editar Hábito",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Nome do Hábito *") },
                    placeholder = { Text("Ex: Leitura Matinal, Hidratação, Caminhada") },
                    isError = nameError,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError) {
                    Text("O nome é obrigatório.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type selector
                Text("Tipo de Hábito:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HABIT_TYPES.forEach { (tKey, tLabel) ->
                        val displayLabel = settings?.getTypeLabel(tKey) ?: tLabel
                        FilterChip(
                            selected = type == tKey,
                            onClick = {
                                type = tKey
                                updateDefaultPhrase(tKey)
                            },
                            label = { Text(displayLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time and Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetTime,
                        onValueChange = { targetTime = it },
                        label = { Text("Horário (HH:mm) *") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duração (min)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Days of Week
                Text("Dias da Semana:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DAYS_OF_WEEK.forEach { (dNum, dLabel) ->
                        val isSelected = selectedDays.contains(dNum)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays - dNum else selectedDays
                                } else {
                                    selectedDays + dNum
                                }
                            },
                            label = { Text(dLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Priority
                Text("Prioridade:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(Pair(1, "Alta"), Pair(2, "Média"), Pair(3, "Baixa")).forEach { (pNum, pLabel) ->
                        FilterChip(
                            selected = priority == pNum,
                            onClick = { priority = pNum },
                            label = { Text(pLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Consequences
                OutlinedTextField(
                    value = positiveConsequence,
                    onValueChange = { positiveConsequence = it },
                    label = { Text("Consequência Positiva (recompensa)") },
                    placeholder = { Text("Ex: Ganho clareza mental e começo o dia produtivo!") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = negativeConsequence,
                    onValueChange = { negativeConsequence = it },
                    label = { Text("Consequência Negativa (reflexão educativa)") },
                    placeholder = { Text("Ex: Pular agora atrasa suas metas e gera estresse.") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Anti-procrastination Phrase
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Frase para Desligar o Alarme:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = dismissPhrase,
                            onValueChange = { dismissPhrase = it },
                            placeholder = { Text("Ex: eu escolho acordar agora") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit button
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        val habit = Habit(
                            id = habitToEdit?.id ?: 0L,
                            name = name.trim(),
                            type = type,
                            targetTime = targetTime.trim().ifBlank { "08:00" },
                            durationMinutes = durationText.toIntOrNull() ?: 30,
                            daysOfWeek = selectedDays.sorted().joinToString(","),
                            priority = priority,
                            isActive = habitToEdit?.isActive ?: true,
                            positiveConsequence = positiveConsequence.trim(),
                            negativeConsequence = negativeConsequence.trim(),
                            dismissPhrase = dismissPhrase.trim().ifBlank { "eu assumo o compromisso" },
                            createdAt = habitToEdit?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(habit)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Hábito", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}
