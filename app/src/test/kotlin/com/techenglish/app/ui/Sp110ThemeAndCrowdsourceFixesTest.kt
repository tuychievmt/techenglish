package com.techenglish.app.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.designsystem.CodeSyntaxHighlighter
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.TechEnglishTheme
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.feature.stage4b.CrowdsourcePrViewModel
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.model.ThemeMode
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Sp110ThemeAndCrowdsourceFixesTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `code highlighting follows active app ThemeMode instead of system dark theme`() {
        var darkFlagTokyoNight: Boolean? = null
        var darkFlagLight: Boolean? = null

        composeRule.setContent {
            TechEnglishTheme(themeMode = ThemeMode.TOKYO_NIGHT, dynamicColor = false) {
                darkFlagTokyoNight = LocalTechInteractionConfig.current.isDarkTheme
            }
            TechEnglishTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
                darkFlagLight = LocalTechInteractionConfig.current.isDarkTheme
            }
        }
        composeRule.waitForIdle()

        assertEquals(true, darkFlagTokyoNight)
        assertEquals(false, darkFlagLight)

        val darkHighlighted = CodeSyntaxHighlighter.highlightLine("fun main() = println(\"hi\")", "kotlin", true, emptyList())
        val lightHighlighted = CodeSyntaxHighlighter.highlightLine("fun main() = println(\"hi\")", "kotlin", false, emptyList())
        assertNotEquals(
            "Dark and light syntax highlighting spans must differ",
            darkHighlighted.spanStyles.firstOrNull()?.item?.color,
            lightHighlighted.spanStyles.firstOrNull()?.item?.color
        )

        val progDir = listOf(
            File("src/main/kotlin/com/techenglish/app/feature/programming"),
            File("app/src/main/kotlin/com/techenglish/app/feature/programming")
        ).first { it.isDirectory }
        val progSource = progDir.listFiles { f -> f.extension == "kt" }
            .orEmpty()
            .joinToString("\n") { it.readText() }
        assertFalse(
            "feature/programming/*.kt must not use isSystemInDarkTheme() directly (BUG-14)",
            progSource.contains("isSystemInDarkTheme()")
        )
    }

    @Test
    fun `crowdsource report uses DICTIONARY_PR targetType and eligibility fetches server endpoint`() = runBlocking {
        assertEquals("DICTIONARY_PR", CrowdsourcePrViewModel.PR_REPORT_TARGET_TYPE)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        tokenStore.setApiBaseUrl("https://api.techenglish.app")
        tokenStore.saveTokensAndSession(
            accessToken = "jwt_tok_110",
            refreshToken = "jwt_ref_110",
            userId = "u-110",
            username = "contrib_110",
            email = "c110@example.com",
            provider = AuthProvider.EMAIL
        )

        var capturedReportBody: String? = null
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/crowdsource/eligibility" -> respond(
                    content = """
                        {
                          "is_eligible": true,
                          "meets_rank_middle_plus": true,
                          "meets_account_age_14_days": true,
                          "meets_learned_terms_100": true,
                          "meets_zero_confirmed_reports": true,
                          "total_xp": 850,
                          "account_age_days": 21,
                          "learned_terms_count": 115,
                          "confirmed_reports": 0,
                          "unmet_reasons_ru": []
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/v1/moderation/report" -> {
                    capturedReportBody = (request.body as? TextContent)?.text
                    respond(
                        content = """{"report_id":"r1","status":"OPEN","target_type":"DICTIONARY_PR","auto_hidden":false}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("""{"detail":"not found"}""", HttpStatusCode.NotFound)
            }
        }

        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = mockEngine)
        val elig = apiClient.fetchCrowdsourceEligibility()
        assertTrue(elig.isEligible)
        assertEquals(21, elig.accountAgeDays)
        assertEquals(115, elig.learnedTermsCount)
        assertEquals(850, elig.totalXp)

        apiClient.reportContent(CrowdsourcePrViewModel.PR_REPORT_TARGET_TYPE, "pr-123", "incorrect translation")
        assertTrue(
            "Report DTO must send target_type=DICTIONARY_PR, got: $capturedReportBody",
            capturedReportBody?.contains(""""target_type":"DICTIONARY_PR"""") == true
        )
    }
}
