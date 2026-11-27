package com.techenglish.app.feature.programming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.CodeSyntaxHighlighter
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.CodeTextStyle
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.ensureAccessibleAccent
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.app.core.designsystem.parseHexColor
import com.techenglish.app.core.designsystem.rememberTechHaptics
import com.techenglish.domain.model.KeywordTooltip

// ==================== 3. INTERACTIVE CODE BREAKDOWN SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CodeBreakdownScreen(
    onBack: () -> Unit,
    onOpenComments: (snippetId: Int, snippetTitle: String) -> Unit = { _, _ -> },
    viewModel: CodeBreakdownViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberTechHaptics()

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val title = state.snippet?.let {
                        "${state.language?.name ?: ""} • ${it.taskTitleEn}"
                    } ?: "Разбор кода"
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    state.snippet?.let { s ->
                        IconButton(onClick = { onOpenComments(s.id, s.taskTitleEn) }) {
                            Icon(Icons.Rounded.Forum, contentDescription = "Комментарии и мнемоники")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            SkeletonListPlaceholder(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val snippet = state.snippet
        val language = state.language
        if (snippet == null || language == null) {
            EmptyStateView(title = "Фрагмент кода не найден", subtitle = "Вернитесь назад.")
            return@Scaffold
        }

        val isDark = LocalTechInteractionConfig.current.isDarkTheme
        val surfaceBg = MaterialTheme.colorScheme.surface
        val langAccent = remember(language.accentColorHex, surfaceBg) {
            ensureAccessibleAccent(parseHexColor(language.accentColorHex), surfaceBg)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = navigationBarContentPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.showRussian,
                        onClick = { viewModel.setShowRussian(true) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Пояснения RU") }
                    SegmentedButton(
                        selected = !state.showRussian,
                        onClick = { viewModel.setShowRussian(false) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Explanations EN") }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(
                        text = language.name,
                        containerColor = langAccent.copy(alpha = 0.16f),
                        contentColor = langAccent
                    )
                    StatusBadge(text = snippet.taskKey)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (state.showRussian) snippet.explanationRu else snippet.explanationEn,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Syntax-highlighted interactive code block with line numbers & horizontal scrolling
            item {
                InteractiveCodeBlock(
                    code = snippet.code,
                    languageSlug = language.slug,
                    keywords = snippet.keywords,
                    selectedLine = state.selectedLine,
                    isDark = isDark,
                    onLineClick = viewModel::selectLine,
                    onKeywordClick = { kw ->
                        haptics.onWordTap()
                        viewModel.openKeyword(kw)
                    }
                )
            }

            // Interactive Keywords Bar
            if (snippet.keywords.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Интерактивные ключевые слова (нажмите для разбора и добавления в словарь):",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            snippet.keywords.forEach { kw ->
                                AssistChip(
                                    onClick = {
                                        haptics.onWordTap()
                                        viewModel.openKeyword(kw.keyword)
                                    },
                                    label = {
                                        Text(
                                            kw.keyword,
                                            style = MonoTagTextStyle,
                                            color = langAccent
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Line-by-line bilingual breakdown
            item {
                Text("Построчный разбор", style = MaterialTheme.typography.titleMedium)
            }

            items(snippet.lineExplanations, key = { it.line }) { lineExpl ->
                val isSelected = state.selectedLine == lineExpl.line
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectLine(lineExpl.line) },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "L${lineExpl.line}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = lineExpl.en,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = lineExpl.ru,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::markBreakdownStudied,
                    enabled = !state.isLoggedCompleted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isLoggedCompleted) "Разбор завершён (+10 XP)" else "Отметить разбор изученным (+10 XP)")
                }
            }
        }
    }

    // Keyword Popup BottomSheet
    state.selectedKeyword?.let { kw ->
        ModalBottomSheet(onDismissRequest = viewModel::dismissKeyword) {
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
                    Text(
                        text = kw.keyword,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { viewModel.speak(kw.keyword) }) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Озвучить")
                    }
                }
                Text(kw.ru, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(kw.en, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (kw.example.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = kw.example,
                            style = CodeTextStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
                Button(
                    onClick = {
                        viewModel.addKeywordToVocabulary(kw)
                        viewModel.dismissKeyword()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить термин в словарь SRS")
                }
            }
        }
    }
}

@Composable
fun InteractiveCodeBlock(
    code: String,
    languageSlug: String,
    keywords: List<KeywordTooltip>,
    selectedLine: Int?,
    isDark: Boolean,
    onLineClick: (Int) -> Unit,
    onKeywordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = remember(code) { code.lines() }
    val bgColor = if (isDark) TechColors.CodeBgDark else TechColors.CodeBgLight
    val gutterColor = if (isDark) TechColors.CodeGutterDark else TechColors.CodeGutterLight
    val borderColor = if (isDark) TechColors.CodeBorderDark else TechColors.CodeBorderLight
    val highlightRowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(bgColor)
            .border(1.dp, borderColor, MaterialTheme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                lines.forEachIndexed { idx, rawLine ->
                    val lineNum = idx + 1
                    val annotated = remember(rawLine, languageSlug, isDark, keywords) {
                        CodeSyntaxHighlighter.highlightLine(rawLine, languageSlug, isDark, keywords)
                    }
                    val latestOnLineClick by androidx.compose.runtime.rememberUpdatedState(onLineClick)
                    val latestOnKeywordClick by androidx.compose.runtime.rememberUpdatedState(onKeywordClick)
                    var layoutResult by remember(rawLine) { mutableStateOf<TextLayoutResult?>(null) }

                    Row(
                        modifier = Modifier
                            .background(if (selectedLine == lineNum) highlightRowColor else Color.Transparent)
                            .clickable { latestOnLineClick(lineNum) }
                            .padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .background(gutterColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = lineNum.toString(),
                                style = CodeTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = annotated,
                            style = CodeTextStyle,
                            softWrap = false,
                            onTextLayout = { layoutResult = it },
                            modifier = Modifier.pointerInput(annotated, lineNum) {
                                detectTapGestures { offset ->
                                    val pos = layoutResult?.getOffsetForPosition(offset) ?: return@detectTapGestures
                                    val kwHit = annotated.getStringAnnotations(
                                        tag = CodeSyntaxHighlighter.KEYWORD_ANNOTATION_TAG,
                                        start = pos,
                                        end = pos
                                    ).firstOrNull()
                                    if (kwHit != null) {
                                        latestOnKeywordClick(kwHit.item)
                                    } else {
                                        latestOnLineClick(lineNum)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

