package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.model.Habit
import com.example.receiver.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

object AlarmScheduler {

    const val ACTION_HABIT_REMINDER = "com.example.ACTION_HABIT_REMINDER"
    const val ACTION_MANDATORY_REMINDER = "com.example.ACTION_MANDATORY_REMINDER"
    const val ACTION_HABIT_QUICK_DONE = "com.example.ACTION_HABIT_QUICK_DONE"
    const val ACTION_HABIT_SNOOZE = "com.example.ACTION_HABIT_SNOOZE"
    const val ACTION_HABIT_SNOOZE_FIRE = "com.example.ACTION_HABIT_SNOOZE_FIRE"

    const val EXTRA_HABIT_ID = "extra_habit_id"
    const val EXTRA_HABIT_NAME = "extra_habit_name"
    const val EXTRA_TARGET_TIME = "extra_target_time"

    private const val MANDATORY_REQUEST_CODE = 99999

    /**
     * Schedules an alarm with AlarmManager for a specific habit based on its targetTime and daysOfWeek.
     */
    fun scheduleHabitReminder(context: Context, habit: Habit) {
        if (!habit.isActive) {
            cancelHabitReminder(context, habit.id)
            return
        }

        val triggerAtMillis = calculateNextTriggerTime(habit.targetTime, habit.daysOfWeek)
        if (triggerAtMillis == null) {
            cancelHabitReminder(context, habit.id)
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
            putExtra(EXTRA_HABIT_ID, habit.id)
            putExtra(EXTRA_HABIT_NAME, habit.name)
            putExtra(EXTRA_TARGET_TIME, habit.targetTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (t: Throwable) {
            android.util.Log.e("AlarmScheduler", "Failed to schedule habit alarm ${habit.id}", t)
        }
    }

    /**
     * Cancels any pending AlarmManager alarm for the given habit ID.
     */
    fun cancelHabitReminder(context: Context, habitId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Schedules a mandatory deadline reminder for the daily review & sleep check-in.
     */
    fun scheduleMandatoryReminder(context: Context, deadlineTime: String) {
        val triggerAtMillis = calculateTodayOrTomorrowTime(deadlineTime)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MANDATORY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MANDATORY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (t: Throwable) {
            android.util.Log.e("AlarmScheduler", "Failed to schedule mandatory alarm", t)
        }
    }

    /**
     * Cancels the pending daily mandatory reminder.
     */
    fun cancelMandatoryReminder(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MANDATORY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MANDATORY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Cancels a one-off snooze alarm for a habit.
     */
    fun cancelHabitSnooze(context: Context, habitId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_HABIT_SNOOZE_FIRE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 10 + 3).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Reschedules alarms according to current preferences, also removing stale alarms.
     */
    suspend fun rescheduleAllAlarmsSuspend(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettings().firstOrNull()
            val habits = db.habitDao().getAllHabits().firstOrNull() ?: emptyList()
            val notificationsEnabled = settings?.notificationsEnabled == true
            val habitRemindersEnabled = notificationsEnabled && settings?.overdueRemindersEnabled == true

            habits.forEach { habit ->
                if (habitRemindersEnabled) {
                    scheduleHabitReminder(context, habit)
                } else {
                    cancelHabitReminder(context, habit.id)
                    cancelHabitSnooze(context, habit.id)
                    androidx.core.app.NotificationManagerCompat.from(context).cancel(habit.id.toInt())
                }
            }

            if (notificationsEnabled && settings?.mandatoryRemindersEnabled == true) {
                scheduleMandatoryReminder(context, settings.mandatoryDeadlineTime)
            } else {
                cancelMandatoryReminder(context)
                androidx.core.app.NotificationManagerCompat.from(context).cancel(MANDATORY_REQUEST_CODE)
            }
        } catch (t: Throwable) {
            android.util.Log.e("AlarmScheduler", "Error rescheduling alarms", t)
        }
    }

    /**
     * Reschedules all active habits and mandatory reminders (e.g. on boot or settings change).
     */
    fun rescheduleAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            rescheduleAllAlarmsSuspend(context)
        }
    }

    private fun calculateNextTriggerTime(timeStr: String, daysOfWeek: String): Long? {
        val parts = timeStr.split(":")
        val targetHour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val targetMin = parts.getOrNull(1)?.toIntOrNull() ?: return null

        val scheduledDays = daysOfWeek.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

        if (scheduledDays.isEmpty()) return null

        val cal = Calendar.getInstance()
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMin)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // ISO day of week: Monday=1 ... Sunday=7
            val isoDay = when (candidate.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            if (scheduledDays.contains(isoDay) && candidate.timeInMillis > System.currentTimeMillis()) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    private fun calculateTodayOrTomorrowTime(timeStr: String): Long {
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 22
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
