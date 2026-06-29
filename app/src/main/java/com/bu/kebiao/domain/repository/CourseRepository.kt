package com.bu.kebiao.domain.repository

import com.bu.kebiao.domain.model.Course
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    fun getAllCourses(): Flow<List<Course>>
    fun getCoursesByDay(dayOfWeek: Int): Flow<List<Course>>
    fun getCoursesByWeek(week: Int): Flow<List<Course>>
    fun getCoursesByDayAndWeek(dayOfWeek: Int, week: Int): Flow<List<Course>>
    fun getCourseCount(): Flow<Int>
    suspend fun getCourseById(id: Long): Course?
    suspend fun insertCourse(course: Course): Long
    suspend fun insertCourses(courses: List<Course>)
    suspend fun insertCoursesIntoSemester(courses: List<Course>, semesterId: String)
    suspend fun updateCourse(course: Course)
    suspend fun updateColorByCourseName(courseName: String, colorIndex: Int)
    suspend fun deleteCourse(course: Course)
    suspend fun deleteBySource(source: String)
    suspend fun deleteCurrentSemesterCourses()
    suspend fun deleteSemesterCourses(semesterId: String)
    fun getAllCoursesBySemester(semesterId: String): Flow<List<Course>>
}
