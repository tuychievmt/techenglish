package com.techenglish.app.update

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.datastore.UserPreferencesDataStore
import com.techenglish.app.core.designsystem.TechEnglishTheme
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.core.update.ApkDownloadState
import com.techenglish.app.core.update.AppUpdateGateManager
import com.techenglish.app.core.update.UpdateGateState
import com.techenglish.app.feature.update.ForceUpdateScreen
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppUpdateGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var userPrefs: UserPreferencesDataStore
    private lateinit var tokenStore: AndroidKeystoreTokenStore

    @Before
    fun setUp(): Unit = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        userPrefs = UserPreferencesDataStore(context)
        tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.setApiBaseUrl("https://api.techenglish.dev")
        userPrefs.saveCachedUpdateMetadata(
            minSupportedVersionCode = 0,
            latestVersionCode = 0,
            latestVersionName = "",
            latestApkUrl = "",
            apkSha256 = "",
            changelogRu = ""
        )
        userPrefs.setOptionalUpdateSnoozedUntil(0L)
        File(context.cacheDir, AppUpdateGateManager.APK_CACHE_SUBDIR).deleteRecursively()
        Unit
    }

    @Test
    fun `currentCode below minSupported resolves to Required and renders ForceUpdateScreen`() {
        runBlocking {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/app/version" -> respond(
                    content = """
                        {
                          "latest_version_code": 325,
                          "latest_version_name": "3.2.5",
                          "min_supported_version_code": 320,
                          "force_update": false,
                          "apk_url": "https://cdn.techenglish.dev/app-3.2.5.apk",
                          "apk_sha256": "aa11bb22",
                          "changelog_ru": "• Обязательное обновление протокола синхронизации",
                          "extra_unknown_field": "ignored_safely"
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("{}", HttpStatusCode.NotFound)
            }
        }
        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = mockEngine)
        val manager = AppUpdateGateManager(
            context = context,
            apiClient = apiClient,
            userPrefs = userPrefs,
            appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            currentVersionCode = 310,
            currentVersionName = "3.1.0",
            fallbackManifestUrl = "https://raw.example.com/ota/latest.json",
            clockMs = { 1_000_000L }
        )

        val state = manager.checkForUpdates(force = true)
        assertTrue("Expected Required state, got $state", state is UpdateGateState.Required)
        val req = state as UpdateGateState.Required
        assertEquals(320, req.minSupportedVersionCode)
        assertEquals("3.2.5", req.latestVersionName)

        composeRule.setContent {
            TechEnglishTheme {
                ForceUpdateScreen(
                    state = req,
                    downloadState = ApkDownloadState.Idle,
                    onDownloadAndInstall = {},
                    onOpenBrowser = {}
                )
            }
        }
        composeRule.onNodeWithText("ТРЕБУЕТСЯ ОБНОВЛЕНИЕ").assertIsDisplayed()
        composeRule.onNodeWithText("Версия устарела").assertIsDisplayed()
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasText("Скачать и установить →"))
        composeRule.onNodeWithText("Скачать и установить →").assertIsDisplayed()
        }
    }

    @Test
    fun `minSupported at or below currentCode and currentCode below latest resolves to Optional and snoozes for 24h`() {
        runBlocking {
        var nowMs = 10_000_000L
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath == "/v1/app/version" -> respond(
                    // Simulate Render sleeping (503) to also verify fallback to static ota/latest.json!
                    content = """{"detail":"Service waking up"}""",
                    status = HttpStatusCode.ServiceUnavailable
                )
                request.url.toString() == "https://raw.example.com/ota/latest.json" -> respond(
                    content = """
                        {
                          "latest_version_code": 315,
                          "latest_version_name": "3.1.5",
                          "min_supported_version_code": 310,
                          "force_update": false,
                          "apk_url": "https://cdn.techenglish.dev/app-3.1.5.apk",
                          "apk_sha256": "",
                          "changelog_ru": "• Улучшен тренажёр произношения"
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8")
                )
                else -> respond("{}", HttpStatusCode.NotFound)
            }
        }
        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = mockEngine)
        val manager = AppUpdateGateManager(
            context = context,
            apiClient = apiClient,
            userPrefs = userPrefs,
            appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            currentVersionCode = 310,
            currentVersionName = "3.1.0",
            fallbackManifestUrl = "https://raw.example.com/ota/latest.json",
            clockMs = { nowMs }
        )

        val state = manager.checkForUpdates(force = true)
        assertTrue("Expected Optional state, got $state", state is UpdateGateState.Optional)
        assertEquals(315, (state as UpdateGateState.Optional).latestVersionCode)

        // Snoozing hides Optional for 24 hours
        manager.snoozeOptionalFor24Hours()
        assertEquals(UpdateGateState.UpToDate, manager.gateState.value)

        nowMs += 60 * 60 * 1000L // +1 hour (< 24h)
        assertEquals(UpdateGateState.UpToDate, manager.checkForUpdates(force = true))

        nowMs += 24 * 60 * 60 * 1000L // > 24 hours elapsed
        assertTrue(manager.checkForUpdates(force = true) is UpdateGateState.Optional)
        }
    }

    @Test
    fun `offline start after previously cached minSupported above currentCode stays Required`() {
        runBlocking {
        // Pre-populate DataStore as if a previous online session recorded min_supported = 330
        userPrefs.saveCachedUpdateMetadata(
            minSupportedVersionCode = 330,
            latestVersionCode = 330,
            latestVersionName = "3.3.0",
            latestApkUrl = "https://cdn.techenglish.dev/app-3.3.0.apk",
            apkSha256 = "deadbeef",
            changelogRu = "• Критический патч безопасности"
        )

        val offlineEngine = MockEngine {
            throw IOException("Airplane mode: network unreachable")
        }
        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = offlineEngine)
        val manager = AppUpdateGateManager(
            context = context,
            apiClient = apiClient,
            userPrefs = userPrefs,
            appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            currentVersionCode = 310,
            currentVersionName = "3.1.0",
            fallbackManifestUrl = "https://raw.example.com/ota/latest.json",
            clockMs = { 50_000L }
        )

        val state = manager.checkForUpdates(force = true)
        assertTrue("Offline start must remain Required when cachedMin > currentCode, got $state", state is UpdateGateState.Required)
        val req = state as UpdateGateState.Required
        assertEquals(330, req.minSupportedVersionCode)
        assertEquals("3.3.0", req.latestVersionName)
        }
    }

    @Test
    fun `offline start on up-to-date version resolves to UpToDate and never blocks offline usage`() {
        runBlocking {
        userPrefs.saveCachedUpdateMetadata(
            minSupportedVersionCode = 310,
            latestVersionCode = 310,
            latestVersionName = "3.1.0",
            latestApkUrl = "https://cdn.techenglish.dev/app-3.1.0.apk",
            apkSha256 = "",
            changelogRu = ""
        )

        val offlineEngine = MockEngine {
            throw IOException("No internet connection")
        }
        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = offlineEngine)
        val manager = AppUpdateGateManager(
            context = context,
            apiClient = apiClient,
            userPrefs = userPrefs,
            appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            currentVersionCode = 310,
            currentVersionName = "3.1.0",
            fallbackManifestUrl = "https://raw.example.com/ota/latest.json",
            clockMs = { 90_000L }
        )

        val state = manager.checkForUpdates(force = true)
        assertEquals(UpdateGateState.UpToDate, state)
        }
    }

    @Test
    fun `SHA-256 mismatch rejects downloaded APK with Error without launching installer while valid SHA-256 succeeds`() {
        runBlocking {
        val fakeApkBytes = "FAKE_SIGNED_ANDROID_APK_BINARY_PAYLOAD_V320".encodeToByteArray()
        val realSha256 = MessageDigest.getInstance("SHA-256")
            .digest(fakeApkBytes)
            .joinToString("") { "%02x".format(it) }

        val mockEngine = MockEngine { request ->
            if (request.url.toString() == "https://cdn.techenglish.dev/update.apk") {
                respond(
                    content = fakeApkBytes,
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf("application/vnd.android.package-archive"),
                        HttpHeaders.ContentLength to listOf(fakeApkBytes.size.toString())
                    )
                )
            } else {
                respond("{}", HttpStatusCode.NotFound)
            }
        }
        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = mockEngine)
        val manager = AppUpdateGateManager(
            context = context,
            apiClient = apiClient,
            userPrefs = userPrefs,
            appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            currentVersionCode = 310,
            currentVersionName = "3.1.0",
            fallbackManifestUrl = "",
            clockMs = { 120_000L }
        )

        // 1. Mismatched SHA-256 must fail and delete the file
        val badResult = manager.downloadAndVerifyApk(
            apkUrl = "https://cdn.techenglish.dev/update.apk",
            expectedSha256 = "0000000000000000000000000000000000000000000000000000000000000000"
        )
        assertTrue("Mismatched SHA-256 must fail", badResult.isFailure)
        val errState = manager.downloadState.value
        assertTrue("Expected ApkDownloadState.Error, got $errState", errState is ApkDownloadState.Error)
        assertTrue((errState as ApkDownloadState.Error).messageRu.contains("SHA-256"))
        val targetApkFile = File(File(context.cacheDir, AppUpdateGateManager.APK_CACHE_SUBDIR), AppUpdateGateManager.APK_FILE_NAME)
        assertFalse("Corrupt/tampered APK must not remain on disk", targetApkFile.exists())

        // 2. Valid SHA-256 succeeds and prepares ReadyToInstall
        val goodResult = manager.downloadAndVerifyApk(
            apkUrl = "https://cdn.techenglish.dev/update.apk",
            expectedSha256 = realSha256.uppercase()
        )
        assertTrue("Matching SHA-256 should succeed", goodResult.isSuccess)
        val readyState = manager.downloadState.value
        assertTrue("Expected ReadyToInstall, got $readyState", readyState is ApkDownloadState.ReadyToInstall)
        assertTrue(targetApkFile.exists())
        assertEquals(fakeApkBytes.size.toLong(), targetApkFile.length())
        }
    }
}
