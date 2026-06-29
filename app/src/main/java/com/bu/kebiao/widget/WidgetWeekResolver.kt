package com.bu.kebiao.widget

import com.bu.kebiao.domain.model.AcademicWeekResolver
import java.time.LocalDateTime
import java.time.ZoneId

object WidgetWeekResolver {
    fun resolve(
        selectedWeek: Int,
        totalWeeks: Int,
        semesterStartDateMillis: Long,
        now: LocalDateTime = LocalDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int = AcademicWeekResolver.resolveCurrentWeek(
        viewingWeek = selectedWeek,
        totalWeeks = totalWeeks,
        semesterStartDateMillis = semesterStartDateMillis,
        today = now.toLocalDate(),
        zoneId = zoneId
    )
}
