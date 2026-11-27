package com.techenglish.domain.usecase

import com.techenglish.domain.model.CommentSortOrder
import com.techenglish.domain.model.CommentTargetType
import com.techenglish.domain.model.CommunityComment
import com.techenglish.domain.model.CommunityDeck
import com.techenglish.domain.model.CommunityDeckTerm
import com.techenglish.domain.model.UgcModerationTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage4ACommunityUseCasesTest {

    private val validateUgc = ValidateUgcContentUseCase()
    private val sortComments = SortAndFilterCommentsUseCase()
    private val prepareImport = PrepareDeckForLocalImportUseCase()

    @Test
    fun `FT-9_4 UGC validation enforces comment length, blocks spam and validates deck publishing`() {
        assertTrue(validateUgc.validateCommentBody("ok").isFailure)
        assertTrue(validateUgc.validateCommentBody("a".repeat(1001)).isFailure)
        assertTrue(validateUgc.validateCommentBody("Check out free-crypto-airdrop now!").isFailure)
        assertTrue(
            validateUgc.validateCommentBody(
                "Visit https://a.test https://b.test and https://c.test now"
            ).isFailure
        )
        assertEquals(
            "Mnemonic: Idempotent = same result on repeat.",
            validateUgc.validateCommentBody("  Mnemonic:   Idempotent = same result on repeat.  ").getOrThrow()
        )

        val validTerms = listOf(
            CommunityDeckTerm("Backpressure", "/ˈbæk.preʃ.ər/", "обратное давление", "Apply backpressure.", "Защита очереди.")
        )
        assertTrue(validateUgc.validateDeckPublish("ab", "desc", listOf("backend"), validTerms).isFailure)
        assertTrue(validateUgc.validateDeckPublish("Valid Deck", "x".repeat(401), listOf("backend"), validTerms).isFailure)
        assertTrue(validateUgc.validateDeckPublish("casino-bonus pack", "desc", listOf("backend"), validTerms).isFailure)
        assertTrue(validateUgc.validateDeckPublish("Empty Deck", "desc", listOf("backend"), emptyList()).isFailure)
        assertTrue(
            validateUgc.validateDeckPublish(
                "Huge Deck",
                "desc",
                listOf("backend"),
                List(201) { CommunityDeckTerm("t$it", "", "tr$it") }
            ).isFailure
        )

        val (title, desc, tags) = validateUgc.validateDeckPublish(
            rawTitle = "  Backend Middle Interview  ",
            rawDescription = "Essential terms for backend system design interviews.",
            rawTags = listOf("#Backend", "System-Design", "backend", "x", "bad tag!"),
            terms = validTerms
        ).getOrThrow()
        assertEquals("Backend Middle Interview", title)
        assertEquals("Essential terms for backend system design interviews.", desc)
        assertEquals(listOf("backend", "system-design"), tags)
    }

    @Test
    fun `FT-13_1 comments sort by score or newest, filter hidden and blocked authors, and toggle votes idempotently`() {
        val c1 = CommunityComment("c1", CommentTargetType.TERM, 10, "u1", "alice", "Senior", "Top mnemonic", upvotes = 12, downvotes = 1, createdAtMillis = 100L)
        val c2 = CommunityComment("c2", CommentTargetType.TERM, 10, "u2", "bob", "Middle", "Newest mnemonic", upvotes = 3, downvotes = 0, createdAtMillis = 300L)
        val c3 = CommunityComment("c3", CommentTargetType.TERM, 10, "u3", "spammer", "Intern", "Blocked user", upvotes = 99, downvotes = 0, createdAtMillis = 200L)
        val c4 = CommunityComment("c4", CommentTargetType.SNIPPET, 5, "u4", "mod_hidden", "Intern", "Hidden comment", upvotes = 50, downvotes = 0, isHidden = true, createdAtMillis = 400L)

        val byTop = sortComments(listOf(c2, c1, c3, c4), blockedUserIds = setOf("u3"), sortOrder = CommentSortOrder.TOP_VOTED)
        assertEquals(listOf("c1", "c2"), byTop.map { it.id })
        assertEquals(11, byTop.first().score)

        val byNewest = sortComments(listOf(c1, c2, c3, c4), blockedUserIds = setOf("u3"), sortOrder = CommentSortOrder.NEWEST)
        assertEquals(listOf("c2", "c1"), byNewest.map { it.id })

        // Vote toggle: 0 -> +1 -> -1 -> 0
        val upvoted = sortComments.applyVoteToggle(c2, 1)
        assertEquals(4, upvoted.upvotes)
        assertEquals(1, upvoted.userVote)

        val switchedDown = sortComments.applyVoteToggle(upvoted, -1)
        assertEquals(3, switchedDown.upvotes)
        assertEquals(1, switchedDown.downvotes)
        assertEquals(-1, switchedDown.userVote)

        val cleared = sortComments.applyVoteToggle(switchedDown, -1)
        assertEquals(3, cleared.upvotes)
        assertEquals(0, cleared.downvotes)
        assertEquals(0, cleared.userVote)

        val invalidVote = sortComments.applyVoteToggle(cleared, 99)
        assertEquals(0, invalidVote.userVote)
    }

    @Test
    fun `FT-13_2 prepareDeckForLocalImport deduplicates case-insensitively and prepares custom Room words`() {
        val deckTerms = listOf(
            CommunityDeckTerm("Deadlock", "/ˈded.lɒk/", "взаимная блокировка", "Avoid deadlock.", "Два потока ждут друг друга."),
            CommunityDeckTerm("Backpressure", "", "обратное давление", "Use bounded channels.", "Контроль потока."),
            CommunityDeckTerm("backpressure", "", "дубль", "", ""),
            CommunityDeckTerm("   ", "", "пустой термин", "", "")
        )
        val plan = prepareImport(
            deckTerms = deckTerms,
            existingNormalizedTerms = setOf("deadlock"),
            targetCategoryId = 11,
            nowMillis = 1_800_000_000_000L
        )
        assertEquals(1, plan.wordsToInsert.size)
        assertEquals(3, plan.skippedDuplicatesCount)
        val inserted = plan.wordsToInsert.first()
        assertEquals("Backpressure", inserted.term)
        assertEquals(11, inserted.categoryId)
        assertTrue(inserted.isCustom)
        assertEquals(2.5f, inserted.easeFactor, 0.001f)

        // Verify model parsers & defaults
        assertEquals(CommentTargetType.SNIPPET, CommentTargetType.fromRaw("snippet"))
        assertEquals(CommentTargetType.TERM, CommentTargetType.fromRaw("unknown"))
        assertEquals(UgcModerationTargetType.DECK, UgcModerationTargetType.fromRaw("DECK"))
        assertEquals(UgcModerationTargetType.COMMENT, UgcModerationTargetType.fromRaw("unknown"))
        val deck = CommunityDeck(id = "d1", title = "Backend", authorUserId = "u1", authorUsername = "alice")
        assertFalse(deck.isDownloadedLocally)
        assertFalse(deck.isHidden)
        assertEquals(0, deck.termCount)
    }
}
