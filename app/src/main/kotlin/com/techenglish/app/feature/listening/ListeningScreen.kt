package com.techenglish.app.feature.listening

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AudioControlBar
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TtsVoiceStatusBanner
import com.techenglish.domain.model.ListeningMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningScreen(
    onBack: () -> Unit,
    viewModel: ListeningViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аудирование и произношение") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TtsVoiceStatusBanner(
                voiceStatus = voiceStatus,
                installIntentProvider = { viewModel.audioController.buildInstallTtsVoiceIntent() },
                onRecheck = { viewModel.audioController.recheckVoiceStatus() }
            )

            // Mode switcher: Listen & Choose vs Listen & Type (Dictation)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ListeningMode.entries.forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = state.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(idx, ListeningMode.entries.size)
                    ) {
                        Text(mode.labelRu)
                    }
                }
            }

            // Corpus filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.includePhrases,
                    onClick = { viewModel.setIncludePhrases(true) },
                    label = { Text("Термины + рабочие фразы") }
                )
                FilterChip(
                    selected = !state.includePhrases,
                    onClick = { viewModel.setIncludePhrases(false) },
                    label = { Text("Только IT-термины") }
                )
            }

            when {
                state.isLoading -> SkeletonListPlaceholder(itemCount = 3)
                state.isFinished -> {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = TechColors.Emerald,
                                modifier = Modifier.size(60.dp)
                            )
                            Text("Тренировка завершена!", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = "Правильно: ${state.correctCount} из ${state.questions.size} • +${state.xpEarned} XP",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { viewModel.startNewSession() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Новая сессия аудирования")
                            }
                            FilledTonalButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                                Text("Назад")
                            }
                        }
                    }
                }
                state.currentQuestion == null -> {
                    EmptyStateView(
                        title = "Нет заданий для аудирования",
                        subtitle = "Попробуйте запустить новую сессию."
                    )
                }
                else -> {
                    val q = state.currentQuestion!!
                    val progress = ((state.currentIndex + 1).toFloat() / state.questions.size.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Задание ${state.currentIndex + 1} из ${state.questions.size}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(q.contextHintRu, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )

                    // Media3 + TTS Playback Control Card
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (state.mode == ListeningMode.CHOOSE) {
                                    "Прослушайте аудио и выберите верный вариант"
                                } else {
                                    "Прослушайте аудио и введите услышанный текст на английском"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            AudioControlBar(
                                playbackState = playbackState,
                                onPlay = viewModel::playCurrentAudio,
                                onPause = { viewModel.audioController.pause() },
                                onReplay = { viewModel.audioController.replay() },
                                onStop = { viewModel.audioController.stop() },
                                onSpeedChange = { viewModel.audioController.setSpeed(it) }
                            )
                        }
                    }

                    // Answer input area
                    if (state.mode == ListeningMode.CHOOSE) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            q.options.forEach { option ->
                                val selected = state.selectedOption == option
                                val eval = state.evaluation
                                val borderColor = when {
                                    eval != null && option == q.correctOption -> TechColors.Emerald
                                    eval != null && selected && !eval.isCorrect -> TechColors.Rose
                                    selected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = eval == null) { viewModel.onSelectOption(option) },
                                    border = BorderStroke(
                                        width = if (selected || borderColor != MaterialTheme.colorScheme.outlineVariant) 2.dp else 1.dp,
                                        color = borderColor
                                    )
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = state.dictationInput,
                            onValueChange = viewModel::onDictationChange,
                            enabled = state.evaluation == null,
                            label = { Text("Введите услышанную фразу на английском") },
                            placeholder = { Text("Type what you heard...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val eval = state.evaluation
                    if (eval == null) {
                        Button(
                            onClick = viewModel::submitCurrentAnswer,
                            enabled = if (state.mode == ListeningMode.CHOOSE) {
                                state.selectedOption != null
                            } else {
                                state.dictationInput.isNotBlank()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Проверить")
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (eval.isCorrect) {
                                    TechColors.Emerald.copy(alpha = 0.14f)
                                } else {
                                    TechColors.Rose.copy(alpha = 0.14f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (eval.isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                        contentDescription = null,
                                        tint = if (eval.isCorrect) TechColors.Emerald else TechColors.Rose
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (eval.isCorrect) {
                                            "Отлично! Совпадение ${eval.similarityPercent}% (+${eval.xpEarned} XP)"
                                        } else {
                                            "Попробуйте ещё раз — совпадение ${eval.similarityPercent}% (+${eval.xpEarned} XP)"
                                        },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Text("Оригинал (EN): ${q.audioText}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Перевод (RU): ${q.translationRu}", style = MaterialTheme.typography.bodySmall)
                                Button(
                                    onClick = viewModel::nextQuestion,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Следующее задание")
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
