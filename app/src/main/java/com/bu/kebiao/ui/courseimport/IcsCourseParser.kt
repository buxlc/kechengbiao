package com.bu.kebiao.ui.courseimport

import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.ui.courseedit.CourseWeekParser
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class IcsCourseParseResult(
    val courses: List<Course>,
    val errors: List<String>
)

object IcsCourseParser {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    private val sectionRegex = Regex("""(?:\u7b2c)?\s*(\d{1,2})(?:\s*[-~\u81f3\u5230]\s*(\d{1,2}))?\s*\u8282""")

    fun parse(text: String, source: String = "ics_import"): IcsCourseParseResult {
        val errors = mutableListOf<String>()
        val events = extractEvents(unfoldLines(text))
        if (events.isEmpty()) {
            return IcsCourseParseResult(emptyList(), listOf("未找到ICS课程事件，请确认选择的是 .ics 日历文件"))
        }
        val parsedEvents = events.mapNotNull { event ->
            parseEvent(event).also { parsed ->
                if (parsed == null) errors += "跳过了一个缺少课程名、日期或节次的日历事件"
            }
        }
        val termStartDate = parsedEvents
            .flatMap { it.occurrenceDates }
            .minOrNull()
            ?: return IcsCourseParseResult(emptyList(), errors.ifEmpty { listOf("没有解析到有效课程事件") })

        val grouped = linkedMapOf<CourseKey, MutableSet<Int>>()
        parsedEvents.forEach { event ->
            val key = CourseKey(
                name = event.name,
                teacher = event.teacher,
                location = event.location,
                dayOfWeek = event.dayOfWeek,
                startSection = event.startSection,
                endSection = event.endSection
            )
            val weeks = grouped.getOrPut(key) { linkedSetOf() }
            event.occurrenceDates.forEach { date ->
                val week = ChronoUnit.WEEKS.between(termStartDate, date).toInt() + 1
                if (week > 0) weeks += week
            }
        }

        val courses = grouped.entries.mapIndexedNotNull { index, (key, weeksSet) ->
            val weeks = weeksSet.toList().sorted()
            if (weeks.isEmpty()) {
                null
            } else {
                Course(
                    name = key.name,
                    teacher = key.teacher,
                    location = key.location,
                    dayOfWeek = key.dayOfWeek,
                    startSection = key.startSection,
                    endSection = key.endSection,
                    startWeek = weeks.first(),
                    endWeek = weeks.last(),
                    weekType = CourseWeekParser.inferWeekType(weeks),
                    weeks = weeks,
                    colorIndex = index % 8,
                    source = source
                )
            }
        }

        return IcsCourseParseResult(courses, errors)
    }

    private fun unfoldLines(text: String): List<String> {
        val result = mutableListOf<String>()
        text.replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .forEach { line ->
                if ((line.startsWith(" ") || line.startsWith("\t")) && result.isNotEmpty()) {
                    result[result.lastIndex] = result.last() + line.drop(1)
                } else {
                    result += line
                }
            }
        return result
    }

