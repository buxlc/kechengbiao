package com.bu.kebiao.ui.courseimport

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.model.WeekType
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

private const val TAG = "CourseJsBridge"

class CourseJsBridge(
    private val context: Context,
    private val webView: WebView,
    private val onCoursesParsed: (List<Course>) -> Unit,
    private val onImportFinished: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var currentToast: Toast? = null
    private data class SectionRange(val start: Int, val end: Int)

    @JavascriptInterface
    fun showToast(message: String) {
        Log.d(TAG, "showToast: $message")
        handler.post {
            currentToast?.cancel()
            val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            toast.show()
            currentToast = toast
        }
    }

    @JavascriptInterface
    fun showAlert(title: String, content: String, confirmText: String, promiseId: String) {
        Log.d(TAG, "showAlert: title=$title, contentLength=${content.length}, confirmText=$confirmText, promiseId=$promiseId")
        handler.post {
            // Auto-confirm for now - in future could show native dialog
            resolveJsPromise(promiseId, "true")
        }
    }

    @JavascriptInterface
    fun saveImportedCourses(jsonString: String, promiseId: String) {
        Log.d(TAG, "saveImportedCourses called, data length=${jsonString.length}, promiseId=$promiseId")
        handler.post {
            try {
                val courses = parseCoursesFromJson(jsonString)
                Log.d(TAG, "Parsed ${courses.size} courses")
                if (courses.isNotEmpty()) {
                    onCoursesParsed(courses)
                    resolveJsPromise(promiseId, "true")
                } else {
                    showToast("\u672a\u627e\u5230\u8bfe\u7a0b\u6570\u636e")
                    resolveJsPromise(promiseId, "false")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Parse error", e)
                showToast("\u89e3\u6790\u5931\u8d25: ${e.message}")
                rejectJsPromise(promiseId, e.message ?: "\u89e3\u6790\u5931\u8d25")
            }
        }
    }

    @JavascriptInterface
    fun saveCourseData(jsonString: String) {
        Log.d(TAG, "saveCourseData called, data length=${jsonString.length}")
        handler.post {
            try {
                val courses = parseCoursesFromJson(jsonString)
                Log.d(TAG, "Parsed ${courses.size} courses via saveCourseData")
                if (courses.isNotEmpty()) {
                    onCoursesParsed(courses)
                    showToast("\u5df2\u89e3\u6790 ${courses.size} \u95e8\u8bfe\u7a0b")
                } else {
                    showToast("\u672a\u627e\u5230\u8bfe\u7a0b\u6570\u636e")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Parse error in saveCourseData", e)
                showToast("\u89e3\u6790\u5931\u8d25: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun savePresetTimeSlots(jsonString: String, promiseId: String) {
        Log.d(TAG, "savePresetTimeSlots called (not implemented yet), data length=${jsonString.length}")
        handler.post {
            resolveJsPromise(promiseId, "true")
        }
    }

    @JavascriptInterface
    fun saveCourseConfig(jsonString: String, promiseId: String) {
        Log.d(TAG, "saveCourseConfig called (not implemented yet), data length=${jsonString.length}")
        handler.post {
            resolveJsPromise(promiseId, "true")
        }
    }

    @JavascriptInterface
    fun notifyTaskCompletion() {
        Log.d(TAG, "notifyTaskCompletion called")
        handler.post {
            onImportFinished()
        }
    }

    private fun parseCoursesFromJson(jsonString: String): List<Course> {
        val courses = mutableListOf<Course>()
        val jsonArray = when (val root = JSONTokener(jsonString).nextValue()) {
            is JSONArray -> root
            is JSONObject -> root.optJSONArray("courses")
                ?: root.optJSONArray("courseList")
                ?: root.optJSONObject("data")?.optJSONArray("courses")
                ?: root.optJSONObject("data")?.optJSONArray("courseList")
                ?: root.optJSONArray("data")
                ?: JSONArray()
            else -> JSONArray()
        }

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val name = obj.optFirstString("name", "courseName", "kcmc", "title")
            if (name.isBlank()) {
                Log.d(TAG, "Skipping course $i: empty name")
                continue
            }

            val day = obj.optFirstInt("day", "dayOfWeek", "weekday", "weekDay", "xqj") ?: 0
            if (day !in 1..7) {
                Log.d(TAG, "Skipping course $i ($name): invalid day=$day")
                continue
            }

            val teacher = obj.optFirstString("teacher", "teachers", "teacherName", "xm")
            val position = obj.optFirstString("position", "location", "classroom", "room", "place", "cdmc")
            val weeks = parseWeeks(obj)
            val sectionRanges = parseSectionRanges(obj)

            val startWeek = weeks.firstOrNull() ?: 1
            val endWeek = weeks.lastOrNull() ?: 20
            val weekType = inferWeekType(weeks)

            val isCustomTime = obj.optBoolean("isCustomTime", false)
            val normalizedRanges = if (sectionRanges.isNotEmpty()) {
                sectionRanges
            } else if (isCustomTime) {
                listOf(SectionRange(1, 1))
            } else {
                emptyList()
            }

            if (normalizedRanges.isEmpty()) {
                Log.d(TAG, "Skipping course $i ($name): invalid sections")
                continue
            }

            normalizedRanges.forEach { range ->
                courses.add(
                    Course(
                        name = name,
                        teacher = teacher,
                        location = position,
                        dayOfWeek = day,
                        startSection = range.start,
                        endSection = range.end,
                        startWeek = startWeek,
                        endWeek = endWeek,
                        weekType = weekType,
                        weeks = weeks,
                        colorIndex = courses.size % 8,
                        source = "edu_web"
                    )
                )
            }
        }
        return courses
    }

    private fun parseSectionRanges(obj: JSONObject): List<SectionRange> {
        val start = obj.optFirstInt("startSection", "startSec", "startNode", "start", "ps")
        val end = obj.optFirstInt("endSection", "endSec", "endNode", "end", "pe")
        val duration = obj.optFirstInt("sectionCount", "duration", "sessionLast", "length", "span")

        if (start != null && end != null) {
            return if (end >= start && start > 0) {
                listOf(SectionRange(start, end))
            } else if (duration != null && duration > 0) {
                listOf(SectionRange(start, start + duration - 1))
            } else {
                emptyList()
            }
        }

        if (start != null && duration != null && duration > 0) {
            return listOf(SectionRange(start, start + duration - 1))
        }

        obj.optSectionArray("sections").takeIf { it.isNotEmpty() }?.let { return it }
        obj.optSectionArray("nodes").takeIf { it.isNotEmpty() }?.let { return it }
        obj.optSectionArray("sectionList").takeIf { it.isNotEmpty() }?.let { return it }

        val sectionKeys = arrayOf(
            "section",
            "sections",
            "sectionRange",
            "sectionText",
            "jcs",
            "jc",
            "period",
            "periodFormat",
            "time"
        )
        sectionKeys.forEach { key ->
            parseSectionRangesText(obj.optString(key, "")).takeIf { it.isNotEmpty() }?.let { return it }
        }

        parseSectionRangesText(obj.optString("weeksStr", ""), requireSectionMarker = true)
            .takeIf { it.isNotEmpty() }
            ?.let { return it }
        parseSectionRangesText(obj.optString("zcd", ""), requireSectionMarker = true)
            .takeIf { it.isNotEmpty() }
            ?.let { return it }

        return emptyList()
    }

    private fun JSONObject.optSectionArray(key: String): List<SectionRange> {
        val array = optJSONArray(key) ?: return emptyList()
        val sections = mutableListOf<Int>()
        for (index in 0 until array.length()) {
            val value = array.opt(index)
            when (value) {
                is Number -> sections.add(value.toInt())
                is String -> value.toIntOrNull()?.let { sections.add(it) }
                is JSONObject -> {
                    val nestedStart = value.optFirstInt("startSection", "start", "startNode")
                    val nestedEnd = value.optFirstInt("endSection", "end", "endNode")
                    if (nestedStart != null && nestedEnd != null && nestedStart > 0 && nestedEnd >= nestedStart) {
                        sections.addAll(nestedStart..nestedEnd)
                    } else {
                        value.optFirstInt("section", "node", "number", "index")?.let { sections.add(it) }
                    }
                }
            }
        }
        return groupSections(sections)
    }

    private fun parseSectionRangesText(text: String, requireSectionMarker: Boolean = false): List<SectionRange> {
        val clean = text.trim()
        if (clean.isBlank()) return emptyList()

        val normalized = clean
            .replace("（", "(")
            .replace("）", ")")
            .replace("【", "[")
            .replace("】", "]")

        val explicitGroups = Regex("""[\[(（]?\s*第?\s*([0-9,\-~—–至到]+)\s*节\s*[\])）]?""")
            .findAll(normalized)
            .map { it.groupValues[1] }
            .toList()

        val extractedSections = when {
            explicitGroups.isNotEmpty() -> explicitGroups.flatMap(::parseSectionNumbers)
            normalized.contains("节") -> parseSectionNumbers(normalized.substringBefore("周").substringBefore("星期"))
            requireSectionMarker -> emptyList()
            else -> parseSectionNumbers(normalized)
        }

        return groupSections(extractedSections)
    }

    private fun parseSectionNumbers(text: String): List<Int> {
        if (text.isBlank()) return emptyList()
        val sections = mutableListOf<Int>()
        Regex("""\d{1,2}\s*(?:-|~|—|–|至|到)\s*\d{1,2}|\d{1,2}""")
            .findAll(text)
            .forEach sectionLoop@{ match ->
                val value = match.value.trim()
                if (value.contains(Regex("""-|~|—|–|至|到"""))) {
                    val parts = value.split(Regex("""\s*(?:-|~|—|–|至|到)\s*"""))
                    val start = parts.getOrNull(0)?.toIntOrNull() ?: return@sectionLoop
                    val end = parts.getOrNull(1)?.toIntOrNull() ?: return@sectionLoop
                    if (start > 0 && end >= start) {
                        sections.addAll(start..end)
                    }
                } else {
                    value.toIntOrNull()?.takeIf { it > 0 }?.let(sections::add)
                }
            }
        return sections
    }

    private fun groupSections(sections: List<Int>): List<SectionRange> {
        val sorted = sections.filter { it > 0 }.distinct().sorted()
        if (sorted.isEmpty()) return emptyList()

        val ranges = mutableListOf<SectionRange>()
        var start = sorted.first()
        var end = start
        for (index in 1 until sorted.size) {
            val current = sorted[index]
            if (current == end + 1) {
                end = current
            } else {
                ranges.add(SectionRange(start, end))
                start = current
                end = current
            }
        }
        ranges.add(SectionRange(start, end))
        return ranges
    }

    private fun parseWeeks(obj: JSONObject): List<Int> {
        obj.optJSONArray("weeks")?.let { weeksArray ->
            if (weeksArray.length() > 0) {
                return (0 until weeksArray.length())
                    .flatMap { index -> parseWeeksText(weeksArray.opt(index).toString()) }
                    .sorted()
                    .distinct()
                    .ifEmpty { (1..20).toList() }
            }
        }

        val weeksText = obj.optFirstString("weeks", "week", "weekRange", "zcd", "weeksStr")
        if (weeksText.isNotBlank()) {
            return parseWeeksText(weeksText).ifEmpty { (1..20).toList() }
        }

        val startWeek = obj.optFirstInt("startWeek", "beginWeek")
        val endWeek = obj.optFirstInt("endWeek", "finishWeek")
        if (startWeek != null && endWeek != null && startWeek > 0 && endWeek >= startWeek) {
            return (startWeek..endWeek).toList()
        }

        return (1..20).toList()
    }

    private fun parseWeeksText(text: String): List<Int> {
        val clean = text
            .replace("（", "(")
            .replace("）", ")")
            .replace("【", "[")
            .replace("】", "]")
            .replace(Regex("""[\[(（【]?\s*第?\s*[\d,\-~—–至到]+\s*节\s*[\])）】]?\s*"""), "")
            .replace("周", "")
            .trim()
        if (clean.isBlank()) return emptyList()
        val weeks = mutableListOf<Int>()
        clean.split(",", "，", ";", "；", "、").forEach segmentLoop@{ segment ->
            val part = segment.trim()
            if (part.isBlank()) return@segmentLoop
            val oddOnly = part.contains("单")
            val evenOnly = part.contains("双")

            val rangeMatches = Regex("""(\d{1,2})\s*(?:-|~|—|–|至|到)\s*(\d{1,2})""").findAll(part).toList()
            if (rangeMatches.isNotEmpty()) {
                rangeMatches.forEach rangeLoop@{ range ->
                    val start = range.groupValues[1].toIntOrNull() ?: return@rangeLoop
                    val end = range.groupValues[2].toIntOrNull() ?: return@rangeLoop
                    for (week in start..end) {
                        if (oddOnly && week % 2 == 0) continue
                        if (evenOnly && week % 2 != 0) continue
                        weeks.add(week)
                    }
                }
            } else {
                Regex("""\d{1,2}""").find(part)?.value?.toIntOrNull()?.let { weeks.add(it) }
            }
        }
        return weeks.filter { it > 0 }.sorted().distinct()
    }

    private fun JSONObject.optFirstString(vararg keys: String): String {
        keys.forEach { key ->
            val value = optString(key, "").trim()
            if (value.isNotBlank() && value != "null") return value
        }
        return ""
    }

    private fun JSONObject.optFirstInt(vararg keys: String): Int? {
        keys.forEach { key ->
            if (!has(key) || isNull(key)) return@forEach
            val value = opt(key)
            when (value) {
                is Number -> return value.toInt()
                is String -> value.trim().toIntOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun inferWeekType(weeks: List<Int>): WeekType {
        if (weeks.isEmpty()) return WeekType.ALL
        val allOdd = weeks.all { it % 2 == 1 }
        val allEven = weeks.all { it % 2 == 0 }
        return when {
            allOdd -> WeekType.ODD
            allEven -> WeekType.EVEN
            else -> WeekType.ALL
        }
    }

    private fun resolveJsPromise(promiseId: String, result: String) {
        handler.post {
            try {
                webView.evaluateJavascript("window._resolveAndroidPromise('$promiseId', $result);", null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve promise", e)
            }
        }
    }

    private fun rejectJsPromise(promiseId: String, error: String) {
        handler.post {
            try {
                val escaped = error.replace("'", "\\'")
                webView.evaluateJavascript("window._rejectAndroidPromise('$promiseId', '$escaped');", null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reject promise", e)
            }
        }
    }
}
