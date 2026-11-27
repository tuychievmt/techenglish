package com.techenglish.app.feature.academic

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.TerminalSearchField
import com.techenglish.app.core.designsystem.formatCountSpaced
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.app.core.designsystem.rememberTechHaptics
import com.techenglish.domain.model.AcademicStudyFilter

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

    AuroraScreenContainer {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(color = Color.Transparent) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Academic Technical Dictionary",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val total = dashboard?.totalVerifiedTerms ?: state.totalMatchingCount
                                Text(
                                    text = "${formatCountSpaced(total)} терминов · Найдено: ${formatCountSpaced(state.totalMatchingCount)}",
                                    style = MonoTagTextStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            AssistChip(
                                onClick = viewModel::toggleTranslationLanguage,
                                label = {
                                    Text(
                                        text = state.translationLanguage.shortLabel,
                                        style = MonoTagTextStyle,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = navigationBarContentPadding(horizontal = 0.dp, top = 16.dp, extraBottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Dashboard Summary Header (Total, Studied, Difficult, Favorites, Due, SRS CTA)
                dashboard?.let { dash ->
                    item {
                        BentoGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "База: ${formatCountSpaced(dash.totalVerifiedTerms)} терминов",
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "10 категорий · 30 областей · SM-2",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = onOpenAcademicSrs,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "SRS (${dash.dueSrsCount})",
                                            style = MonoTagTextStyle,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = state.filter.onlyFavorites,
                                        onClick = viewModel::toggleOnlyFavorites,
                                        label = {
                                            Text("Избранное (${dash.favoritesCount})", maxLines = 1, softWrap = false)
                                        }
                                    )
                                    FilterChip(
                                        selected = state.filter.onlyDue,
                                        onClick = viewModel::toggleOnlyDue,
                                        label = {
                                            Text("К повторению (${dash.dueSrsCount})", maxLines = 1, softWrap = false)
                                        }
                                    )
                                    FilterChip(
                                        selected = state.filter.onlyDifficult,
                                        onClick = viewModel::toggleOnlyDifficult,
                                        label = {
                                            Text("Сложные", maxLines = 1, softWrap = false)
                                        }
                                    )
                                }

                                if (dash.recentlyStudied.isNotEmpty()) {
                                    Text(
                                        text = "Недавно изученные:",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(dash.recentlyStudied, key = { it.id }) { recent ->
                                            AssistChip(
                                                onClick = { onOpenTermDetail(recent.id) },
                                                label = {
                                                    Text(recent.term, style = MonoTagTextStyle, maxLines = 1, softWrap = false)
                                                }
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
                        placeholderText = "Поиск термина, перевода или аббревиатуры...",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 3. Major Categories Horizontal Selector (Edge-to-edge)
                dashboard?.let { dash ->
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(dash.majorCategories, key = { it.categoryId }) { cat ->
                                FilterChip(
                                    selected = state.filter.majorCategory == cat.majorCategory,
                                    onClick = { viewModel.selectMajorCategory(cat.majorCategory) },
                                    label = {
                                        Text(
                                            text = "${cat.majorCategory} (${cat.termCount})",
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Collapsible Advanced Filters (30 Domains, Difficulty, CEFR, POS, Studied/Not Studied)
                if (state.showAdvancedFilters) {
                    item {
                        AcademicAdvancedFiltersCard(
                            filter = state.filter,
                            difficulties = viewModel.difficulties,
                            cefrLevels = viewModel.cefrLevels,
                            partsOfSpeech = viewModel.partsOfSpeech,
                            domains = dashboard?.domains.orEmpty(),
                            onSelectDifficulty = viewModel::selectDifficulty,
                            onSelectCefr = viewModel::selectCefr,
                            onSelectPartOfSpeech = viewModel::selectPartOfSpeech,
                            onSelectStudyFilter = viewModel::selectStudyFilter,
                            onSelectDomain = viewModel::selectDomain,
                            onClearAllFilters = viewModel::clearAllFilters,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // 5. Paginated Academic Term List
                if (state.isLoading) {
                    item {
                        SkeletonListPlaceholder(
                            itemCount = 4,
                            statusMessage = "Compiling vocabulary...",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else if (state.items.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Термины не найдены",
                            subtitle = "Попробуйте изменить поисковый запрос или сбросить активные фильтры.",
                            actionLabel = "Сбросить фильтры",
                            onAction = viewModel::clearAllFilters,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
                            translationLanguage = state.translationLanguage,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                FilledTonalButton(onClick = viewModel::loadNextPage) {
                                    Text(
                                        text = "Загрузить ещё (${formatCountSpaced(state.items.size)} из ${formatCountSpaced(state.totalMatchingCount)})",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

