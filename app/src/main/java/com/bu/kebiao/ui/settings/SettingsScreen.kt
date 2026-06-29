package com.bu.kebiao.ui.settings

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bu.kebiao.domain.model.AcademicWeekResolver
import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Semester
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    onImportClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimeEditor by remember { mutableStateOf<ClassTime?>(null) }
    var showClassTimePage by remember { mutableStateOf(false) }
    var showWeekPicker by remember { mutableStateOf(false) }
    var showNewSemesterDialog by remember { mutableStateOf(false) }
    var showExportSemesterDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "\u8bbe\u7f6e",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Semester section
        SettingsSection("\u5b66\u671f\u8bbe\u7f6e") {
            // Semester start date
            val startDateText = if (state.semesterStartDate > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(Date(state.semesterStartDate))
            } else {
                "\u672a\u8bbe\u7f6e"
            }
            SettingsItem(
                icon = Icons.Default.DateRange,
                title = "\u5f00\u5b66\u7b2c\u4e00\u5929",
                subtitle = startDateText,
                onClick = {
                    val cal = Calendar.getInstance()
                    if (state.semesterStartDate > 0) {
                        cal.timeInMillis = state.semesterStartDate
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            cal.set(year, month, day)
                            viewModel.updateSemesterStartDate(cal.timeInMillis, state.totalWeeks)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            )
            SettingsItem(
                icon = Icons.Default.DateRange,
                title = "\u67e5\u770b\u5468\u6b21",
                subtitle = "${AcademicWeekResolver.formatViewingWeekLabel(state.viewingWeek, state.currentWeek)} / ${state.totalWeeks}\u5468",
                onClick = { showWeekPicker = true }
            )
            state.semesters.forEach { semester ->
                SemesterRow(
                    semester = semester,
                    selected = semester.id == state.currentSemesterId,
                    onSelect = { viewModel.switchSemester(semester.id) },
                    onDelete = { viewModel.deleteSemester(semester.id) }
                )
            }
            SettingsItem(
                icon = Icons.Default.Add,
                title = "新增学期",
                subtitle = "创建一份独立课程数据",
                onClick = { showNewSemesterDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Share,
                title = "导出学期 CSV",
                subtitle = "选择一个学期导出，包含手动新增和修改后的课程",
                onClick = { showExportSemesterDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Import section
        SettingsSection("\u5bfc\u5165\u7ba1\u7406") {
            SettingsItem(
                icon = Icons.Default.Refresh,
                title = if (state.hasImported) "\u91cd\u65b0\u5bfc\u5165\u8bfe\u8868" else "\u5bfc\u5165\u8bfe\u8868",
                subtitle = if (state.eduSchool.isNotBlank()) "\u5df2\u5bfc\u5165: ${state.eduSchool}"
                else "\u6559\u52a1\u5bfc\u5165 / PDF\u5bfc\u5165",
                onClick = onImportClick
            )
            if (state.hasImported) {
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "清除当前学期课程",
                    subtitle = "只删除当前学期，不影响其他学期",
                    onClick = { viewModel.clearEduData() },
                    destructive = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Class time section - entry to sub-page
        SettingsSection("\u4e0a\u8bfe\u65f6\u95f4") {
            SettingsItem(
                icon = Icons.Default.DateRange,
                title = "\u7ba1\u7406\u4e0a\u8bfe\u65f6\u95f4",
                subtitle = "\u8c03\u6574\u6bcf\u8282\u8bfe\u7684\u4e0a\u4e0b\u8bfe\u65f6\u95f4",
                onClick = { showClassTimePage = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("显示设置") {
            CourseCardDensitySelector(
                current = state.courseTextSize,
                onSelect = viewModel::updateCourseTextSize
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection("后台提醒") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "允许准时上课提醒",
                subtitle = "打开通知、闹钟和后台运行权限后，清掉后台也更容易准时提醒",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }.onFailure {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About section
        SettingsSection("\u5173\u4e8e") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Bu\u8bfe\u8868",
                subtitle = "$ABOUT_VERSION_NAME \u00b7 \u8f7b\u91cf\u8bfe\u7a0b\u8868 \u00b7 GitHub",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ABOUT_GITHUB_URL)))
                }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // Time editor dialog
    showTimeEditor?.let { classTime ->
        TimeEditDialog(
            classTime = classTime,
            onDismiss = { showTimeEditor = null },
            onConfirm = { updated ->
                viewModel.updateClassTime(updated)
                showTimeEditor = null
            }
        )
    }

    // Week picker dialog
    // Class time management sub-page
    if (showClassTimePage) {
        ClassTimeManagementPage(
            classTimes = state.classTimes,
            onBack = { showClassTimePage = false },
            onEditTime = { showTimeEditor = it },
        )
    }

    if (showWeekPicker) {
        WeekPickerDialog(
            viewingWeek = state.viewingWeek,
            currentWeek = state.currentWeek,
            totalWeeks = state.totalWeeks,
            onDismiss = { showWeekPicker = false },
            onConfirm = { week ->
                viewModel.updateViewingWeek(week)
                showWeekPicker = false
            }
        )
    }

    if (showNewSemesterDialog) {
        NewSemesterDialog(
            onDismiss = { showNewSemesterDialog = false },
            onConfirm = { name ->
                viewModel.createSemester(name)
                showNewSemesterDialog = false
            }
        )
    }

    if (showExportSemesterDialog) {
        ExportSemesterDialog(
            semesters = state.semesters,
            onDismiss = { showExportSemesterDialog = false },
            onExport = { semester ->
                viewModel.exportSemesterCsv(semester.id) { csv ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, "${semester.name}.csv")
                        putExtra(Intent.EXTRA_TEXT, csv)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "导出课程 CSV"))
                }
                showExportSemesterDialog = false
            }
        )
    }
}

@Composable
private fun SemesterRow(
    semester: Semester,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = semester.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = "${semester.courseCount} 门课",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NewSemesterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("新学期") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增学期", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("学期名称") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.ifBlank { "新学期" }) }) {
                Text("创建", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ExportSemesterDialog(
    semesters: List<Semester>,
    onDismiss: () -> Unit,
    onExport: (Semester) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择导出的学期", fontWeight = FontWeight.Bold) },
        text = {
            if (semesters.isEmpty()) {
                Text(
                    text = "暂无可导出的学期",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    semesters.forEach { semester ->
                        Surface(
                            onClick = { onExport(semester) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = semester.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${semester.courseCount} 门课",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CourseCardDensitySelector(
    current: String,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        Triple("small", "紧凑", "更多信息"),
        Triple("medium", "标准", "均衡显示"),
        Triple("large", "舒展", "大字精简")
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "课程卡片密度",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "影响每周视图卡片的字号、间距和信息量",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label, hint) ->
                val selected = current == value
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(value) },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label)
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun TimeEditDialog(
    classTime: ClassTime,
    onDismiss: () -> Unit,
    onConfirm: (ClassTime) -> Unit
) {
    var startHour by remember(classTime) { mutableIntStateOf(classTime.startTime.split(":")[0].toIntOrNull() ?: 8) }
    var startMin by remember(classTime) { mutableIntStateOf(classTime.startTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0) }
    var endHour by remember(classTime) { mutableIntStateOf(classTime.endTime.split(":")[0].toIntOrNull() ?: 8) }
    var endMin by remember(classTime) { mutableIntStateOf(classTime.endTime.split(":").getOrNull(1)?.toIntOrNull() ?: 45) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("第 ${classTime.sectionNumber} 节时间", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("开始时间", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        WheelNumberPicker(
                            items = (0..23).toList(),
                            selectedIndex = startHour,
                            onSelectedChange = { startHour = it },
                            modifier = Modifier.weight(1f).height(160.dp)
                        )
                        Text(":", modifier = Modifier.align(Alignment.CenterVertically), fontWeight = FontWeight.Bold)
                        WheelNumberPicker(
                            items = (0..59 step 5).toList(),
                            selectedIndex = (0..59 step 5).indexOfFirst { it == startMin }.coerceAtLeast(0),
                            onSelectedChange = { idx -> startMin = (0..59 step 5).elementAt(idx) },
                            modifier = Modifier.weight(1f).height(160.dp)
                        )
                    }
                }
                Column {
                    Text("结束时间", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        WheelNumberPicker(
                            items = (0..23).toList(),
                            selectedIndex = endHour,
                            onSelectedChange = { endHour = it },
                            modifier = Modifier.weight(1f).height(160.dp)
                        )
                        Text(":", modifier = Modifier.align(Alignment.CenterVertically), fontWeight = FontWeight.Bold)
                        WheelNumberPicker(
                            items = (0..59 step 5).toList(),
                            selectedIndex = (0..59 step 5).indexOfFirst { it == endMin }.coerceAtLeast(0),
                            onSelectedChange = { idx -> endMin = (0..59 step 5).elementAt(idx) },
                            modifier = Modifier.weight(1f).height(160.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    ClassTime(
                        sectionNumber = classTime.sectionNumber,
                        startTime = "${startHour.toString().padStart(2, '0')}:${startMin.toString().padStart(2, '0')}",
                        endTime = "${endHour.toString().padStart(2, '0')}:${endMin.toString().padStart(2, '0')}"
                    )
                )
            }) { Text("保存", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun WheelNumberPicker(
    items: List<Int>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeightDp = 40.dp
    val pickerHeight = 160.dp
    val centerPadding = (pickerHeight - itemHeightDp) / 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex))

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            onSelectedChange(listState.firstVisibleItemIndex)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            contentPadding = PaddingValues(vertical = centerPadding),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items.size) { index ->
                val isSelected = listState.firstVisibleItemIndex == index && !listState.isScrollInProgress
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index].toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Center highlight bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
        )
    }
}

@Composable
private fun WeekPickerDialog(
    viewingWeek: Int,
    currentWeek: Int,
    totalWeeks: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedWeek by remember { mutableIntStateOf(viewingWeek) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u8bbe\u7f6e\u67e5\u770b\u5468\u6b21", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "\u6b63\u5728\u67e5\u770b${AcademicWeekResolver.formatViewingWeekLabel(selectedWeek, currentWeek)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = selectedWeek.toFloat(),
                    onValueChange = { selectedWeek = it.toInt() },
                    valueRange = 1f..totalWeeks.toFloat(),
                    steps = (totalWeeks - 2).coerceAtLeast(0)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedWeek) }) { Text("\u786e\u5b9a", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") } }
    )
}
@Composable
private fun ClassTimeManagementPage(
    classTimes: List<ClassTime>,
    onBack: () -> Unit,
    onEditTime: (ClassTime) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .zIndex(10f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Back button + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "返回")
                }
                Text(
                    text = "上课时间",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "点击修改每节课的上下课时间",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection("节次时间") {
                classTimes.forEach { classTime ->
                    SettingsItem(
                        icon = Icons.Default.DateRange,
                        title = "第 ${classTime.sectionNumber} 节",
                        subtitle = "${classTime.startTime} - ${classTime.endTime}",
                        onClick = { onEditTime(classTime) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
