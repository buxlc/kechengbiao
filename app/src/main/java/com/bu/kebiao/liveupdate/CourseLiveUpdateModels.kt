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
        val sectionNumber: Int,
        val sectionEndTimeText: String,
        val minutesUntilSectionEnd: Int,
        val progressPercent: Int
    ) : CourseLiveUpdateState

    data class SectionBreak(
        val course: Course,
        val nextSectionNumber: Int,
        val nextSectionStartsAt: LocalDateTime,
        val nextSectionStartTimeText: String,
        val minutesUntilNextSection: Int
    ) : CourseLiveUpdateState
}

data class CourseLiveUpdateText(
    val title: String,
    val content: String,
    val expandedText: String,
    val expandedLines: List<String> = emptyList(),
    val progress: Int = 0,
    val progressMax: Int = 0
)
