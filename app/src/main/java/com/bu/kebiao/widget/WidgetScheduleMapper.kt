package com.bu.kebiao.widget

import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import java.time.LocalDate

object WidgetScheduleMapper {
    fun build(
        courses: List<Course>,
        classTimes: List<ClassTime>,
        todayDayOfWeek: Int,
        currentWeek: Int = 1,
        todayDate: LocalDate = LocalDate.now()
    ): WidgetScheduleSnapshot {
        val todayCourses = courses.filter { it.dayOfWeek == todayDayOfWeek }.sortedBy { it.startSection }
        val tomorrowDay = if (todayDayOfWeek == 7) 1 else todayDayOfWeek + 1
        val tomorrowCourses = courses.filter { it.dayOfWeek == tomorrowDay }.sortedBy { it.startSection }

        return WidgetScheduleSnapshot(
            small = buildSmallCard(todayCourses, classTimes, todayDayOfWeek),
            today = buildDayColumn("今天", todayCourses, classTimes),
            tomorrow = buildDayColumn("明天", tomorrowCourses, classTimes),
            headerLeft = "大二下",
            headerCenter = "${todayDate.monthValue}.${todayDate.dayOfMonth}  第${currentWeek}周",
            headerRight = dayName(todayDayOfWeek)
        )
    }

    private fun buildSmallCard(
        todayCourses: List<Course>,
        classTimes: List<ClassTime>,
        todayDayOfWeek: Int
    ): WidgetSmallScheduleSnapshot =
        WidgetSmallScheduleSnapshot(
            label = dayName(todayDayOfWeek),
            countText = "共${todayCourses.size}节课",
            courses = todayCourses.mapIndexed { index, course ->
                WidgetCourseCard(
                    stateLabel = dayName(todayDayOfWeek),
                    courseName = course.name.trim().ifEmpty { "课程" },
                    detailLine = nextTimeLine(course, classTimes),
                    extraLine = course.location.trim().ifEmpty { "地点待定" },
                    accentColor = accentFor(index)
                )
            }
        )

    private fun buildDayColumn(
        label: String,
        courses: List<Course>,
        classTimes: List<ClassTime>
    ): WidgetDayColumn {
        val tiles = courses.take(LargeColumnCourseCapacity).mapIndexed { index, course ->
            WidgetCourseTile(
                title = course.name.trim().ifEmpty { "课程" },
                timeLine = nextTimeLine(course, classTimes),
                locationLine = course.location.trim().ifEmpty { "地点待定" },
                accentColor = accentFor(index)
            )
        }
        return WidgetDayColumn(
            label = label,
            countText = "共${courses.size}节课",
            tiles = tiles,
            emptyText = ""
        )
    }

    private fun nextTimeLine(course: Course, classTimes: List<ClassTime>): String {
        val start = classTimes.firstOrNull { it.sectionNumber == course.startSection }?.startTime.orEmpty()
        val end = classTimes.firstOrNull { it.sectionNumber == course.endSection }?.endTime.orEmpty()
        return if (start.isNotBlank() && end.isNotBlank()) "$start - $end" else "时间待定"
    }

    private fun accentFor(index: Int): Int =
        when (index % 4) {
            0 -> 0xFF4CA9EF.toInt()
            1 -> 0xFF2DBCC8.toInt()
            2 -> 0xFFFF737C.toInt()
            else -> 0xFF44A8EF.toInt()
        }

    private fun dayName(dayOfWeek: Int): String =
        when (dayOfWeek) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            else -> "周日"
        }

    private const val LargeColumnCourseCapacity = 2
}
