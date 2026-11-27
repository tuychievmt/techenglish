package com.techenglish.app.feature.stage4a

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.app.feature.stage3.OfflineModeBanner
import com.techenglish.domain.model.CommentSortOrder
import com.techenglish.domain.model.CommunityComment
import com.techenglish.domain.model.CommunityDeck
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// =========================================================================
// 1. Comments & Mnemonics Screen (ФТ-13.1 — StackOverflow Format)
// =========================================================================

@Composable
fun CommentsMnemonicsScreen(
    onBack: () -> Unit,
    viewModel: CommentsMnemonicsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    CommentsMnemonicsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSelectSort = viewModel::setSortOrder,
        onRefresh = viewModel::refresh,
        onPostComment = viewModel::postComment,
        onVote = viewModel::voteComment,
        onReportAndBlock = viewModel::reportAndBlockComment
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsMnemonicsContent(
    state: CommentsMnemonicsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onSelectSort: (CommentSortOrder) -> Unit,
    onRefresh: () -> Unit,
    onPostComment: (String) -> Unit,
    onVote: (CommunityComment, Int) -> Unit,
    onReportAndBlock: (CommunityComment, String, Boolean) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var moderationTarget by remember { mutableStateOf<CommunityComment?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Комментарии и мнемоники", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${state.targetType.apiKey} • ${state.targetTitle}",
                            style = MonoTagTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                item {
                    OfflineModeBanner("Офлайн-режим: показаны кэшированные мнемоники из Room.")
                }
            }

            // Composer
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Поделиться мнемоникой или опытом с собеседования", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = { Text("Подсказка по запоминанию (5–1000 символов)") },
                            supportingText = {
                                val len = draft.trim().length
                                if (len in 0..4) Text("Минимум 5 символов ($len/5)")
                            },
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onPostComment(draft)
                                draft = ""
                            },
                            enabled = draft.trim().length >= 5 && !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Опубликовать комментарий")
                        }
                    }
                }
            }

            // Sort Toggle
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CommentSortOrder.entries.forEachIndexed { idx, order ->
                        SegmentedButton(
                            selected = state.sortOrder == order,
                            onClick = { onSelectSort(order) },
                            shape = SegmentedButtonDefaults.itemShape(idx, CommentSortOrder.entries.size)
                        ) {
                            Text(order.labelRu)
                        }
                    }
                }
            }

            if (state.comments.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(36.dp))
                            Text("Пока нет комментариев", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Напишите первую мнемонику или реальный пример употребления с собеседования!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.comments, key = { it.id }) { comment ->
                    CommentCard(
                        comment = comment,
                        onUpvote = { onVote(comment, 1) },
                        onDownvote = { onVote(comment, -1) },
                        onReport = { moderationTarget = comment }
                    )
                }
            }
        }
    }

    moderationTarget?.let { target ->
        UgcModerationDialog(
            title = "Пожаловаться на комментарий @${target.authorUsername}",
            onDismiss = { moderationTarget = null },
            onSubmit = { reason, blockAuthor ->
                onReportAndBlock(target, reason, blockAuthor)
                moderationTarget = null
            }
        )
    }
}

