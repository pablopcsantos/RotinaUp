package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils

private val MOOD_LABELS = listOf(
    "",
    "1/5 - Muito para baixo",
    "2/5 - Desanimado",
    "3/5 - Neutro / Tranquilo",
    "4/5 - Bem disposto",
    "5/5 - Excelente / Radiante"
)

private val ENERGY_LABELS = listOf(
    "",
    "1/5 - Exausto",
    "2/5 - Baixa energia",
    "3/5 - Moderada",
    "4/5 - Produtivo",
    "5/5 - Pico de vitalidade"
)

private val FOCUS_LABELS = listOf(
    "",
    "1/5 - Muita distração",
    "2/5 - Dificuldade de foco",
    "3/5 - Foco razoável",
    "4/5 - Boa concentração",
    "5/5 - Hiperfoco pleno"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReviewScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("TODAY") } // "TODAY" or "HISTORY"

    val todayReview by viewModel.todayDailyReview.collectAsState()
    val allReviews by viewModel.allDailyReviews.collectAsState()

    var mood by remember(todayReview) { mutableIntStateOf(todayReview?.mood ?: 3) }
    var energy by remember(todayReview) { mutableIntStateOf(todayReview?.energy ?: 3) }
    var focus by remember(todayReview) { mutableIntStateOf(todayReview?.focus ?: 3) }
    var note by remember(todayReview) { mutableStateOf(todayReview?.note ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Diário de Hoje", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(
                            DateUtils.formatFriendlyDate(viewModel.todayDateStr),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Tab Selector: Avaliação de Hoje vs Histórico
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { activeTab = "TODAY" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTab == "TODAY") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeTab == "TODAY") Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Avaliação de Hoje", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        onClick = { activeTab = "HISTORY" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTab == "HISTORY") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeTab == "HISTORY") Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Histórico do Diário", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeTab == "TODAY") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    item {
                        Text(
                            text = "Como você se avalia no dia de hoje? O preenchimento diário ajuda a mapear sua vitalidade e foco ao longo do tempo.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    // Humor Slider
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Humor", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        MOOD_LABELS.getOrElse(mood) { "" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = mood.toFloat(),
                                    onValueChange = { mood = it.toInt() },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Energia Slider
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Energia & Vitalidade", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        ENERGY_LABELS.getOrElse(energy) { "" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = energy.toFloat(),
                                    onValueChange = { energy = it.toInt() },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Foco Slider
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Foco & Atenção", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        FOCUS_LABELS.getOrElse(focus) { "" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = focus.toFloat(),
                                    onValueChange = { focus = it.toInt() },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Reflexão Livre
                    item {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("O que marcou seu dia hoje?") },
                            placeholder = { Text("Reflexões, conquistas, obstáculos ou o que pode ser ajustado amanhã...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    // Botão Salvar
                    item {
                        Button(
                            onClick = {
                                viewModel.saveDailyReview(
                                    mood = mood,
                                    energy = energy,
                                    focus = focus,
                                    note = note
                                )
                                onNavigateBack()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salvar Diário", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                // HISTORY TAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    if (allReviews.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhum diário registrado anteriormente.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(allReviews) { review ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = DateUtils.formatFriendlyDate(review.date),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Humor ${review.mood}/5",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Text("Humor: ${review.mood}/5", fontSize = 11.sp)
                                        Text("Energia: ${review.energy}/5", fontSize = 11.sp)
                                        Text("Foco: ${review.focus}/5", fontSize = 11.sp)
                                    }

                                    if (!review.note.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "\"${review.note}\"",
                                                fontSize = 11.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
