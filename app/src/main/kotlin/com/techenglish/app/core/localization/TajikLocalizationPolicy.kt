package com.techenglish.app.core.localization

import com.techenglish.app.BuildConfig

/**
 * SP3-03 / BUG-20 / R7: Feature flag gate and RU fallback for Tajik UI localization.
 * Per R7, `values-tg` UI localization stays gated behind `TAJIK_UI_LOCALIZATION_ENABLED`
 * until human review of `docs/tg_review/ui_strings_tg_draft.json` is approved by the owner.
 */
object TajikLocalizationPolicy {
    val isTajikUiFeatureEnabled: Boolean
        get() = BuildConfig.TAJIK_UI_LOCALIZATION_ENABLED

    fun resolveUiLocaleTag(
        preferTajikUi: Boolean,
        featureFlagEnabled: Boolean = isTajikUiFeatureEnabled
    ): String = if (preferTajikUi && featureFlagEnabled) "tg" else "ru"

    fun resolveTermTranslation(
        translationRu: String,
        translationTg: String,
        preferTajik: Boolean
    ): String {
        if (preferTajik && translationTg.isNotBlank()) {
            return translationTg
        }
        return translationRu
    }
}
