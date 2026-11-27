package com.techenglish.app.feature.stage4c

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.app.feature.stage3.OfflineModeBanner
import com.techenglish.domain.model.PeerReviewSubmission
import com.techenglish.domain.model.PvpMatchMode
import com.techenglish.domain.model.PvpMatchState
import com.techenglish.domain.model.RoomChatMessage
import com.techenglish.domain.model.ThematicRoomChannel

// =========================================================================
// 1. Thematic Developer Rooms Screen (ФТ-13.3 — WebSockets + Moderation)
// =========================================================================

@Composable
fun ThematicRoomsScreen(
    onBack: () -> Unit,
    viewModel: ThematicRoomsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    ThematicRoomsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSelectRoom = viewModel::selectRoom,
        onSendMessage = viewModel::sendMessage,
        onReportAndBlock = viewModel::reportAndBlockMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThematicRoomsContent(
    state: ThematicRoomsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onSelectRoom: (ThematicRoomChannel) -> Unit,
    onSendMessage: (String) -> Unit,
    onReportAndBlock: (RoomChatMessage, String, Boolean) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var moderationTarget by remember { mutableStateOf<RoomChatMessage?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Тематические комнаты (Rooms)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = navigationBarContentPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isOffline) {
                item { OfflineModeBanner("Офлайн-режим: показана локальная история room_messages_cache.") }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThematicRoomChannel.entries.forEach { room ->
                        FilterChip(
                            selected = state.selectedRoom == room,
                            onClick = { onSelectRoom(room) },
                            label = { Text(room.title, style = MonoTagTextStyle) }
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.selectedRoom.title, style = MaterialTheme.typography.titleSmall)
                            Text(state.selectedRoom.descriptionRu, style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.isWebSocketConnected) {
                            Icon(Icons.Rounded.Wifi, contentDescription = "WebSocket подключён", tint = TechColors.EmeraldDark)
                        }
                    }
                }
            }

            // Composer
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = { Text("Сообщение в ${state.selectedRoom.title}") },
                            supportingText = {
                                val len = draft.trim().length
                                if (len in 0..4) Text("Минимум 5 символов ($len/5)")
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onSendMessage(draft)
                                draft = ""
                            },
                            enabled = draft.trim().length >= 5 && !state.isBusy
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Отправить")
                        }
                    }
                }
            }

            if (state.messages.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(32.dp))
                            Text("В комнате пока нет сообщений", style = MaterialTheme.typography.titleSmall)
                            Text("Поделитесь фразой с Code Review или вопросом с интервью.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(state.messages, key = { it.id }) { msg ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "@${msg.authorUsername} • ${msg.authorRankTitle}",
                                    style = MonoTagTextStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { moderationTarget = msg },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Flag, contentDescription = "Пожаловаться на сообщение", modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(msg.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    moderationTarget?.let { target ->
        Stage4CModerationDialog(
            title = "Пожаловаться на сообщение @${target.authorUsername}",
            onDismiss = { moderationTarget = null },
            onSubmit = { reason, blockAuthor ->
                onReportAndBlock(target, reason, blockAuthor)
                moderationTarget = null
            }
        )
    }
}

// =========================================================================
// 2. PvP 1v1 Duels Screen (ФТ-14.1 — 60s, 15s Training Fallback, Disconnect)
// =========================================================================

@Composable
fun PvpDuelScreen(
    onBack: () -> Unit,
    viewModel: PvpDuelViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    BackHandler {
        viewModel.handleDisconnectGracefully()
        onBack()
    }

    PvpDuelContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = {
            viewModel.handleDisconnectGracefully()
            onBack()
        },
        onStartRanked = viewModel::startRankedSearch,
        onStartTraining = viewModel::startTrainingFallbackRound,
        onSelectAnswer = viewModel::submitAnswer,
        onDisconnect = viewModel::handleDisconnectGracefully
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvpDuelContent(
    state: PvpDuelUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onStartRanked: () -> Unit,
    onStartTraining: () -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onDisconnect: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("1v1 PvP-дуэль (WebSockets)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = navigationBarContentPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.isOffline) {
                item { OfflineModeBanner("Офлайн-режим: доступен тренировочный раунд без рейтинговых очков.") }
            }

            when (state.matchState) {
                PvpMatchState.IDLE -> {
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = TechColors.AmberDark)
                                    Text("60-секундная дуэль разработчиков", style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    text = "• 8 вопросов с сервера: перевод терминов, диагностика логов и фразы для Pull Request.\n" +
                                        "• Ожидание соперника — 15 секунд; если соперник не найден, предлагается тренировочный раунд без рейтинговых очков.\n" +
                                        "• При разрыве связи матч корректно завершается без зависания.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(onClick = onStartRanked, modifier = Modifier.fillMaxWidth()) {
                                    Text("Найти соперника (Рейтинговая 1v1)")
                                }
                                OutlinedButton(onClick = onStartTraining, modifier = Modifier.fillMaxWidth()) {
                                    Text("Тренировочный раунд (0 рейтинговых очков)")
                                }
                            }
                        }
                    }
                }

                PvpMatchState.SEARCHING -> {
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Поиск соперника близкого ранга…", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Осталось ${state.matchmakingSecondsLeft} с до тренировочного режима",
                                    style = MonoTagTextStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = onStartTraining) {
                                        Text("Перейти к тренировке сейчас")
                                    }
                                    OutlinedButton(onClick = onDisconnect) {
                                        Text("Отмена")
                                    }
                                }
                            }
                        }
                    }
                }

                PvpMatchState.IN_PROGRESS -> {
                    val q = state.questions.getOrNull(state.currentQuestionIndex)
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.mode.labelRu} • Вопрос ${state.currentQuestionIndex + 1}/${state.questions.size} • Вы ${state.playerScore}:${state.opponentScore} Соперник",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("${state.roundSecondsLeft}s", style = MonoTagTextStyle, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (q != null) {
                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("[${q.categoryTag}]", style = MonoTagTextStyle, color = MaterialTheme.colorScheme.primary)
                                    Text(q.promptEn, style = MaterialTheme.typography.titleMedium)
                                    q.options.forEachIndexed { idx, opt ->
                                        FilledTonalButton(
                                            onClick = { onSelectAnswer(idx) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(opt)
                                        }
                                    }
                                    HorizontalDivider()
                                    OutlinedButton(
                                        onClick = onDisconnect,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Завершить / Сдаться (Disconnect)")
                                    }
                                }
                            }
                        }
                    }
                }

                PvpMatchState.COMPLETED,
                PvpMatchState.DISCONNECTED_FORFEIT,
                PvpMatchState.DISCONNECTED_OPPONENT_LEFT -> {
                    item {
                        val out = state.outcome
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (state.matchState == PvpMatchState.COMPLETED) "Матч завершён" else "Матч прерван (разрыв связи)",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(out?.summaryRu ?: "Счёт: ${state.playerScore}", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Рейтинговый XP: +${out?.rankedXpAwarded ?: 0} • Дельта рейтинга: ${out?.ratingDelta ?: 0}",
                                    style = MonoTagTextStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Button(onClick = onStartRanked, modifier = Modifier.fillMaxWidth()) {
                                    Text("Сыграть ещё раз")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. Peer Review Arena Screen (ФТ-14.2 & Errata E5 — 30-day Voice TTL)
// =========================================================================

@Composable
fun PeerReviewArenaScreen(
    onBack: () -> Unit,
    viewModel: PeerReviewArenaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    PeerReviewArenaContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onSubmitResponse = viewModel::submitResponse,
        onSubmitFeedback = viewModel::submitFeedback,
        onReportAndBlock = viewModel::reportAndBlockSubmission,
        onStartRecording = viewModel::startVoiceRecording,
        onStopRecording = viewModel::stopVoiceRecording,
        onClearVoiceClip = viewModel::clearRecordedVoice,
        onPlayVoiceClip = viewModel::playSubmissionVoice,
        onMicPermissionDenied = viewModel::onMicPermissionDenied
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerReviewArenaContent(
    state: PeerReviewArenaUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSubmitResponse: (text: String, voiceBase64: String?, voiceDurationSeconds: Int) -> Unit,
    onSubmitFeedback: (submissionId: String, stars: Int, comment: String) -> Unit,
    onReportAndBlock: (PeerReviewSubmission, String, Boolean) -> Unit,
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onClearVoiceClip: () -> Unit = {},
    onPlayVoiceClip: (String) -> Unit = {},
    onMicPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartRecording() else onMicPermissionDenied()
    }
    var answerText by remember { mutableStateOf("") }
    var feedbackTarget by remember { mutableStateOf<PeerReviewSubmission?>(null) }
    var moderationTarget by remember { mutableStateOf<PeerReviewSubmission?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Peer Review Arena") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = navigationBarContentPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isOffline) {
                item { OfflineModeBanner("Офлайн-режим: ежедневный кейс доступен для подготовки; отправка требует интернет.") }
            }

            // Daily Case Card
            item {
                val c = state.dailyCase
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.RateReview, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Ежедневный кейс (${c.dateIso})", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(c.titleEn, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(c.promptEn, style = MaterialTheme.typography.bodyMedium)
                        Text(c.promptRu, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Голосовые ответы (≤ 20 с) хранятся в S3/MinIO не более ${state.voiceTtlDays} дней (TTL, Errata E5) и удаляются вместе с аккаунтом.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        OutlinedTextField(
                            value = answerText,
                            onValueChange = { answerText = it },
                            label = { Text("Ваш ответ на английском (минимум 15 символов)") },
                            supportingText = {
                                val len = answerText.trim().length
                                if (state.recordedVoiceClip == null && len < 15) {
                                    Text("Напишите ещё ${15 - len} симв. или запишите голос")
                                }
                            },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (state.isRecordingVoice) {
                                        onStopRecording()
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) onStartRecording()
                                        else recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (state.isRecordingVoice) "Стоп (${state.recordingSecondsElapsed}/20 с)"
                                    else "Записать голос (≤ 20 с)"
                                )
                            }
                            if (state.recordedVoiceClip != null) {
                                OutlinedButton(onClick = onClearVoiceClip) {
                                    Text("Убрать (${state.recordedVoiceClip.durationSeconds} с)")
                                }
                            }
                        }
                        state.micPermissionError?.let { err ->
                            Text(text = err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = {
                                onSubmitResponse(answerText, null, 0)
                                answerText = ""
                            },
                            enabled = (answerText.trim().length >= 15 || state.recordedVoiceClip != null) && !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Отправить ответ на Peer Review")
                        }
                    }
                }
            }

            item {
                Text("Ответы сообщества (${state.submissions.size})", style = MaterialTheme.typography.titleLarge)
            }

            items(state.submissions, key = { it.id }) { sub ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "@${sub.authorUsername} • ${sub.authorRankTitle}",
                                style = MonoTagTextStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { moderationTarget = sub },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.Flag, contentDescription = "Пожаловаться на ответ", modifier = Modifier.size(16.dp))
                            }
                        }
                        if (sub.textResponse.isNotBlank()) {
                            Text(sub.textResponse, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (sub.hasVoiceClip) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = TechColors.EmeraldDark)
                                    Text(
                                        text = "Голос: ${sub.voiceDurationSeconds} с (TTL 30 дн.)",
                                        style = MonoTagTextStyle,
                                        color = TechColors.EmeraldDark
                                    )
                                }
                                TextButton(onClick = { onPlayVoiceClip(sub.id) }) {
                                    Text(if (state.playingSubmissionId == sub.id) "Воспроизведение…" else "Прослушать")
                                }
                            }
                        }
                        sub.feedbackList.forEach { fb ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = TechColors.Amber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${fb.ratingStars} @${fb.reviewerUsername}: ${fb.commentText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { feedbackTarget = sub },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Оставить Peer Review") }
                    }
                }
            }
        }
    }

    feedbackTarget?.let { target ->
        var stars by remember { mutableIntStateOf(5) }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { feedbackTarget = null },
            title = { Text("Peer Review для @${target.authorUsername}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { s ->
                            FilterChip(
                                selected = stars == s,
                                onClick = { stars = s },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("$s", style = MonoTagTextStyle)
                                        Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(13.dp))
                                    }
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Разбор формулировок и советы") },
                        supportingText = {
                            val len = comment.trim().length
                            if (len in 0..4) Text("Минимум 5 символов ($len/5)")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitFeedback(target.id, stars, comment)
                        feedbackTarget = null
                    },
                    enabled = comment.trim().length >= 5
                ) { Text("Отправить") }
            },
            dismissButton = {
                TextButton(onClick = { feedbackTarget = null }) { Text("Отмена") }
            }
        )
    }

    moderationTarget?.let { target ->
        Stage4CModerationDialog(
            title = "Пожаловаться на ответ @${target.authorUsername}",
            onDismiss = { moderationTarget = null },
            onSubmit = { reason, blockAuthor ->
                onReportAndBlock(target, reason, blockAuthor)
                moderationTarget = null
            }
        )
    }
}

@Composable
private fun Stage4CModerationDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, blockAuthor: Boolean) -> Unit
) {
    var reason by remember { mutableStateOf("Спам или нарушение правил сообщества") }
    var blockAuthor by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Причина жалобы") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { blockAuthor = !blockAuthor }
                ) {
                    Checkbox(checked = blockAuthor, onCheckedChange = { blockAuthor = it })
                    Spacer(Modifier.width(6.dp))
                    Text("Также заблокировать автора", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason, blockAuthor) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Отправить жалобу")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
