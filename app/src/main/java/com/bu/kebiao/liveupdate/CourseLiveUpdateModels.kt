package com.bu.kebiao.liveupdate

import com.bu.kebiao.domain.model.Course
import java.time.LocalDateTime

sealed interface CourseLiveUpdateState {
    data object Hidden : CourseLiveUpdateState

    data class Upcoming(
        val course: Course,
        val startsAt: LocalDateTime,
        val endsAt: LocalDateTime,
        val minutesUntilStart: Int,
        val startTimeText: String
    ) : CourseLiveUpdateState

    data class InClass(
        val course: Course,
        val startsAt: LocalDateTime,
        val endsAt: LocalDateTime,
        val endTimeText: String,
        val minutesUntilEnd: Int,
        val progressPercent: Int
    ) : CourseLiveUpdateState
}

data class CourseLiveUpdateText(
    val title: String,
    val content: String,
    val expandedText: String,
    val progress: Int = 0,
    val progressMax: Int = 0
)
