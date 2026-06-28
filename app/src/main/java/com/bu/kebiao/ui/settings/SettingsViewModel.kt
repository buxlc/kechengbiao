package com.bu.kebiao.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.repository.ClassTimeRepository
import com.bu.kebiao.domain.repository.CourseRepository
import com.bu.kebiao.liveupdate.CourseLiveUpdateScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val classTimes: List<ClassTime> = emptyList(),
    val currentWeek: Int = 1,
    val totalWeeks: Int = 20,
    val semesterName: String = "",
    val semesterStartDate: Long = 0L,
    val themeMode: String = "system",
    val eduSchool: String = "",
    val eduAccount: String = "",
    val hasImported: Boolean = false,
    val courseCount: Int = 0,
    val editingTime: ClassTime? = null,
    val courseTextSize: String = "medium"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val classTimeRepository: ClassTimeRepository,
    private val courseRepository: CourseRepository,
    private val userPreferences: UserPreferences,
    private val liveUpdateScheduler: CourseLiveUpdateScheduler
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        classTimeRepository.getAllClassTimes(),
        userPreferences.preferencesFlow,
        courseRepository.getCourseCount()
    ) { times, prefs, count ->
        SettingsUiState(
            classTimes = times,
            currentWeek = prefs.currentWeek,
            totalWeeks = prefs.totalWeeks,
            semesterName = prefs.semesterName,
            semesterStartDate = prefs.semesterStartDate,
            themeMode = prefs.themeMode,
            eduSchool = prefs.eduSchool,
            eduAccount = prefs.eduAccount,
            hasImported = prefs.hasImported,
            courseCount = count,
            courseTextSize = prefs.courseTextSize
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateCurrentWeek(week: Int) {
        viewModelScope.launch {
            userPreferences.updateCurrentWeek(week)
            liveUpdateScheduler.refreshNow()
        }
    }

    fun updateSemesterStartDate(startDateMillis: Long, totalWeeks: Int) {
        viewModelScope.launch {
            userPreferences.updateSemesterInfo(
                name = "",
                startDate = startDateMillis,
                totalWeeks = totalWeeks
            )
            // Auto-calculate current week based on start date
            val now = System.currentTimeMillis()
            val diffDays = ((now - startDateMillis) / (1000L * 60 * 60 * 24)).toInt()
            val calculatedWeek = (diffDays / 7) + 1
            if (calculatedWeek in 1..totalWeeks) {
                userPreferences.updateCurrentWeek(calculatedWeek)
            }
            liveUpdateScheduler.refreshNow()
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
            courseRepository.deleteBySource("edu_web")
            courseRepository.deleteBySource("excel")
            courseRepository.deleteBySource("pdf")
            userPreferences.setHasImported(false)
            liveUpdateScheduler.refreshNow()
        }
    }
}
