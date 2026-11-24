package com.techenglish.app.feature.academic

import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.DifficultyBadge
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.PartOfSpeechBadge
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermTitleTextStyle
import com.techenglish.app.core.designsystem.TerminalSearchField
import com.techenglish.app.core.designsystem.rememberTechHaptics
import com.techenglish.domain.model.AcademicStudyFilter
import com.techenglish.domain.model.AcademicTerm
import com.techenglish.domain.model.TranslationLanguage

// ==================== 1. ACADEMIC TECHNICAL DICTIONARY MAIN SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AcademicDictionaryScreen(
    onBack: () -> Unit,
    onOpenTermDetail: (Int) -> Unit,
    onOpenAcademicSrs: () -> Unit,
    viewModel: AcademicDictionaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboardState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberTechHaptics()

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Academic Technical Dictionary")
                        val total = dashboard?.totalVerifiedTerms ?: state.totalMatchingCount
                        Text(
                            "Проверенных терминов: $total+ • Найдено: ${state.totalMatchingCount}",
                            style = MonoTagTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    AssistChip(
                        onClick = viewModel::toggleTranslationLanguage,
                        label = { Text(state.translationLanguage.shortLabel, style = MonoTagTextStyle) }
                    )
                    IconButton(onClick = viewModel::toggleAdvancedFilters) {
                        Icon(
                            Icons.Rounded.FilterList,
                            contentDescription = "Фильтры",
                            tint = if (state.showAdvancedFilters) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Dashboard Summary Header (Total 10,000+, Studied, Difficult, Favorites, Due, SRS CTA)
            dashboard?.let { dash ->
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Академическая база: ${dash.totalVerifiedTerms} терминов",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "10 категорий • 30 предметных областей • SM-2 SRS",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(onClick = onOpenAcademicSrs) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("SRS (${dash.dueSrsCount})", style = MonoTagTextStyle)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = state.filter.onlyFavorites,
                                    onClick = viewModel::toggleOnlyFavorites,
                                    label = { Text("Избранное (${dash.favoritesCount})") }
                                )
                                FilterChip(
                                    selected = state.filter.onlyDue,
                                    onClick = viewModel::toggleOnlyDue,
                                    label = { Text("К повторению (${dash.dueSrsCount})") }
                                )
                                FilterChip(
                                    selected = state.filter.onlyDifficult,
                                    onClick = viewModel::toggleOnlyDifficult,
                                    label = { Text("Сложные") }
                                )
                            }

                            if (dash.recentlyStudied.isNotEmpty()) {
                                Text("Недавно изученные:", style = MaterialTheme.typography.labelMedium)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(dash.recentlyStudied, key = { it.id }) { recent ->
                                        AssistChip(
                                            onClick = { onOpenTermDetail(recent.id) },
                                            label = { Text(recent.term, style = MonoTagTextStyle, maxLines = 1) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Multi-field Offline Search with Blinking Terminal Cursor '_' (УИ-3.3)
            item {
                TerminalSearchField(
                    value = state.filter.query,
                    onValueChange = viewModel::updateQuery,
                    placeholderText = "Поиск: EN термин, RU перевод, определение, аббревиатура..."
                )
            }

            // 3. Major Categories Horizontal Selector (10 Major Categories)
            dashboard?.let { dash ->
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dash.majorCategories, key = { it.categoryId }) { cat ->
                            FilterChip(
                                selected = state.filter.majorCategory == cat.majorCategory,
                                onClick = { viewModel.selectMajorCategory(cat.majorCategory) },
                                label = { Text("${cat.majorCategory} (${cat.termCount})") }
                            )
                        }
                    }
                }
            }

            // 4. Collapsible Advanced Filters (30 Domains, Difficulty, CEFR, POS, Studied/Not Studied)
            if (state.showAdvancedFilters) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Расширенные фильтры", style = MaterialTheme.typography.titleSmall)
                                TextButton(onClick = viewModel::clearAllFilters) {
                                    Text("Сбросить все")
                                }
                            }

                            Text("Уровень сложности:", style = MaterialTheme.typography.labelMedium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(viewModel.difficulties) { diff ->
                                    FilterChip(
                                        selected = state.filter.difficulty == diff,
                                        onClick = { viewModel.selectDifficulty(diff) },
                                        label = { Text(diff) }
                                    )
                                }
                            }

                            Text("Уровень CEFR и Часть речи:", style = MaterialTheme.typography.labelMedium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(viewModel.cefrLevels) { cefr ->
                                    FilterChip(
                                        selected = state.filter.cefr == cefr,
                                        onClick = { viewModel.selectCefr(cefr) },
                                        label = { Text(cefr, style = MonoTagTextStyle) }
                                    )
                                }
                                items(viewModel.partsOfSpeech) { pos ->
                                    FilterChip(
                                        selected = state.filter.partOfSpeech == pos,
                                        onClick = { viewModel.selectPartOfSpeech(pos) },
                                        label = { Text(pos, style = MonoTagTextStyle) }
                                    )
                                }
                            }

                            Text("Статус изучения:", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AcademicStudyFilter.entries.forEach { sf ->
                                    FilterChip(
                                        selected = state.filter.studyFilter == sf,
                                        onClick = { viewModel.selectStudyFilter(sf) },
                                        label = { Text(sf.labelRu) }
                                    )
                                }
                            }

                            dashboard?.let { dash ->
                                Text("30 предметных областей:", style = MaterialTheme.typography.labelMedium)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(dash.domains, key = { it.domain }) { dom ->
                                        FilterChip(
                                            selected = state.filter.domain == dom.domain,
                                            onClick = { viewModel.selectDomain(dom.domain) },
                                            label = { Text("${dom.domain} (${dom.termCount})") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Paginated Academic Term List (Never loads all 10,000+ rows at once)
            if (state.isLoading) {
                item {
                    SkeletonListPlaceholder(
                        itemCount = 4,
                        statusMessage = "Compiling vocabulary..."
                    )
                }
            } else if (state.items.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Термины не найдены",
                        subtitle = "Попробуйте изменить поисковый запрос или сбросить активные фильтры.",
                        actionLabel = "Сбросить фильтры",
                        onAction = viewModel::clearAllFilters
                    )
                }
            } else {
                items(state.items, key = { it.id }) { term ->
                    AcademicTermRowCard(
                        term = term,
                        onClick = {
                            haptics.onWordTap()
                            onOpenTermDetail(term.id)
                        },
                        onSpeak = { viewModel.speak(term.term) },
                        onToggleFavorite = { viewModel.onToggleFavorite(term) },
                        onToggleSrs = { viewModel.onToggleSrsQueue(term) },
                        translationLanguage = state.translationLanguage
                    )
                }

                if (state.hasMorePages) {
                    item {
                        LaunchedEffect(state.items.size) {
                            viewModel.loadNextPage()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FilledTonalButton(onClick = viewModel::loadNextPage) {
                                Text("Загрузить ещё (${state.items.size} из ${state.totalMatchingCount})")
                            }
                        }
                    }
                }
            }
        }
    }
}

