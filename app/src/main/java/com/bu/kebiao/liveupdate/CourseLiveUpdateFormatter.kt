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
                        appendLine(course.name.cleanOrFallback())
                        appendLine("${course.location.cleanLocation()} · ${course.teacher.cleanTeacher()}")
                        append("${state.startTimeText}开始 · 还有${state.minutesUntilStart}分钟")
                    },
                    expandedLines = listOf(
                        course.name.cleanOrFallback(),
                        "${course.location.cleanLocation()} · ${course.teacher.cleanTeacher()}",
                        "${state.startTimeText}开始 · 还有${state.minutesUntilStart}分钟"
                    )
                )
            }
            is CourseLiveUpdateState.InClass -> {
                val course = state.course
                CourseLiveUpdateText(
                    title = "正在上课",
                    content = listOfNotNull(
                        course.name.cleanOrNull(),
                        "第${state.sectionNumber}节"
                    ).joinToString(" · "),
                    expandedText = buildString {
                        appendLine(course.name.cleanOrFallback())
                        appendLine("${course.location.cleanLocation()} · ${course.teacher.cleanTeacher()}")
                        append("第${state.sectionNumber}节 · 还有${state.minutesUntilSectionEnd}分钟下课")
                    },
                    expandedLines = listOf(
                        course.name.cleanOrFallback(),
                        "${course.location.cleanLocation()} · ${course.teacher.cleanTeacher()}",
                        "第${state.sectionNumber}节 · 还有${state.minutesUntilSectionEnd}分钟下课"
                    ),
                    progress = state.progressPercent,
                    progressMax = 100
                )
            }
            is CourseLiveUpdateState.SectionBreak -> {
                val course = state.course
                CourseLiveUpdateText(
                    title = "课间休息",
                    content = listOfNotNull(
                        course.name.cleanOrNull(),
                        "第${state.nextSectionNumber}节${state.nextSectionStartTimeText}开始"
                    ).joinToString(" · "),
                    expandedText = buildString {
                        appendLine(course.name.cleanOrFallback())
                        appendLine("${course.location.cleanLocation()} · ${course.teacher.cleanTeacher()}")
                        append("第${state.nextSectionNumber}节 ${state.nextSectionStartTimeText}开始 · 还有${state.minutesUntilNextSection}分钟")
                    },
                    expandedLines = listOf(
                        course.name.cleanOrFallback(),
                        "${course.location.cleanLocation()} · ${course.teacher.cleanTeacher()}",
                        "第${state.nextSectionNumber}节 ${state.nextSectionStartTimeText}开始 · 还有${state.minutesUntilNextSection}分钟"
                    )
                )
            }
        }

    private fun String.cleanOrNull(): String? = trim().takeIf { it.isNotEmpty() }

    private fun String.cleanOrFallback(): String = cleanOrNull() ?: "未填写"

    private fun String.cleanLocation(): String = cleanOrNull() ?: "地点待定"

    private fun String.cleanTeacher(): String = cleanOrNull() ?: "未填写"
}
