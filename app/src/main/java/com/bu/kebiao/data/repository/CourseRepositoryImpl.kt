package com.bu.kebiao.data.repository

import com.bu.kebiao.data.local.CourseDao
import com.bu.kebiao.data.local.CourseEntity
import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.model.WeekType
import com.bu.kebiao.domain.repository.CourseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class CourseRepositoryImpl(
    private val courseDao: CourseDao,
    private val userPreferences: UserPreferences
) : CourseRepository {

    private val currentSemesterIdFlow = userPreferences.preferencesFlow.map { it.currentSemesterId.ifBlank { "default" } }

    override fun getAllCourses(): Flow<List<Course>> =
        currentSemesterIdFlow.flatMapLatest { semesterId ->
            courseDao.getAllCourses(semesterId).map { entities -> entities.map { it.toDomain() } }
        }

    override fun getCoursesByDay(dayOfWeek: Int): Flow<List<Course>> =
        currentSemesterIdFlow.flatMapLatest { semesterId ->
            courseDao.getCoursesByDay(semesterId, dayOfWeek).map { entities -> entities.map { it.toDomain() } }
        }

    override fun getCoursesByWeek(week: Int): Flow<List<Course>> =
        currentSemesterIdFlow.flatMapLatest { semesterId ->
            courseDao.getCoursesByWeek(semesterId, week).map { entities -> entities.map { it.toDomain() } }
        }

    override fun getCoursesByDayAndWeek(dayOfWeek: Int, week: Int): Flow<List<Course>> =
        currentSemesterIdFlow.flatMapLatest { semesterId ->
            courseDao.getCoursesByDayAndWeek(semesterId, dayOfWeek, week).map { entities -> entities.map { it.toDomain() } }
        }

    override fun getCourseCount(): Flow<Int> =
        currentSemesterIdFlow.flatMapLatest { semesterId -> courseDao.getCourseCount(semesterId) }

    override suspend fun getCourseById(id: Long): Course? =
        courseDao.getCourseById(id)?.toDomain()

    override suspend fun insertCourse(course: Course): Long =
        courseDao.insert(course.withCurrentSemester().toEntity())

    override suspend fun insertCourses(courses: List<Course>) {
        val semesterId = currentSemesterId()
        insertCoursesIntoSemester(courses, semesterId)
    }

    override suspend fun insertCoursesIntoSemester(courses: List<Course>, semesterId: String) {
        courseDao.insertCourses(CourseSemesterAssigner.assign(courses, semesterId).map { it.toEntity() })
    }

    override suspend fun updateCourse(course: Course) {
        val semesterId = courseDao.getCourseById(course.id)?.semesterId ?: currentSemesterId()
        courseDao.update(course.copy(semesterId = semesterId).toEntity())
    }

    override suspend fun updateColorByCourseName(courseName: String, colorIndex: Int) {
        courseDao.updateColorByCourseName(currentSemesterId(), courseName, colorIndex)
    }

    override suspend fun deleteCourse(course: Course) {
        courseDao.delete(course.toEntity())
    }

    override suspend fun deleteBySource(source: String) {
        courseDao.deleteBySource(currentSemesterId(), source)
    }

    override suspend fun deleteCurrentSemesterCourses() {
        courseDao.deleteBySemester(currentSemesterId())
    }

    override suspend fun deleteSemesterCourses(semesterId: String) {
        courseDao.deleteBySemester(semesterId)
    }

    override fun getAllCoursesBySemester(semesterId: String): Flow<List<Course>> =
        courseDao.getAllCourses(semesterId.ifBlank { "default" }).map { entities -> entities.map { it.toDomain() } }

    private suspend fun currentSemesterId(): String =
        userPreferences.preferencesFlow.first().currentSemesterId.ifBlank { "default" }

    private suspend fun Course.withCurrentSemester(): Course =
        copy(semesterId = currentSemesterId())

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
        source = source,
        semesterId = semesterId
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
        source = source,
        semesterId = semesterId
    )

    private fun String.toWeekList(): List<Int> =
        split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .distinct()
            .sorted()
}
