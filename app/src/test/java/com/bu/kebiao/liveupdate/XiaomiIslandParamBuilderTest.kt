package com.bu.kebiao.liveupdate

import com.bu.kebiao.domain.model.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class XiaomiIslandParamBuilderTest {
    private val course = Course(
        name = "大学英语",
        teacher = "李老师",
        location = "语音室 402",
        dayOfWeek = 2,
        startSection = 3,
        endSection = 4
    )

    @Test
    fun hiddenStateHasNoIslandParam() {
        val text = CourseLiveUpdateFormatter.format(CourseLiveUpdateState.Hidden)

        assertEquals(null, XiaomiIslandParamBuilder.build(CourseLiveUpdateState.Hidden, text))
    }

    @Test
    fun upcomingStateBuildsMiuiFocusParam() {
        val state = CourseLiveUpdateState.Upcoming(
            course = course,
            startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
            endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
            minutesUntilStart = 12,
            startTimeText = "10:00"
        )
        val json = XiaomiIslandParamBuilder.build(state, CourseLiveUpdateFormatter.format(state)).orEmpty()

        assertTrue(json.contains("\"param_v2\""))
        assertTrue(json.contains("\"business\":\"course_schedule\""))
        assertTrue(json.contains("\"enableFloat\":true"))
        assertTrue(json.contains("\"updatable\":true"))
        assertTrue(json.contains("\"ticker\":\"准备上课\""))
        assertTrue(json.contains("\"title\":\"大学英语\""))
        assertTrue(json.contains("\"content\":\"10:00开始 · 还有12分钟\""))
    }

    @Test
    fun inClassStateIncludesSectionProgress() {
        val state = CourseLiveUpdateState.InClass(
            course = course,
            startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
            endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
            sectionNumber = 3,
            sectionEndTimeText = "10:45",
            minutesUntilSectionEnd = 20,
            progressPercent = 80
        )
        val json = XiaomiIslandParamBuilder.build(state, CourseLiveUpdateFormatter.format(state)).orEmpty()

        assertTrue(json.contains("\"ticker\":\"正在上课\""))
        assertTrue(json.contains("\"content\":\"第3节 · 还有20分钟下课\""))
        assertTrue(json.contains("\"progress\":80"))
        assertTrue(json.contains("\"progressMax\":100"))
    }

    @Test
    fun sectionBreakStateDoesNotPretendToBeInClassProgress() {
        val state = CourseLiveUpdateState.SectionBreak(
            course = course,
            nextSectionNumber = 4,
            nextSectionStartsAt = LocalDateTime.of(2026, 6, 30, 10, 55),
            nextSectionStartTimeText = "10:55",
            minutesUntilNextSection = 6
        )
        val json = XiaomiIslandParamBuilder.build(state, CourseLiveUpdateFormatter.format(state)).orEmpty()

        assertTrue(json.contains("\"ticker\":\"课间休息\""))
        assertTrue(json.contains("\"content\":\"第4节10:55开始 · 还有6分钟\""))
        assertFalse(json.contains("\"progressMax\""))
    }

    @Test
    fun jsonEscapesCourseFields() {
        val state = CourseLiveUpdateState.Upcoming(
            course = course.copy(name = "英语\"听说", location = "A\\B"),
            startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
            endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
            minutesUntilStart = 1,
            startTimeText = "10:00"
        )
        val json = XiaomiIslandParamBuilder.build(state, CourseLiveUpdateFormatter.format(state)).orEmpty()

        assertTrue(json.contains("英语\\\"听说"))
        assertTrue(json.contains("A\\\\B"))
    }
}
