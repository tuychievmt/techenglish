package com.techenglish.domain.usecase

import com.techenglish.domain.model.CommentSortOrder
import com.techenglish.domain.model.CommunityComment
import com.techenglish.domain.model.CommunityDeckTerm
import com.techenglish.domain.model.DeckLocalImportPlan
import com.techenglish.domain.model.Word
import java.util.Locale
import javax.inject.Inject

// =========================================================================
// ФТ-9.4 & Errata E2 — UGC Content Validation (Comments & Community Decks)
// =========================================================================

class ValidateUgcContentUseCase @Inject constructor() {

    fun validateCommentBody(rawBody: String): Result<String> {
        val clean = rawBody.trim().replace(Regex("\\s+"), " ")
        if (clean.length < MIN_COMMENT_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Комментарий должен содержать минимум $MIN_COMMENT_LENGTH символов.")
            )
        }
        if (clean.length > MAX_COMMENT_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Комментарий не должен превышать $MAX_COMMENT_LENGTH символов.")
            )
        }
        if (containsBlockedContent(clean)) {
            return Result.failure(
                IllegalArgumentException("Текст нарушает правила сообщества (обнаружен спам или запрещённый контент).")
            )
        }
        return Result.success(clean)
    }

    fun validateDeckPublish(
        rawTitle: String,
        rawDescription: String,
        rawTags: List<String>,
        terms: List<CommunityDeckTerm>
    ): Result<Triple<String, String, List<String>>> {
        val title = rawTitle.trim().replace(Regex("\\s+"), " ")
        val description = rawDescription.trim().replace(Regex("\\s+"), " ")
        if (title.length < MIN_DECK_TITLE_LENGTH || title.length > MAX_DECK_TITLE_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Название колоды должно быть от $MIN_DECK_TITLE_LENGTH до $MAX_DECK_TITLE_LENGTH символов.")
            )
        }
        if (description.length > MAX_DECK_DESC_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Описание колоды не должно превышать $MAX_DECK_DESC_LENGTH символов.")
            )
        }
        if (containsBlockedContent(title) || containsBlockedContent(description)) {
            return Result.failure(
                IllegalArgumentException("Название или описание колоды нарушает правила модерации.")
            )
        }
        if (terms.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Добавьте минимум 1 термин в «Мои слова» перед публикацией колоды.")
            )
        }
        if (terms.size > MAX_DECK_TERMS) {
            return Result.failure(
                IllegalArgumentException("В одной колоде допускается не более $MAX_DECK_TERMS терминов.")
            )
        }
        val cleanTags = rawTags
            .map { it.trim().removePrefix("#").lowercase(Locale.ROOT) }
            .filter { it.length in 2..24 && TAG_REGEX.matches(it) && !containsBlockedContent(it) }
            .distinct()
            .take(MAX_DECK_TAGS)

        return Result.success(Triple(title, description, cleanTags))
    }

    internal fun containsBlockedContent(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        if (BLOCKED_TERMS.any { lower.contains(it) }) return true
        val urlMatches = URL_REGEX.findAll(lower).count()
        return urlMatches >= 3
    }

    companion object {
        const val MIN_COMMENT_LENGTH = 5
        const val MAX_COMMENT_LENGTH = 1000
        const val MIN_DECK_TITLE_LENGTH = 4
        const val MAX_DECK_TITLE_LENGTH = 80
        const val MAX_DECK_DESC_LENGTH = 400
        const val MAX_DECK_TERMS = 200
        const val MAX_DECK_TAGS = 6
        private val TAG_REGEX = Regex("^[a-z0-9_\\-+]+$")
        private val URL_REGEX = Regex("https?://\\S+")
        private val BLOCKED_TERMS = setOf(
            "casino-bonus", "free-crypto-airdrop", "porn", "nazi", "buy-followers", "scam-link"
        )
    }
}

// =========================================================================
// ФТ-13.1 — StackOverflow Comment Sorting, Blocking & Vote Toggle
// =========================================================================

class SortAndFilterCommentsUseCase @Inject constructor() {

    operator fun invoke(
        comments: List<CommunityComment>,
        blockedUserIds: Set<String>,
        sortOrder: CommentSortOrder
    ): List<CommunityComment> {
        val visible = comments.filter { !it.isHidden && it.authorUserId !in blockedUserIds }
        return when (sortOrder) {
            CommentSortOrder.TOP_VOTED -> visible.sortedWith(
                compareByDescending<CommunityComment> { it.score }
                    .thenByDescending { it.upvotes }
                    .thenByDescending { it.createdAtMillis }
            )
            CommentSortOrder.NEWEST -> visible.sortedByDescending { it.createdAtMillis }
        }
    }

    /**
     * Applies an idempotent upvote (+1) or downvote (-1) toggle on a comment.
     * Clicking the same vote again resets it to 0.
     */
    fun applyVoteToggle(comment: CommunityComment, requestedVote: Int): CommunityComment {
        val targetVote = when {
            requestedVote !in listOf(-1, 0, 1) -> 0
            comment.userVote == requestedVote -> 0
            else -> requestedVote
        }
        var up = comment.upvotes
        var down = comment.downvotes
        if (comment.userVote == 1) up = (up - 1).coerceAtLeast(0)
        if (comment.userVote == -1) down = (down - 1).coerceAtLeast(0)
        if (targetVote == 1) up += 1
        if (targetVote == -1) down += 1
        return comment.copy(upvotes = up, downvotes = down, userVote = targetVote)
    }
}

// =========================================================================
// ФТ-13.2 — 1-Tap Community Deck Import into Local Room «Мои слова»
// =========================================================================

class PrepareDeckForLocalImportUseCase @Inject constructor() {

    operator fun invoke(
        deckTerms: List<CommunityDeckTerm>,
        existingNormalizedTerms: Set<String>,
        targetCategoryId: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): DeckLocalImportPlan {
        val seen = existingNormalizedTerms
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .toMutableSet()

        val toInsert = mutableListOf<Word>()
        var skipped = 0

        for (item in deckTerms) {
            val cleanTerm = item.term.trim()
            val cleanTranslation = item.translation.trim()
            if (cleanTerm.isEmpty() || cleanTranslation.isEmpty()) {
                skipped++
                continue
            }
            val key = cleanTerm.lowercase(Locale.ROOT)
            if (!seen.add(key)) {
                skipped++
                continue
            }
            toInsert.add(
                Word(
                    id = 0,
                    categoryId = targetCategoryId,
                    categoryName = "Мои слова",
                    languageId = null,
                    term = cleanTerm,
                    transcription = item.transcription.trim(),
                    translation = cleanTranslation,
                    example = item.example.trim(),
                    explanationRu = item.explanationRu.trim(),
                    isFavorite = false,
                    isCustom = true,
                    repetitions = 0,
                    easeFactor = 2.5f,
                    intervalDays = 0,
                    nextReviewAt = nowMillis
                )
            )
        }

        return DeckLocalImportPlan(
            wordsToInsert = toInsert,
            skippedDuplicatesCount = skipped
        )
    }
}
