package com.bu.kebiao.data.repository

import com.bu.kebiao.data.local.ClassTimeDao
import com.bu.kebiao.data.local.ClassTimeEntity
import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.repository.ClassTimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClassTimeRepositoryImpl(
    private val classTimeDao: ClassTimeDao
) : ClassTimeRepository {

    override fun getAllClassTimes(): Flow<List<ClassTime>> =
        classTimeDao.getAllClassTimes().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getClassTime(section: Int): ClassTime? =
        classTimeDao.getClassTime(section)?.toDomain()

    override suspend fun insertClassTime(classTime: ClassTime) {
        classTimeDao.insert(classTime.toEntity())
    }

    override suspend fun updateClassTime(classTime: ClassTime) {
        classTimeDao.update(classTime.toEntity())
    }

    override suspend fun insertDefaultTimes() {
        val defaults = listOf(
            ClassTime(1, "08:00", "08:45"),
            ClassTime(2, "08:55", "09:40"),
            ClassTime(3, "10:00", "10:45"),
            ClassTime(4, "10:55", "11:40"),
            ClassTime(5, "14:00", "14:45"),
            ClassTime(6, "14:55", "15:40"),
            ClassTime(7, "16:00", "16:45"),
            ClassTime(8, "16:55", "17:40"),
            ClassTime(9, "19:00", "19:45"),
            ClassTime(10, "19:55", "20:40"),
            ClassTime(11, "20:50", "21:35"),
            ClassTime(12, "21:45", "22:30")
        )
        classTimeDao.insertAll(defaults.map { it.toEntity() })
    }

    private fun ClassTimeEntity.toDomain() = ClassTime(
        sectionNumber = sectionNumber,
        startTime = startTime,
        endTime = endTime
    )

    private fun ClassTime.toEntity() = ClassTimeEntity(
        sectionNumber = sectionNumber,
        startTime = startTime,
        endTime = endTime
    )
}
