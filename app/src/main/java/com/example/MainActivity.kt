package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.Habito
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.RotinaVivaTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleWidgetIntent(intent)

        setContent {
            val settings by viewModel.userSettings.collectAsState()
            val isDarkTheme = when (settings.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            RotinaVivaTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RotinaUpApp(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        intent = intent
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra("from_widget", false) == true) {
            // Opened via home screen widget: snooze popups so user lands cleanly on the today view
            viewModel.snoozeOverdue(5)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateCurrentTime()
    }
}

@Composable
fun RotinaUpApp(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    intent: android.content.Intent? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "today"

    val settings by viewModel.userSettings.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val consequence by viewModel.consequence.collectAsState()

    val activeHabitsList by viewModel.activeHabits.collectAsState()
    val todayLogsList by viewModel.todayLogs.collectAsState()
    val todaySleepLog by viewModel.todaySleepLog.collectAsState()
    val todayDailyReview by viewModel.todayDailyReview.collectAsState()
    val currentTimeStr by viewModel.currentTimeStr.collectAsState()
    val snoozeUntil by viewModel.snoozeUntil.collectAsState()
    val mandatorySnoozeUntil by viewModel.mandatorySnoozeUntil.collectAsState()

    val authState by viewModel.authState.collectAsState()

    // Settle delay to ensure Room database flows have emitted their initial disk state
    // before any modal dialog or alarm evaluation occurs (eliminates startup dialog flash)
    var isDataSettled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(600)
        isDataSettled = true
    }

    val isFromWidget = remember(intent) { intent?.getBooleanExtra("from_widget", false) == true }

    // Dialog & overlay states
    var activeFocusHabit by remember { mutableStateOf<Habito?>(null) }
    var activeAlarmHabit by remember { mutableStateOf<Habito?>(null) }
    var activeCheckInHabit by remember { mutableStateOf<Habito?>(null) }
    var showWidgetPreview by remember { mutableStateOf(false) }

    // Overdue Habit Alert triggering logic (Requirement 7)
    val currentDateStr by viewModel.currentDateStr.collectAsState()
    val todayIsoDay = remember(currentDateStr) { DateUtils.getIsoDayOfWeek(currentDateStr) }
    val todayLogsMap = remember(todayLogsList) { todayLogsList.associateBy { it.habitId } }
    val isUnlocked = authState is com.example.ui.viewmodel.AuthState.Unlocked

    val overdueHabitForAlarm = remember(isUnlocked, isDataSettled, activeHabitsList, todayLogsMap, currentTimeStr, settings.overdueRemindersEnabled, snoozeUntil, isFromWidget) {
        if (!isUnlocked || !isDataSettled || isFromWidget || !settings.overdueRemindersEnabled || System.currentTimeMillis() < snoozeUntil) null
        else {
            activeHabitsList.firstOrNull { h ->
                val isToday = h.isScheduledOn(todayIsoDay)
                val log = todayLogsMap[h.id]
                val isDoneOrSkipped = log?.status == "DONE" || log?.status == "SKIPPED"
                isToday && !isDoneOrSkipped && h.targetTime <= currentTimeStr
            }
        }
    }

    // Overdue Mandatory Steps Alarm triggering logic
    val isMandatoryOverdueForAlarm = remember(
        isUnlocked,
        isDataSettled,
        isFromWidget,
        todaySleepLog,
        todayDailyReview,
        currentTimeStr,
        settings.mandatoryRemindersEnabled,
        settings.mandatoryDeadlineTime,
        mandatorySnoozeUntil
    ) {
        if (!isUnlocked || !isDataSettled || isFromWidget || !settings.mandatoryRemindersEnabled || System.currentTimeMillis() < mandatorySnoozeUntil) {
            false
        } else {
            val pending = (todaySleepLog == null) || (todayDailyReview == null)
            pending && currentTimeStr >= settings.mandatoryDeadlineTime
        }
    }

    // Periodic time ticker for overdue checks
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000)
            viewModel.updateCurrentTime()
        }
    }

    // Auto-dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000)
            viewModel.clearToast()
        }
    }

    // 0. Loading State: Do not expose any protected screens while loading settings
    if (authState is com.example.ui.viewmodel.AuthState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // 1. PIN Lock Screen: Must verify before granting access
    if (authState is com.example.ui.viewmodel.AuthState.Locked) {
        AuthPinLockScreen(
            onVerifyPin = { entered, recordFailure -> viewModel.verifyPin(entered, recordFailure) },
            onSuccess = { viewModel.unlockSession() }
        )
        return
    }

    // 2. Full-Screen Modo Foco Total
    if (activeFocusHabit != null) {
        val habit = activeFocusHabit!!
        DeepFocusScreen(
            habit = habit,
            audioPlayer = viewModel.audioPlayer,
            onClose = { activeFocusHabit = null },
            onComplete = { status ->
                viewModel.checkInHabit(habit.id, status, null, "Concluído em Modo Foco Total")
                activeFocusHabit = null
            }
        )
        return
    }

    // 3. Full-Screen Anti-procrastination Typing Alarm
    if (activeAlarmHabit != null) {
        val habit = activeAlarmHabit!!
        AlarmDismissModal(
            habit = habit,
            audioPlayer = viewModel.audioPlayer,
            onDismiss = { typedText, attempts ->
                viewModel.checkInHabit(habit.id, "DONE", null, "Concluído via Alarme com Digitação")
                viewModel.recordAlarmEvent(habit.id, habit.name, attempts, false, typedText)
                activeAlarmHabit = null
            },
            onSnooze = {
                viewModel.recordAlarmEvent(habit.id, habit.name, 1, true, "")
                viewModel.snoozeOverdue(5)
                activeAlarmHabit = null
            }
        )
    }

    // 4. Overdue Habit Alarm & Dialog (Requirement 7)
    if (activeFocusHabit == null && activeAlarmHabit == null && overdueHabitForAlarm != null) {
        OverdueAlarmDialog(
            habit = overdueHabitForAlarm,
            intervalMinutes = settings.overdueIntervalMinutes,
            audioPlayer = viewModel.audioPlayer,
            onCheckIn = {
                viewModel.checkInHabit(
                    overdueHabitForAlarm.id,
                    "DONE",
                    null,
                    "Realizado (registrado via lembrete de atraso)"
                )
            },
            onAdiar = {
                viewModel.snoozeOverdue(settings.overdueIntervalMinutes)
            }
        )
    }

    // 4.1 Overdue Mandatory Steps Alarm & Dialog
    if (activeFocusHabit == null && activeAlarmHabit == null && overdueHabitForAlarm == null && isMandatoryOverdueForAlarm) {
        MandatoryOverdueDialog(
            deadlineTime = settings.mandatoryDeadlineTime,
            intervalMinutes = settings.mandatoryIntervalMinutes,
            soundEnabled = settings.mandatoryAlarmSound,
            isSleepMissing = todaySleepLog == null,
            isDailyReviewMissing = todayDailyReview == null,
            audioPlayer = viewModel.audioPlayer,
            onNavigateToSleep = {
                viewModel.snoozeMandatoryOverdue(settings.mandatoryIntervalMinutes)
                navController.navigate("sleep")
            },
            onNavigateToDailyReview = {
                viewModel.snoozeMandatoryOverdue(settings.mandatoryIntervalMinutes)
                navController.navigate("daily")
            },
            onSnooze = {
                viewModel.snoozeMandatoryOverdue(settings.mandatoryIntervalMinutes)
            }
        )
    }

    // 5. Consequence Modal
    consequence?.let { (habitName, msg) ->
        ConsequenceDialog(
            habitName = habitName,
            message = msg,
            isSuccess = !msg.contains("Pular", ignoreCase = true) && !msg.contains("atrasa", ignoreCase = true) && !msg.contains("prejudica", ignoreCase = true) && !msg.contains("sedentarismo", ignoreCase = true),
            onDismiss = { viewModel.clearConsequence() }
        )
    }

    // 6. Detailed Check-In Modal
    activeCheckInHabit?.let { habit ->
        CheckInDialog(
            habit = habit,
            onDismiss = { activeCheckInHabit = null },
            onConfirm = { status, reason, note ->
                viewModel.checkInHabit(habit.id, status, reason, note)
                activeCheckInHabit = null
            },
            onStartFocusMode = {
                val h = habit
                activeCheckInHabit = null
                activeFocusHabit = h
            }
        )
    }

    // 7. Widget Preview Modal
    if (showWidgetPreview) {
        val habits by viewModel.allHabits.collectAsState()
        val logs by viewModel.todayLogs.collectAsState()
        val allLogs by viewModel.allLogs.collectAsState()
        WidgetPreviewModal(
            habits = habits,
            logs = logs,
            allLogs = allLogs,
            onDismiss = { showWidgetPreview = false },
            onQuickCheckIn = { h, s -> viewModel.checkInHabit(h.id, s) }
        )
    }

    // 8. Dummy Content Notice (First Launch - only after data has settled and not when launched from widget)
    if (isDataSettled && !isFromWidget && settings.isDummyActive && !settings.hasSeenDummyPrompt) {
        DummyContentDialog(
            onDismiss = { viewModel.dismissDummyPrompt() },
            onClearImmediately = { viewModel.clearDummyContent() }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brand Logo + Title + Date
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "RotinaUp",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RotinaUp",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = DateUtils.formatFriendlyDate(viewModel.todayDateStr),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Top Action Icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.navigate("weekly_report") }) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = "Relatório Semanal",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            val nextMode = if (isDarkTheme) "light" else "dark"
                            viewModel.updateSettings(settings.copy(themeMode = nextMode))
                        }) {
                            Icon(
                                if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Alternar Tema",
                                tint = if (isDarkTheme) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Hoje
                NavigationBarItem(
                    selected = currentRoute == "today",
                    onClick = {
                        if (currentRoute != "today") {
                            navController.navigate("today") {
                                popUpTo("today") { inclusive = true }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Hoje") },
                    label = { Text("Hoje", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )

                // Sono
                NavigationBarItem(
                    selected = currentRoute == "sleep",
                    onClick = {
                        if (currentRoute != "sleep") {
                            navController.navigate("sleep")
                        }
                    },
                    icon = { Icon(Icons.Default.Nightlight, contentDescription = "Sono") },
                    label = { Text("Sono", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )

                // Diário
                NavigationBarItem(
                    selected = currentRoute == "daily",
                    onClick = {
                        if (currentRoute != "daily") {
                            navController.navigate("daily")
                        }
                    },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Diário") },
                    label = { Text("Diário", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )

                // Hábitos
                NavigationBarItem(
                    selected = currentRoute == "habits",
                    onClick = {
                        if (currentRoute != "habits") {
                            navController.navigate("habits")
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Hábitos") },
                    label = { Text("Hábitos", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )

                // Ajustes
                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = {
                        if (currentRoute != "settings") {
                            navController.navigate("settings")
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "today",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("today") {
                    TodayScreen(
                        viewModel = viewModel,
                        onNavigateToSleep = { navController.navigate("sleep") },
                        onNavigateToDailyReview = { navController.navigate("daily") },
                        onNavigateToAddHabit = { navController.navigate("habits") },
                        onStartFocusMode = { habit -> activeFocusHabit = habit },
                        onOpenDetailedCheckIn = { habit -> activeCheckInHabit = habit }
                    )
                }

                composable("sleep") {
                    SleepScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("daily") {
                    DailyReviewScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("habits") {
                    HabitsListScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToHabits = { navController.navigate("habits") },
                        onOpenWidgetPreview = { showWidgetPreview = true },
                        onTriggerTestAlarm = { habit -> activeAlarmHabit = habit }
                    )
                }

                composable("weekly_report") {
                    WeeklyReportScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            // Toast Floating Banner
            toastMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
