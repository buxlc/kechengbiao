package com.bu.kebiao.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bu.kebiao.domain.model.Course
import com.bu.kebiao.ui.theme.CourseColors
import com.bu.kebiao.ui.theme.CourseColorsSoft
import com.bu.kebiao.ui.theme.CourseColorsSoftDark

@Composable
fun CourseCard(
    course: Course,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val colorIndex = course.colorIndex % CourseColors.size
    val bgColor = if (isDark) CourseColorsSoftDark[colorIndex] else CourseColorsSoft[colorIndex]
    val accentColor = CourseColors[colorIndex]

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "card-scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(if (compact) 8.dp else 12.dp)
    ) {
        Column {
            // Section indicator dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))

            Text(
                text = course.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (course.location.isNotBlank()) {
                Text(
                    text = course.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (course.teacher.isNotBlank() && !compact) {
                Text(
                    text = course.teacher,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1
                )
            }

            Text(
                text = "第${course.startSection}-${course.endSection}节",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
