package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class RotinaUpWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SYNC_WIDGET) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val thisWidget = ComponentName(context, RotinaUpWidgetProvider::class.java)
                    val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                    for (id in allWidgetIds) {
                        updateAppWidgetSync(context, appWidgetManager, id)
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("RotinaUpWidget", "Erro durante onReceive sync do widget", t)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidgetSync(context, appWidgetManager, appWidgetId)
                }
            } catch (t: Throwable) {
                android.util.Log.e("RotinaUpWidget", "Erro durante onUpdate do widget", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAppWidgetSync(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_rotinaup)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("from_widget", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setTextViewText(R.id.widget_title, "RotinaUp")
            views.setTextColor(R.id.widget_title, 0xFF38BDF8.toInt())
            views.setTextViewText(R.id.widget_date, DateUtils.formatWidgetDate(DateUtils.getTodayDateString(0)))
            views.setTextColor(R.id.widget_date, 0xFF94A3B8.toInt())
            views.setTextColor(R.id.widget_more_text, 0xFF94A3B8.toInt())

            // Set last 7 days initials
            val last7Days = DateUtils.getLast7Days()
            val dayLabelIds = listOf(
                R.id.day_label_1, R.id.day_label_2, R.id.day_label_3,
                R.id.day_label_4, R.id.day_label_5, R.id.day_label_6, R.id.day_label_7
            )
            last7Days.forEachIndexed { idx, (_, initial, _) ->
                if (idx < dayLabelIds.size) {
                    views.setTextViewText(dayLabelIds[idx], initial)
                }
            }

            try {
                val db = AppDatabase.getDatabase(context)
                val settings = kotlinx.coroutines.runBlocking {
                    db.settingsDao().getSettings().firstOrNull()
                }
                val isLocked = settings?.requireAuth == true && settings.hasPin()

                val allActiveHabits = kotlinx.coroutines.runBlocking {
                    db.habitDao().getAllActiveHabits().firstOrNull() ?: emptyList()
                }
                val habits = allActiveHabits.filter { it.showInWidget }.sortedBy { it.targetTime }
                val allLogs = kotlinx.coroutines.runBlocking {
                    db.habitDao().getAllLogs().firstOrNull() ?: emptyList()
                }

                views.removeAllViews(R.id.widget_habits_container)

                val dotIds = listOf(
                    R.id.dot_1, R.id.dot_2, R.id.dot_3,
                    R.id.dot_4, R.id.dot_5, R.id.dot_6, R.id.dot_7
                )

                habits.forEach { habit ->
                    val rowView = RemoteViews(context.packageName, R.layout.widget_habit_item)
                    rowView.setTextViewText(R.id.habit_item_name, if (isLocked) "Hábito Protegido" else habit.name)
                    rowView.setTextViewText(R.id.habit_item_time, habit.targetTime)

                    // Populate 7-day adherence dots
                    last7Days.forEachIndexed { dIdx, (dateStr, _, isToday) ->
                        if (dIdx < dotIds.size) {
                            val dotId = dotIds[dIdx]
                            val log = allLogs.find { it.habitId == habit.id && it.date == dateStr }
                            val isEligible = habit.isEligibleOnDate(dateStr)

                            val dotDrawable = when {
                                log?.status == "DONE" -> R.drawable.widget_dot_done
                                log?.status == "PARTIAL" -> R.drawable.widget_dot_partial
                                log?.status == "SKIPPED" -> R.drawable.widget_dot_missed
                                !isEligible -> R.drawable.widget_dot_empty
                                !isToday -> R.drawable.widget_dot_missed
                                else -> R.drawable.widget_dot_empty
                            }
                            rowView.setImageViewResource(dotId, dotDrawable)
                        }
                    }

                    views.addView(R.id.widget_habits_container, rowView)
                }

                if (habits.isEmpty()) {
                    views.setTextViewText(
                        R.id.widget_more_text,
                        "Nenhum hábito no widget • Toque para configurar"
                    )
                } else {
                    views.setTextViewText(
                        R.id.widget_more_text,
                        "${habits.size} hábito${if (habits.size > 1) "s" else ""} no widget • 🟢 Feito 🟠 Parcial 🔴 Não feito"
                    )
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (t: Throwable) {
                android.util.Log.e("RotinaUpWidget", "Falha ao sincronizar dados do widget", t)
                try {
                    val syncIntent = Intent(context, RotinaUpWidgetProvider::class.java).apply {
                        action = ACTION_SYNC_WIDGET
                    }
                    val syncPendingIntent = PendingIntent.getBroadcast(
                        context,
                        appWidgetId,
                        syncIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.removeAllViews(R.id.widget_habits_container)
                    views.setTextViewText(
                        R.id.widget_more_text,
                        "⚠️ Toque para sincronizar seus hábitos"
                    )
                    views.setOnClickPendingIntent(R.id.widget_more_text, syncPendingIntent)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (_: Throwable) {}
            }
        }

        const val ACTION_SYNC_WIDGET = "com.example.ACTION_SYNC_WIDGET"

        fun updateAllWidgets(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val thisWidget = ComponentName(context, RotinaUpWidgetProvider::class.java)
                    val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                    for (id in allWidgetIds) {
                        updateAppWidgetSync(context, appWidgetManager, id)
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("RotinaUpWidget", "Erro ao disparar updateAllWidgets", t)
                }
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                val myProvider = ComponentName(context, RotinaUpWidgetProvider::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val successCallback = PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent(context, RotinaUpWidgetProvider::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    return appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                }
            }
            return false
        }
    }
}
