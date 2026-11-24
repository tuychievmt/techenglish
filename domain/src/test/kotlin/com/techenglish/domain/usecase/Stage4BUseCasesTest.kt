package com.techenglish.domain.usecase

import com.techenglish.domain.model.AiInterviewLevel
import com.techenglish.domain.model.AiInterviewRole
import com.techenglish.domain.model.AiInterviewTurn
import com.techenglish.domain.model.AiPrReviewFeedback
import com.techenglish.domain.model.DictionaryPrItem
import com.techenglish.domain.model.DictionaryPrStatus
import com.techenglish.domain.model.SquadInfo
import com.techenglish.domain.model.SquadMemberInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage4BUseCasesTest {

    private val sanitizePii = SanitizeAiPromptPiiUseCase()
    private val evaluateSquad = EvaluateSquadWeeklyGoalUseCase()
    private val checkEligibility = CheckCrowdsourceEligibilityUseCase()
    private val validatePr = ValidateDictionaryPrUseCase()

    @Test
    fun `FT-8_3 AI PII scrubber strips emails, phones, handles and bearer tokens before server proxy`() {
        val dirty = "Hi, I am @farhod_dev, reach me at dev.user@example.com or +992 90 123-45-67 with token=secret123."
        val clean = sanitizePii(dirty)
        assertFalse(clean.contains("dev.user@example.com"))
        assertFalse(clean.contains("+992 90 123-45-67"))
        assertFalse(clean.contains("@farhod_dev"))
        assertFalse(clean.contains("secret123"))
        assertTrue(clean.contains("[REDACTED_EMAIL]"))
        assertTrue(clean.contains("[REDACTED_PHONE]"))
        assertTrue(clean.contains("@user"))
        assertTrue(clean.contains("[REDACTED_TOKEN]"))
    }

    @Test
    fun `FT-14_4 Squads require 5 to 10 members and multiplier never affects ranked leaderboard XP`() {
        val tooSmall = evaluateSquad(memberCount = 4, currentWeekCollectiveXp = 1_500)
        assertFalse(tooSmall.isSquadSizeValid)
        assertFalse(tooSmall.isGoalReached)
        assertEquals(1.0f, tooSmall.nextWeekNonRankedMultiplier, 0.001f)
        assertFalse(tooSmall.affectsRankedLeaderboardXp)

        val tooLarge = evaluateSquad(memberCount = 11, currentWeekCollectiveXp = 2_000)
        assertFalse(tooLarge.isSquadSizeValid)

        val activeNotReached = evaluateSquad(memberCount = 6, currentWeekCollectiveXp = 850)
        assertTrue(activeNotReached.isSquadSizeValid)
        assertFalse(activeNotReached.isGoalReached)
        assertEquals(1.0f, activeNotReached.nextWeekNonRankedMultiplier, 0.001f)

        val activeReached = evaluateSquad(memberCount = 7, currentWeekCollectiveXp = 1_250)
        assertTrue(activeReached.isSquadSizeValid)
        assertTrue(activeReached.isGoalReached)
        assertEquals(1.25f, activeReached.nextWeekNonRankedMultiplier, 0.001f)
        assertFalse(activeReached.affectsRankedLeaderboardXp)

        // Ranked XP stays canonical (20), squad bonus is separate non-ranked XP (+5)
        val (rankedXp, bonusXp) = evaluateSquad.computeNonRankedBonusXp(canonicalXp = 20, squadMultiplier = 1.25f)
        assertEquals(20, rankedXp)
        assertEquals(5, bonusXp)

        val squad = SquadInfo(
            id = "sq-1",
            name = "Kotlin Architects",
            inviteCode = "SQ-KOTLIN",
            members = List(5) { SquadMemberInfo("u$it", "dev$it", "Middle", 250) },
            weeklyGoalXp = 1_000,
            currentWeekXp = 750,
            isActiveForChallenge = true
        )
        assertEquals(5, squad.memberCount)
        assertEquals(0.75f, squad.progressFraction, 0.001f)
    }

    @Test
    fun `FT-14_3 Crowdsourcing enforces 4 strict eligibility gates and project license acknowledgment`() {
        val blocked = checkEligibility(
            totalXp = 300,
            accountAgeDays = 5,
            learnedTermsCount = 40,
            confirmedReportsCount = 1
        )
        assertFalse(blocked.isEligible)
        assertEquals(4, blocked.unmetReasonsRu.size)
        assertFalse(blocked.meetsRankMiddlePlus)
        assertFalse(blocked.meetsAccountAge14Days)
        assertFalse(blocked.meetsLearnedTerms100)
        assertFalse(blocked.meetsZeroConfirmedReports)

        val eligible = checkEligibility(
            totalXp = 600,
            accountAgeDays = 14,
            learnedTermsCount = 100,
            confirmedReportsCount = 0
        )
        assertTrue(eligible.isEligible)
        assertTrue(eligible.unmetReasonsRu.isEmpty())

        // PR Validation
        assertTrue(validatePr("Deadlock", "взаимная блокировка", "", "https://en.wikipedia.org/wiki/Deadlock", licenseAccepted = false).isFailure)
        assertTrue(validatePr("a", "перевод", "", "https://rfc-editor.org", licenseAccepted = true).isFailure)
        assertTrue(validatePr("Deadlock", "", "", "https://rfc-editor.org", licenseAccepted = true).isFailure)
        assertTrue(validatePr("Deadlock", "перевод", "", "random blog", licenseAccepted = true).isFailure)
        assertTrue(validatePr("Idempotency", "идемпотентность", "идемпотентност", "RFC 9110 Section 9.2.2", licenseAccepted = true).isSuccess)

        // Models & parsers
        assertEquals(AiInterviewRole.DEVOPS_SRE, AiInterviewRole.fromRaw("devops_sre"))
        assertEquals(AiInterviewRole.BACKEND, AiInterviewRole.fromRaw("unknown"))
        assertEquals(AiInterviewLevel.SENIOR, AiInterviewLevel.fromRaw("senior"))
        assertEquals(AiInterviewLevel.MIDDLE, AiInterviewLevel.fromRaw("unknown"))
        assertEquals(DictionaryPrStatus.APPROVED, DictionaryPrStatus.fromRaw("APPROVED"))
        assertEquals(DictionaryPrStatus.OPEN, DictionaryPrStatus.fromRaw("unknown"))

        val turn = AiInterviewTurn("t1", "Q?", "A", 85, 90, "Good", "Better", "Next?")
        assertEquals(85, turn.englishScore)
        val prReview = AiPrReviewFeedback("Constructive", "Could we extract this?", listOf("Tip 1"), 14)
        assertEquals(14, prReview.remainingDailyQuota)
        val prItem = DictionaryPrItem("pr1", "Sharding", "шардирование", sourceReference = "https://postgresql.org", authorUsername = "dev")
        assertEquals(DictionaryPrStatus.OPEN, prItem.status)
    }
}
