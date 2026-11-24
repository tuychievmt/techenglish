package com.techenglish.app.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.domain.model.AuthProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Gap06KtorAuthRefreshHeadersTest {

    @Test
    fun `fetchFriends refreshes expired JWT on 401 without duplicate Authorization header`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        tokenStore.setApiBaseUrl("https://api.techenglish.app")
        tokenStore.saveTokensAndSession(
            accessToken = "expired_access_token",
            refreshToken = "valid_refresh_token",
            userId = "u-gap06",
            username = "gap06_dev",
            email = "gap06@example.com",
            provider = AuthProvider.EMAIL
        )

        var friendsCalls = 0
        var refreshCalls = 0
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            val authHeaders = request.headers.getAll(HttpHeaders.Authorization).orEmpty()
            when {
                path == "/v1/friends" && authHeaders == listOf("Bearer expired_access_token") -> {
                    friendsCalls++
                    respond(
                        content = """{"detail":"Token expired"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                path == "/v1/auth/refresh" -> {
                    refreshCalls++
                    respond(
                        content = """
                            {
                              "access_token": "fresh_access_token",
                              "refresh_token": "fresh_refresh_token",
                              "user_id": "u-gap06",
                              "username": "gap06_dev",
                              "provider": "EMAIL",
                              "is_email_verified": true
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                path == "/v1/friends" && authHeaders == listOf("Bearer fresh_access_token") -> {
                    friendsCalls++
                    respond(
                        content = """{"friends":[]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond(
                    content = """{"detail":"Unexpected request $path authHeaders=$authHeaders"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val apiClient = TechEnglishApiClient(
            context = context,
            tokenStore = tokenStore,
            engineOverride = mockEngine
        )

        val friends = apiClient.fetchFriends()
        assertEquals(0, friends.friends.size)
        assertEquals(1, refreshCalls)
        assertEquals(2, friendsCalls)
        assertEquals("fresh_access_token", tokenStore.getAccessToken())
    }

    @Test
    fun `fetchAiQuota refreshes expired JWT on 401 without duplicate Authorization header`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        tokenStore.setApiBaseUrl("https://api.techenglish.app")
        tokenStore.saveTokensAndSession(
            accessToken = "expired_ai_token",
            refreshToken = "valid_refresh_token",
            userId = "u-gap06",
            username = "gap06_dev",
            email = "gap06@example.com",
            provider = AuthProvider.EMAIL
        )

        var quotaCalls = 0
        var refreshCalls = 0
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            val authHeaders = request.headers.getAll(HttpHeaders.Authorization).orEmpty()
            when {
                path == "/v1/ai/quota" && authHeaders == listOf("Bearer expired_ai_token") -> {
                    quotaCalls++
                    respond(
                        content = """{"detail":"Token expired"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                path == "/v1/auth/refresh" -> {
                    refreshCalls++
                    respond(
                        content = """
                            {
                              "access_token": "fresh_ai_token",
                              "refresh_token": "fresh_refresh_token",
                              "user_id": "u-gap06",
                              "username": "gap06_dev",
                              "provider": "EMAIL",
                              "is_email_verified": true
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                path == "/v1/ai/quota" && authHeaders == listOf("Bearer fresh_ai_token") -> {
                    quotaCalls++
                    respond(
                        content = """{"used_today":1,"daily_limit":10,"remaining_today":9}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond(
                    content = """{"detail":"Unexpected request $path authHeaders=$authHeaders"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val apiClient = TechEnglishApiClient(
            context = context,
            tokenStore = tokenStore,
            engineOverride = mockEngine
        )

        val quota = apiClient.fetchAiQuota()
        assertEquals(9, quota.remainingToday)
        assertEquals(1, refreshCalls)
        assertEquals(2, quotaCalls)
    }

    @Test
    fun `external OTA download does not leak Authorization bearer header`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        tokenStore.setApiBaseUrl("https://api.techenglish.app")
        tokenStore.saveTokensAndSession(
            accessToken = "secret_jwt_token",
            refreshToken = "valid_refresh_token",
            userId = "u-gap06",
            username = "gap06_dev",
            email = "gap06@example.com",
            provider = AuthProvider.EMAIL
        )

        var leakedAuthHeader: String? = "UNSET"
        val mockEngine = MockEngine { request ->
            leakedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"pack_id":"ext_1","version":1,"terms":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val apiClient = TechEnglishApiClient(
            context = context,
            tokenStore = tokenStore,
            engineOverride = mockEngine
        )

        val payload = apiClient.fetchExternalOtaPayload("https://cdn.example.com/packs/ext_1.json")
        assertEquals("""{"pack_id":"ext_1","version":1,"terms":[]}""", payload)
        assertEquals("External OTA URL must not receive Authorization header", null, leakedAuthHeader)
    }

    @Test
    fun `TechEnglishApiClient delegates Bearer auth solely to Ktor Auth plugin without manual headers`() {
        val candidates = listOf(
            java.io.File("src/main/kotlin/com/techenglish/app/core/network/TechEnglishApiClient.kt"),
            java.io.File("app/src/main/kotlin/com/techenglish/app/core/network/TechEnglishApiClient.kt")
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Could not locate TechEnglishApiClient.kt from ${java.io.File(".").absolutePath}")
        val manualAuthLines = sourceFile.readLines().withIndex()
            .filter { it.value.contains("header(HttpHeaders.Authorization") }
            .map { "${it.index + 1}: ${it.value.trim()}" }
        assertEquals(
            "Manual Authorization headers conflict with Ktor Auth bearer refresh plugin",
            emptyList<String>(),
            manualAuthLines
        )
    }
}
