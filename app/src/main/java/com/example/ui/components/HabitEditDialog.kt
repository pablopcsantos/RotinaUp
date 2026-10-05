package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Habit
import com.example.data.model.UserSettings

private fun getHabitTypeIcon(typeKey: String): ImageVector {
    return when (typeKey.uppercase()) {
        "SLEEP" -> Icons.Default.Nightlight
        "FOCUS" -> Icons.Default.Psychology
        "EXERCISE" -> Icons.Default.FitnessCenter
        "WATER" -> Icons.Default.WaterDrop
        else -> Icons.Default.Star
    }
}

private val DAYS_OF_WEEK = listOf(
    Pair(1, "Seg"),
    Pair(2, "Ter"),
    Pair(3, "Qua"),
    Pair(4, "Qui"),
    Pair(5, "Sex"),
    Pair(6, "Sáb"),
    Pair(7, "Dom")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditDialog(
    habitToEdit: Habit?,
    settings: UserSettings? = null,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit
) {
    val availableTypes = remember(settings) {
        settings?.getAllHabitTypes() ?: listOf(
            Pair("SLEEP", "Sono"),
            Pair("FOCUS", "Foco"),
            Pair("EXERCISE", "Exercício"),
            Pair("WATER", "Água"),
            Pair("CUSTOM", "Personalizado")
        )
    }
    var expandedTypeDropdown by remember { mutableStateOf(false) }

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
    var showInWidget by remember { mutableStateOf(habitToEdit?.showInWidget ?: true) }

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

                // Type selector with ExposedDropdownMenuBox (dropdown menu)
                val currentTypeLabel = remember(type, availableTypes) {
                    availableTypes.find { it.first.equals(type, ignoreCase = true) }?.second
                        ?: settings?.getTypeLabel(type)
                        ?: type
                }

                Text(
                    text = "Tipo de Hábito *",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedTypeDropdown,
                    onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentTypeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selecione o tipo do hábito") },
                        leadingIcon = {
                            Icon(
                                imageVector = getHabitTypeIcon(type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false }
                    ) {
                        availableTypes.forEach { (tKey, tLabel) ->
                            val isSelected = type.equals(tKey, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = getHabitTypeIcon(tKey),
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = tLabel,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selecionado",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    type = tKey
                                    updateDefaultPhrase(tKey)
                                    expandedTypeDropdown = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
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

                Spacer(modifier = Modifier.height(12.dp))

                // Mechanism to configure presentation in homescreen widget (Requirement 3)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Widgets,
                                contentDescription = null,
                                tint = if (showInWidget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Apresentar no Widget",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (showInWidget) "Hábito visível no widget da tela inicial" else "Hábito oculto do widget da tela inicial",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = showInWidget,
                            onCheckedChange = { showInWidget = it }
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
                            showInWidget = showInWidget,
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
