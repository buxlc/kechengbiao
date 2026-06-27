package com.bu.kebiao.ui.courseedit

import com.bu.kebiao.domain.model.WeekType

object CourseWeekParser {

    private const val ODD = "\u5355"
    private const val EVEN = "\u53cc"
    private const val WEEK = "\u5468"
    private const val ODD_WEEKS = "\u5355\u5468"
    private const val EVEN_WEEKS = "\u53cc\u5468"
    private const val WEEK_TEXT = "\u5468\u6b21"
    private const val EVERY_WEEK = "\u6bcf\u5468"

    fun parseWeeksText(text: String, defaultTotalWeeks: Int = 20): List<Int> {
        val normalized = normalize(text)
        val maxWeek = defaultTotalWeeks.coerceAtLeast(1)
        if (normalized.isBlank()) {
            return (1..maxWeek).toList()
        }

        val result = linkedSetOf<Int>()
        normalized
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { segment ->
                val weekType = when {
                    segment.contains(ODD) -> WeekType.ODD
                    segment.contains(EVEN) -> WeekType.EVEN
                    else -> WeekType.ALL
                }

                val cleanSegment = segment
                    .replace(ODD_WEEKS, "")
                    .replace(EVEN_WEEKS, "")
                    .replace(ODD, "")
                    .replace(EVEN, "")
                    .replace(WEEK, "")
                    .trim(',')
                    .trim()

                WEEK_TOKEN_REGEX.findAll(cleanSegment).forEach tokenLoop@{ match ->
                    val start = match.groupValues[1].toIntOrNull() ?: return@tokenLoop
                    val end = match.groupValues[2].toIntOrNull()?.coerceAtLeast(start) ?: start
                    for (week in start..end) {
                        if (week !in 1..maxWeek) continue
                        if (weekType == WeekType.ODD && week % 2 == 0) continue
                        if (weekType == WeekType.EVEN && week % 2 != 0) continue
                        result += week
                    }
                }
            }

        return result.toList().sorted()
    }

    fun inferWeekType(weeks: List<Int>): WeekType = when {
        weeks.isEmpty() -> WeekType.ALL
        weeks.all { it % 2 == 1 } -> WeekType.ODD
        weeks.all { it % 2 == 0 } -> WeekType.EVEN
        else -> WeekType.ALL
    }

    fun toWeeksStorage(text: String, defaultTotalWeeks: Int = 20): String =
        parseWeeksText(text, defaultTotalWeeks).joinToString(",")

    fun toDisplayText(weeks: List<Int>, defaultTotalWeeks: Int = 20): String {
        val sortedWeeks = weeks.filter { it > 0 }.distinct().sorted()
        if (sortedWeeks.isEmpty()) {
            return "1-${defaultTotalWeeks.coerceAtLeast(1)}"
        }

        val oddOnly = sortedWeeks.all { it % 2 == 1 }
        val evenOnly = sortedWeeks.all { it % 2 == 0 }
        if ((oddOnly || evenOnly) && sortedWeeks.size > 1) {
            val isContinuous = sortedWeeks.zipWithNext().all { (current, next) -> next - current == 2 }
            if (isContinuous) {
                val suffix = if (oddOnly) ODD_WEEKS else EVEN_WEEKS
                return "${sortedWeeks.first()}-${sortedWeeks.last()}$suffix"
            }
        }

        val ranges = mutableListOf<String>()
        var start = sortedWeeks.first()
        var end = start

        for (index in 1 until sortedWeeks.size) {
            val current = sortedWeeks[index]
            if (current == end + 1) {
                end = current
            } else {
                ranges += if (start == end) "$start" else "$start-$end"
                start = current
                end = current
            }
        }

        ranges += if (start == end) "$start" else "$start-$end"
        return ranges.joinToString(",")
    }

    private fun normalize(text: String): String {
        return text
            .trim()
            .replace('\uFF08', '(')
            .replace('\uFF09', ')')
            .replace('\u3010', '[')
            .replace('\u3011', ']')
            .replace('\uFF3B', '[')
            .replace('\uFF3D', ']')
            .replace('\uFF0C', ',')
            .replace('\u3001', ',')
            .replace('\uFF1B', ',')
            .replace(';', ',')
            .replace('\uFF5E', '-')
            .replace('~', '-')
            .replace('\u2014', '-')
            .replace('\u2013', '-')
            .replace('\uFF0D', '-')
            .replace('\u81F3', '-')
            .replace('\u5230', '-')
            .replace(" ", "")
            .replace(WEEK_TEXT, "")
            .replace(EVERY_WEEK, "")
            .replace(SECTION_BLOCK_REGEX, ",")
            .replace(FULL_SECTION_REGEX, ",")
            .replace(BRACKET_REGEX, ",")
            .trim(',')
    }

    private val WEEK_TOKEN_REGEX = Regex("""(\d{1,2})(?:-(\d{1,2}))?""")
    private val FULL_SECTION_REGEX = Regex("""(?:\u7b2c)?\d{1,2}(?:-\d{1,2})?\u8282""")
    private val SECTION_BLOCK_REGEX = Regex("""[\(\[]\d{1,2}(?:-\d{1,2})?\u8282[\)\]]""")
    private val BRACKET_REGEX = Regex("""[\(\)\[\]]""")
}
