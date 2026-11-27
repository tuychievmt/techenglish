package com.techenglish.app.feature.stage4b

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SegmentedPillToggle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.app.feature.stage3.OfflineModeBanner
import com.techenglish.domain.model.AiInterviewLevel
import com.techenglish.domain.model.AiInterviewRole
import com.techenglish.domain.model.AiInterviewTurn

@Composable
fun AiMentorScreen(
    onBack: () -> Unit,
    viewModel: AiMentorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    AiMentorContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAcceptConsent = viewModel::acceptAiConsent,
        onSelectTab = viewModel::selectTab,
        onSelectRole = viewModel::selectRole,
        onSelectLevel = viewModel::selectLevel,
        onSubmitInterview = viewModel::submitInterviewAnswer,
        onSubmitPrReview = viewModel::submitPrReview,
        onDismissError = viewModel::dismissError
    )
}

@Composable
fun AiMentorContent(
    state: AiMentorUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onAcceptConsent: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onSelectRole: (AiInterviewRole) -> Unit,
    onSelectLevel: (AiInterviewLevel) -> Unit,
    onSubmitInterview: (String) -> Unit,
    onSubmitPrReview: (String) -> Unit,
    onDismissError: () -> Unit = {}
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    var interviewInput by remember { mutableStateOf("") }
    var prDraftInput by remember { mutableStateOf("") }

    // Clear input ONLY when a submission actually succeeded (never clear on auth/network/API error)
    LaunchedEffect(state.interviewSubmitSuccessSeq) {
        if (state.interviewSubmitSuccessSeq > 0) {
            interviewInput = ""
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val matches = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val transcript = matches?.firstOrNull().orEmpty()
            if (transcript.isNotBlank()) {
                if (state.selectedTabIndex == 0) {
                    interviewInput = if (interviewInput.isBlank()) transcript else "$interviewInput $transcript"
                } else {
                    prDraftInput = if (prDraftInput.isBlank()) transcript else "$prDraftInput $transcript"
                }
            }
        }
    }

    val activeInput = if (state.selectedTabIndex == 0) interviewInput else prDraftInput
    val trimmedLength = activeInput.trim().length
    val minRequiredLength = AI_MENTOR_MIN_CHARS

    AuroraScreenContainer {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AiMentorTopBar(
                    state = state,
                    isDark = isDark,
                    onBack = onBack
                )
            },
            bottomBar = {
                if (state.consentAccepted) {
                    AiComposerBottomBar(
                        value = activeInput,
                        onValueChange = {
                            if (state.errorMessage != null) onDismissError()
                            if (state.selectedTabIndex == 0) interviewInput = it else prDraftInput = it
                        },
                        placeholder = if (state.selectedTabIndex == 0) "Ваш ответ на английском..." else "Черновик комментария к PR...",
                        canSubmit = trimmedLength >= minRequiredLength && !state.isBusy,
                        isBusy = state.isBusy,
                        validationHint = if (!state.isBusy && trimmedLength < minRequiredLength) {
                            "Минимум $minRequiredLength символа ($trimmedLength/$minRequiredLength)"
                        } else null,
                        submitLabel = if (state.selectedTabIndex == 0) "Отправить →" else "Проверить →",
                        onVoiceClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak in English")
                            }
                            runCatching { speechLauncher.launch(intent) }
                        },
                        onSubmit = {
                            if (state.selectedTabIndex == 0) {
                                onSubmitInterview(interviewInput)
                            } else {
                                onSubmitPrReview(prDraftInput)
                            }
                        }
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = if (!state.consentAccepted) {
                        navigationBarContentPadding(top = 12.dp)
                    } else {
                        PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
                    },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isOffline) {
                        item {
                            OfflineModeBanner("Офлайн-режим: AI-прокси работает онлайн; локальные тренажёры доступны без сети.")
                        }
                    }

                    state.errorMessage?.let { errMsg ->
                        item {
                            AiMentorErrorBanner(
                                message = errMsg,
                                onDismiss = onDismissError
                            )
                        }
                    }

                // Explicit Consent Gate (ФТ-8.3)
                if (!state.consentAccepted) {
                    item {
                        BentoGlassCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Согласие на обработку текста AI-прокси (ФТ-8.3)", style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    text = "• Запросы идут строго через серверный FastAPI-прокси (без API-ключей в приложении).\n" +
                                        "• Персональные данные профиля (email, username, ссылки) автоматически удаляются.\n" +
                                        "• Голосовой ввод распознаётся на устройстве — аудиозаписи НЕ сохраняются и не отправляются.\n" +
                                        "• Действует серверный дневной лимит (${state.dailyLimit} запросов/день).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onAcceptConsent,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("Принять условия и включить AI-ментора")
                                }
                            }
                        }
                    }
                    return@LazyColumn
                }

                // 2. Mode Switcher
                item {
                    SegmentedPillToggle(
                        options = listOf("Mock Interview", "Code / PR Reviewer"),
                        selectedIndex = state.selectedTabIndex,
                        onSelect = onSelectTab
                    )
                }

                if (state.selectedTabIndex == 0) {
                    // 3. Role & Seniority Chips
                    item {
                        BentoGlassCard {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(AiInterviewRole.entries) { role ->
                                        val selected = state.role == role
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.clickable { onSelectRole(role) }
                                        ) {
                                            Text(
                                                text = role.labelRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AiInterviewLevel.entries.forEach { lvl ->
                                        val selected = state.level == lvl
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.clickable { onSelectLevel(lvl) }
                                        ) {
                                            Text(
                                                text = lvl.labelRu,
                                                style = MonoTagTextStyle,
                                                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Interviewer Question Aura Card
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isDark) {
                                            Brush.linearGradient(
                                                listOf(Color(0xFF1E2A4A), Color(0xFF292042), Color(0xFF111522))
                                            )
                                        } else {
                                            Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF4F7FF)))
                                        }
                                    )
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "ВОПРОС ИНТЕРВЬЮЕРА · EN",
                                            style = MonoTagTextStyle,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = state.currentQuestionEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (state.isBusy) {
                        item {
                            AiMentorBusyBanner(text = "AI-ментор анализирует ваш ответ…")
                        }
                    }

                    // 5. Evaluation Turns with Dual Canvas Score Rings
                    items(state.interviewTurns, key = { it.id }) { turn ->
                        AiTurnEvaluationCard(turn = turn)
                    }
                } else {
                    item {
                        BentoGlassCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Code / PR Reviewer", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Введите черновик комментария к Pull Request в поле ниже — AI-ментор оценит тон и предложит более вежливую и точную формулировку.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (state.isBusy) {
                        item {
                            AiMentorBusyBanner(text = "AI-ментор проверяет тон PR-комментария…")
                        }
                    }

                    state.prReviewFeedback?.let { fb ->
                        item {
                            BentoGlassCard {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (fb.isFallback) {
                                        Text(
                                            text = "Резервный эвристический разбор" +
                                                (fb.fallbackReason?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Text("Оценка тона:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    Text(fb.toneAssessmentRu, style = MaterialTheme.typography.bodyMedium)
                                    ImprovedPhrasingBlock(
                                        title = "РЕКОМЕНДУЕМЫЙ ВАРИАНТ (EN)",
                                        text = fb.improvedEnglishText
                                    )
                                    fb.keySuggestionsRu.forEach { tip ->
                                        Text("• $tip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun AiMentorTopBar(
    state: AiMentorUiState,
    isDark: Boolean,
    onBack: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    Text("AI-ментор", style = MaterialTheme.typography.headlineSmall)
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = if (isDark) TechColors.Emerald else TechColors.EmeraldDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "PII скрыт",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.remainingDailyQuota}/${state.dailyLimit}",
                            style = MonoTagTextStyle,
                            color = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun AiTurnEvaluationCard(turn: AiInterviewTurn) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    BentoGlassCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScoreRingPill(
                    label = "English",
                    score = turn.englishScore,
                    accent = if (isDark) TechColors.Cyan else TechColors.CyanDark,
                    containerBg = if (isDark) TechColors.CyanContainerDark.copy(alpha = 0.7f) else TechColors.CyanContainerLight,
                    textFg = if (isDark) TechColors.OnCyanContainerDark else TechColors.OnCyanContainerLight,
                    modifier = Modifier.weight(1f)
                )
                ScoreRingPill(
                    label = "Tech",
                    score = turn.technicalScore,
                    accent = if (isDark) TechColors.Emerald else TechColors.EmeraldDark,
                    containerBg = if (isDark) TechColors.EmeraldContainerDark.copy(alpha = 0.6f) else TechColors.EmeraldContainerLight,
                    textFg = if (isDark) TechColors.OnEmeraldContainerDark else TechColors.OnEmeraldContainerLight,
                    modifier = Modifier.weight(1f)
                )
            }

            if (turn.isFallback) {
                Text(
                    text = "Резервный эвристический разбор" +
                        (turn.fallbackReason?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = turn.feedbackRu,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            ImprovedPhrasingBlock(
                title = "УЛУЧШЕННАЯ ФОРМУЛИРОВКА (EN)",
                text = turn.improvedAnswerEn
            )
        }
    }
}

@Composable
private fun ScoreRingPill(
    label: String,
    score: Int,
    accent: Color,
    containerBg: Color,
    textFg: Color,
    modifier: Modifier = Modifier
) {
    val trackCol = MaterialTheme.colorScheme.surfaceContainerHigh
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerBg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val stroke = 3.dp.toPx()
                    drawArc(
                        color = trackCol,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = textFg)
            }
            Text(
                text = "$score/100",
                style = MonoTagTextStyle,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ImprovedPhrasingBlock(title: String, text: String) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val bg = if (isDark) TechColors.TerminalObsidian else TechColors.EmeraldContainerLight.copy(alpha = 0.65f)
    val accent = if (isDark) TechColors.Emerald else TechColors.EmeraldDark
    val bodyCol = if (isDark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(64.dp)
                    .background(accent)
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, style = MonoTagTextStyle, color = accent)
                Text(text = text, style = MaterialTheme.typography.bodyMedium, color = bodyCol)
            }
        }
    }
}

@Composable
private fun AiMentorErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Скрыть ошибку",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AiMentorBusyBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AiComposerBottomBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    canSubmit: Boolean,
    isBusy: Boolean,
    validationHint: String?,
    submitLabel: String,
    onVoiceClick: () -> Unit,
    onSubmit: () -> Unit
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = !isBusy,
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (validationHint != null) {
                Text(
                    text = validationHint,
                    style = MonoTagTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable(enabled = !isBusy, onClick = onVoiceClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = "Голосовой ввод",
                            tint = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Голос", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isDark) Color(0xFF06080F) else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Отправка…", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Text(submitLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
