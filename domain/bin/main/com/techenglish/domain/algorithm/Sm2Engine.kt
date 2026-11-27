package com.techenglish.domain.algorithm

import com.techenglish.domain.model.ReviewQuality
import com.techenglish.domain.model.Sm2Result
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure Kotlin SuperMemo-2 (SM-2) Spaced Repetition Engine.
 *
 * Rules per technical specification (ФТ-1.3 & §5):
 * - Initial state: EF = 2.5, repetitions = 0, interval = 0.
 * - Quality scale:
 *   - FORGOT (q = 1): resets repetitions = 0 and interval = 0 (due immediately).
 *   - HARD   (q = 3): successful recall with reduced EF.
 *   - GOOD   (q = 4): successful recall maintaining EF.
 *   - EASY   (q = 5): successful recall increasing EF.
 * - EF formula: EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
 * - Minimum EF floor: 1.3
 * - Intervals on successful recall (q >= 3):
 *   - 1st success (repetitions == 1): 1 day
 *   - 2nd success (repetitions == 2): 6 days
 *   - 3rd+ success (repetitions >= 3): round(previousInterval * EF')
 */
class Sm2Engine {

    fun calculate(
        currentRepetitions: Int,
        currentEaseFactor: Float,
        currentIntervalDays: Int,
        quality: ReviewQuality,
        nowMillis: Long
    ): Sm2Result {
        require(currentRepetitions >= 0) { "Repetitions must be non-negative" }
        require(currentIntervalDays >= 0) { "Interval days must be non-negative" }

        val q = quality.q
        val diff = 5 - q
        val efDelta = 0.1 - diff * (0.08 + diff * 0.02)
        val rawEaseFactor = currentEaseFactor.toDouble() + efDelta
        val clampedEaseFactor = max(MIN_EASE_FACTOR, rawEaseFactor)
        // Round to 4 decimal places to avoid floating-point drift over dozens of reviews
        val roundedEaseFactor = ((clampedEaseFactor * 10_000.0).roundToInt() / 10_000.0).toFloat()

        if (q < 3) {
            return Sm2Result(
                repetitions = 0,
                easeFactor = roundedEaseFactor,
                intervalDays = 0,
                nextReviewAt = nowMillis
            )
        }

        val newRepetitions = currentRepetitions + 1
        val newIntervalDays = when (newRepetitions) {
            1 -> FIRST_INTERVAL_DAYS
            2 -> SECOND_INTERVAL_DAYS
            else -> max(1, (currentIntervalDays * roundedEaseFactor).roundToInt())
        }

        val nextReviewAt = nowMillis + newIntervalDays.toLong() * MILLIS_PER_DAY
        return Sm2Result(
            repetitions = newRepetitions,
            easeFactor = roundedEaseFactor,
            intervalDays = newIntervalDays,
            nextReviewAt = nextReviewAt
        )
    }

    fun previewIntervals(
        currentRepetitions: Int,
        currentEaseFactor: Float,
        currentIntervalDays: Int,
        nowMillis: Long
    ): Map<ReviewQuality, Int> = ReviewQuality.entries.associateWith { quality ->
        calculate(
            currentRepetitions = currentRepetitions,
            currentEaseFactor = currentEaseFactor,
            currentIntervalDays = currentIntervalDays,
            quality = quality,
            nowMillis = nowMillis
        ).intervalDays
    }

    companion object {
        const val INITIAL_EASE_FACTOR: Float = 2.5f
        const val MIN_EASE_FACTOR: Double = 1.3
        const val FIRST_INTERVAL_DAYS: Int = 1
        const val SECOND_INTERVAL_DAYS: Int = 6
        const val MILLIS_PER_DAY: Long = 86_400_000L
    }
}
