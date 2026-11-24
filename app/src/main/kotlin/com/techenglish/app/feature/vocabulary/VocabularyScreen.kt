package com.techenglish.app.feature.vocabulary

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.ConfirmDialog
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.HighlightedExampleQuoteCard
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.PracticalExplanationCard
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermDisplayTextStyle
import com.techenglish.app.core.designsystem.TermTitleTextStyle
import com.techenglish.app.core.designsystem.TerminalSearchField
import com.techenglish.app.core.designsystem.TranslationContainerCard
import com.techenglish.app.core.designsystem.TtsVoiceStatusBanner
import com.techenglish.app.core.designsystem.categoryIconVector
import com.techenglish.domain.model.Category
import com.techenglish.domain.model.ProgrammingLanguage
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

    var selectedWord by remember { mutableStateOf<Word?>(null) }
    var showWordEditor by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<Word?>(null) }
    var wordPendingDelete by remember { mutableStateOf<Word?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingWord = null
                    showWordEditor = true
                },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Добавить слово") }
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("IT-словарь", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Показано слов: ${state.words.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = { onStartFlashcards(state.selectedCategoryId) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Карточки (${state.dueCount})")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenDeckImportExport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Импорт / экспорт", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = onOpenCommunityDecks,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Колоды сообщества", maxLines = 1)
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAcademicDictionary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Academic Technical Dictionary (20 600+)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Академическая и научная лексика • 36 областей • AI/ML, Crypto, GameDev, Web3, Mobile, QA",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(onClick = onOpenAcademicDictionary) {
                            Text("Открыть")
                        }
                    }
                }
            }

            item {
                TtsVoiceStatusBanner(
                    voiceStatus = voiceStatus,
                    installIntentProvider = { viewModel.audioController.buildInstallTtsVoiceIntent() },
                    onRecheck = { viewModel.audioController.recheckVoiceStatus() }
                )
            }

            // Search bar with blinking terminal cursor '_' (УИ-3.3)
            item {
                TerminalSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholderText = "Поиск термина, перевода или описания..."
                )
            }

            // Quick filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(VocabQuickFilter.entries) { filter ->
                        FilterChip(
                            selected = state.quickFilter == filter,
                            onClick = { viewModel.onQuickFilterSelect(filter) },
                            label = { Text(filter.labelRu) }
                        )
                    }
                }
            }

            // Category chips (all 10 + protected «Мои слова»)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = state.selectedCategoryId == cat.id,
                            onClick = { viewModel.onCategorySelect(cat.id) },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIconVector(cat.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            if (state.words.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Слова не найдены",
                        subtitle = "Попробуйте изменить запрос или добавьте собственный технический термин.",
                        actionLabel = "Добавить свой термин",
                        onAction = {
                            editingWord = null
                            showWordEditor = true
                        }
                    )
                }
            } else {
                items(state.words, key = { it.id }) { word ->
                    WordListCard(
                        word = word,
                        onClick = { selectedWord = word },
                        onSpeak = { viewModel.onSpeak(word.term) },
                        onToggleFavorite = { viewModel.onToggleFavorite(word) }
                    )
                }
            }
        }
    }

    // Word detail bottom sheet
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
    onToggleFavorite: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(word.term, style = TermTitleTextStyle)
                    if (word.languageName != null) {
                        StatusBadge(
                            text = word.languageName!!,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = word.transcription,
                    style = IpaTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(text = word.categoryName)
                    val masteryText = when {
                        word.isMastered -> "Выучено (${word.intervalDays} дн.)"
                        word.isLearned -> "Повторено: ${word.repetitions}"
                        else -> "Новое"
                    }
                    StatusBadge(
                        text = masteryText,
                        containerColor = if (word.isMastered) TechColors.Emerald.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (word.isMastered) TechColors.EmeraldDark
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onSpeak) {
                    Icon(
                        Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Озвучить термин",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onToggleFavorite) {
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

private val reviewDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
private fun WordDetailSheetContent(
    word: Word,
    onSpeakTerm: () -> Unit,
    onSpeakExample: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(word.term, style = TermDisplayTextStyle)
                Text(
                    word.transcription,
                    style = IpaTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onSpeakTerm) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Произнести")
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (word.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "В избранное",
                        tint = if (word.isFavorite) TechColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(text = word.categoryName)
            word.languageName?.let { StatusBadge(text = it) }
            if (word.isCustom) StatusBadge(text = "Пользовательское")
        }

        TranslationContainerCard(translationText = word.translation)

        PracticalExplanationCard(
            title = "ПОЯСНЕНИЕ",
            explanationText = word.explanationRu
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Пример в контексте", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = onSpeakExample) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Озвучить")
            }
        }
        HighlightedExampleQuoteCard(
            exampleEn = word.example,
            targetTerm = word.term
        )

        HorizontalDivider()

        val nextReviewLabel = if (word.nextReviewAt <= System.currentTimeMillis()) {
            "Доступно сейчас"
        } else {
            Instant.ofEpochMilli(word.nextReviewAt).atZone(ZoneId.systemDefault()).format(reviewDateFormatter)
        }
        Text(
            text = "SM-2: повторений ${word.repetitions} • EF ${"%.2f".format(word.easeFactor)} • интервал ${word.intervalDays} дн. • след. показ: $nextReviewLabel",
            style = MonoTagTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (onEdit != null || onDelete != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                onEdit?.let {
                    FilledTonalButton(onClick = it, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Редактировать")
                    }
                }
                onDelete?.let {
                    TextButton(onClick = it, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomWordEditorDialog(
    editingWord: Word?,
    categories: List<Category>,
    languages: List<ProgrammingLanguage>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Int?, Int?) -> Unit
) {
    var term by remember { mutableStateOf(editingWord?.term ?: "") }
    var transcription by remember { mutableStateOf(editingWord?.transcription ?: "") }
    var translation by remember { mutableStateOf(editingWord?.translation ?: "") }
    var example by remember { mutableStateOf(editingWord?.example ?: "") }
    var explanationRu by remember { mutableStateOf(editingWord?.explanationRu ?: "") }

    val defaultCatId = editingWord?.categoryId ?: categories.firstOrNull { it.isSystem }?.id ?: 11
    var selectedCategoryId by remember { mutableStateOf(defaultCatId) }
    var selectedLanguageId by remember { mutableStateOf(editingWord?.languageId) }
    var catExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingWord == null) "Новый термин" else "Редактировать термин") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text("English Term *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = transcription,
                    onValueChange = { transcription = it },
                    label = { Text("Транскрипция (IPA)") },
                    placeholder = { Text("/ˈɪn.deks/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text("Перевод на русский *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text("Пример употребления (EN)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = explanationRu,
                    onValueChange = { explanationRu = it },
                    label = { Text("Пояснение (RU)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Мои слова",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                // Optional Language dropdown
                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = !langExpanded }
                ) {
                    OutlinedTextField(
                        value = languages.firstOrNull { it.id == selectedLanguageId }?.name ?: "Не указан",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Язык программирования (опционально)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Не указан") },
                            onClick = {
                                selectedLanguageId = null
                                langExpanded = false
                            }
                        )
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.name) },
                                onClick = {
                                    selectedLanguageId = lang.id
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        term,
                        transcription,
                        translation,
                        example,
                        explanationRu,
                        selectedCategoryId,
                        selectedLanguageId
                    )
                },
                enabled = term.isNotBlank() && translation.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
