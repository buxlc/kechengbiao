package com.bu.kebiao.widget

import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.liveupdate.CourseLiveUpdateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime

class WidgetCourseMapperTest {
    private val earlyCourse = Course(
        name = "高等数学",
        teacher = "张三",
        location = "教学楼 301",
        dayOfWeek = 2,
        startSection = 1,
        endSection = 2
    )

    private val laterCourse = Course(
        name = "大学英语",
        teacher = "李老师",
        location = "语音室 402",
        dayOfWeek = 2,
        startSection = 3,
        endSection = 4
    )

    @Test
    fun upcomingStateUsesWarmAccentAndCountdown() {
        val snapshot = WidgetCourseMapper.buildSnapshot(
            CourseLiveUpdateState.Upcoming(
                course = laterCourse,
                startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
                endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
                minutesUntilStart = 12,
                startTimeText = "10:00"
            ),
            classTimes = listOf(ClassTime(1, "08:00", "08:45")),
            todayCourses = listOf(laterCourse),
            now = LocalDateTime.of(2026, 6, 30, 7, 50)
        )

        assertEquals("准备上课", snapshot.primary.stateLabel)
        assertEquals("大学英语", snapshot.primary.courseName)
        assertEquals("10:00开始", snapshot.primary.detailLine)
        assertEquals("语音室 402 · 李老师", snapshot.primary.extraLine)
        assertEquals(0xFFD4850F.toInt(), snapshot.primary.accentColor)
        assertNotNull(snapshot.secondary)
    }

    @Test
    fun inClassStateShowsProgressBarData() {
        val snapshot = WidgetCourseMapper.buildSnapshot(
            CourseLiveUpdateState.InClass(
                course = laterCourse,
                startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
                endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
                sectionNumber = 3,
                sectionEndTimeText = "10:45",
                minutesUntilSectionEnd = 20,
                progressPercent = 80
            ),
            classTimes = listOf(ClassTime(1, "08:00", "08:45")),
            todayCourses = listOf(laterCourse),
            now = LocalDateTime.of(2026, 6, 30, 8, 10)
        )

        assertEquals("正在上课", snapshot.primary.stateLabel)
        assertEquals("第3节", snapshot.primary.detailLine)
        assertEquals(80, snapshot.primary.progress)
        assertEquals(100, snapshot.primary.progressMax)
        assertEquals(0xFF2D8A56.toInt(), snapshot.primary.accentColor)
    }

    @Test
    fun hiddenStateShowsNextCourseBeforeClassesEnd() {
        val snapshot = WidgetCourseMapper.buildSnapshot(
            CourseLiveUpdateState.Hidden,
            classTimes = listOf(
                ClassTime(1, "08:00", "08:45"),
                ClassTime(2, "08:55", "09:40"),
                ClassTime(3, "10:00", "10:45"),
                ClassTime(4, "10:55", "11:40")
            ),
            todayCourses = listOf(earlyCourse, laterCourse),
            now = LocalDateTime.of(2026, 6, 30, 7, 30)
        )

        assertEquals("下一节课", snapshot.primary.stateLabel)
        assertEquals("高等数学", snapshot.primary.courseName)
        assertEquals("08:00 - 08:45", snapshot.primary.detailLine)
        assertEquals("教学楼 301 · 张三", snapshot.primary.extraLine)
    }

    @Test
    fun hiddenStateShowsNoCourseAfterLastClass() {
        val snapshot = WidgetCourseMapper.buildSnapshot(
            CourseLiveUpdateState.Hidden,
            classTimes = listOf(
                ClassTime(1, "08:00", "08:45"),
                ClassTime(2, "08:55", "09:40"),
                ClassTime(3, "10:00", "10:45"),
                ClassTime(4, "10:55", "11:40")
            ),
            todayCourses = listOf(earlyCourse, laterCourse),
            now = LocalDateTime.of(2026, 6, 30, 12, 10)
        )

        assertEquals("今日无课", snapshot.primary.stateLabel)
        assertEquals("Bu课表", snapshot.primary.courseName)
        assertEquals("今天已没有课程", snapshot.primary.detailLine)
    }

    @Test
    fun widgetContentProviderUsesCalculatorState() {
        val snapshot = WidgetContentProvider.buildSnapshot(
            courses = listOf(laterCourse),
            classTimes = listOf(ClassTime(1, "08:00", "08:45")),
            currentWeek = 1,
            now = LocalDateTime.of(2026, 6, 30, 7, 50)
        )

        assertEquals("准备上课", snapshot.primary.stateLabel)
        assertNotNull(snapshot.footer)
    }
}
