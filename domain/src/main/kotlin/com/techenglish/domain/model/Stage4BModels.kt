package com.techenglish.domain.model

// =========================================================================
// ФТ-8.1–8.3 — AI Mock Interviewer & AI Code/PR Reviewer (FastAPI Proxy)
// =========================================================================

enum class AiInterviewRole(val labelRu: String, val apiKey: String) {
    BACKEND("Backend Engineer", "backend"),
    ANDROID("Android Developer", "android"),
    SYSTEM_DESIGN("System Design", "system_design"),
    DEVOPS_SRE("DevOps / SRE", "devops_sre"),
    ML_AI("ML / AI Engineer", "ml_ai");

    companion object {
        fun fromRaw(raw: String): AiInterviewRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) || it.apiKey.equals(raw, ignoreCase = true) }
                ?: BACKEND
    }
}

enum class AiInterviewLevel(val labelRu: String, val apiKey: String) {
    JUNIOR("Junior", "junior"),
    MIDDLE("Middle", "middle"),
    SENIOR("Senior", "senior");

    companion object {
        fun fromRaw(raw: String): AiInterviewLevel =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) || it.apiKey.equals(raw, ignoreCase = true) }
                ?: MIDDLE
    }
}

data class AiInterviewTurn(
    val id: String,
    val questionEn: String,
    val userAnswerEn: String,
    val englishScore: Int,
    val technicalScore: Int,
    val feedbackRu: String,
    val improvedAnswerEn: String,
    val nextQuestionEn: String
)

data class AiPrReviewFeedback(
    val toneAssessmentRu: String,
    val improvedEnglishText: String,
    val keySuggestionsRu: List<String>,
    val remainingDailyQuota: Int
)

// =========================================================================
// ФТ-14.4 — Squads (5–10 Developers, Weekly Goal, Non-Ranked Multiplier)
// =========================================================================

data class SquadMemberInfo(
    val userId: String,
    val username: String,
    val rankTitle: String,
    val weeklyXp: Int
)

data class SquadInfo(
    val id: String,
    val name: String,
    val inviteCode: String,
    val members: List<SquadMemberInfo>,
    val weeklyGoalXp: Int = 1_000,
    val currentWeekXp: Int = 0,
    val nextWeekBonusMultiplier: Float = 1.0f,
    val isActiveForChallenge: Boolean = false
) {
    val memberCount: Int get() = members.size
    val progressFraction: Float
        get() = if (weeklyGoalXp <= 0) 0f else (currentWeekXp.toFloat() / weeklyGoalXp.toFloat()).coerceIn(0f, 1f)
}

data class SquadGoalEvaluation(
    val isSquadSizeValid: Boolean,
    val isGoalReached: Boolean,
    val nextWeekNonRankedMultiplier: Float,
    val affectsRankedLeaderboardXp: Boolean = false
)

// =========================================================================
// ФТ-14.3 — Dictionary Crowdsourcing («PR в словарь» + Contributor Badge)
// =========================================================================

data class CrowdsourceEligibility(
    val isEligible: Boolean,
    val meetsRankMiddlePlus: Boolean,
    val meetsAccountAge14Days: Boolean,
    val meetsLearnedTerms100: Boolean,
    val meetsZeroConfirmedReports: Boolean,
    val unmetReasonsRu: List<String>
)

enum class DictionaryPrStatus(val labelRu: String) {
    OPEN("На проверке"),
    APPROVED("Одобрено"),
    REJECTED("Отклонено");

    companion object {
        fun fromRaw(raw: String): DictionaryPrStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OPEN
    }
}

data class DictionaryPrItem(
    val id: String,
    val term: String,
    val proposedTranslationRu: String,
    val proposedTranslationTg: String = "",
    val proposedDefinitionRu: String = "",
    val sourceReference: String,
    val authorUsername: String,
    val status: DictionaryPrStatus = DictionaryPrStatus.OPEN,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val licenseAccepted: Boolean = true,
    val createdAtMillis: Long = 0L
)
