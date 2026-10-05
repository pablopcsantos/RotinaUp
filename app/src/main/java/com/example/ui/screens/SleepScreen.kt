package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.NapLog
import com.example.data.model.SleepLog
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPartial
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("NIGHT") } // "NIGHT", "NAP", "HISTORY"

    val todaySleepLog by viewModel.todaySleepLog.collectAsState()
    val todayNapLog by viewModel.todayNapLog.collectAsState()
    val allSleepLogs by viewModel.allSleepLogs.collectAsState()
    val allNapLogs by viewModel.allNapLogs.collectAsState()

    // Form Night Sleep states
    var bedtimeStr by remember(todaySleepLog) { mutableStateOf(todaySleepLog?.bedtimeStr ?: "23:00") }
    var wakeTimeStr by remember(todaySleepLog) { mutableStateOf(todaySleepLog?.wakeTimeStr ?: "07:00") }
    var quality by remember(todaySleepLog) { mutableIntStateOf(todaySleepLog?.quality ?: 4) }
    // "Não percebi" marked by default (-1)
    var awakenings by remember(todaySleepLog) { mutableIntStateOf(todaySleepLog?.awakenings ?: -1) }
    var nightNote by remember(todaySleepLog) { mutableStateOf(todaySleepLog?.note ?: "") }

    // Form Nap states (Separated, records reality with any duration > 30 min)
    var hasNap by remember(todayNapLog) { mutableStateOf(todayNapLog?.hasNap ?: false) }
    var napTimeStr by remember(todayNapLog) { mutableStateOf(todayNapLog?.napTime ?: "13:30") }
    var napDurationText by remember(todayNapLog) { mutableStateOf(if (todayNapLog != null && todayNapLog!!.hasNap) todayNapLog!!.napMinutes.toString() else "20") }
    var napNote by remember(todayNapLog) { mutableStateOf(todayNapLog?.note ?: "") }

    // History filter
    var historyFilter by remember { mutableStateOf("ALL") } // "ALL", "NIGHT", "NAP"

    // Calculate duration display
    val calculatedDuration = remember(bedtimeStr, wakeTimeStr) {
        try {
            val (bH, bM) = bedtimeStr.split(":").map { it.toInt() }
            val (wH, wM) = wakeTimeStr.split(":").map { it.toInt() }
            var diff = (wH * 60 + wM) - (bH * 60 + bM)
            if (diff <= 0) diff += 24 * 60
            val h = diff / 60
            val m = diff % 60
            if (m > 0) "${h}h ${m}min" else "${h}h"
        } catch (_: Exception) {
            "8h"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Registro de Sono", fontWeight = FontWeight.Black, fontSize = 18.sp)
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
            // Tab Selector: Sono Noturno, Cochilos, Histórico
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
                    // TAB 1: Sono Noturno
                    Surface(
                        onClick = { activeTab = "NIGHT" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTab == "NIGHT") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeTab == "NIGHT") Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Nightlight, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sono Noturno", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // TAB 2: Cochilos
                    Surface(
                        onClick = { activeTab = "NAP" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTab == "NAP") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeTab == "NAP") Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cochilos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // TAB 3: Histórico
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
                            Text("Histórico", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                // ==================== TAB 1: SONO NOTURNO ====================
                if (activeTab == "NIGHT") {
                    item {
                        // Duration Highlight Card
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Nightlight, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "DURAÇÃO CALCULADA DO SONO",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = calculatedDuration,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 24.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Times: Bedtime and Wake time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = bedtimeStr,
                                onValueChange = { bedtimeStr = it },
                                label = { Text("Hora de Dormir (HH:mm)") },
                                leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = wakeTimeStr,
                                onValueChange = { wakeTimeStr = it },
                                label = { Text("Hora de Acordar (HH:mm)") },
                                leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        // Sleep Quality Rating (1 to 5 Stars)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Qualidade do Sono",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (star in 1..5) {
                                        IconButton(
                                            onClick = { quality = star },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = "$star estrelas",
                                                tint = if (star <= quality) Color(0xFFFBBF24) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (quality) {
                                            1 -> "Péssimo"
                                            2 -> "Ruim"
                                            3 -> "Regular"
                                            4 -> "Bom"
                                            else -> "Excelente"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Awakenings with "Não percebi" as DEFAULT (-1)
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
                                    Text(
                                        text = "Despertares Noturnos",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (awakenings == -1) "Não percebi (padrão)" else "$awakenings despertar(es)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Nem sempre o indivíduo com apneia do sono se lembrará de ter acordado, por isso mantemos 'Não percebi' como padrão.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Button "Não percebi"
                                    FilterChip(
                                        selected = awakenings == -1,
                                        onClick = { awakenings = -1 },
                                        label = { Text("Não percebi", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        )
                                    )

                                    // Numeric options 0, 1, 2, 3, 4+
                                    listOf(0, 1, 2, 3, 4).forEach { num ->
                                        FilterChip(
                                            selected = awakenings == num,
                                            onClick = { awakenings = num },
                                            label = { Text(if (num == 4) "4+" else num.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Night observations
                        OutlinedTextField(
                            value = nightNote,
                            onValueChange = { nightNote = it },
                            label = { Text("Observações sobre o sono noturno") },
                            placeholder = { Text("Ex: Acordei descansado, quarto escuro e arejado...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    item {
                        // REQUIREMENT: Button strictly named "Salvar registro"
                        Button(
                            onClick = {
                                viewModel.saveSleepLog(
                                    bedtimeStr = bedtimeStr,
                                    wakeTimeStr = wakeTimeStr,
                                    quality = quality,
                                    awakenings = awakenings,
                                    note = nightNote
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
                            Text("Salvar registro", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }

                // ==================== TAB 2: COCHILOS ====================
                if (activeTab == "NAP") {
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
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
                                                .background(StatusPartial.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = StatusPartial)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Tirou cochilo hoje?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Registro independente diurno", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Switch(
                                        checked = hasNap,
                                        onCheckedChange = { hasNap = it }
                                    )
                                }

                                if (hasNap) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = napTimeStr,
                                            onValueChange = { napTimeStr = it },
                                            label = { Text("Horário do Cochilo") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = napDurationText,
                                            onValueChange = { napDurationText = it },
                                            label = { Text("Duração Real (min)") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Quick Presets
                                    Text("Atalhos rápidos:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(15, 20, 30, 45, 60, 90).forEach { mins ->
                                            val isSelected = napDurationText == mins.toString()
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { napDurationText = mins.toString() },
                                                label = { Text("${mins}m", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                            )
                                        }
                                    }

                                    val currentMinutes = napDurationText.toIntOrNull() ?: 0
                                    // Educational guidance if > 30 minutes, WITHOUT BLOCKING!
                                    if (currentMinutes > 30) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = StatusPartial.copy(alpha = 0.12f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusPartial.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = StatusPartial,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Registro real mantido: $currentMinutes minutos",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = StatusPartial
                                                    )
                                                    Text(
                                                        text = "Cochilos acima de 30 minutos podem entrar em sono profundo e dificultar o adormecer noturno. Recomendamos observar seu sono nesta noite.",
                                                        fontSize = 11.sp,
                                                        lineHeight = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedTextField(
                                        value = napNote,
                                        onValueChange = { napNote = it },
                                        label = { Text("Observações do cochilo") },
                                        placeholder = { Text("Ex: Cochilei após o almoço...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 2,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = StatusDone.copy(alpha = 0.1f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusDone, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Excelente! Evitar cochilos durante o dia preserva a pressão de sono para adormecer rápido à noite.",
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                color = StatusDone,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // REQUIREMENT: Button strictly named "Salvar registro"
                        Button(
                            onClick = {
                                val mins = if (hasNap) (napDurationText.toIntOrNull() ?: 0) else 0
                                viewModel.saveNapLog(
                                    hasNap = hasNap,
                                    napTime = napTimeStr,
                                    napMinutes = mins,
                                    note = napNote
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
                            Text("Salvar registro", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }

                // ==================== TAB 3: HISTÓRICO ====================
                if (activeTab == "HISTORY") {
                    item {
                        // Filter chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = historyFilter == "ALL",
                                onClick = { historyFilter = "ALL" },
                                label = { Text("Todos", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = historyFilter == "NIGHT",
                                onClick = { historyFilter = "NIGHT" },
                                label = { Text("Apenas Sono Noturno", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = historyFilter == "NAP",
                                onClick = { historyFilter = "NAP" },
                                label = { Text("Apenas Cochilos", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (historyFilter == "ALL" || historyFilter == "NIGHT") {
                        item {
                            Text(
                                text = "Histórico do Sono da Noite",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (allSleepLogs.isEmpty()) {
                            item {
                                Text("Nenhum registro de sono noturno.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(allSleepLogs) { log ->
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
                                            Column {
                                                Text(DateUtils.formatFriendlyDate(log.date), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(
                                                    "${log.bedtimeStr} → ${log.wakeTimeStr} (${log.durationMinutes / 60}h${if (log.durationMinutes % 60 > 0) " ${log.durationMinutes % 60}m" else ""})",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row {
                                                repeat(log.quality) {
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Despertares: ${if (log.awakenings == -1 || log.awakeningsUnnoticed) "Não percebi" else "${log.awakenings} vezes"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (!log.note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "\"${log.note}\"",
                                                    fontSize = 11.sp,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (historyFilter == "ALL" || historyFilter == "NAP") {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Histórico de Cochilos Diurnos",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = StatusPartial
                            )
                        }

                        if (allNapLogs.isEmpty()) {
                            item {
                                Text("Nenhum registro de cochilo.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(allNapLogs) { nap ->
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
                                            Column {
                                                Text(DateUtils.formatFriendlyDate(nap.date), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(
                                                    if (nap.hasNap) "Cochilou às ${nap.napTime}" else "Não tirou cochilo",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (nap.hasNap) StatusPartial.copy(alpha = 0.2f) else StatusDone.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = if (nap.hasNap) "${nap.napMinutes} minutos" else "Sem cochilo",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (nap.hasNap) StatusPartial else StatusDone,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        if (!nap.note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "\"${nap.note}\"",
                                                    fontSize = 11.sp,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    modifier = Modifier.padding(8.dp)
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
}
