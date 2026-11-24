package com.techenglish.app.feature.stage3

import android.content.Context
import androidx.compose.runtime.Immutable
import com.techenglish.app.core.security.OAuthCallbackHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.database.dao.OtaContentMetaDao
import com.techenglish.app.core.database.dao.SocialCacheDao
import com.techenglish.app.core.database.dao.SyncQueueDao
import com.techenglish.app.core.database.entity.ActivityFeedCacheEntity
import com.techenglish.app.core.database.entity.BadgesCacheEntity
import com.techenglish.app.core.database.entity.FriendsCacheEntity
import com.techenglish.app.core.database.entity.LeaderboardCacheEntity
import com.techenglish.app.core.database.entity.LocalAccountLinkEntity
import com.techenglish.app.core.database.entity.OtaContentMetaEntity
import com.techenglish.app.core.database.entity.UserProfileCacheEntity
import com.techenglish.app.core.network.LoginBodyDto
import com.techenglish.app.core.network.RegisterBodyDto
import com.techenglish.app.core.network.SyncBatchBodyDto
import com.techenglish.app.core.network.SyncEventBodyDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.core.sync.CloudSyncEngine
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.model.AuthSession
import com.techenglish.domain.model.DailyProgress
import com.techenglish.domain.model.LeaderboardTab
import com.techenglish.domain.model.SyncEventType
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.usecase.CalculateWeeklyLeagueUseCase
import com.techenglish.domain.usecase.GetDailyProgressUseCase
import com.techenglish.domain.usecase.UpdateSettingsUseCase
import com.techenglish.domain.usecase.ValidateSocialActionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// =========================================================================
// 1. Cloud Account, Outbox Sync, Guest Link & OTA Delivery (ФТ-9.1, 9.2, 9.4, 14.5)
// =========================================================================

data class CloudAccountSyncUiState(
    val session: AuthSession = AuthSession(),
    val pendingSyncEvents: Int = 0,
    val appliedOtaPacks: List<OtaContentMetaEntity> = emptyList(),
    val isOffline: Boolean = false,
    val isBusy: Boolean = false,
    val apiBaseUrl: String = AndroidKeystoreTokenStore.DEFAULT_API_BASE_URL,
    val linkedAtMillis: Long = 0L,
    val settings: UserSettings = UserSettings()
)

