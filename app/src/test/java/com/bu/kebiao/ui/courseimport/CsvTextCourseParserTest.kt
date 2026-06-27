package com.bu.kebiao.ui.courseimport

import com.bu.kebiao.domain.model.WeekType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTextCourseParserTest {

    @Test
    fun parsesStrictCsvWithQuotedWeeksAndLocation() {
        val input = """
            name,day,start_section,end_section,weeks,teacher,location
            Python 程序设计,1,5,6,"4-5,8,14-17",王五,机房 301
            大学英语（1）,6,7,8,7,孙八,"教学楼 115, 语音室 402"
        """.trimIndent()

        val result = CsvTextCourseParser.parse(input, source = "csv_import")

        assertEquals(2, result.courses.size)
        assertEquals(emptyList<String>(), result.errors)
        assertEquals("Python 程序设计", result.courses[0].name)
        assertEquals(listOf(4, 5, 8, 14, 15, 16, 17), result.courses[0].weeks)
        assertEquals("教学楼 115, 语音室 402", result.courses[1].location)
    }

    @Test
    fun repairsChineseCommasAndMergedTeacherLocation() {
        val input = """
            name,day,start_section,end_section,weeks,teacher,location
            高等数学（下）,1,1,2,3-18, 张三，教学楼 301
            Python 程序设计，1,5,6,"4-5,8,14-17", 王五，机房 301
            选修课，7,1,2,6-12 双周，赵六，教学楼 402
        """.trimIndent()

        val result = CsvTextCourseParser.parse(input, source = "csv_import")

        assertEquals(3, result.courses.size)
        assertEquals(emptyList<String>(), result.errors)
        assertEquals("张三", result.courses[0].teacher)
        assertEquals("教学楼 301", result.courses[0].location)
        assertEquals(listOf(6, 8, 10, 12), result.courses[2].weeks)
        assertEquals(WeekType.EVEN, result.courses[2].weekType)
    }

    @Test
    fun repairsUnquotedWeeksWithComma() {
        val input = """
            name,day,start_section,end_section,weeks,teacher,location
            大学英语（1）,7,5,6,5-7,12,孙八,教学楼 302
        """.trimIndent()

        val result = CsvTextCourseParser.parse(input, source = "text_import")

        assertEquals(1, result.courses.size)
        assertEquals(emptyList<String>(), result.errors)
        assertEquals(listOf(5, 6, 7, 12), result.courses.single().weeks)
        assertEquals("text_import", result.courses.single().source)
    }

    @Test
    fun parsesPipeTextRows() {
        val input = """
            高等数学（下） | 周一 | 1-2节 | 3-18周 | 张三 | 教学楼 301
            体育 | 周五 | 7-8节 | 2-18双周 | 王五 | 操场
        """.trimIndent()

        val result = CsvTextCourseParser.parse(input, source = "text_import")

        assertEquals(2, result.courses.size)
        assertEquals(5, result.courses[1].dayOfWeek)
        assertEquals(listOf(2, 4, 6, 8, 10, 12, 14, 16, 18), result.courses[1].weeks)
    }

    @Test
    fun rejectsInvalidWeeksWithCourseCode() {
        val input = """
            name,day,start_section,end_section,weeks,teacher,location
            形势与政策,2,3,4,F422200006024,马红,
        """.trimIndent()

        val result = CsvTextCourseParser.parse(input, source = "csv_import")

        assertTrue(result.courses.isEmpty())
        assertEquals(1, result.errors.size)
    }
}
