package com.techenglish.app.feature.documentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AudioControlBar
import com.techenglish.app.core.designsystem.ChameleonScenarioContainer
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.HighlightedExampleQuoteCard
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.JetBrainsMonoFontFamily
import com.techenglish.app.core.designsystem.PracticalExplanationCard
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermDisplayTextStyle
import com.techenglish.app.core.designsystem.TerminalSearchField
import com.techenglish.app.core.designsystem.TranslationContainerCard
import com.techenglish.app.core.designsystem.TtsVoiceStatusBanner
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.app.core.designsystem.rememberTechHaptics
import com.techenglish.domain.model.ChameleonThemeType
import com.techenglish.domain.model.DocArticle
import com.techenglish.domain.model.ScenarioChameleonRegistry
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentationListScreen(
    onBack: () -> Unit,
    onOpenArticle: (Int) -> Unit,
    viewModel: DocListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Техническая документация") },
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = navigationBarContentPadding(horizontal = 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TerminalSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholderText = "Поиск по статьям и логам ошибок...",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.techTags) { tag ->
                        FilterChip(
                            selected = state.selectedTag == tag,
                            onClick = { viewModel.onTagToggle(tag) },
                            label = { Text(tag, maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }

            if (state.articles.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Статьи не найдены",
                        subtitle = "Попробуйте выбрать другую категорию или очистить поиск.",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(state.articles, key = { it.id }) { article ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DocArticleCard(article = article, onClick = { onOpenArticle(article.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DocArticleCard(article: DocArticle, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(text = article.techTag)
                    StatusBadge(
                        text = article.level,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                if (article.isCompleted) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Прочитано",
                        tint = TechColors.Emerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(article.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = article.explanationRu,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (article.readingProgress > 0f) {
                LinearProgressIndicator(
                    progress = { article.readingProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

private const val WORD_TAG = "DOC_WORD"
private val englishWordRegex = Regex("[A-Za-z][A-Za-z0-9'-]*")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocReaderScreen(
    onBack: () -> Unit,
    viewModel: DocReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val haptics = rememberTechHaptics()

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    // Track scroll reading progress persistently in Room only once loaded
    LaunchedEffect(scrollState, state.isLoading, state.article?.id) {
        if (state.isLoading || state.article == null) return@LaunchedEffect
        var lastSavedProgress = 0f
        snapshotFlow {
            if (scrollState.maxValue <= 0) 0f
            else (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        }
            .distinctUntilChanged()
            .collect { progress ->
                if (progress > 0.1f && (progress - lastSavedProgress >= 0.05f || progress >= 0.98f)) {
                    lastSavedProgress = progress
                    viewModel.onScrollProgress(progress)
                }
            }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.article?.techTag ?: "Чтение",
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
                    IconButton(onClick = { viewModel.adjustFontSize(-1f) }) {
                        Icon(Icons.Rounded.TextDecrease, contentDescription = "Уменьшить шрифт")
                    }
                    IconButton(onClick = { viewModel.adjustFontSize(1f) }) {
                        Icon(Icons.Rounded.TextIncrease, contentDescription = "Увеличить шрифт")
                    }
                    IconButton(onClick = viewModel::toggleRussianGuide) {
                        Icon(
                            Icons.Rounded.Translate,
                            contentDescription = "Пояснение на русском",
                            tint = if (state.showRussianGuide) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            SkeletonListPlaceholder(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val article = state.article
        if (article == null) {
            EmptyStateView(title = "Статья не найдена", subtitle = "Вернитесь к списку документации.")
            return@Scaffold
        }

        val glossaryTermsLower = remember(article.glossary) {
            article.glossary.map { it.term.lowercase() }.toSet()
        }
        val highlightColor = MaterialTheme.colorScheme.primary
        val annotatedContent = remember(article.content, glossaryTermsLower, highlightColor) {
            buildAnnotatedString {
                append(article.content)
                englishWordRegex.findAll(article.content).forEach { match ->
                    val token = match.value
                    addStringAnnotation(
                        tag = WORD_TAG,
                        annotation = token,
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                    if (token.lowercase() in glossaryTermsLower) {
                        addStyle(
                            SpanStyle(
                                color = highlightColor,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            ),
                            match.range.first,
                            match.range.last + 1
                        )
                    }
                }
            }
        }
        var layoutResult by remember(article.id) { mutableStateOf<TextLayoutResult?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(navigationBarContentPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(text = article.techTag)
                StatusBadge(
                    text = "CEFR ${article.level}",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                StatusBadge(text = "${state.fontSizeSp.toInt()}sp")
            }

            Text(article.title, style = MaterialTheme.typography.headlineSmall)

            TtsVoiceStatusBanner(
                voiceStatus = voiceStatus,
                installIntentProvider = { viewModel.audioController.buildInstallTtsVoiceIntent() },
                onRecheck = { viewModel.audioController.recheckVoiceStatus() }
            )

            // TTS + Media3 Playback controls (ФТ-5.1)
            AudioControlBar(
                playbackState = playbackState,
                onPlay = viewModel::playArticle,
                onPause = { viewModel.audioController.pause() },
                onReplay = { viewModel.audioController.replay() },
                onStop = { viewModel.audioController.stop() },
                onSpeedChange = { viewModel.audioController.setSpeed(it) }
            )

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ) {
                Text(
                    text = "Нажмите на любое английское слово в тексте, чтобы увидеть перевод, пояснение и добавить его в SRS.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                )
            }

            // Interactive word-clickable documentation text (Terminal Chameleon theme for Error Logs, УИ-1.3, УИ-2.4)
            val docChameleonTheme = remember(article.techTag) {
                ScenarioChameleonRegistry.resolveForDocTag(article.techTag)
            }
            ChameleonScenarioContainer(
                themeType = docChameleonTheme,
                headerTitle = article.techTag,
                headerSubtitle = "${article.title} • CEFR ${article.level}"
            ) {
                Text(
                    text = annotatedContent,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = state.fontSizeSp.sp,
                        lineHeight = (state.fontSizeSp * 1.48f).sp,
                        fontFamily = if (docChameleonTheme == ChameleonThemeType.TERMINAL) JetBrainsMonoFontFamily
                        else MaterialTheme.typography.bodyLarge.fontFamily,
                        color = if (docChameleonTheme == ChameleonThemeType.TERMINAL) androidx.compose.ui.graphics.Color(0xFFE6EDF3)
                        else MaterialTheme.colorScheme.onSurface
                    ),
                    onTextLayout = { layoutResult = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(annotatedContent) {
                            detectTapGestures { offset ->
                                val charOffset = layoutResult?.getOffsetForPosition(offset) ?: return@detectTapGestures
                                val hit = annotatedContent.getStringAnnotations(
                                    tag = WORD_TAG,
                                    start = charOffset,
                                    end = charOffset
                                ).firstOrNull()
                                if (hit != null) {
                                    haptics.onWordTap()
                                    viewModel.onWordTapped(hit.item)
                                }
                            }
                        }
                )
            }

            // Key glossary quick chips
            if (article.glossary.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ключевые термины статьи", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        article.glossary.forEach { entry ->
                            AssistChip(
                                onClick = { viewModel.onWordTapped(entry.term) },
                                label = { Text("${entry.term} — ${entry.translation}") }
                            )
                        }
                    }
                }
            }

            // Collapsible Russian explanation
            FilledTonalButton(
                onClick = viewModel::toggleRussianGuide,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Translate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.showRussianGuide) "Скрыть пояснение на русском" else "Показать пояснение на русском")
            }

            if (state.showRussianGuide) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Разбор на русском", style = MaterialTheme.typography.titleSmall)
                        Text(article.explanationRu, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Button(
                onClick = viewModel::markCompleted,
                enabled = !article.isCompleted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (article.isCompleted) "Статья прочитана" else "Отметить прочитанным (+15 XP)")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // Word Lookup Popup Sheet (ФТ-2.2)
    state.lookupResult?.let { lookup ->
        ModalBottomSheet(onDismissRequest = viewModel::dismissLookup) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lookup.term, style = TermDisplayTextStyle)
                        Text(
                            lookup.transcription,
                            style = IpaTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.speakWord(lookup.term) }) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Озвучить")
                    }
                }
                TranslationContainerCard(translationText = lookup.translation)
                PracticalExplanationCard(
                    title = "КОНТЕКСТНОЕ ПОЯСНЕНИЕ",
                    explanationText = lookup.explanationRu
                )
                HighlightedExampleQuoteCard(
                    exampleEn = lookup.example,
                    targetTerm = lookup.term
                )
                Button(
                    onClick = { viewModel.addLookupToVocabulary(lookup) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (lookup.isAlreadyInVocabulary) "Закрепить в повторении SRS"
                        else "Добавить в словарь SRS"
                    )
                }
            }
        }
    }
}
