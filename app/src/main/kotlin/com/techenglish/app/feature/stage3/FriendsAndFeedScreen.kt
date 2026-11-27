package com.techenglish.app.feature.stage3

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// =========================================================================
// 4. Friends, QR Invite, Activity Feed & Ping Screen (ФТ-12.1–12.3, Errata E2)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsAndFeedScreen(
    onBack: () -> Unit,
    viewModel: FriendsAndFeedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var friendQuery by remember { mutableStateOf("") }
    var moderationTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Друзья и лента активности") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.isOffline) {
                item { OfflineModeBanner("Офлайн-режим: показан кэш друзей и ленты активности.") }
            }

            // Add friend + QR Invite card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Добавить друга по @username", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = friendQuery,
                                onValueChange = { friendQuery = it },
                                label = { Text("@username друга") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    viewModel.addFriend(friendQuery)
                                    friendQuery = ""
                                },
                                enabled = friendQuery.isNotBlank() && !state.isBusy
                            ) {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = "Добавить")
                            }
                        }

                        HorizontalDivider()

                        // QR Invite Pattern & Share Deep-Link (ФТ-12.1)
                        val inviteLink = "techenglish://invite/@${state.currentUsername}"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DeterministicQrMatrix(seed = inviteLink, sizeDp = 64)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("QR-приглашение", style = MaterialTheme.typography.titleSmall)
                                }
                                Text(inviteLink, style = MonoTagTextStyle, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Добавляйся ко мне в TechEnglish: $inviteLink")
                                }
                                runCatching { context.startActivity(Intent.createChooser(intent, "Поделиться приглашением")) }
                            }) {
                                Icon(Icons.Rounded.Share, contentDescription = "Поделиться")
                            }
                        }

                        HorizontalDivider()

                        // Friend Ping Opt-out switch (ФТ-12.3)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Разрешить «Пнуть» (Ping) от друзей", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Не чаще 1 раза в 24 часа, можно отключить в любой момент",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.allowFriendPings,
                                onCheckedChange = viewModel::setAllowFriendPings
                            )
                        }
                    }
                }
            }

            // Friends list
            item {
                Text("Друзья (${state.friends.size})", style = MaterialTheme.typography.titleLarge)
            }
            if (state.friends.isEmpty()) {
                item {
                    Text(
                        text = "Список друзей пока пуст. Найдите коллег по @username или отправьте QR-ссылку.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.friends, key = { it.userId }) { friend ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                PresetAvatarBadge(avatarKey = friend.avatarKey, sizeDp = 40)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("@${friend.username}", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${friend.rankTitle} • Серия: ${friend.streakDays} дн.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalButton(
                                    onClick = { viewModel.pingFriend(friend) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Пнуть")
                                }
                                IconButton(onClick = { moderationTarget = friend.userId to friend.username }) {
                                    Icon(Icons.Rounded.Flag, contentDescription = "Пожаловаться / заблокировать")
                                }
                            }
                        }
                    }
                }
            }

            // Friends Activity Feed (ФТ-12.2)
            item {
                Text("Лента активности друзей", style = MaterialTheme.typography.titleLarge)
            }
            if (state.feed.isEmpty()) {
                item {
                    Text(
                        text = "События друзей (достижения, серии, закрытые модули) появятся здесь после синхронизации.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.feed, key = { it.id }) { item ->
                    val timeLabel = Instant.ofEpochMilli(item.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("@${item.actorUsername} • $timeLabel", style = MonoTagTextStyle, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text(item.summaryRu, style = MaterialTheme.typography.bodyMedium)
                            }
                            FilledTonalButton(
                                onClick = { viewModel.reactToFeedItem(item.id) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.ThumbUp, contentDescription = "Реакция", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${item.reactionCount}")
                            }
                        }
                    }
                }
            }
        }
    }

    moderationTarget?.let { (targetId, targetUsername) ->
        var reason by remember { mutableStateOf("Неприемлемый ник или спам") }
        AlertDialog(
            onDismissRequest = { moderationTarget = null },
            title = { Text("Модерация: @$targetUsername") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Вы можете отправить жалобу и заблокировать пользователя (Errata E2).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Причина жалобы") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reportAndBlockUser(targetId, targetUsername, reason)
                        moderationTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Заблокировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { moderationTarget = null }) { Text("Отмена") }
            }
        )
    }
}

