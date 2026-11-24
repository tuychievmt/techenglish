package com.techenglish.app.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.BuildConfig
import com.techenglish.app.core.network.AiCodeReviewRequestDto
import com.techenglish.app.core.network.AiInterviewRequestDto
import com.techenglish.app.core.network.LoginBodyDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
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
    fun `TechEnglishApiClient timeouts and Render base URL accommodate free-tier cold start`() {
        assertEquals("https://techenglish-trwk.onrender.com", BuildConfig.API_BASE_URL)
        assertEquals(BuildConfig.API_BASE_URL, TechEnglishApiClient.DEFAULT_BASE_URL)
        assertEquals(30_000L, TechEnglishApiClient.CONNECT_TIMEOUT_MS)
        assertEquals(60_000L, TechEnglishApiClient.READ_TIMEOUT_MS)
        assertEquals(60_000L, TechEnglishApiClient.WRITE_TIMEOUT_MS)
        assertEquals(60_000L, TechEnglishApiClient.CALL_TIMEOUT_MS)
        assertEquals(60_000L, TechEnglishApiClient.REQUEST_TIMEOUT_MS)
        assertEquals(60_000L, TechEnglishApiClient.SOCKET_TIMEOUT_MS)
    }

    @Test
    fun `login access_token is forwarded via Bearer interceptor to AI endpoints with consent and fallback handling`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()

        var interviewBodyJson = ""
        var codeReviewBodyJson = ""
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            val authHeader = request.headers[HttpHeaders.Authorization]
            when {
                path == "/v1/auth/login" -> respond(
                    content = """
                        {
                          "access_token": "render_jwt_token_123",
                          "refresh_token": "render_refresh_token_456",
                          "user_id": "u-render-1",
                          "username": "render_dev",
                          "provider": "EMAIL",
                          "is_email_verified": true
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                path == "/v1/ai/interview" && authHeader == "Bearer render_jwt_token_123" -> {
                    interviewBodyJson = (request.body as? TextContent)?.text.orEmpty()
                    respond(
                        content = """
                            {
                              "provider": "heuristic-fallback",
                              "is_fallback": true,
                              "fallback_reason": "gemini_api_key_not_configured",
                              "question_en": "How do you ensure idempotency?",
                              "english_score": 82,
                              "technical_score": 88,
                              "feedback_ru": "Хорошее использование профильной лексики.",
                              "improved_answer_en": "In production, I use idempotency keys.",
                              "next_question_en": "When would you choose optimistic locking?",
                              "remaining_daily_quota": 14
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                path == "/v1/ai/code-review" && authHeader == "Bearer render_jwt_token_123" -> {
                    codeReviewBodyJson = (request.body as? TextContent)?.text.orEmpty()
                    respond(
                        content = """
                            {
                              "provider": "heuristic-fallback",
                              "is_fallback": true,
                              "fallback_reason": "gemini_timeout",
                              "tone_assessment_ru": "Конструктивный и профессиональный тон.",
                              "improved_english_text": "Could we extract this helper?",
                              "key_suggestions_ru": ["Добавьте обоснование."],
                              "remaining_daily_quota": 13
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond(
                    content = """{"detail":"Unexpected $path auth=$authHeader"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val apiClient = TechEnglishApiClient(
            context = context,
            tokenStore = tokenStore,
            engineOverride = mockEngine
        )

        val loginResp = apiClient.login(LoginBodyDto(email = "dev@example.com", password = "password123"))
        assertEquals("render_jwt_token_123", loginResp.accessToken)

        val interviewResp = apiClient.submitAiInterview(
            AiInterviewRequestDto(
                role = "backend",
                level = "middle",
                questionEn = "How do you ensure idempotency?",
                userAnswerText = "I use idempotency keys and database constraints."
            )
        )
        assertTrue(interviewBodyJson.contains("\"consent_accepted\":true"))
        assertEquals(true, interviewResp.isFallback)
        assertEquals("gemini_api_key_not_configured", interviewResp.fallbackReason)

        val reviewResp = apiClient.submitAiCodeReview(
            AiCodeReviewRequestDto(draftText = "Please extract this function to avoid duplication.")
        )
        assertTrue(codeReviewBodyJson.contains("\"consent_accepted\":true"))
        assertEquals(true, reviewResp.isFallback)
        assertEquals("gemini_timeout", reviewResp.fallbackReason)
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
