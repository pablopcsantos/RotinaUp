package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.StatusSkipped
import com.example.util.AudioPlayer
import kotlinx.coroutines.delay

@Composable
fun DeepFocusScreen(
    habit: Habit,
    audioPlayer: AudioPlayer,
    onClose: () -> Unit,
    onComplete: (status: String) -> Unit
) {
    BackHandler {
        onClose()
    }

    val initialMinutes = habit.durationMinutes ?: 25
    var totalSeconds by remember { mutableIntStateOf(maxOf(60, initialMinutes * 60)) }
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(true) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, secondsLeft) {
        if (isRunning && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
            if (secondsLeft == 0) {
                isRunning = false
                isFinished = true
                audioPlayer.playCompletionFanfare()
            }
        }
    }

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)
    val progress = if (totalSeconds > 0) (totalSeconds - secondsLeft).toFloat() / totalSeconds.toFloat() else 1f
    val percentInt = (progress * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF22C55E).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MODO FOCO TOTAL",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Zero distrações",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
            }
        }

        // Center Content: Habit Info + Giant Timer
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = habit.name,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Horário: ${habit.targetTime} • Meta: ${totalSeconds / 60} minutos",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Big Circular Timer with Custom Canvas Ring
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 14.dp.toPx()
                    // Track circle
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = (size.minDimension - strokeW) / 2,
                        style = Stroke(width = strokeW)
                    )
                    // Progress arc
                    drawArc(
                        color = Color(0xFF22C55E),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormatted,
                        color = Color.White,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = if (isFinished) "Tempo Esgotado!" else if (isRunning) "Foco em Andamento" else "Pausado",
                        color = if (isFinished) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$percentInt% Concluído",
                        color = Color(0xFF4ADE80),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Timer Controls: -5m, Play/Pause, Reset, +5m
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val newTotal = maxOf(60, totalSeconds - 300)
                        totalSeconds = newTotal
                        secondsLeft = maxOf(0, secondsLeft - 300)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("-5m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (isFinished) {
                            secondsLeft = totalSeconds
                            isFinished = false
                            isRunning = true
                        } else {
                            isRunning = !isRunning
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFFEAB308) else Color(0xFF16A34A),
                        contentColor = if (isRunning) Color(0xFF0F172A) else Color.White
                    ),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "Pausar" else if (isFinished) "Reiniciar" else "Iniciar Foco",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                IconButton(
                    onClick = {
                        isRunning = false
                        isFinished = false
                        secondsLeft = totalSeconds
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        val newTotal = totalSeconds + 300
                        totalSeconds = newTotal
                        secondsLeft += 300
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("+5m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bottom Action Bar: Direct check-in to conclude
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Finalizou ou precisa registrar o hábito agora?",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onComplete("DONE") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusDone)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Feito", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onComplete("PARTIAL") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusPartial.copy(alpha = 0.2f),
                            contentColor = StatusPartial
                        )
                    ) {
                        Text("Parcial", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onComplete("SKIPPED") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusSkipped.copy(alpha = 0.2f),
                            contentColor = StatusSkipped
                        )
                    ) {
                        Text("Pular", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
