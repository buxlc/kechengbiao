package com.bu.kebiao.ui.courseimport

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bu.kebiao.data.adapter.AdapterInfo
import com.bu.kebiao.data.adapter.SchoolAdapterLoader
import com.bu.kebiao.data.adapter.SchoolInfo
import com.bu.kebiao.data.adapter.cloud.AdapterCloudRepository
import com.bu.kebiao.data.adapter.cloud.AdapterCloudSyncResult
import com.bu.kebiao.data.adapter.cloud.SchoolIndexCloudRepository
import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.model.Semester
import com.bu.kebiao.domain.repository.CourseRepository
import com.bu.kebiao.domain.repository.SemesterRepository
import com.bu.kebiao.liveupdate.CourseLiveUpdateScheduler
import com.bu.kebiao.widget.WidgetUpdateDispatcher
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
    CsvText,
    IcsFile
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
    val isRefreshingAdapter: Boolean = false,
    val webViewVisible: Boolean = false,
    val webViewUrl: String = "",
    val urlInputText: String = "",
    val csvTextInput: String = "",
    val csvTextErrors: List<String> = emptyList(),
    val currentScreen: ImportScreen = ImportScreen.EduHome,
    val eduImportMode: EduImportMode = EduImportMode.UrlInput,
    val semesters: List<Semester> = emptyList(),
    val importAsNewSemester: Boolean = true,
    val selectedImportSemesterId: String = "",
    val newImportSemesterName: String = "新学期"
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
    private val semesterRepository: SemesterRepository,
    private val userPreferences: UserPreferences,
    private val liveUpdateScheduler: CourseLiveUpdateScheduler,
    private val widgetUpdateDispatcher: WidgetUpdateDispatcher,
    private val adapterCloudRepository: AdapterCloudRepository,
    private val schoolIndexCloudRepository: SchoolIndexCloudRepository,
    private val app: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    init {
        loadSchools()
        observeSemesters()
    }

    private fun observeSemesters() {
        viewModelScope.launch {
            semesterRepository.observeSemesters().collect { semesters ->
                _uiState.update { state ->
                    val selected = state.selectedImportSemesterId
                        .takeIf { id -> semesters.any { it.id == id } }
                        ?: semesters.firstOrNull()?.id.orEmpty()
                    state.copy(
                        semesters = semesters,
                        selectedImportSemesterId = selected,
                        importAsNewSemester = if (semesters.isEmpty()) true else state.importAsNewSemester
                    )
                }
            }
        }
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

    private fun reloadSchoolsAfterCloudSync() {
        SchoolAdapterLoader.clearCache()
        loadSchools()
    }

    fun refreshCloudSchoolPresets() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshingAdapter = true, importMessage = "正在刷新云端学校预设...") }
            val result = schoolIndexCloudRepository.refreshSchoolIndex(app.filesDir)
            val message = when (result) {
                is com.bu.kebiao.data.adapter.cloud.SchoolIndexRefreshResult.Success ->
                    "此次新增${result.schoolCount}个预设学校"
                is com.bu.kebiao.data.adapter.cloud.SchoolIndexRefreshResult.Failed ->
                    result.reason
            }
            if (result is com.bu.kebiao.data.adapter.cloud.SchoolIndexRefreshResult.Success) {
                withContext(Dispatchers.Main) {
                    reloadSchoolsAfterCloudSync()
                }
            }
            _uiState.update { it.copy(isRefreshingAdapter = false, importMessage = message) }
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

    fun setImportAsNewSemester(value: Boolean) {
        _uiState.update { it.copy(importAsNewSemester = value) }
    }

    fun selectImportSemester(semesterId: String) {
        _uiState.update { it.copy(selectedImportSemesterId = semesterId, importAsNewSemester = false) }
    }

    fun onNewImportSemesterNameChanged(name: String) {
        _uiState.update { it.copy(newImportSemesterName = name) }
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

    fun parseIcsFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val bytes = runCatching {
                app.contentResolver.openInputStream(uri)
                    ?.use { it.readBytes() }
                    ?: ByteArray(0)
            }.getOrElse { error ->
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(importMessage = "ICS文件读取失败: ${error.message ?: "未知错误"}") }
                }
                return@launch
            }
            val content = IcsTextDecoder.decode(bytes)

            val result = IcsCourseParser.parse(content, source = "ics_import")
            withContext(Dispatchers.Main) {
                if (result.courses.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            parsedCourses = emptyList(),
                            importMessage = buildIcsParseFailureMessage(result, bytes, content)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            eduImportMode = EduImportMode.IcsFile,
                            parsedCourses = result.courses,
                            currentScreen = ImportScreen.Preview,
                            importMessage = if (result.errors.isNotEmpty()) {
                                "已解析 ${result.courses.size} 门课，跳过 ${result.errors.size} 个日历事件"
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }

    private fun buildIcsParseFailureMessage(
        result: IcsCourseParseResult,
        bytes: ByteArray,
        content: String
    ): String {
        val detail = result.errors.firstOrNull()
        val head = content
            .replace("\r", "")
            .replace("\n", " ")
            .trim()
            .take(36)
        return when {
            bytes.isEmpty() -> "ICS文件是空的，请重新选择原始 .ics 文件"
            content.isBlank() -> "ICS文件读取后为空，请换一个文件管理器重新选择"
            detail != null -> detail
            else -> "未找到ICS课程事件，文件头: ${head.ifBlank { "空" }}"
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
                val state = _uiState.value
                val targetSemesterId = if (state.importAsNewSemester || state.semesters.isEmpty()) {
                    val name = state.newImportSemesterName.ifBlank { "新学期" }
                    semesterRepository.createSemester(name)
                } else {
                    state.selectedImportSemesterId.ifBlank {
                        state.semesters.firstOrNull()?.id ?: semesterRepository.createSemester("新学期")
                    }
                }

                userPreferences.updateCurrentSemesterId(targetSemesterId)
                courseRepository.deleteSemesterCourses(targetSemesterId)
                courseRepository.insertCoursesIntoSemester(courses, targetSemesterId)
                userPreferences.setHasImported(true)
                liveUpdateScheduler.refreshNow()
                widgetUpdateDispatcher.refresh()
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
                    SchoolAdapterLoader.loadJsScript(app, school.id, school.folder, adapter.jsPath, adapter.adapterId)
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

    fun refreshCloudAdapterScript() {
        refreshCloudSchoolPresets()
    }
}
