package com.bu.kebiao.liveupdate

object CourseLiveUpdateFormatter {
    fun format(state: CourseLiveUpdateState): CourseLiveUpdateText =
        when (state) {
            CourseLiveUpdateState.Hidden -> CourseLiveUpdateText("", "", "")
            is CourseLiveUpdateState.Upcoming -> {
                val course = state.course
                CourseLiveUpdateText(
                    title = "准备上课",
                    content = listOfNotNull(
                        course.name.cleanOrNull(),
                        "${state.startTimeText}开始"
                    ).joinToString(" · "),
                    expandedText = buildString {
                        appendLine("课程：${course.name.cleanOrFallback()}")
                        appendLine("地点：${course.location.cleanOrFallback()}")
                        appendLine("教师：${course.teacher.cleanOrFallback()}")
                        append("倒计时：还有${state.minutesUntilStart}分钟上课")
                    }
                )
            }
            is CourseLiveUpdateState.InClass -> {
                val course = state.course
                CourseLiveUpdateText(
                    title = "正在上课",
                    content = listOfNotNull(
                        course.name.cleanOrNull(),
                        "${state.endTimeText}下课"
                    ).joinToString(" · "),
                    expandedText = buildString {
                        appendLine("课程：${course.name.cleanOrFallback()}")
                        appendLine("地点：${course.location.cleanOrFallback()}")
                        appendLine("教师：${course.teacher.cleanOrFallback()}")
                        append("课间倒计时：还有${state.minutesUntilEnd}分钟下课")
                    },
                    progress = state.progressPercent,
                    progressMax = 100
                )
            }
        }

    private fun String.cleanOrNull(): String? = trim().takeIf { it.isNotEmpty() }

    private fun String.cleanOrFallback(): String = cleanOrNull() ?: "未填写"
}
