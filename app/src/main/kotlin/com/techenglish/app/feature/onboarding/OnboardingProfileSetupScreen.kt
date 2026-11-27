package com.techenglish.app.feature.onboarding

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SegmentedPillToggle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.resolveColorSchemeForMode
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TranslationLanguage

@Composable
fun OnboardingProfileSetupScreen(
    onBack: () -> Unit,
    onSetupCompleted: () -> Unit,
    viewModel: OnboardingProfileSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { evt ->
            when (evt) {
                OnboardingSetupNavEvent.NavigateToHome -> onSetupCompleted()
            }
        }
    }

    OnboardingProfileSetupContent(
        state = state,
        onBack = onBack,
        onUsernameChanged = viewModel::onUsernameChanged,
        onDisplayNameChanged = viewModel::onDisplayNameChanged,
        onRoleSelected = viewModel::onRoleSelected,
        onDailyGoalSelected = viewModel::onDailyGoalSelected,
        onTranslationLanguageSelected = viewModel::onTranslationLanguageSelected,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onCompleteClick = viewModel::completeSetup
    )
}

@Composable
fun OnboardingProfileSetupContent(
    state: OnboardingProfileSetupUiState,
    onBack: () -> Unit,
    onUsernameChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onRoleSelected: (DeveloperRoleLevel) -> Unit,
    onDailyGoalSelected: (Int) -> Unit,
    onTranslationLanguageSelected: (TranslationLanguage) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onCompleteClick: () -> Unit
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme

    AuroraScreenContainer {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header + Step 2/2 badge
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(40.dp).clickable(onClick = onBack)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад", modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text("Настройка профиля", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    text = if (state.isAuthenticated) "Облачный аккаунт подключён" else "Локальный профиль (Offline-First)",
                                    style = MonoTagTextStyle,
                                    color = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "ШАГ 2 / 2",
                                style = MonoTagTextStyle,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                state.errorMessage?.let { err ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                // 1. @username, Display Name & Developer Level
                item {
                    BentoGlassCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Профиль разработчика", style = MaterialTheme.typography.titleMedium)

                            OutlinedTextField(
                                value = state.username,
                                onValueChange = onUsernameChanged,
                                label = { Text("@username") },
                                placeholder = { Text("kotlin_dev") },
                                prefix = { Text("@", style = MonoTagTextStyle, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null) },
                                isError = state.usernameError != null,
                                supportingText = {
                                    Text(
                                        text = state.usernameError ?: "3–20 символов: латиница (a-z), цифры или _",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = state.displayName,
                                onValueChange = onDisplayNameChanged,
                                label = { Text("Имя разработчика") },
                                placeholder = { Text("Алексей") },
                                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Уровень / Роль", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SegmentedPillToggle(
                                options = DeveloperRoleLevel.entries.map { it.labelRu },
                                selectedIndex = DeveloperRoleLevel.entries.indexOf(state.selectedRole),
                                onSelect = { idx -> onRoleSelected(DeveloperRoleLevel.entries[idx]) }
                            )
                        }
                    }
                }

                // 2. Daily Goal & Translation Language
                item {
                    BentoGlassCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Обучение и переводы", style = MaterialTheme.typography.titleMedium)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Дневная цель", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(10, 20, 30).forEach { goal ->
                                        val selected = state.dailyGoal == goal
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.clickable { onDailyGoalSelected(goal) }
                                        ) {
                                            Text(
                                                text = "$goal кар.",
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
                                Text("Язык переводов", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TranslationLanguage.entries.forEach { lang ->
                                        val selected = state.translationLanguage == lang
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.clickable { onTranslationLanguageSelected(lang) }
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
                        }
                    }
                }

                // 3. Theme Mode & IDE Presets
                item {
                    BentoGlassCard {
                        Column(
                            modifier = Modifier.padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Тема оформления", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))

                            val baseModes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                            val baseSelectedIdx = baseModes.indexOf(state.themeMode).coerceAtLeast(-1)
                            SegmentedPillToggle(
                                options = baseModes.map { it.labelRu },
                                selectedIndex = baseSelectedIdx,
                                onSelect = { idx -> onThemeModeSelected(baseModes[idx]) },
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
                                    val selected = state.themeMode == ideMode
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.clickable { onThemeModeSelected(ideMode) }
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
                        }
                    }
                }

                // Finish CTA + 2-step indicator
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onCompleteClick,
                            enabled = !state.isSaving && state.username.length >= 3 && state.usernameError == null,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Сохранение…")
                            } else {
                                Text("Завершить и начать →")
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                                                if (isDark) TechColors.Indigo else Color(0xFF4338CA)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