@Composable
private fun CommentCard(
    comment: CommunityComment,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onReport: () -> Unit
) {
    val dateLabel = Instant.ofEpochMilli(comment.createdAtMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // StackOverflow-style vertical vote column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onUpvote, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Голос за",
                        tint = if (comment.userVote == 1) TechColors.EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${comment.score}",
                    style = MonoTagTextStyle,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        comment.score > 0 -> TechColors.EmeraldDark
                        comment.score < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                IconButton(onClick = onDownvote, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Голос против",
                        tint = if (comment.userVote == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@${comment.authorUsername} • ${comment.authorRankTitle}",
                        style = MonoTagTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onReport, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Rounded.Flag,
                            contentDescription = "Пожаловаться на комментарий",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(text = comment.body, style = MaterialTheme.typography.bodyMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.ThumbUp, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${comment.upvotes}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.ThumbDown, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${comment.downvotes}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. Community Decks Screen (ФТ-13.2 + ФТ-9.4 UGC Moderation)
// =========================================================================

@Composable
fun CommunityDecksScreen(
    onBack: () -> Unit,
    viewModel: CommunityDecksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    CommunityDecksContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSearchChange = viewModel::setSearchQuery,
        onTagClick = viewModel::setTagFilter,
        onRefresh = viewModel::refreshFromServer,
        onDownloadDeck = viewModel::downloadDeckToRoom,
        onPublishDeck = viewModel::publishFromMyWords,
        onRateDeck = viewModel::rateDeck,
        onReportAndBlock = viewModel::reportAndBlockDeck
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommunityDecksContent(
    state: CommunityDecksUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onDownloadDeck: (CommunityDeck) -> Unit,
    onPublishDeck: (String, String, String) -> Unit,
    onRateDeck: (CommunityDeck, Int) -> Unit,
    onReportAndBlock: (CommunityDeck, String, Boolean) -> Unit
) {
    var showPublishDialog by remember { mutableStateOf(false) }
    var ratingTarget by remember { mutableStateOf<CommunityDeck?>(null) }
    var moderationTarget by remember { mutableStateOf<CommunityDeck?>(null) }
    val popularTags = listOf("backend", "android", "kotlin", "interview", "devops", "system-design")

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Колоды сообщества (Decks)") },
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
                item {
                    OfflineModeBanner("Офлайн-режим: скачивание кэшированных колод в локальный Room доступно полностью.")
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChange,
                            label = { Text("Поиск колод по названию или тегу") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            popularTags.forEach { tag ->
                                FilterChip(
                                    selected = state.selectedTag == tag,
                                    onClick = { onTagClick(tag) },
                                    label = { Text("#$tag", style = MonoTagTextStyle) }
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { showPublishDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Опубликовать колоду из «Мои слова» (${state.myCustomWordsCount})")
                        }
                    }
                }
            }

            items(state.decks, key = { it.id }) { deck ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(deck.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "@${deck.authorUsername} • ${deck.termCount} слов",
                                        style = MonoTagTextStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${deck.downloadsCount}",
                                        style = MonoTagTextStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TechColors.Amber.copy(alpha = 0.16f),
                                    modifier = Modifier.clickable { ratingTarget = deck }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Rounded.Star, contentDescription = "Оценить", tint = TechColors.AmberDark, modifier = Modifier.size(15.dp))
                                        Text("${"%.1f".format(deck.rating)}", style = MonoTagTextStyle, color = TechColors.AmberDark)
                                    }
                                }
                                IconButton(onClick = { moderationTarget = deck }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Flag, contentDescription = "Пожаловаться на колоду", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (deck.description.isNotBlank()) {
                            Text(deck.description, style = MaterialTheme.typography.bodySmall)
                        }

                        if (deck.tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                deck.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MonoTagTextStyle,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onDownloadDeck(deck) },
                                enabled = !state.isBusy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (deck.isDownloadedLocally) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (deck.isDownloadedLocally) "В Room (обновить)" else "Скачать в Room (1 тап)")
                            }
                            OutlinedButton(onClick = { ratingTarget = deck }) {
                                Text("Оценить")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPublishDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var tags by remember { mutableStateOf("backend, interview") }
        AlertDialog(
            onDismissRequest = { showPublishDialog = false },
            title = { Text("Публикация из «Мои слова»") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Будут опубликованы ваши пользовательские термины (${state.myCustomWordsCount} шт.) из категории «Мои слова».",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Название колоды") },
                        supportingText = if (title.trim().length < 4) {
                            { Text("Минимум 4 символа") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Краткое описание") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Теги через запятую") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPublishDeck(title, desc, tags)
                        showPublishDialog = false
                    },
                    enabled = title.trim().length >= 4
                ) { Text("Опубликовать") }
            },
            dismissButton = {
                TextButton(onClick = { showPublishDialog = false }) { Text("Отмена") }
            }
        )
    }

    ratingTarget?.let { target ->
        var selectedStars by remember { mutableIntStateOf(5) }
        AlertDialog(
            onDismissRequest = { ratingTarget = null },
            title = { Text("Оценить «${target.title}»") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (1..5).forEach { stars ->
                        FilterChip(
                            selected = selectedStars == stars,
                            onClick = { selectedStars = stars },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("$stars", style = MonoTagTextStyle)
                                    Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(13.dp))
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRateDeck(target, selectedStars)
                    ratingTarget = null
                }) { Text("Отправить") }
            },
            dismissButton = {
                TextButton(onClick = { ratingTarget = null }) { Text("Отмена") }
            }
        )
    }

    moderationTarget?.let { target ->
        UgcModerationDialog(
            title = "Пожаловаться на колоду «${target.title}»",
            onDismiss = { moderationTarget = null },
            onSubmit = { reason, blockAuthor ->
                onReportAndBlock(target, reason, blockAuthor)
                moderationTarget = null
            }
        )
    }
}

@Composable
private fun UgcModerationDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, blockAuthor: Boolean) -> Unit
) {
    var reason by remember { mutableStateOf("Спам или недостоверный контент") }
    var blockAuthor by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Контент будет скрыт у вас немедленно и отправлен в очередь модерации (ФТ-9.4).",
                    style = MaterialTheme.typography.bodySmall
                )
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
