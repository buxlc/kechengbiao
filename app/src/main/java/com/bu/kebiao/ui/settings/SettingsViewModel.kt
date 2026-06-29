package com.bu.kebiao.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.domain.model.AcademicWeekResolver
import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Semester
import com.bu.kebiao.domain.repository.ClassTimeRepository
import com.bu.kebiao.domain.repository.CourseRepository
import com.bu.kebiao.domain.repository.SemesterRepository
import com.bu.kebiao.liveupdate.CourseLiveUpdateScheduler
import com.bu.kebiao.ui.courseimport.SemesterCsvExporter
import com.bu.kebiao.widget.WidgetUpdateDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val classTimes: List<ClassTime> = emptyList(),
    val currentWeek: Int = 1,
    val viewingWeek: Int = 1,
    val totalWeeks: Int = 20,
    val semesterName: String = "",
    val semesterStartDate: Long = 0L,
    val themeMode: String = "system",
    val eduSchool: String = "",
    val eduAccount: String = "",
    val hasImported: Boolean = false,
    val courseCount: Int = 0,
    val editingTime: ClassTime? = null,
    val courseTextSize: String = "medium",
    val currentSemesterId: String = "default",
    val semesters: List<Semester> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val classTimeRepository: ClassTimeRepository,
    private val courseRepository: CourseRepository,
    private val semesterRepository: SemesterRepository,
    private val userPreferences: UserPreferences,
    private val liveUpdateScheduler: CourseLiveUpdateScheduler,
    private val widgetUpdateDispatcher: WidgetUpdateDispatcher
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(classTimeRepository.getAllClassTimes(), userPreferences.preferencesFlow) { times, prefs -> times to prefs },
        combine(courseRepository.getCourseCount(), semesterRepository.observeSemesters()) { count, semesters -> count to semesters }
    ) { timeAndPrefs, countAndSemesters ->
        val (times, prefs) = timeAndPrefs
        val (count, semesters) = countAndSemesters
        val viewingWeek = AcademicWeekResolver.normalizeViewingWeek(prefs.viewingWeek, prefs.totalWeeks)
        val currentWeek = AcademicWeekResolver.resolveCurrentWeek(
            viewingWeek = viewingWeek,
            totalWeeks = prefs.totalWeeks,
            semesterStartDateMillis = prefs.semesterStartDate
        )
        SettingsUiState(
            classTimes = times,
            currentWeek = currentWeek,
            viewingWeek = viewingWeek,
            totalWeeks = prefs.totalWeeks,
            semesterName = prefs.semesterName,
            semesterStartDate = prefs.semesterStartDate,
            themeMode = prefs.themeMode,
            eduSchool = prefs.eduSchool,
            eduAccount = prefs.eduAccount,
            hasImported = prefs.hasImported,
            courseCount = count,
            courseTextSize = prefs.courseTextSize,
            currentSemesterId = prefs.currentSemesterId,
            semesters = semesters
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateViewingWeek(week: Int) {
        viewModelScope.launch {
            userPreferences.updateViewingWeek(week)
        }
    }

    fun updateSemesterStartDate(startDateMillis: Long, totalWeeks: Int) {
        viewModelScope.launch {
            userPreferences.updateSemesterInfo(
                name = "",
                startDate = startDateMillis,
                totalWeeks = totalWeeks
            )
            val currentWeek = AcademicWeekResolver.resolveCurrentWeek(
                viewingWeek = uiState.value.viewingWeek,
                totalWeeks = totalWeeks,
                semesterStartDateMillis = startDateMillis
            )
            userPreferences.updateViewingWeek(currentWeek)
            liveUpdateScheduler.refreshNow()
            widgetUpdateDispatcher.refresh()
        }
    }

    fun createSemester(name: String = "新学期") {
        viewModelScope.launch {
            val semesterId = semesterRepository.createSemester(name)
            userPreferences.updateCurrentSemesterId(semesterId)
            userPreferences.setHasImported(false)
            liveUpdateScheduler.refreshNow()
            widgetUpdateDispatcher.refresh()
        }
    }

    fun switchSemester(semesterId: String) {
        viewModelScope.launch {
            userPreferences.updateCurrentSemesterId(semesterId)
            val hasCourses = uiState.value.semesters.firstOrNull { it.id == semesterId }?.courseCount.orZero() > 0
            userPreferences.setHasImported(hasCourses)
            liveUpdateScheduler.refreshNow()
            widgetUpdateDispatcher.refresh()
        }
    }

    fun renameSemester(semesterId: String, name: String) {
        viewModelScope.launch {
            semesterRepository.renameSemester(semesterId, name)
        }
    }

    fun deleteSemester(semesterId: String) {
        viewModelScope.launch {
            val currentSemesterId = uiState.value.currentSemesterId
            courseRepository.deleteSemesterCourses(semesterId)
            semesterRepository.deleteSemester(semesterId)
            if (semesterId == currentSemesterId) {
                val fallback = uiState.value.semesters.firstOrNull { it.id != semesterId }?.id.orEmpty()
                userPreferences.updateCurrentSemesterId(fallback)
                val fallbackCount = uiState.value.semesters.firstOrNull { it.id == fallback }?.courseCount.orZero()
                userPreferences.setHasImported(fallbackCount > 0)
            }
            liveUpdateScheduler.refreshNow()
            widgetUpdateDispatcher.refresh()
        }
    }

    fun exportCurrentSemesterCsv(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val state = uiState.value
            val semester = state.semesters.firstOrNull { it.id == state.currentSemesterId }
                ?: Semester(id = "default", name = state.semesterName.ifBlank { "默认学期" })
            val courses = courseRepository.getAllCoursesBySemester(semester.id).first()
            onReady(SemesterCsvExporter.export(semester, courses))
        }
    }

    fun exportSemesterCsv(semesterId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val semester = uiState.value.semesters.firstOrNull { it.id == semesterId } ?: return@launch
            val courses = courseRepository.getAllCoursesBySemester(semester.id).first()
            onReady(SemesterCsvExporter.export(semester, courses))
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferences.updateThemeMode(mode)
        }
    }

    fun updateClassTime(classTime: ClassTime) {
        viewModelScope.launch {
            classTimeRepository.updateClassTime(classTime)
            liveUpdateScheduler.refreshNow()
        }
    }

    fun setEditingTime(classTime: ClassTime?) {
        classTime ?: return
        // Managed by UI state
    }

    fun updateCourseTextSize(size: String) {
        viewModelScope.launch {
            userPreferences.updateCourseTextSize(size)
        }
    }

    fun clearEduData() {
        viewModelScope.launch {
            userPreferences.updateEduInfo("", "")
            courseRepository.deleteCurrentSemesterCourses()
            userPreferences.setHasImported(false)
            liveUpdateScheduler.refreshNow()
            widgetUpdateDispatcher.refresh()
        }
    }

    private fun Int?.orZero(): Int = this ?: 0
}
