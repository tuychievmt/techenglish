package com.techenglish.app.feature.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.audio.TtsVoiceStatus
import com.techenglish.app.core.designsystem.GitHubContributionGraph
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TtsVoiceStatusBanner
import com.techenglish.app.core.designsystem.resolveColorSchemeForMode
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TranslationLanguage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onOpenStatistics: () -> Unit,
    onOpenCloudSync: () -> Unit = {},
    onOpenDeveloperCard: () -> Unit = {},
    onOpenCommunityArenaHub: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNameDialog by remember { mutableStateOf(false) }
    var showAboutLicensesDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Профиль и настройки", style = MaterialTheme.typography.headlineMedium)
            }

            // Section 1: Developer Profile, Contribution Graph & Statistics
            item {
                val daily = state.dailyProgress
                val level = daily?.levelProgress
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Person,
                                            contentDescription = "Аватар профиля",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(state.settings.userName, style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = if (level != null) "Ур. ${level.level} • ${level.rankTitleRu}" else "Загрузка...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { showNameDialog = true }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Изменить имя")
                                }
                            }
                            if (level != null) {
                                LinearProgressIndicator(
                                    progress = { level.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                )
                                Text(
                                    text = "${level.totalXp} XP всего • Серия: ${daily.streakDays} дн.",
                                    style = MonoTagTextStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenStatistics)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Детальная статистика", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "Недельные, месячные и годовые графики",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Открыть статистику")
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenDeveloperCard)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Карточка разработчика и бейджи", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "@username, стек, пресеты аватаров и достижения",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                            }
                        }
                    }

                    GitHubContributionGraph(
                        yearlyActivity = state.dailyProgress?.yearlyActivity.orEmpty()
                    )
                }
            }

            // Section 2: Cloud Sync & Community / Arena Hub (SP2-06 / BUG-21)
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Облако и Сообщество (16+)", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenCloudSync)
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Аккаунт, синхронизация и OTA", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Гостевой режим, OAuth, outbox sync_queue, OTA-пакеты",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenCommunityArenaHub)
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Groups, contentDescription = null, tint = TechColors.Indigo)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Сообщество и Арена", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "AI-ментор, PvP-дуэли, Peer Review, комнаты, лидерборды, друзья, Squads, PR",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            // Section 3: Learning Preferences, IDE Themes & Accessibility
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Обучение, темы и доступность", style = MaterialTheme.typography.titleMedium)
                        }

                        Text("Ежедневная цель (карточек в день)", style = MaterialTheme.typography.labelLarge)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val options = listOf(10, 20, 30)
                            options.forEachIndexed { idx, goal ->
                                SegmentedButton(
                                    selected = state.settings.dailyGoal == goal,
                                    onClick = { viewModel.setDailyGoal(goal) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, options.size)
                                ) {
                                    Text("$goal", style = MonoTagTextStyle)
                                }
                            }
                        }

                        Text("Язык перевода терминов (RU / TJ)", style = MaterialTheme.typography.labelLarge)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val langs = TranslationLanguage.entries
                            langs.forEachIndexed { idx, lang ->
                                SegmentedButton(
                                    selected = state.settings.translationLanguage == lang,
                                    onClick = { viewModel.setTranslationLanguage(lang) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, langs.size)
                                ) {
                                    Text(lang.labelRu, maxLines = 1)
                                }
                            }
                        }
                        Text(
                            text = "Если верифицированный перевод на таджикский язык ещё отсутствует, автоматически отображается русский перевод.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        val baseModes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            baseModes.forEachIndexed { idx, mode ->
                                SegmentedButton(
                                    selected = state.settings.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, baseModes.size)
                                ) {
                                    Text(mode.labelRu, maxLines = 1)
                                }
                            }
                        }

                        Text(
                            text = "Коллекция культовых IDE-тем (WCAG AA ≥ 4.5:1)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        val ideThemes = listOf(
                            ThemeMode.TOKYO_NIGHT,
                            ThemeMode.DRACULA,
                            ThemeMode.GITHUB_DARK,
                            ThemeMode.NORD,
                            ThemeMode.GRUVBOX
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ideThemes.forEach { ideMode ->
                                val previewScheme = resolveColorSchemeForMode(ideMode, systemDark = true)
                                val isSelected = state.settings.themeMode == ideMode
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = previewScheme.surface,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) previewScheme.primary else previewScheme.outlineVariant
                                    ),
                                    modifier = Modifier.clickable { viewModel.setThemeMode(ideMode) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(previewScheme.primary)
                                        )
                                        Text(
                                            text = ideMode.labelRu,
                                            style = MonoTagTextStyle,
                                            color = previewScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dynamic Colors (Material You)", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "Адаптировать палитру под обои Android 12+",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = state.settings.dynamicColors,
                                    onCheckedChange = viewModel::setDynamicColors
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Тактильный отклик (Haptic Feedback)", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Лёгкая вибрация при перевороте карточки, свайпе и верном ответе",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.settings.hapticFeedbackEnabled,
                                onCheckedChange = viewModel::setHapticFeedbackEnabled
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Убрать анимации (Reduce Motion)", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Отключить мигание курсора '_', свечение комбо и пульсацию скелетонов",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.settings.reduceMotionEnabled,
                                onCheckedChange = viewModel::setReduceMotionEnabled
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Размер текста документации", style = MaterialTheme.typography.bodyLarge)
                                Text("${state.settings.readingFontSizeSp.toInt()} sp", style = MonoTagTextStyle)
                            }
                            Slider(
                                value = state.settings.readingFontSizeSp,
                                onValueChange = viewModel::setReadingFontSize,
                                valueRange = 13f..24f,
                                steps = 10
                            )
                        }
                    }
                }
            }

            // Section 4: Local Notifications & Offline TTS Diagnostics
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Напоминания о повторении", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "WorkManager проверяет наступившие карточки SM-2 локально",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = state.settings.notificationsEnabled,
                                onCheckedChange = { enable ->
                                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotificationPermission) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setNotificationsEnabled(enable)
                                    }
                                }
                            )
                        }

                        Text("Время напоминания:", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(9, 13, 19, 21).forEach { hour ->
                                FilterChip(
                                    selected = state.settings.reminderHour == hour,
                                    onClick = { viewModel.setReminderHour(hour) },
                                    label = { Text("${"%02d:00".format(hour)}", style = MonoTagTextStyle) }
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotificationPermission) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.sendTestNotification()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Проверить локальное уведомление сейчас")
                        }

                        HorizontalDivider()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Text("Синтез речи (TTS) и офлайн-режим", style = MaterialTheme.typography.titleSmall)
                        }
                        val statusLabel = when (val s = voiceStatus) {
                            is TtsVoiceStatus.ReadyOffline -> "Готов офлайн ✓ (${s.voiceName})"
                            is TtsVoiceStatus.ReadyOnlineOnly -> "Английский голос найден (установите офлайн-пакет для работы без сети)"
                            is TtsVoiceStatus.Initializing -> "Инициализация движка TTS..."
                            is TtsVoiceStatus.MissingVoiceData -> "Офлайн-пакет en-US отсутствует"
                            is TtsVoiceStatus.Error -> s.message
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (voiceStatus is TtsVoiceStatus.ReadyOffline) TechColors.EmeraldDark
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TtsVoiceStatusBanner(
                            voiceStatus = voiceStatus,
                            installIntentProvider = { viewModel.audioController.buildInstallTtsVoiceIntent() },
                            onRecheck = { viewModel.audioController.recheckVoiceStatus() }
                        )
                    }
                }
            }

            // Section 5: About App & Embedded Font Licenses (SIL OFL 1.1)
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutLicensesDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("О приложении и лицензии (SIL OFL)", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "TechEnglish • Шрифты Inter и JetBrains Mono (SIL Open Font License 1.1)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Открыть сведения о лицензиях")
                    }
                }
            }
        }
    }

    if (showNameDialog) {
        ProfileNameDialog(
            initialName = state.settings.userName,
            onSave = viewModel::setUserName,
            onDismiss = { showNameDialog = false }
        )
    }

    if (showAboutLicensesDialog) {
        ProfileAboutLicensesDialog(onDismiss = { showAboutLicensesDialog = false })
    }
}
