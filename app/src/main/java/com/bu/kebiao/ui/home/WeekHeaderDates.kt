package com.bu.kebiao.ui.home

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class WeekHeaderDay(
    val date: LocalDate,
    val weekdayLabel: String,
    val isToday: Boolean
)

data class WeekHeaderDates(
    val monthLabel: String,
    val days: List<WeekHeaderDay>
)

fun buildWeekHeaderDates(
    currentWeek: Int,
    semesterStartDateMillis: Long,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): WeekHeaderDates {
    val baseWeekStart = if (semesterStartDateMillis > 0L) {
        Instant.ofEpochMilli(semesterStartDateMillis)
            .atZone(zoneId)
            .toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks((currentWeek - 1).coerceAtLeast(0).toLong())
    } else {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val days = (0..6).map { offset ->
        val date = baseWeekStart.plusDays(offset.toLong())
        WeekHeaderDay(
            date = date,
            weekdayLabel = getWeekdayShortLabel(date.dayOfWeek.value),
            isToday = date == today
        )
    }

    return WeekHeaderDates(
        monthLabel = "${baseWeekStart.monthValue}月",
        days = days
    )
}

private fun getWeekdayShortLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> ""
}
