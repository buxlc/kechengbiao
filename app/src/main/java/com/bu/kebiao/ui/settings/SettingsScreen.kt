package com.bu.kebiao.ui.settings

import android.app.DatePickerDialog
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
import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.ui.theme.BuOrange
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
                icon = Icons.Default.CalendarMonth,
                title = "\u5f53\u524d\u5468\u6b21",
                subtitle = "\u7b2c ${state.currentWeek} / ${state.totalWeeks} \u5468",
                onClick = { showWeekPicker = true }
            )
            }

        Spacer(modifier = Modifier.height(16.dp))

        // Import section
        SettingsSection("\u5bfc\u5165\u7ba1\u7406") {
            SettingsItem(
                icon = Icons.Default.CloudDownload,
                title = if (state.hasImported) "\u91cd\u65b0\u5bfc\u5165\u8bfe\u8868" else "\u5bfc\u5165\u8bfe\u8868",
                subtitle = if (state.eduSchool.isNotBlank()) "\u5df2\u5bfc\u5165: ${state.eduSchool}"
                else "\u6559\u52a1\u5bfc\u5165 / PDF\u5bfc\u5165",
                onClick = onImportClick
            )
            if (state.hasImported) {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "\u6e05\u9664\u6559\u52a1\u6570\u636e",
                    subtitle = "\u5220\u9664\u6240\u6709\u6559\u52a1\u5bfc\u5165\u7684\u8bfe\u7a0b",
                    onClick = { viewModel.clearEduData() },
                    destructive = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Class time section - entry to sub-page
        SettingsSection("\u4e0a\u8bfe\u65f6\u95f4") {
            SettingsItem(
                icon = Icons.Default.Schedule,
                title = "\u7ba1\u7406\u4e0a\u8bfe\u65f6\u95f4",
                subtitle = "\u8c03\u6574\u6bcf\u8282\u8bfe\u7684\u4e0a\u4e0b\u8bfe\u65f6\u95f4",
                onClick = { showClassTimePage = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About section
        SettingsSection("\u5173\u4e8e") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Bu\u8bfe\u8868",
                subtitle = "v1.0.0 \u00b7 \u8f7b\u91cf\u8bfe\u7a0b\u8868",
                onClick = {}
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
            currentWeek = state.currentWeek,
            totalWeeks = state.totalWeeks,
            onDismiss = { showWeekPicker = false },
            onConfirm = { week ->
                viewModel.updateCurrentWeek(week)
                showWeekPicker = false
            }
        )
    }
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
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ThemeSelector(
    current: String,
    onSelect: (String) -> Unit
) {
    val themes = listOf("system" to "\u8ddf\u968f\u7cfb\u7edf", "light" to "\u6d45\u8272", "dark" to "\u6df1\u8272")
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        themes.forEach { (value, label) ->
            val isSelected = current == value
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                } else null
            )
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
    currentWeek: Int,
    totalWeeks: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedWeek by remember { mutableIntStateOf(currentWeek) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u8bbe\u7f6e\u5f53\u524d\u5468\u6b21", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("\u5f53\u524d\u4e3a\u7b2c $selectedWeek \u5468", style = MaterialTheme.typography.bodyMedium)
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
                    Icon(Icons.Default.ChevronLeft, contentDescription = "返回")
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
                        icon = Icons.Default.Schedule,
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
