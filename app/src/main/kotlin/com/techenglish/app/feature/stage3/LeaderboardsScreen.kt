package com.techenglish.app.feature.stage3

import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.database.entity.LeaderboardCacheEntity
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.domain.model.LeaderboardTab

// =========================================================================
// 3. Top-10 Segmented Leaderboards & Weekly Leagues (ФТ-11.1–11.2, НФТ-7.1)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardsScreen(
    onBack: () -> Unit,
    viewModel: LeaderboardsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    LeaderboardsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSelectTab = viewModel::selectTab,
        onRefresh = viewModel::refresh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardsContent(
    state: LeaderboardsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onSelectTab: (LeaderboardTab) -> Unit,
    onRefresh: () -> Unit
) {
    val top10 = state.entries.filter { it.rank <= 10 }
    val pinnedCurrentUser = state.entries.firstOrNull { it.isCurrentUser && it.rank > 10 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Лидерборды Top-10 и Лиги") },
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
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isOffline) {
                item {
                    OfflineModeBanner("Офлайн-режим: показан кэш лидерборда из локальной БД.")
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    LeaderboardTab.entries.forEachIndexed { idx, tab ->
                        SegmentedButton(
                            selected = state.selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            shape = SegmentedButtonDefaults.itemShape(idx, LeaderboardTab.entries.size)
                        ) {
                            Text(tab.labelRu, maxLines = 1)
                        }
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = TechColors.AmberDark)
                        Text(
                            text = "Сброс лиг: понедельник 00:00 Asia/Dushanbe (старт недели: ${state.weekStartUtc5Label}). Топ-3 ↑, последние 3 ↓.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (top10.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(36.dp))
                            Text("Локальный кэш вкладки пуст", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Войдите в облачный аккаунт и выполните синхронизацию, чтобы загрузить Top-10 участников.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(top10, key = { "${it.tab}_${it.userId}" }) { row ->
                    LeaderboardEntryRow(row)
                }
            }

            if (pinnedCurrentUser != null) {
                item {
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text("Ваша позиция:", style = MaterialTheme.typography.labelLarge)
                    LeaderboardEntryRow(pinnedCurrentUser)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardEntryRow(entry: LeaderboardCacheEntity) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = if (entry.isCurrentUser) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${entry.rank}",
                    style = MonoTagTextStyle,
                    fontWeight = FontWeight.Bold,
                    color = when (entry.rank) {
                        1 -> TechColors.AmberDark
                        2 -> MaterialTheme.colorScheme.primary
                        3 -> TechColors.EmeraldDark
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.width(34.dp)
                )
                PresetAvatarBadge(avatarKey = entry.avatarKey, sizeDp = 34)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "@${entry.username}${if (entry.isCurrentUser) " (вы)" else ""}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${entry.rankTitle} • ${entry.league}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${entry.weeklyXp} XP",
                style = MonoTagTextStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

