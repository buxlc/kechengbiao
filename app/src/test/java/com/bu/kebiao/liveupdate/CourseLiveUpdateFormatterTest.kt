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

        assertEquals("准备上课", text.title)
        assertEquals("大学英语 · 10:00开始", text.content)
        assertEquals("课程：大学英语\n地点：语音室 402\n教师：未填写\n倒计时：还有12分钟上课", text.expandedText)
        assertEquals(0, text.progressMax)
        assertEquals(0, text.progress)
    }

    @Test
    fun formatsInClassState() {
        val text = CourseLiveUpdateFormatter.format(
            CourseLiveUpdateState.InClass(
                course = course,
                startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
                endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
                endTimeText = "11:40",
                minutesUntilEnd = 20,
                progressPercent = 80
            )
        )

        assertEquals("正在上课", text.title)
        assertEquals("大学英语 · 11:40下课", text.content)
        assertEquals("课程：大学英语\n地点：语音室 402\n教师：未填写\n课间倒计时：还有20分钟下课", text.expandedText)
        assertEquals(100, text.progressMax)
        assertEquals(80, text.progress)
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

        assertEquals("课程：大学英语\n地点：未填写\n教师：未填写\n倒计时：还有3分钟上课", text.expandedText)
    }
}
