package com.bu.kebiao.liveupdate

import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CourseLiveUpdateCalculatorTest {
    private val monday: LocalDate = LocalDate.of(2026, 6, 29)
    private val course = Course(
        name = "高等数学",
        location = "教学楼 301",
        teacher = "张三",
        dayOfWeek = 1,
        startSection = 1,
        endSection = 2,
        weeks = listOf(1)
    )
    private val nextCourse = Course(
        name = "大学英语",
        location = "语音室 402",
        dayOfWeek = 1,
        startSection = 3,
        endSection = 4,
        weeks = listOf(1)
    )
    private val classTimes = listOf(
        ClassTime(1, "08:00", "08:45"),
        ClassTime(2, "08:55", "09:40"),
        ClassTime(3, "10:00", "10:45"),
        ClassTime(4, "10:55", "11:40")
    )

    @Test
    fun courseStartingInTwentyOneMinutesIsHidden() {
        val state = calculateAt(7, 39)

        assertTrue(state is CourseLiveUpdateState.Hidden)
    }

    @Test
    fun courseStartingInTwentyMinutesIsUpcoming() {
        val state = calculateAt(7, 40)

        state as CourseLiveUpdateState.Upcoming
        assertEquals("高等数学", state.course.name)
        assertEquals(20, state.minutesUntilStart)
        assertEquals("08:00", state.startTimeText)
    }

    @Test
    fun courseStartingInFiveMinutesIsUpcoming() {
        val state = calculateAt(7, 55)

        state as CourseLiveUpdateState.Upcoming
        assertEquals(5, state.minutesUntilStart)
    }

    @Test
    fun startedCourseIsInClass() {
        val state = calculateAt(8, 10)

        state as CourseLiveUpdateState.InClass
        assertEquals("高等数学", state.course.name)
        assertEquals(1, state.sectionNumber)
        assertEquals("08:45", state.sectionEndTimeText)
        assertEquals(35, state.minutesUntilSectionEnd)
        assertEquals(22, state.progressPercent)
    }

    @Test
    fun breakBetweenSectionsShowsSectionBreak() {
        val state = calculateAt(8, 50)

        state as CourseLiveUpdateState.SectionBreak
        assertEquals("高等数学", state.course.name)
        assertEquals(2, state.nextSectionNumber)
        assertEquals("08:55", state.nextSectionStartTimeText)
        assertEquals(5, state.minutesUntilNextSection)
    }

    @Test
    fun secondSectionCountsDownToItsOwnEnd() {
        val state = calculateAt(9, 10)

        state as CourseLiveUpdateState.InClass
        assertEquals(2, state.sectionNumber)
        assertEquals("09:40", state.sectionEndTimeText)
        assertEquals(30, state.minutesUntilSectionEnd)
        assertEquals(33, state.progressPercent)
    }

    @Test
    fun endedCourseMovesToNextUpcomingCourseInsideWindow() {
        val state = CourseLiveUpdateCalculator.calculate(
            courses = listOf(course, nextCourse),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, LocalTime.of(9, 41))
        )

        state as CourseLiveUpdateState.Upcoming
        assertEquals("大学英语", state.course.name)
        assertEquals(19, state.minutesUntilStart)
    }

    @Test
    fun endedCourseIsHiddenWhenNoNextCourseIsInsideWindow() {
        val state = calculateAt(9, 41)

        assertTrue(state is CourseLiveUpdateState.Hidden)
    }

    @Test
    fun missingClassTimeIsSkipped() {
        val state = CourseLiveUpdateCalculator.calculate(
            courses = listOf(course.copy(startSection = 11, endSection = 12)),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, LocalTime.of(7, 50))
        )

        assertTrue(state is CourseLiveUpdateState.Hidden)
    }

    @Test
    fun inactiveWeekIsSkipped() {
        val state = CourseLiveUpdateCalculator.calculate(
            courses = listOf(course.copy(weeks = listOf(2))),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, LocalTime.of(7, 50))
        )

        assertTrue(state is CourseLiveUpdateState.Hidden)
    }

    @Test
    fun nextCheckForHiddenStateIsTwentyMinutesBeforeNextCourse() {
        val nextCheck = CourseLiveUpdateCalculator.nextCheckTime(
            courses = listOf(course),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, LocalTime.of(7, 0))
        )

        assertEquals(LocalDateTime.of(monday, LocalTime.of(7, 40)), nextCheck)
    }

    @Test
    fun nextCheckForUpcomingStateIsNextMinute() {
        val nextCheck = CourseLiveUpdateCalculator.nextCheckTime(
            courses = listOf(course),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, LocalTime.of(7, 55))
        )

        assertEquals(LocalDateTime.of(monday, LocalTime.of(7, 56)), nextCheck)
    }

    @Test
    fun nextCheckForInClassStateIsNextMinute() {
        val nextCheck = CourseLiveUpdateCalculator.nextCheckTime(
            courses = listOf(course),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, LocalTime.of(8, 10))
        )

        assertEquals(LocalDateTime.of(monday, LocalTime.of(8, 11)), nextCheck)
    }

    @Test
    fun nextCheckForSectionBreakIsNextMinute() {
        val nextCheck = CourseLiveUpdateCalculator.nextCheckTime(
            courses = listOf(course),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, LocalTime.of(8, 50))
        )

        assertEquals(LocalDateTime.of(monday, LocalTime.of(8, 51)), nextCheck)
    }

    private fun calculateAt(hour: Int, minute: Int): CourseLiveUpdateState =
        CourseLiveUpdateCalculator.calculate(
            courses = listOf(course),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, LocalTime.of(hour, minute))
        )
}
