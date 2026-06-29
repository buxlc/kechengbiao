package com.bu.kebiao.data.repository

import com.bu.kebiao.domain.model.Course

internal object CourseSemesterAssigner {
    fun assign(courses: List<Course>, semesterId: String): List<Course> {
        val targetSemesterId = semesterId.ifBlank { "default" }
        return courses.map { it.copy(semesterId = targetSemesterId) }
    }
}
