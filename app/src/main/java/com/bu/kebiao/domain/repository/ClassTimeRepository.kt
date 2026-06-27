package com.bu.kebiao.domain.repository

import com.bu.kebiao.domain.model.ClassTime
import kotlinx.coroutines.flow.Flow

interface ClassTimeRepository {
    fun getAllClassTimes(): Flow<List<ClassTime>>
    suspend fun getClassTime(section: Int): ClassTime?
    suspend fun insertClassTime(classTime: ClassTime)
    suspend fun updateClassTime(classTime: ClassTime)
    suspend fun insertDefaultTimes()
}