@HiltViewModel
class CloudAccountSyncViewModel @Inject constructor(
    private val tokenStore: AndroidKeystoreTokenStore,
    private val syncQueueDao: SyncQueueDao,
    private val socialCacheDao: SocialCacheDao,
    private val otaContentMetaDao: OtaContentMetaDao,
    private val apiClient: TechEnglishApiClient,
    private val cloudSyncEngine: CloudSyncEngine,
    private val settingsRepository: SettingsRepository,
    private val updateSettings: UpdateSettingsUseCase,
    private val validateSocialActions: ValidateSocialActionsUseCase,
    private val oauthCallbackHandler: OAuthCallbackHandler
) : ViewModel() {

    private val isBusyFlow = MutableStateFlow(false)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<CloudAccountSyncUiState> = combine(
        tokenStore.sessionFlow,
        syncQueueDao.observePendingCount(),
        otaContentMetaDao.observeAppliedPacks(),
        tokenStore.apiBaseUrlFlow,
        combine(
            socialCacheDao.observeAccountLink(),
            settingsRepository.observeSettings(),
            isBusyFlow
        ) { link, settings, busy -> Triple(link, settings, busy) }
    ) { session, pendingCount, otaPacks, apiBaseUrl, extra ->
        val (link, settings, busy) = extra
        CloudAccountSyncUiState(
            session = session,
            pendingSyncEvents = pendingCount,
            appliedOtaPacks = otaPacks,
            isOffline = !apiClient.isNetworkAvailable(),
            isBusy = busy,
            apiBaseUrl = apiBaseUrl,
            linkedAtMillis = link?.linkedAt ?: 0L,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CloudAccountSyncUiState(isOffline = !apiClient.isNetworkAvailable())
    )

    fun registerWithEmail(email: String, password: String, username: String, displayName: String) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Нет подключения к сети. Продолжайте в гостевом офлайн-режиме.")
                return@launch
            }
            val cleanUsername = validateSocialActions.validateUsername(username).getOrElse {
                _events.emit(it.message ?: "Некорректный @username")
                return@launch
            }
            if (password.length < 8) {
                _events.emit("Пароль должен содержать минимум 8 символов.")
                return@launch
            }
            isBusyFlow.value = true
            runCatching {
                val res = apiClient.register(
                    RegisterBodyDto(
                        email = email.trim(),
                        password = password,
                        username = cleanUsername,
                        displayName = displayName.trim().ifEmpty { cleanUsername },
                        ageConfirmed16Plus = true
                    )
                )
                tokenStore.saveTokensAndSession(
                    accessToken = res.accessToken,
                    refreshToken = res.refreshToken,
                    userId = res.userId,
                    username = res.username,
                    email = email.trim(),
                    provider = AuthProvider.EMAIL
                )
                socialCacheDao.upsertProfile(
                    UserProfileCacheEntity(
                        userId = res.userId,
                        username = res.username,
                        displayName = displayName.trim().ifEmpty { res.username },
                        updatedAt = System.currentTimeMillis()
                    )
                )
                cloudSyncEngine.scheduleBackgroundSync()
                val summary = cloudSyncEngine.runFullSyncNow()
                _events.emit("Аккаунт @${res.username} создан. ${summary.messageRu}")
            }.onFailure { err ->
                _events.emit("Ошибка регистрации: ${err.localizedMessage ?: "проверьте соединение"}")
            }
            isBusyFlow.value = false
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: вход недоступен без сети.")
                return@launch
            }
            isBusyFlow.value = true
            runCatching {
                val res = apiClient.login(LoginBodyDto(email = email.trim(), password = password))
                tokenStore.saveTokensAndSession(
                    accessToken = res.accessToken,
                    refreshToken = res.refreshToken,
                    userId = res.userId,
                    username = res.username,
                    email = email.trim(),
                    provider = AuthProvider.fromRaw(res.provider)
                )
                cloudSyncEngine.scheduleBackgroundSync()
                val summary = cloudSyncEngine.runFullSyncNow()
                _events.emit("Выполнен вход как @${res.username}. ${summary.messageRu}")
            }.onFailure { err ->
                _events.emit("Ошибка входа: ${err.localizedMessage ?: "проверьте email и пароль"}")
            }
            isBusyFlow.value = false
        }
    }

    /**
     * Prepares a server-side OAuth 2.0 Custom Tabs flow (SP1-03b / BUG-04):
     * persists random `client_state` in DataStore and launches Custom Tabs to `/v1/auth/oauth/{provider}/authorize`.
     */
    fun startOAuthLogin(context: Context, provider: AuthProvider) {
        viewModelScope.launch {
            val uri = oauthCallbackHandler.prepareAuthorizeUrl(provider)
            oauthCallbackHandler.launchCustomTab(context, uri)
        }
    }

    fun linkGuestAccount() {
        viewModelScope.launch {
            val session = uiState.value.session
            if (session.isGuest) {
                _events.emit("Сначала войдите или зарегистрируйтесь, чтобы привязать гостевой прогресс.")
                return@launch
            }
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: привязка выполнится при появлении сети.")
                return@launch
            }
            isBusyFlow.value = true
            runCatching {
                val pending = syncQueueDao.getPendingSyncEventsBatch(limit = 100)
                val batch = SyncBatchBodyDto(
                    events = pending.map { row ->
                        val type = SyncEventType.fromRaw(row.eventType)
                        SyncEventBodyDto(
                            eventId = row.eventId,
                            eventType = type.name,
                            clientReportedXp = type.canonicalXp,
                            clientTimestampMs = row.createdAt,
                            payloadJson = row.payloadJson
                        )
                    }
                )
                val res = apiClient.linkGuestProgress(batch)
                val doneIds = (res.acceptedEventIds + res.duplicateEventIds).distinct()
                if (doneIds.isNotEmpty()) {
                    syncQueueDao.markSynced(doneIds)
                }
                socialCacheDao.upsertAccountLink(
                    LocalAccountLinkEntity(
                        localUserId = "guest_local",
                        remoteUserId = session.userId,
                        provider = session.provider.name,
                        linkedAt = System.currentTimeMillis()
                    )
                )
                _events.emit("Гостевой прогресс объединён (+${res.awardedXp} XP на сервере).")
            }.onFailure { err ->
                _events.emit("Не удалось объединить гостевой прогресс: ${err.localizedMessage ?: "ошибка сети"}")
            }
            isBusyFlow.value = false
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            isBusyFlow.value = true
            val summary = cloudSyncEngine.runFullSyncNow()
            _events.emit(summary.messageRu)
            isBusyFlow.value = false
        }
    }

    fun checkOtaPacks() {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: проверка OTA-пакетов требует интернет.")
                return@launch
            }
            isBusyFlow.value = true
            val applied = cloudSyncEngine.syncOtaContentPacks()
            _events.emit(
                if (applied > 0) "Установлено новых OTA-пакетов: $applied (SHA-256 подтверждён)."
                else "База контента актуальна, новых OTA-пакетов нет."
            )
            isBusyFlow.value = false
        }
    }

    fun updateApiBaseUrl(url: String) {
        viewModelScope.launch {
            tokenStore.setApiBaseUrl(url)
            _events.emit("Адрес сервера синхронизации обновлён.")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            tokenStore.clearSession()
            _events.emit("Выход выполнен. Локальный офлайн-прогресс сохранён.")
        }
    }

    fun deleteAccountAndCloudData() {
        viewModelScope.launch {
            isBusyFlow.value = true
            runCatching {
                if (!uiState.value.session.isGuest && apiClient.isNetworkAvailable()) {
                    apiClient.deleteAccount()
                }
                tokenStore.clearSession()
                socialCacheDao.clearProfile()
                socialCacheDao.clearAccountLink()
                socialCacheDao.clearFriends()
                socialCacheDao.clearActivityFeed()
                LeaderboardTab.entries.forEach { socialCacheDao.clearLeaderboardTab(it.name) }
                _events.emit("Облачный аккаунт и кэш удалены. Приложение переведено в гостевой режим.")
            }.onFailure { err ->
                _events.emit("Ошибка удаления аккаунта: ${err.localizedMessage ?: "повторите онлайн"}")
            }
            isBusyFlow.value = false
        }
    }

    fun confirmAge16Plus() {
        viewModelScope.launch {
            updateSettings.setAgeVerified16Plus(true)
        }
    }
}

