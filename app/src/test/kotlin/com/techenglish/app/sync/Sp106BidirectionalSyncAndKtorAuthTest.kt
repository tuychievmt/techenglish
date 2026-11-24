package com.techenglish.app.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.network.Sm2ItemPayloadDto
import com.techenglish.app.core.network.SyncBatchBodyDto
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Sp106BidirectionalSyncAndKtorAuthTest {

    @Test
    fun `ktor client automatically refreshes jwt on 401 and retries GET sync state`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        tokenStore.setApiBaseUrl("https://api.techenglish.app")
        tokenStore.saveTokensAndSession(
            accessToken = "expired_access_token",
            refreshToken = "valid_refresh_token",
            userId = "u-42",
            username = "sync_dev",
            email = "sync@example.com",
            provider = AuthProvider.EMAIL
        )

        var syncStateCalls = 0
        var refreshCalls = 0
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            val authHeader = request.headers[HttpHeaders.Authorization]
            when {
                path == "/v1/sync/state" && authHeader == "Bearer expired_access_token" -> {
                    syncStateCalls++
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
                              "user_id": "u-42",
                              "username": "sync_dev",
                              "provider": "EMAIL",
                              "is_email_verified": true
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                path == "/v1/sync/state" && authHeader == "Bearer fresh_access_token" -> {
                    syncStateCalls++
                    respond(
                        content = """
                            {
                              "user_id": "u-42",
                              "server_total_xp": 320,
                              "rank_title": "Junior Developer",
                              "league": "BRONZE",
                              "sm2_states": [
                                {
                                  "normalized_term": "idempotency",
                                  "repetitions": 3,
                                  "ease_factor": 2.6,
                                  "interval_days": 10,
                                  "next_review_at_ms": 1800000000000,
                                  "client_timestamp_ms": 1799999000000,
                                  "server_received_ms": 1799999001000
                                }
                              ],
                              "favorite_terms": ["idempotency"],
                              "custom_words": {"singleflight": "объединение дублирующих запросов"}
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond(
                    content = """{"detail":"Unexpected request $path auth=$authHeader"}""",
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

        val state = apiClient.fetchSyncState()
        assertEquals(1, refreshCalls)
        assertEquals(2, syncStateCalls)
        assertEquals("u-42", state.userId)
        assertEquals(1, state.sm2States.size)
        assertEquals("idempotency", state.sm2States.first().normalizedTerm)
        assertEquals(listOf("idempotency"), state.favoriteTerms)
        assertEquals("объединение дублирующих запросов", state.customWords["singleflight"])
        assertEquals("fresh_access_token", tokenStore.getAccessToken())
        assertEquals("fresh_refresh_token", tokenStore.getRefreshToken())
    }

    @Test
    fun `sync batch dto carries sm2_updates favorite_terms and custom_words`() {
        val batch = SyncBatchBodyDto(
            events = emptyList(),
            sm2Updates = listOf(
                Sm2ItemPayloadDto(
                    normalizedTerm = "deadlock",
                    repetitions = 4,
                    easeFactor = 2.6f,
                    intervalDays = 15,
                    nextReviewAtMs = 1_800_000_000_000L,
                    clientTimestampMs = 1_799_999_000_000L
                )
            ),
            favoriteTerms = listOf("deadlock"),
            customWords = mapOf("backpressure" to "обратное давление")
        )
        assertEquals(1, batch.sm2Updates.size)
        assertTrue(batch.favoriteTerms.contains("deadlock"))
        assertEquals("обратное давление", batch.customWords["backpressure"])
    }
}
