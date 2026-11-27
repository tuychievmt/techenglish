package com.techenglish.app.feature.programming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SegmentedPillToggle
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.navigationBarContentPadding

@Composable
fun LanguageComparisonScreen(
    onBack: () -> Unit,
    onOpenSnippetBreakdown: (Int) -> Unit,
    viewModel: LanguageComparisonViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    var codeViewMode by remember { mutableIntStateOf(0) } // 0=A, 1=B, 2=Split

    AuroraScreenContainer {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            if (state.isLoading) {
                Column(modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding().padding(16.dp)) {
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
                    SkeletonListPlaceholder()
                }
                return@Scaffold
            }
            val comp = state.comparison
            if (comp == null) {
                Column(modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding().padding(16.dp)) {
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
                    EmptyStateView(
                        title = "Недостаточно данных",
                        subtitle = "Выберите два языка для сравнения."
                    )
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding(),
                contentPadding = navigationBarContentPadding(horizontal = 0.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Custom Top App Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(40.dp).clickable(onClick = onBack)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Назад",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Сравнение языков",
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "${state.allLanguages.size} ЯЗЫКОВ",
                                style = MonoTagTextStyle,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 2. Dual Language Selector with Center Glowing Swap Button
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LanguageSelectorPill(
                                selected = comp.languageA,
                                all = state.allLanguages,
                                accentDot = if (isDark) TechColors.Violet else MaterialTheme.colorScheme.secondary,
                                alignEndPadding = true,
                                onSelect = viewModel::selectLanguageA,
                                modifier = Modifier.weight(1f)
                            )
                            LanguageSelectorPill(
                                selected = comp.languageB,
                                all = state.allLanguages,
                                accentDot = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                                alignEndPadding = false,
                                onSelect = viewModel::selectLanguageB,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, if (isDark) TechColors.Cyan.copy(alpha = 0.50f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(onClick = viewModel::swapLanguages)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (isDark) {
                                            Modifier.background(
                                                Brush.linearGradient(
                                                    listOf(TechColors.Indigo.copy(alpha = 0.35f), TechColors.Cyan.copy(alpha = 0.25f))
                                                )
                                            )
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.SwapHoriz,
                                    contentDescription = "Поменять местами",
                                    tint = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Explanations RU | Explanations EN Toggle + Compact Specs Matrix
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SegmentedPillToggle(
                            options = listOf("Объяснения RU", "Explanations EN"),
                            selectedIndex = if (state.explanationInRussian) 0 else 1,
                            onSelect = { viewModel.setExplanationLanguage(it == 0) }
                        )

                        BentoGlassCard {
                            val colorA = if (isDark) TechColors.Violet else MaterialTheme.colorScheme.secondary
                            val colorB = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary
                            SpecsMatrixRow("Типизация", comp.languageA.typingSystem, comp.languageB.typingSystem, colorA, colorB)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SpecsMatrixRow("Память", comp.languageA.memoryManagement, comp.languageB.memoryManagement, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurface)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SpecsMatrixRow("Парадигмы", comp.languageA.paradigms.joinToString(", "), comp.languageB.paradigms.joinToString(", "), MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // 4. Common Task Chips (Edge-to-edge)
                item {
                    if (comp.commonTasks.isEmpty()) {
                        EmptyStateView(
                            title = "Нет общих задач для этой пары",
                            subtitle = "Выберите другую комбинацию языков.",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(comp.commonTasks, key = { it.taskKey }) { task ->
                                val selected = comp.selectedTaskKey == task.taskKey
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) (if (isDark) TechColors.Cyan.copy(alpha = 0.55f) else MaterialTheme.colorScheme.primary)
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.clickable { viewModel.selectTask(task.taskKey) }
                                ) {
                                    Text(
                                        text = if (state.explanationInRussian) task.titleRu else task.titleEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. IDE Terminal Workspace (Код A | Код B | Split)
                if (comp.snippetA != null && comp.snippetB != null) {
                    item {
                        SegmentedPillToggle(
                            options = listOf(
                                "Код A (${comp.languageA.name})",
                                "Код B (${comp.languageB.name})",
                                "Split"
                            ),
                            selectedIndex = codeViewMode,
                            onSelect = { codeViewMode = it },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (codeViewMode == 2) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CodeWorkspacePanel(
                                    language = comp.languageA,
                                    snippet = comp.snippetA!!,
                                    inRussian = state.explanationInRussian,
                                    onOpenBreakdown = { onOpenSnippetBreakdown(comp.snippetA!!.id) },
                                    isSplitMode = true,
                                    modifier = Modifier.weight(1f)
                                )
                                CodeWorkspacePanel(
                                    language = comp.languageB,
                                    snippet = comp.snippetB!!,
                                    inRussian = state.explanationInRussian,
                                    onOpenBreakdown = { onOpenSnippetBreakdown(comp.snippetB!!.id) },
                                    isSplitMode = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        val activeLang = if (codeViewMode == 0) comp.languageA else comp.languageB
                        val activeSnippet = if (codeViewMode == 0) comp.snippetA!! else comp.snippetB!!
                        item {
                            CodeWorkspacePanel(
                                language = activeLang,
                                snippet = activeSnippet,
                                inRussian = state.explanationInRussian,
                                onOpenBreakdown = { onOpenSnippetBreakdown(activeSnippet.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
