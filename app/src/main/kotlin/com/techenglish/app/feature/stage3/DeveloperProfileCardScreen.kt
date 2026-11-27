package com.techenglish.app.feature.stage3

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.domain.usecase.ValidateSocialActionsUseCase

// =========================================================================
// 2. Developer Profile Card & Badges Screen (ФТ-10.1–10.3, Errata E2)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeveloperProfileCardScreen(
    onBack: () -> Unit,
    viewModel: DeveloperCardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val existing = state.profile
    var draftInitialized by remember { mutableStateOf(false) }
    val resolvedDefaultUsername = existing?.username
        ?: state.session.username.takeIf { !state.session.isGuest && it.isNotBlank() }
        ?: state.settings.localUsername.ifBlank { state.session.username }
    var username by remember { mutableStateOf(resolvedDefaultUsername) }
    var displayName by remember { mutableStateOf(existing?.displayName ?: state.settings.userName) }
    var avatarKey by remember { mutableStateOf(existing?.avatarKey ?: "preset_terminal") }
    var statusText by remember { mutableStateOf(existing?.statusText ?: "Building offline-first software") }
    var techStack by remember { mutableStateOf(existing?.techStackCsv ?: "Kotlin, Python, SQL") }
    var githubUrl by remember { mutableStateOf(existing?.githubUrl ?: "") }
    var telegramHandle by remember { mutableStateOf(existing?.telegramHandle ?: "") }

    LaunchedEffect(existing, state.session.username, state.settings.localUsername, state.settings.userName) {
        if (!draftInitialized) {
            if (existing != null) {
                username = existing.username
                displayName = existing.displayName
                avatarKey = existing.avatarKey
                statusText = existing.statusText
                techStack = existing.techStackCsv
                githubUrl = existing.githubUrl
                telegramHandle = existing.telegramHandle
                draftInitialized = true
            } else if (!state.session.isGuest || state.settings.localUsername.isNotBlank() || state.settings.userName != "Разработчик") {
                username = resolvedDefaultUsername
                displayName = state.settings.userName
                draftInitialized = true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Карточка разработчика и бейджи") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Public Developer Card Preview
            item {
                val level = state.dailyProgress?.levelProgress
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PresetAvatarBadge(avatarKey = avatarKey, sizeDp = 56)
                            Column {
                                Text(displayName, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "@$username • ${level?.rankTitleRu ?: "Intern Developer"}",
                                    style = MonoTagTextStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${level?.totalXp ?: 0} XP • Серия ${state.dailyProgress?.streakDays ?: 0} дн.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (statusText.isNotBlank()) {
                            Text(statusText, style = MaterialTheme.typography.bodyMedium)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            techStack.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = tag,
                                        style = MonoTagTextStyle,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Preset Avatars (Errata E2: no binary image upload)
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Редактирование публичной карточки", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Аватар (Errata E2 — безопасные пресеты разработчика без загрузки произвольных фото):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ValidateSocialActionsUseCase.PRESET_AVATARS.forEach { preset ->
                                val selected = avatarKey == preset
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.clickable { avatarKey = preset }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PresetAvatarBadge(avatarKey = preset, sizeDp = 24)
                                        Text(preset.removePrefix("preset_"), style = MonoTagTextStyle)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("@username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Имя разработчика") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = statusText,
                            onValueChange = { statusText = it },
                            label = { Text("Статус / специализация") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = techStack,
                            onValueChange = { techStack = it },
                            label = { Text("Стек технологий (через запятую)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = githubUrl,
                            onValueChange = { githubUrl = it },
                            label = { Text("GitHub профиль (опционально)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = telegramHandle,
                            onValueChange = { telegramHandle = it },
                            label = { Text("Telegram @handle (опционально)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.saveDeveloperProfile(
                                    username = username,
                                    displayName = displayName,
                                    avatarKey = avatarKey,
                                    statusText = statusText,
                                    techStackCsv = techStack,
                                    githubUrl = githubUrl,
                                    telegramHandle = telegramHandle
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Сохранить карточку разработчика")
                        }
                    }
                }
            }

            // Badges Showcase (ФТ-10.3)
            item {
                Text("Витрина бейджей", style = MaterialTheme.typography.titleLarge)
            }
            items(state.badges, key = { it.id }) { badge ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (badge.isUnlocked) TechColors.Emerald.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = badgeIconFor(badge.iconKey),
                                    contentDescription = null,
                                    tint = if (badge.isUnlocked) TechColors.EmeraldDark
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(badge.titleRu, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = badge.descriptionRu,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (badge.isUnlocked) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Открыто", tint = TechColors.EmeraldDark)
                        }
                    }
                }
            }
        }
    }
}

