package com.techenglish.app.feature.programming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.parseHexColor
import com.techenglish.domain.model.CodeSnippet
import com.techenglish.domain.model.ProgrammingLanguage

// ==================== 4. DYNAMIC SIDE-BY-SIDE COMPARISON SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageComparisonScreen(
    onBack: () -> Unit,
    onOpenSnippetBreakdown: (Int) -> Unit,
    viewModel: LanguageComparisonViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = LocalTechInteractionConfig.current.isDarkTheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сравнение языков") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            SkeletonListPlaceholder(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val comp = state.comparison
        if (comp == null) {
            EmptyStateView(title = "Недостаточно данных", subtitle = "Выберите два языка для сравнения.")
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Language A vs Language B Pickers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LanguagePickerDropdown(
                        label = "Язык A",
                        selected = comp.languageA,
                        all = state.allLanguages,
                        onSelect = viewModel::selectLanguageA,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::swapLanguages) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = "Поменять местами")
                    }
                    LanguagePickerDropdown(
                        label = "Язык B",
                        selected = comp.languageB,
                        all = state.allLanguages,
                        onSelect = viewModel::selectLanguageB,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // EN / RU Toggle
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.explanationInRussian,
                        onClick = { viewModel.setExplanationLanguage(true) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Объяснения RU") }
                    SegmentedButton(
                        selected = !state.explanationInRussian,
                        onClick = { viewModel.setExplanationLanguage(false) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Explanations EN") }
                }
            }

            // Side-by-side characteristics comparison card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Сравнение характеристик", style = MaterialTheme.typography.titleMedium)
                        ComparisonTableRow("Типизация", comp.languageA.typingSystem, comp.languageB.typingSystem)
                        HorizontalDivider()
                        ComparisonTableRow("Память", comp.languageA.memoryManagement, comp.languageB.memoryManagement)
                        HorizontalDivider()
                        ComparisonTableRow("Сложность", comp.languageA.difficulty, comp.languageB.difficulty)
                        HorizontalDivider()
                        ComparisonTableRow("Парадигмы", comp.languageA.paradigms.joinToString(", "), comp.languageB.paradigms.joinToString(", "))
                        HorizontalDivider()
                        ComparisonTableRow("Области", comp.languageA.domains.joinToString(", "), comp.languageB.domains.joinToString(", "))
                    }
                }
            }

            // Common task_key selector (strictly tasks available in BOTH languages)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Общие задачи (${comp.commonTasks.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (comp.commonTasks.isEmpty()) {
                        EmptyStateView(
                            title = "Нет общих задач для этой пары",
                            subtitle = "Выберите другую комбинацию языков."
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(comp.commonTasks, key = { it.taskKey }) { task ->
                                FilterChip(
                                    selected = comp.selectedTaskKey == task.taskKey,
                                    onClick = { viewModel.selectTask(task.taskKey) },
                                    label = {
                                        Text(if (state.explanationInRussian) task.titleRu else task.titleEn)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Side-by-Side Code Panels
            if (comp.snippetA != null && comp.snippetB != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SideBySideCodeColumn(
                            language = comp.languageA,
                            snippet = comp.snippetA!!,
                            inRussian = state.explanationInRussian,
                            isDark = isDark,
                            onOpenBreakdown = { onOpenSnippetBreakdown(comp.snippetA!!.id) },
                            modifier = Modifier.weight(1f)
                        )
                        SideBySideCodeColumn(
                            language = comp.languageB,
                            snippet = comp.snippetB!!,
                            inRussian = state.explanationInRussian,
                            isDark = isDark,
                            onOpenBreakdown = { onOpenSnippetBreakdown(comp.snippetB!!.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerDropdown(
    label: String,
    selected: ProgrammingLanguage,
    all: List<ProgrammingLanguage>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            all.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.name) },
                    onClick = {
                        onSelect(lang.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ComparisonTableRow(label: String, valueA: String, valueB: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(valueA, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(valueB, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SideBySideCodeColumn(
    language: ProgrammingLanguage,
    snippet: CodeSnippet,
    inRussian: Boolean,
    isDark: Boolean,
    onOpenBreakdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = remember(language.accentColorHex) { parseHexColor(language.accentColorHex) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(
                text = language.name,
                containerColor = accent.copy(alpha = 0.16f),
                contentColor = accent
            )
            Text(
                text = "Разбор →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onOpenBreakdown)
            )
        }
        InteractiveCodeBlock(
            code = snippet.code,
            languageSlug = language.slug,
            keywords = snippet.keywords,
            selectedLine = null,
            isDark = isDark,
            onLineClick = { onOpenBreakdown() },
            onKeywordClick = { onOpenBreakdown() }
        )
        Text(
            text = if (inRussian) snippet.explanationRu else snippet.explanationEn,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
