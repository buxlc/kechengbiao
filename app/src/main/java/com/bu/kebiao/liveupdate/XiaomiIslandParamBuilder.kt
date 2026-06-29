package com.bu.kebiao.liveupdate

import org.json.JSONObject

object XiaomiIslandParamBuilder {
    private const val BUSINESS_COURSE_SCHEDULE = "course_schedule"
    private const val COLOR_PRIMARY = "#2F6BFF"
    private const val COLOR_BREAK = "#00A37A"

    fun build(
        state: CourseLiveUpdateState,
        text: CourseLiveUpdateText
    ): String? {
        if (state is CourseLiveUpdateState.Hidden) return null

        val visual = state.toVisualData(text)
        val paramV2 = JSONObject()
            .put("protocol", 1)
            .put("business", BUSINESS_COURSE_SCHEDULE)
            .put("enableFloat", true)
            .put("updatable", true)
            .put("ticker", text.title)
            .put("aodTitle", text.title)
            .put("param_island", visual.toIslandJson())
            .put("baseInfo", visual.toBaseInfoJson())
            .put("hintInfo", visual.toHintInfoJson())
            .put("extraInfo", visual.toExtraInfoJson())

        return JSONObject()
            .put("param_v2", paramV2)
            .toString()
    }

    private fun CourseLiveUpdateState.toVisualData(text: CourseLiveUpdateText): XiaomiIslandVisualData {
        val course = when (this) {
            CourseLiveUpdateState.Hidden -> null
            is CourseLiveUpdateState.Upcoming -> course
            is CourseLiveUpdateState.InClass -> course
            is CourseLiveUpdateState.SectionBreak -> course
        }
        val location = course?.location?.cleanOrFallback("地点待定").orEmpty()
        val teacher = course?.teacher?.cleanOrFallback("未填写").orEmpty()
        val courseName = course?.name?.cleanOrFallback("课程").orEmpty()

        return when (this) {
            CourseLiveUpdateState.Hidden -> XiaomiIslandVisualData(
                title = text.title,
                frontTitle = "",
                content = text.content,
                subContent = "",
                color = COLOR_PRIMARY
            )
            is CourseLiveUpdateState.Upcoming -> XiaomiIslandVisualData(
                title = courseName,
                frontTitle = text.title,
                content = "${startTimeText}开始 · 还有${minutesUntilStart}分钟",
                subContent = "$location · $teacher",
                color = COLOR_PRIMARY
            )
            is CourseLiveUpdateState.InClass -> XiaomiIslandVisualData(
                title = courseName,
                frontTitle = text.title,
                content = "第${sectionNumber}节 · 还有${minutesUntilSectionEnd}分钟下课",
                subContent = "$location · $teacher",
                color = COLOR_PRIMARY,
                progress = text.progress,
                progressMax = text.progressMax.takeIf { it > 0 }
            )
            is CourseLiveUpdateState.SectionBreak -> XiaomiIslandVisualData(
                title = courseName,
                frontTitle = text.title,
                content = "第${nextSectionNumber}节${nextSectionStartTimeText}开始 · 还有${minutesUntilNextSection}分钟",
                subContent = "$location · $teacher",
                color = COLOR_BREAK
            )
        }
    }

    private fun XiaomiIslandVisualData.toIslandJson(): JSONObject =
        JSONObject()
            .put("islandProperty", 1)
            .put(
                "bigIslandArea",
                JSONObject()
                    .put("imageTextInfoLeft", toImageTextJson(useHighLight = true))
                    .put("imageTextInfoRight", toSubTextJson())
            )
            .put(
                "smallIslandArea",
                JSONObject()
                    .put("textInfo", toSmallTextJson())
            )
            .put(
                "shareData",
                JSONObject().put("title", title)
            )

    private fun XiaomiIslandVisualData.toImageTextJson(useHighLight: Boolean): JSONObject =
        JSONObject()
            .put("type", 1)
            .put(
                "miui.focus.paramtextInfo",
                JSONObject()
                    .put("frontTitle", frontTitle)
                    .put("title", title)
                    .put("content", content)
                    .put("useHighLight", useHighLight)
                    .apply {
                        progress?.let { put("progress", it) }
                        progressMax?.let { put("progressMax", it) }
                    }
            )

    private fun XiaomiIslandVisualData.toSubTextJson(): JSONObject =
        JSONObject()
            .put("type", 1)
            .put(
                "miui.focus.paramtextInfo",
                JSONObject()
                    .put("title", subContent)
                    .put("content", content)
                    .put("useHighLight", false)
            )

    private fun XiaomiIslandVisualData.toSmallTextJson(): JSONObject =
        JSONObject()
            .put("frontTitle", frontTitle)
            .put("title", title)
            .put("content", content)
            .put("useHighLight", true)

    private fun XiaomiIslandVisualData.toBaseInfoJson(): JSONObject =
        JSONObject()
            .put("title", frontTitle)
            .put("content", "$title · $content")
            .put("colorTitle", color)
            .put("type", 2)

    private fun XiaomiIslandVisualData.toHintInfoJson(): JSONObject =
        JSONObject()
            .put("type", 1)
            .put("title", content)

    private fun XiaomiIslandVisualData.toExtraInfoJson(): JSONObject =
        JSONObject()
            .put("courseName", title)
            .put("courseDetail", subContent)
            .put("state", frontTitle)

    private fun String.cleanOrFallback(fallback: String): String = trim().takeIf { it.isNotEmpty() } ?: fallback

    private data class XiaomiIslandVisualData(
        val title: String,
        val frontTitle: String,
        val content: String,
        val subContent: String,
        val color: String,
        val progress: Int? = null,
        val progressMax: Int? = null
    )
}
