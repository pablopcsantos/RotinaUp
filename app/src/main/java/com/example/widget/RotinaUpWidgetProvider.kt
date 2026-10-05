package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
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

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
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
            views.setTextViewText(R.id.widget_date, DateUtils.getNowTimeStr())
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

            // Asynchronously load habits and logs to populate the 7-day adherence dots
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val allActiveHabits = db.habitDao().getAllActiveHabits().firstOrNull() ?: emptyList()
                    val habits = allActiveHabits.filter { it.showInWidget }
                    val allLogs = db.habitDao().getAllLogs().firstOrNull() ?: emptyList()

                    val habitRows = listOf(
                        Triple(R.id.h1_row, R.id.h1_name, R.id.h1_time) to listOf(
                            R.id.h1_d1, R.id.h1_d2, R.id.h1_d3, R.id.h1_d4, R.id.h1_d5, R.id.h1_d6, R.id.h1_d7
                        ),
                        Triple(R.id.h2_row, R.id.h2_name, R.id.h2_time) to listOf(
                            R.id.h2_d1, R.id.h2_d2, R.id.h2_d3, R.id.h2_d4, R.id.h2_d5, R.id.h2_d6, R.id.h2_d7
                        ),
                        Triple(R.id.h3_row, R.id.h3_name, R.id.h3_time) to listOf(
                            R.id.h3_d1, R.id.h3_d2, R.id.h3_d3, R.id.h3_d4, R.id.h3_d5, R.id.h3_d6, R.id.h3_d7
                        )
                    )

                    habitRows.forEachIndexed { index, (viewsInfo, dotIds) ->
                        val (rowId, nameId, timeId) = viewsInfo
                        if (index < habits.size) {
                            val habit = habits[index]
                            views.setViewVisibility(rowId, View.VISIBLE)
                            views.setTextViewText(nameId, habit.name)
                            views.setTextViewText(timeId, habit.targetTime)

                            // Populate 7-day adherence dots
                            last7Days.forEachIndexed { dIdx, (dateStr, _, isToday) ->
                                if (dIdx < dotIds.size) {
                                    val dotId = dotIds[dIdx]
                                    val log = allLogs.find { it.habitId == habit.id && it.date == dateStr }
                                    val isoDay = DateUtils.getIsoDayOfWeek(dateStr)
                                    val isScheduled = habit.daysOfWeek.split(",")
                                        .mapNotNull { it.trim().toIntOrNull() }
                                        .contains(isoDay)

                                    val dotDrawable = when {
                                        log?.status == "DONE" -> R.drawable.widget_dot_done
                                        log?.status == "PARTIAL" -> R.drawable.widget_dot_partial
                                        log?.status == "SKIPPED" -> R.drawable.widget_dot_missed
                                        !isScheduled -> R.drawable.widget_dot_empty
                                        !isToday -> R.drawable.widget_dot_missed
                                        else -> R.drawable.widget_dot_empty
                                    }
                                    views.setImageViewResource(dotId, dotDrawable)
                                }
                            }
                        } else {
                            views.setViewVisibility(rowId, View.GONE)
                        }
                    }

                    if (habits.isEmpty()) {
                        views.setTextViewText(
                            R.id.widget_more_text,
                            "Nenhum hábito no widget • Toque para configurar"
                        )
                    } else if (habits.size > 3) {
                        views.setTextViewText(
                            R.id.widget_more_text,
                            "+ ${habits.size - 3} outros no widget • Toque para abrir"
                        )
                    } else {
                        views.setTextViewText(
                            R.id.widget_more_text,
                            "🟢 Feito  🟠 Parcial  🔴 Não feito"
                        )
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (_: Throwable) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, RotinaUpWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
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
