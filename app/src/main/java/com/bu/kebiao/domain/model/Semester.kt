package com.bu.kebiao.domain.model

data class Semester(
    val id: String,
    val name: String,
    val startDate: Long = 0L,
    val totalWeeks: Int = 20,
    val createdAt: Long = 0L,
    val courseCount: Int = 0
)