// =========================================================================
// 2. Public Developer Profile & Badges (ФТ-10.1–10.3, Errata E2)
// =========================================================================

data class DeveloperCardUiState(
    val profile: UserProfileCacheEntity? = null,
    val badges: List<BadgesCacheEntity> = emptyList(),
    val dailyProgress: DailyProgress? = null,
    val session: AuthSession = AuthSession(),
    val settings: UserSettings = UserSettings()
)

@HiltViewModel
class DeveloperCardViewModel @Inject constructor(
    private val socialCacheDao: SocialCacheDao,
    private val getDailyProgress: GetDailyProgressUseCase,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val settingsRepository: SettingsRepository,
    private val validateSocialActions: ValidateSocialActionsUseCase,
    private val apiClient: TechEnglishApiClient
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<DeveloperCardUiState> = combine(
        socialCacheDao.observeProfile(),
        socialCacheDao.observeBadges(),
        getDailyProgress(),
        tokenStore.sessionFlow,
        settingsRepository.observeSettings()
    ) { profile, badges, daily, session, settings ->
        DeveloperCardUiState(
            profile = profile,
            badges = badges,
            dailyProgress = daily,
            session = session,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DeveloperCardUiState()
    )

    init {
        viewModelScope.launch {
            seedAndRefreshLocalBadges()
        }
    }

    private suspend fun seedAndRefreshLocalBadges() {
        val now = System.currentTimeMillis()
        val daily = uiState.value.dailyProgress
        val xp = daily?.levelProgress?.totalXp ?: 0
        val streak = daily?.streakDays ?: 0
        val defaults = listOf(
            BadgesCacheEntity(
                id = "badge_offline_pioneer",
                titleRu = "Offline-First Pioneer",
                descriptionRu = "Активировал TechEnglish v3 и начал локальное обучение",
                iconKey = "terminal",
                isUnlocked = true,
                unlockedAt = now
            ),
            BadgesCacheEntity(
                id = "badge_streak_7",
                titleRu = "7-Day Commit Streak",
                descriptionRu = "Учебная серия 7 дней подряд без пропусков",
                iconKey = "flame",
                isUnlocked = streak >= 7,
                unlockedAt = if (streak >= 7) now else 0L
            ),
            BadgesCacheEntity(
                id = "badge_xp_500",
                titleRu = "Senior Lexicon",
                descriptionRu = "Набрано 500+ XP в словаре и тренажёрах",
                iconKey = "bolt",
                isUnlocked = xp >= 500,
                unlockedAt = if (xp >= 500) now else 0L
            ),
            BadgesCacheEntity(
                id = "badge_log_debugger",
                titleRu = "Stacktrace Detective",
                descriptionRu = "Пройдены разборы реальных логов и документации",
                iconKey = "bug",
                isUnlocked = xp >= 100,
                unlockedAt = if (xp >= 100) now else 0L
            ),
            BadgesCacheEntity(
                id = "badge_polyglot",
                titleRu = "19-Language Polyglot",
                descriptionRu = "Сравнение синтаксиса языков в каталоге",
                iconKey = "code",
                isUnlocked = xp >= 150,
                unlockedAt = if (xp >= 150) now else 0L
            )
        )
        socialCacheDao.insertBadges(defaults)
    }

    fun saveDeveloperProfile(
        username: String,
        displayName: String,
        avatarKey: String,
        statusText: String,
        techStackCsv: String,
        githubUrl: String,
        telegramHandle: String
    ) {
        viewModelScope.launch {
            val cleanUsername = validateSocialActions.validateUsername(username).getOrElse {
                _events.emit(it.message ?: "Недопустимый @username")
                return@launch
            }
            if (!validateSocialActions.isAvatarAllowed(avatarKey)) {
                _events.emit("Errata E2: разрешены только встроенные пресеты или верифицированный OAuth-аватар.")
                return@launch
            }
            val current = uiState.value
            val level = current.dailyProgress?.levelProgress
            val cleanDisplayName = displayName.trim().ifEmpty { current.settings.userName }
            val cleanStatus = statusText.trim().take(120)
            val cleanTechStackCsv = techStackCsv.trim().take(120)
            val cleanGithub = githubUrl.trim().take(160)
            val cleanTelegram = telegramHandle.trim().take(64)
            socialCacheDao.upsertProfile(
                UserProfileCacheEntity(
                    userId = current.session.userId,
                    username = cleanUsername,
                    displayName = cleanDisplayName,
                    avatarKey = avatarKey,
                    statusText = cleanStatus,
                    techStackCsv = cleanTechStackCsv,
                    githubUrl = cleanGithub,
                    telegramHandle = cleanTelegram,
                    rankTitle = level?.rankTitleRu ?: "Intern Developer",
                    totalXp = level?.totalXp ?: 0,
                    streakDays = current.dailyProgress?.streakDays ?: 0,
                    league = current.profile?.league ?: "BRONZE",
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                val stackList = cleanTechStackCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                runCatching {
                    apiClient.updateMyProfile(
                        com.techenglish.app.core.network.ProfileUpdateBodyDto(
                            username = cleanUsername,
                            displayName = cleanDisplayName,
                            avatarKey = avatarKey,
                            statusText = cleanStatus,
                            techStack = stackList,
                            githubUrl = cleanGithub,
                            telegramHandle = cleanTelegram
                        )
                    )
                }.onSuccess {
                    _events.emit("Карточка разработчика @$cleanUsername сохранена и синхронизирована.")
                }.onFailure {
                    _events.emit("Карточка разработчика @$cleanUsername сохранена локально (синхронизируется позже).")
                }
            } else {
                _events.emit("Карточка разработчика @$cleanUsername сохранена локально.")
            }
        }
    }

    fun reportProfile(targetId: String, reason: String) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: жалоба будет доступна при подключении к сети.")
                return@launch
            }
            runCatching { apiClient.reportUser(targetId, reason) }
                .onSuccess { _events.emit("Жалоба отправлена на модерацию.") }
                .onFailure { _events.emit("Не удалось отправить жалобу: ${it.localizedMessage ?: "требуется вход"}") }
        }
    }
}

