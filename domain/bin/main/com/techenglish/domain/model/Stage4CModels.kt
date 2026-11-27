package com.techenglish.domain.model

// =========================================================================
// ФТ-13.3 — Thematic Developer Rooms (WebSockets + Redis)
// =========================================================================

enum class ThematicRoomChannel(
    val slug: String,
    val title: String,
    val descriptionRu: String
) {
    CODE_REVIEW_PRACTICE(
        slug = "code-review-practice",
        title = "#code-review-practice",
        descriptionRu = "Разбор комментариев к Pull Request и вежливых формулировок"
    ),
    DAILY_STANDUP(
        slug = "daily-standup",
        title = "#daily-standup",
        descriptionRu = "Ежедневные статус-апдейты, блокеры и синки команды"
    ),
    INTERVIEWS(
        slug = "interviews",
        title = "#interviews",
        descriptionRu = "Подготовка к техническим и behavioural собеседованиям на английском"
    ),
    BACKEND_ENGLISH(
        slug = "backend-english",
        title = "#backend-english",
        descriptionRu = "Архитектура, базы данных, API-контракты и инциденты"
    );

    companion object {
        fun fromSlug(raw: String): ThematicRoomChannel =
            entries.firstOrNull { it.slug.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) }
                ?: CODE_REVIEW_PRACTICE
    }
}

data class RoomChatMessage(
    val id: String,
    val roomSlug: String,
    val authorUserId: String,
    val authorUsername: String,
    val authorRankTitle: String = "Developer",
    val body: String,
    val isHidden: Boolean = false,
    val createdAtMillis: Long = 0L
)

// =========================================================================
// ФТ-14.1 — Real-Time 1v1 PvP Duels (60s, 7–10 Server Questions, 15s Fallback)
// =========================================================================

enum class PvpMatchMode(val labelRu: String, val awardsRankedPoints: Boolean) {
    RANKED_1V1("Рейтинговая 1v1 дуэль", awardsRankedPoints = true),
    TRAINING_FALLBACK("Тренировочный раунд (0 рейтинговых очков)", awardsRankedPoints = false)
}

enum class PvpMatchState {
    IDLE,
    SEARCHING,
    IN_PROGRESS,
    COMPLETED,
    DISCONNECTED_FORFEIT,
    DISCONNECTED_OPPONENT_LEFT
}

data class PvpQuestion(
    val id: String,
    val promptEn: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val categoryTag: String = "VOCAB"
)

data class PvpMatchOutcome(
    val matchId: String,
    val mode: PvpMatchMode,
    val finalState: PvpMatchState,
    val playerScore: Int,
    val opponentScore: Int,
    val rankedXpAwarded: Int,
    val ratingDelta: Int,
    val summaryRu: String
)

// =========================================================================
// ФТ-14.2 — Peer Review Arena (Daily Case + Text or <=20s Voice, 30d TTL)
// =========================================================================

data class PeerReviewCase(
    val caseId: String,
    val dateIso: String,
    val titleEn: String,
    val promptEn: String,
    val promptRu: String,
    val targetSentencesCount: Int = 3
)

data class PeerReviewFeedbackItem(
    val id: String,
    val reviewerUsername: String,
    val reviewerRankTitle: String,
    val ratingStars: Int,
    val commentText: String,
    val createdAtMillis: Long = 0L
)

data class PeerReviewSubmission(
    val id: String,
    val caseId: String,
    val authorUserId: String,
    val authorUsername: String,
    val authorRankTitle: String,
    val textResponse: String,
    val hasVoiceClip: Boolean = false,
    val voiceDurationSeconds: Int = 0,
    val voiceExpiresAtMillis: Long = 0L,
    val feedbackList: List<PeerReviewFeedbackItem> = emptyList(),
    val createdAtMillis: Long = 0L
)
