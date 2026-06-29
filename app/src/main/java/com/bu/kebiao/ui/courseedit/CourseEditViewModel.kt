package com.bu.kebiao.ui.courseedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.domain.repository.CourseColorRepository
import com.bu.kebiao.domain.repository.CourseRepository
import com.bu.kebiao.widget.WidgetUpdateDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseEditUiState(
    val draft: CourseEditDraft = CourseEditDraft(),
    val totalWeeks: Int = 20,
    val colorMap: Map<String, Int> = emptyMap(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class CourseEditCoreState(
    val draft: CourseEditDraft,
    val totalWeeks: Int,
    val colorMap: Map<String, Int>
)

data class CourseEditStatusState(
    val isLoading: Boolean,
    val isSaving: Boolean,
    val saveSuccess: Boolean,
    val errorMessage: String?
)

@HiltViewModel
class CourseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository,
    private val courseColorRepository: CourseColorRepository,
    userPreferences: UserPreferences,
    private val widgetUpdateDispatcher: WidgetUpdateDispatcher
) : ViewModel() {

    private val courseId: Long = savedStateHandle["courseId"] ?: 0L

    private val draftState = MutableStateFlow(
        CourseEditDraft(
            id = courseId,
            name = savedStateHandle["name"] ?: "",
            teacher = savedStateHandle["teacher"] ?: "",
            location = savedStateHandle["location"] ?: "",
            dayOfWeek = savedStateHandle["day"] ?: 1,
            startSection = savedStateHandle["start"] ?: 1,
            endSection = savedStateHandle["end"] ?: 2,
            weeksText = savedStateHandle["weeks"] ?: "1-20",
            colorIndex = savedStateHandle["color"] ?: 0
        )
    )
    private val loadingState = MutableStateFlow(true)
    private val savingState = MutableStateFlow(false)
    private val saveSuccessState = MutableStateFlow(false)
    private val errorState = MutableStateFlow<String?>(null)

    private val coreState = combine(
        combine(draftState, userPreferences.preferencesFlow) { draft, prefs ->
            draft to prefs
        },
        courseColorRepository.observeColorMap()
    ) { draftAndPrefs, colorMap ->
        val (draft, prefs) = draftAndPrefs
        CourseEditCoreState(
            draft = draft,
            totalWeeks = prefs.totalWeeks,
            colorMap = colorMap
        )
    }

    private val statusState = combine(
        loadingState,
        savingState,
        saveSuccessState,
        errorState
    ) { isLoading, isSaving, saveSuccess, errorMessage ->
        CourseEditStatusState(
            isLoading = isLoading,
            isSaving = isSaving,
            saveSuccess = saveSuccess,
            errorMessage = errorMessage
        )
    }

    val uiState: StateFlow<CourseEditUiState> = combine(
        coreState,
        statusState
    ) { core, status ->
        CourseEditUiState(
            draft = core.draft,
            totalWeeks = core.totalWeeks,
            colorMap = core.colorMap,
            isEditMode = courseId > 0,
            isLoading = status.isLoading,
            isSaving = status.isSaving,
            saveSuccess = status.saveSuccess,
            errorMessage = status.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CourseEditUiState()
    )

    init {
        viewModelScope.launch {
            val currentDraft = draftState.value
            if (courseId > 0 && currentDraft.name.isBlank()) {
                val course = courseRepository.getCourseById(courseId)
                if (course != null) {
                    val mappedColor = courseColorRepository.getColorIndex(course.name) ?: course.colorIndex
                    draftState.value = course.toEditDraft(
                        defaultTotalWeeks = 20,
                        resolvedColorIndex = mappedColor
                    )
                }
            } else if (currentDraft.name.isNotBlank()) {
                val mappedColor = courseColorRepository.getColorIndex(currentDraft.name)
                if (mappedColor != null) {
                    draftState.value = currentDraft.copy(colorIndex = mappedColor)
                }
            }
            loadingState.value = false
        }
    }

    fun updateDraft(transform: (CourseEditDraft) -> CourseEditDraft) {
        draftState.update(transform)
    }

    fun clearError() {
        errorState.value = null
    }

    fun save() {
        val state = uiState.value
        val draft = state.draft
        val name = draft.normalizedName()

        if (name.isBlank()) {
            errorState.value = "\u8bf7\u5148\u586b\u5199\u8bfe\u7a0b\u540d\u79f0"
            return
        }
        if (draft.startSection <= 0 || draft.endSection < draft.startSection) {
            errorState.value = "\u8282\u6b21\u8303\u56f4\u4e0d\u6b63\u786e"
            return
        }

        val parsedWeeks = CourseWeekParser.parseWeeksText(draft.weeksText, state.totalWeeks)
        if (parsedWeeks.isEmpty()) {
            errorState.value = "\u5468\u6b21\u683c\u5f0f\u4e0d\u6b63\u786e"
            return
        }

        viewModelScope.launch {
            savingState.value = true
            errorState.value = null
            try {
                val source = if (draft.id > 0) {
                    courseRepository.getCourseById(draft.id)?.source ?: "manual"
                } else {
                    "manual"
                }
                val finalColor = state.colorMap[name] ?: draft.colorIndex
                val course = draft.copy(colorIndex = finalColor).toCourse(
                    weeks = parsedWeeks,
                    source = source
                )
                if (course.id > 0) {
                    courseRepository.updateCourse(course)
                } else {
                    courseRepository.insertCourse(course)
                }
                courseColorRepository.upsertColor(name, finalColor)
                courseRepository.updateColorByCourseName(name, finalColor)
                widgetUpdateDispatcher.refresh()
                saveSuccessState.value = true
            } catch (error: Exception) {
                errorState.value = error.message ?: "\u4fdd\u5b58\u5931\u8d25"
            } finally {
                savingState.value = false
            }
        }
    }

    fun deleteCourse() {
        val draft = uiState.value.draft
        if (draft.id <= 0) {
            saveSuccessState.value = true
            return
        }
        viewModelScope.launch {
            val course = courseRepository.getCourseById(draft.id) ?: return@launch
            courseRepository.deleteCourse(course)
            widgetUpdateDispatcher.refresh()
            saveSuccessState.value = true
        }
    }
}
