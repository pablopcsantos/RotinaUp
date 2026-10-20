package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.StatusSkipped
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReportScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val habits by viewModel.allHabits.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val sleepLogs by viewModel.allSleepLogs.collectAsState()
    val napLogs by viewModel.allNapLogs.collectAsState()
    val dailyReviews by viewModel.allDailyReviews.collectAsState()

    var medalFilter by remember { mutableStateOf("ALL") } // ALL, UNLOCKED, LOCKED

    val report = remember(habits, allLogs, sleepLogs, napLogs, dailyReviews) {
        viewModel.computeWeeklyReport(habits, allLogs, sleepLogs, napLogs, dailyReviews)
    }

    val periodStr = remember {
        val start = DateUtils.getTodayDateString(-6)
        val end = DateUtils.getTodayDateString(0)
        "${DateUtils.formatFriendlyDate(start)} até ${DateUtils.formatFriendlyDate(end)}"
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        viewModel.showToast("$label copiado para a área de transferência!")
    }

    fun exportJson() {
        val root = JSONObject()
        root.put("periodo", periodStr)
        root.put("adesaoGeralPercent", report.overallAdherence)
        root.put("nivelConsistencia", report.tier.name)
        root.put("nivelNumero", report.tier.levelNumber)
        root.put("mediaSleepHours", report.avgSleepHours ?: JSONObject.NULL)
        root.put("variacaoDormirHours", report.bedtimeVariationHours ?: JSONObject.NULL)
        root.put("contagemCochilos", report.napCount)
        root.put("sugestaoCircadiana", report.suggestion)

        val habArray = JSONArray()
        report.habitMatrices.forEach { m ->
            val hObj = JSONObject()
            hObj.put("habito", m.habit.name)
            hObj.put("taxaAdesaoPercent", m.rate)
            hObj.put("diasConcluidos", "${m.completedDaysCount}/7")
            habArray.put(hObj)
        }
        root.put("matrizHabitos", habArray)

        val medArray = JSONArray()
        report.medals.forEach { m ->
            val medObj = JSONObject()
            medObj.put("titulo", m.title)
            medObj.put("conquistada", m.unlocked)
            medObj.put("progresso", m.progressText ?: "")
            medArray.put(medObj)
        }
        root.put("medalhas", medArray)

        copyToClipboard("Relatório Semanal (JSON)", root.toString(2))
    }

    fun exportCsv() {
        val sb = StringBuilder()
        sb.append("Métrica,Valor\n")
        sb.append("Período,\"$periodStr\"\n")
        sb.append("Adesão Geral (%),${report.overallAdherence}\n")
        sb.append("Nível Semanal,\"${report.tier.name}\"\n")
        sb.append("Média de Sono (h),${report.avgSleepHours ?: "Sem dados"}\n")
        sb.append("Variação Horário Deitar (h),${report.bedtimeVariationHours?.let { "±$it" } ?: "Sem dados"}\n")
        sb.append("Contagem de Cochilos,${report.napCount}\n")
        sb.append("Sugestão,\"${report.suggestion.replace("\"", "\"\"")}\"\n\n")

        sb.append("Hábito,Adesão (%),Dias Concluídos\n")
        report.habitMatrices.forEach { m ->
            sb.append("\"${m.habit.name.replace("\"", "\"\"")}\",${m.rate},${m.completedDaysCount}/7\n")
        }

        copyToClipboard("Relatório Semanal (CSV)", sb.toString())
    }

    val unlockedCount = remember(report.medals) { report.medals.count { it.unlocked } }
    val filteredMedals = remember(report.medals, medalFilter) {
        when (medalFilter) {
            "UNLOCKED" -> report.medals.filter { it.unlocked }
            "LOCKED" -> report.medals.filter { !it.unlocked }
            else -> report.medals
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Relatório Semanal", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Últimos 7 dias", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = { exportJson() }) {
                        Text("JSON", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    TextButton(onClick = { exportCsv() }) {
                        Text("CSV", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
            contentPadding = PaddingValues(top = 6.dp, bottom = 40.dp)
        ) {
            item {
                Text(
                    text = "Período: $periodStr",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // ==================== 1. NÍVEL DE CONSISTÊNCIA SEMANAL (1 a 5) ====================
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(report.tier.colorHex).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(report.tier.colorHex).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "NÍVEL SEMANAL ${report.tier.levelNumber} DE 5",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(report.tier.colorHex),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+${report.tier.xpPoints} XP",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = StatusPartial
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = report.tier.badge, fontSize = 32.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = report.tier.name,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = Color(report.tier.colorHex)
                                        )
                                        Text(
                                            text = report.tier.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${report.overallAdherence}%",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = StatusDone
                                )
                                Text("Adesão Geral", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar (Pure Green)
                        LinearProgressIndicator(
                            progress = { report.overallAdherence / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = StatusDone,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 5 Progression Tiers Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("L1: Bronze", fontSize = 9.sp, fontWeight = if (report.tier.levelNumber >= 1) FontWeight.Bold else FontWeight.Normal)
                            Text("L2: Prata", fontSize = 9.sp, fontWeight = if (report.tier.levelNumber >= 2) FontWeight.Bold else FontWeight.Normal)
                            Text("L3: Ouro", fontSize = 9.sp, fontWeight = if (report.tier.levelNumber >= 3) FontWeight.Bold else FontWeight.Normal)
                            Text("L4: Platina", fontSize = 9.sp, fontWeight = if (report.tier.levelNumber >= 4) FontWeight.Bold else FontWeight.Normal)
                            Text("L5: Diamante", fontSize = 9.sp, fontWeight = if (report.tier.levelNumber >= 5) FontWeight.Bold else FontWeight.Normal)
                        }

                        if (report.tier.nextLevelName != null && report.tier.pointsToNextLevel != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Próximo nível: ${report.tier.nextLevelName}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Faltam +${report.tier.pointsToNextLevel}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 2. SISTEMA DE MEDALHAS DA SEMANA ====================
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Medalhas de Hábitos da Semana", fontWeight = FontWeight.Black, fontSize = 15.sp)
                                Text("$unlockedCount de ${report.medals.size} medalhas conquistadas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.audioPlayer.playCompletionFanfare() }) {
                                Icon(Icons.Default.Celebration, contentDescription = "Comemorar", tint = StatusPartial)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = medalFilter == "ALL",
                                onClick = { medalFilter = "ALL" },
                                label = { Text("Todas (${report.medals.size})", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = medalFilter == "UNLOCKED",
                                onClick = { medalFilter = "UNLOCKED" },
                                label = { Text("Conquistadas ($unlockedCount)", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = medalFilter == "LOCKED",
                                onClick = { medalFilter = "LOCKED" },
                                label = { Text("Em Progresso (${report.medals.size - unlockedCount})", fontSize = 10.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Medals Grid
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredMedals.forEach { medal ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (medal.unlocked) StatusDone.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (medal.unlocked) StatusDone.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (medal.unlocked) StatusDone.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = if (medal.unlocked) medal.icon else "🔒", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(medal.title, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                                Text(
                                                    text = if (medal.unlocked) "Conquistada" else "Em Progresso",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (medal.unlocked) StatusDone else StatusPartial
                                                )
                                            }
                                            Text(medal.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (!medal.progressText.isNullOrBlank()) {
                                                Text(
                                                    text = "Meta: ${medal.requirement} • ${medal.progressText}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (medal.unlocked) StatusDone else StatusPartial
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

            // ==================== 3. MATRIZ DE TRANSFORMAÇÃO DOS HÁBITOS ====================
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Transformação dos Hábitos", fontWeight = FontWeight.Black, fontSize = 15.sp)
                                Text("Bolinhas verdes indicam consolidação", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Matriz Semanal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusDone)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            report.habitMatrices.forEach { m ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(m.habit.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("${m.completedDaysCount}/7 dias", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (m.rate >= 70) StatusDone.copy(alpha = 0.15f) else StatusPartial.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "${m.rate}%",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (m.rate >= 70) StatusDone else StatusPartial,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // 7 Dots Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Dias:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                m.days.forEach { d ->
                                                    val isDone = d.status == "DONE"
                                                    val isPartial = d.status == "PARTIAL"
                                                    val isSkipped = d.status == "SKIPPED"

                                                    val dotColor = when {
                                                        isDone -> StatusDone
                                                        isPartial -> StatusPartial
                                                        isSkipped -> StatusSkipped
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }

                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(dotColor)
                                                                .border(
                                                                    1.dp,
                                                                    if (isDone) StatusDone else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                                    CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isDone) {
                                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                            } else if (isPartial) {
                                                                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                            } else if (isSkipped) {
                                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                            }
                                                        }
                                                        Text(
                                                            text = d.dayInitial,
                                                            fontSize = 9.sp,
                                                            fontWeight = if (d.isToday) FontWeight.Black else FontWeight.Normal,
                                                            color = if (d.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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

            // ==================== 4. PADRÕES DE SONO & DESCANSO ====================
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Padrões de Sono & Descanso", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Nightlight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (report.avgSleepHours != null) "${report.avgSleepHours}h" else "--", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text("Média de Sono", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = StatusPartial, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (report.bedtimeVariationHours != null) "±${report.bedtimeVariationHours}h" else "--", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text("Variação Deitar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.WbSunny, contentDescription = null, tint = StatusDone, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${report.napCount}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text("Cochilos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Top Motivos para Pular
            if (report.topSkipReasons.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Top Motivos para Pular", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            report.topSkipReasons.forEachIndexed { idx, (reason, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${idx + 1}. $reason", fontSize = 12.sp)
                                    Text("$count ${if (count == 1) "vez" else "vezes"}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Sugestão Circadiana
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = StatusPartial, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Sugestão Circadiana Rotina Viva", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(report.suggestion, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
