package com.techenglish.app.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.database.dao.SocialCacheDao
import com.techenglish.app.core.database.entity.UserProfileCacheEntity
import com.techenglish.app.core.network.ApiHttpException
import com.techenglish.app.core.network.AuthResponseDto
import com.techenglish.app.core.network.LoginBodyDto
import com.techenglish.app.core.network.ProfileUpdateBodyDto
import com.techenglish.app.core.network.RegisterBodyDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.core.security.OAuthCallbackEvent
import com.techenglish.app.core.security.OAuthCallbackHandler
import com.techenglish.app.core.sync.CloudSyncEngine
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.usecase.ValidateSocialActionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DeveloperRoleLevel(val labelRu: String, val apiKey: String) {
    JUNIOR("Junior", "junior"),
    MIDDLE("Middle", "middle"),
    SENIOR("Senior", "senior");

    companion object {
        fun fromRaw(raw: String): DeveloperRoleLevel =
            entries.firstOrNull {
                it.labelRu.equals(raw, ignoreCase = true) || it.apiKey.equals(raw, ignoreCase = true)
            } ?: MIDDLE
    }
}

sealed interface OnboardingAuthNavEvent {
    data object NavigateToProfileSetup : OnboardingAuthNavEvent
    data object NavigateToHome : OnboardingAuthNavEvent
}

