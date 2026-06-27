package com.bu.kebiao.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseColorDao {
    @Query("SELECT * FROM course_name_colors")
    fun observeAll(): Flow<List<CourseColorEntity>>

    @Query("SELECT * FROM course_name_colors WHERE courseName = :courseName LIMIT 1")
    suspend fun getByCourseName(courseName: String): CourseColorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CourseColorEntity)
}
