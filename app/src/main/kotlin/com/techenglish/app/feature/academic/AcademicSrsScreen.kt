package com.techenglish.app.feature.academic

import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.ComboStreakBanner
import com.techenglish.app.core.designsystem.DifficultyBadge
import com.techenglish.app.core.designsystem.HighlightedExampleQuoteCard
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermDisplayTextStyle
import com.techenglish.app.core.designsystem.TranslationContainerCard
import com.techenglish.app.core.designsystem.rememberTechHaptics
import com.techenglish.domain.model.AcademicSrsMode
import com.techenglish.domain.model.ReviewQuality
import com.techenglish.domain.model.TranslationLanguage

// ==================== 3. ADVANCED SRS & DEFINITION QUIZ SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicSrsScreen(
    onBack: () -> Unit,
    viewModel: AcademicSrsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberTechHaptics()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Academic SRS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 6 Mandatory Academic SRS Modes Selector
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AcademicSrsMode.entries) { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        label = { Text(mode.labelRu) }
                    )
                }
            }

            // 2. Format Switcher: SM-2 Flashcards vs Definition Comprehension Quiz
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AcademicPracticeFormat.entries.forEachIndexed { idx, fmt ->
                    SegmentedButton(
                        selected = state.format == fmt,
                        onClick = { viewModel.selectFormat(fmt) },
                        shape = SegmentedButtonDefaults.itemShape(idx, AcademicPracticeFormat.entries.size)
                    ) {
                        Text(fmt.labelRu)
                    }
                }
            }

            // 3. Combo Streak Banner (УИ-3.2)
            ComboStreakBanner(comboState = state.comboState)

            when {
                state.isLoading -> SkeletonListPlaceholder(itemCount = 2)
                state.isSessionFinished -> {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.School,
                                contentDescription = null,
                                tint = TechColors.Emerald,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (state.reviewedCount > 0) "Сессия завершена!" else "В этой очереди пока нет карточек",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            if (state.reviewedCount > 0) {
                                Text(
                                    text = "Повторено: ${state.reviewedCount} • Верно: ${state.correctCount} • +${state.xpEarned} XP",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "Выберите другой тематический режим выше или добавьте термины из Академического словаря кнопкой Add to SRS.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { viewModel.loadDeck() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Загрузить новый набор")
                            }
                        }
                    }
                }
                else -> {
                    val term = state.currentTerm ?: return@Column
                    val progress = ((state.currentIndex + 1).toFloat() / state.deck.size.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Text(
                        text = "${state.mode.labelRu} • Карточка ${state.currentIndex + 1} из ${state.deck.size}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )

                    if (state.format == AcademicPracticeFormat.SM2_CARDS) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.onCardFlip()
                                    viewModel.flipCard()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusBadge(text = term.domain)
                                        DifficultyBadge(difficulty = term.difficultyLevel)
                                    }
                                    IconButton(onClick = { viewModel.speak(term.term) }) {
                                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Озвучить")
                                    }
                                }

                                Text(term.term, style = TermDisplayTextStyle)
                                if (term.phoneticTranscription.isNotBlank()) {
                                    Text(
                                        term.phoneticTranscription,
                                        style = IpaTextStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (state.isFlipped) {
                                    HorizontalDivider()
                                    TranslationContainerCard(
                                        translationText = term.displayTranslation(state.translationLanguage),
                                        secondaryTranslationText = if (state.translationLanguage == TranslationLanguage.TJ && term.hasTajikTranslation) {
                                            "RU: ${term.translationRu}"
                                        } else {
                                            term.translationTg.takeIf { it.isNotBlank() }?.let { "TJ: $it" }
                                        }
                                    )
                                    Text(term.definitionEn, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        term.definitionRu,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    HighlightedExampleQuoteCard(
                                        exampleEn = term.exampleEn,
                                        targetTerm = term.term,
                                        exampleRu = term.exampleRu
                                    )
                                } else {
                                    Text(
                                        "Нажмите на карточку, чтобы увидеть перевод, академическое определение и пример",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (!state.isFlipped) {
                            Button(
                                onClick = {
                                    haptics.onCardFlip()
                                    viewModel.flipCard()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Flip, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Показать определение и перевод")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReviewQuality.entries.forEach { q ->
                                    val days = state.intervalPreviews[q] ?: 0
                                    FilledTonalButton(
                                        onClick = {
                                            if (q.isCorrect) haptics.onCorrectAnswer() else haptics.onSwipe()
                                            viewModel.rateCard(q)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(q.labelRu, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                            Text(
                                                if (days == 0) "Сейчас" else "$days дн.",
                                                style = MonoTagTextStyle
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // DEFINITION COMPREHENSION QUIZ
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusBadge(text = "${term.domain} • ${term.subdomain}")
                                Text("Выберите термин, соответствующий научному определению:", style = MaterialTheme.typography.labelLarge)
                                Text(term.definitionEn, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    term.definitionRu,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val correctOption = "${term.term} — ${term.displayTranslation(state.translationLanguage)}"
                        state.quizOptions.forEach { option ->
                            val selected = state.selectedQuizOption == option
                            val answered = state.selectedQuizOption != null
                            val borderColor = when {
                                answered && option == correctOption -> TechColors.Emerald
                                answered && selected && option != correctOption -> TechColors.Rose
                                selected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !answered) {
                                        if (option == correctOption) haptics.onCorrectAnswer() else haptics.onSwipe()
                                        viewModel.answerQuiz(option)
                                    },
                                border = BorderStroke(if (answered || selected) 2.dp else 1.dp, borderColor)
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }

                        if (state.selectedQuizOption != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (state.selectedQuizOption == correctOption) {
                                        TechColors.Emerald.copy(alpha = 0.14f)
                                    } else {
                                        TechColors.Rose.copy(alpha = 0.14f)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (state.selectedQuizOption == correctOption) "Верно! Интервал SM-2 обновлён."
                                        else "Правильный ответ: $correctOption",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Button(
                                        onClick = viewModel::nextAfterQuiz,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Следующий термин")
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
