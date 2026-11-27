package com.techenglish.app.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.SegmentedPillToggle
import com.techenglish.app.feature.stage3.CloudAuthHeroSection
import com.techenglish.app.feature.stage3.PrivacyPolicyDialog
import com.techenglish.domain.model.AuthProvider

@Composable
fun OnboardingAuthScreen(
    onNavigateToProfileSetup: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: OnboardingAuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { evt ->
            when (evt) {
                OnboardingAuthNavEvent.NavigateToProfileSetup -> onNavigateToProfileSetup()
                OnboardingAuthNavEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    OnboardingAuthContent(
        state = state,
        onGoogleClick = { viewModel.startOAuth(context, AuthProvider.GOOGLE) },
        onGithubClick = { viewModel.startOAuth(context, AuthProvider.GITHUB) },
        onContinueOffline = viewModel::continueOffline,
        onToggleEmailForm = viewModel::toggleEmailForm,
        onSetRegisterMode = viewModel::setRegisterMode,
        onLoginEmail = viewModel::loginWithEmail,
        onRegisterEmail = viewModel::registerWithEmail,
        onDismissError = viewModel::dismissError
    )
}

@Composable
fun OnboardingAuthContent(
    state: OnboardingAuthUiState,
    onGoogleClick: () -> Unit,
    onGithubClick: () -> Unit,
    onContinueOffline: () -> Unit,
    onToggleEmailForm: () -> Unit,
    onSetRegisterMode: (Boolean) -> Unit,
    onLoginEmail: (String, String) -> Unit,
    onRegisterEmail: (String, String, String, String) -> Unit,
    onDismissError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }

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
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    CloudAuthHeroSection(
                        onGoogleClick = onGoogleClick,
                        onGithubClick = onGithubClick,
                        onContinueAsGuest = onContinueOffline,
                        isOnboarding = true,
                        stepBadgeText = "ШАГ 1 / 2",
                        guestButtonText = "Продолжить офлайн без облака",
                        activeStepIndex = 0,
                        totalSteps = 2,
                        isBusy = state.isBusy || state.isProcessingOAuth
                    )
                }

                if (state.isBusy || state.isProcessingOAuth) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(
                                    text = if (state.isProcessingOAuth) "Завершаем вход через OAuth…" else "Проверяем профиль и синхронизацию…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
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
                                modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onDismissError, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Скрыть ошибку",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Compact expandable Email Login / Register block
                item {
                    BentoGlassCard {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onToggleEmailForm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.AlternateEmail,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Войти или создать аккаунт по Email",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Icon(
                                    imageVector = if (state.showEmailForm) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(visible = state.showEmailForm) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SegmentedPillToggle(
                                        options = listOf("Вход", "Регистрация (16+)"),
                                        selectedIndex = if (state.isRegisterMode) 1 else 0,
                                        onSelect = { onSetRegisterMode(it == 1) }
                                    )
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
                                    if (state.isRegisterMode) {
                                        OutlinedTextField(
                                            value = username,
                                            onValueChange = { username = it.replace(" ", "").removePrefix("@").lowercase() },
                                            label = { Text("@username (3–20: a-z, 0-9, _)") },
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
                                            if (state.isRegisterMode) {
                                                onRegisterEmail(email, password, username, displayName)
                                            } else {
                                                onLoginEmail(email, password)
                                            }
                                        },
                                        enabled = !state.isBusy && email.isNotBlank() && password.isNotBlank(),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text(if (state.isRegisterMode) "Создать аккаунт →" else "Войти по Email →")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { showPrivacyDialog = true }) {
                            Icon(Icons.Rounded.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("16+ и Политика конфиденциальности", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }
}
