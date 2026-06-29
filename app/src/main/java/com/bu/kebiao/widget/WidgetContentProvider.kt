package com.bu.kebiao.widget

import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.liveupdate.CourseLiveUpdateCalculator
import com.bu.kebiao.liveupdate.CourseLiveUpdateState
import java.time.LocalDateTime

object WidgetContentProvider {
    fun buildSnapshot(
        courses: List<Course>,
        classTimes: List<ClassTime>,
        currentWeek: Int,
        now: LocalDateTime
    ): WidgetCourseSnapshot {
        val state = CourseLiveUpdateCalculator.calculate(
            courses = courses,
            classTimes = classTimes,
            currentWeek = currentWeek,
            now = now
        )
        return WidgetCourseMapper.buildSnapshot(state, courses, classTimes, now)
    }

    fun stateFor(
        courses: List<Course>,
        classTimes: List<ClassTime>,
        currentWeek: Int,
        now: LocalDateTime
    ): CourseLiveUpdateState =
        CourseLiveUpdateCalculator.calculate(
            courses = courses,
            classTimes = classTimes,
            currentWeek = currentWeek,
            now = now
        )
}
