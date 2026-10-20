package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.HabitLog
import com.example.util.AlarmScheduler
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        AlarmScheduler.rescheduleAllAlarmsSuspend(context)
                    }

                    AlarmScheduler.ACTION_HABIT_REMINDER -> {
                        val habitId = intent.getLongExtra(AlarmScheduler.EXTRA_HABIT_ID, -1L)
                        if (habitId != -1L) {
                            handleHabitReminder(context, habitId, isSnooze = false)
                        }
                    }

                    AlarmScheduler.ACTION_HABIT_SNOOZE_FIRE -> {
                        val habitId = intent.getLongExtra(AlarmScheduler.EXTRA_HABIT_ID, -1L)
                        if (habitId != -1L) {
                            handleHabitReminder(context, habitId, isSnooze = true)
                        }
                    }

                    AlarmScheduler.ACTION_MANDATORY_REMINDER -> {
                        handleMandatoryReminder(context)
                    }

                    AlarmScheduler.ACTION_HABIT_QUICK_DONE -> {
                        val habitId = intent.getLongExtra(AlarmScheduler.EXTRA_HABIT_ID, -1L)
                        if (habitId != -1L) {
                            handleQuickDone(context, habitId)
                        }
                    }

                    AlarmScheduler.ACTION_HABIT_SNOOZE -> {
                        val habitId = intent.getLongExtra(AlarmScheduler.EXTRA_HABIT_ID, -1L)
                        handleSnooze(context, habitId)
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e("AlarmReceiver", "Error processing alarm broadcast: $action", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleHabitReminder(context: Context, habitId: Long, isSnooze: Boolean = false) {
        val db = AppDatabase.getDatabase(context)
        val habits = db.habitDao().getAllActiveHabits().firstOrNull() ?: emptyList()
        val habit = habits.find { it.id == habitId } ?: return

        // Always reschedule the next regular occurrence immediately so the recurring chain is preserved
        if (!isSnooze) {
            AlarmScheduler.scheduleHabitReminder(context, habit)
        }

        val settings = db.settingsDao().getSettings().firstOrNull() ?: return
        if (!settings.notificationsEnabled || !settings.overdueRemindersEnabled) return

        val todayDateStr = DateUtils.getTodayDateString(0)
        val todayIsoDay = DateUtils.getIsoDayOfWeek(todayDateStr)

        if (!habit.isScheduledOn(todayIsoDay)) return

        val todayLogs = db.habitDao().getLogsForDate(todayDateStr).firstOrNull() ?: emptyList()
        val existingLog = todayLogs.find { it.habitId == habit.id }
        if (existingLog?.status == "DONE" || existingLog?.status == "SKIPPED") {
            return
        }

        // Post system notification
        ensureNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_notification", true)
            putExtra("habit_id", habit.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            habit.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_HABIT_QUICK_DONE
            putExtra(AlarmScheduler.EXTRA_HABIT_ID, habit.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (habit.id * 10 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_HABIT_SNOOZE
            putExtra(AlarmScheduler.EXTRA_HABIT_ID, habit.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (habit.id * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("RotinaUp: ${habit.name}")
            .setContentText("Horário previsto: ${habit.targetTime}. Mantenha seu ritmo de hábitos hoje!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Concluir ✅", donePendingIntent)
            .addAction(0, "Adiar 15m ⏰", snoozePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(habit.id.toInt(), builder.build())
        } catch (_: SecurityException) {
            // Notification permission might not be granted by user
        }
    }

    private suspend fun handleMandatoryReminder(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val settings = db.settingsDao().getSettings().firstOrNull() ?: return
        if (!settings.notificationsEnabled || !settings.mandatoryRemindersEnabled) return

        val todayDateStr = DateUtils.getTodayDateString(0)
        val sleepLog = db.sleepDao().getSleepLog(todayDateStr).firstOrNull()
        val dailyReview = db.sleepDao().getDailyReview(todayDateStr).firstOrNull()

        // Reschedule for tomorrow
        AlarmScheduler.scheduleMandatoryReminder(context, settings.mandatoryDeadlineTime)

        if (sleepLog != null && dailyReview != null) return

        ensureNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_notification", true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            99998,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val missingText = when {
            sleepLog == null && dailyReview == null -> "Sono e Diário Noturno pendentes!"
            sleepLog == null -> "Registro de sono pendente!"
            else -> "Diário Noturno pendente!"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("RotinaUp: Etapas Obrigatórias")
            .setContentText("$missingText Toque para preencher antes de dormir.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(99999, builder.build())
        } catch (_: SecurityException) {}
    }

    private suspend fun handleQuickDone(context: Context, habitId: Long) {
        val db = AppDatabase.getDatabase(context)
        val habits = db.habitDao().getAllActiveHabits().firstOrNull() ?: emptyList()
        val habit = habits.find { it.id == habitId } ?: return

        val today = DateUtils.getTodayDateString(0)
        db.habitDao().insertOrUpdateLog(
            HabitLog(
                habitId = habitId,
                date = today,
                status = "DONE",
                note = "Concluído via notificação"
            )
        )
        NotificationManagerCompat.from(context).cancel(habitId.toInt())
        com.example.widget.RotinaUpWidgetProvider.updateAllWidgets(context)
    }

    private fun handleSnooze(context: Context, habitId: Long) {
        NotificationManagerCompat.from(context).cancel(habitId.toInt())
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_HABIT_SNOOZE_FIRE
            putExtra(AlarmScheduler.EXTRA_HABIT_ID, habitId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 10 + 3).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + 15 * 60 * 1000L
        alarmManager?.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lembretes RotinaUp"
            val descriptionText = "Notificações de horários e lembretes de hábitos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "rotinaup_reminders"
    }
}
