package com.bu.kebiao.domain.repository

import kotlinx.coroutines.flow.Flow

interface CourseColorRepository {
    fun observeColorMap(): Flow<Map<String, Int>>
    suspend fun getColorIndex(courseName: String): Int?
    suspend fun upsertColor(courseName: String, colorIndex: Int)
}
