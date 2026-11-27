package com.techenglish.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.CircularProgressRingCanvas
import com.techenglish.app.core.designsystem.ErrorStateView
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.formatCountSpaced
import com.techenglish.app.core.designsystem.resolveHeroPalette

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

    AuroraScreenContainer {
        when (val current = state) {
            HomeUiState.Loading -> SkeletonListPlaceholder(itemCount = 5, modifier = Modifier.statusBarsPadding())
            is HomeUiState.Error -> ErrorStateView(message = current.message, modifier = Modifier.statusBarsPadding())
            is HomeUiState.Content -> HomeContent(
                content = current,
                onSpeakWord = { term -> viewModel.audioController.speakQuick(term) },
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
}

@Composable
private fun HomeContent(
    content: HomeUiState.Content,
    onSpeakWord: (String) -> Unit,
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
    val config = LocalTechInteractionConfig.current
    val isDark = config.isDarkTheme
    val daily = content.dailyProgress
    val level = daily.levelProgress
    val heroPalette = remember(config.themeMode, isDark) {
        resolveHeroPalette(config.themeMode, isDark)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Atmospheric Header: TechEnglish + ● OFFLINE READY + XP Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "TechEnglish",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) TechColors.Emerald else TechColors.EmeraldDark)
                            )
                            Text(
                                text = "OFFLINE READY",
                                style = MonoTagTextStyle,
                                color = if (isDark) TechColors.Emerald else TechColors.EmeraldDark
                            )
                        }
                    }
                }

                val xpBg = if (isDark) TechColors.AmberContainerDark else TechColors.AmberContainerLight
                val xpFg = if (isDark) TechColors.OnAmberContainerDark else TechColors.OnAmberContainerLight
                Surface(
                    shape = RoundedCornerShape(50),
                    color = xpBg,
                    border = BorderStroke(1.dp, TechColors.Amber.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = if (isDark) TechColors.Amber else TechColors.AmberDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${level.totalXp} XP",
                            style = MonoTagTextStyle,
                            color = xpFg
                        )
                    }
                }
            }
        }

        // 2. Flagship Hero Card «Дневная цель»
        item {
            val remaining = (daily.dailyGoal - daily.reviewedCardsToday).coerceAtLeast(0)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.16f) else Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    heroPalette.gradientStart,
                                    heroPalette.gradientMid,
                                    heroPalette.gradientEnd
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
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = heroPalette.badgeBg
                                ) {
                                    Text(
                                        text = "SM-2 · $remaining ОСТАЛОСЬ",
                                        style = MonoTagTextStyle,
                                        color = heroPalette.badgeContent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Дневная цель",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = heroPalette.onHero
                                )
                                Text(
                                    text = "${daily.reviewedCardsToday} / ${daily.dailyGoal}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = heroPalette.onHero
                                )
                                Text(
                                    text = "Ур. ${level.level} · ${level.xpInCurrentLevel}/${level.xpForNextLevel} XP",
                                    style = MonoTagTextStyle,
                                    color = heroPalette.onHeroMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(level.progressFraction.coerceIn(0.04f, 1f))
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(heroPalette.ringEnd, heroPalette.ringStart)
                                                )
                                            )
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            CircularProgressRingCanvas(
                                progress = daily.goalProgressFraction,
                                centerLabel = "${(daily.goalProgressFraction * 100).toInt()}%",
                                startColor = heroPalette.ringStart,
                                endColor = heroPalette.ringEnd,
                                trackColor = Color.White.copy(alpha = 0.14f),
                                tipColor = Color.White,
                                labelColor = heroPalette.onHero,
                                size = 90.dp,
                                strokeWidth = 8.dp
                            )
                        }

                        Button(
                            onClick = onStartLearning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = heroPalette.ctaBackground,
                                contentColor = heroPalette.onCta
                            )
                        ) {
                            Text(
                                text = "Начать сессию",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. 2-Column Equal-Height Bento Row: СЕРИЯ & ОСВОЕНО
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StreakBentoTile(
                    streakDays = daily.streakDays,
                    bestStreak = content.bestStreakDays,
                    last7Days = daily.yearlyActivity.takeLast(7),
                    onClick = onOpenStatistics,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                MasteredBentoTile(
                    learnedCount = content.learnedWordsCount,
                    weeklyDelta = content.weeklyLearnedDelta,
                    accuracyPercent = content.accuracyPercent,
                    sparkValues = daily.yearlyActivity.takeLast(7).map { it.count.toFloat() },
                    onClick = onOpenStatistics,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }

        // 4. Raycast-Style «Термин дня» Spotlight Card
        item {
            WordOfTheDaySpotlightCard(
                word = content.wordOfTheDay,
                onSpeak = onSpeakWord,
                onOpenVocabulary = onOpenVocabulary
            )
        }

        // 5. «Офлайн-пакеты» 2-Column Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Офлайн-пакеты",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Все ${content.categoryPacks.size.coerceAtLeast(4)} →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onOpenVocabulary)
                    )
                }

                val topPacks = content.categoryPacks.take(2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (topPacks.isNotEmpty()) {
                        topPacks.forEachIndexed { idx, pack ->
                            OfflinePackBentoCard(
                                pack = pack,
                                accent = if (idx == 0) TechColors.Cyan else TechColors.Violet,
                                onClick = onOpenVocabulary,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        OfflinePackFallbackCard(
                            title = "Distributed Systems",
                            accent = TechColors.Cyan,
                            onClick = onOpenVocabulary,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        OfflinePackFallbackCard(
                            title = "Rust Async",
                            accent = TechColors.Violet,
                            onClick = onOpenVocabulary,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }

        // 6. Compact Developer Modules Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Инструменты и модули",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                BentoGlassCard(onClick = onOpenAcademicDictionary) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.School,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Academic Technical Dictionary", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${formatCountSpaced(content.academicTermCount)} терминов · 36 областей · AI/ML, Systems",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactModuleTile(
                        title = "Языки кода",
                        subtitle = "${content.languagesCount} языков",
                        icon = Icons.Rounded.Code,
                        accent = TechColors.Cyan,
                        onClick = onOpenLanguages,
                        modifier = Modifier.weight(1f)
                    )
                    CompactModuleTile(
                        title = "Сравнение",
                        subtitle = "Код EN / RU",
                        icon = Icons.AutoMirrored.Rounded.CompareArrows,
                        accent = TechColors.Violet,
                        onClick = onOpenCompare,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactModuleTile(
                        title = "Документация",
                        subtitle = "Прочитано: ${content.completedDocsCount}",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        accent = TechColors.EmeraldMid,
                        onClick = onOpenDocs,
                        modifier = Modifier.weight(1f)
                    )
                    CompactModuleTile(
                        title = "Аудирование",
                        subtitle = "TTS + Media3",
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
                    CompactModuleTile(
                        title = "Сценарии",
                        subtitle = "Пройдено: ${content.completedScenariosCount}",
                        icon = Icons.Rounded.Forum,
                        accent = TechColors.Amber,
                        onClick = onOpenScenarios,
                        modifier = Modifier.weight(1f)
                    )
                    CompactModuleTile(
                        title = "Лидерборды",
                        subtitle = "Топ-10 и лиги",
                        icon = Icons.Rounded.EmojiEvents,
                        accent = TechColors.Indigo,
                        onClick = onOpenLeaderboards,
                        modifier = Modifier.weight(1f)
                    )
                }
                BentoGlassCard(onClick = onOpenFriends) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Group, contentDescription = null, tint = TechColors.EmeraldMid)
                            Spacer(Modifier.width(10.dp))
                            Text("Друзья и лента активности", style = MaterialTheme.typography.titleSmall)
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
