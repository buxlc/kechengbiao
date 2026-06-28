package com.bu.kebiao.ui.courseimport

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bu.kebiao.data.adapter.AdapterInfo
import com.bu.kebiao.data.adapter.SchoolAdapterLoader
import com.bu.kebiao.data.adapter.SchoolInfo
import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.repository.CourseRepository
import com.bu.kebiao.liveupdate.CourseLiveUpdateScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class EduImportMode {
    UrlInput,
    SchoolPreset,
    CsvText
}

data class ImportUiState(
    val schools: List<SchoolInfo> = emptyList(),
    val filteredSchools: List<SchoolInfo> = emptyList(),
    val searchQuery: String = "",
    val selectedSchool: SchoolInfo? = null,
    val selectedAdapter: AdapterInfo? = null,
    val parsedCourses: List<Course> = emptyList(),
    val isParsing: Boolean = false,
    val isSchoolListLoading: Boolean = false,
    val importResult: Boolean? = null,
    val importMessage: String? = null,
    val webViewVisible: Boolean = false,
    val webViewUrl: String = "",
    val urlInputText: String = "",
    val csvTextInput: String = "",
    val csvTextErrors: List<String> = emptyList(),
    val currentScreen: ImportScreen = ImportScreen.EduHome,
    val eduImportMode: EduImportMode = EduImportMode.UrlInput
)

