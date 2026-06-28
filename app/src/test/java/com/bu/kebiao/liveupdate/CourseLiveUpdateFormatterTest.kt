package com.bu.kebiao.liveupdate

import com.bu.kebiao.domain.model.Course
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class CourseLiveUpdateFormatterTest {
    private val course = Course(
        name = "大学英语",
        location = "语音室 402",
        dayOfWeek = 2,
        startSection = 3,
        endSection = 4
    )

    @Test
    fun formatsUpcomingCountdown() {
        val text = CourseLiveUpdateFormatter.format(
            CourseLiveUpdateState.Upcoming(
                course = course,
                startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
                endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
                minutesUntilStart = 12,
                startTimeText = "10:00"
            )
        )

        assertEquals("12分钟后上课", text.title)
        assertEquals("大学英语 · 语音室 402 · 10:00开始", text.content)
    }

    @Test
    fun formatsInClassState() {
        val text = CourseLiveUpdateFormatter.format(
            CourseLiveUpdateState.InClass(
                course = course,
                startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
                endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
                endTimeText = "11:40"
            )
        )

        assertEquals("正在上课", text.title)
        assertEquals("大学英语 · 语音室 402 · 11:40下课", text.content)
    }

    @Test
    fun omitsBlankLocation() {
        val text = CourseLiveUpdateFormatter.format(
            CourseLiveUpdateState.Upcoming(
                course = course.copy(location = ""),
                startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
                endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
                minutesUntilStart = 3,
                startTimeText = "10:00"
            )
        )

        assertEquals("大学英语 · 10:00开始", text.content)
    }
}
