package com.techenglish.app.localization

import com.techenglish.app.BuildConfig
import com.techenglish.app.core.localization.TajikLocalizationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Sp303TajikLocalizationPolicyTest {

    private fun findProjectRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, "settings.gradle.kts").exists()) cwd else cwd.parentFile
    }

    @Test
    fun `values-tg exists behind feature flag with RU fallback and all review drafts are pending_review`() {
        val root = findProjectRoot()

        // 1. Resource bucket & feature flag gate (R7)
        val tgStrings = File(root, "app/src/main/res/values-tg/strings.xml")
        assertTrue("Expected app/src/main/res/values-tg/strings.xml", tgStrings.exists())
        assertFalse("Tajik UI feature flag must stay disabled by default until human review approval", BuildConfig.TAJIK_UI_LOCALIZATION_ENABLED)

        assertEquals("ru", TajikLocalizationPolicy.resolveUiLocaleTag(preferTajikUi = true, featureFlagEnabled = false))
        assertEquals("tg", TajikLocalizationPolicy.resolveUiLocaleTag(preferTajikUi = true, featureFlagEnabled = true))
        assertEquals("кэширование", TajikLocalizationPolicy.resolveTermTranslation("кэширование", "", preferTajik = true))
        assertEquals("кешкунӣ", TajikLocalizationPolicy.resolveTermTranslation("кэширование", "кешкунӣ", preferTajik = true))

        // 2. Review drafts in docs/tg_review/ strictly marked pending_review (no machine-translated verified claims!)
        val uiDraft = File(root, "docs/tg_review/ui_strings_tg_draft.json")
        val termsDraft = File(root, "docs/tg_review/terms_tg_draft.csv")
        assertTrue(uiDraft.exists())
        assertTrue(termsDraft.exists())

        val uiJson = uiDraft.readText()
        assertTrue(uiJson.contains("\"pending_review\""))
        assertFalse("UI draft must not mark unreviewed machine translations as verified", uiJson.contains("\"status\": \"verified\""))

        val csvLines = termsDraft.readLines().drop(1).filter { it.isNotBlank() }
        assertTrue("Expected >= 100 pending_review draft terms for owner review", csvLines.size >= 100)
        assertTrue(
            "Every draft row in terms_tg_draft.csv must stay pending_review until human review",
            csvLines.all { it.contains(",pending_review,") }
        )
    }
}
