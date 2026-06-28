package com.bu.kebiao.liveupdate

import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.model.WeekType
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object CourseLiveUpdateCalculator {
    private const val REMINDER_WINDOW_MINUTES = 20L
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun calculate(
        courses: List<Course>,
        classTimes: List<ClassTime>,
        currentWeek: Int,
        now: LocalDateTime
    ): CourseLiveUpdateState {
        val today = now.dayOfWeek.value
        val slots = courses
            .filter { it.dayOfWeek == today && it.isActiveInWeek(currentWeek) }
            .mapNotNull { it.toSlot(classTimes, now) }
            .sortedWith(compareBy<CourseSlot> { it.startsAt }.thenBy { it.endsAt }.thenBy { it.course.name })

        val active = slots
            .filter { !now.isBefore(it.startsAt) && now.isBefore(it.endsAt) }
            .minWithOrNull(compareBy<CourseSlot> { it.endsAt }.thenBy { it.startsAt })

        if (active != null) {
            val totalMinutes = Duration.between(active.startsAt, active.endsAt).toMinutes().coerceAtLeast(1)
            val elapsedMinutes = Duration.between(active.startsAt, now).toMinutes().coerceAtLeast(0)
            val minutesUntilEnd = Duration.between(now, active.endsAt).toMinutes().toInt().coerceAtLeast(0)
            return CourseLiveUpdateState.InClass(
                course = active.course,
                startsAt = active.startsAt,
                endsAt = active.endsAt,
                endTimeText = active.endsAt.toLocalTime().format(timeFormatter),
                minutesUntilEnd = minutesUntilEnd,
                progressPercent = ((elapsedMinutes * 100) / totalMinutes).toInt().coerceIn(0, 100)
            )
        }

        val upcoming = slots
            .filter { now.isBefore(it.startsAt) }
            .firstOrNull { Duration.between(now, it.startsAt).toMinutes() <= REMINDER_WINDOW_MINUTES }
            ?: return CourseLiveUpdateState.Hidden

        return CourseLiveUpdateState.Upcoming(
            course = upcoming.course,
            startsAt = upcoming.startsAt,
            endsAt = upcoming.endsAt,
            minutesUntilStart = Duration.between(now, upcoming.startsAt).toMinutes().toInt().coerceAtLeast(0),
            startTimeText = upcoming.startsAt.toLocalTime().format(timeFormatter)
        )
    }

    fun nextCheckTime(
        courses: List<Course>,
        classTimes: List<ClassTime>,
        currentWeek: Int,
        now: LocalDateTime
    ): LocalDateTime? {
        val state = calculate(courses, classTimes, currentWeek, now)
        if (state is CourseLiveUpdateState.Upcoming) {
            val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
            return minOf(nextMinute, state.startsAt)
        }
        if (state is CourseLiveUpdateState.InClass) {
            val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
            return minOf(nextMinute, state.endsAt)
        }

        val today = now.dayOfWeek.value
        return courses
            .filter { it.dayOfWeek == today && it.isActiveInWeek(currentWeek) }
            .mapNotNull { it.toSlot(classTimes, now) }
            .filter { now.isBefore(it.startsAt) }
            .map { it.startsAt.minusMinutes(REMINDER_WINDOW_MINUTES) }
            .filter { !it.isBefore(now) }
            .minOrNull()
    }

    private fun Course.toSlot(classTimes: List<ClassTime>, now: LocalDateTime): CourseSlot? {
        val timesBySection = classTimes.associateBy { it.sectionNumber }
        val start = timesBySection[startSection]?.startTime?.parseLocalTime() ?: return null
        val end = timesBySection[endSection]?.endTime?.parseLocalTime() ?: return null
        val date = now.toLocalDate()
        return CourseSlot(
            course = this,
            startsAt = LocalDateTime.of(date, start),
            endsAt = LocalDateTime.of(date, end)
        )
    }

    private fun String.parseLocalTime(): LocalTime? =
        try {
            LocalTime.parse(trim(), timeFormatter)
        } catch (_: DateTimeParseException) {
            null
        }

    private fun Course.isActiveInWeek(week: Int): Boolean {
        if (weeks.isNotEmpty()) return week in weeks
        if (week !in startWeek..endWeek) return false
        return when (weekType) {
            WeekType.ALL -> true
            WeekType.ODD -> week % 2 == 1
            WeekType.EVEN -> week % 2 == 0
        }
    }

    private data class CourseSlot(
        val course: Course,
        val startsAt: LocalDateTime,
        val endsAt: LocalDateTime
    )
}
