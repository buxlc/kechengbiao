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
        val endTimeText: String
    ) : CourseLiveUpdateState
}

data class CourseLiveUpdateText(
    val title: String,
    val content: String
)
