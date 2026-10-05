package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPartial
import com.example.ui.viewmodel.MainViewModel
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onOpenWidgetPreview: () -> Unit,
    onTriggerTestAlarm: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsState()
    val habits by viewModel.allHabits.collectAsState()

    var showClearDummyDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var tempPin by remember { mutableStateOf("") }

    var sleepGoalHours by remember(settings) { mutableIntStateOf(settings.sleepGoalMinutes / 60) }
    var bedtimeTarget by remember(settings) { mutableStateOf(settings.bedtimeTarget) }
    var wakeTarget by remember(settings) { mutableStateOf(settings.wakeTarget) }

    var typeSleep by remember(settings) { mutableStateOf(settings.typeSleepName) }
    var typeFocus by remember(settings) { mutableStateOf(settings.typeFocusName) }
    var typeExercise by remember(settings) { mutableStateOf(settings.typeExerciseName) }
    var typeWater by remember(settings) { mutableStateOf(settings.typeWaterName) }
    var typeCustom by remember(settings) { mutableStateOf(settings.typeCustomName) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        viewModel.showToast("$label copiado para a área de transferência!")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Configurações", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Personalize tema, metas e segurança", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
        ) {
            // ==================== 1. TEMA CLARO E ESCURO ====================
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Aparência do Aplicativo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.updateSettings(settings.copy(themeMode = "light")) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (settings.themeMode == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (settings.themeMode == "light") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Claro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.updateSettings(settings.copy(themeMode = "dark")) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (settings.themeMode == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (settings.themeMode == "dark") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Escuro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==================== 2. GERENCIAR HÁBITOS (ATALHO) ====================
            item {
                Surface(
                    onClick = onNavigateToHabits,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Gerenciar Todos os Hábitos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${habits.size} hábitos • Ativar, frases de alarme e exclusão", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            // ==================== 3. WIDGET NA TELA INICIAL ====================
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Widget na Tela Inicial", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Monitore bolinhas de aderência e faça check-in", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Gere e fixe o widget na tela inicial do dispositivo para acompanhar hábitos sem precisar abrir o app.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val pinned = com.example.widget.RotinaUpWidgetProvider.requestPinWidget(context)
                                    if (pinned) {
                                        viewModel.showToast("Solicitação enviada para a tela inicial!")
                                    } else {
                                        viewModel.showToast("Abra o menu de widgets do dispositivo e adicione RotinaUp.")
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AddHome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gerar na Homescreen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onOpenWidgetPreview,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Visualizador", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==================== EDITAR NOMES DOS TIPOS DE HÁBITOS (Requisito 6) ====================
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Nomes dos Tipos de Hábitos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Personalize como cada categoria é chamada", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = typeSleep,
                            onValueChange = { typeSleep = it },
                            label = { Text("Tipo: Sono") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = typeFocus,
                            onValueChange = { typeFocus = it },
                            label = { Text("Tipo: Foco") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = typeExercise,
                            onValueChange = { typeExercise = it },
                            label = { Text("Tipo: Exercício") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = typeWater,
                            onValueChange = { typeWater = it },
                            label = { Text("Tipo: Água") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = typeCustom,
                            onValueChange = { typeCustom = it },
                            label = { Text("Tipo: Personalizado") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.updateSettings(
                                    settings.copy(
                                        typeSleepName = typeSleep.trim().ifBlank { "Sono" },
                                        typeFocusName = typeFocus.trim().ifBlank { "Foco" },
                                        typeExerciseName = typeExercise.trim().ifBlank { "Exercício" },
                                        typeWaterName = typeWater.trim().ifBlank { "Água" },
                                        typeCustomName = typeCustom.trim().ifBlank { "Personalizado" }
                                    )
                                )
                                viewModel.showToast("Nomes dos tipos atualizados com sucesso!")
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar Nomes dos Tipos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ==================== 4. LEMBRETES DE HÁBITOS ATRASADOS ====================
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = StatusPartial)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Lembretes de Hábitos Atrasados", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Avisos sonoros para hábitos pendentes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = settings.overdueRemindersEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(overdueRemindersEnabled = it)) }
                            )
                        }

                        if (settings.overdueRemindersEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Intervalo entre lembretes:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Interval chips
                            val intervals = listOf(
                                Pair(5, "5m"),
                                Pair(10, "10m"),
                                Pair(15, "15m"),
                                Pair(30, "30m"),
                                Pair(45, "45m"),
                                Pair(60, "1h"),
                                Pair(120, "2h")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                intervals.forEach { (mins, label) ->
                                    FilterChip(
                                        selected = settings.overdueIntervalMinutes == mins,
                                        onClick = { viewModel.updateSettings(settings.copy(overdueIntervalMinutes = mins)) },
                                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Alarme sonoro ao disparar lembrete", fontSize = 12.sp)
                                Switch(
                                    checked = settings.overdueAlarmSound,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(overdueAlarmSound = it)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    viewModel.audioPlayer.playOverdueChime()
                                    viewModel.showToast("Som de alarme de lembrete reproduzido!")
                                }
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testar Som do Alarme de Lembrete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==================== 5. ALARME ANTI-PROCRASTINAÇÃO (TESTE) ====================
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Alarme Anti-Procrastinação", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Toca som contínuo e exige digitação exata", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Para vencer a procrastinação, o alarme só desliga quando a frase de compromisso pré-configurada for digitada.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = {
                                val target = habits.firstOrNull { it.isActive } ?: habits.firstOrNull()
                                if (target != null) {
                                    onTriggerTestAlarm(target)
                                } else {
                                    viewModel.showToast("Cadastre um hábito para testar o alarme.")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disparar Teste do Alarme Agora", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ==================== 6. METAS DE SONO & RITMO CIRCADIANO ====================
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Metas de Sono & Ritmo Circadiano", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Meta diária de sono:", fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (sleepGoalHours > 5) {
                                            sleepGoalHours -= 1
                                            viewModel.updateSettings(settings.copy(sleepGoalMinutes = sleepGoalHours * 60))
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Menos")
                                }
                                Text("${sleepGoalHours}h", fontWeight = FontWeight.Black, fontSize = 15.sp)
                                IconButton(
                                    onClick = {
                                        if (sleepGoalHours < 12) {
                                            sleepGoalHours += 1
                                            viewModel.updateSettings(settings.copy(sleepGoalMinutes = sleepGoalHours * 60))
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Mais")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = bedtimeTarget,
                                onValueChange = {
                                    bedtimeTarget = it
                                    viewModel.updateSettings(settings.copy(bedtimeTarget = it))
                                },
                                label = { Text("Hora Dormir Meta") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = wakeTarget,
                                onValueChange = {
                                    wakeTarget = it
                                    viewModel.updateSettings(settings.copy(wakeTarget = it))
                                },
                                label = { Text("Hora Acordar Meta") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ==================== 7. SEGURANÇA / PIN ====================
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Proteção por Senha (PIN)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        if (settings.requireAuth && !settings.pinCode.isNullOrBlank()) "Proteção ativa (${settings.pinCode?.length} dígitos)" else "Desativada",
                                        fontSize = 11.sp,
                                        color = if (settings.requireAuth) StatusDone else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.requireAuth,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        showPinDialog = true
                                    } else {
                                        viewModel.updateSettings(settings.copy(requireAuth = false, pinCode = null))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ==================== 8. GERENCIAMENTO DE DADOS (CRONOGRAMA & BACKUP) ====================
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gerenciamento de Dados", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Cronograma (.rvschedule)
                        Text("Cronograma de Hábitos (.rvschedule)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val jsonArr = JSONArray()
                                    habits.forEach { h ->
                                        val obj = JSONObject()
                                        obj.put("name", h.name)
                                        obj.put("type", h.type)
                                        obj.put("targetTime", h.targetTime)
                                        obj.put("durationMinutes", h.durationMinutes)
                                        obj.put("daysOfWeek", h.daysOfWeek)
                                        obj.put("priority", h.priority)
                                        obj.put("positiveConsequence", h.positiveConsequence)
                                        obj.put("negativeConsequence", h.negativeConsequence)
                                        obj.put("dismissPhrase", h.dismissPhrase)
                                        jsonArr.put(obj)
                                    }
                                    copyToClipboard("Cronograma (.rvschedule)", jsonArr.toString(2))
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exportar", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Backup Completo
                        Text("Backup Completo (.rvbackup)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val backupObj = JSONObject()
                                    backupObj.put("app", "Rotina Viva")
                                    backupObj.put("timestamp", System.currentTimeMillis())
                                    val habArr = JSONArray()
                                    habits.forEach { h ->
                                        val obj = JSONObject()
                                        obj.put("id", h.id)
                                        obj.put("name", h.name)
                                        obj.put("type", h.type)
                                        obj.put("targetTime", h.targetTime)
                                        obj.put("durationMinutes", h.durationMinutes)
                                        obj.put("daysOfWeek", h.daysOfWeek)
                                        obj.put("dismissPhrase", h.dismissPhrase)
                                        habArr.put(obj)
                                    }
                                    backupObj.put("habits", habArr)
                                    copyToClipboard("Backup Completo (.rvbackup)", backupObj.toString(2))
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gerar Backup", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // ==================== 9. REMOVER CONTEÚDO DE TESTE (RODAPÉ) ====================
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Conteúdo de Demonstração", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                Text(
                                    if (settings.isDummyActive) "Os 30 dias de dados de teste estão carregados." else "O conteúdo de teste já foi removido. Seus dados são 100% reais.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Deseja limpar todos os registros de teste e começar seu acompanhamento diário com histórico zerado? Seus hábitos cadastrados serão mantidos.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { showClearDummyDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remover conteúdo de teste", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Clear Dummy Confirmation Dialog
    if (showClearDummyDialog) {
        AlertDialog(
            onDismissRequest = { showClearDummyDialog = false },
            title = { Text("Remover Conteúdo de Teste?") },
            text = { Text("Esta ação irá apagar todos os registros de teste (check-ins, sonos e diários dos últimos 30 dias). Seus hábitos permanecerão cadastrados.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearDummyContent()
                        showClearDummyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sim, Limpar Tudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDummyDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Definir PIN de Proteção") },
            text = {
                Column {
                    Text("Insira de 4 a 6 dígitos numéricos:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempPin,
                        onValueChange = { tempPin = it.filter { char -> char.isDigit() }.take(6) },
                        placeholder = { Text("Ex: 1234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPin.length >= 4) {
                            viewModel.updateSettings(settings.copy(requireAuth = true, pinCode = tempPin))
                            viewModel.showToast("PIN configurado com sucesso!")
                            showPinDialog = false
                            tempPin = ""
                        }
                    },
                    enabled = tempPin.length >= 4
                ) {
                    Text("Salvar PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    tempPin = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
