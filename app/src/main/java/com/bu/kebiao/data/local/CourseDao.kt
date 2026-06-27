package com.bu.kebiao.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startSection")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :dayOfWeek ORDER BY startSection")
    fun getCoursesByDay(dayOfWeek: Int): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE ((weeks != '' AND (',' || weeks || ',') LIKE '%,' || :week || ',%') OR (weeks = '' AND startWeek <= :week AND endWeek >= :week AND (weekType = 0 OR (weekType = 1 AND :week % 2 = 1) OR (weekType = 2 AND :week % 2 = 0)))) ORDER BY dayOfWeek, startSection")
    fun getCoursesByWeek(week: Int): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :dayOfWeek AND ((weeks != '' AND (',' || weeks || ',') LIKE '%,' || :week || ',%') OR (weeks = '' AND startWeek <= :week AND endWeek >= :week AND (weekType = 0 OR (weekType = 1 AND :week % 2 = 1) OR (weekType = 2 AND :week % 2 = 0)))) ORDER BY startSection")
    fun getCoursesByDayAndWeek(dayOfWeek: Int, week: Int): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: CourseEntity): Long

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("SELECT COUNT(*) FROM courses")
    fun getCourseCount(): Flow<Int>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Query("UPDATE courses SET colorIndex = :colorIndex WHERE name = :courseName")
    suspend fun updateColorByCourseName(courseName: String, colorIndex: Int)
}
