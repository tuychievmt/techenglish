package com.techenglish.app.feature.programming

import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.ensureAccessibleAccent
import com.techenglish.app.core.designsystem.parseHexColor

// ==================== 2. LANGUAGE DETAIL SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LanguageDetailScreen(
    onBack: () -> Unit,
    onOpenSnippet: (Int) -> Unit,
    onCompareWith: (Int) -> Unit,
    viewModel: LanguageDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = state.language

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lang?.name ?: "Язык") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (lang != null) {
                        IconButton(onClick = { onCompareWith(lang.id) }) {
                            Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = "Сравнить")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            SkeletonListPlaceholder(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        if (lang == null) {
            EmptyStateView(title = "Язык не найден", subtitle = "Вернитесь в каталог.")
            return@Scaffold
        }

        val surfaceBg = MaterialTheme.colorScheme.surface
        val accent = remember(lang.accentColorHex, surfaceBg) {
            ensureAccessibleAccent(parseHexColor(lang.accentColorHex), surfaceBg)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // EN / RU switcher
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.showRussian,
                        onClick = { viewModel.toggleLanguageMode(true) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Русский (RU)") }
                    SegmentedButton(
                        selected = !state.showRussian,
                        onClick = { viewModel.toggleLanguageMode(false) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("English (EN)") }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(
                                text = lang.difficulty,
                                containerColor = accent.copy(alpha = 0.16f),
                                contentColor = accent
                            )
                            Text(
                                text = "Создан в ${lang.yearCreated} • ${lang.creator}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (state.showRussian) lang.descriptionRu else lang.descriptionEn,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        HorizontalDivider()
                        Text("История создания", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (state.showRussian) lang.historyRu else lang.historyEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Технические характеристики", style = MaterialTheme.typography.titleMedium)
                        SpecRow("Типизация", lang.typingSystem)
                        SpecRow("Управление памятью", lang.memoryManagement)
                        SpecRow("Парадигмы", lang.paradigms.joinToString(", "))
                        SpecRow("Области применения", lang.domains.joinToString(", "))
                        Text("Популярные фреймворки", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            lang.frameworks.forEach { fw -> StatusBadge(text = fw) }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Практические примеры (${state.snippets.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    FilledTonalButton(onClick = { onCompareWith(lang.id) }) {
                        Text("Сравнить код")
                    }
                }
            }

            items(state.snippets, key = { it.id }) { snippet ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSnippet(snippet.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (state.showRussian) snippet.taskTitleRu else snippet.taskTitleEn,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (state.showRussian) snippet.explanationRu else snippet.explanationEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Code, contentDescription = null, tint = accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

