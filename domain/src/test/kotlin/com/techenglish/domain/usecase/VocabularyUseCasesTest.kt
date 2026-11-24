package com.techenglish.domain.usecase

import com.techenglish.domain.FakeStudyLogRepository
import com.techenglish.domain.FakeVocabularyRepository
import com.techenglish.domain.algorithm.Sm2Engine
import com.techenglish.domain.model.ReviewQuality
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.Word
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyUseCasesTest {

    private val now = 1_700_000_000_000L

    private fun sampleWord(id: Int = 1, isCustom: Boolean = false, nextReviewAt: Long = 0L) = Word(
        id = id,
        categoryId = 1,
        categoryName = "Backend",
        term = "idempotency",
        transcription = "/ˌaɪ.demˈpoʊ.tən.si/",
        translation = "идемпотентность",
        example = "PUT endpoints must be idempotent.",
        explanationRu = "Повторный вызов даёт тот же результат.",
        isCustom = isCustom,
        nextReviewAt = nextReviewAt
    )

    @Test
    fun `reviewWord updates SRS state and inserts study log`() = runTest {
        val word = sampleWord(id = 1)
        val vocabRepo = FakeVocabularyRepository(listOf(word))
        val logRepo = FakeStudyLogRepository()
        val calc = CalculateNextReviewUseCase(Sm2Engine())
        val reviewWord = ReviewWordUseCase(calc, vocabRepo, logRepo)

        val result = reviewWord(word, ReviewQuality.GOOD, nowMillis = now)

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        val stored = vocabRepo.getWordById(1)!!
        assertEquals(1, stored.repetitions)
        assertEquals(now + Sm2Engine.MILLIS_PER_DAY, stored.nextReviewAt)
        assertEquals(1, logRepo.currentLogs.size)
        assertEquals(StudyActionType.FLASHCARD, logRepo.currentLogs.first().actionType)
        assertTrue(logRepo.currentLogs.first().isCorrect)
        assertEquals(ReviewWordUseCase.XP_FLASHCARD_CORRECT, logRepo.currentLogs.first().xpEarned)
    }

    @Test
    fun `reviewWord with FORGOT awards retry XP and keeps word due`() = runTest {
        val word = sampleWord(id = 2).copy(repetitions = 3, intervalDays = 15)
        val vocabRepo = FakeVocabularyRepository(listOf(word))
        val logRepo = FakeStudyLogRepository()
        val reviewWord = ReviewWordUseCase(CalculateNextReviewUseCase(Sm2Engine()), vocabRepo, logRepo)

        val result = reviewWord(word, ReviewQuality.FORGOT, nowMillis = now)

        assertEquals(0, result.repetitions)
        assertEquals(0, result.intervalDays)
        assertFalse(logRepo.currentLogs.first().isCorrect)
        assertEquals(ReviewWordUseCase.XP_FLASHCARD_RETRY, logRepo.currentLogs.first().xpEarned)
    }

    @Test
    fun `getDueWords filters out future scheduled words`() = runTest {
        val due = sampleWord(id = 1, nextReviewAt = 0L)
        val future = sampleWord(id = 2, nextReviewAt = now + Sm2Engine.MILLIS_PER_DAY)
        val vocabRepo = FakeVocabularyRepository(listOf(due, future))
        val useCase = GetDueWordsUseCase(vocabRepo)

        val dueList = useCase(limit = 10, nowMillis = now)
        assertEquals(1, dueList.size)
        assertEquals(1, dueList.first().id)
        assertEquals(1, useCase.observeCount(now).first())
    }

    @Test
    fun `custom word add edit delete lifecycle and validations`() = runTest {
        val vocabRepo = FakeVocabularyRepository(listOf(sampleWord(id = 1, isCustom = false)))
        val addUseCase = AddCustomWordUseCase(vocabRepo)
        val updateUseCase = UpdateCustomWordUseCase(vocabRepo)
        val deleteUseCase = DeleteCustomWordUseCase(vocabRepo)

        assertTrue(addUseCase(term = "  ", translation = "тест").isFailure)
        assertTrue(addUseCase(term = "shard", translation = "  ").isFailure)

        val insertedId = addUseCase(term = "sharding", translation = "шардирование").getOrThrow().toInt()
        val inserted = vocabRepo.getWordById(insertedId)
        assertNotNull(inserted)
        assertTrue(inserted!!.isCustom)
        assertEquals(11, inserted.categoryId) // system category «Мои слова»

        // Editing built-in word rejected
        assertTrue(updateUseCase(sampleWord(id = 1, isCustom = false)).isFailure)
        // Editing custom word with blank fails
        assertTrue(updateUseCase(inserted.copy(term = "")).isFailure)
        // Editing custom word succeeds
        assertTrue(updateUseCase(inserted.copy(translation = "горизонтальное шардирование")).isSuccess)
        assertEquals("горизонтальное шардирование", vocabRepo.getWordById(insertedId)?.translation)

        // Deleting built-in fails, deleting custom succeeds
        assertFalse(deleteUseCase(1))
        assertTrue(deleteUseCase(insertedId))
    }

    @Test
    fun `searchWords and toggleFavorite work properly`() = runTest {
        val vocabRepo = FakeVocabularyRepository(listOf(sampleWord(id = 1)))
        val search = SearchWordsUseCase(vocabRepo)
        val fav = ToggleFavoriteWordUseCase(vocabRepo)

        assertEquals(1, search("").first().size)
        assertEquals(1, search("idemp").first().size)
        assertEquals(0, search("nonexistent").first().size)

        fav(1, true)
        assertTrue(vocabRepo.getWordById(1)!!.isFavorite)
    }

    @Test
    fun `addWordFromDoc inserts new word or reactivates existing word for SRS`() = runTest {
        val existing = sampleWord(id = 1, nextReviewAt = now + 999_999L)
        val vocabRepo = FakeVocabularyRepository(listOf(existing))
        val useCase = AddWordFromDocUseCase(vocabRepo)

        // Blank term rejected
        assertTrue(useCase("", "перевод").isFailure)

        // Existing word becomes favorite & immediately due (nextReviewAt = 0L)
        val existingRes = useCase("idempotency", "идемпотентность").getOrThrow()
        assertEquals(1L, existingRes)
        assertEquals(0L, vocabRepo.getWordById(1)!!.nextReviewAt)
        assertTrue(vocabRepo.getWordById(1)!!.isFavorite)

        // Brand-new word inserted into «Мои слова»
        val newId = useCase("backpressure", "обратное давление").getOrThrow().toInt()
        val created = vocabRepo.getWordById(newId)!!
        assertEquals("backpressure", created.term)
        assertTrue(created.isCustom)
        assertTrue(created.isFavorite)
    }
}
