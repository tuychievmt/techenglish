package com.techenglish.app.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.designsystem.CodeSyntaxHighlighter
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.TechEnglishTheme
import com.techenglish.app.core.designsystem.calculateContrastRatio
import com.techenglish.app.core.designsystem.resolveColorSchemeForMode
import com.techenglish.app.core.designsystem.resolveHeroPalette
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

    @Test
    fun `all 7 themes pass WCAG AA contrast for Hero card, CTA buttons, and surfaceContainer levels`() {
        val cases = ThemeMode.entries.flatMap { mode ->
            if (mode == ThemeMode.SYSTEM) {
                listOf(mode to false, mode to true)
            } else {
                listOf(mode to mode.isDarkPalette)
            }
        }

        for ((mode, systemDark) in cases) {
            val scheme = resolveColorSchemeForMode(mode, systemDark)
            val hero = resolveHeroPalette(mode, systemDark)
            val tag = "${mode.name}(dark=$systemDark)"

            assertNotEquals(
                "$tag must define distinct 5-level surfaceContainerLowest..Highest",
                scheme.surfaceContainerLowest,
                scheme.surfaceContainerHighest
            )

            val containerChecks = listOf(
                "onBackground/background" to (scheme.onBackground to scheme.background),
                "onSurface/surface" to (scheme.onSurface to scheme.surface),
                "onSurface/surfaceContainerLowest" to (scheme.onSurface to scheme.surfaceContainerLowest),
                "onSurface/surfaceContainerLow" to (scheme.onSurface to scheme.surfaceContainerLow),
                "onSurface/surfaceContainer" to (scheme.onSurface to scheme.surfaceContainer),
                "onSurface/surfaceContainerHigh" to (scheme.onSurface to scheme.surfaceContainerHigh),
                "onSurface/surfaceContainerHighest" to (scheme.onSurface to scheme.surfaceContainerHighest),
                "onSurfaceVariant/surfaceContainer" to (scheme.onSurfaceVariant to scheme.surfaceContainer),
                "onSurfaceVariant/surfaceContainerHigh" to (scheme.onSurfaceVariant to scheme.surfaceContainerHigh),
                "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
                "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
                "onSecondary/secondary" to (scheme.onSecondary to scheme.secondary),
                "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
                "onTertiary/tertiary" to (scheme.onTertiary to scheme.tertiary),
                "onTertiaryContainer/tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
                "onError/error" to (scheme.onError to scheme.error),
                "onErrorContainer/errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
                "hero.onHero/gradientStart" to (hero.onHero to hero.gradientStart),
                "hero.onHero/gradientEnd" to (hero.onHero to hero.gradientEnd),
                "hero.onHeroMuted/gradientStart" to (hero.onHeroMuted to hero.gradientStart),
                "hero.onHeroMuted/gradientEnd" to (hero.onHeroMuted to hero.gradientEnd),
                "hero.onCta/ctaBackground" to (hero.onCta to hero.ctaBackground)
            )

            for ((pairName, colors) in containerChecks) {
                val ratio = calculateContrastRatio(colors.first, colors.second)
                assertTrue("$tag $pairName contrast $ratio < 4.5", ratio >= 4.5)
            }
        }
    }
}
