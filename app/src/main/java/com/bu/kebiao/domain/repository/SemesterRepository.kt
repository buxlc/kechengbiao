package com.bu.kebiao.domain.repository

import com.bu.kebiao.domain.model.Semester
import kotlinx.coroutines.flow.Flow

interface SemesterRepository {
    fun observeSemesters(): Flow<List<Semester>>
    suspend fun ensureDefaultSemester()
    suspend fun createSemester(name: String, startDate: Long = 0L, totalWeeks: Int = 20): String
    suspend fun renameSemester(id: String, name: String)
    suspend fun deleteSemester(id: String)
}
