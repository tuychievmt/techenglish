package com.techenglish.app.feature.vocabulary

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.ComboStreakBanner
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.HighlightedExampleQuoteCard
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.PracticalExplanationCard
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermDisplayTextStyle
import com.techenglish.app.core.designsystem.TranslationContainerCard
import com.techenglish.app.core.designsystem.TtsVoiceStatusBanner
import com.techenglish.app.core.designsystem.rememberTechHaptics
import com.techenglish.domain.model.ReviewQuality
import com.techenglish.domain.model.Word
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    onBack: () -> Unit,
    viewModel: FlashcardsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val haptics = rememberTechHaptics()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val countLabel = if (state.totalInSession > 0 && !state.isSessionFinished) {
                        "Карточка ${(state.currentIndex + 1).coerceAtMost(state.totalInSession)} из ${state.totalInSession}"
                    } else {
                        "Повторение SM-2"
                    }
                    Text(countLabel)
                },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TtsVoiceStatusBanner(
                voiceStatus = voiceStatus,
                installIntentProvider = { viewModel.audioController.buildInstallTtsVoiceIntent() },
                onRecheck = { viewModel.audioController.recheckVoiceStatus() }
            )

            when {
                state.isLoading -> SkeletonListPlaceholder(itemCount = 2)
                state.isSessionFinished && state.reviewedCount == 0 -> {
                    EmptyStateView(
                        title = "На сегодня все слова повторены!",
                        subtitle = "Алгоритм SM-2 запланировал следующие интервалы. Вы можете добавить новые термины или изучить документацию.",
                        icon = Icons.Rounded.CheckCircle,
                        actionLabel = "Вернуться назад",
                        onAction = onBack
                    )
                }
                state.isSessionFinished -> {
                    SessionCompletedCard(
                        reviewed = state.reviewedCount,
                        correct = state.correctCount,
                        xp = state.xpEarned,
                        onPracticeMore = { viewModel.loadSession() },
                        onDone = onBack
                    )
                }
                else -> {
                    val word = state.currentWord ?: return@Column
                    val progress = (state.currentIndex.toFloat() / state.totalInSession.coerceAtLeast(1)).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )

                    ComboStreakBanner(comboState = state.comboState)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        SwipeableFlipFlashcard(
                            word = word,
                            isFlipped = state.isFlipped,
                            onFlip = {
                                haptics.onCardFlip()
                                viewModel.flipCard()
                            },
                            onSpeakTerm = { viewModel.speak(word.term) },
                            onSpeakExample = { viewModel.speak(word.example) },
                            onToggleFavorite = viewModel::toggleCurrentFavorite,
                            onSwipeLeft = {
                                haptics.onSwipe()
                                viewModel.rateCurrentCard(ReviewQuality.FORGOT)
                            },
                            onSwipeRight = {
                                haptics.onCorrectAnswer()
                                viewModel.rateCurrentCard(ReviewQuality.GOOD)
                            }
                        )
                    }

                    if (!state.isFlipped) {
                        Button(
                            onClick = {
                                haptics.onCardFlip()
                                viewModel.flipCard()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Rounded.Flip, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Показать ответ и пояснение")
                        }
                        Text(
                            text = "Подсказка: нажмите на карточку для переворота или свайпните влево / вправо",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Насколько уверенно вы вспомнили термин? (SM-2)",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RatingButton(
                                quality = ReviewQuality.FORGOT,
                                intervalDays = state.intervalPreviews[ReviewQuality.FORGOT] ?: 0,
                                accent = TechColors.Rose,
                                onClick = {
                                    haptics.onSwipe()
                                    viewModel.rateCurrentCard(ReviewQuality.FORGOT)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            RatingButton(
                                quality = ReviewQuality.HARD,
                                intervalDays = state.intervalPreviews[ReviewQuality.HARD] ?: 1,
                                accent = TechColors.AmberDark,
                                onClick = {
                                    haptics.onCorrectAnswer()
                                    viewModel.rateCurrentCard(ReviewQuality.HARD)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            RatingButton(
                                quality = ReviewQuality.GOOD,
                                intervalDays = state.intervalPreviews[ReviewQuality.GOOD] ?: 1,
                                accent = TechColors.Sky,
                                onClick = {
                                    haptics.onCorrectAnswer()
                                    viewModel.rateCurrentCard(ReviewQuality.GOOD)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            RatingButton(
                                quality = ReviewQuality.EASY,
                                intervalDays = state.intervalPreviews[ReviewQuality.EASY] ?: 1,
                                accent = TechColors.Emerald,
                                onClick = {
                                    haptics.onCorrectAnswer()
                                    viewModel.rateCurrentCard(ReviewQuality.EASY)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableFlipFlashcard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSpeakTerm: () -> Unit,
    onSpeakExample: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val reduceMotion = LocalTechInteractionConfig.current.reduceMotionEnabled
    var dragOffsetX by remember(word.id) { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipe_offset"
    )
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 360),
        label = "card_flip"
    )

    val swipeHintColor = when {
        dragOffsetX > 90f -> TechColors.Emerald
        dragOffsetX < -90f -> TechColors.Rose
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .graphicsLayer {
                rotationY = rotation
                rotationZ = (animatedOffsetX / 35f).coerceIn(-12f, 12f)
                cameraDistance = 14f * density
            }
            .pointerInput(word.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragOffsetX > 180f -> {
                                dragOffsetX = 0f
                                onSwipeRight()
                            }
                            dragOffsetX < -180f -> {
                                dragOffsetX = 0f
                                onSwipeLeft()
                            }
                            else -> dragOffsetX = 0f
                        }
                    },
                    onDragCancel = { dragOffsetX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffsetX += dragAmount
                    }
                )
            }
            .clickable(onClick = onFlip),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = if (abs(dragOffsetX) > 90f) 2.5.dp else 1.dp,
            color = swipeHintColor
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        if (rotation <= 90f) {
            // FRONT OF CARD
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusBadge(text = word.categoryName)
                        word.languageName?.let { StatusBadge(text = it) }
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (word.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "Избранное",
                            tint = if (word.isFavorite) TechColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = word.term,
                        style = TermDisplayTextStyle,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = word.transcription,
                        style = IpaTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    FilledTonalButton(onClick = onSpeakTerm) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Прослушать")
                    }
                }

                Text(
                    text = when {
                        dragOffsetX > 90f -> "→ Свайп вправо: Хорошо (q=4)"
                        dragOffsetX < -90f -> "← Свайп влево: Не помню (q=1)"
                        else -> "Нажмите на карточку, чтобы проверить себя"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (abs(dragOffsetX) > 90f) swipeHintColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // BACK OF CARD (counter-rotated 180° so text reads normally)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = word.term,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onSpeakExample) {
                        Icon(
                            Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Озвучить пример",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TranslationContainerCard(translationText = word.translation)
                    PracticalExplanationCard(
                        title = "ПОЯСНЕНИЕ",
                        explanationText = word.explanationRu
                    )
                    HighlightedExampleQuoteCard(
                        exampleEn = word.example,
                        targetTerm = word.term
                    )
                }

                Text(
                    text = "EF: ${"%.2f".format(word.easeFactor)} • Повторений: ${word.repetitions}",
                    style = MonoTagTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RatingButton(
    quality: ReviewQuality,
    intervalDays: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val intervalText = if (intervalDays == 0) "Сейчас" else "$intervalDays дн."
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = quality.labelRu,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = intervalText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionCompletedCard(
    reviewed: Int,
    correct: Int,
    xp: Int,
    onPracticeMore: () -> Unit,
    onDone: () -> Unit
) {
    val accuracy = if (reviewed == 0) 0 else (correct * 100) / reviewed
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = TechColors.Emerald,
                modifier = Modifier.size(64.dp)
            )
            Text("Сессия завершена!", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Повторено: $reviewed • Точность: $accuracy% • +$xp XP",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onPracticeMore, modifier = Modifier.fillMaxWidth()) {
                Text("Повторить ещё партию")
            }
            FilledTonalButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Готово")
            }
        }
    }
}
