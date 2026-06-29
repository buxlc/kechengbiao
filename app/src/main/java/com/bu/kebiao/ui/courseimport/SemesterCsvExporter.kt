package com.bu.kebiao.ui.courseimport

import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.model.Semester

object SemesterCsvExporter {
    fun export(semester: Semester, courses: List<Course>): String {
        val lines = mutableListOf<String>()
        lines += "# semester_name=${semester.name}"
        lines += "# total_weeks=${semester.totalWeeks}"
        if (semester.startDate > 0L) lines += "# start_date=${semester.startDate}"
        lines += "name,day,start_section,end_section,weeks,teacher,location,color_index"

        courses
            .sortedWith(compareBy<Course> { it.dayOfWeek }.thenBy { it.startSection }.thenBy { it.name })
            .forEach { course ->
                lines += listOf(
                    course.name,
                    course.dayOfWeek.toString(),
                    course.startSection.toString(),
                    course.endSection.toString(),
                    course.weeks.joinToString(",").ifBlank { "${course.startWeek}-${course.endWeek}" },
                    course.teacher,
                    course.location,
                    course.colorIndex.toString()
                ).joinToString(",") { escape(it) }
            }

        return lines.joinToString("\n")
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
