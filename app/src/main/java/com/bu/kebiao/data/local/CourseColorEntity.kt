package com.bu.kebiao.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_name_colors")
data class CourseColorEntity(
    @PrimaryKey
    val courseName: String,
    val colorIndex: Int
)
