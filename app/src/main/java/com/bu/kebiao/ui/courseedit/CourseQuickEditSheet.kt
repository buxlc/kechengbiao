package com.bu.kebiao.ui.courseedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bu.kebiao.ui.theme.CourseColors
import com.bu.kebiao.ui.theme.CourseColorsSoft

@Composable
fun CourseQuickEditSheet(
    draft: CourseEditDraft,
    isEditMode: Boolean,
    canSave: Boolean,
    onDraftChange: (CourseEditDraft) -> Unit,
    onSave: () -> Unit,
    onMoreSettings: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = if (isEditMode) "\u5feb\u901f\u7f16\u8f91\u8bfe\u7a0b" else "\u5feb\u901f\u6dfb\u52a0\u8bfe\u7a0b",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "\u8fd9\u91cc\u5148\u6539\u540d\u79f0\u3001\u65f6\u95f4\u548c\u989c\u8272\uff1b\u9700\u8981\u66f4\u591a\u4fe1\u606f\u518d\u8fdb\u5b8c\u6574\u7f16\u8f91\u9875\u3002",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            label = { Text("\u8bfe\u7a0b\u540d\u79f0") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("\u661f\u671f", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        WeekdaySelector(selectedDay = draft.dayOfWeek, onDaySelected = { onDraftChange(draft.copy(dayOfWeek = it)) })

        Spacer(modifier = Modifier.height(16.dp))
        SectionRangeEditor(
            startSection = draft.startSection,
            endSection = draft.endSection,
            onStartSectionChange = { start ->
                onDraftChange(
                    draft.copy(
                        startSection = start,
                        endSection = draft.endSection.coerceAtLeast(start)
                    )
                )
            },
            onEndSectionChange = { end ->
                onDraftChange(draft.copy(endSection = end.coerceAtLeast(draft.startSection)))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("\u8bfe\u7a0b\u989c\u8272", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        CourseColorPicker(selectedIndex = draft.colorIndex, onColorSelected = { onDraftChange(draft.copy(colorIndex = it)) })

        Spacer(modifier = Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "\u9884\u89c8\uff1a\u5468${draft.dayOfWeek} \u00b7 \u7b2c${draft.startSection}-${draft.endSection}\u8282",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onSave,
                enabled = canSave,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("\u4fdd\u5b58", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                onClick = onMoreSettings,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("\u66f4\u591a\u8bbe\u7f6e", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (isEditMode && onDelete != null) {
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(6.dp))
                Text("\u5220\u9664\u8bfe\u7a0b", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun WeekdaySelector(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("\u5468\u4e00", "\u5468\u4e8c", "\u5468\u4e09", "\u5468\u56db", "\u5468\u4e94", "\u5468\u516d", "\u5468\u65e5")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val day = index + 1
            FilterChip(
                selected = selectedDay == day,
                onClick = { onDaySelected(day) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun SectionRangeEditor(
    startSection: Int,
    endSection: Int,
    onStartSectionChange: (Int) -> Unit,
    onEndSectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionStepper(
            label = "\u5f00\u59cb\u8282\u6b21",
            value = startSection,
            onDecrease = { onStartSectionChange((startSection - 1).coerceAtLeast(1)) },
            onIncrease = { onStartSectionChange((startSection + 1).coerceAtMost(30)) },
            modifier = Modifier.weight(1f)
        )
        SectionStepper(
            label = "\u7ed3\u675f\u8282\u6b21",
            value = endSection,
            onDecrease = { onEndSectionChange((endSection - 1).coerceAtLeast(1)) },
            onIncrease = { onEndSectionChange((endSection + 1).coerceAtMost(30)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SectionStepper(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDecrease) { Icon(Icons.Outlined.Remove, contentDescription = null) }
                Text(text = value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onIncrease) { Icon(Icons.Outlined.Add, contentDescription = null) }
            }
        }
    }
}

@Composable
fun CourseColorPicker(
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CourseColors.forEachIndexed { index, color ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(CourseColorsSoft[index])
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) color else color.copy(alpha = 0.22f),
                        shape = CircleShape
                    )
                    .padding(5.dp)
                    .clickable { onColorSelected(index) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
