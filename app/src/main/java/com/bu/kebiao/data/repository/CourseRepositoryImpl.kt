package com.bu.kebiao.data.repository

import com.bu.kebiao.data.local.CourseDao
import com.bu.kebiao.data.local.CourseEntity
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.model.WeekType
import com.bu.kebiao.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CourseRepositoryImpl(
    private val courseDao: CourseDao
) : CourseRepository {

    override fun getAllCourses(): Flow<List<Course>> =
        courseDao.getAllCourses().map { entities -> entities.map { it.toDomain() } }

    override fun getCoursesByDay(dayOfWeek: Int): Flow<List<Course>> =
        courseDao.getCoursesByDay(dayOfWeek).map { entities -> entities.map { it.toDomain() } }

    override fun getCoursesByWeek(week: Int): Flow<List<Course>> =
        courseDao.getCoursesByWeek(week).map { entities -> entities.map { it.toDomain() } }

    override fun getCoursesByDayAndWeek(dayOfWeek: Int, week: Int): Flow<List<Course>> =
        courseDao.getCoursesByDayAndWeek(dayOfWeek, week).map { entities -> entities.map { it.toDomain() } }

    override fun getCourseCount(): Flow<Int> = courseDao.getCourseCount()

    override suspend fun getCourseById(id: Long): Course? =
        courseDao.getCourseById(id)?.toDomain()

    override suspend fun insertCourse(course: Course): Long =
        courseDao.insert(course.toEntity())

    override suspend fun insertCourses(courses: List<Course>) {
        courseDao.insertCourses(courses.map { it.toEntity() })
    }

    override suspend fun updateCourse(course: Course) {
        courseDao.update(course.toEntity())
    }

    override suspend fun updateColorByCourseName(courseName: String, colorIndex: Int) {
        courseDao.updateColorByCourseName(courseName, colorIndex)
    }

    override suspend fun deleteCourse(course: Course) {
        courseDao.delete(course.toEntity())
    }

    override suspend fun deleteBySource(source: String) {
        courseDao.deleteBySource(source)
    }

    private fun CourseEntity.toDomain() = Course(
        id = id,
        name = name,
        teacher = teacher,
        location = location,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        endSection = endSection,
        startWeek = startWeek,
        endWeek = endWeek,
        weekType = WeekType.fromValue(weekType),
        weeks = weeks.toWeekList(),
        colorIndex = colorIndex,
        source = source
    )

    private fun Course.toEntity() = CourseEntity(
        id = id,
        name = name,
        teacher = teacher,
        location = location,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        endSection = endSection,
        startWeek = startWeek,
        endWeek = endWeek,
        weekType = weekType.value,
        weeks = weeks.joinToString(","),
        colorIndex = colorIndex,
        source = source
    )

    private fun String.toWeekList(): List<Int> =
        split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .distinct()
            .sorted()
}