data class OnboardingAuthUiState(
    val isBusy: Boolean = false,
    val isProcessingOAuth: Boolean = false,
    val showEmailForm: Boolean = false,
    val isRegisterMode: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OnboardingAuthViewModel @Inject constructor(
    private val oauthCallbackHandler: OAuthCallbackHandler,
    private val apiClient: TechEnglishApiClient,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val socialCacheDao: SocialCacheDao,
    private val cloudSyncEngine: CloudSyncEngine,
    private val settingsRepository: SettingsRepository,
    private val validateSocialActions: ValidateSocialActionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingAuthUiState())
    val uiState: StateFlow<OnboardingAuthUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<OnboardingAuthNavEvent>(extraBufferCapacity = 4)
    val navEvents: SharedFlow<OnboardingAuthNavEvent> = _navEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            oauthCallbackHandler.isProcessingCallback.collect { processing ->
                _uiState.update { it.copy(isProcessingOAuth = processing) }
            }
        }
        viewModelScope.launch {
            oauthCallbackHandler.callbackResults.collect { evt ->
                when (evt) {
                    is OAuthCallbackEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                isProcessingOAuth = false,
                                errorMessage = evt.messageRu
                            )
                        }
                    }
                    is OAuthCallbackEvent.Success -> {
                        onCloudAuthSucceeded(evt.auth)
                    }
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleEmailForm() {
        _uiState.update { it.copy(showEmailForm = !it.showEmailForm, errorMessage = null) }
    }

    fun setRegisterMode(isRegister: Boolean) {
        _uiState.update { it.copy(isRegisterMode = isRegister, errorMessage = null) }
    }

    fun startOAuth(context: Context, provider: AuthProvider) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Нет подключения к сети. Для входа через ${provider.labelRu} нужен интернет или выберите «Продолжить офлайн»."
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(errorMessage = null) }
            val uri = oauthCallbackHandler.prepareAuthorizeUrl(provider)
            oauthCallbackHandler.launchCustomTab(context, uri)
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(errorMessage = "Офлайн-режим: для входа по Email требуется подключение к сети.")
                }
                return@launch
            }
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                val res = apiClient.login(LoginBodyDto(email = email.trim(), password = password))
                onCloudAuthSucceeded(res)
            } catch (ce: CancellationException) {
                throw ce
            } catch (err: Exception) {
                val msg = when {
                    err is ApiHttpException && err.statusCode == 401 -> "Неверный email или пароль."
                    err is ApiHttpException -> err.detail
                    else -> "Ошибка входа: ${err.localizedMessage ?: "проверьте соединение"}"
                }
                _uiState.update { it.copy(isBusy = false, errorMessage = msg) }
            }
        }
    }

    fun registerWithEmail(email: String, password: String, username: String, displayName: String) {
        viewModelScope.launch {
            val cleanUsername = validateSocialActions.validateUsername(username).getOrElse {
                _uiState.update { st -> st.copy(errorMessage = it.message ?: "Некорректный @username") }
                return@launch
            }
            if (password.length < 8) {
                _uiState.update { it.copy(errorMessage = "Пароль должен содержать минимум 8 символов.") }
                return@launch
            }
            if (!apiClient.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(errorMessage = "Офлайн-режим: регистрация по Email требует подключения к сети.")
                }
                return@launch
            }
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                val res = apiClient.register(
                    RegisterBodyDto(
                        email = email.trim(),
                        password = password,
                        username = cleanUsername,
                        displayName = displayName.trim().ifEmpty { cleanUsername },
                        ageConfirmed16Plus = true
                    )
                )
                settingsRepository.setLocalUsername(res.username)
                settingsRepository.setUserName(displayName.trim().ifEmpty { res.username })
                settingsRepository.setAgeVerified16Plus(true)
                cloudSyncEngine.scheduleBackgroundSync()
                _uiState.update { it.copy(isBusy = false) }
                _navEvents.emit(OnboardingAuthNavEvent.NavigateToProfileSetup)
            } catch (ce: CancellationException) {
                throw ce
            } catch (err: Exception) {
                val msg = when {
                    err is ApiHttpException && err.statusCode == 409 -> "Email или @$cleanUsername уже зарегистрированы."
                    err is ApiHttpException -> err.detail
                    else -> "Ошибка регистрации: ${err.localizedMessage ?: "проверьте соединение"}"
                }
                _uiState.update { it.copy(isBusy = false, errorMessage = msg) }
            }
        }
    }

    fun continueOffline() {
        viewModelScope.launch {
            settingsRepository.setAgeVerified16Plus(true)
            _navEvents.emit(OnboardingAuthNavEvent.NavigateToProfileSetup)
        }
    }

    private suspend fun onCloudAuthSucceeded(auth: AuthResponseDto) {
        _uiState.update { it.copy(isBusy = true, errorMessage = null) }
        val profile = runCatching { apiClient.fetchMyProfile() }.getOrNull()
        val resolvedUsername = profile?.username?.takeIf { it.isNotBlank() } ?: auth.username
        val resolvedDisplayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: resolvedUsername

        if (profile != null && profile.userId.isNotBlank()) {
            socialCacheDao.upsertProfile(
                UserProfileCacheEntity(
                    userId = profile.userId,
                    username = resolvedUsername,
                    displayName = resolvedDisplayName,
                    avatarKey = profile.avatarKey,
                    statusText = profile.statusText,
                    techStackCsv = profile.techStack.joinToString(", "),
                    githubUrl = profile.githubUrl,
                    telegramHandle = profile.telegramHandle,
                    rankTitle = profile.rankTitle,
                    totalXp = profile.totalXp,
                    streakDays = profile.streakDays,
                    league = profile.league,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        val isAutoGeneratedUsername = AUTO_GENERATED_USERNAME_REGEX.matches(resolvedUsername)
        val hasConfiguredProfile = profile != null &&
            !isAutoGeneratedUsername &&
            (profile.totalXp > 0 || (profile.displayName.isNotBlank() && profile.statusText.isNotBlank()))

        settingsRepository.setAgeVerified16Plus(true)
        settingsRepository.setLocalUsername(if (isAutoGeneratedUsername) "" else resolvedUsername)
        settingsRepository.setUserName(if (isAutoGeneratedUsername) "" else resolvedDisplayName)
        cloudSyncEngine.scheduleBackgroundSync()

        if (hasConfiguredProfile) {
            settingsRepository.setInitialSetupCompleted(true)
            _uiState.update { it.copy(isBusy = false) }
            _navEvents.emit(OnboardingAuthNavEvent.NavigateToHome)
        } else {
            _uiState.update { it.copy(isBusy = false) }
            _navEvents.emit(OnboardingAuthNavEvent.NavigateToProfileSetup)
        }
    }

    companion object {
        internal val AUTO_GENERATED_USERNAME_REGEX = Regex("^dev_[0-9a-f]{6,}$")
    }
}

sealed interface OnboardingSetupNavEvent {
    data object NavigateToHome : OnboardingSetupNavEvent
}

data class OnboardingProfileSetupUiState(
    val username: String = "",
    val usernameError: String? = null,
    val displayName: String = "",
    val selectedRole: DeveloperRoleLevel = DeveloperRoleLevel.MIDDLE,
    val dailyGoal: Int = 20,
    val translationLanguage: TranslationLanguage = TranslationLanguage.RU,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isAuthenticated: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OnboardingProfileSetupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val apiClient: TechEnglishApiClient,
    private val socialCacheDao: SocialCacheDao,
    private val cloudSyncEngine: CloudSyncEngine,
    private val validateSocialActions: ValidateSocialActionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingProfileSetupUiState())
    val uiState: StateFlow<OnboardingProfileSetupUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<OnboardingSetupNavEvent>(extraBufferCapacity = 4)
    val navEvents: SharedFlow<OnboardingSetupNavEvent> = _navEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            val session = tokenStore.sessionFlow.first()
            val cachedProfile = runCatching { socialCacheDao.getProfile() }.getOrNull()
            val settings = settingsRepository.observeSettings().first()
            val rawUsername = when {
                !session.isGuest && session.username.isNotBlank() -> session.username
                settings.localUsername.isNotBlank() -> settings.localUsername
                else -> ""
            }
            val prefillUsername = if (OnboardingAuthViewModel.AUTO_GENERATED_USERNAME_REGEX.matches(rawUsername) ||
                rawUsername == "guest_dev" ||
                rawUsername == "dev_user"
            ) {
                ""
            } else {
                rawUsername
            }
            val rawDisplayName = cachedProfile?.displayName?.takeIf { it.isNotBlank() }
                ?: settings.userName.takeIf { it.isNotBlank() && it != "Разработчик" }
                ?: prefillUsername

            _uiState.update {
                it.copy(
                    username = prefillUsername,
                    displayName = rawDisplayName,
                    selectedRole = DeveloperRoleLevel.fromRaw(settings.developerRole),
                    dailyGoal = settings.dailyGoal,
                    translationLanguage = settings.translationLanguage,
                    themeMode = settings.themeMode,
                    isAuthenticated = !session.isGuest
                )
            }
        }
    }

    fun onUsernameChanged(raw: String) {
        val cleaned = raw.replace(Regex("\\s+"), "").removePrefix("@").lowercase()
        val validationError = if (cleaned.isNotEmpty()) {
            validateSocialActions.validateUsername(cleaned).exceptionOrNull()?.message
        } else {
            null
        }
        _uiState.update {
            it.copy(
                username = cleaned.take(20),
                usernameError = validationError,
                errorMessage = null
            )
        }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name.take(40), errorMessage = null) }
    }

    fun onRoleSelected(role: DeveloperRoleLevel) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun onDailyGoalSelected(goal: Int) {
        _uiState.update { it.copy(dailyGoal = goal) }
    }

    fun onTranslationLanguageSelected(language: TranslationLanguage) {
        _uiState.update { it.copy(translationLanguage = language) }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun completeSetup() {
        val current = _uiState.value
        if (current.isSaving) return
        val cleanUsername = validateSocialActions.validateUsername(current.username).getOrElse { err ->
            _uiState.update {
                it.copy(usernameError = err.message ?: "Username должен содержать 3–20 символов (a-z, 0-9, _)")
            }
            return
        }
        val cleanDisplayName = current.displayName.trim().ifBlank { cleanUsername }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, usernameError = null, errorMessage = null) }
            try {
                val session = tokenStore.sessionFlow.first()
                val hasCloudToken = !session.isGuest && !tokenStore.getAccessToken().isNullOrBlank()
                var pendingSync = hasCloudToken

                if (hasCloudToken && apiClient.isNetworkAvailable()) {
                    try {
                        val updated = apiClient.updateMyProfile(
                            ProfileUpdateBodyDto(
                                username = cleanUsername,
                                displayName = cleanDisplayName,
                                statusText = "${current.selectedRole.labelRu} Developer"
                            )
                        )
                        tokenStore.updateSessionProfile(username = updated.username)
                        socialCacheDao.upsertProfile(
                            UserProfileCacheEntity(
                                userId = updated.userId.ifBlank { session.userId },
                                username = updated.username,
                                displayName = updated.displayName.ifBlank { cleanDisplayName },
                                avatarKey = updated.avatarKey,
                                statusText = updated.statusText,
                                techStackCsv = updated.techStack.joinToString(", "),
                                githubUrl = updated.githubUrl,
                                telegramHandle = updated.telegramHandle,
                                rankTitle = updated.rankTitle,
                                totalXp = updated.totalXp,
                                streakDays = updated.streakDays,
                                league = updated.league,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        pendingSync = false
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (err: ApiHttpException) {
                        if (err.statusCode == 409) {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    usernameError = "Ник @$cleanUsername уже занят — выберите другой"
                                )
                            }
                            return@launch
                        }
                        if (err.statusCode == 422) {
                            _uiState.update {
                                it.copy(isSaving = false, usernameError = err.detail)
                            }
                            return@launch
                        }
                        // Transient 5xx/cold-start: save locally and queue background sync
                        pendingSync = true
                    } catch (_: Exception) {
                        // Offline or network timeout: save locally and queue background sync
                        pendingSync = true
                    }
                } else if (hasCloudToken) {
                    tokenStore.updateSessionProfile(username = cleanUsername)
                    pendingSync = true
                }

                settingsRepository.completeInitialSetup(
                    localUsername = cleanUsername,
                    displayName = cleanDisplayName,
                    developerRole = current.selectedRole.labelRu,
                    dailyGoal = current.dailyGoal,
                    translationLanguage = current.translationLanguage,
                    themeMode = current.themeMode,
                    pendingProfileSync = pendingSync
                )
                if (hasCloudToken) {
                    cloudSyncEngine.scheduleBackgroundSync()
                }
                _uiState.update { it.copy(isSaving = false) }
                _navEvents.emit(OnboardingSetupNavEvent.NavigateToHome)
            } catch (ce: CancellationException) {
                throw ce
            } catch (err: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Не удалось сохранить профиль: ${err.localizedMessage ?: "повторите попытку"}"
                    )
                }
            }
        }
    }
}
