package com.bu.kebiao.data.repository

import com.bu.kebiao.data.local.CourseColorDao
import com.bu.kebiao.data.local.CourseColorEntity
import com.bu.kebiao.domain.repository.CourseColorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CourseColorRepositoryImpl(
    private val courseColorDao: CourseColorDao
) : CourseColorRepository {

    override fun observeColorMap(): Flow<Map<String, Int>> =
        courseColorDao.observeAll().map { entities ->
            entities.associate { it.courseName to it.colorIndex }
        }

    override suspend fun getColorIndex(courseName: String): Int? =
        courseColorDao.getByCourseName(courseName.trim())?.colorIndex

    override suspend fun upsertColor(courseName: String, colorIndex: Int) {
        val normalizedName = courseName.trim()
        if (normalizedName.isBlank()) return
        courseColorDao.upsert(CourseColorEntity(normalizedName, colorIndex))
    }
}
