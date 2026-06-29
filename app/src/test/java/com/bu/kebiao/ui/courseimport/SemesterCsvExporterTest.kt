package com.bu.kebiao.ui.courseimport

import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.model.Semester
import org.junit.Assert.assertTrue
import org.junit.Test

class SemesterCsvExporterTest {

    @Test
    fun exportsSemesterMetadataAndEscapesCsvFields() {
        val semester = Semester(id = "spring", name = "2026春季学期", totalWeeks = 20)
        val courses = listOf(
            Course(
                name = "生物化学",
                teacher = "王峰,赵瑞强",
                location = "A3栋427-分子医",
                dayOfWeek = 1,
                startSection = 2,
                endSection = 5,
                weeks = listOf(3, 4, 5, 7, 9, 11),
                colorIndex = 2,
                source = "ics_import",
                semesterId = "spring"
            )
        )

        val csv = SemesterCsvExporter.export(semester, courses)

        assertTrue(csv.contains("# semester_name=2026春季学期"))
        assertTrue(csv.contains("name,day,start_section,end_section,weeks,teacher,location,color_index"))
        assertTrue(csv.contains("生物化学,1,2,5,\"3,4,5,7,9,11\",\"王峰,赵瑞强\",A3栋427-分子医,2"))
    }
}
