package com.bu.kebiao.domain.model

data class Course(
    val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val weekType: WeekType = WeekType.ALL,
    val weeks: List<Int> = emptyList(),
    val colorIndex: Int = 0,
    val source: String = "manual"
)

enum class WeekType(val value: Int) {
    ALL(0),
    ODD(1),
    EVEN(2);

    companion object {
        fun fromValue(value: Int): WeekType = entries.find { it.value == value } ?: ALL
    }
}
