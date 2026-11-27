package com.techenglish.app.core.designsystem

import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techenglish.domain.model.ChameleonThemeType
import com.techenglish.domain.model.ComboStreakState
import com.techenglish.domain.model.DayActivity

// ==================== 365-DAY GITHUB CONTRIBUTION GRAPH (УИ-2.2) ====================

@Composable
fun GitHubContributionGraph(
    yearlyActivity: List<DayActivity>,
    modifier: Modifier = Modifier
) {
    val days = if (yearlyActivity.isEmpty()) {
        List(365) { idx -> DayActivity(idx.toLong(), "", 0, 0) }
    } else {
        yearlyActivity
    }
    val activeDaysCount = remember(days) { days.count { it.count > 0 } }
    val totalActionsCount = remember(days) { days.sumOf { it.count } }
    val weeks = remember(days) { days.chunked(7) }
    val scrollState = rememberScrollState()
    val isDark = LocalTechInteractionConfig.current.isDarkTheme

    LaunchedEffect(weeks.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    fun cellColor(level: Int): Color = when (level) {
        0 -> if (isDark) Color(0xFF1F2937) else Color(0xFFE2E8F0)
        1 -> if (isDark) Color(0xFF064E3B) else Color(0xFFA7F3D0)
        2 -> if (isDark) Color(0xFF047857) else Color(0xFF34D399)
        3 -> if (isDark) Color(0xFF10B981) else Color(0xFF059669)
        else -> if (isDark) Color(0xFF34D399) else Color(0xFF064E3B)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "График активности GitHub за 365 дней: активных дней $activeDaysCount, всего действий $totalActionsCount"
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GitHub Contribution Graph (365 дн.)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Активных дней: $activeDaysCount • Действий за год: $totalActionsCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${days.size}d",
                    style = MonoTagTextStyle,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            val gridColumns = weeks.size.coerceAtLeast(1)
            val cellSizeDp = 11.dp
            val cellGapDp = 3.dp
            val totalWidthDp = (cellSizeDp * gridColumns) + (cellGapDp * (gridColumns - 1).coerceAtLeast(0))
            val totalHeightDp = (cellSizeDp * 7) + (cellGapDp * 6)
            val c0 = cellColor(0)
            val c1 = cellColor(1)
            val c2 = cellColor(2)
            val c3 = cellColor(3)
            val c4 = cellColor(4)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(totalWidthDp)
                        .height(totalHeightDp)
                ) {
                    val cellPx = cellSizeDp.toPx()
                    val gapPx = cellGapDp.toPx()
                    val stepPx = cellPx + gapPx
                    val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

                    weeks.forEachIndexed { colIdx, week ->
                        val x = colIdx * stepPx
                        week.forEachIndexed { rowIdx, day ->
                            val y = rowIdx * stepPx
                            val col = when (day.intensityLevel) {
                                0 -> c0
                                1 -> c1
                                2 -> c2
                                3 -> c3
                                else -> c4
                            }
                            drawRoundRect(
                                color = col,
                                topLeft = Offset(x, y),
                                size = Size(cellPx, cellPx),
                                cornerRadius = radius
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Меньше",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                (0..4).forEach { lvl ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(cellColor(lvl))
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Больше",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== COMBO STREAK BANNER WITH DEVELOPER HUMOR (УИ-3.2) ====================

@Composable
fun ComboStreakBanner(
    comboState: ComboStreakState,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalTechInteractionConfig.current.reduceMotionEnabled
    val glowAlpha = if (reduceMotion) {
        0.22f
    } else {
        val transition = rememberInfiniteTransition(label = "combo_glow")
        val animated by transition.animateFloat(
            initialValue = 0.16f,
            targetValue = 0.36f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "combo_glow_alpha"
        )
        animated
    }

    AnimatedVisibility(
        visible = comboState.isMilestoneActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val msg = comboState.message ?: return@AnimatedVisibility
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = TechColors.Emerald.copy(alpha = glowAlpha),
            border = BorderStroke(1.5.dp, TechColors.Emerald),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Комбо серия ${comboState.currentStreak}: $msg"
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = msg,
                            style = TermTitleTextStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Серия верных ответов подряд: ${comboState.currentStreak}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "COMBO x${comboState.currentStreak}",
                        style = MonoTagTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ==================== CHAMELEON INTERFACE CONTAINERS (УИ-2.4) ====================

@Composable
fun ChameleonScenarioContainer(
    themeType: ChameleonThemeType,
    headerTitle: String,
    headerSubtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val (headerBg, headerFg, borderCol, icon) = when (themeType) {
        ChameleonThemeType.GITHUB_PR -> Quad(
            first = if (isDark) Color(0xFF161B22) else Color(0xFFF6F8FA),
            second = if (isDark) Color(0xFF58A6FF) else Color(0xFF0969DA),
            third = if (isDark) Color(0xFF30363D) else Color(0xFFD0D7DE),
            fourth = Icons.AutoMirrored.Rounded.MergeType
        )
        ChameleonThemeType.SLACK -> Quad(
            first = if (isDark) Color(0xFF2B1233) else Color(0xFFF9F0FF),
            second = if (isDark) Color(0xFFE8912D) else Color(0xFF611F69),
            third = if (isDark) Color(0xFF4A154B) else Color(0xFFE9D5FF),
            fourth = Icons.Rounded.Forum
        )
        ChameleonThemeType.TERMINAL -> Quad(
            first = Color(0xFF0D1117),
            second = Color(0xFF56D364),
            third = Color(0xFF30363D),
            fourth = Icons.Rounded.Terminal
        )
        ChameleonThemeType.DEFAULT_IDE -> Quad(
            first = MaterialTheme.colorScheme.surfaceVariant,
            second = MaterialTheme.colorScheme.primary,
            third = MaterialTheme.colorScheme.outlineVariant,
            fourth = Icons.Rounded.Code
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Контейнер ${themeType.labelRu}: $headerTitle" },
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderCol),
        colors = CardDefaults.cardColors(
            containerColor = if (themeType == ChameleonThemeType.TERMINAL) Color(0xFF161B22)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Chameleon Chrome Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (themeType == ChameleonThemeType.TERMINAL) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                            Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                            Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                        }
                        Spacer(Modifier.width(4.dp))
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = headerFg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (themeType) {
                                ChameleonThemeType.GITHUB_PR -> "GitHub PR • feature/review → main"
                                ChameleonThemeType.SLACK -> "#engineering-sync • Slack Thread"
                                ChameleonThemeType.TERMINAL -> "bash — console — 80×24"
                                ChameleonThemeType.DEFAULT_IDE -> headerTitle
                            },
                            style = MonoTagTextStyle,
                            color = headerFg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = headerSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (themeType == ChameleonThemeType.TERMINAL) Color(0xFFB1BAC4)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = headerFg.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = themeType.badgeCode,
                        style = MonoTagTextStyle,
                        color = headerFg,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

