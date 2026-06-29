package com.bu.kebiao.widget

import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.liveupdate.CourseLiveUpdateState
import java.time.LocalDateTime

object WidgetCourseMapper {
    fun buildSnapshot(
        state: CourseLiveUpdateState,
        todayCourses: List<Course>,
        classTimes: List<ClassTime>,
        now: LocalDateTime = LocalDateTime.now()
    ): WidgetCourseSnapshot {
        val orderedTodayCourses = todayCourses.sortedWith(
            compareBy<Course> { it.startSection }.thenBy { it.endSection }.thenBy { it.name }
        )
        val primary = buildPrimaryCard(state, orderedTodayCourses, classTimes, now)
        val secondary = buildSecondaryCard(state, orderedTodayCourses, classTimes, now)
        val footer = when (state) {
            CourseLiveUpdateState.Hidden -> "今日无课"
            is CourseLiveUpdateState.Upcoming -> "准备上课"
            is CourseLiveUpdateState.InClass -> "正在上课"
            is CourseLiveUpdateState.SectionBreak -> "课间休息"
        }
        return WidgetCourseSnapshot(primary = primary, secondary = secondary, footer = footer)
    }

    private fun buildPrimaryCard(
        state: CourseLiveUpdateState,
        todayCourses: List<Course>,
        classTimes: List<ClassTime>,
        now: LocalDateTime
    ): WidgetCourseCard {
        return when (state) {
            CourseLiveUpdateState.Hidden -> buildIdleCard(todayCourses, classTimes, now)
            is CourseLiveUpdateState.Upcoming -> WidgetCourseCard(
                stateLabel = "准备上课",
                courseName = state.course.name.cleanOrFallback("课程"),
                detailLine = formatCourseTimeLine(state.course, classTimes),
                extraLine = "${state.course.location.cleanOrFallback("地点待定")} · ${state.course.teacher.cleanOrFallback("未填写")}",
                accentColor = 0xFFD4850F.toInt()
            )
            is CourseLiveUpdateState.InClass -> WidgetCourseCard(
                stateLabel = "正在上课",
                courseName = state.course.name.cleanOrFallback("课程"),
                detailLine = "第${state.sectionNumber}节",
                extraLine = "${state.course.location.cleanOrFallback("地点待定")} · ${state.course.teacher.cleanOrFallback("未填写")}",
                accentColor = 0xFF2D8A56.toInt(),
                progress = state.progressPercent,
                progressMax = 100
            )
            is CourseLiveUpdateState.SectionBreak -> WidgetCourseCard(
                stateLabel = "课间休息",
                courseName = state.course.name.cleanOrFallback("课程"),
                detailLine = "第${state.nextSectionNumber}节 ${state.nextSectionStartTimeText} 开始",
                extraLine = "下一节课即将开始",
                accentColor = 0xFF2B4A8C.toInt()
            )
        }
    }

    private fun buildSecondaryCard(
        state: CourseLiveUpdateState,
        todayCourses: List<Course>,
        classTimes: List<ClassTime>,
        now: LocalDateTime
    ): WidgetCourseCard? {
        if (todayCourses.isEmpty()) return null

        val nextCourse = when (state) {
            CourseLiveUpdateState.Hidden -> findNextCourse(todayCourses, classTimes, now)
            is CourseLiveUpdateState.Upcoming -> todayCourses.firstOrNull { it.startSection > state.course.startSection }
                ?: todayCourses.firstOrNull { it.id != state.course.id }
            is CourseLiveUpdateState.InClass -> todayCourses.firstOrNull { it.startSection > state.course.endSection }
                ?: todayCourses.firstOrNull { it.id != state.course.id }
            is CourseLiveUpdateState.SectionBreak -> todayCourses.firstOrNull { it.startSection >= state.nextSectionNumber }
                ?: todayCourses.firstOrNull { it.id != state.course.id }
        } ?: return null

        val startLine = classTimes.firstOrNull { it.sectionNumber == nextCourse.startSection }
            ?.let { "${it.startTime} 开始" }
            ?: "今天共${todayCourses.size}节课"

        return WidgetCourseCard(
            stateLabel = "今日安排",
            courseName = nextCourse.name.cleanOrFallback("课程"),
            detailLine = "${nextCourse.location.cleanOrFallback("地点待定")} · ${nextCourse.teacher.cleanOrFallback("未填写")}",
            extraLine = startLine,
            accentColor = 0xFFD8E2F4.toInt()
        )
    }

    private fun buildIdleCard(
        todayCourses: List<Course>,
        classTimes: List<ClassTime>,
        now: LocalDateTime
    ): WidgetCourseCard {
        val nextCourse = findNextCourse(todayCourses, classTimes, now)
        if (nextCourse == null) {
            return WidgetCourseCard(
                stateLabel = "今日无课",
                courseName = "Bu课表",
                detailLine = "今天没有课程",
                extraLine = "明天再来",
                accentColor = 0xFFD8DCE4.toInt()
            )
        }

        return WidgetCourseCard(
            stateLabel = "下一节课",
            courseName = nextCourse.name.cleanOrFallback("课程"),
            detailLine = formatCourseTimeLine(nextCourse, classTimes),
            extraLine = "${nextCourse.location.cleanOrFallback("地点待定")} · ${nextCourse.teacher.cleanOrFallback("未填写")}",
            accentColor = 0xFF5B8DEF.toInt()
        )
    }

    private fun findNextCourse(
        todayCourses: List<Course>,
        classTimes: List<ClassTime>,
        now: LocalDateTime
    ): Course? {
        val today = now.toLocalDate()
        val timeBySection = classTimes.associateBy { it.sectionNumber }
        return todayCourses
            .sortedBy { it.startSection }
            .firstOrNull { course ->
                val startTime = timeBySection[course.startSection]?.startTime?.parseLocalTime()
                val startDateTime = startTime?.let { LocalDateTime.of(today, it) } ?: return@firstOrNull false
                now.isBefore(startDateTime)
            }
    }

    private fun formatCourseTimeLine(course: Course, classTimes: List<ClassTime>): String {
        val start = classTimes.firstOrNull { it.sectionNumber == course.startSection }?.startTime.orEmpty()
        val end = classTimes.firstOrNull { it.sectionNumber == course.endSection }?.endTime.orEmpty()
        return if (start.isNotBlank() && end.isNotBlank()) {
            "$start - $end"
        } else {
            "时间待定"
        }
    }

    private fun String.parseLocalTime() = runCatching {
        java.time.LocalTime.parse(this.trim())
    }.getOrNull()

    private fun String.cleanOrFallback(fallback: String): String = trim().takeIf { it.isNotEmpty() } ?: fallback
}
