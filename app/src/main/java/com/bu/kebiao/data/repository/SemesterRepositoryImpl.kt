package com.bu.kebiao.data.repository

import com.bu.kebiao.data.local.SemesterDao
import com.bu.kebiao.data.local.SemesterEntity
import com.bu.kebiao.data.local.SemesterWithCount
import com.bu.kebiao.domain.model.Semester
import com.bu.kebiao.domain.repository.SemesterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SemesterRepositoryImpl(
    private val semesterDao: SemesterDao
) : SemesterRepository {

    override fun observeSemesters(): Flow<List<Semester>> =
        semesterDao.observeSemestersWithCount().map { semesters -> semesters.map { it.toDomain() } }

    override suspend fun ensureDefaultSemester() {
        if (semesterDao.getSemester(DEFAULT_SEMESTER_ID) == null) {
            semesterDao.upsert(
                SemesterEntity(
                    id = DEFAULT_SEMESTER_ID,
                    name = DEFAULT_SEMESTER_NAME,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun createSemester(name: String, startDate: Long, totalWeeks: Int): String {
        val id = "semester_${UUID.randomUUID()}"
        semesterDao.upsert(
            SemesterEntity(
                id = id,
                name = name.ifBlank { "新学期" },
                startDate = startDate,
                totalWeeks = totalWeeks.coerceAtLeast(1),
                createdAt = System.currentTimeMillis()
            )
        )
        return id
    }

    override suspend fun renameSemester(id: String, name: String) {
        if (id.isBlank() || name.isBlank()) return
        semesterDao.rename(id, name.trim())
    }

    override suspend fun deleteSemester(id: String) {
        if (id.isBlank()) return
        semesterDao.delete(id)
    }

    private fun SemesterWithCount.toDomain() = Semester(
        id = id,
        name = name,
        startDate = startDate,
        totalWeeks = totalWeeks,
        createdAt = createdAt,
        courseCount = courseCount
    )

    private companion object {
        const val DEFAULT_SEMESTER_ID = "default"
        const val DEFAULT_SEMESTER_NAME = "默认学期"
    }
}
