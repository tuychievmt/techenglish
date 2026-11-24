package com.techenglish.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.ErrorStateView
import com.techenglish.app.core.designsystem.GitHubContributionGraph
import com.techenglish.app.core.designsystem.MonoMetricTextStyle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.domain.model.StudyLog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onStartLearning: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onOpenAcademicDictionary: () -> Unit,
    onOpenLanguages: () -> Unit,
    onOpenCompare: () -> Unit,
    onOpenDocs: () -> Unit,
    onOpenScenarios: () -> Unit,
    onOpenListening: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenLeaderboards: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val current = state) {
        HomeUiState.Loading -> SkeletonListPlaceholder(itemCount = 5)
        is HomeUiState.Error -> ErrorStateView(message = current.message)
        is HomeUiState.Content -> HomeContent(
            content = current,
            onStartLearning = onStartLearning,
            onOpenVocabulary = onOpenVocabulary,
            onOpenAcademicDictionary = onOpenAcademicDictionary,
            onOpenLanguages = onOpenLanguages,
            onOpenCompare = onOpenCompare,
            onOpenDocs = onOpenDocs,
            onOpenScenarios = onOpenScenarios,
            onOpenListening = onOpenListening,
            onOpenStatistics = onOpenStatistics,
            onOpenLeaderboards = onOpenLeaderboards,
            onOpenFriends = onOpenFriends
        )
    }
}

@Composable
private fun HomeContent(
    content: HomeUiState.Content,
    onStartLearning: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onOpenAcademicDictionary: () -> Unit,
    onOpenLanguages: () -> Unit,
    onOpenCompare: () -> Unit,
    onOpenDocs: () -> Unit,
    onOpenScenarios: () -> Unit,
    onOpenListening: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenLeaderboards: () -> Unit,
    onOpenFriends: () -> Unit
) {
    val daily = content.dailyProgress
    val level = daily.levelProgress

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Greeting + Streak & XP pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${content.greetingRu}, ${content.userName}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Ур. ${level.level} • ${level.rankTitleRu}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricChip(
                        icon = Icons.Rounded.LocalFireDepartment,
                        value = "${daily.streakDays}",
                        tint = TechColors.Amber
                    )
                    MetricChip(
                        icon = Icons.Rounded.Bolt,
                        value = "${level.totalXp} XP",
                        tint = TechColors.Sky
                    )
                }
            }
        }

        // 2. Hero Daily Learning Goal + 1-Tap Start Learning CTA (НФТ-4.3)
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ежедневная цель",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${daily.reviewedCardsToday} / ${daily.dailyGoal} карточек",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "К повторению по SM-2: ${daily.dueWordsCount} слов",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(66.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { daily.goalProgressFraction },
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.25f),
                                    strokeWidth = 6.dp
                                )
                                Text(
                                    text = "${(daily.goalProgressFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }

                        // Level XP progress bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Прогресс уровня ${level.level}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "${level.xpInCurrentLevel} / ${level.xpForNextLevel} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            LinearProgressIndicator(
                                progress = { level.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = TechColors.Emerald,
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }

                        Button(
                            onClick = onStartLearning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Начать повторение",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // 2b. 365-Day GitHub Contribution Graph (УИ-2.2)
        item {
            GitHubContributionGraph(yearlyActivity = daily.yearlyActivity)
        }

        // 3. Adaptive Learning Recommendation
        item {
            val rec = content.recommendation
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Рекомендация",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(text = rec.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = rec.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                when (rec.destination) {
                                    RecommendationTarget.FLASHCARDS -> onStartLearning()
                                    RecommendationTarget.DOCUMENTATION -> onOpenDocs()
                                    RecommendationTarget.SCENARIOS -> onOpenScenarios()
                                    RecommendationTarget.LISTENING -> onOpenListening()
                                    RecommendationTarget.COMPARE_LANGUAGES -> onOpenCompare()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(rec.actionLabel, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Learning Modules Grid
        item {
            Text(
                text = "Учебные модули",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAcademicDictionary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Rounded.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Academic Technical Dictionary", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "20 658+ академических терминов • 36 областей • AI/ML, Crypto, Systems",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModuleShortcutCard(
                        title = "IT-словарь SRS",
                        subtitle = "${daily.dueWordsCount} к повторению",
                        icon = Icons.Rounded.Style,
                        accent = TechColors.Indigo,
                        onClick = onOpenVocabulary,
                        modifier = Modifier.weight(1f)
                    )
                    ModuleShortcutCard(
                        title = "Языки кода",
                        subtitle = "${content.languagesCount} языков + разбор",
                        icon = Icons.Rounded.Code,
                        accent = TechColors.Sky,
                        onClick = onOpenLanguages,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModuleShortcutCard(
                        title = "Сравнение языков",
                        subtitle = "Код рядом EN/RU",
                        icon = Icons.AutoMirrored.Rounded.CompareArrows,
                        accent = TechColors.Violet,
                        onClick = onOpenCompare,
                        modifier = Modifier.weight(1f)
                    )
                    ModuleShortcutCard(
                        title = "Документация",
                        subtitle = "Прочитано: ${content.completedDocsCount}",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        accent = TechColors.Emerald,
                        onClick = onOpenDocs,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModuleShortcutCard(
                        title = "Рабочие сценарии",
                        subtitle = "Пройдено: ${content.completedScenariosCount}",
                        icon = Icons.Rounded.Forum,
                        accent = TechColors.AmberDark,
                        onClick = onOpenScenarios,
                        modifier = Modifier.weight(1f)
                    )
                    ModuleShortcutCard(
                        title = "Аудирование",
                        subtitle = "TTS + Media3 офлайн",
                        icon = Icons.Rounded.Headphones,
                        accent = TechColors.Rose,
                        onClick = onOpenListening,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModuleShortcutCard(
                        title = "Лидерборды Top-10",
                        subtitle = "Лиги • кэш офлайн",
                        icon = Icons.Rounded.EmojiEvents,
                        accent = TechColors.AmberDark,
                        onClick = onOpenLeaderboards,
                        modifier = Modifier.weight(1f)
                    )
                    ModuleShortcutCard(
                        title = "Друзья и лента",
                        subtitle = "QR-код • пинг 1/24ч",
                        icon = Icons.Rounded.Group,
                        accent = TechColors.EmeraldDark,
                        onClick = onOpenFriends,
                        modifier = Modifier.weight(1f)
                    )
                }
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenStatistics),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Подробная статистика", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Графики активности, точность, прогресс по категориям",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 5. Recent Activity from real Room study_logs
        item {
            Text(
                text = "Последняя активность",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (content.recentActivity.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Пока нет записей активности",
                    subtitle = "Начните с повторения карточек или пройдите рабочий сценарий — ваш прогресс и серия дней появятся здесь.",
                    icon = Icons.Rounded.School,
                    actionLabel = "Начать первую сессию",
                    onAction = onStartLearning
                )
            }
        } else {
            items(content.recentActivity, key = { it.id }) { log ->
                RecentActivityRow(log)
            }
        }
    }
}

@Composable
private fun MetricChip(icon: ImageVector, value: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(
                text = value,
                style = MonoTagTextStyle,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ModuleShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private val activityTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")

@Composable
private fun RecentActivityRow(log: StudyLog) {
    val formattedTime = Instant.ofEpochMilli(log.timestamp)
        .atZone(ZoneId.systemDefault())
        .format(activityTimeFormatter)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = if (log.isCorrect) TechColors.Emerald else TechColors.Amber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(log.actionType.labelRu, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (log.isCorrect) "Успешно • $formattedTime" else "Повтор • $formattedTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "+${log.xpEarned} XP",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
