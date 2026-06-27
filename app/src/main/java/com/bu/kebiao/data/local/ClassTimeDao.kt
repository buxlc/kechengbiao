package com.bu.kebiao.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassTimeDao {
    @Query("SELECT * FROM class_times ORDER BY sectionNumber ASC")
    fun getAllClassTimes(): Flow<List<ClassTimeEntity>>

    @Query("SELECT * FROM class_times WHERE sectionNumber = :section")
    suspend fun getClassTime(section: Int): ClassTimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(classTimes: List<ClassTimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(classTime: ClassTimeEntity)

    @Update
    suspend fun update(classTime: ClassTimeEntity)
}
