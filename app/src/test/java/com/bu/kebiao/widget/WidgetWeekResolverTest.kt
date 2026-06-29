package com.bu.kebiao.widget

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetWeekResolverTest {
    private val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun resolvesRealCurrentWeekFromSemesterStartInsteadOfSelectedWeek() {
        val semesterStart = LocalDate.of(2026, 3, 2)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val week = WidgetWeekResolver.resolve(
            selectedWeek = 3,
            totalWeeks = 20,
            semesterStartDateMillis = semesterStart,
            now = LocalDateTime.of(2026, 6, 29, 9, 0),
            zoneId = zoneId
        )

        assertEquals(18, week)
    }

    @Test
    fun fallsBackToSelectedWeekWhenSemesterStartIsMissing() {
        val week = WidgetWeekResolver.resolve(
            selectedWeek = 7,
            totalWeeks = 20,
            semesterStartDateMillis = 0L,
            now = LocalDateTime.of(2026, 6, 29, 9, 0),
            zoneId = zoneId
        )

        assertEquals(7, week)
    }
}
