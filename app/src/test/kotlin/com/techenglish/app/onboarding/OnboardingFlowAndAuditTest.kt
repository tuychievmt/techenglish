package com.techenglish.app.onboarding

import android.content.Context
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.database.TechEnglishDatabase
import com.techenglish.app.core.designsystem.TechEnglishTheme
import com.techenglish.app.core.navigation.Routes
import com.techenglish.app.core.navigation.resolveStartDestination
import com.techenglish.app.core.network.ApiHttpException
import com.techenglish.app.core.network.AuthResponseDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.core.security.OAuthCallbackHandler
import com.techenglish.app.core.sync.CloudSyncEngine
import com.techenglish.app.core.datastore.UserPreferencesDataStore
import com.techenglish.app.core.database.ContentPackImporter
import com.techenglish.app.feature.onboarding.DeveloperRoleLevel
import com.techenglish.app.feature.onboarding.OnboardingProfileSetupContent
import com.techenglish.app.feature.onboarding.OnboardingProfileSetupUiState
import com.techenglish.app.feature.onboarding.OnboardingProfileSetupViewModel
import com.techenglish.app.feature.onboarding.OnboardingSetupNavEvent
import com.techenglish.app.feature.profile.ProfileUiState
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.usecase.ResolveSm2SyncConflictUseCase
import com.techenglish.domain.usecase.ValidateSocialActionsUseCase
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingFlowAndAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class InMemorySettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
        val flow = MutableStateFlow(initial)
        override fun observeSettings(): Flow<UserSettings> = flow
        override suspend fun setUserName(name: String) {
            flow.value = flow.value.copy(userName = name)
        }
        override suspend fun setDailyGoal(goal: Int) {
            flow.value = flow.value.copy(dailyGoal = goal)
        }
        override suspend fun setThemeMode(mode: ThemeMode) {
            flow.value = flow.value.copy(themeMode = mode)
        }
        override suspend fun setDynamicColors(enabled: Boolean) {
            flow.value = flow.value.copy(dynamicColors = enabled)
        }
        override suspend fun setReadingFontSize(sizeSp: Float) {
            flow.value = flow.value.copy(readingFontSizeSp = sizeSp)
        }
        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            flow.value = flow.value.copy(notificationsEnabled = enabled)
        }
        override suspend fun setReminderTime(hour: Int, minute: Int) {
            flow.value = flow.value.copy(reminderHour = hour, reminderMinute = minute)
        }
        override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
            flow.value = flow.value.copy(hapticFeedbackEnabled = enabled)
        }
        override suspend fun setReduceMotionEnabled(enabled: Boolean) {
            flow.value = flow.value.copy(reduceMotionEnabled = enabled)
        }
        override suspend fun setTranslationLanguage(language: TranslationLanguage) {
            flow.value = flow.value.copy(translationLanguage = language)
        }
        override suspend fun setAgeVerified16Plus(verified: Boolean) {
            flow.value = flow.value.copy(ageVerified16Plus = verified)
        }
        override suspend fun setLocalUsername(username: String) {
            flow.value = flow.value.copy(localUsername = username)
        }
        override suspend fun completeInitialSetup(
            localUsername: String,
            displayName: String,
            developerRole: String,
            dailyGoal: Int,
            translationLanguage: TranslationLanguage,
            themeMode: ThemeMode,
            pendingProfileSync: Boolean
        ) {
            flow.value = flow.value.copy(
                localUsername = localUsername,
                userName = displayName,
                developerRole = developerRole,
                dailyGoal = dailyGoal,
                translationLanguage = translationLanguage,
                themeMode = themeMode,
                pendingProfileSync = pendingProfileSync,
                ageVerified16Plus = true,
                isInitialSetupCompleted = true
            )
        }
    }

    @Test
    fun `start destination routes to ONBOARDING_AUTH before setup and directly to HOME after setup`() {
        assertEquals(Routes.ONBOARDING_AUTH, resolveStartDestination(isInitialSetupCompleted = false))
        assertEquals(Routes.HOME, resolveStartDestination(isInitialSetupCompleted = true))
    }

    @Test
    fun `OnboardingProfileSetupViewModel validates @username, saves offline profile, and marks setup complete`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TechEnglishDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val tokenStore = AndroidKeystoreTokenStore(context)
            tokenStore.clearSession()
            val apiClient = TechEnglishApiClient(
                context = context,
                tokenStore = tokenStore,
                engineOverride = MockEngine {
                    respond("{}", HttpStatusCode.ServiceUnavailable)
                }
            )
            val prefs = UserPreferencesDataStore(context)
            val syncEngine = CloudSyncEngine(
                context = context,
                syncQueueDao = db.syncQueueDao(),
                socialCacheDao = db.socialCacheDao(),
                otaContentMetaDao = db.otaContentMetaDao(),
                wordDao = db.wordDao(),
                contentPackImporter = ContentPackImporter(db),
                apiClient = apiClient,
                tokenStore = tokenStore,
                userPrefs = prefs,
                resolveSm2Conflict = ResolveSm2SyncConflictUseCase()
            )
            val settingsRepo = InMemorySettingsRepository()

            val vm = OnboardingProfileSetupViewModel(
                settingsRepository = settingsRepo,
                tokenStore = tokenStore,
                apiClient = apiClient,
                socialCacheDao = db.socialCacheDao(),
                cloudSyncEngine = syncEngine,
                validateSocialActions = ValidateSocialActionsUseCase()
            )
            val navEvents = mutableListOf<OnboardingSetupNavEvent>()
            val collectJob = launch(UnconfinedTestDispatcher(testDispatcher.scheduler)) {
                vm.navEvents.collect { navEvents += it }
            }
            repeat(5) {
                testDispatcher.scheduler.advanceUntilIdle()
                delay(20)
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // Auto-cleans spaces/@ and validates invalid short or reserved usernames
            vm.onUsernameChanged("  @ab ")
            assertNotNull("Short username must report error", vm.uiState.value.usernameError)

            vm.onUsernameChanged("admin")
            assertNotNull("Reserved username must report error", vm.uiState.value.usernameError)

            vm.onUsernameChanged("  @Kotlin_Dev42 ")
            assertEquals("kotlin_dev42", vm.uiState.value.username)
            assertEquals(null, vm.uiState.value.usernameError)

            vm.onDisplayNameChanged("Амир")
            vm.onRoleSelected(DeveloperRoleLevel.SENIOR)
            vm.onDailyGoalSelected(30)
            vm.onTranslationLanguageSelected(TranslationLanguage.TJ)
            vm.onThemeModeSelected(ThemeMode.TOKYO_NIGHT)

            vm.completeSetup()
            withTimeout(5_000) {
                while (!settingsRepo.flow.value.isInitialSetupCompleted || navEvents.isEmpty()) {
                    testDispatcher.scheduler.advanceUntilIdle()
                    delay(20)
                }
            }
            testDispatcher.scheduler.advanceUntilIdle()

            val saved = settingsRepo.flow.value
            assertTrue(saved.isInitialSetupCompleted)
            assertTrue(saved.ageVerified16Plus)
            assertEquals("kotlin_dev42", saved.localUsername)
            assertEquals("Амир", saved.userName)
            assertEquals("Senior", saved.developerRole)
            assertEquals(30, saved.dailyGoal)
            assertEquals(TranslationLanguage.TJ, saved.translationLanguage)
            assertEquals(ThemeMode.TOKYO_NIGHT, saved.themeMode)
            assertEquals(listOf(OnboardingSetupNavEvent.NavigateToHome), navEvents)
            collectJob.cancel()
        } finally {
            db.close()
        }
    }

    @Test
    fun `ApiClient requireSuccess throws ApiHttpException on 409 and refreshTokens keeps session on Render 503`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        tokenStore.setApiBaseUrl("https://api.techenglish.dev")
        tokenStore.saveTokensAndSession(
            accessToken = "old_jwt",
            refreshToken = "keep_refresh_jwt",
            userId = "u-audit-1",
            username = "audit_dev",
            email = "audit@example.com",
            provider = AuthProvider.GOOGLE
        )

        val apiClient = TechEnglishApiClient(
            context = context,
            tokenStore = tokenStore,
            engineOverride = MockEngine { req ->
                when (req.url.encodedPath) {
                    "/v1/profile/me" -> respond(
                        content = """{"detail":"Username already taken"}""",
                        status = HttpStatusCode.Conflict,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                    "/v1/friends" -> respond(
                        content = """{"detail":"Expired"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                    "/v1/auth/refresh" -> respond(
                        content = """{"detail":"Render waking up"}""",
                        status = HttpStatusCode.ServiceUnavailable,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                    else -> respond("{}", HttpStatusCode.OK)
                }
            }
        )

        val err = runCatching {
            apiClient.updateMyProfile(com.techenglish.app.core.network.ProfileUpdateBodyDto(username = "taken_nick"))
        }.exceptionOrNull()
        assertTrue(err is ApiHttpException)
        assertEquals(409, (err as ApiHttpException).statusCode)
        assertEquals("Username already taken", err.detail)

        // 401 -> /v1/auth/refresh returns 503 -> must NOT wipe user's session!
        runCatching { apiClient.fetchFriends() }
        val sessionAfter503 = tokenStore.sessionFlow.first()
        assertFalse("Transient 503 on refresh must never force sign-out", sessionAfter503.isGuest)
        assertEquals("audit_dev", sessionAfter503.username)
    }

    @Test
    fun `OAuth foreign deep link does not clear valid pending OAuth state`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        val handler = OAuthCallbackHandler(
            tokenStore = tokenStore,
            ticketExchanger = {
                AuthResponseDto("a1", "r1", "u1", "oauth_user", "GOOGLE", true)
            }
        )
        val authUrl = handler.prepareAuthorizeUrl(AuthProvider.GOOGLE)
        val validState = authUrl.getQueryParameter("client_state")!!

        // Forged/foreign state arrives first
        val forged = handler.handleCallbackUri(Uri.parse("techenglish://auth/callback?ticket=evil&state=wrong_state"))
        assertTrue(forged.isFailure)

        // Genuine callback right after still succeeds because pending state was NOT prematurely wiped
        val genuine = handler.handleCallbackUri(Uri.parse("techenglish://auth/callback?ticket=real_tkt&state=$validState"))
        assertTrue("Genuine callback after forged attempt should succeed", genuine.isSuccess)
        assertEquals("oauth_user", genuine.getOrThrow().username)
    }

    @Test
    fun `ProfileUiState and OnboardingProfileSetup UI display real @username instead of hardcoded dev`() {
        val withCloud = ProfileUiState(cloudUsername = "amir_kt", settings = UserSettings(localUsername = "local_kt"))
        assertEquals("amir_kt", withCloud.displayUsername)

        val withLocalOnly = ProfileUiState(cloudUsername = null, settings = UserSettings(localUsername = "offline_ninja"))
        assertEquals("offline_ninja", withLocalOnly.displayUsername)

        composeRule.setContent {
            TechEnglishTheme {
                OnboardingProfileSetupContent(
                    state = OnboardingProfileSetupUiState(
                        username = "kotlin_hero",
                        displayName = "Kotlin Hero"
                    ),
                    onBack = {},
                    onUsernameChanged = {},
                    onDisplayNameChanged = {},
                    onRoleSelected = {},
                    onDailyGoalSelected = {},
                    onTranslationLanguageSelected = {},
                    onThemeModeSelected = {},
                    onCompleteClick = {}
                )
            }
        }
        composeRule.onNodeWithText("Настройка профиля").assertIsDisplayed()
        composeRule.onNodeWithText("kotlin_hero").assertIsDisplayed()
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasText("Завершить и начать →"))
        composeRule.onNodeWithText("Завершить и начать →").assertIsDisplayed()
    }
}
