package com.techenglish.domain.usecase

import com.techenglish.domain.model.PeerReviewCase
import com.techenglish.domain.model.PeerReviewFeedbackItem
import com.techenglish.domain.model.PeerReviewSubmission
import com.techenglish.domain.model.PvpMatchMode
import com.techenglish.domain.model.PvpMatchState
import com.techenglish.domain.model.PvpQuestion
import com.techenglish.domain.model.RoomChatMessage
import com.techenglish.domain.model.ThematicRoomChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage4CRealtimeUseCasesTest {

    private val evaluatePvp = EvaluatePvpDuelUseCase()
    private val validatePeer = ValidatePeerReviewSubmissionUseCase()

    @Test
    fun `FT-14_1 PvP duel handles 15s training fallback, ranked scoring, and graceful disconnect`() {
        assertFalse(evaluatePvp.shouldFallbackToTraining(14))
        assertTrue(evaluatePvp.shouldFallbackToTraining(15))

        // Training fallback awards strictly 0 ranked XP and 0 rating points (Audit #21)
        val train = evaluatePvp.evaluateCompletedMatch("m1", PvpMatchMode.TRAINING_FALLBACK, 8, 5)
        assertEquals(0, train.rankedXpAwarded)
        assertEquals(0, train.ratingDelta)
        assertEquals(PvpMatchState.COMPLETED, train.finalState)

        // Ranked win / draw / loss
        val win = evaluatePvp.evaluateCompletedMatch("m2", PvpMatchMode.RANKED_1V1, 7, 4)
        assertEquals(30, win.rankedXpAwarded)
        assertEquals(15, win.ratingDelta)

        val draw = evaluatePvp.evaluateCompletedMatch("m3", PvpMatchMode.RANKED_1V1, 5, 5)
        assertEquals(15, draw.rankedXpAwarded)
        assertEquals(0, draw.ratingDelta)

        val loss = evaluatePvp.evaluateCompletedMatch("m4", PvpMatchMode.RANKED_1V1, 3, 6)
        assertEquals(5, loss.rankedXpAwarded)
        assertEquals(-10, loss.ratingDelta)

        // Clean disconnect outcomes
        val localDrop = evaluatePvp.evaluateDisconnect("m5", PvpMatchMode.RANKED_1V1, disconnectedByLocalPlayer = true, 2, 3)
        assertEquals(PvpMatchState.DISCONNECTED_FORFEIT, localDrop.finalState)
        assertEquals(0, localDrop.rankedXpAwarded)
        assertEquals(-10, localDrop.ratingDelta)

        val oppDrop = evaluatePvp.evaluateDisconnect("m6", PvpMatchMode.RANKED_1V1, disconnectedByLocalPlayer = false, 4, 2)
        assertEquals(PvpMatchState.DISCONNECTED_OPPONENT_LEFT, oppDrop.finalState)
        assertEquals(30, oppDrop.rankedXpAwarded)

        val trainDrop = evaluatePvp.evaluateDisconnect("m7", PvpMatchMode.TRAINING_FALLBACK, disconnectedByLocalPlayer = true, 2, 1)
        assertEquals(0, trainDrop.rankedXpAwarded)
        assertEquals(0, trainDrop.ratingDelta)
    }

    @Test
    fun `FT-14_2 and Errata E5 validate Peer Review text, 20s voice clip limits, 30-day TTL and room models`() {
        assertTrue(validatePeer.validateSubmission("", false, 0, 0).isFailure)
        assertTrue(validatePeer.validateSubmission("Too short", false, 0, 0).isFailure)
        assertTrue(validatePeer.validateSubmission("Valid text answer explaining deployment rollback clearly.", true, 21, 10_000).isFailure)
        assertTrue(validatePeer.validateSubmission("Valid text answer explaining deployment rollback clearly.", true, 12, 600_000).isFailure)
        assertTrue(validatePeer.validateSubmission("The deployment failed due to a missing migration index; we rolled back safely.", true, 18, 48_000).isSuccess)

        val now = 1_800_000_000_000L
        assertEquals(now + 30L * 24L * 60L * 60L * 1000L, validatePeer.calculateVoiceExpiryMillis(now))

        assertEquals(ThematicRoomChannel.BACKEND_ENGLISH, ThematicRoomChannel.fromSlug("backend-english"))
        assertEquals(ThematicRoomChannel.CODE_REVIEW_PRACTICE, ThematicRoomChannel.fromSlug("unknown"))

        val msg = RoomChatMessage("r1", "interviews", "u1", "dev1", body = "Tip: use STAR method.")
        assertFalse(msg.isHidden)
        val q = PvpQuestion("q1", "What does idempotent mean?", listOf("A", "B", "C", "D"), 0)
        assertEquals("VOCAB", q.categoryTag)
        val c = PeerReviewCase("case1", "2026-11-19", "Deploy Rollback", "Explain rollback.", "Объясните откат.")
        assertEquals(3, c.targetSentencesCount)
        val fb = PeerReviewFeedbackItem("fb1", "lead_dev", "Tech Lead", 5, "Clear and concise!")
        val sub = PeerReviewSubmission("s1", "case1", "u1", "dev1", "Middle", "We rolled back.", hasVoiceClip = true, voiceDurationSeconds = 15, feedbackList = listOf(fb))
        assertEquals(1, sub.feedbackList.size)
    }
}
