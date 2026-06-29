package com.bu.kebiao.widget

import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.domain.repository.ClassTimeRepository
import com.bu.kebiao.domain.repository.CourseRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class WidgetDataLoader(
    private val courseRepository: CourseRepository,
    private val classTimeRepository: ClassTimeRepository,
    private val userPreferences: UserPreferences
) {
    suspend fun load(now: LocalDateTime = LocalDateTime.now()): WidgetCourseSnapshot {
        return runCatching {
            val prefs = userPreferences.preferencesFlow.first()
            val widgetWeek = prefs.resolveWidgetWeek(now)
            val courses = courseRepository.getCoursesByWeek(widgetWeek).first()
            val classTimes = classTimeRepository.getAllClassTimes().first()
            val todayCourses = courses.filter { it.dayOfWeek == now.dayOfWeek.value }
            WidgetContentProvider.buildSnapshot(
                courses = todayCourses,
                classTimes = classTimes,
                currentWeek = widgetWeek,
                now = now
            )
        }.getOrElse {
            WidgetCourseSnapshot(
                primary = loadingFailedCard(),
                footer = "数据暂不可用"
            )
        }
    }

    suspend fun loadSchedule(now: LocalDateTime = LocalDateTime.now()): WidgetScheduleSnapshot {
        return runCatching {
            val prefs = userPreferences.preferencesFlow.first()
            val widgetWeek = prefs.resolveWidgetWeek(now)
            val courses = courseRepository.getCoursesByWeek(widgetWeek).first()
            val classTimes = classTimeRepository.getAllClassTimes().first()
            WidgetScheduleMapper.build(
                courses = courses,
                classTimes = classTimes,
                todayDayOfWeek = now.dayOfWeek.value,
                currentWeek = widgetWeek,
                todayDate = now.toLocalDate()
            )
        }.getOrElse {
            WidgetScheduleSnapshot(
                small = WidgetSmallScheduleSnapshot(
                    label = "",
                    countText = "",
                    courses = emptyList()
                ),
                today = WidgetDayColumn(
                    label = "今天",
                    countText = "数据暂不可用",
                    emptyText = "今天无课"
                ),
                tomorrow = WidgetDayColumn(
                    label = "明天",
                    countText = "数据暂不可用",
                    emptyText = "明天无课"
                )
            )
        }
    }

    private fun loadingFailedCard(): WidgetCourseCard =
        WidgetCourseCard(
            stateLabel = "加载失败",
            courseName = "Bu课表",
            detailLine = "暂时读不到课表",
            extraLine = "稍后重试",
            accentColor = 0xFFD8DCE4.toInt()
        )

    private fun UserPreferences.Preferences.resolveWidgetWeek(now: LocalDateTime): Int =
        WidgetWeekResolver.resolve(
            selectedWeek = viewingWeek,
            totalWeeks = totalWeeks,
            semesterStartDateMillis = semesterStartDate,
            now = now
        )
}
