package com.techenglish.app.feature.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.ErrorStateView
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoMetricTextStyle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.categoryIconVector
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.domain.model.CategoryProgress
import com.techenglish.domain.model.DayActivity
import com.techenglish.domain.model.UserStats

@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AuroraScreenContainer {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp).clickable(onClick = onBack)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад", modifier = Modifier.size(20.dp))
                        }
                    }
                    Text("Статистика обучения", style = MaterialTheme.typography.headlineSmall)
                }

                when (val current = state) {
                    StatisticsUiState.Loading -> SkeletonListPlaceholder()
                    is StatisticsUiState.Error -> ErrorStateView(message = current.message)
                    is StatisticsUiState.Content -> StatisticsContent(stats = current.stats)
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    stats: UserStats,
    modifier: Modifier = Modifier
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    var periodIdx by remember { mutableIntStateOf(2) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = navigationBarContentPadding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 2x2 KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatKpiCard(
                        label = "Изучено слов",
                        value = "${stats.learnedWords} / ${stats.totalWords}",
                        icon = Icons.Rounded.School,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatKpiCard(
                        label = "Точность",
                        value = "${stats.accuracyPercent}%",
                        icon = Icons.Rounded.CheckCircle,
                        accent = if (isDark) TechColors.Emerald else TechColors.EmeraldDark,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatKpiCard(
                        label = "Серия дней",
                        value = "${stats.streakDays} дн.",
                        icon = Icons.Rounded.LocalFireDepartment,
                        accent = if (isDark) TechColors.Amber else TechColors.AmberDark,
                        modifier = Modifier.weight(1f)
                    )
                    StatKpiCard(
                        label = "Опыт и уровень",
                        value = "Ур.${stats.levelProgress.level} · ${stats.levelProgress.totalXp} XP",
                        icon = Icons.Rounded.Bolt,
                        accent = if (isDark) TechColors.Cyan else TechColors.CyanDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Daily Goal Progress
        item {
            BentoGlassCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ежедневная цель", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${stats.dailyProgress.reviewedCardsToday} / ${stats.dailyProgress.dailyGoal} карточек",
                            style = MonoTagTextStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(stats.dailyProgress.goalProgressFraction.coerceIn(0.04f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Повторений всего: ${stats.reviewedTotal}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Статей: ${stats.completedDocsCount} • Сценариев: ${stats.completedScenariosCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Single Unified Activity Card with 7Д | 30Д | 365Д Switcher
        item {
            BentoGlassCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Активность обучения", style = MaterialTheme.typography.titleMedium)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                listOf("7Д", "30Д", "365Д").forEachIndexed { idx, label ->
                                    val active = periodIdx == idx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .clickable { periodIdx = idx }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MonoTagTextStyle,
                                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    when (periodIdx) {
                        0 -> WeeklyBarChart(days = stats.weeklyActivity)
                        1 -> HeatmapGrid(days = stats.monthlyActivity, columns = 10, isDark = isDark)
                        else -> HeatmapGrid(days = stats.yearlyActivity.takeLast(48), columns = 12, isDark = isDark)
                    }
                }
            }
        }

        item {
            Text("Прогресс по категориям", style = MaterialTheme.typography.titleMedium)
        }

        items(stats.categoryProgress, key = { it.categoryId }) { cat ->
            CategoryProgressRowCard(cat)
        }
    }
}

@Composable
private fun StatKpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    BentoGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Text(value, style = MonoMetricTextStyle, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(days: List<DayActivity>) {
    val maxCount = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val ratio = (day.count.toFloat() / maxCount.toFloat()).coerceIn(0.08f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (day.count > 0) "${day.count}" else "",
                    style = MonoTagTextStyle,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.52f)
                        .height((76f * ratio).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (day.count > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(day.dayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun HeatmapGrid(days: List<DayActivity>, columns: Int, isDark: Boolean) {
    val rows = days.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.forEach { rowDays ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                rowDays.forEach { day ->
                    val color = when {
                        day.count == 0 -> MaterialTheme.colorScheme.surfaceContainerHigh
                        day.count <= 3 -> if (isDark) Color(0xFF064E3B) else Color(0xFFA7F3D0)
                        day.count <= 10 -> if (isDark) Color(0xFF10B981) else Color(0xFF34D399)
                        else -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.35f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
                repeat((columns - rowDays.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressRowCard(cat: CategoryProgress) {
    BentoGlassCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = categoryIconVector(cat.categoryIcon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(cat.categoryName, style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    text = "${cat.learnedWords}/${cat.totalWords} (освоено ${cat.masteredWords})",
                    style = MonoTagTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(cat.progressFraction.coerceIn(0.03f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
