package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private fun getIsoFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    private fun getTimeFormat() = SimpleDateFormat("HH:mm", Locale.ROOT)

    fun getTodayDateString(offsetDays: Int = 0): String {
        val calendar = Calendar.getInstance()
        if (offsetDays != 0) {
            calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
        }
        return getIsoFormat().format(calendar.time)
    }

    fun getNowTimeStr(): String {
        return getTimeFormat().format(Date())
    }

    fun parseIso(dateStr: String): Date? {
        return try {
            getIsoFormat().parse(dateStr)
        } catch (_: Exception) {
            null
        }
    }

    fun formatWidgetDate(dateStr: String): String {
        return try {
            val date = parseIso(dateStr) ?: return dateStr
            val calendar = Calendar.getInstance().apply { time = date }
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Dom"
                Calendar.MONDAY -> "Seg"
                Calendar.TUESDAY -> "Ter"
                Calendar.WEDNESDAY -> "Qua"
                Calendar.THURSDAY -> "Qui"
                Calendar.FRIDAY -> "Sex"
                Calendar.SATURDAY -> "Sáb"
                else -> ""
            }
            "$dayOfWeek, %02d/%02d".format(day, month)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun formatFriendlyDate(dateStr: String): String {
        return try {
            val date = parseIso(dateStr) ?: return dateStr
            val calendar = Calendar.getInstance().apply { time = date }
            val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Domingo"
                Calendar.MONDAY -> "Segunda-feira"
                Calendar.TUESDAY -> "Terça-feira"
                Calendar.WEDNESDAY -> "Quarta-feira"
                Calendar.THURSDAY -> "Quinta-feira"
                Calendar.FRIDAY -> "Sexta-feira"
                Calendar.SATURDAY -> "Sábado"
                else -> ""
            }
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = when (calendar.get(Calendar.MONTH)) {
                Calendar.JANUARY -> "janeiro"
                Calendar.FEBRUARY -> "fevereiro"
                Calendar.MARCH -> "março"
                Calendar.APRIL -> "abril"
                Calendar.MAY -> "maio"
                Calendar.JUNE -> "junho"
                Calendar.JULY -> "julho"
                Calendar.AUGUST -> "agosto"
                Calendar.SEPTEMBER -> "setembro"
                Calendar.OCTOBER -> "outubro"
                Calendar.NOVEMBER -> "novembro"
                Calendar.DECEMBER -> "dezembro"
                else -> ""
            }
            "$dayOfWeek, $day de $month"
        } catch (_: Exception) {
            dateStr
        }
    }

    fun getDayInitial(dateStr: String): String {
        return try {
            val date = parseIso(dateStr) ?: return "D"
            val calendar = Calendar.getInstance().apply { time = date }
            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "D"
                Calendar.MONDAY -> "S"
                Calendar.TUESDAY -> "T"
                Calendar.WEDNESDAY -> "Q"
                Calendar.THURSDAY -> "Q"
                Calendar.FRIDAY -> "S"
                Calendar.SATURDAY -> "S"
                else -> "D"
            }
        } catch (_: Exception) {
            "D"
        }
    }

    fun getIsoDayOfWeek(dateStr: String): Int {
        return try {
            val date = parseIso(dateStr) ?: return 1
            val calendar = Calendar.getInstance().apply { time = date }
            val day = calendar.get(Calendar.DAY_OF_WEEK)
            if (day == Calendar.SUNDAY) 7 else day - 1
        } catch (_: Exception) {
            1
        }
    }

    data class DayInfo(val dateStr: String, val dayInitial: String, val isToday: Boolean)

    fun getLast7Days(): List<DayInfo> {
        return (6 downTo 0).map { offset ->
            val dateStr = getTodayDateString(-offset)
            DayInfo(
                dateStr = dateStr,
                dayInitial = getDayInitial(dateStr),
                isToday = offset == 0
            )
        }
    }
}
