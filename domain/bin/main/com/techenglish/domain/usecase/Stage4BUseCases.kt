package com.techenglish.domain.usecase

import com.techenglish.domain.model.CrowdsourceEligibility
import com.techenglish.domain.model.SquadGoalEvaluation
import java.util.Locale
import javax.inject.Inject

// =========================================================================
// ФТ-8.1–8.3 — AI Prompt PII Scrubber (Zero Profile PII to LLM)
// =========================================================================

class SanitizeAiPromptPiiUseCase @Inject constructor() {

    operator fun invoke(rawText: String): String {
        var sanitized = rawText.trim()
        sanitized = EMAIL_REGEX.replace(sanitized, "[REDACTED_EMAIL]")
        sanitized = PHONE_REGEX.replace(sanitized, "[REDACTED_PHONE]")
        sanitized = HANDLE_REGEX.replace(sanitized, "@user")
        sanitized = TOKEN_QUERY_REGEX.replace(sanitized, "$1[REDACTED_TOKEN]")
        return sanitized.take(MAX_PROMPT_CHARS)
    }

    companion object {
        const val MAX_PROMPT_CHARS = 2_000
        private val EMAIL_REGEX = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        private val PHONE_REGEX = Regex("(?<!\\d)(?:\\+?\\d[\\d\\s\\-()]{7,}\\d)(?!\\d)")
        private val HANDLE_REGEX = Regex("(?<!\\w)@[a-zA-Z][a-zA-Z0-9_]{2,31}")
        private val TOKEN_QUERY_REGEX = Regex("(?i)(token=|api_key=|authorization:\\s*bearer\\s+)\\S+")
    }
}

// =========================================================================
// ФТ-14.4 vs ФТ-9.3 — Squads Weekly Goal & Strictly Non-Ranked Multiplier
// =========================================================================

class EvaluateSquadWeeklyGoalUseCase @Inject constructor() {

    operator fun invoke(
        memberCount: Int,
        currentWeekCollectiveXp: Int,
        weeklyGoalXp: Int = DEFAULT_WEEKLY_GOAL_XP
    ): SquadGoalEvaluation {
        val validSize = memberCount in MIN_SQUAD_MEMBERS..MAX_SQUAD_MEMBERS
        val reached = validSize && weeklyGoalXp > 0 && currentWeekCollectiveXp >= weeklyGoalXp
        val multiplier = if (reached) NEXT_WEEK_BONUS_MULTIPLIER else 1.0f
        return SquadGoalEvaluation(
            isSquadSizeValid = validSize,
            isGoalReached = reached,
            nextWeekNonRankedMultiplier = multiplier,
            affectsRankedLeaderboardXp = false
        )
    }

    /**
     * Audit #23: Squad multiplier computes non-ranked bonus progression XP only;
     * ranked leaderboard XP MUST remain strictly equal to canonical server XP.
     */
    fun computeNonRankedBonusXp(canonicalXp: Int, squadMultiplier: Float): Pair<Int, Int> {
        val safeCanonical = canonicalXp.coerceAtLeast(0)
        val safeMult = squadMultiplier.coerceIn(1.0f, MAX_BONUS_MULTIPLIER)
        val bonusOnly = ((safeCanonical * safeMult) - safeCanonical).toInt().coerceAtLeast(0)
        return safeCanonical to bonusOnly
    }

    companion object {
        const val MIN_SQUAD_MEMBERS = 5
        const val MAX_SQUAD_MEMBERS = 10
        const val DEFAULT_WEEKLY_GOAL_XP = 1_000
        const val NEXT_WEEK_BONUS_MULTIPLIER = 1.25f
        const val MAX_BONUS_MULTIPLIER = 1.5f
    }
}

// =========================================================================
// ФТ-14.3 — Dictionary Crowdsourcing Eligibility Gate & PR Validator
// =========================================================================

class CheckCrowdsourceEligibilityUseCase @Inject constructor() {

    operator fun invoke(
        totalXp: Int,
        accountAgeDays: Int,
        learnedTermsCount: Int,
        confirmedReportsCount: Int
    ): CrowdsourceEligibility {
        val meetsRank = totalXp >= MIN_MIDDLE_RANK_XP
        val meetsAge = accountAgeDays >= MIN_ACCOUNT_AGE_DAYS
        val meetsTerms = learnedTermsCount >= MIN_LEARNED_TERMS
        val meetsCleanRecord = confirmedReportsCount == 0

        val reasons = mutableListOf<String>()
        if (!meetsRank) {
            reasons.add("Требуется ранг Middle Developer и выше (≥ $MIN_MIDDLE_RANK_XP XP, сейчас $totalXp XP).")
        }
        if (!meetsAge) {
            reasons.add("Возраст аккаунта должен быть ≥ $MIN_ACCOUNT_AGE_DAYS дней (сейчас $accountAgeDays дн.).")
        }
        if (!meetsTerms) {
            reasons.add("Необходимо изучить ≥ $MIN_LEARNED_TERMS терминов (сейчас $learnedTermsCount).")
        }
        if (!meetsCleanRecord) {
            reasons.add("Допуск закрыт: есть подтверждённые жалобы модерации ($confirmedReportsCount).")
        }

        return CrowdsourceEligibility(
            isEligible = reasons.isEmpty(),
            meetsRankMiddlePlus = meetsRank,
            meetsAccountAge14Days = meetsAge,
            meetsLearnedTerms100 = meetsTerms,
            meetsZeroConfirmedReports = meetsCleanRecord,
            unmetReasonsRu = reasons
        )
    }

    companion object {
        // Level 5 (Middle Developer) starts at 4 * 150 = 600 XP per CalculateXpLevelUseCase
        const val MIN_MIDDLE_RANK_XP = 600
        const val MIN_ACCOUNT_AGE_DAYS = 14
        const val MIN_LEARNED_TERMS = 100
    }
}

class ValidateDictionaryPrUseCase @Inject constructor() {

    operator fun invoke(
        rawTerm: String,
        rawTranslationRu: String,
        rawTranslationTg: String,
        rawSourceReference: String,
        licenseAccepted: Boolean
    ): Result<Unit> {
        val term = rawTerm.trim()
        val trRu = rawTranslationRu.trim()
        val trTg = rawTranslationTg.trim()
        val src = rawSourceReference.trim()

        if (!licenseAccepted) {
            return Result.failure(
                IllegalArgumentException("Необходимо подтвердить публикацию правки под открытой лицензией проекта (CC BY-SA 4.0).")
            )
        }
        if (term.length < 2 || term.length > 120) {
            return Result.failure(IllegalArgumentException("Термин должен содержать от 2 до 120 символов."))
        }
        if (trRu.isEmpty() && trTg.isEmpty()) {
            return Result.failure(IllegalArgumentException("Укажите перевод на русский или таджикский язык."))
        }
        if (src.length < 4 || (!src.lowercase(Locale.ROOT).startsWith("https://") && !src.contains("RFC", ignoreCase = true) && !src.contains("IEEE", ignoreCase = true) && !src.contains("NIST", ignoreCase = true) && !src.contains("ISO", ignoreCase = true))) {
            return Result.failure(
                IllegalArgumentException("Укажите авторитетный источник (HTTPS-ссылка или стандарт RFC/IEEE/NIST/ISO).")
            )
        }
        return Result.success(Unit)
    }
}
