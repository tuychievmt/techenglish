package com.techenglish.domain.algorithm

import com.techenglish.domain.model.ReviewQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2EngineTest {

    private val engine = Sm2Engine()
    private val baseTime = 1_700_000_000_000L

    @Test
    fun `first successful review with GOOD sets interval 1 day and keeps EF 2_5`() {
        val result = engine.calculate(
            currentRepetitions = 0,
            currentEaseFactor = 2.5f,
            currentIntervalDays = 0,
            quality = ReviewQuality.GOOD,
            nowMillis = baseTime
        )
        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(2.5f, result.easeFactor, 0.0001f)
        assertEquals(baseTime + Sm2Engine.MILLIS_PER_DAY, result.nextReviewAt)
    }

    @Test
    fun `second successful review sets interval to 6 days`() {
        val result = engine.calculate(
            currentRepetitions = 1,
            currentEaseFactor = 2.5f,
            currentIntervalDays = 1,
            quality = ReviewQuality.GOOD,
            nowMillis = baseTime
        )
        assertEquals(2, result.repetitions)
        assertEquals(6, result.intervalDays)
        assertEquals(2.5f, result.easeFactor, 0.0001f)
        assertEquals(baseTime + 6L * Sm2Engine.MILLIS_PER_DAY, result.nextReviewAt)
    }

    @Test
    fun `third and subsequent reviews multiply previous interval by EF`() {
        val third = engine.calculate(
            currentRepetitions = 2,
            currentEaseFactor = 2.5f,
            currentIntervalDays = 6,
            quality = ReviewQuality.GOOD,
            nowMillis = baseTime
        )
        assertEquals(3, third.repetitions)
        assertEquals(15, third.intervalDays) // round(6 * 2.5) = 15
        assertEquals(baseTime + 15L * Sm2Engine.MILLIS_PER_DAY, third.nextReviewAt)

        val fourthEasy = engine.calculate(
            currentRepetitions = third.repetitions,
            currentEaseFactor = third.easeFactor,
            currentIntervalDays = third.intervalDays,
            quality = ReviewQuality.EASY,
            nowMillis = third.nextReviewAt
        )
        // EF' = 2.5 + 0.1 = 2.6; round(15 * 2.6) = 39
        assertEquals(4, fourthEasy.repetitions)
        assertEquals(2.6f, fourthEasy.easeFactor, 0.0001f)
        assertEquals(39, fourthEasy.intervalDays)
        assertEquals(third.nextReviewAt + 39L * Sm2Engine.MILLIS_PER_DAY, fourthEasy.nextReviewAt)
    }

    @Test
    fun `quality q=1 FORGOT resets repetitions and interval to zero and reduces EF`() {
        val result = engine.calculate(
            currentRepetitions = 4,
            currentEaseFactor = 2.5f,
            currentIntervalDays = 30,
            quality = ReviewQuality.FORGOT,
            nowMillis = baseTime
        )
        // delta = 0.1 - 4 * (0.08 + 4 * 0.02) = 0.1 - 0.64 = -0.54 -> 1.96
        assertEquals(0, result.repetitions)
        assertEquals(0, result.intervalDays)
        assertEquals(1.96f, result.easeFactor, 0.0001f)
        assertEquals(baseTime, result.nextReviewAt)
    }

    @Test
    fun `quality q=3 HARD reduces EF by 0_14 and advances repetitions`() {
        val result = engine.calculate(
            currentRepetitions = 2,
            currentEaseFactor = 2.5f,
            currentIntervalDays = 6,
            quality = ReviewQuality.HARD,
            nowMillis = baseTime
        )
        // delta = 0.1 - 2 * (0.08 + 2 * 0.02) = 0.1 - 0.24 = -0.14 -> 2.36
        // interval = round(6 * 2.36) = round(14.16) = 14
        assertEquals(3, result.repetitions)
        assertEquals(2.36f, result.easeFactor, 0.0001f)
        assertEquals(14, result.intervalDays)
    }

    @Test
    fun `quality q=5 EASY increases EF by 0_1`() {
        val result = engine.calculate(
            currentRepetitions = 0,
            currentEaseFactor = 2.5f,
            currentIntervalDays = 0,
            quality = ReviewQuality.EASY,
            nowMillis = baseTime
        )
        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(2.6f, result.easeFactor, 0.0001f)
    }

    @Test
    fun `ease factor never drops below minimum 1_3`() {
        var ef = 1.5f
        repeat(10) {
            val res = engine.calculate(
                currentRepetitions = 1,
                currentEaseFactor = ef,
                currentIntervalDays = 1,
                quality = ReviewQuality.FORGOT,
                nowMillis = baseTime
            )
            ef = res.easeFactor
            assertTrue("EF dropped below 1.3: $ef", ef >= 1.3f)
        }
        assertEquals(1.3f, ef, 0.0001f)
    }

    @Test
    fun `previewIntervals calculates preview for all four ratings`() {
        val preview = engine.previewIntervals(
            currentRepetitions = 2,
            currentEaseFactor = 2.5f,
            currentIntervalDays = 6,
            nowMillis = baseTime
        )
        assertEquals(0, preview[ReviewQuality.FORGOT])
        assertEquals(14, preview[ReviewQuality.HARD])
        assertEquals(15, preview[ReviewQuality.GOOD])
        assertEquals(16, preview[ReviewQuality.EASY]) // round(6 * 2.6) = 16
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative repetitions throws IllegalArgumentException`() {
        engine.calculate(-1, 2.5f, 0, ReviewQuality.GOOD, baseTime)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative interval throws IllegalArgumentException`() {
        engine.calculate(0, 2.5f, -5, ReviewQuality.GOOD, baseTime)
    }
}
