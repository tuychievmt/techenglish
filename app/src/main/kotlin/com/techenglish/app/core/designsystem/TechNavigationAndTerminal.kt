package com.techenglish.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Recessed dark-slate IDE terminal card with macOS window dots (used in both Dark & Light Ceramic).
 */
@Composable
fun IdeTerminalCard(
    fileName: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    trailingActionLabel: String? = null,
    onTrailingAction: (() -> Unit)? = null,
    compactHeader: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val bg = if (isDark) TechColors.TerminalObsidian else TechColors.TerminalSlate
    val borderCol = if (isDark) Color(0xFF242B42) else Color(0xFF1E293B)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Column(
            modifier = Modifier.padding(if (compactHeader) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactHeader) 8.dp else 10.dp)
        ) {
            if (compactHeader) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(TechColors.MacRed))
                            Box(Modifier.size(7.dp).clip(CircleShape).background(TechColors.MacYellow))
                            Box(Modifier.size(7.dp).clip(CircleShape).background(TechColors.MacGreen))
                        }
                        if (!badgeText.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF292042)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MonoTagTextStyle,
                                    color = Color(0xFFEDE9FE),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = fileName,
                                style = IpaTextStyle,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (trailingActionLabel != null && onTrailingAction != null) {
                        Text(
                            text = trailingActionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TechColors.Cyan,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(onClick = onTrailingAction)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(TechColors.MacRed))
                            Box(Modifier.size(9.dp).clip(CircleShape).background(TechColors.MacYellow))
                            Box(Modifier.size(9.dp).clip(CircleShape).background(TechColors.MacGreen))
                        }
                        if (!badgeText.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF292042)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MonoTagTextStyle,
                                    color = Color(0xFFEDE9FE),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (trailingActionLabel != null && onTrailingAction != null) {
                        Text(
                            text = trailingActionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TechColors.Cyan,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.clickable(onClick = onTrailingAction)
                        )
                    } else {
                        Text(
                            text = fileName,
                            style = IpaTextStyle,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            content()
        }
    }
}

@Immutable
data class CapsuleDockItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Detached floating frosted capsule navigation dock replacing the rectangular NavigationBar.
 */
@Composable
fun FloatingCapsuleNavDock(
    items: List<CapsuleDockItem>,
    selectedRoute: String?,
    onSelect: (CapsuleDockItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalTechInteractionConfig.current
    val isDark = config.isDarkTheme
    val dockBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f)
    else Color.White.copy(alpha = 0.94f)
    val dockBorder = if (isDark) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            color = dockBg,
            border = BorderStroke(1.dp, dockBorder),
            shadowElevation = if (isDark) 12.dp else 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = selectedRoute == item.route
                    val targetBg = when {
                        selected && isDark -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        selected -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    }
                    val targetContent = when {
                        selected && isDark -> MaterialTheme.colorScheme.onPrimaryContainer
                        selected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val pillBg by animateColorAsState(
                        targetValue = targetBg,
                        animationSpec = tween(durationMillis = if (config.reduceMotionEnabled) 0 else 100),
                        label = "dock_pill_bg"
                    )
                    val tintColor by animateColorAsState(
                        targetValue = targetContent,
                        animationSpec = tween(durationMillis = if (config.reduceMotionEnabled) 0 else 100),
                        label = "dock_pill_fg"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(pillBg)
                            .clickable(role = Role.Tab) { onSelect(item) }
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary else tintColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = tintColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sleek rounded pill segmented switch matching Stitch Obsidian & Ceramic Glass.
 */
@Composable
fun SegmentedPillToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val trackColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerLowest
    else MaterialTheme.colorScheme.surfaceContainerHigh

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = trackColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { idx, label ->
                val selected = idx == selectedIndex
                val pillBg = when {
                    selected && isDark -> MaterialTheme.colorScheme.primaryContainer
                    selected -> MaterialTheme.colorScheme.surface
                    else -> Color.Transparent
                }
                val textColor = when {
                    selected && isDark -> MaterialTheme.colorScheme.onPrimaryContainer
                    selected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(pillBg)
                        .then(
                            if (selected && isDark) {
                                Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                                    RoundedCornerShape(50)
                                )
                            } else Modifier
                        )
                        .clickable { onSelect(idx) }
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
