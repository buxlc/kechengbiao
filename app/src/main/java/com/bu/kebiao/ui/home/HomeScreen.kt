package com.bu.kebiao.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bu.kebiao.domain.model.AcademicWeekResolver
import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.ui.courseedit.CourseConflictSheet
import com.bu.kebiao.ui.courseedit.CourseEditDraft
import com.bu.kebiao.ui.courseedit.CourseQuickEditSheet
import com.bu.kebiao.ui.courseedit.createDefaultCourseDraft
import com.bu.kebiao.ui.courseedit.suggestedCourseColorIndex
import com.bu.kebiao.ui.courseedit.toEditDraft
import com.bu.kebiao.ui.theme.BuDuration
import com.bu.kebiao.ui.theme.BuEasing
import com.bu.kebiao.ui.theme.CourseColors
import com.bu.kebiao.ui.theme.CourseColorsSoft
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onImportClick: () -> Unit,
    onOpenCourseEditor: (CourseEditDraft) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cardLayout = remember(state.courseTextSize) { courseCardTextLayout(state.courseTextSize) }
    var quickDraft by remember { mutableStateOf<CourseEditDraft?>(null) }
    var quickEditTarget by remember { mutableStateOf<Course?>(null) }
    var conflictCourses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var showWeekPicker by remember { mutableStateOf(false) }

    fun openCourseEntry(courses: List<Course>) {
        when {
            courses.isEmpty() -> Unit
            courses.size == 1 -> {
                val course = courses.first()
                quickEditTarget = course
                quickDraft = course.toEditDraft(
                    defaultTotalWeeks = state.totalWeeks,
                    resolvedColorIndex = course.colorIndex
                )
            }
            else -> conflictCourses = courses
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            TopBar(
                viewingWeek = state.viewingWeek,
                currentWeek = state.currentWeek,
                totalWeeks = state.totalWeeks,
                onPrevWeek = viewModel::prevWeek,
                onNextWeek = viewModel::nextWeek,
                onWeekClick = { showWeekPicker = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
            SegmentButton(isTodayView = state.isTodayView, onToggle = viewModel::setTodayView)
            Spacer(modifier = Modifier.height(18.dp))

            if (!state.hasImported && state.courses.isEmpty()) {
                FirstImportCard(onClick = onImportClick)
                Spacer(modifier = Modifier.height(18.dp))
            }

            if (state.isTodayView) {
                DaySelector(selectedDay = state.selectedDay, onDaySelected = viewModel::setSelectedDay)
                Spacer(modifier = Modifier.height(12.dp))
            }

            AnimatedContent(
                targetState = state.isTodayView,
                transitionSpec = {
                    (
                        slideInHorizontally(
                            initialOffsetX = { if (targetState) -it else it },
                            animationSpec = tween(BuDuration.Medium, easing = BuEasing.Emphasized)
                        ) + fadeIn()
                    ).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { if (targetState) it else -it },
                            animationSpec = tween(BuDuration.Medium, easing = BuEasing.Emphasized)
                        ) + fadeOut()
                    )
                },
                label = "home-view"
            ) { isTodayView ->
                if (isTodayView) {
                    TodayView(
                        courses = state.todayCourses,
                        classTimes = state.classTimes,
                        fontScale = cardLayout.fontScale,
                        onCoursesClick = ::openCourseEntry
                    )
                } else {
                    WeekView(
                        courses = state.courses,
                        classTimes = state.classTimes,
                        viewingWeek = state.viewingWeek,
                        semesterStartDate = state.semesterStartDate,
                        cardLayout = cardLayout,
                        onCoursesClick = ::openCourseEntry
                    )
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }

        FloatingActionButton(
            onClick = {
                quickEditTarget = null
                quickDraft = createDefaultCourseDraft(
                    dayOfWeek = state.selectedDay,
                    totalWeeks = state.totalWeeks,
                    colorIndex = suggestedCourseColorIndex("")
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 100.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "\u6dfb\u52a0\u8bfe\u7a0b", tint = Color.White)
        }
    }

    quickDraft?.let { draft ->
        ModalBottomSheet(
            onDismissRequest = {
                quickDraft = null
                quickEditTarget = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CourseQuickEditSheet(
                draft = draft,
                isEditMode = quickEditTarget != null,
                canSave = draft.name.trim().isNotBlank() && draft.endSection >= draft.startSection,
                onDraftChange = { nextDraft ->
                    val mappedColor = state.colorMap[nextDraft.name.trim()]
                    quickDraft = if (mappedColor != null && nextDraft.name.trim().isNotBlank()) {
                        nextDraft.copy(colorIndex = mappedColor)
                    } else {
                        nextDraft
                    }
                },
                onSave = {
                    viewModel.saveCourseDraft(draft)
                    quickDraft = null
                    quickEditTarget = null
                },
                onMoreSettings = {
                    onOpenCourseEditor(draft)
                    quickDraft = null
                    quickEditTarget = null
                },
                onDelete = quickEditTarget?.let { course ->
                    {
                        viewModel.deleteCourse(course)
                        quickDraft = null
                        quickEditTarget = null
                    }
                }
            )
        }
    }

    if (conflictCourses.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { conflictCourses = emptyList() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CourseConflictSheet(
                courses = conflictCourses,
                onCourseSelected = { course ->
                    conflictCourses = emptyList()
                    quickEditTarget = course
                    quickDraft = course.toEditDraft(
                        defaultTotalWeeks = state.totalWeeks,
                        resolvedColorIndex = course.colorIndex
                    )
                }
            )
        }
    }

    if (showWeekPicker) {
        WeekPickerDialog(
            viewingWeek = state.viewingWeek,
            currentWeek = state.currentWeek,
            totalWeeks = state.totalWeeks,
            onDismiss = { showWeekPicker = false },
            onConfirm = {
                viewModel.setCurrentWeek(it)
                showWeekPicker = false
            }
        )
    }
}

@Composable
private fun TopBar(
    viewingWeek: Int,
    currentWeek: Int,
    totalWeeks: Int,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onWeekClick: () -> Unit
) {
    val today = LocalDate.now()
    val dateText = "${today.year}\u5e74${today.monthValue}\u6708${today.dayOfMonth}\u65e5 \u00b7 ${getDayName(today.dayOfWeek.value)}"
    val weekLabel = AcademicWeekResolver.formatViewingWeekLabel(viewingWeek, currentWeek)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text("Bu\u8bfe\u8868", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(dateText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Surface(
            onClick = onWeekClick,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(weekLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onPrevWeek, enabled = viewingWeek > 1, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "\u4e0a\u4e00\u5468", modifier = Modifier.size(16.dp))
                    }
                    Text("$viewingWeek/$totalWeeks", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = onNextWeek, enabled = viewingWeek < totalWeeks, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "\u4e0b\u4e00\u5468", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentButton(isTodayView: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(4.dp)) {
            SegBtn("\u4eca\u5929", isTodayView, { onToggle(true) }, Modifier.weight(1f))
            SegBtn("\u672c\u5468", !isTodayView, { onToggle(false) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SegBtn(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "seg-bg"
    )
    val fgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "seg-fg"
    )
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = bgColor, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = fgColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun DaySelector(selectedDay: Int, onDaySelected: (Int) -> Unit) {
    val labels = listOf("\u5468\u4e00", "\u5468\u4e8c", "\u5468\u4e09", "\u5468\u56db", "\u5468\u4e94", "\u5468\u516d", "\u5468\u65e5")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            val day = index + 1
            val selected = day == selectedDay
            val bgColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                label = "day-bg"
            )
            val fgColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "day-fg"
            )
            Surface(onClick = { onDaySelected(day) }, shape = RoundedCornerShape(10.dp), color = bgColor, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(text = label, style = MaterialTheme.typography.labelSmall, color = fgColor)
                }
            }
        }
    }
}

@Composable
private fun FirstImportCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("\u5bfc\u5165\u4f60\u7684\u8bfe\u8868", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "\u652f\u6301\u6559\u52a1\u5bfc\u5165\uff0c\u4e5f\u53ef\u4ee5\u5148\u624b\u52a8\u6dfb\u52a0\u8bfe\u7a0b\u3002",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun TodayView(
    courses: List<Course>,
    classTimes: List<ClassTime>,
    fontScale: Float = 1f,
    onCoursesClick: (List<Course>) -> Unit
) {
    if (courses.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))
                Text("\u4eca\u5929\u6ca1\u6709\u8bfe\u7a0b", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("\u4eab\u53d7\u4f60\u7684\u4e00\u5929\u5427 \uD83C\uDF89", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        return
    }

    val groups = courses
        .sortedWith(compareBy<Course> { it.startSection }.thenBy { it.endSection }.thenBy { it.name })
        .groupBy { it.startSection to it.endSection }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { (sectionRange, groupCourses) ->
            val startTime = classTimes.find { it.sectionNumber == sectionRange.first }?.startTime.orEmpty()
            val endTime = classTimes.find { it.sectionNumber == sectionRange.second }?.endTime.orEmpty()
            val sectionCount = (sectionRange.second - sectionRange.first + 1).coerceAtLeast(1)
            val cardHeight = (54 * sectionCount + 6 * (sectionCount - 1)).dp

            Row(modifier = Modifier.fillMaxWidth().height(cardHeight), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                TodayTimeRail(
                    startTime = startTime,
                    endTime = endTime,
                    sectionRangeText = formatSectionRange(sectionRange.first, sectionRange.second),
                    modifier = Modifier.width(54.dp).fillMaxHeight()
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (sectionRange.first..sectionRange.second).forEach { section ->
                        TodaySectionCard(
                            courses = groupCourses,
                            section = section,
                            fontScale = fontScale,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            onClick = { onCoursesClick(groupCourses) }
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun WeekView(
    courses: List<Course>,
    classTimes: List<ClassTime>,
    viewingWeek: Int,
    semesterStartDate: Long,
    cardLayout: CourseCardTextLayout = courseCardTextLayout("medium"),
    onCoursesClick: (List<Course>) -> Unit
) {
    val filteredCourses = remember(courses, viewingWeek) {
        courses
            .filter { it.isActiveInWeek(viewingWeek) }
            .sortedWith(compareBy<Course> { it.dayOfWeek }.thenBy { it.startSection }.thenBy { it.endSection }.thenBy { it.name })
    }
    val placements = remember(filteredCourses) { calculateWeekPlacements(filteredCourses) }
    val maxSection = remember(filteredCourses, classTimes) {
        maxOf(filteredCourses.maxOfOrNull { it.endSection } ?: 0, classTimes.maxOfOrNull { it.sectionNumber } ?: 0, 12)
    }
    val timeBuckets = remember(classTimes, maxSection) { buildWeekTimeBuckets(classTimes, maxSection) }
    val sectionHeight = 64.dp
    val headerHeight = 56.dp
    val leftWidth = 32.dp
    val gridHeight = sectionHeight * maxSection
    val contentHeight = gridHeight + headerHeight + 8.dp
    val viewportHeight = minOf(contentHeight, 620.dp)
    val gridViewportHeight = viewportHeight - headerHeight
    val headerDates = remember(viewingWeek, semesterStartDate) {
        buildWeekHeaderDates(
            currentWeek = viewingWeek,
            semesterStartDateMillis = semesterStartDate
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(viewportHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f))
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        val dayWidth = (maxWidth - leftWidth) / 7f

        Row(modifier = Modifier.fillMaxWidth().height(headerHeight), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.width(leftWidth).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = headerDates.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            headerDates.days.forEach { day ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (day.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                    modifier = Modifier.width(dayWidth)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = day.weekdayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .offset(y = headerHeight)
                .fillMaxWidth()
                .height(gridViewportHeight)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(gridHeight)) {
                timeBuckets.forEach { bucket ->
                    val topOffset = sectionHeight * (bucket.startSection - 1)
                    val bucketHeight = sectionHeight * (bucket.endSection - bucket.startSection + 1)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        modifier = Modifier.offset(y = topOffset + 1.dp).width(leftWidth - 2.dp).height(bucketHeight - 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = bucket.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (bucket.startTime.isNotBlank() || bucket.endTime.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = bucket.startTime.ifBlank { "--:--" },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .height(1.dp)
                                        .padding(vertical = 1.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                )
                                Text(
                                    text = bucket.endTime.ifBlank { "--:--" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    repeat(7) { dayIndex ->
                        Box(
                            modifier = Modifier
                                .offset(x = leftWidth + dayWidth * dayIndex.toFloat(), y = topOffset + 1.dp)
                                .width(dayWidth - 1.dp)
                                .height(bucketHeight - 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f))
                        )
                    }
                }

                placements.forEach { placement ->
                    val dayOffset = leftWidth + dayWidth * (placement.group.dayOfWeek - 1).toFloat()
                    val courseWidth = dayWidth / placement.columnCount.toFloat()
                    val xOffset = dayOffset + courseWidth * placement.columnIndex.toFloat() + 0.5.dp
                    val yOffset = sectionHeight * (placement.group.startSection - 1) + 2.dp
                    val blockHeight = sectionHeight * (placement.group.endSection - placement.group.startSection + 1) - 4.dp

                    WeekCourseBlock(
                        layout = cardLayout,
                        courses = placement.group.courses,
                        isOverlapping = placement.columnCount > 1,
                        onClick = { onCoursesClick(placement.detailCourses) },
                        modifier = Modifier.offset(x = xOffset, y = yOffset).width(courseWidth - 1.dp).height(blockHeight)
                    )
                }

                if (filteredCourses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.TopCenter) {
                        Text("\u7b2c$viewingWeek\u5468\u6ca1\u6709\u8bfe\u7a0b", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayTimeRail(
    startTime: String,
    endTime: String,
    sectionRangeText: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .padding(top = 14.dp, bottom = 18.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
        )
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            TimeRailLabel(text = startTime.ifBlank { "--:--" }, emphasized = true)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimeRailLabel(text = endTime.ifBlank { "--:--" })
                Text(sectionRangeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TimeRailLabel(text: String, emphasized: Boolean = false) {
    Surface(shape = RoundedCornerShape(8.dp), color = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TodaySectionCard(
    fontScale: Float = 1f,
    courses: List<Course>,
    section: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val first = courses.first()
    val color = CourseColors[first.colorIndex % CourseColors.size]
    val bgColor = CourseColorsSoft[first.colorIndex % CourseColorsSoft.size]
    val subtitle = buildCourseSubtitle(first)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = modifier.border(1.dp, color.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(99.dp)).background(color))
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(first.name, style = MaterialTheme.typography.labelLarge, fontSize = (14 * fontScale).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (courses.size > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        ConflictBadge(count = courses.size)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle.ifBlank { "\u7b2c${section}\u8282" },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = (11 * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
                Text("${section}\u8282", style = MaterialTheme.typography.labelSmall, fontSize = (11 * fontScale).sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
private fun WeekCourseBlock(
    layout: CourseCardTextLayout = courseCardTextLayout("medium"),
    courses: List<Course>,
    modifier: Modifier = Modifier,
    isOverlapping: Boolean = false,
    onClick: () -> Unit = {}
) {
    val first = courses.first()
    val color = CourseColors[first.colorIndex % CourseColors.size]
    val bgColor = CourseColorsSoft[first.colorIndex % CourseColorsSoft.size]

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = layout.horizontalPaddingDp.dp, vertical = layout.verticalPaddingDp.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(14.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(color))
                if (courses.size > 1 || isOverlapping) {
                    Spacer(modifier = Modifier.weight(1f))
                    ConflictBadge(count = courses.size.takeIf { it > 1 })
                }
            }
            Spacer(modifier = Modifier.height(layout.accentSpacingDp.dp))
            Text(
                first.name,
                style = MaterialTheme.typography.titleSmall,
                fontSize = (13 * layout.fontScale).sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (courses.size > 1) (layout.titleMaxLines - 1).coerceAtLeast(2) else layout.titleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = buildWeekCourseSubtitle(first, layout)
            if (subtitle.isNotBlank() || layout.showDetailFallback) {
                Spacer(modifier = Modifier.height(layout.detailSpacingDp.dp))
                Text(
                    text = subtitle.ifBlank { "\u70b9\u51fb\u67e5\u770b\u8be6\u60c5" },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = (11 * layout.fontScale).sp,
                    color = if (courses.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (courses.size > 1) (layout.detailMaxLines + 1).coerceAtLeast(2) else layout.detailMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ConflictBadge(count: Int? = null) {
    Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Text(
            text = if (count != null && count > 1) "+${count - 1}" else "\u51b2\u7a81",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            maxLines = 1
        )
    }
}

private fun buildCourseSubtitle(course: Course): String {
    return listOf(course.location, course.teacher).filter { it.isNotBlank() }.joinToString(" \u00b7 ")
}

private fun buildWeekCourseSubtitle(course: Course, layout: CourseCardTextLayout): String {
    val details = mutableListOf<String>()
    if (course.location.isNotBlank()) details += course.location
    if (course.teacher.isNotBlank()) details += course.teacher
    if (layout.includeWeekRange) {
        details += formatSectionRange(course.startSection, course.endSection)
        details += formatCourseWeeks(course)
    }
    return details.filter { it.isNotBlank() }.joinToString("\n")
}


private fun formatSectionRange(startSection: Int, endSection: Int, prefix: Boolean = true): String {
    val body = if (startSection == endSection) "${startSection}\u8282" else "${startSection}-${endSection}\u8282"
    return if (prefix) "\u7b2c$body" else body
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
        confirmButton = { TextButton(onClick = { onConfirm(selectedWeek) }) { Text("\u786e\u5b9a") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") } },
        title = { Text("\u5207\u6362\u67e5\u770b\u5468\u6570", fontWeight = FontWeight.Bold) },
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
        shape = RoundedCornerShape(20.dp)
    )
}

private fun getDayName(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "\u5468\u4e00"
    2 -> "\u5468\u4e8c"
    3 -> "\u5468\u4e09"
    4 -> "\u5468\u56db"
    5 -> "\u5468\u4e94"
    6 -> "\u5468\u516d"
    7 -> "\u5468\u65e5"
    else -> ""
}

private fun formatCourseWeeks(course: Course): String {
    if (course.weeks.isNotEmpty()) return "\u7b2c${course.weeks.joinToString(",")}\u5468"
    return "\u7b2c${course.startWeek}-${course.endWeek}\u5468"
}
