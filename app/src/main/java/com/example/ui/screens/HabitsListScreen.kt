package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.model.Habit
import com.example.ui.components.HabitEditDialog
import com.example.ui.theme.StatusDone
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsListScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.allHabits.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gerenciar Hábitos", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(
                            "${habits.size} hábitos cadastrados",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            habitToEdit = null
                            showEditDialog = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Novo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
        ) {
            items(habits, key = { it.id }) { habit ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (habit.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (habit.type) {
                                            "SLEEP" -> Icons.Default.Nightlight
                                            "EXERCISE" -> Icons.Default.FitnessCenter
                                            "WATER" -> Icons.Default.WaterDrop
                                            "CUSTOM" -> Icons.Default.Star
                                            else -> Icons.Default.Psychology
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = habit.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${habit.targetTime}${if (habit.durationMinutes != null) " • ${habit.durationMinutes}min" else ""}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Widgets,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = if (habit.showInWidget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (habit.showInWidget) "No widget" else "Oculto do widget",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (habit.showInWidget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = habit.isActive,
                                    onCheckedChange = { viewModel.toggleHabitActive(habit) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = {
                                    habitToEdit = habit
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { habitToDelete = habit }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Excluir",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (habit.dismissPhrase.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Frase do alarme: ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "\"${habit.dismissPhrase}\"",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusDone
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit or Add Dialog
    if (showEditDialog) {
        HabitEditDialog(
            habitToEdit = habitToEdit,
            settings = settings,
            onDismiss = {
                showEditDialog = false
                habitToEdit = null
            },
            onSave = { saved ->
                viewModel.saveHabit(saved)
                showEditDialog = false
                habitToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (habitToDelete != null) {
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Excluir Hábito?") },
            text = { Text("Deseja realmente remover o hábito \"${habitToDelete?.name}\"? O histórico de registros continuará salvo.") },
            confirmButton = {
                Button(
                    onClick = {
                        habitToDelete?.let { viewModel.deleteHabit(it) }
                        habitToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
