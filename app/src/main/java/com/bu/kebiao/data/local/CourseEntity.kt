package com.bu.kebiao.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val weekType: Int = 0,
    val weeks: String = "",
    val colorIndex: Int = 0,
    val source: String = "manual"
)
