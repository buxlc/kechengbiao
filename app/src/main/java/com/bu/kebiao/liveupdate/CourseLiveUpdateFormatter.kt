package com.bu.kebiao.liveupdate

object CourseLiveUpdateFormatter {
    fun format(state: CourseLiveUpdateState): CourseLiveUpdateText =
        when (state) {
            CourseLiveUpdateState.Hidden -> CourseLiveUpdateText("", "")
            is CourseLiveUpdateState.Upcoming -> CourseLiveUpdateText(
                title = "${state.minutesUntilStart}分钟后上课",
                content = listOfNotNull(
                    state.course.name.trim().takeIf { it.isNotEmpty() },
                    state.course.location.trim().takeIf { it.isNotEmpty() },
                    "${state.startTimeText}开始"
                ).joinToString(" · ")
            )
            is CourseLiveUpdateState.InClass -> CourseLiveUpdateText(
                title = "正在上课",
                content = listOfNotNull(
                    state.course.name.trim().takeIf { it.isNotEmpty() },
                    state.course.location.trim().takeIf { it.isNotEmpty() },
                    "${state.endTimeText}下课"
                ).joinToString(" · ")
            )
        }
}
