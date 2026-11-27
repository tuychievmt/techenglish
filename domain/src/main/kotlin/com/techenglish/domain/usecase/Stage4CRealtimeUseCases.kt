package com.techenglish.domain.usecase

import com.techenglish.domain.model.PvpMatchMode
import com.techenglish.domain.model.PvpMatchOutcome
import com.techenglish.domain.model.PvpMatchState
import javax.inject.Inject

// =========================================================================
// ФТ-14.1 — 1v1 PvP Duel Evaluator (60s, 15s Training Fallback, Disconnect)
// =========================================================================

class EvaluatePvpDuelUseCase @Inject constructor() {

    fun shouldFallbackToTraining(waitedSeconds: Int): Boolean =
        waitedSeconds >= MATCHMAKING_TIMEOUT_SECONDS

    fun evaluateCompletedMatch(
        matchId: String,
        mode: PvpMatchMode,
        playerScore: Int,
        opponentScore: Int
    ): PvpMatchOutcome {
        if (!mode.awardsRankedPoints) {
            return PvpMatchOutcome(
                matchId = matchId,
                mode = mode,
                finalState = PvpMatchState.COMPLETED,
                playerScore = playerScore,
                opponentScore = opponentScore,
                rankedXpAwarded = 0,
                ratingDelta = 0,
                summaryRu = "Тренировочный раунд завершён (Вы $playerScore : $opponentScore Соперник). Рейтинговые очки не начисляются."
            )
        }
        val (xp, delta, summary) = when {
            playerScore > opponentScore -> Triple(
                RANKED_WIN_XP,
                RANKED_WIN_RATING_DELTA,
                "Победа в 1v1 дуэли ($playerScore : $opponentScore)! +$RANKED_WIN_XP XP, +$RANKED_WIN_RATING_DELTA рейтинга."
            )
            playerScore == opponentScore -> Triple(
                RANKED_DRAW_XP,
                0,
                "Ничья в 1v1 дуэли ($playerScore : $opponentScore). +$RANKED_DRAW_XP XP."
            )
            else -> Triple(
                RANKED_LOSS_XP,
                RANKED_LOSS_RATING_DELTA,
                "Поражение в 1v1 дуэли ($playerScore : $opponentScore). +$RANKED_LOSS_XP XP за участие."
            )
        }
        return PvpMatchOutcome(
            matchId = matchId,
            mode = mode,
            finalState = PvpMatchState.COMPLETED,
            playerScore = playerScore,
            opponentScore = opponentScore,
            rankedXpAwarded = xp,
            ratingDelta = delta,
            summaryRu = summary
        )
    }

    /**
     * Cleanly terminates a PvP duel on network drop / socket close without hanging or double-scoring.
     */
    fun evaluateDisconnect(
        matchId: String,
        mode: PvpMatchMode,
        disconnectedByLocalPlayer: Boolean,
        playerScore: Int,
        opponentScore: Int
    ): PvpMatchOutcome {
        if (!mode.awardsRankedPoints) {
            return PvpMatchOutcome(
                matchId = matchId,
                mode = mode,
                finalState = PvpMatchState.DISCONNECTED_FORFEIT,
                playerScore = playerScore,
                opponentScore = opponentScore,
                rankedXpAwarded = 0,
                ratingDelta = 0,
                summaryRu = "Тренировочный раунд прерван из-за разрыва связи (0 рейтинговых очков)."
            )
        }
        return if (disconnectedByLocalPlayer) {
            PvpMatchOutcome(
                matchId = matchId,
                mode = mode,
                finalState = PvpMatchState.DISCONNECTED_FORFEIT,
                playerScore = playerScore,
                opponentScore = opponentScore,
                rankedXpAwarded = 0,
                ratingDelta = RANKED_LOSS_RATING_DELTA,
                summaryRu = "Соединение разорвано: засчитано техническое поражение ($RANKED_LOSS_RATING_DELTA рейтинга)."
            )
        } else {
            PvpMatchOutcome(
                matchId = matchId,
                mode = mode,
                finalState = PvpMatchState.DISCONNECTED_OPPONENT_LEFT,
                playerScore = playerScore,
                opponentScore = opponentScore,
                rankedXpAwarded = RANKED_WIN_XP,
                ratingDelta = RANKED_WIN_RATING_DELTA,
                summaryRu = "Соперник отключился: засчитана техническая победа (+$RANKED_WIN_XP XP)."
            )
        }
    }

    companion object {
        const val ROUND_DURATION_SECONDS = 60
        const val MATCHMAKING_TIMEOUT_SECONDS = 15
        const val MIN_QUESTIONS_PER_ROUND = 7
        const val MAX_QUESTIONS_PER_ROUND = 10
        const val RANKED_WIN_XP = 30
        const val RANKED_DRAW_XP = 15
        const val RANKED_LOSS_XP = 5
        const val RANKED_WIN_RATING_DELTA = 15
        const val RANKED_LOSS_RATING_DELTA = -10
    }
}

// =========================================================================
// ФТ-14.2 & Errata E5 — Peer Review Arena Submission & Voice TTL Validator
// =========================================================================

class ValidatePeerReviewSubmissionUseCase @Inject constructor() {

    fun validateSubmission(
        rawText: String,
        hasVoiceClip: Boolean,
        voiceDurationSeconds: Int,
        voicePayloadBytes: Int
    ): Result<String> {
        val cleanText = rawText.trim()
        if (cleanText.isEmpty() && !hasVoiceClip) {
            return Result.failure(
                IllegalArgumentException("Напишите текстовый ответ или прикрепите голосовую запись до 20 секунд.")
            )
        }
        if (cleanText.isNotEmpty() && (cleanText.length < MIN_TEXT_LEN || cleanText.length > MAX_TEXT_LEN)) {
            return Result.failure(
                IllegalArgumentException("Текстовый ответ должен содержать от $MIN_TEXT_LEN до $MAX_TEXT_LEN символов.")
            )
        }
        if (hasVoiceClip) {
            if (voiceDurationSeconds !in 1..MAX_VOICE_SECONDS) {
                return Result.failure(
                    IllegalArgumentException("Длительность голосовой записи должна быть от 1 до $MAX_VOICE_SECONDS секунд (ФТ-14.2).")
                )
            }
            if (voicePayloadBytes <= 0 || voicePayloadBytes > MAX_VOICE_BYTES) {
                return Result.failure(
                    IllegalArgumentException("Размер голосовой записи не должен превышать 512 КБ.")
                )
            }
        }
        return Result.success(cleanText)
    }

    fun calculateVoiceExpiryMillis(createdAtMillis: Long): Long =
        createdAtMillis + VOICE_TTL_DAYS * 24L * 60L * 60L * 1000L

    companion object {
        const val MIN_TEXT_LEN = 15
        const val MAX_TEXT_LEN = 1_200
        const val MAX_VOICE_SECONDS = 20
        const val MAX_VOICE_BYTES = 512 * 1024
        const val VOICE_TTL_DAYS = 30L // Errata E5 ADR DEC-13
    }
}
