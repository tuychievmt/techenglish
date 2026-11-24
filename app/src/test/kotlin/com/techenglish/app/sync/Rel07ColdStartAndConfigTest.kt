package com.techenglish.app.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Rel07ColdStartAndConfigTest {

    private fun resolveAppFile(relativePath: String): File {
        val candidates = listOf(File(relativePath), File("app/$relativePath"))
        return candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate $relativePath from ${File(".").absolutePath}")
    }

    @Test
    fun `TechEnglishApiClient timeouts accommodate 65s free-tier cold start`() {
        assertEquals(15_000L, TechEnglishApiClient.CONNECT_TIMEOUT_MS)
        assertEquals(65_000L, TechEnglishApiClient.REQUEST_TIMEOUT_MS)
        assertEquals(65_000L, TechEnglishApiClient.SOCKET_TIMEOUT_MS)
    }

    @Test
    fun `AndroidKeystoreTokenStore allows 127_0_0_1 local base URL`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.setApiBaseUrl("http://127.0.0.1:8000/")
        assertEquals("http://127.0.0.1:8000", tokenStore.getApiBaseUrl())
    }

    @Test
    fun `BuildConfig defines API_BASE_URL, MainActivity handles Idle and Error, and SyncScreen shows v6`() {
        val gradleKts = resolveAppFile("build.gradle.kts").readText()
        assertTrue("app/build.gradle.kts must define BuildConfig.API_BASE_URL", gradleKts.contains("API_BASE_URL"))

        val mainActivity = resolveAppFile("src/main/kotlin/com/techenglish/app/MainActivity.kt").readText()
        assertTrue("MainActivity must explicitly handle DatabaseBootstrapState.Idle", mainActivity.contains("DatabaseBootstrapState.Idle"))
        assertTrue("MainActivity must explicitly handle DatabaseBootstrapState.Error", mainActivity.contains("DatabaseBootstrapState.Error"))

        val syncScreen = resolveAppFile("src/main/kotlin/com/techenglish/app/feature/stage3/CloudAccountSyncScreen.kt").readText()
        assertFalse("CloudAccountSyncScreen must not reference outdated v4", syncScreen.contains("офлайн-база v4"))
        assertTrue("CloudAccountSyncScreen must reference v6", syncScreen.contains("офлайн-база v6"))
    }
}
