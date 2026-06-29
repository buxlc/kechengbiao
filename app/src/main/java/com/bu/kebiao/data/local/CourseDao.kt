package com.bu.kebiao.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY dayOfWeek, startSection")
    fun getAllCourses(semesterId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId AND dayOfWeek = :dayOfWeek ORDER BY startSection")
    fun getCoursesByDay(semesterId: String, dayOfWeek: Int): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId AND ((weeks != '' AND (',' || weeks || ',') LIKE '%,' || :week || ',%') OR (weeks = '' AND startWeek <= :week AND endWeek >= :week AND (weekType = 0 OR (weekType = 1 AND :week % 2 = 1) OR (weekType = 2 AND :week % 2 = 0)))) ORDER BY dayOfWeek, startSection")
    fun getCoursesByWeek(semesterId: String, week: Int): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId AND dayOfWeek = :dayOfWeek AND ((weeks != '' AND (',' || weeks || ',') LIKE '%,' || :week || ',%') OR (weeks = '' AND startWeek <= :week AND endWeek >= :week AND (weekType = 0 OR (weekType = 1 AND :week % 2 = 1) OR (weekType = 2 AND :week % 2 = 0)))) ORDER BY startSection")
    fun getCoursesByDayAndWeek(semesterId: String, dayOfWeek: Int, week: Int): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: CourseEntity): Long

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses WHERE semesterId = :semesterId AND source = :source")
    suspend fun deleteBySource(semesterId: String, source: String)

    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteBySemester(semesterId: String)

    @Query("SELECT COUNT(*) FROM courses WHERE semesterId = :semesterId")
    fun getCourseCount(semesterId: String): Flow<Int>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Query("UPDATE courses SET colorIndex = :colorIndex WHERE semesterId = :semesterId AND name = :courseName")
    suspend fun updateColorByCourseName(semesterId: String, courseName: String, colorIndex: Int)
}
