package com.bu.kebiao.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class NavDestination(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home("课表", "home", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    Settings("设置", "settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun BuBottomNav(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavDestination.entries.forEach { destination ->
            val selected = currentDestination == destination
            NavItem(
                destination = destination,
                selected = selected,
                onClick = { onDestinationSelected(destination) }
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "nav-icon-color"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "nav-text-color"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 4.dp else 0.dp,
        label = "nav-indicator"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = destination.label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(indicatorWidth)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
