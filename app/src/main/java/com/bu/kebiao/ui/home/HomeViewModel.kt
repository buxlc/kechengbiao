package com.bu.kebiao.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.domain.model.AcademicWeekResolver
import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.repository.ClassTimeRepository
import com.bu.kebiao.domain.repository.CourseColorRepository
import com.bu.kebiao.domain.repository.CourseRepository
import com.bu.kebiao.liveupdate.CourseLiveUpdateScheduler
import com.bu.kebiao.widget.WidgetUpdateDispatcher
import com.bu.kebiao.ui.courseedit.CourseEditDraft
import com.bu.kebiao.ui.courseedit.CourseWeekParser
import com.bu.kebiao.ui.courseedit.toCourse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val courses: List<Course> = emptyList(),
    val todayCourses: List<Course> = emptyList(),
    val classTimes: List<ClassTime> = emptyList(),
    val colorMap: Map<String, Int> = emptyMap(),
    val currentWeek: Int = 1,
    val viewingWeek: Int = 1,
    val totalWeeks: Int = 20,
    val semesterStartDate: Long = 0L,
    val hasImported: Boolean = false,
    val isTodayView: Boolean = true,
    val selectedDay: Int = LocalDate.now().dayOfWeek.value,
    val courseTextSize: String = "medium"
)

private data class HomeCoreState(
    val courses: List<Course>,
    val classTimes: List<ClassTime>,
    val colorMap: Map<String, Int>,
    val currentWeek: Int,
    val viewingWeek: Int,
    val totalWeeks: Int,
    val semesterStartDate: Long,
    val hasImported: Boolean,
    val courseTextSize: String
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val classTimeRepository: ClassTimeRepository,
    private val courseColorRepository: CourseColorRepository,
    private val userPreferences: UserPreferences,
    private val liveUpdateScheduler: CourseLiveUpdateScheduler,
    private val widgetUpdateDispatcher: WidgetUpdateDispatcher
) : ViewModel() {

    private val isTodayViewFlow = MutableStateFlow(true)
    private val selectedDayFlow = MutableStateFlow(LocalDate.now().dayOfWeek.value)

    private val coreState = combine(
        combine(
            combine(courseRepository.getAllCourses(), classTimeRepository.getAllClassTimes()) { courses, classTimes ->
                courses to classTimes
            },
            courseColorRepository.observeColorMap()
        ) { courseAndTime, colorMap ->
            Triple(courseAndTime.first, courseAndTime.second, colorMap)
        },
        userPreferences.preferencesFlow
    ) { core, prefs ->
        val viewingWeek = AcademicWeekResolver.normalizeViewingWeek(prefs.viewingWeek, prefs.totalWeeks)
        val currentWeek = AcademicWeekResolver.resolveCurrentWeek(
            viewingWeek = viewingWeek,
            totalWeeks = prefs.totalWeeks,
            semesterStartDateMillis = prefs.semesterStartDate
        )
        HomeCoreState(
            courses = core.first.map { resolveCourseColor(it, core.third) },
            classTimes = core.second.sortedBy { it.sectionNumber },
            colorMap = core.third,
            currentWeek = currentWeek,
            viewingWeek = viewingWeek,
            totalWeeks = prefs.totalWeeks,
            semesterStartDate = prefs.semesterStartDate,
            hasImported = prefs.hasImported,
            courseTextSize = prefs.courseTextSize
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(coreState, isTodayViewFlow) { core, isTodayView -> core to isTodayView },
        selectedDayFlow
    ) { coreAndView, selectedDay ->
        val (core, isTodayView) = coreAndView
        val todayCourses = core.courses
            .filter { it.dayOfWeek == selectedDay && it.isActiveInWeek(core.viewingWeek) }
            .sortedWith(compareBy<Course> { it.startSection }.thenBy { it.endSection }.thenBy { it.name })

        HomeUiState(
            courses = core.courses,
            todayCourses = todayCourses,
            classTimes = core.classTimes,
            colorMap = core.colorMap,
            currentWeek = core.currentWeek,
            viewingWeek = core.viewingWeek,
            totalWeeks = core.totalWeeks,
            semesterStartDate = core.semesterStartDate,
            hasImported = core.hasImported,
            isTodayView = isTodayView,
            selectedDay = selectedDay,
            courseTextSize = core.courseTextSize
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun setTodayView(isToday: Boolean) {
        isTodayViewFlow.value = isToday
    }

    fun setSelectedDay(day: Int) {
        selectedDayFlow.value = day.coerceIn(1, 7)
    }

    fun prevWeek() {
        val viewingWeek = uiState.value.viewingWeek
        if (viewingWeek > 1) {
            viewModelScope.launch {
                userPreferences.updateViewingWeek(viewingWeek - 1)
            }
        }
    }

    fun nextWeek() {
        val viewingWeek = uiState.value.viewingWeek
        val totalWeeks = uiState.value.totalWeeks
        if (viewingWeek < totalWeeks) {
            viewModelScope.launch {
                userPreferences.updateViewingWeek(viewingWeek + 1)
            }
        }
    }

    fun setCurrentWeek(week: Int) {
        val safeWeek = week.coerceIn(1, uiState.value.totalWeeks.coerceAtLeast(1))
        viewModelScope.launch {
            userPreferences.updateViewingWeek(safeWeek)
        }
    }

    fun saveCourseDraft(draft: CourseEditDraft) {
        val currentState = uiState.value
        val name = draft.name.trim()
        if (name.isBlank() || draft.startSection <= 0 || draft.endSection < draft.startSection) return

        viewModelScope.launch {
            val parsedWeeks = CourseWeekParser.parseWeeksText(draft.weeksText, currentState.totalWeeks)
            val source = if (draft.id > 0) {
                courseRepository.getCourseById(draft.id)?.source ?: "manual"
            } else {
                "manual"
            }
            val finalColor = currentState.colorMap[name] ?: draft.colorIndex
            val course = draft.copy(colorIndex = finalColor).toCourse(parsedWeeks, source)

            if (course.id > 0) {
                courseRepository.updateCourse(course)
            } else {
                courseRepository.insertCourse(course)
            }

            courseColorRepository.upsertColor(name, finalColor)
            courseRepository.updateColorByCourseName(name, finalColor)
            liveUpdateScheduler.refreshNow()
            widgetUpdateDispatcher.refresh()
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            courseRepository.deleteCourse(course)
            liveUpdateScheduler.refreshNow()
            widgetUpdateDispatcher.refresh()
        }
    }
}
