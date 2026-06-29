package com.bu.kebiao.domain.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class AcademicWeekResolverTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun resolvesCurrentWeekFromSemesterStartInsteadOfViewingWeek() {
        val semesterStart = LocalDate.of(2026, 3, 2)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val week = AcademicWeekResolver.resolveCurrentWeek(
            viewingWeek = 3,
            totalWeeks = 20,
            semesterStartDateMillis = semesterStart,
            today = LocalDate.of(2026, 6, 29),
            zoneId = zoneId
        )

        assertEquals(18, week)
    }

    @Test
    fun fallsBackToViewingWeekWhenSemesterStartIsMissing() {
        val week = AcademicWeekResolver.resolveCurrentWeek(
            viewingWeek = 7,
            totalWeeks = 20,
            semesterStartDateMillis = 0L,
            today = LocalDate.of(2026, 6, 29),
            zoneId = zoneId
        )

        assertEquals(7, week)
    }

    @Test
    fun formatsViewingWeekLabelWithCurrentWeekMarker() {
        assertEquals("第9周（本周）", AcademicWeekResolver.formatViewingWeekLabel(9, 9))
        assertEquals("第8周", AcademicWeekResolver.formatViewingWeekLabel(8, 9))
    }
}
