package com.bu.kebiao.ui.courseedit

import android.net.Uri
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.ui.theme.CourseColors

data class CourseEditDraft(
    val id: Long = 0,
    val name: String = "",
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int = 1,
    val startSection: Int = 1,
    val endSection: Int = 2,
    val weeksText: String = "1-20",
    val colorIndex: Int = 0
)

object CourseEditDestination {
    const val route =
        "course/edit?courseId={courseId}&name={name}&teacher={teacher}&location={location}&day={day}&start={start}&end={end}&weeks={weeks}&color={color}"

    fun createRoute(draft: CourseEditDraft): String {
        return buildString {
            append("course/edit")
            append("?courseId=${draft.id}")
            append("&name=${Uri.encode(draft.name)}")
            append("&teacher=${Uri.encode(draft.teacher)}")
            append("&location=${Uri.encode(draft.location)}")
            append("&day=${draft.dayOfWeek}")
            append("&start=${draft.startSection}")
            append("&end=${draft.endSection}")
            append("&weeks=${Uri.encode(draft.weeksText)}")
            append("&color=${draft.colorIndex}")
        }
    }
}

fun Course.toEditDraft(defaultTotalWeeks: Int = 20, resolvedColorIndex: Int = colorIndex): CourseEditDraft {
    val displayWeeks = if (weeks.isNotEmpty()) {
        CourseWeekParser.toDisplayText(weeks, defaultTotalWeeks)
    } else {
        "${startWeek.coerceAtLeast(1)}-${endWeek.coerceAtLeast(startWeek)}"
    }
    return CourseEditDraft(
        id = id,
        name = name,
        teacher = teacher,
        location = location,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        endSection = endSection,
        weeksText = displayWeeks,
        colorIndex = resolvedColorIndex
    )
}

fun createDefaultCourseDraft(
    dayOfWeek: Int,
    totalWeeks: Int,
    colorIndex: Int = 0
): CourseEditDraft {
    return CourseEditDraft(
        dayOfWeek = dayOfWeek.coerceIn(1, 7),
        startSection = 1,
        endSection = 2,
        weeksText = "1-${totalWeeks.coerceAtLeast(1)}",
        colorIndex = colorIndex.coerceAtLeast(0)
    )
}

fun suggestedCourseColorIndex(name: String): Int {
    if (CourseColors.isEmpty()) return 0
    val normalized = name.trim()
    if (normalized.isBlank()) return 0
    return (normalized.hashCode() and Int.MAX_VALUE) % CourseColors.size
}

fun CourseEditDraft.normalizedName(): String = name.trim()

fun CourseEditDraft.toCourse(
    weeks: List<Int>,
    source: String = "manual"
): Course {
    val safeWeeks = weeks.filter { it > 0 }.distinct().sorted()
    return Course(
        id = id,
        name = normalizedName(),
        teacher = teacher.trim(),
        location = location.trim(),
        dayOfWeek = dayOfWeek.coerceIn(1, 7),
        startSection = startSection.coerceAtLeast(1),
        endSection = endSection.coerceAtLeast(startSection.coerceAtLeast(1)),
        startWeek = safeWeeks.firstOrNull() ?: 1,
        endWeek = safeWeeks.lastOrNull() ?: 20,
        weekType = CourseWeekParser.inferWeekType(safeWeeks),
        weeks = safeWeeks,
        colorIndex = colorIndex.coerceAtLeast(0),
        source = source
    )
}
