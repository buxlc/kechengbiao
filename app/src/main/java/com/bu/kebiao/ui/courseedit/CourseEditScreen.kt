package com.bu.kebiao.ui.courseedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    onBack: () -> Unit,
    viewModel: CourseEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onBack()
        }
    }

    val parsedWeeks = CourseWeekParser.parseWeeksText(state.draft.weeksText, state.totalWeeks)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "\u7f16\u8f91\u8bfe\u7a0b" else "\u65b0\u589e\u8bfe\u7a0b",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "\u8fd4\u56de")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = "\u8fd9\u91cc\u53ef\u4ee5\u6539\u5b8c\u6574\u4fe1\u606f\uff0c\u5305\u62ec\u8001\u5e08\u3001\u5730\u70b9\u3001\u5468\u6b21\u548c\u989c\u8272\u3002",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = state.draft.name,
                onValueChange = { viewModel.updateDraft { draft -> draft.copy(name = it) } },
                label = { Text("\u8bfe\u7a0b\u540d\u79f0") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.draft.teacher,
                onValueChange = { viewModel.updateDraft { draft -> draft.copy(teacher = it) } },
                label = { Text("\u8001\u5e08") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.draft.location,
                onValueChange = { viewModel.updateDraft { draft -> draft.copy(location = it) } },
                label = { Text("\u5730\u70b9") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))
            Text("\u661f\u671f", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            WeekdaySelector(
                selectedDay = state.draft.dayOfWeek,
                onDaySelected = { day -> viewModel.updateDraft { draft -> draft.copy(dayOfWeek = day) } }
            )

            Spacer(modifier = Modifier.height(18.dp))
            SectionRangeEditor(
                startSection = state.draft.startSection,
                endSection = state.draft.endSection,
                onStartSectionChange = { start ->
                    viewModel.updateDraft { draft ->
                        draft.copy(
                            startSection = start,
                            endSection = draft.endSection.coerceAtLeast(start)
                        )
                    }
                },
                onEndSectionChange = { end ->
                    viewModel.updateDraft { draft ->
                        draft.copy(endSection = end.coerceAtLeast(draft.startSection))
                    }
                }
            )

            Spacer(modifier = Modifier.height(18.dp))
            OutlinedTextField(
                value = state.draft.weeksText,
                onValueChange = { viewModel.updateDraft { draft -> draft.copy(weeksText = it) } },
                label = { Text("\u5468\u6b21\u89c4\u5219") },
                supportingText = { Text("\u652f\u6301 1-4,6-14\u30011-16\u5355\u5468\u30012-18\u53cc\u5468") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\u89e3\u6790\u7ed3\u679c\uff1a${CourseWeekParser.toDisplayText(parsedWeeks, state.totalWeeks)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text("\u8bfe\u7a0b\u989c\u8272", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            CourseColorPicker(
                selectedIndex = state.draft.colorIndex,
                onColorSelected = { color -> viewModel.updateDraft { draft -> draft.copy(colorIndex = color) } }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = viewModel::save,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(modifier = Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("\u4fdd\u5b58\u8bfe\u7a0b", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (state.isEditMode) {
                    Surface(
                        onClick = viewModel::deleteCourse,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(modifier = Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("\u5220\u9664\u8bfe\u7a0b", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!state.isEditMode) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("\u7a0d\u540e\u518d\u8bf4")
                }
            }
        }
    }
}
