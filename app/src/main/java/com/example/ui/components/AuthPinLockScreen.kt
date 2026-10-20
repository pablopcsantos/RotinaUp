package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusSkipped
import kotlinx.coroutines.delay

@Composable
fun AuthPinLockScreen(
    onVerifyPin: (String) -> Boolean,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableStateOf(0) }
    var lockoutSeconds by remember { mutableStateOf(0) }

    // Rate limiting countdown timer
    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000)
            lockoutSeconds -= 1
        }
    }

    fun submitPin(entered: String) {
        if (lockoutSeconds > 0) return
        val isValid = onVerifyPin(entered)
        if (isValid) {
            failedAttempts = 0
            onSuccess()
        } else {
            isError = true
            pin = ""
            val nextFailed = failedAttempts + 1
            failedAttempts = nextFailed
            if (nextFailed >= 3) {
                // Progressive lockout delay (15s for 3 attempts, 30s for 4+)
                lockoutSeconds = if (nextFailed == 3) 15 else 30
            }
        }
    }

    fun onDigit(d: String) {
        if (lockoutSeconds > 0) return
        if (pin.length < 6) {
            val next = pin + d
            pin = next
            isError = false
            // Check if matches immediately (e.g. 4-digit PIN)
            if (onVerifyPin(next)) {
                failedAttempts = 0
                onSuccess()
            } else if (next.length == 6) {
                // Reached max length, verify and flag error if wrong
                submitPin(next)
            }
        }
    }

    fun onDelete() {
        if (lockoutSeconds > 0) return
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            isError = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            if (lockoutSeconds > 0) StatusSkipped.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (lockoutSeconds > 0) StatusSkipped else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "RotinaUp Protegida",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (lockoutSeconds > 0) "Acesso temporariamente bloqueado" else "Digite seu PIN para acessar",
                    fontSize = 12.sp,
                    color = if (lockoutSeconds > 0) StatusSkipped else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Dots indicator (4 to 6 dots)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val targetLen = 4
                    for (i in 0 until targetLen) {
                        val isFilled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        lockoutSeconds > 0 || isError -> StatusSkipped
                                        isFilled -> StatusDone
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (lockoutSeconds > 0 || isError) StatusSkipped else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                        )
                    }
                }

                if (lockoutSeconds > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Muitas tentativas. Aguarde ${lockoutSeconds}s...",
                        color = StatusSkipped,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isError) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "PIN incorreto. (${failedAttempts}/3 tentativas antes do bloqueio)",
                        color = StatusSkipped,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Numpad 3x4
                val isLockedOut = lockoutSeconds > 0
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(if (pin.length >= 4) "OK" else "", "0", "DEL")
                )

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.size(64.dp))
                            } else if (key == "OK") {
                                Surface(
                                    onClick = { submitPin(pin) },
                                    enabled = !isLockedOut && pin.length >= 4,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Confirmar",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else if (key == "DEL") {
                                Surface(
                                    onClick = { onDelete() },
                                    enabled = !isLockedOut,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Backspace,
                                            contentDescription = "Apagar",
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    onClick = { onDigit(key) },
                                    enabled = !isLockedOut,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
