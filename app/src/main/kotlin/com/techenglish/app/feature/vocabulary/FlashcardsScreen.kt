package com.techenglish.app.feature.vocabulary

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AudioEqualizerButton
import com.techenglish.app.core.designsystem.AuroraScreenContainer
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

@Composable
fun FlashcardsScreen(
    onBack: () -> Unit,
    viewModel: FlashcardsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val haptics = rememberTechHaptics()
    val isDark = LocalTechInteractionConfig.current.isDarkTheme

    AuroraScreenContainer {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Custom Session Header
                val countLabel = if (state.totalInSession > 0 && !state.isSessionFinished) {
                    "Карточка ${(state.currentIndex + 1).coerceAtMost(state.totalInSession)} из ${state.totalInSession}"
                } else {
                    "Повторение SM-2"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(onClick = onBack)
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
                        text = countLabel,
                        style = MonoTagTextStyle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.size(40.dp))
                }

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
                        val progress = ((state.currentIndex + 1).toFloat() / state.totalInSession.coerceAtLeast(1)).coerceIn(0.04f, 1f)

                        // Glowing Cyan-to-Indigo Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                                                if (isDark) TechColors.Indigo else TechColors.Cyan
                                            )
                                        )
                                    )
                            )
                        }

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
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isDark) Color(0xFF06080F) else MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Rounded.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Показать ответ и пояснение", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Text(
                                text = "Оцените, насколько уверенно вспомнили термин (SM-2)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    accent = if (isDark) TechColors.Rose else TechColors.RoseDark,
                                    containerBg = if (isDark) TechColors.RoseContainerDark.copy(alpha = 0.6f) else TechColors.RoseContainerLight,
                                    textFg = if (isDark) TechColors.OnRoseContainerDark else TechColors.OnRoseContainerLight,
                                    onClick = {
                                        haptics.onSwipe()
                                        viewModel.rateCurrentCard(ReviewQuality.FORGOT)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                RatingButton(
                                    quality = ReviewQuality.HARD,
                                    intervalDays = state.intervalPreviews[ReviewQuality.HARD] ?: 1,
                                    accent = if (isDark) TechColors.Amber else TechColors.AmberDark,
                                    containerBg = if (isDark) TechColors.AmberContainerDark.copy(alpha = 0.6f) else TechColors.AmberContainerLight,
                                    textFg = if (isDark) TechColors.OnAmberContainerDark else TechColors.OnAmberContainerLight,
                                    onClick = {
                                        haptics.onCorrectAnswer()
                                        viewModel.rateCurrentCard(ReviewQuality.HARD)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                RatingButton(
                                    quality = ReviewQuality.GOOD,
                                    intervalDays = state.intervalPreviews[ReviewQuality.GOOD] ?: 6,
                                    accent = if (isDark) TechColors.Cyan else TechColors.CyanDark,
                                    containerBg = if (isDark) TechColors.CyanContainerDark.copy(alpha = 0.7f) else TechColors.CyanContainerLight,
                                    textFg = if (isDark) TechColors.OnCyanContainerDark else TechColors.OnCyanContainerLight,
                                    onClick = {
                                        haptics.onCorrectAnswer()
                                        viewModel.rateCurrentCard(ReviewQuality.GOOD)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                RatingButton(
                                    quality = ReviewQuality.EASY,
                                    intervalDays = state.intervalPreviews[ReviewQuality.EASY] ?: 14,
                                    accent = if (isDark) TechColors.Emerald else TechColors.EmeraldDark,
                                    containerBg = if (isDark) TechColors.EmeraldContainerDark.copy(alpha = 0.6f) else TechColors.EmeraldContainerLight,
                                    textFg = if (isDark) TechColors.OnEmeraldContainerDark else TechColors.OnEmeraldContainerLight,
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
}

@Composable
private fun SwipeableFlipFlashcard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSpeakTerm: () -> Unit,
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
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 260),
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
            .fillMaxHeight()
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
                    onHorizontalDrag = { _, dragAmount -> dragOffsetX += dragAmount }
                )
            }
            .clickable(onClick = onFlip),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(
            width = if (abs(dragOffsetX) > 90f) 2.dp else 1.dp,
            color = swipeHintColor
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        if (rotation <= 90f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FlashcardTopRow(
                    word = word,
                    onSpeak = onSpeakTerm,
                    onToggleFavorite = onToggleFavorite
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = word.term,
                        style = TermDisplayTextStyle,
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = word.transcription,
                            style = IpaTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = when {
                        dragOffsetX > 90f -> "→ Свайп вправо: Хорошо"
                        dragOffsetX < -90f -> "← Свайп влево: Снова"
                        else -> "Нажмите на карточку, чтобы перевернуть"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (abs(dragOffsetX) > 90f) swipeHintColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlashcardTopRow(
                    word = word,
                    onSpeak = onSpeakTerm,
                    onToggleFavorite = onToggleFavorite
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = word.term,
                        style = TermDisplayTextStyle,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = word.transcription,
                            style = IpaTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                TranslationContainerCard(translationText = word.translation)

                HighlightedExampleQuoteCard(
                    exampleEn = word.example,
                    targetTerm = word.term
                )

                PracticalExplanationCard(
                    title = "ПРАКТИЧЕСКОЕ ПРИМЕНЕНИЕ",
                    explanationText = word.explanationRu
                )
            }
        }
    }
}

@Composable
private fun FlashcardTopRow(
    word: Word,
    onSpeak: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            StatusBadge(
                text = word.categoryName,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            word.languageName?.let { StatusBadge(text = it) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AudioEqualizerButton(
                onClick = onSpeak,
                contentDescription = "Озвучить произношение",
                size = 38.dp
            )
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (word.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Избранное",
                    tint = if (word.isFavorite) TechColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant
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
    containerBg: Color,
    textFg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val intervalText = "$intervalDays дн."
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = containerBg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.50f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = quality.labelRu,
                style = MaterialTheme.typography.labelSmall,
                color = textFg,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = intervalText,
                style = MonoTagTextStyle,
                color = accent,
                maxLines = 1
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