// =========================================================================
// 3. Top-10 Segmented Leaderboards & Weekly Leagues (ФТ-11.1–11.2, НФТ-7.1)
// =========================================================================

@Immutable
data class LeaderboardsUiState(
    val selectedTab: LeaderboardTab = LeaderboardTab.WEEKLY,
    val entries: ImmutableList<LeaderboardCacheEntity> = persistentListOf(),
    val isOffline: Boolean = false,
    val isRefreshing: Boolean = false,
    val weekStartUtc5Label: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LeaderboardsViewModel @Inject constructor(
    private val socialCacheDao: SocialCacheDao,
    private val apiClient: TechEnglishApiClient,
    private val cloudSyncEngine: CloudSyncEngine,
    private val calculateWeeklyLeague: CalculateWeeklyLeagueUseCase
) : ViewModel() {

    private val selectedTabFlow = MutableStateFlow(LeaderboardTab.WEEKLY)
    private val isRefreshingFlow = MutableStateFlow(false)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val weekStartLabel: String = run {
        val weekStartMs = calculateWeeklyLeague.calculateWeekStartUtc5Millis(System.currentTimeMillis())
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy 00:00 'UTC+5'")
        Instant.ofEpochMilli(weekStartMs).atOffset(ZoneOffset.ofHours(5)).format(formatter)
    }

    val uiState: StateFlow<LeaderboardsUiState> = combine(
        selectedTabFlow,
        selectedTabFlow.flatMapLatest { tab -> socialCacheDao.observeLeaderboardForTab(tab.name) },
        isRefreshingFlow
    ) { tab, rows, refreshing ->
        LeaderboardsUiState(
            selectedTab = tab,
            entries = rows.toImmutableList(),
            isOffline = !apiClient.isNetworkAvailable(),
            isRefreshing = refreshing,
            weekStartUtc5Label = weekStartLabel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LeaderboardsUiState(
            isOffline = !apiClient.isNetworkAvailable(),
            weekStartUtc5Label = weekStartLabel
        )
    )

    fun selectTab(tab: LeaderboardTab) {
        selectedTabFlow.value = tab
    }

    fun refresh() {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: показан сохранённый локальный кэш лидерборда.")
                return@launch
            }
            isRefreshingFlow.value = true
            runCatching { cloudSyncEngine.refreshOnlineCaches() }
                .onSuccess { _events.emit("Лидерборды обновлены.") }
                .onFailure { _events.emit("Показан локальный кэш (войдите в аккаунт для синхронизации).") }
            isRefreshingFlow.value = false
        }
    }
}

