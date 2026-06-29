package com.bu.kebiao.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WeekHeaderDatesTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun buildsWeekHeaderDatesFromSemesterStartAndCurrentWeek() {
        val semesterStart = LocalDate.of(2026, 5, 4).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val header = buildWeekHeaderDates(
            currentWeek = 9,
            semesterStartDateMillis = semesterStart,
            today = LocalDate.of(2026, 6, 28),
            zoneId = zoneId
        )

        assertEquals("6月", header.monthLabel)
        assertEquals(listOf(22, 23, 24, 25, 26, 27, 28), header.days.map { it.date.dayOfMonth })
        assertEquals(listOf("一", "二", "三", "四", "五", "六", "日"), header.days.map { it.weekdayLabel })
        assertTrue(header.days.last().isToday)
        assertFalse(header.days.first().isToday)
    }

    @Test
    fun fallsBackToCalendarWeekWhenSemesterStartIsMissing() {
        val header = buildWeekHeaderDates(
            currentWeek = 3,
            semesterStartDateMillis = 0L,
            today = LocalDate.of(2026, 6, 28),
            zoneId = zoneId
        )

        assertEquals("6月", header.monthLabel)
        assertEquals(listOf(22, 23, 24, 25, 26, 27, 28), header.days.map { it.date.dayOfMonth })
        assertTrue(header.days[6].isToday)
    }
}
