package com.techenglish.app.feature.scenarios

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AudioControlBar
import com.techenglish.app.core.designsystem.ChameleonScenarioContainer
import com.techenglish.app.core.designsystem.CodeTextStyle
import com.techenglish.app.core.designsystem.ComboStreakBanner
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TerminalSearchField
import com.techenglish.app.core.designsystem.TtsVoiceStatusBanner
import com.techenglish.app.core.designsystem.rememberTechHaptics
import com.techenglish.domain.model.ChameleonThemeType
import com.techenglish.domain.model.ExerciseType
import com.techenglish.domain.model.ScenarioChameleonRegistry
import com.techenglish.domain.model.ScenarioType
import com.techenglish.domain.model.WorkPhrase
import com.techenglish.domain.model.WorkScenario

// ==================== 1. PRACTICE HUB SCREEN (BOTTOM NAV TAB) ====================

@Composable
fun PracticeHubScreen(
    onOpenScenario: (Int) -> Unit,
    onOpenDocs: () -> Unit,
    onOpenListening: () -> Unit,
    onOpenReadOwnDocs: () -> Unit = {},
    onOpenDebugTheLog: () -> Unit = {},
    onOpenPronunciation: () -> Unit = {},
    onOpenTimeAttack: () -> Unit = {},
    viewModel: PracticeHubViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            SkeletonListPlaceholder(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Практика и сценарии", style = MaterialTheme.typography.headlineMedium)
            }

            // Quick launchers for Technical Docs & Listening Studio
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenDocs)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = TechColors.Emerald)
                            Text("Документация", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "166 статей и логов",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenListening)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Headphones, contentDescription = null, tint = TechColors.Rose)
                            Text("Аудирование", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Выбор и диктант офлайн",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Stage 2 Offline Trainers (ФТ-7.1, ФТ-7.2, ФТ-7.3, ФТ-7.5)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onOpenReadOwnDocs)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Свои доки", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Импорт .md / README",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onOpenDebugTheLog)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.BugReport, contentDescription = null, tint = TechColors.Rose)
                                Text("Debug the Log", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Разбор стектрейсов",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onOpenPronunciation)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, tint = TechColors.Emerald)
                                Text("Shadowing", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Тренажёр произношения",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onOpenTimeAttack)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Text("Time Attack", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Блиц-тест 60 секунд",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Switcher between Scenarios & 64 Workplace Phrases
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Сценарии (${state.scenarios.size})") }
                    SegmentedButton(
                        selected = state.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Фразы (${state.phrases.size})") }
                }
            }

            // Filter chips across ALL 8 mandatory scenario categories
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ScenarioType.entries) { type ->
                        FilterChip(
                            selected = state.selectedScenarioType == type,
                            onClick = { viewModel.toggleType(type) },
                            label = { Text(type.labelRu) }
                        )
                    }
                }
            }

            if (state.selectedTab == 0) {
                if (state.scenarios.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Сценарии не найдены",
                            subtitle = "Сбросьте фильтр, чтобы увидеть все тренажёры."
                        )
                    }
                } else {
                    items(state.scenarios, key = { it.id }) { scenario ->
                        ScenarioCard(
                            scenario = scenario,
                            isCompleted = scenario.id in state.completedScenarioIds,
                            onClick = { onOpenScenario(scenario.id) }
                        )
                    }
                }
            } else {
                item {
                    TerminalSearchField(
                        value = state.phraseQuery,
                        onValueChange = viewModel::onPhraseQueryChange,
                        placeholderText = "Поиск рабочей фразы на EN или RU..."
                    )
                }

                if (state.phrases.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Фразы не найдены",
                            subtitle = "Попробуйте другой поисковый запрос."
                        )
                    }
                } else {
                    items(state.phrases, key = { it.id }) { phrase ->
                        WorkPhraseCard(
                            phrase = phrase,
                            onSpeak = { viewModel.speak(phrase.phraseEn) },
                            onSave = { viewModel.savePhraseToVocabulary(phrase) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: WorkScenario,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(text = scenario.type.labelRu)
                    StatusBadge(
                        text = scenario.exerciseType.labelRu,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    StatusBadge(text = scenario.difficulty)
                }
                if (isCompleted) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Пройдено",
                        tint = TechColors.Emerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(scenario.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = scenario.promptRu,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun WorkPhraseCard(
    phrase: WorkPhrase,
    onSpeak: () -> Unit,
    onSave: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(text = phrase.category.labelRu)
                    StatusBadge(text = phrase.difficulty)
                }
                Row {
                    IconButton(onClick = onSpeak) {
                        Icon(
                            Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Озвучить фразу",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onSave) {
                        Icon(
                            Icons.Rounded.BookmarkAdd,
                            contentDescription = "Добавить в словарь",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Text(phrase.phraseEn, style = MaterialTheme.typography.titleSmall)
            Text(
                phrase.translationRu,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                phrase.contextRu,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== 2. INTERACTIVE SCENARIO EXERCISE RUNNER ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScenarioExerciseScreen(
    onBack: () -> Unit,
    viewModel: ScenarioExerciseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberTechHaptics()

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.scenario?.type?.labelRu ?: "Тренажёр") },
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
        val scenario = state.scenario
        if (scenario == null) {
            EmptyStateView(title = "Сценарий не найден", subtitle = "Вернитесь к списку.")
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val chameleonTheme = remember(scenario.type) {
                ScenarioChameleonRegistry.resolveForScenario(scenario.type)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(text = scenario.exerciseType.labelRu)
                StatusBadge(text = "CEFR ${scenario.difficulty}")
                StatusBadge(text = chameleonTheme.labelRu)
            }

            Text(scenario.title, style = MaterialTheme.typography.headlineSmall)

            ComboStreakBanner(comboState = state.comboState)

            TtsVoiceStatusBanner(
                voiceStatus = voiceStatus,
                installIntentProvider = { viewModel.audioController.buildInstallTtsVoiceIntent() },
                onRecheck = { viewModel.audioController.recheckVoiceStatus() }
            )

            // Chameleon Interface Prompt Card (GitHub PR / Slack / Terminal / IDE, УИ-2.4)
            ChameleonScenarioContainer(
                themeType = chameleonTheme,
                headerTitle = scenario.type.labelRu,
                headerSubtitle = "${scenario.type.labelRu} • ${scenario.exerciseType.labelRu}"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = scenario.promptRu,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (chameleonTheme == ChameleonThemeType.TERMINAL) androidx.compose.ui.graphics.Color(0xFFE6EDF3)
                        else MaterialTheme.colorScheme.onSurface
                    )

                    if (scenario.exerciseType == ExerciseType.LISTENING_CHOICE) {
                        AudioControlBar(
                            playbackState = playbackState,
                            onPlay = viewModel::playPromptAudio,
                            onPause = { viewModel.audioController.pause() },
                            onReplay = { viewModel.audioController.replay() },
                            onStop = { viewModel.audioController.stop() },
                            onSpeedChange = { viewModel.audioController.setSpeed(it) }
                        )
                        if (state.submissionResult != null) {
                            Text(
                                text = "Расшифровка: «${scenario.prompt}»",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = scenario.prompt,
                                style = if (chameleonTheme == ChameleonThemeType.TERMINAL) CodeTextStyle
                                else MaterialTheme.typography.bodyLarge,
                                color = if (chameleonTheme == ChameleonThemeType.TERMINAL) androidx.compose.ui.graphics.Color(0xFF56D364)
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.speak(scenario.prompt) }) {
                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Озвучить задание")
                            }
                        }
                    }
                }
            }

            // Exercise-type specific interactive workspace
            when (scenario.exerciseType) {
                ExerciseType.SENTENCE_BUILD -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ваше предложение:", style = MaterialTheme.typography.labelLarge)
                                if (state.selectedSentenceBlocks.isNotEmpty() && state.submissionResult == null) {
                                    TextButton(onClick = viewModel::clearSentenceBlocks) {
                                        Text("Сбросить")
                                    }
                                }
                            }
                            if (state.selectedSentenceBlocks.isEmpty()) {
                                Text(
                                    "Нажимайте на блоки ниже в правильном порядке...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.selectedSentenceBlocks.forEach { idx ->
                                        val word = scenario.options.getOrNull(idx) ?: return@forEach
                                        InputChip(
                                            selected = true,
                                            onClick = { viewModel.toggleSentenceBlock(idx) },
                                            label = { Text(word) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text("Доступные блоки:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scenario.options.forEachIndexed { idx, block ->
                            val used = idx in state.selectedSentenceBlocks
                            AssistChip(
                                onClick = { viewModel.toggleSentenceBlock(idx) },
                                enabled = !used && state.submissionResult == null,
                                label = { Text(block) }
                            )
                        }
                    }
                }

                ExerciseType.FILL_BLANKS -> {
                    OutlinedTextField(
                        value = state.customBlankInput,
                        onValueChange = viewModel::onBlankInputChange,
                        enabled = state.submissionResult == null,
                        label = { Text("Введите пропущенное слово или выберите ниже") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scenario.options.forEach { option ->
                            FilterChip(
                                selected = state.selectedOption == option,
                                onClick = { viewModel.selectOption(option) },
                                enabled = state.submissionResult == null,
                                label = { Text(option) }
                            )
                        }
                    }
                }

                ExerciseType.TRANSLATION_CHOICE,
                ExerciseType.PHRASE_CHOICE,
                ExerciseType.INTERVIEW_ANSWER,
                ExerciseType.LISTENING_CHOICE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        scenario.options.forEach { option ->
                            val isSelected = state.selectedOption == option
                            val result = state.submissionResult
                            val borderColor = when {
                                result != null && option == scenario.solution -> TechColors.Emerald
                                result != null && isSelected && !result.isCorrect -> TechColors.Rose
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = result == null) { viewModel.selectOption(option) },
                                border = BorderStroke(if (isSelected || borderColor != MaterialTheme.colorScheme.outlineVariant) 2.dp else 1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (scenario.exerciseType == ExerciseType.PHRASE_CHOICE ||
                                        scenario.exerciseType == ExerciseType.INTERVIEW_ANSWER
                                    ) {
                                        IconButton(onClick = { viewModel.speak(option) }) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.VolumeUp,
                                                contentDescription = "Озвучить вариант",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Submit button or Instant Feedback card
            val result = state.submissionResult
            if (result == null) {
                Button(
                    onClick = {
                        haptics.onCorrectAnswer()
                        viewModel.submitAnswer()
                    },
                    enabled = state.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Проверить ответ")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isCorrect) {
                            TechColors.Emerald.copy(alpha = 0.14f)
                        } else {
                            TechColors.Rose.copy(alpha = 0.14f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (result.isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                contentDescription = null,
                                tint = if (result.isCorrect) TechColors.Emerald else TechColors.Rose
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (result.isCorrect) "Верно! +${result.xpEarned} XP" else "Есть ошибка (+${result.xpEarned} XP за попытку)",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (result.isCorrect) TechColors.EmeraldDark else TechColors.Rose
                            )
                        }

                        Text("Эталонный ответ:", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = result.expectedAnswer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.speak(result.expectedAnswer) }) {
                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Озвучить эталон")
                            }
                        }

                        Text("Разбор:", style = MaterialTheme.typography.labelLarge)
                        Text(result.explanationRu, style = MaterialTheme.typography.bodyMedium)

                        if (result.usefulExpressions.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Полезные выражения:", style = MaterialTheme.typography.labelLarge)
                            result.usefulExpressions.forEach { expr ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(expr.en, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${expr.ru} • ${expr.context}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { viewModel.speak(expr.en) }) {
                                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Озвучить", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.saveUsefulExpression(expr) }) {
                                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = "В словарь", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = viewModel::retry,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Повторить")
                            }
                            state.nextScenarioId?.let { nextId ->
                                Button(
                                    onClick = { viewModel.loadScenario(nextId) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Дальше")
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
