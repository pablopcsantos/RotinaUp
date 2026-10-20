package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Habit
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.StatusSkipped
import com.example.util.AudioPlayer
import com.example.util.TextNormalizer

@Composable
fun AlarmDismissModal(
    habit: Habit,
    audioPlayer: AudioPlayer,
    onDismiss: (typedText: String, attempts: Int) -> Unit,
    onSnooze: () -> Unit
) {
    val context = LocalContext.current
    var typedText by remember { mutableStateOf("") }
    var attempts by remember { mutableIntStateOf(1) }

    var showMismatchError by remember { mutableStateOf(false) }

    val targetPhrase = habit.dismissPhrase.ifBlank { "eu assumo o compromisso" }
    val isMatch = remember(typedText, targetPhrase) {
        TextNormalizer.isPhraseMatch(typedText, targetPhrase)
    }

    // Start alarm sound and vibration on launch
    DisposableEffect(Unit) {
        audioPlayer.startAlarmLoop()
        var vibrator: Vibrator? = null
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 400, 200, 400, 800)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 400, 200, 400), 0)
            }
        } catch (_: Throwable) {}

        onDispose {
            audioPlayer.stopAlarm()
            try {
                vibrator?.cancel()
            } catch (_: Throwable) {}
        }
    }

    Dialog(
        onDismissRequest = { /* Cannot dismiss without typing or snoozing! */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF881337).copy(alpha = 0.95f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing Alarm Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StatusSkipped.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = StatusSkipped,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "ALARME ANTI-PROCRASTINAÇÃO",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = StatusSkipped,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = habit.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Horário programado: ${habit.targetTime}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target Phrase Box
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Para desligar o som, digite exatamente:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"$targetPhrase\"",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = {
                            typedText = it
                            showMismatchError = false
                        },
                        placeholder = { Text(targetPhrase) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isMatch) StatusDone else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (isMatch) StatusDone else MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Feedback status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMatch) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = StatusDone, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Frase correta! Desligamento liberado.",
                                color = StatusDone,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (showMismatchError) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusSkipped, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Frase incorreta (${attempts}ª tentativa). Digite exatamente.",
                                color = StatusSkipped,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusPartial, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aguardando digitação correta da frase...",
                                color = StatusPartial,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dismiss Button
                    Button(
                        onClick = {
                            if (isMatch) {
                                audioPlayer.stopAlarm()
                                audioPlayer.playCompletionFanfare()
                                onDismiss(typedText, attempts)
                            } else {
                                attempts++
                                showMismatchError = true
                            }
                        },
                        enabled = typedText.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMatch) StatusDone else MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = if (isMatch) "DESLIGAR ALARME E FAZER" else "VALIDAR E DESLIGAR",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Snooze Button
                    TextButton(
                        onClick = {
                            audioPlayer.stopAlarm()
                            onSnooze()
                        }
                    ) {
                        Text(
                            text = "Soneca (5 minutos)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
