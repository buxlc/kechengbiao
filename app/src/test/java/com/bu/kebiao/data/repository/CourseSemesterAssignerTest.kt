package com.bu.kebiao.data.repository

import com.bu.kebiao.domain.model.Course
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseSemesterAssignerTest {

    @Test
    fun assignsEveryImportedCourseToSelectedSemester() {
        val courses = listOf(
            Course(name = "生物化学", dayOfWeek = 1, startSection = 1, endSection = 2),
            Course(name = "药理学", dayOfWeek = 2, startSection = 3, endSection = 4, semesterId = "old")
        )

        val assigned = CourseSemesterAssigner.assign(courses, "spring_2026")

        assertEquals(listOf("spring_2026", "spring_2026"), assigned.map { it.semesterId })
    }

    @Test
    fun blankSemesterFallsBackToDefault() {
        val course = Course(name = "诊断学", dayOfWeek = 3, startSection = 5, endSection = 6)

        val assigned = CourseSemesterAssigner.assign(listOf(course), "")

        assertEquals("default", assigned.single().semesterId)
    }
}