enum class ImportScreen {
    EduHome,
    SchoolList,
    AdapterList,
    WebView,
    CsvText,
    Preview
}
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val userPreferences: UserPreferences,
    private val liveUpdateScheduler: CourseLiveUpdateScheduler,
    private val app: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    init {
        loadSchools()
    }

    private fun loadSchools() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSchoolListLoading = true) }
            try {
                val schools = SchoolAdapterLoader.loadSchools(app)
                _uiState.update {
                    it.copy(schools = schools, filteredSchools = schools, isSchoolListLoading = false)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSchoolListLoading = false) }
            }
        }
    }

    fun selectEduMode(mode: EduImportMode) {
        _uiState.update { it.copy(eduImportMode = mode) }
    }

    fun onUrlInputChanged(url: String) {
        _uiState.update { it.copy(urlInputText = url) }
    }

    fun openUrlInWebView() {
        val url = _uiState.value.urlInputText.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(importMessage = "\u8bf7\u8f93\u5165\u7f51\u5740") }
            return
        }
        val fullUrl = if (url.startsWith("http")) url else "https://" + url
        val detected = SchoolAdapterLoader.detectAdapterByUrl(fullUrl, _uiState.value.schools)
        _uiState.update {
            it.copy(
                webViewUrl = fullUrl,
                webViewVisible = true,
                currentScreen = ImportScreen.WebView,
                selectedSchool = detected?.first,
                selectedAdapter = detected?.second,
                importMessage = null
            )
        }
    }

    fun onWebUrlChanged(url: String) {
        val fullUrl = url.trim()
        if (fullUrl.isBlank()) return

        val state = _uiState.value
        if (state.eduImportMode != EduImportMode.UrlInput) {
            _uiState.update { it.copy(webViewUrl = fullUrl) }
            return
        }

        val detected = SchoolAdapterLoader.detectAdapterByUrl(fullUrl, state.schools)
        _uiState.update {
            it.copy(
                webViewUrl = fullUrl,
                selectedSchool = detected?.first,
                selectedAdapter = detected?.second
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) state.schools
            else state.schools.filter {
                it.name.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true)
            }
            state.copy(searchQuery = query, filteredSchools = filtered)
        }
    }

    fun onSchoolSelected(school: SchoolInfo) {
        _uiState.update { it.copy(selectedSchool = school, currentScreen = ImportScreen.AdapterList) }
    }

    fun onAdapterSelected(adapter: AdapterInfo) {
        val url = adapter.importUrl.ifBlank { "" }
        _uiState.update {
            it.copy(selectedAdapter = adapter, webViewUrl = url, webViewVisible = true, currentScreen = ImportScreen.WebView)
        }
    }

    fun onBackFromSchoolList() {
        _uiState.update { it.copy(selectedSchool = null, currentScreen = ImportScreen.EduHome) }
    }

    fun onBackFromAdapterList() {
        _uiState.update { it.copy(selectedSchool = null, currentScreen = ImportScreen.SchoolList) }
    }

    fun onBackFromWebView() {
        _uiState.update {
            it.copy(webViewVisible = false, webViewUrl = "", selectedAdapter = null, currentScreen = ImportScreen.EduHome)
        }
    }

    fun onBackFromCsvText() {
        _uiState.update { it.copy(currentScreen = ImportScreen.EduHome) }
    }

    fun onBackFromPreview() {
        _uiState.update {
            it.copy(parsedCourses = emptyList(), currentScreen = ImportScreen.EduHome, selectedSchool = null, selectedAdapter = null)
        }
    }

    fun navigateToSchoolList() {
        _uiState.update { it.copy(currentScreen = ImportScreen.SchoolList) }
    }

    fun navigateToCsvTextImport() {
        _uiState.update { it.copy(eduImportMode = EduImportMode.CsvText, currentScreen = ImportScreen.CsvText) }
    }

    fun onCsvTextChanged(text: String) {
        _uiState.update { it.copy(csvTextInput = text, csvTextErrors = emptyList()) }
    }

    fun parseCsvTextInput() {
        parseCsvText(_uiState.value.csvTextInput, source = "csv_import")
    }

    fun parseCsvTextFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = runCatching {
                app.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
            }.getOrElse { error ->
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(importMessage = "文件读取失败: ${error.message ?: "未知错误"}") }
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(csvTextInput = content) }
                parseCsvText(content, source = "csv_import")
            }
        }
    }

    private fun parseCsvText(input: String, source: String) {
        if (input.isBlank()) {
            _uiState.update { it.copy(importMessage = "请先粘贴 CSV 或文本内容") }
            return
        }
        val result = CsvTextCourseParser.parse(input, source = source)
        if (result.courses.isEmpty()) {
            _uiState.update {
                it.copy(
                    csvTextErrors = result.errors,
                    importMessage = result.errors.firstOrNull() ?: "未解析到课程"
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                parsedCourses = result.courses,
                csvTextErrors = result.errors,
                currentScreen = ImportScreen.Preview,
                importMessage = if (result.errors.isNotEmpty()) {
                    "已解析 ${result.courses.size} 门课，跳过 ${result.errors.size} 行异常数据"
                } else {
                    null
                }
            )
        }
    }

    fun onCoursesParsed(courses: List<Course>) {
        _uiState.update {
            it.copy(parsedCourses = courses, webViewVisible = false, currentScreen = ImportScreen.Preview)
        }
    }

    fun onImportFinished() {
        if (_uiState.value.parsedCourses.isNotEmpty()) {
            _uiState.update { it.copy(webViewVisible = false, currentScreen = ImportScreen.Preview) }
        }
    }
    fun confirmImport() {
        val courses = _uiState.value.parsedCourses
        if (courses.isEmpty()) {
            _uiState.update { it.copy(importMessage = "\u6ca1\u6709\u53ef\u5bfc\u5165\u7684\u8bfe\u7a0b") }
            return
        }
        viewModelScope.launch {
            try {
                courses
                    .map { it.source }
                    .filter { it.isNotBlank() && it != "manual" }
                    .distinct()
                    .forEach { source -> courseRepository.deleteBySource(source) }
                courseRepository.insertCourses(courses)
                userPreferences.setHasImported(true)
                liveUpdateScheduler.refreshNow()
                val school = _uiState.value.selectedSchool
                if (school != null) {
                    userPreferences.updateEduInfo(school.name, "")
                }
                _uiState.update {
                    it.copy(importResult = true, importMessage = "\u6210\u529f\u5bfc\u5165 " + courses.size + " \u95e8\u8bfe\u7a0b")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(importResult = false, importMessage = "\u5bfc\u5165\u5931\u8d25: " + (e.message ?: "\u672a\u77e5\u9519\u8bef"))
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(importMessage = null) }
    }

    fun reportWebLoadIssue(message: String) {
        if (message.isBlank()) return
        _uiState.update { it.copy(importMessage = message) }
    }

    fun loadAdapterScript(webView: android.webkit.WebView) {
        val adapter = _uiState.value.selectedAdapter
        val school = _uiState.value.selectedSchool
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsContent = if (adapter != null && school != null) {
                    SchoolAdapterLoader.loadJsScript(app, school.folder, adapter.jsPath)
                } else {
                    CourseParserJs.SCRIPT
                }
                val fullScript = JsBridgeHelper.buildAdapterScript(jsContent)
                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(fullScript, null)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "unknown"
                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(
                        "AndroidBridge.showToast('script error: " + msg.replace("'", "") + "')",
                        null
                    )
                }
            }
        }
    }
}
