package com.techenglish.app.feature.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.audio.TtsVoiceStatus
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoMetricTextStyle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SegmentedPillToggle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TtsVoiceStatusBanner
import com.techenglish.app.core.designsystem.resolveColorSchemeForMode
import com.techenglish.domain.model.DayActivity
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.UserStats

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
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    var showNameDialog by remember { mutableStateOf(false) }
    var showAboutLicensesDialog by remember { mutableStateOf(false) }
    var analyticsPeriodIdx by remember { mutableIntStateOf(2) } // 0=7Д, 1=30Д, 2=365Д

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setNotificationsEnabled(true)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    AuroraScreenContainer {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Developer Identity Hero Card
                item {
                    val daily = state.dailyProgress
                    val level = daily?.levelProgress
                    val xpPct = ((level?.progressFraction ?: 0f) * 100).toInt()
                    val heroBg = if (isDark) {
                        Brush.linearGradient(listOf(Color(0xFF1E2A4A), Color(0xFF292042), Color(0xFF111522)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF6F9FF)))
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(heroBg)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.Person,
                                            contentDescription = "Аватар профиля",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = state.settings.userName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.size(28.dp).clickable { showNameDialog = true }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "Изменить имя", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (level != null) "Ур. ${level.level} · ${level.rankTitleRu}" else "Загрузка...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (level != null && daily != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(level.progressFraction.coerceIn(0.05f, 1f))
                                                .fillMaxHeight()
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                                                            if (isDark) TechColors.Emerald else TechColors.EmeraldDark
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${level.totalXp} XP · Серия ${daily.streakDays} дн.",
                                            style = MonoTagTextStyle,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$xpPct% до Ур. ${level.level + 1}",
                                            style = MonoTagTextStyle,
                                            color = if (isDark) TechColors.Emerald else TechColors.EmeraldDark
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileQuickActionTile(
                                    icon = Icons.Rounded.Badge,
                                    tint = if (isDark) TechColors.Violet else MaterialTheme.colorScheme.secondary,
                                    label = state.displayUsername?.let { "Карточка и бейджи (@$it)" } ?: "Карточка и бейджи (Локальный)",
                                    onClick = onOpenDeveloperCard,
                                    modifier = Modifier.weight(1f)
                                )
                                ProfileQuickActionTile(
                                    icon = Icons.Rounded.CloudSync,
                                    tint = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                                    label = "Облако и Sync (OAuth · OTA)",
                                    onClick = onOpenCloudSync,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileQuickActionTile(
                                    icon = Icons.Rounded.Groups,
                                    tint = MaterialTheme.colorScheme.primary,
                                    label = "Сообщество и Арена",
                                    onClick = onOpenCommunityArenaHub,
                                    modifier = Modifier.weight(1f)
                                )
                                ProfileQuickActionTile(
                                    icon = Icons.Rounded.BarChart,
                                    tint = if (isDark) TechColors.Emerald else TechColors.EmeraldDark,
                                    label = "Подробная статистика",
                                    onClick = onOpenStatistics,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 2. Unified Analytics Bento Section (2x2 KPI + Single Activity Card with 7Д | 30Д | 365Д)
                item {
                    ProfileAnalyticsSection(
                        stats = state.userStats,
                        streakFallback = state.dailyProgress?.streakDays ?: 0,
                        yearlyFallback = state.dailyProgress?.yearlyActivity.orEmpty(),
                        selectedPeriod = analyticsPeriodIdx,
                        onSelectPeriod = { analyticsPeriodIdx = it }
                    )
                }
                // 3. Group A: IDE-темы и оформление
                item {
                    BentoGlassCard {
                        Column(
                            modifier = Modifier.padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("IDE-темы и оформление", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                            val baseModes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                            val baseSelectedIdx = baseModes.indexOf(state.settings.themeMode).coerceAtLeast(-1)
                            SegmentedPillToggle(
                                options = baseModes.map { it.labelRu },
                                selectedIndex = baseSelectedIdx,
                                onSelect = { idx -> viewModel.setThemeMode(baseModes[idx]) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            val ideThemes = listOf(
                                ThemeMode.TOKYO_NIGHT,
                                ThemeMode.DRACULA,
                                ThemeMode.GITHUB_DARK,
                                ThemeMode.NORD,
                                ThemeMode.GRUVBOX
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ideThemes.forEach { ideMode ->
                                    val preview = resolveColorSchemeForMode(ideMode, systemDark = true)
                                    val selected = state.settings.themeMode == ideMode
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.clickable { viewModel.setThemeMode(ideMode) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(preview.primary)
                                            )
                                            Text(
                                                text = ideMode.labelRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
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
                                        Text("Dynamic Colors (Material You)", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "Палитра из обоев Android 12+",
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
                        }
                    }
                }

                // 4. Group B: Обучение и доступность
                item {
                    BentoGlassCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Обучение и доступность", style = MaterialTheme.typography.titleMedium)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Дневная цель", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(10, 20, 30).forEach { goal ->
                                        val selected = state.settings.dailyGoal == goal
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.clickable { viewModel.setDailyGoal(goal) }
                                        ) {
                                            Text(
                                                text = "$goal",
                                                style = MonoTagTextStyle,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Язык перевода", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TranslationLanguage.entries.forEach { lang ->
                                        val selected = state.settings.translationLanguage == lang
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.clickable { viewModel.setTranslationLanguage(lang) }
                                        ) {
                                            Text(
                                                text = lang.labelRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Тактильный отклик (Haptics)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
                                Text("Убрать анимации (Reduce Motion)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
                                    Text("Размер текста документации", style = MaterialTheme.typography.bodyMedium)
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

                // 5. Notifications, Offline TTS & About/SIL OFL Licenses
                item {
                    BentoGlassCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Напоминания о повторении", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Локальная проверка SM-2 через WorkManager",
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

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text("Синтез речи (TTS) и офлайн-режим", style = MaterialTheme.typography.titleSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (voiceStatus is TtsVoiceStatus.ReadyOffline) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isDark) TechColors.Emerald else TechColors.EmeraldDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                val statusLabel = when (val s = voiceStatus) {
                                    is TtsVoiceStatus.ReadyOffline -> "Готов офлайн (${s.voiceName})"
                                    is TtsVoiceStatus.ReadyOnlineOnly -> "Английский голос найден (установите офлайн-пакет)"
                                    is TtsVoiceStatus.Initializing -> "Инициализация движка TTS..."
                                    is TtsVoiceStatus.MissingVoiceData -> "Офлайн-пакет en-US отсутствует"
                                    is TtsVoiceStatus.Error -> s.message
                                }
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (voiceStatus is TtsVoiceStatus.ReadyOffline) {
                                        if (isDark) TechColors.Emerald else TechColors.EmeraldDark
                                    } else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TtsVoiceStatusBanner(
                                voiceStatus = voiceStatus,
                                installIntentProvider = { viewModel.audioController.buildInstallTtsVoiceIntent() },
                                onRecheck = { viewModel.audioController.recheckVoiceStatus() }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAboutLicensesDialog = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("О приложении и лицензии (SIL OFL)", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Inter & JetBrains Mono (SIL OFL 1.1)",
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

@Composable
private fun ProfileQuickActionTile(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileAnalyticsSection(
    stats: UserStats?,
    streakFallback: Int,
    yearlyFallback: List<DayActivity>,
    selectedPeriod: Int,
    onSelectPeriod: (Int) -> Unit
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val learnedStr = if (stats != null) "${stats.learnedWords} / ${stats.totalWords}" else "—"
    val accuracyStr = if (stats != null) "${stats.accuracyPercent}%" else "—"
    val streakStr = "${stats?.streakDays ?: streakFallback} дн."
    val reviewedStr = if (stats != null) "${stats.reviewedTotal}" else "—"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileKpiTile(
                label = "Изучено слов",
                value = learnedStr,
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                tint = MaterialTheme.colorScheme.primary,
                badgeBg = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            ProfileKpiTile(
                label = "Точность",
                value = accuracyStr,
                icon = Icons.Rounded.CheckCircle,
                tint = if (isDark) TechColors.Emerald else TechColors.EmeraldDark,
                badgeBg = if (isDark) TechColors.EmeraldContainerDark else TechColors.EmeraldContainerLight,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileKpiTile(
                label = "Серия",
                value = streakStr,
                icon = Icons.Rounded.LocalFireDepartment,
                tint = if (isDark) TechColors.Amber else TechColors.AmberDark,
                badgeBg = if (isDark) TechColors.AmberContainerDark else TechColors.AmberContainerLight,
                modifier = Modifier.weight(1f)
            )
            ProfileKpiTile(
                label = "Повторений",
                value = reviewedStr,
                icon = Icons.Rounded.Bolt,
                tint = if (isDark) TechColors.Cyan else TechColors.CyanDark,
                badgeBg = if (isDark) TechColors.CyanContainerDark else TechColors.CyanContainerLight,
                modifier = Modifier.weight(1f)
            )
        }

        val yearly = stats?.yearlyActivity ?: yearlyFallback
        val weekly = stats?.weeklyActivity ?: yearly.takeLast(7)
        val monthly = stats?.monthlyActivity ?: yearly.takeLast(30)

        BentoGlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Активность обучения", style = MaterialTheme.typography.titleMedium)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf("7Д", "30Д", "365Д").forEachIndexed { idx, label ->
                                val active = selectedPeriod == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { onSelectPeriod(idx) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MonoTagTextStyle,
                                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                when (selectedPeriod) {
                    0 -> UnifiedWeeklyBars(weekly)
                    1 -> UnifiedHeatmapCells(monthly.takeLast(30), columns = 10)
                    else -> UnifiedHeatmapCells(yearly.takeLast(48), columns = 12)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Меньше", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    listOf(0, 1, 2, 4).forEach { lvl ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(11.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(heatmapCellColor(lvl, isDark))
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Больше", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ProfileKpiTile(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    badgeBg: Color,
    modifier: Modifier = Modifier
) {
    BentoGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = RoundedCornerShape(8.dp), color = badgeBg, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(value, style = MonoMetricTextStyle, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun UnifiedWeeklyBars(days: List<DayActivity>) {
    val maxCount = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(96.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val ratio = (day.count.toFloat() / maxCount.toFloat()).coerceIn(0.10f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.50f)
                        .height((62f * ratio).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (day.count > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(day.dayLabel.take(3), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun UnifiedHeatmapCells(days: List<DayActivity>, columns: Int) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val padded = if (days.isEmpty()) List(columns * 4) { DayActivity(0L, "", 0, 0) } else days
    val rows = padded.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.forEach { rowDays ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                rowDays.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.4f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(heatmapCellColor(day.intensityLevel, isDark))
                    )
                }
                repeat((columns - rowDays.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun heatmapCellColor(level: Int, isDark: Boolean): Color = when (level) {
    0 -> if (isDark) Color(0xFF171C2D) else Color(0xFFE2E8F0)
    1 -> if (isDark) Color(0xFF064E3B) else Color(0xFFA7F3D0)
    2 -> if (isDark) Color(0xFF10B981) else Color(0xFF34D399)
    3 -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    else -> if (isDark) Color(0xFF34D399) else Color(0xFF047857)
}
