package com.bu.kebiao.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val startDate: Long = 0L,
    val totalWeeks: Int = 20,
    val createdAt: Long = 0L
)

data class SemesterWithCount(
    val id: String,
    val name: String,
    val startDate: Long = 0L,
    val totalWeeks: Int = 20,
    val createdAt: Long = 0L,
    val courseCount: Int = 0
)