    private fun extractEvents(lines: List<String>): List<List<String>> {
        val events = mutableListOf<List<String>>()
        var current: MutableList<String>? = null
        var nestedDepth = 0
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed == "BEGIN:VEVENT" -> {
                    current = mutableListOf()
                    nestedDepth = 0
                }
                trimmed == "END:VEVENT" -> {
                    current?.let { events += it.toList() }
                    current = null
                    nestedDepth = 0
                }
                current != null && trimmed.startsWith("BEGIN:") -> {
                    nestedDepth++
                }
                current != null && trimmed.startsWith("END:") && nestedDepth > 0 -> {
                    nestedDepth--
                }
                current != null && nestedDepth == 0 -> {
                    current?.add(line)
                }
            }
        }
        return events
    }

    private fun parseEvent(lines: List<String>): ParsedEvent? {
        val props = lines
            .mapNotNull { parseProperty(it) }
            .groupBy({ it.key }, { it.value })
            .mapValues { it.value.lastOrNull().orEmpty() }

        val name = props["SUMMARY"]?.let(::decodeText)?.trim().orEmpty()
        if (name.isBlank()) return null

        val startDateTime = props["DTSTART"]?.let(::parseDateTime) ?: return null
        val descriptionLines = props["DESCRIPTION"]
            ?.let(::decodeText)
            ?.replace("\r\n", "\n")
            ?.replace('\r', '\n')
            ?.lines()
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val section = descriptionLines.firstOrNull()?.let(::parseSectionRange) ?: return null
        val location = descriptionLines.getOrNull(1)
            ?.let(::cleanText)
            ?.takeIf { it.isNotBlank() }
            ?: props["LOCATION"]?.let(::decodeText)?.let(::cleanText).orEmpty()
        val teacher = descriptionLines.getOrNull(2)?.let(::cleanText).orEmpty()
        val occurrenceDates = expandDates(startDateTime, props["RRULE"]).ifEmpty {
            listOf(startDateTime.toLocalDate())
        }

        return ParsedEvent(
            name = cleanText(name),
            teacher = teacher,
            location = location,
            dayOfWeek = startDateTime.dayOfWeek.value,
            startSection = section.first,
            endSection = section.second,
            occurrenceDates = occurrenceDates
        )
    }

    private fun parseProperty(line: String): IcsProperty? {
        val separator = line.indexOf(':')
        if (separator < 0) return null
        val rawName = line.substring(0, separator)
        val key = rawName.substringBefore(';').uppercase()
        val value = line.substring(separator + 1)
        return IcsProperty(key, value)
    }

    private fun parseDateTime(value: String): LocalDateTime? {
        return runCatching {
            when {
                value.endsWith("Z") -> Instant.parse(toIsoInstant(value))
                    .atZone(zone)
                    .toLocalDateTime()
                value.length == 8 -> LocalDateTime.of(LocalDate.parse(value, dateFormatter), LocalTime.MIDNIGHT)
                else -> LocalDateTime.parse(value, dateTimeFormatter)
            }
        }.getOrNull()
    }

    private fun parseUntilInstant(value: String, start: LocalDateTime): Instant? {
        return runCatching {
            when {
                value.endsWith("Z") -> Instant.parse(toIsoInstant(value))
                value.length == 8 -> LocalDate.parse(value, dateFormatter)
                    .atTime(LocalTime.MAX)
                    .atZone(zone)
                    .toInstant()
                else -> LocalDateTime.parse(value, dateTimeFormatter)
                    .atZone(zone)
                    .toInstant()
            }
        }.getOrElse {
            start.atZone(zone).toInstant()
        }
    }

    private fun toIsoInstant(value: String): String {
        val normalized = value.removeSuffix("Z")
        return "${normalized.substring(0, 4)}-${normalized.substring(4, 6)}-${normalized.substring(6, 8)}T" +
            "${normalized.substring(9, 11)}:${normalized.substring(11, 13)}:${normalized.substring(13, 15)}Z"
    }

    private fun expandDates(start: LocalDateTime, rrule: String?): List<LocalDate> {
        if (rrule.isNullOrBlank()) return listOf(start.toLocalDate())
        val parts = rrule.split(';')
            .mapNotNull { part ->
                val index = part.indexOf('=')
                if (index < 0) null else part.substring(0, index).uppercase() to part.substring(index + 1)
            }
            .toMap()
        if (parts["FREQ"]?.uppercase() != "WEEKLY") return listOf(start.toLocalDate())

        val interval = parts["INTERVAL"]?.toLongOrNull()?.coerceAtLeast(1) ?: 1L
        val count = parts["COUNT"]?.toIntOrNull()
        val until = parts["UNTIL"]?.let { parseUntilInstant(it, start) }
        val dates = mutableListOf<LocalDate>()
        var current = start

        while (dates.size < 80) {
            val currentInstant = current.atZone(zone).toInstant()
            if (until != null && currentInstant > until) break
            dates += current.toLocalDate()
            if (count != null && dates.size >= count) break
            current = current.plusWeeks(interval)
        }

        return dates
    }

    private fun parseSectionRange(text: String): Pair<Int, Int>? {
        val match = sectionRegex.find(text) ?: return null
        val start = match.groupValues[1].toIntOrNull() ?: return null
        val end = match.groupValues.getOrNull(2)?.toIntOrNull() ?: start
        return start to end.coerceAtLeast(start)
    }

    private fun decodeText(value: String): String {
        val result = StringBuilder()
        var index = 0
        while (index < value.length) {
            val ch = value[index]
            if (ch == '\\' && index + 1 < value.length) {
                when (val next = value[index + 1]) {
                    'n', 'N' -> result.append('\n')
                    ',', ';', '\\' -> result.append(next)
                    else -> result.append(next)
                }
                index += 2
            } else {
                result.append(ch)
                index++
            }
        }
        return result.toString()
    }

    private fun cleanText(value: String): String =
        value.trim()
            .replace(Regex("""\s*,\s*"""), ",")
            .replace(Regex("""\s+"""), " ")

    private data class IcsProperty(
        val key: String,
        val value: String
    )

    private data class ParsedEvent(
        val name: String,
        val teacher: String,
        val location: String,
        val dayOfWeek: Int,
        val startSection: Int,
        val endSection: Int,
        val occurrenceDates: List<LocalDate>
    )

    private data class CourseKey(
        val name: String,
        val teacher: String,
        val location: String,
        val dayOfWeek: Int,
        val startSection: Int,
        val endSection: Int
    )
}
