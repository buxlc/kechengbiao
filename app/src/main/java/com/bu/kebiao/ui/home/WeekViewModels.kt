package com.bu.kebiao.ui.home

import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.model.WeekType

data class WeekTimeBucket(
    val startSection: Int,
    val endSection: Int,
    val label: String,
    val startTime: String,
    val endTime: String
)

data class WeekCourseGroup(
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val courses: List<Course>
)

data class WeekCoursePlacement(
    val group: WeekCourseGroup,
    val columnIndex: Int,
    val columnCount: Int,
    val detailCourses: List<Course>
)

fun resolveCourseColor(course: Course, colorMap: Map<String, Int>): Course {
    val mappedColor = colorMap[course.name.trim()] ?: course.colorIndex
    return course.copy(colorIndex = mappedColor)
}

fun Course.isActiveInWeek(week: Int): Boolean {
    if (weeks.isNotEmpty()) return week in weeks
    if (week !in startWeek..endWeek) return false
    return when (weekType) {
        WeekType.ALL -> true
        WeekType.ODD -> week % 2 == 1
        WeekType.EVEN -> week % 2 == 0
    }
}

fun buildWeekTimeBuckets(
    classTimes: List<ClassTime>,
    fallbackSectionCount: Int = 12
): List<WeekTimeBucket> {
    val maxSection = maxOf(classTimes.maxOfOrNull { it.sectionNumber } ?: 0, fallbackSectionCount).coerceAtLeast(1)
    val timesBySection = classTimes.associateBy { it.sectionNumber }
    return (1..maxSection step 2).map { startSection ->
        val endSection = (startSection + 1).coerceAtMost(maxSection)
        WeekTimeBucket(
            startSection = startSection,
            endSection = endSection,
            label = if (startSection == endSection) "${startSection}\u8282" else "${startSection}-${endSection}\u8282",
            startTime = timesBySection[startSection]?.startTime.orEmpty(),
            endTime = timesBySection[endSection]?.endTime.orEmpty()
        )
    }
}

fun calculateWeekPlacements(courses: List<Course>): List<WeekCoursePlacement> {
    val groups = courses
        .groupBy { Triple(it.dayOfWeek, it.startSection, it.endSection) }
        .map { (key, groupedCourses) ->
            WeekCourseGroup(
                dayOfWeek = key.first,
                startSection = key.second,
                endSection = key.third,
                courses = groupedCourses.sortedBy { it.name }
            )
        }
        .sortedWith(
            compareBy<WeekCourseGroup> { it.dayOfWeek }
                .thenBy { it.startSection }
                .thenByDescending { it.endSection - it.startSection }
        )

    val placements = mutableListOf<WeekCoursePlacement>()

    groups.groupBy { it.dayOfWeek }.forEach { (_, dayGroups) ->
        val clusters = mutableListOf<MutableList<WeekCourseGroup>>()
        var currentCluster: MutableList<WeekCourseGroup>? = null
        var clusterEnd = 0

        dayGroups.forEach { group ->
            if (currentCluster == null || group.startSection > clusterEnd) {
                currentCluster = mutableListOf(group)
                clusters += currentCluster!!
                clusterEnd = group.endSection
            } else {
                currentCluster?.add(group)
                clusterEnd = maxOf(clusterEnd, group.endSection)
            }
        }

        clusters.forEach { cluster ->
            val columnEnds = mutableListOf<Int>()
            val columnIndexes = mutableMapOf<WeekCourseGroup, Int>()

            cluster.forEach { group ->
                val availableColumn = columnEnds.indexOfFirst { it < group.startSection }
                val targetColumn = if (availableColumn >= 0) availableColumn else columnEnds.size
                if (availableColumn >= 0) {
                    columnEnds[availableColumn] = group.endSection
                } else {
                    columnEnds += group.endSection
                }
                columnIndexes[group] = targetColumn
            }

            val columnCount = columnEnds.size.coerceAtLeast(1)
            val detailCourses = cluster
                .flatMap { it.courses }
                .distinctBy { it.id }
                .sortedWith(compareBy<Course> { it.startSection }.thenBy { it.endSection }.thenBy { it.name })

            cluster.forEach { group ->
                placements += WeekCoursePlacement(
                    group = group,
                    columnIndex = columnIndexes[group] ?: 0,
                    columnCount = columnCount,
                    detailCourses = if (columnCount > 1) detailCourses else group.courses
                )
            }
        }
    }

    return placements
}
