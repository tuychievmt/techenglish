package com.techenglish.app.feature.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AudioEqualizerButton
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.ConfirmDialog
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermTitleTextStyle
import com.techenglish.app.core.designsystem.TerminalSearchField
import com.techenglish.app.core.designsystem.TtsVoiceStatusBanner
import com.techenglish.app.core.designsystem.categoryIconVector
import com.techenglish.app.core.designsystem.formatCountSpaced
import com.techenglish.domain.model.Word
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    onStartFlashcards: (categoryId: Int?) -> Unit,
    onOpenAcademicDictionary: () -> Unit = {},
    onOpenDeckImportExport: () -> Unit = {},
    onOpenCommunityDecks: () -> Unit = {},
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = LocalTechInteractionConfig.current.isDarkTheme

    var selectedWord by remember { mutableStateOf<Word?>(null) }
    var showWordEditor by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<Word?>(null) }
    var wordPendingDelete by remember { mutableStateOf<Word?>(null) }
    var showToolsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    val listState = rememberLazyListState()
    val fabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
        }
    }

    AuroraScreenContainer {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                Surface(
                    onClick = {
                        editingWord = null
                        showWordEditor = true
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .height(44.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = if (fabExpanded) {
                            Modifier.padding(horizontal = 14.dp)
                        } else {
                            Modifier.size(44.dp)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Добавить слово",
                            modifier = Modifier.size(20.dp)
                        )
                        AnimatedVisibility(visible = fabExpanded) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Добавить слово",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            if (state.isLoading) {
                SkeletonListPlaceholder(modifier = Modifier.padding(padding).statusBarsPadding())
                return@Scaffold
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Compact Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "IT-словарь",
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "${state.words.size} терминов",
                                    style = MonoTagTextStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onStartFlashcards(state.selectedCategoryId) },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isDark) Color(0xFF06080F) else MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = "Карточки (${state.dueCount}) →",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { showToolsSheet = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SwapVert,
                                        contentDescription = "Импорт, экспорт и колоды сообщества",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Compact Academic Dictionary Strip
                item {
                    BentoGlassCard(
                        onClick = onOpenAcademicDictionary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.School,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Academic Technical Dictionary · ${formatCountSpaced(state.academicTermCount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Открыть →",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                item {
                    TtsVoiceStatusBanner(
                        voiceStatus = voiceStatus,
                        installIntentProvider = { viewModel.audioController.buildInstallTtsVoiceIntent() },
                        onRecheck = { viewModel.audioController.recheckVoiceStatus() },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 3. Terminal Search Field (>_)
                item {
                    TerminalSearchField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholderText = "Поиск термина, перевода или IPA...",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 4. Two-Row Edge-to-Edge Horizontal Filters
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(VocabQuickFilter.entries) { filter ->
                                val selected = state.quickFilter == filter
                                val label = if (filter == VocabQuickFilter.DUE) {
                                    "${filter.labelRu} (${state.dueCount})"
                                } else {
                                    filter.labelRu
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.clickable { viewModel.onQuickFilterSelect(filter) }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.categories, key = { it.id }) { cat ->
                                val selected = state.selectedCategoryId == cat.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.clickable { viewModel.onCategorySelect(cat.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = categoryIconVector(cat.icon),
                                            contentDescription = null,
                                            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = cat.name,
                                            style = MonoTagTextStyle,
                                            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Term Cards
                if (state.words.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Слова не найдены",
                            subtitle = "Попробуйте изменить запрос или добавьте собственный технический термин.",
                            actionLabel = "Добавить свой термин",
                            onAction = {
                                editingWord = null
                                showWordEditor = true
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(state.words, key = { it.id }) { word ->
                        WordListCard(
                            word = word,
                            onClick = { selectedWord = word },
                            onSpeak = { viewModel.onSpeak(word.term) },
                            onToggleFavorite = { viewModel.onToggleFavorite(word) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showToolsSheet) {
        ModalBottomSheet(onDismissRequest = { showToolsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Инструменты словаря", style = MaterialTheme.typography.titleMedium)
                BentoGlassCard(
                    onClick = {
                        showToolsSheet = false
                        onOpenDeckImportExport()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.ImportExport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Импорт / экспорт колод", style = MaterialTheme.typography.titleSmall)
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
                BentoGlassCard(
                    onClick = {
                        showToolsSheet = false
                        onOpenCommunityDecks()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = TechColors.Cyan)
                            Text("Колоды сообщества", style = MaterialTheme.typography.titleSmall)
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }

    selectedWord?.let { word ->
        ModalBottomSheet(onDismissRequest = { selectedWord = null }) {
            WordDetailSheetContent(
                word = word,
                onSpeakTerm = { viewModel.onSpeak(word.term) },
                onSpeakExample = { viewModel.onSpeak(word.example) },
                onToggleFavorite = {
                    viewModel.onToggleFavorite(word)
                    selectedWord = word.copy(isFavorite = !word.isFavorite)
                },
                onEdit = if (word.isCustom) {
                    {
                        editingWord = word
                        selectedWord = null
                        showWordEditor = true
                    }
                } else null,
                onDelete = if (word.isCustom) {
                    {
                        wordPendingDelete = word
                        selectedWord = null
                    }
                } else null
            )
        }
    }

    if (showWordEditor) {
        CustomWordEditorDialog(
            editingWord = editingWord,
            categories = state.categories,
            languages = state.languages,
            onDismiss = { showWordEditor = false },
            onSave = { term, ipa, ru, ex, expl, catId, langId ->
                viewModel.onSaveCustomWord(editingWord, term, ipa, ru, ex, expl, catId, langId)
                showWordEditor = false
            }
        )
    }

    wordPendingDelete?.let { target ->
        ConfirmDialog(
            title = "Удалить термин?",
            message = "Термин «${target.term}» будет удалён из пользовательского словаря. История занятий сохранится.",
            onConfirm = {
                viewModel.onDeleteCustomWord(target)
                wordPendingDelete = null
            },
            onDismiss = { wordPendingDelete = null }
        )
    }
}

@Composable
private fun WordListCard(
    word: Word,
    onClick: () -> Unit,
    onSpeak: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val masteryBg = when {
        word.isMastered -> if (isDark) TechColors.EmeraldContainerDark else TechColors.EmeraldContainerLight
        word.isLearned -> MaterialTheme.colorScheme.primaryContainer
        else -> if (isDark) TechColors.AmberContainerDark else TechColors.AmberContainerLight
    }
    val masteryFg = when {
        word.isMastered -> if (isDark) TechColors.OnEmeraldContainerDark else TechColors.OnEmeraldContainerLight
        word.isLearned -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> if (isDark) TechColors.OnAmberContainerDark else TechColors.OnAmberContainerLight
    }
    val masteryText = when {
        word.isMastered -> "Выучено · ${word.intervalDays} дн."
        word.isLearned -> "Повторено: ${word.repetitions}"
        else -> "К повторению"
    }

    BentoGlassCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = word.term,
                        style = TermTitleTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    StatusBadge(
                        text = word.languageName ?: word.categoryName,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = word.transcription,
                    style = IpaTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = masteryBg
                ) {
                    Text(
                        text = masteryText,
                        style = MonoTagTextStyle,
                        color = masteryFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AudioEqualizerButton(
                    onClick = onSpeak,
                    contentDescription = "Озвучить термин",
                    size = 42.dp
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
}
