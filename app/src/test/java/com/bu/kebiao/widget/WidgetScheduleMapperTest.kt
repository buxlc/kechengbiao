package com.bu.kebiao.widget

import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetScheduleMapperTest {
    private val classTimes = listOf(
        ClassTime(1, "08:00", "08:45"),
        ClassTime(2, "08:55", "09:40"),
        ClassTime(3, "10:00", "10:45"),
        ClassTime(4, "10:55", "11:40"),
        ClassTime(5, "14:00", "14:45"),
        ClassTime(6, "14:55", "15:40")
    )

    @Test
    fun largeSnapshotIncludesOnlyTwoCourseTilesForWideWidgetCapacity() {
        val snapshot = WidgetScheduleMapper.build(
            courses = listOf(
                Course(name = "大学物理", teacher = "王老师", location = "电教202", dayOfWeek = 2, startSection = 1, endSection = 1),
                Course(name = "军事理论", teacher = "李老师", location = "A201", dayOfWeek = 2, startSection = 2, endSection = 2),
                Course(name = "英语口语", teacher = "张老师", location = "B103", dayOfWeek = 2, startSection = 3, endSection = 3),
                Course(name = "软件工程实践", teacher = "赵老师", location = "实训楼", dayOfWeek = 2, startSection = 4, endSection = 4)
            ),
            classTimes = classTimes,
            todayDayOfWeek = 2
        )

        assertEquals("今天", snapshot.today.label)
        assertEquals("共4节课", snapshot.today.countText)
        assertEquals(2, snapshot.today.tiles.size)
        assertEquals("大学物理", snapshot.today.tiles[0].title)
        assertEquals("军事理论", snapshot.today.tiles[1].title)
    }

    @Test
    fun smallSnapshotKeepsAllTodayCoursesForScrolling() {
        val snapshot = WidgetScheduleMapper.build(
            courses = listOf(
                Course(name = "Physics", teacher = "Wang", location = "Room 202", dayOfWeek = 2, startSection = 1, endSection = 1),
                Course(name = "Military Theory", teacher = "Li", location = "Room 301", dayOfWeek = 2, startSection = 2, endSection = 2),
                Course(name = "English", teacher = "Zhang", location = "Room 401", dayOfWeek = 2, startSection = 3, endSection = 3)
            ),
            classTimes = classTimes,
            todayDayOfWeek = 2
        )

        assertEquals("周二", snapshot.small.label)
        assertEquals("共3节课", snapshot.small.countText)
        assertEquals(3, snapshot.small.courses.size)
        assertEquals("Military Theory", snapshot.small.courses[1].courseName)
        assertEquals("08:55 - 09:40", snapshot.small.courses[1].detailLine)
        assertEquals("Room 301", snapshot.small.courses[1].extraLine)
    }

    @Test
    fun smallSnapshotLeavesCourseListEmptyWhenNoCourses() {
        val snapshot = WidgetScheduleMapper.build(
            courses = emptyList(),
            classTimes = classTimes,
            todayDayOfWeek = 7
        )

        assertEquals("周日", snapshot.small.label)
        assertEquals("共0节课", snapshot.small.countText)
        assertTrue(snapshot.small.courses.isEmpty())
    }
}