// =========================================================================
// 4. Friends, QR Invite, Activity Feed & Ping (ФТ-12.1–12.3, Errata E2)
// =========================================================================

@Immutable
data class FriendsAndFeedUiState(
    val friends: ImmutableList<FriendsCacheEntity> = persistentListOf(),
    val feed: ImmutableList<ActivityFeedCacheEntity> = persistentListOf(),
    val allowFriendPings: Boolean = true,
    val currentUsername: String = "guest_dev",
    val isOffline: Boolean = false,
    val isBusy: Boolean = false
)

@HiltViewModel
class FriendsAndFeedViewModel @Inject constructor(
    private val socialCacheDao: SocialCacheDao,
    private val apiClient: TechEnglishApiClient,
    private val cloudSyncEngine: CloudSyncEngine,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val settingsRepository: SettingsRepository,
    private val updateSettings: UpdateSettingsUseCase,
    private val validateSocialActions: ValidateSocialActionsUseCase
) : ViewModel() {

    private val isBusyFlow = MutableStateFlow(false)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<FriendsAndFeedUiState> = combine(
        socialCacheDao.observeFriends(),
        socialCacheDao.observeActivityFeed(),
        settingsRepository.observeSettings(),
        tokenStore.sessionFlow,
        isBusyFlow
    ) { friends, feed, settings, session, busy ->
        FriendsAndFeedUiState(
            friends = friends.toImmutableList(),
            feed = feed.toImmutableList(),
            allowFriendPings = settings.allowFriendPings,
            currentUsername = session.username,
            isOffline = !apiClient.isNetworkAvailable(),
            isBusy = busy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = FriendsAndFeedUiState(isOffline = !apiClient.isNetworkAvailable())
    )

    fun addFriend(rawUsername: String) {
        viewModelScope.launch {
            val clean = validateSocialActions.validateUsername(rawUsername).getOrElse {
                _events.emit(it.message ?: "Неверный @username")
                return@launch
            }
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: добавление друзей требует интернет.")
                return@launch
            }
            isBusyFlow.value = true
            runCatching {
                apiClient.addFriend(clean)
                cloudSyncEngine.refreshOnlineCaches()
                _events.emit("@$clean добавлен в друзья!")
            }.onFailure { err ->
                _events.emit("Не удалось добавить @$clean: ${err.localizedMessage ?: "проверьте вход и логин"}")
            }
            isBusyFlow.value = false
        }
    }

    fun pingFriend(friend: FriendsCacheEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (!validateSocialActions.canPingFriend(friend.acceptsPings, friend.lastPingedAt, now)) {
                val msg = if (!friend.acceptsPings) {
                    "@${friend.username} отключил напоминания от друзей."
                } else {
                    "Лимит ФТ-12.3: пнуть @${friend.username} можно не чаще 1 раза в 24 часа."
                }
                _events.emit(msg)
                return@launch
            }
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: отправка пинга требует подключение к сети.")
                return@launch
            }
            runCatching {
                apiClient.pingFriend(friend.userId)
                socialCacheDao.updateFriendLastPinged(friend.userId, now)
                _events.emit("Напоминание отправлено @${friend.username}!")
            }.onFailure { err ->
                _events.emit("Не удалось отправить пинг: ${err.localizedMessage ?: "ошибка сети"}")
            }
        }
    }

    fun reactToFeedItem(itemId: String) {
        viewModelScope.launch {
            socialCacheDao.incrementFeedReaction(itemId)
            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching { apiClient.reactToFeedEvent(itemId) }
            }
            _events.emit("Реакция добавлена!")
        }
    }

    fun setAllowFriendPings(enabled: Boolean) {
        viewModelScope.launch {
            updateSettings.setAllowFriendPings(enabled)
            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching {
                    apiClient.updateMyProfile(
                        com.techenglish.app.core.network.ProfileUpdateBodyDto(acceptsPings = enabled)
                    )
                }
            }
            _events.emit(
                if (enabled) "Напоминания от друзей включены."
                else "Напоминания от друзей отключены (opt-out)."
            )
        }
    }

    fun reportAndBlockUser(targetUserId: String, username: String, reason: String) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: модерация требует подключение к сети.")
                return@launch
            }
            runCatching {
                apiClient.reportUser(targetUserId, reason)
                apiClient.blockUser(targetUserId)
                cloudSyncEngine.refreshOnlineCaches()
                _events.emit("Пользователь @$username заблокирован, жалоба отправлена.")
            }.onFailure { err ->
                _events.emit("Ошибка модерации: ${err.localizedMessage ?: "требуется вход"}")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: показаны кэшированные друзья и лента.")
                return@launch
            }
            isBusyFlow.value = true
            runCatching { cloudSyncEngine.refreshOnlineCaches() }
            isBusyFlow.value = false
        }
    }
}
