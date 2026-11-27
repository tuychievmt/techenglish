package com.techenglish.app.feature.academic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.DifficultyBadge
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.HighlightedExampleQuoteCard
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.PartOfSpeechBadge
import com.techenglish.app.core.designsystem.PracticalExplanationCard
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermDisplayTextStyle
import com.techenglish.app.core.designsystem.TranslationContainerCard
import com.techenglish.app.core.designsystem.rememberTechHaptics
import com.techenglish.domain.model.TranslationLanguage

// ==================== 2. ACADEMIC TERM DETAIL SCREEN (PROGRESSIVE DISCLOSURE УИ-2.3) ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AcademicTermDetailScreen(
    onBack: () -> Unit,
    onOpenComments: (termId: Int, termTitle: String) -> Unit = { _, _ -> },
    viewModel: AcademicTermDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberTechHaptics()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.detail?.term?.domain ?: "Термин",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    state.detail?.term?.let { t ->
                        IconButton(onClick = { onOpenComments(t.id, t.term) }) {
                            Icon(
                                imageVector = Icons.Rounded.Forum,
                                contentDescription = "Комментарии и мнемоники"
                            )
                        }
                    }
                    val currentLocale = playbackState.speechLocaleTag
                    TextButton(
                        onClick = {
                            val next = if (currentLocale == "en-US") "en-GB" else "en-US"
                            viewModel.switchAccent(next)
                        }
                    ) {
                        Text(currentLocale, style = MonoTagTextStyle, maxLines = 1, softWrap = false)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            SkeletonListPlaceholder(
                modifier = Modifier.padding(padding),
                statusMessage = "Resolving dependencies..."
            )
            return@Scaffold
        }
        val detail = state.detail
        if (detail == null) {
            EmptyStateView(title = "Термин не найден", subtitle = "Вернитесь к списку.")
            return@Scaffold
        }
        val term = detail.term

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(navigationBarContentPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Primary Hero Screen (Progressive Disclosure First Layer: Term -> Translation -> Short Example -> Audio)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DifficultyBadge(difficulty = term.difficultyLevel)
                        PartOfSpeechBadge(partOfSpeech = term.partOfSpeech)
                        StatusBadge(text = "CEFR ${term.cefrLevel}")
                        if (term.abbreviations.isNotBlank()) {
                            StatusBadge(
                                text = term.abbreviations,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Large bold term with generous bottom margin & compact muted IPA (УИ-1.3)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = term.term,
                            style = TermDisplayTextStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (term.phoneticTranscription.isNotBlank()) {
                            Text(
                                text = term.phoneticTranscription,
                                style = IpaTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Separate Visual Container 1: Translation (УИ-1.4, Errata E6)
                    TranslationContainerCard(
                        translationText = term.displayTranslation(state.translationLanguage),
                        secondaryTranslationText = if (state.translationLanguage == TranslationLanguage.TJ && term.hasTajikTranslation) {
                            "RU: ${term.translationRu}"
                        } else {
                            term.translationTg.takeIf { it.isNotBlank() }?.let { "TJ: $it" }
                        }
                    )

                    // Separate Visual Container 2: Highlighted Usage Example with Left Accent Bar (УИ-1.2, УИ-1.4)
                    HighlightedExampleQuoteCard(
                        exampleEn = term.exampleEn,
                        targetTerm = term.term,
                        exampleRu = term.exampleRu
                    )

                    // Action Buttons Row: Listen (Term & Example), Favorites, Add to SRS, Mark as Learned
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(onClick = { viewModel.speak(term.term) }) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Озвучить термин", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Listen")
                        }
                        FilledTonalButton(onClick = { viewModel.speak(term.exampleEn) }) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Озвучить пример", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Пример")
                        }
                        FilledTonalButton(onClick = viewModel::onToggleFavorite) {
                            Icon(
                                if (term.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = null,
                                tint = if (term.isFavorite) TechColors.Amber else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (term.isFavorite) "В избранном" else "В избранное")
                        }
                        FilledTonalButton(onClick = viewModel::onToggleInSrs) {
                            Icon(
                                if (term.isInSrs) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (term.isInSrs) "В очереди SRS" else "Add to SRS")
                        }
                        Button(
                            onClick = {
                                haptics.onCorrectAnswer()
                                viewModel.onMarkAsLearned()
                            },
                            enabled = !term.isMastered
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (term.isMastered) "Изучено" else "Mark as Learned")
                        }
                    }
                }
            }

            // 2. Progressive Disclosure Tabs (УИ-2.3: Deep Explanations, Senses, Relations, Sources)
            val tabs = AcademicDetailTab.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { idx, tab ->
                    val badgeCount = when (tab) {
                        AcademicDetailTab.SENSES -> detail.senses.size
                        AcademicDetailTab.RELATIONS -> term.relatedTerms.size + term.synonyms.size
                        else -> 0
                    }
                    val label = if (badgeCount > 0) "${tab.labelRu} ($badgeCount)" else tab.labelRu
                    SegmentedButton(
                        selected = selectedTabIndex == idx,
                        onClick = {
                            haptics.onWordTap()
                            selectedTabIndex = idx
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, tabs.size)
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // 3. Selected Tab Content
            when (tabs[selectedTabIndex]) {
                AcademicDetailTab.EXPLANATIONS -> {
                    // Technical Definitions (EN/RU)
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Technical Definition (EN)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { viewModel.speak(term.definitionEn) }) {
                                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Озвучить определение")
                                }
                            }
                            Text(term.definitionEn, style = MaterialTheme.typography.bodyLarge)
                            HorizontalDivider()
                            Text(
                                "Техническое определение (RU)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(term.definitionRu, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Academic Explanation (EN/RU)
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Академическое объяснение (Academic Explanation)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(term.academicExplanationEn, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                term.academicExplanationRu,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Separate Visual Container 3: Practical Engineering Explanation (УИ-1.4)
                    PracticalExplanationCard(
                        explanationText = term.practicalExplanation
                    )
                }

                AcademicDetailTab.SENSES -> {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Значения термина по предметным областям (${detail.senses.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (detail.senses.isEmpty()) {
                                Text(
                                    text = "Термин однозначен в рамках домена «${term.domain}» (${term.subdomain}). Основное определение представлено на вкладке «Объяснения».",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                detail.senses.forEach { sense ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                StatusBadge(text = "#${sense.senseNumber} • ${sense.domain}")
                                                Text(sense.senseLabel, style = MaterialTheme.typography.titleSmall)
                                            }
                                            Text(sense.definitionEn, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                sense.definitionRu,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            HighlightedExampleQuoteCard(
                                                exampleEn = sense.exampleEn,
                                                targetTerm = term.term,
                                                exampleRu = sense.exampleRu
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AcademicDetailTab.RELATIONS -> {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (term.synonyms.isNotEmpty()) {
                                Text("Синонимы (Synonyms):", style = MaterialTheme.typography.titleSmall)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    term.synonyms.forEach { syn ->
                                        StatusBadge(text = syn)
                                    }
                                }
                            }

                            if (term.relatedTerms.isNotEmpty()) {
                                Text("Связанные термины (нажмите для перехода):", style = MaterialTheme.typography.titleSmall)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    term.relatedTerms.forEach { rel ->
                                        AssistChip(
                                            onClick = {
                                                haptics.onWordTap()
                                                viewModel.onRelatedTermClicked(rel)
                                            },
                                            label = { Text(rel, style = MonoTagTextStyle) }
                                        )
                                    }
                                }
                            }

                            if (term.synonyms.isEmpty() && term.relatedTerms.isEmpty()) {
                                Text(
                                    text = "Для данного термина дополнительные синонимы не зафиксированы.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                AcademicDetailTab.SOURCES -> {
                    AcademicDetailSourcesCard(detail = detail)
                }
            }
        }
    }
}

