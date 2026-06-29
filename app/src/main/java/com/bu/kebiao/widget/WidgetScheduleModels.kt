package com.bu.kebiao.widget

data class WidgetCourseTile(
    val title: String,
    val timeLine: String,
    val locationLine: String,
    val accentColor: Int
)

data class WidgetDayColumn(
    val label: String,
    val countText: String,
    val tiles: List<WidgetCourseTile> = emptyList(),
    val emptyText: String = "今天没有课程"
)

data class WidgetSmallScheduleSnapshot(
    val label: String,
    val countText: String,
    val courses: List<WidgetCourseCard> = emptyList()
)

data class WidgetScheduleSnapshot(
    val small: WidgetSmallScheduleSnapshot,
    val today: WidgetDayColumn,
    val tomorrow: WidgetDayColumn,
    val headerLeft: String = "大二上",
    val headerCenter: String = "",
    val headerRight: String = ""
)
