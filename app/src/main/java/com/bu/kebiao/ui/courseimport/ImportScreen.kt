package com.bu.kebiao.ui.courseimport

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import android.webkit.SslErrorHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward

import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bu.kebiao.data.adapter.AdapterInfo
import com.bu.kebiao.data.adapter.SchoolInfo
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.domain.model.Semester
import com.bu.kebiao.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImportSuccess: () -> Unit = {},
    viewModel: ImportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show feedback via Snackbar
    state.importMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }

    // Handle import success -> navigate back
    state.importResult?.let { success ->
        if (success) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500)
                onImportSuccess()
                onBack()
            }
        }
    }

    BackHandler {
        when (state.currentScreen) {
            ImportScreen.WebView -> viewModel.onBackFromWebView()
            ImportScreen.AdapterList -> viewModel.onBackFromAdapterList()
            ImportScreen.SchoolList -> viewModel.onBackFromSchoolList()
            ImportScreen.CsvText -> viewModel.onBackFromCsvText()
            ImportScreen.Preview -> viewModel.onBackFromPreview()
            ImportScreen.EduHome -> onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\u5bfc\u5165\u8bfe\u8868", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (state.currentScreen) {
                            ImportScreen.WebView -> viewModel.onBackFromWebView()
                            ImportScreen.AdapterList -> viewModel.onBackFromAdapterList()
                            ImportScreen.SchoolList -> viewModel.onBackFromSchoolList()
                            ImportScreen.CsvText -> viewModel.onBackFromCsvText()
                            ImportScreen.Preview -> viewModel.onBackFromPreview()
                            ImportScreen.EduHome -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "\u8fd4\u56de")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (state.importResult == true)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (state.importResult == true)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)
        ) {
            EduImportFlow(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun EduImportFlow(state: ImportUiState, viewModel: ImportViewModel) {
    AnimatedContent(
        targetState = state.currentScreen,
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn() togetherWith
            slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
        },
        label = "edu-flow"
    ) { screen ->
        when (screen) {
            ImportScreen.EduHome -> EduHomeContent(state = state, viewModel = viewModel)
            ImportScreen.SchoolList -> SchoolListContent(
                schools = state.filteredSchools,
                searchQuery = state.searchQuery,
                isLoading = state.isSchoolListLoading,
                isRefreshing = state.isRefreshingAdapter,
                onSearchChanged = viewModel::onSearchQueryChanged,
                onSchoolSelected = viewModel::onSchoolSelected,
                onRefreshCloudSchools = viewModel::refreshCloudSchoolPresets
            )
            ImportScreen.AdapterList -> AdapterListContent(
                school = state.selectedSchool,
                onAdapterSelected = viewModel::onAdapterSelected
            )
            ImportScreen.WebView -> WebViewContent(state = state, viewModel = viewModel)
            ImportScreen.CsvText -> CsvTextImportContent(state = state, viewModel = viewModel)
            ImportScreen.Preview -> CoursePreviewContent(
                state = state,
                courses = state.parsedCourses,
                importResult = state.importResult,
                onConfirm = viewModel::confirmImport,
                onBack = viewModel::onBackFromPreview,
                onImportAsNewChanged = viewModel::setImportAsNewSemester,
                onSemesterSelected = viewModel::selectImportSemester,
                onNewSemesterNameChanged = viewModel::onNewImportSemesterNameChanged
            )
        }
    }
}

// Edu Home - Two mode selector
@Composable
private fun EduHomeContent(state: ImportUiState, viewModel: ImportViewModel) {
    val icsFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(viewModel::parseIcsFile)
    }

    Column {
        Text(
            text = "\u9009\u62e9\u5bfc\u5165\u65b9\u5f0f",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "\u4ece\u6559\u52a1\u7cfb\u7edf\u5bfc\u5165\u8bfe\u7a0b\u6570\u636e",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Mode 1: URL Input
        Surface(
            onClick = { viewModel.selectEduMode(EduImportMode.UrlInput) },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "\u8f93\u5165\u7f51\u5740",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\u76f4\u63a5\u8f93\u5165\u6559\u52a1\u7cfb\u7edf\u7f51\u5740\uff0c\u81ea\u52a8\u8bc6\u522b\u5e76\u89e3\u6790",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode 2: School Preset
        Surface(
            onClick = {
                viewModel.selectEduMode(EduImportMode.SchoolPreset)
                viewModel.navigateToSchoolList()
            },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Home,
                        null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "\u9009\u62e9\u5b66\u6821\u9884\u8bbe",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\u4ece ${state.schools.size} \u6240\u5b66\u6821\u4e2d\u9009\u62e9\uff0c\u4f7f\u7528\u9884\u914d\u9002\u914d\u811a\u672c",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode 4: ICS calendar file
        Surface(
            onClick = {
                viewModel.selectEduMode(EduImportMode.IcsFile)
                icsFileLauncher.launch(arrayOf("text/calendar", "application/ics", "application/octet-stream", "*/*"))
            },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ICS\u65e5\u5386\u5bfc\u5165",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\u9009\u62e9 .ics \u65e5\u5386\u6587\u4ef6\uff0c\u9002\u5408 WakeUp/\u7cfb\u7edf\u65e5\u5386\u5bfc\u51fa\u7684\u8bfe\u7a0b\u8868",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode 3: AI screenshot / CSV text
        Surface(
            onClick = { viewModel.navigateToCsvTextImport() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI截图/CSV导入",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "复制提示词给 AI，粘贴或选择 CSV/文本文件导入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // URL Input area (shown when UrlInput mode selected)
        if (state.eduImportMode == EduImportMode.UrlInput) {
            Text(
                text = "\u6559\u52a1\u7f51\u5740",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.urlInputText,
                    onValueChange = viewModel::onUrlInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("\u8f93\u5165\u6559\u52a1\u7cfb\u7edf\u7f51\u5740\uff0c\u5982 jwgl.xxx.edu.cn") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = viewModel::openUrlInWebView,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "\u6253\u5f00")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "\u63d0\u793a\uff1a\u6253\u5f00\u7f51\u5740\u540e\uff0c\u767b\u5f55\u6559\u52a1\u7cfb\u7edf\u5e76\u8fdb\u5165\u8bfe\u8868\u9875\u9762\uff0c\u7136\u540e\u70b9\u51fb\u201c\u5c1d\u8bd5\u89e3\u6790\u201d\u6309\u94ae",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// School List
@Composable
private fun SchoolListContent(
    schools: List<SchoolInfo>,
    searchQuery: String,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onSearchChanged: (String) -> Unit,
    onSchoolSelected: (SchoolInfo) -> Unit,
    onRefreshCloudSchools: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("\u641c\u7d22\u5b66\u6821\u540d\u79f0...") },
                leadingIcon = { Icon(Icons.Default.Search, "\u641c\u7d22") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, "\u6e05\u9664", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = onRefreshCloudSchools,
                enabled = !isRefreshing,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, "\u5237\u65b0\u5b66\u6821\u9884\u8bbe", modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\u5171 ${schools.size} \u6240\u5b66\u6821",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                items(schools, key = { it.id }) { school ->
                    SchoolListItem(school = school, onClick = { onSchoolSelected(school) })
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SchoolListItem(school: SchoolInfo, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = school.initial.take(1), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = school.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = "${school.adapters.size} \u4e2a\u9002\u914d\u5668", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

// Adapter List
@Composable
private fun AdapterListContent(school: SchoolInfo?, onAdapterSelected: (AdapterInfo) -> Unit) {
    if (school == null) return
    Column {
        Text(text = school.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "\u9009\u62e9\u9002\u914d\u811a\u672c", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(school.adapters) { adapter ->
                AdapterCard(adapter = adapter, onClick = { onAdapterSelected(adapter) })
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun AdapterCard(adapter: AdapterInfo, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = adapter.adapterName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (adapter.importUrl.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(text = "\u81ea\u52a8\u8df3\u8f6c", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
            if (adapter.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = adapter.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "\u7ef4\u62a4\u8005: ${adapter.maintainer}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun CsvTextImportContent(state: ImportUiState, viewModel: ImportViewModel) {
    val context = LocalContext.current
    var showGuide by remember { mutableStateOf(false) }
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(viewModel::parseCsvTextFile)
    }

    if (showGuide) {
        AlertDialog(
            onDismissRequest = { showGuide = false },
            confirmButton = {
                TextButton(onClick = { showGuide = false }) { Text("知道了") }
            },
            title = { Text("AI截图/CSV导入教程") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    item {
                        Text(
                            text = CSV_IMPORT_GUIDE_SUMMARY,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "AI截图/CSV导入",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "适合学校暂不支持教务导入时使用",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Bu课表 AI 导入提示词", AI_CSV_PROMPT))
                    viewModel.reportWebLoadIssue("AI 提示词已复制")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("复制提示词")
            }
            OutlinedButton(onClick = { showGuide = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("查看教程")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { fileLauncher.launch(arrayOf("text/*", "text/csv", "application/csv", "application/vnd.ms-excel")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("选择 CSV / TXT 文件")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.csvTextInput,
            onValueChange = viewModel::onCsvTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("粘贴 CSV 或文本") },
            placeholder = { Text("name,day,start_section,end_section,weeks,teacher,location\n高等数学,1,1,2,1-16,张三,教学楼 101") },
            shape = RoundedCornerShape(12.dp)
        )

        if (state.csvTextErrors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "异常行：${state.csvTextErrors.take(2).joinToString("；")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = viewModel::parseCsvTextInput,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("解析并预览")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// Enhanced WebView with proper lifecycle
@SuppressLint("SetJavaScriptEnabled")
@Suppress("DEPRECATION")
@Composable
private fun WebViewContent(state: ImportUiState, viewModel: ImportViewModel) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var urlInput by remember { mutableStateOf(state.webViewUrl) }
    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    val preferDesktopMode = remember(state.selectedSchool?.id, state.webViewUrl) {
        state.selectedSchool?.id == "LNTU" ||
            (state.webViewUrl.contains("webvpn", ignoreCase = true) && state.webViewUrl.contains("eams", ignoreCase = true))
    }
    var isDesktopMode by remember(preferDesktopMode) { mutableStateOf(preferDesktopMode) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showUrlBar by remember { mutableStateOf(state.webViewUrl.isBlank()) }
    var isExecuting by remember { mutableStateOf(false) }

    // Proper WebView cleanup
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.clearHistory()
                (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                wv.destroy()
            }
            webViewRef = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Adapter info or URL mode banner
        val bannerText = state.selectedAdapter?.adapterName ?: "\u8f93\u5165\u7f51\u5740\u6a21\u5f0f"
        val bannerDesc = state.selectedAdapter?.description ?: "\u767b\u5f55\u540e\u70b9\u51fb\u201c\u5c1d\u8bd5\u89e3\u6790\u201d\u6309\u94ae"

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = bannerText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = bannerDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), maxLines = 1)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (showUrlBar) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = urlInput, onValueChange = { urlInput = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("\u8f93\u5165\u6559\u52a1\u7cfb\u7edf\u7f51\u5740") },
                    shape = RoundedCornerShape(10.dp), singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        val url = if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
                        viewModel.onWebUrlChanged(url)
                        webViewRef?.loadUrl(url); showUrlBar = false
                    },
                    modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp)
                ) { Icon(Icons.AutoMirrored.Filled.Send, "\u524d\u5f80", modifier = Modifier.size(18.dp)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (loadingProgress < 1f) {
            LinearProgressIndicator(progress = { loadingProgress }, modifier = Modifier.fillMaxWidth().height(2.dp))
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.loadsImagesAutomatically = true
                        settings.blockNetworkImage = false
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.allowFileAccessFromFileURLs = true
                        if (preferDesktopMode) {
                            settings.userAgentString = desktopUserAgent
                        }
                        // 禁用 X-Requested-With 请求头，避免教务系统/VPN拒绝请求
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            runCatching {
                                val mode = WebSettings::class.java
                                    .getField("REQUESTED_WITH_HEADER_MODE_NO_HEADER")
                                    .getInt(null)
                                WebSettings::class.java
                                    .getMethod("setRequestedWithHeaderMode", Int::class.javaPrimitiveType)
                                    .invoke(settings, mode)
                            }
                        }
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let(viewModel::onWebUrlChanged)
                                canGoBack = view?.canGoBack() == true; canGoForward = view?.canGoForward() == true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let(viewModel::onWebUrlChanged)
                                canGoBack = view?.canGoBack() == true; canGoForward = view?.canGoForward() == true
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    val errorMsg = error?.description?.toString() ?: "未知错误"
                                    val userMessage = when {
                                        errorMsg.contains("ERR_SSL", ignoreCase = true) -> "SSL证书错误，请尝试在设置中忽略证书或联系学校"
                                        errorMsg.contains("ERR_TIMED_OUT", ignoreCase = true) -> "连接超时，请检查网络或稍后重试"
                                        errorMsg.contains("ERR_CONNECTION", ignoreCase = true) -> "连接失败，请检查教务系统地址是否正确"
                                        errorMsg.contains("ERR_NAME_NOT_RESOLVED", ignoreCase = true) -> "域名无法解析，请检查网址"
                                        else -> "网页加载异常：$errorMsg"
                                    }
                                    viewModel.reportWebLoadIssue(userMessage)
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                // 教育系统常用过期或自签名证书，提示用户后继续
                                android.app.AlertDialog.Builder(view?.context)
                                    .setTitle("安全证书警告")
                                    .setMessage("该教务系统的安全证书有问题，是否继续访问？\n\n错误：${error?.primaryError?.let { getSslErrorText(it) } ?: "未知"}")
                                    .setPositiveButton("继续访问") { _, _ -> handler?.proceed() }
                                    .setNegativeButton("取消") { _, _ -> handler?.cancel() }
                                    .setCancelable(false)
                                    .show()
                            }

                            private fun getSslErrorText(error: Int): String = when (error) {
                                SslError.SSL_DATE_INVALID -> "证书日期无效"
                                SslError.SSL_EXPIRED -> "证书已过期"
                                SslError.SSL_IDMISMATCH -> "证书域名不匹配"
                                SslError.SSL_NOTYETVALID -> "证书尚未生效"
                                SslError.SSL_UNTRUSTED -> "证书不受信任"
                                SslError.SSL_INVALID -> "证书无效"
                                else -> "未知SSL错误"
                            }
                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (request?.isForMainFrame == true) {
                                    val code = errorResponse?.statusCode ?: 0
                                    if (code in 400..599) {
                                        val hint = when (code) {
                                            403 -> "访问被拒绝，可能需要重新登录"
                                            404 -> "页面不存在"
                                            500, 502, 503 -> "教务系统服务器异常，请稍后重试"
                                            else -> "HTTP错误 $code"
                                        }
                                        viewModel.reportWebLoadIssue("服务器错误：$hint")
                                    }
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) { loadingProgress = newProgress / 100f }
                        }
                        addJavascriptInterface(
                            CourseJsBridge(ctx, this,
                                onCoursesParsed = { courses -> viewModel.onCoursesParsed(courses) },
                                onImportFinished = { viewModel.onImportFinished() }
                            ), "AndroidBridge"
                        )
                        val initialUrl = state.webViewUrl
                        if (initialUrl.isNotBlank()) loadUrl(initialUrl)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Execute button (prominent)
        FilledTonalButton(
            onClick = {
                isExecuting = true
                webViewRef?.let { wv -> viewModel.loadAdapterScript(wv) }
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (state.selectedAdapter != null) "\u6267\u884c\u5bfc\u5165\u811a\u672c" else "\u5c1d\u8bd5\u89e3\u6790\u5f53\u524d\u9875\u9762",
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom toolbar
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { webViewRef?.goBack() }, enabled = canGoBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "\u540e\u9000", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { webViewRef?.goForward() }, enabled = canGoForward, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "\u524d\u8fdb", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { webViewRef?.reload() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, "\u5237\u65b0", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = { viewModel.refreshCloudAdapterScript() },
                    enabled = !state.isRefreshingAdapter,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (state.isRefreshingAdapter) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, "\u5237\u65b0\u811a\u672c", modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(
                    onClick = {
                        isDesktopMode = !isDesktopMode
                        webViewRef?.let { wv ->
                            wv.settings.userAgentString = if (isDesktopMode) desktopUserAgent else null
                            wv.settings.loadWithOverviewMode = isDesktopMode; wv.settings.useWideViewPort = isDesktopMode; wv.reload()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(if (isDesktopMode) Icons.Default.Home else Icons.Default.Phone, null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showUrlBar = !showUrlBar }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Info, "\u5730\u5740\u680f", modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// Course Preview with proper feedback
@Composable
private fun CoursePreviewContent(
    state: ImportUiState,
    courses: List<Course>,
    importResult: Boolean?,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onImportAsNewChanged: (Boolean) -> Unit,
    onSemesterSelected: (String) -> Unit,
    onNewSemesterNameChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (courses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("\u672a\u89e3\u6790\u5230\u8bfe\u7a0b\u6570\u636e", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onBack) { Text("\u8fd4\u56de\u91cd\u8bd5") }
                }
            }
        } else {
            Text(text = "\u8bfe\u7a0b\u9884\u89c8", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\u5171\u89e3\u6790 ${courses.size} \u95e8\u8bfe\u7a0b",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            ImportTargetContent(
                semesters = state.semesters,
                importAsNewSemester = state.importAsNewSemester,
                selectedSemesterId = state.selectedImportSemesterId,
                newSemesterName = state.newImportSemesterName,
                onImportAsNewChanged = onImportAsNewChanged,
                onSemesterSelected = onSemesterSelected,
                onNewSemesterNameChanged = onNewSemesterNameChanged
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                items(courses.size) { index -> CoursePreviewItem(courses[index]) }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Import button with clear state feedback
            val buttonText = when (importResult) {
                true -> "\u2713 \u5bfc\u5165\u6210\u529f\uff0c\u6b63\u5728\u8fd4\u56de..."
                false -> "\u5bfc\u5165\u5931\u8d25\uff0c\u70b9\u51fb\u91cd\u8bd5"
                null -> "\u786e\u8ba4\u5bfc\u5165 (${courses.size} \u95e8\u8bfe)"
            }
            val buttonEnabled = importResult != true

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = buttonEnabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (importResult == false) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                if (importResult == true) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = buttonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ImportTargetContent(
    semesters: List<Semester>,
    importAsNewSemester: Boolean,
    selectedSemesterId: String,
    newSemesterName: String,
    onImportAsNewChanged: (Boolean) -> Unit,
    onSemesterSelected: (String) -> Unit,
    onNewSemesterNameChanged: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "导入到学期",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = importAsNewSemester || semesters.isEmpty(),
                    onClick = { onImportAsNewChanged(true) },
                    label = { Text("新建学期") },
                    leadingIcon = if (importAsNewSemester || semesters.isEmpty()) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = !importAsNewSemester && semesters.isNotEmpty(),
                    onClick = { if (semesters.isNotEmpty()) onImportAsNewChanged(false) },
                    enabled = semesters.isNotEmpty(),
                    label = { Text("覆盖已有") },
                    leadingIcon = if (!importAsNewSemester && semesters.isNotEmpty()) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (importAsNewSemester || semesters.isEmpty()) {
                OutlinedTextField(
                    value = newSemesterName,
                    onValueChange = onNewSemesterNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("新学期名称") },
                    shape = RoundedCornerShape(10.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    semesters.forEach { semester ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSemesterSelected(semester.id) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = semester.id == selectedSemesterId,
                                onClick = { onSemesterSelected(semester.id) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(semester.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${semester.courseCount} 门课，将被覆盖", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoursePreviewItem(course: Course) {
    val dayNames = listOf("", "\u5468\u4e00", "\u5468\u4e8c", "\u5468\u4e09", "\u5468\u56db", "\u5468\u4e94", "\u5468\u516d", "\u5468\u65e5")
    val weekTypeText = when (course.weekType) {
        com.bu.kebiao.domain.model.WeekType.ODD -> "\u5355\u5468"
        com.bu.kebiao.domain.model.WeekType.EVEN -> "\u53cc\u5468"
        else -> ""
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(4.dp, 36.dp).clip(RoundedCornerShape(2.dp)).background(getCourseColor(course.colorIndex)))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = course.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Row {
                    Text(
                        text = "${dayNames.getOrElse(course.dayOfWeek) { ""}} ${course.startSection}-${course.endSection}\u8282",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (weekTypeText.isNotBlank()) {
                        Text(text = " $weekTypeText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (course.teacher.isNotBlank() || course.location.isNotBlank()) {
                    Text(
                        text = buildString {
                            if (course.teacher.isNotBlank()) append(course.teacher)
                            if (course.teacher.isNotBlank() && course.location.isNotBlank()) append(" \u00b7 ")
                            if (course.location.isNotBlank()) append(course.location)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Text(text = "${course.startWeek}-${course.endWeek}\u5468", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun getCourseColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFFE91E63),
        Color(0xFF9C27B0), Color(0xFF00BCD4), Color(0xFFFF5722), Color(0xFF607D8B)
    )
    return colors[index % colors.size]
}

private val CSV_IMPORT_GUIDE_SUMMARY = """
1. 截图学校课表。
2. 点击“复制提示词”，把提示词和截图一起发给支持图片识别的 AI。(记得开思考模式
   并且精度拉到最高，特别是唐包)
3. 复制 AI 返回的 CSV。
4. 粘贴到输入框，或保存为 .csv / .txt 后选择文件。
5. 点击“解析并预览”，确认后导入。

""".trimIndent()

private val AI_CSV_PROMPT = """
课程表截图 OCR 精准提取提示词
身份与唯一任务
你是仅执行截图原文扫描的课程表信息提取工具，唯一任务：100% 基于截图可见文字、零猜测、零补全，提取所有课程信息并转换为可导入 CSV 格式。除提取与格式化外，不执行任何额外操作。
严禁行为（绝对禁止）
不要分析课程冲突、判断排课合理性、合并 / 删除重叠课程、输出任何冲突提醒。
不要根据常识、经验补全截图中看不清、未显示的任何内容，包括课程名、老师、地点、周次。
不要将课程内的章节标题、知识点、实验项目等内容当成独立课程输出。
不要修改截图中可见的课程名、周次、节次、老师、地点原文，不要拼接不同课程的字段。
不要输出解释、备注、建议、总结、识别过程等任何额外文字。
不要将 F/CXT 开头的课程代码、编号混入课程名称、老师、地点字段。
不要错误拆分或合并课程块的节次，严格按单元格视觉边界判断起止节次。
CSV 输出规范
固定表头
name,day,start_section,end_section,weeks,teacher,location
强制格式规则
每一行必须严格包含 7 个字段，字段分隔符仅使用英文半角逗号,，禁止用中文逗号分隔字段。
若字段内容本身包含英文逗号、中文逗号、换行、多段内容，必须用英文双引号包裹该字段。
weeks 字段包含多个周次段时，必须用英文双引号包裹，例如 "4,5-8,14-17"。
teacher 字段包含多位老师时，必须用英文双引号包裹，例如 "张三,李四"。
location 字段包含多个地点时，必须用英文双引号包裹，例如 "博雅楼115,尔雅楼402"。
输出前自检：除表头外，每一行都能被标准 CSV 解析为恰好 7 列。
字段说明
name：课程中文名称，必填。仅提取括号外的正式课程名，剔除课程代码。课程名看不清则整行跳过。
day：星期数字，1 = 周一，2 = 周二，3 = 周三，4 = 周四，5 = 周五，6 = 周六，7 = 周日，按课程所在列判断。
start_section：开始节次，阿拉伯数字，按课程块顶部所在节次判断。
end_section：结束节次，阿拉伯数字，按课程块底部所在节次判断。
weeks：上课周次，严格照搬原文数字，仅按规则清理空格。截图未显示周次统一填 1-20。
teacher：授课教师姓名，从对应括号内提取，看不清则留空。
location：上课地点，从对应括号内提取，看不清则留空。
周次格式统一规则
仅删除原文多余空格，不修改周次数字范围：
每周上课：1-20（未标注周次时默认填写）
指定范围：1-16
单周标注：原文 1-16 单周 统一改为 1-16单周
双周标注：原文 6-12 双周 统一改为 6-12双周
不连续周次：原样保留分隔，例如 1-4,6,8-12
单独某周：例如 5
提取执行细则
逐列逐行扫描：按周一到周日的列顺序，从上到下逐节次扫描所有课程单元格，不遗漏任何可见课程。
多课程单元格处理：同一个单元格内包含多门不同名称的课程（不同周次 / 老师）时，每门课单独输出一行，各自对应自己的周次、老师、地点，禁止字段混淆。
课程名提取规则：
取每门课开头的中文正式名称，括号内的课程代码全部剔除；课程名自带的星号、序号原样保留。
单元格内的章节标题、知识点列表属于课程内容，不是独立课程，严禁单独成行。
老师与地点提取规则：
从课程对应的括号内容中精准提取：标注为姓名的归入 teacher 字段，标注为地点的归入 location 字段，严格对应，禁止张冠李戴。
同一课程多位老师的，原样保留分隔符，整体用双引号包裹。
无法明确区分、文字看不清的，对应字段留空，禁止猜测。
节次判断规则：课程块顶部对齐第几节，start_section 就填几；底部对齐第几节，end_section 就填几；跨多节的课程块合并为一行。
模糊内容处理：课程名称无法清晰识别的，整门课跳过；老师 / 地点看不清的，对应字段留空。
最终输出要求
仅输出纯 CSV 文本，第一行必须是固定表头，不要 Markdown、不要代码块、不要任何额外文字。
""".trimIndent()
