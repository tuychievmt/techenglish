package com.techenglish.app.feature.stage3

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.SystemUpdateAlt
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.domain.model.AuthProvider

// =========================================================================
// 1. Cloud Account, Outbox Sync, Guest Link & OTA Screen
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudAccountSyncScreen(
    onBack: () -> Unit,
    viewModel: CloudAccountSyncViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аккаунт, синхронизация и OTA") },
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
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.isOffline) {
                item { OfflineModeBanner("Офлайн-режим: обучение доступно полностью, события сохраняются в sync_queue.") }
            }

            // Account status card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (state.session.isGuest) Icons.Rounded.AccountCircle else Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = if (state.session.isGuest) MaterialTheme.colorScheme.secondary else TechColors.EmeraldDark,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = if (state.session.isGuest) "Гостевой офлайн-режим" else "@${state.session.username}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (state.session.isGuest) {
                                        "100% локального контента открыто без регистрации"
                                    } else {
                                        "Провайдер: ${state.session.provider.labelRu} • ${state.session.email}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "В очереди sync_queue: ${state.pendingSyncEvents}",
                                style = MonoTagTextStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FilledTonalButton(
                                onClick = { viewModel.syncNow() },
                                enabled = !state.isBusy
                            ) {
                                Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Синхронизировать")
                            }
                        }

                        if (!state.session.isGuest) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { viewModel.linkGuestAccount() },
                                    enabled = !state.isBusy,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Привязать гостя")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.signOut() },
                                    enabled = !state.isBusy,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Выйти")
                                }
                            }
                        }
                    }
                }
            }

            // Auth form when in guest mode
            if (state.session.isGuest) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Вход или регистрация (16+)", style = MaterialTheme.typography.titleMedium)
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = !isRegisterMode,
                                    onClick = { isRegisterMode = false },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                                ) { Text("Вход") }
                                SegmentedButton(
                                    selected = isRegisterMode,
                                    onClick = { isRegisterMode = true },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                                ) { Text("Регистрация") }
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Пароль (минимум 8 символов)") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (isRegisterMode) {
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("@username (a-z, 0-9, _)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { displayName = it },
                                    label = { Text("Отображаемое имя") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = {
                                    if (isRegisterMode) {
                                        viewModel.registerWithEmail(email, password, username, displayName)
                                    } else {
                                        viewModel.loginWithEmail(email, password)
                                    }
                                },
                                enabled = !state.isBusy && email.isNotBlank() && password.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isRegisterMode) "Создать аккаунт и синхронизировать" else "Войти по Email")
                            }

                            HorizontalDivider()
                            Text(
                                text = "Быстрый вход OAuth 2.0 (PKCE S256):",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.startOAuthLogin(context, AuthProvider.GITHUB) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("GitHub PKCE") }
                                OutlinedButton(
                                    onClick = { viewModel.startOAuthLogin(context, AuthProvider.GOOGLE) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Google PKCE") }
                            }
                        }
                    }
                }
            }

            // OTA Data-Only Content Delivery (ФТ-14.5)
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("OTA-пакеты контента (только данные)", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Словарь и статьи обновляются инкрементально по HTTPS с проверкой SHA-256 через ContentPackImporter (без исполняемого кода).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.appliedOtaPacks.isEmpty()) {
                            Text(
                                text = "Установленные OTA-пакеты: встроенная офлайн-база v6 (20 658+ терминов).",
                                style = MonoTagTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.appliedOtaPacks.forEach { pack ->
                                val statusTag = if (pack.lastStatus == "FAILED") "ОШИБКА" else "OK"
                                Text(
                                    text = "• [$statusTag] ${pack.titleRu} v${pack.version} (${pack.itemCount} зап.) • SHA256:${pack.sha256Hex.take(10).ifBlank { "—" }}…",
                                    style = MonoTagTextStyle,
                                    color = if (pack.lastStatus == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                if (pack.lastError.isNotBlank()) {
                                    Text(
                                        text = "  Причина: ${pack.lastError}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        FilledTonalButton(
                            onClick = { viewModel.checkOtaPacks() },
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Проверить обновления контента (OTA)")
                        }
                    }
                }
            }

            // Server endpoint, Privacy Policy & Account Deletion (ФТ-9.4)
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Приватность и управление данными (ФТ-9.4)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Backend API: ${state.apiBaseUrl}",
                            style = MonoTagTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showServerDialog = true },
                                modifier = Modifier.weight(1f)
                            ) { Text("Сервер API") }
                            OutlinedButton(
                                onClick = { showPrivacyDialog = true },
                                modifier = Modifier.weight(1f)
                            ) { Text("Политика") }
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Удалить облачный аккаунт и данные")
                        }
                    }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить облачный аккаунт?") },
            text = {
                Text("Облачный профиль, события на сервере и токены будут удалены без возможности восстановления. Локальный офлайн-словарь на устройстве останется доступен.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccountAndCloudData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showServerDialog) {
        var urlInput by remember { mutableStateOf(state.apiBaseUrl) }
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Адрес FastAPI-сервера") },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = {
                        Text(
                            if (com.techenglish.app.BuildConfig.DEBUG) {
                                "HTTPS URL или http://10.0.2.2:8000"
                            } else {
                                "HTTPS URL сервера"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateApiBaseUrl(urlInput)
                    showServerDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) { Text("Отмена") }
            }
        )
    }
}

