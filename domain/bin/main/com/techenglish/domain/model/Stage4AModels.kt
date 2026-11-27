package com.techenglish.domain.model

// =========================================================================
// ФТ-13.1 — StackOverflow-style Comments & Mnemonics (TERM / SNIPPET)
// =========================================================================

enum class CommentTargetType(val apiKey: String) {
    TERM("TERM"),
    SNIPPET("SNIPPET");

    companion object {
        fun fromRaw(raw: String): CommentTargetType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TERM
    }
}

enum class CommentSortOrder(val labelRu: String, val apiKey: String) {
    TOP_VOTED("Лучшие", "top"),
    NEWEST("Новые", "newest")
}

data class CommunityComment(
    val id: String,
    val targetType: CommentTargetType,
    val targetId: Int,
    val authorUserId: String,
    val authorUsername: String,
    val authorRankTitle: String = "Developer",
    val body: String,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val userVote: Int = 0, // -1, 0, +1
    val isHidden: Boolean = false,
    val createdAtMillis: Long = 0L
) {
    val score: Int get() = upvotes - downvotes
}

// =========================================================================
// ФТ-13.2 — Community Decks (Publish from «Мои слова» & 1-Tap Room Download)
// =========================================================================

data class CommunityDeckTerm(
    val term: String,
    val transcription: String = "",
    val translation: String,
    val example: String = "",
    val explanationRu: String = ""
)

data class CommunityDeck(
    val id: String,
    val title: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val authorUserId: String,
    val authorUsername: String,
    val rating: Float = 0f,
    val downloadsCount: Int = 0,
    val termCount: Int = 0,
    val isDownloadedLocally: Boolean = false,
    val isHidden: Boolean = false,
    val createdAtMillis: Long = 0L,
    val terms: List<CommunityDeckTerm> = emptyList()
)

data class DeckLocalImportPlan(
    val wordsToInsert: List<Word>,
    val skippedDuplicatesCount: Int
)

// =========================================================================
// ФТ-9.4 & Errata E2 — UGC Moderation Targets
// =========================================================================

enum class UgcModerationTargetType(val apiKey: String, val labelRu: String) {
    PROFILE("PROFILE", "Профиль"),
    COMMENT("COMMENT", "Комментарий"),
    DECK("DECK", "Колода"),
    ROOM_MESSAGE("ROOM_MESSAGE", "Сообщение в комнате");

    companion object {
        fun fromRaw(raw: String): UgcModerationTargetType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: COMMENT
    }
}
