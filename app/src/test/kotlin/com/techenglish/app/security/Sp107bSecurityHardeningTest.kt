package com.techenglish.app.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.core.security.TokenCipherKeyProvider
import com.techenglish.domain.model.AuthProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.SecretKey

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Sp107bSecurityHardeningTest {

    @Test
    fun `manifest has allowBackup false and does not declare CAMERA permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertFalse("CAMERA permission must be removed (BUG-12)", "android.permission.CAMERA" in permissions)

        val appInfo = pm.getApplicationInfo(context.packageName, 0)
        val allowBackup = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        assertFalse("allowBackup must be false (BUG-10)", allowBackup)
    }

    @Test
    fun `release network security config forbids cleartext and debug allows only localhost and 10_0_2_2`() {
        val mainConfig = listOf(
            File("src/main/res/xml/network_security_config.xml"),
            File("app/src/main/res/xml/network_security_config.xml")
        ).first { it.exists() }.readText()
        assertTrue(mainConfig.contains("""cleartextTrafficPermitted="false""""))
        assertFalse("Release config must not allow cleartext", mainConfig.contains("""cleartextTrafficPermitted="true""""))

        val debugConfigFile = listOf(
            File("src/debug/res/xml/network_security_config.xml"),
            File("app/src/debug/res/xml/network_security_config.xml")
        ).firstOrNull { it.exists() }
        assertTrue("Debug network_security_config.xml must exist", debugConfigFile != null)
        val debugConfig = debugConfigFile!!.readText()
        assertTrue(debugConfig.contains("10.0.2.2"))
        assertTrue(debugConfig.contains("localhost"))
    }

    @Test
    fun `keystore token store never writes PLAIN prefix and resets key plus soft-logs out on keystore failure`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Normal encryption produces GCM:, never PLAIN:, and rejects legacy PLAIN: blobs
        val normalStore = AndroidKeystoreTokenStore(context)
        val encrypted = normalStore.encrypt("jwt.secret.token")
        assertTrue("Expected GCM ciphertext, got: $encrypted", encrypted.startsWith("GCM:"))
        assertFalse(encrypted.startsWith("PLAIN:"))
        assertNull("Legacy PLAIN: blobs must be rejected", normalStore.decrypt("PLAIN:c2VjcmV0"))

        // 2. Keystore failure triggers key reset + soft session logout (no plaintext fallback!)
        var resetCalls = 0
        val brokenProvider = object : TokenCipherKeyProvider {
            override fun getOrCreateKey(): SecretKey {
                throw IllegalStateException("Simulated Keystore permanent invalidation")
            }

            override fun resetKey() {
                resetCalls++
            }
        }
        val failingStore = AndroidKeystoreTokenStore(context, brokenProvider)
        failingStore.saveTokensAndSession(
            accessToken = "tok1",
            refreshToken = "ref1",
            userId = "u1",
            username = "dev",
            email = "dev@example.com",
            provider = AuthProvider.EMAIL
        )
        assertTrue("Key reset should be attempted on Keystore failure", resetCalls >= 1)
        assertTrue("Session must softly revert to guest on Keystore failure", failingStore.sessionFlow.first().isGuest)
        assertNull(failingStore.getAccessToken())
    }

    @Test
    fun `api client requests single-use ws ticket and builds ticket URL without jwt token query param`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        tokenStore.setApiBaseUrl("https://api.techenglish.app")
        tokenStore.saveTokensAndSession(
            accessToken = "jwt_access_secret",
            refreshToken = "jwt_refresh_secret",
            userId = "u-ws",
            username = "ws_dev",
            email = "ws@example.com",
            provider = AuthProvider.EMAIL
        )

        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath == "/v1/ws/ticket") {
                respond(
                    content = """{"ticket":"one_time_redis_ticket_777","expires_in_seconds":30}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("""{"detail":"not found"}""", HttpStatusCode.NotFound)
            }
        }

        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = mockEngine)
        val ticketDto = apiClient.requestWsTicket("rooms")
        assertEquals("one_time_redis_ticket_777", ticketDto.ticket)

        val wsUrl = apiClient.buildRoomWsUrl("https://api.techenglish.app", "backend-english", ticketDto.ticket)
        assertEquals("wss://api.techenglish.app/ws/rooms/backend-english?ticket=one_time_redis_ticket_777", wsUrl)
        assertFalse(wsUrl.contains("token="))
        assertFalse(wsUrl.contains("jwt_access_secret"))
    }
}
