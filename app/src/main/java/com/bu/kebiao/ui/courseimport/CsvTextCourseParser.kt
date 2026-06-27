package com.bu.kebiao.ui.courseimport

import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.model.WeekType
import com.bu.kebiao.ui.courseedit.CourseWeekParser

data class CsvTextParseResult(
    val courses: List<Course>,
    val errors: List<String>
)

object CsvTextCourseParser {
    private val headerAliases = setOf("name", "课程名", "课程名称")
    private val weeksPattern = Regex("""^[0-9,\-单双周\s]+$""")

    fun parse(text: String, source: String): CsvTextParseResult {
        val courses = mutableListOf<Course>()
        val errors = mutableListOf<String>()
        val lines = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        lines.forEachIndexed { index, line ->
            if (isHeader(line)) return@forEachIndexed
            val row = parseRow(line)
            if (row == null) {
                errors += "第 ${index + 1} 行无法识别为 7 列课程数据"
                return@forEachIndexed
            }

            val course = rowToCourse(row, source, courses.size)
            if (course == null) {
                errors += "第 ${index + 1} 行字段异常：${row.joinToString(" | ")}"
            } else {
                courses += course
            }
        }

        return CsvTextParseResult(courses, errors)
    }

    private fun isHeader(line: String): Boolean {
        val first = parseCsvLine(line).firstOrNull()?.trim()?.lowercase().orEmpty()
        return first in headerAliases
    }

    private fun parseRow(line: String): List<String>? {
        if (line.contains('|')) {
            normalizeTextFields(line.split('|').map { it.trim() })?.let { return it }
        }

        val strict = parseCsvLine(line).map { it.trim() }
        normalizeCsvFields(strict)?.let { return it }

        val repairedLine = repairLikelyChineseDelimiters(line)
        val repaired = parseCsvLine(repairedLine).map { it.trim() }
        return normalizeCsvFields(repaired)
    }

    private fun normalizeTextFields(fields: List<String>): List<String>? {
        if (fields.size < 4) return null
        val name = fields.getOrNull(0).orEmpty()
        val day = parseDay(fields.getOrNull(1).orEmpty())?.toString() ?: return null
        val sections = parseSectionRange(fields.getOrNull(2).orEmpty()) ?: return null
        val weeks = fields.getOrNull(3).orEmpty()
        val teacher = fields.getOrNull(4).orEmpty()
        val location = fields.getOrNull(5).orEmpty()
        return listOf(name, day, sections.first.toString(), sections.second.toString(), weeks, teacher, location)
    }

    private fun normalizeCsvFields(fields: List<String>): List<String>? {
        if (fields.size == 7) return fields
        if (fields.size == 6) {
            val last = fields[5]
            val split = splitTeacherLocation(last) ?: return null
            return fields.take(5) + split.first + split.second
        }
        if (fields.size > 7) {
            repairUnquotedWeeks(fields)?.let { return it }
        }
        return null
    }

    private fun repairUnquotedWeeks(fields: List<String>): List<String>? {
        if (fields.size < 8) return null
        val prefix = fields.take(4)
        val suffix = fields.takeLast(2)
        val weeksParts = fields.drop(4).dropLast(2)
        val weeks = weeksParts.joinToString(",").trim()
        return (prefix + weeks + suffix).takeIf { it.size == 7 }
    }

    private fun splitTeacherLocation(value: String): Pair<String, String>? {
        val normalized = value.trim().trim('"')
        val separatorIndex = normalized.indexOf('，').takeIf { it >= 0 }
            ?: normalized.indexOf(',').takeIf { it >= 0 }
            ?: return null
        val teacher = normalized.substring(0, separatorIndex).trim()
        val location = normalized.substring(separatorIndex + 1).trim()
        return teacher to location
    }

    private fun repairLikelyChineseDelimiters(line: String): String {
        val builder = StringBuilder()
        var inQuotes = false
        line.forEachIndexed { index, ch ->
            when (ch) {
                '"' -> {
                    builder.append(ch)
                    inQuotes = !inQuotes
                }
                '，' -> {
                    val next = line.drop(index + 1).firstOrNull { !it.isWhitespace() }
                    val prev = line.take(index).lastOrNull { !it.isWhitespace() }
                    if (!inQuotes && (next?.isDigit() == true || prev?.isDigit() == true)) {
                        builder.append(',')
                    } else {
                        builder.append(ch)
                    }
                }
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val ch = line[index]
            when {
                ch == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    fields += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
            index++
        }
        fields += current.toString()
        return fields
    }

    private fun rowToCourse(row: List<String>, source: String, colorIndex: Int): Course? {
        val name = row[0].trim()
        if (name.isBlank()) return null

        val day = parseDay(row[1]) ?: return null
        val startSection = row[2].trim().toIntOrNull() ?: return null
        val endSection = row[3].trim().toIntOrNull()?.coerceAtLeast(startSection) ?: return null
        val weeksText = normalizeWeeksText(row[4])
        if (!isValidWeeksText(weeksText)) return null
        val weeks = CourseWeekParser.parseWeeksText(weeksText).ifEmpty { return null }
        val weekType = CourseWeekParser.inferWeekType(weeks)

        return Course(
            name = name,
            teacher = row[5].trim().trim('"'),
            location = row[6].trim().trim('"'),
            dayOfWeek = day,
            startSection = startSection,
            endSection = endSection,
            startWeek = weeks.first(),
            endWeek = weeks.last(),
            weekType = weekType,
            weeks = weeks,
            colorIndex = colorIndex % 8,
            source = source
        )
    }

    private fun parseDay(value: String): Int? {
        val text = value.trim()
        text.toIntOrNull()?.takeIf { it in 1..7 }?.let { return it }
        return when {
            text.contains("一") -> 1
            text.contains("二") -> 2
            text.contains("三") -> 3
            text.contains("四") -> 4
            text.contains("五") -> 5
            text.contains("六") -> 6
            text.contains("日") || text.contains("天") -> 7
            else -> null
        }
    }

    private fun parseSectionRange(value: String): Pair<Int, Int>? {
        val match = Regex("""(\d{1,2})(?:\s*[-~至到]\s*(\d{1,2}))?""").find(value) ?: return null
        val start = match.groupValues[1].toIntOrNull() ?: return null
        val end = match.groupValues.getOrNull(2)?.toIntOrNull() ?: start
        return start to end.coerceAtLeast(start)
    }

    private fun normalizeWeeksText(value: String): String =
        value.trim()
            .trim('"')
            .replace(" ", "")
            .replace("，", ",")
            .replace("周", "")

    private fun isValidWeeksText(value: String): Boolean =
        value.isNotBlank() && weeksPattern.matches(value) && value.any { it.isDigit() }
}
