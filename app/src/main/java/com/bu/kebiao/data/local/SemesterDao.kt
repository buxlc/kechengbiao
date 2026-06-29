package com.bu.kebiao.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query(
        """
        SELECT semesters.id, semesters.name, semesters.startDate, semesters.totalWeeks, semesters.createdAt,
               COUNT(courses.id) AS courseCount
        FROM semesters
        LEFT JOIN courses ON courses.semesterId = semesters.id
        GROUP BY semesters.id
        ORDER BY semesters.createdAt DESC, semesters.name ASC
        """
    )
    fun observeSemestersWithCount(): Flow<List<SemesterWithCount>>

    @Query("SELECT * FROM semesters WHERE id = :id LIMIT 1")
    suspend fun getSemester(id: String): SemesterEntity?

    @Query("SELECT id FROM semesters ORDER BY createdAt DESC LIMIT 1")
    suspend fun getFirstSemesterId(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(semester: SemesterEntity)

    @Query("UPDATE semesters SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun delete(id: String)
}
