package com.bu.kebiao.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object AcademicWeekResolver {
    fun resolveCurrentWeek(
        viewingWeek: Int,
        totalWeeks: Int,
        semesterStartDateMillis: Long,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        val safeTotalWeeks = totalWeeks.coerceAtLeast(1)
        if (semesterStartDateMillis <= 0L) {
            return normalizeViewingWeek(viewingWeek, safeTotalWeeks)
        }

        val semesterStartDate = Instant.ofEpochMilli(semesterStartDateMillis)
            .atZone(zoneId)
            .toLocalDate()
        val daysFromStart = ChronoUnit.DAYS.between(semesterStartDate, today)
        val calculatedWeek = if (daysFromStart < 0L) {
            1
        } else {
            (daysFromStart / 7L + 1L).toInt()
        }
        return calculatedWeek.coerceIn(1, safeTotalWeeks)
    }

    fun normalizeViewingWeek(viewingWeek: Int, totalWeeks: Int): Int =
        viewingWeek.coerceIn(1, totalWeeks.coerceAtLeast(1))

    fun formatViewingWeekLabel(viewingWeek: Int, currentWeek: Int): String =
        if (viewingWeek == currentWeek) {
            "第${viewingWeek}周（本周）"
        } else {
            "第${viewingWeek}周"
        }
}
