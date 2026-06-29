package com.bu.kebiao.widget

data class WidgetCourseCard(
    val stateLabel: String,
    val courseName: String,
    val detailLine: String,
    val extraLine: String,
    val accentColor: Int,
    val progress: Int = 0,
    val progressMax: Int = 0
)

data class WidgetCourseSnapshot(
    val primary: WidgetCourseCard,
    val secondary: WidgetCourseCard? = null,
    val footer: String = ""
)
