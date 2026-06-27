package com.bu.kebiao.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_times")
data class ClassTimeEntity(
    @PrimaryKey
    val sectionNumber: Int,
    val startTime: String,
    val endTime: String
)
