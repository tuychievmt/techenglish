package com.techenglish.domain.usecase

import com.techenglish.domain.algorithm.Sm2Engine
import com.techenglish.domain.model.ReviewQuality
import com.techenglish.domain.model.Sm2Result
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.Word
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CalculateNextReviewUseCase @Inject constructor(
    private val sm2Engine: Sm2Engine
) {
    operator fun invoke(
        word: Word,
        quality: ReviewQuality,
        nowMillis: Long = System.currentTimeMillis()
    ): Sm2Result = sm2Engine.calculate(
        currentRepetitions = word.repetitions,
        currentEaseFactor = word.easeFactor,
        currentIntervalDays = word.intervalDays,
        quality = quality,
        nowMillis = nowMillis
    )

    fun previewIntervals(
        word: Word,
        nowMillis: Long = System.currentTimeMillis()
    ): Map<ReviewQuality, Int> = sm2Engine.previewIntervals(
        currentRepetitions = word.repetitions,
        currentEaseFactor = word.easeFactor,
        currentIntervalDays = word.intervalDays,
        nowMillis = nowMillis
    )
}

class GetDueWordsUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {
    suspend operator fun invoke(
        limit: Int = 20,
        categoryId: Int? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Word> = vocabularyRepository.getDueWords(
        nowMillis = nowMillis,
        limit = limit.coerceAtLeast(1),
        categoryId = categoryId
    )

    fun observeCount(nowMillis: Long = System.currentTimeMillis()): Flow<Int> =
        vocabularyRepository.observeDueCount(nowMillis)
}

class ReviewWordUseCase @Inject constructor(
    private val calculateNextReview: CalculateNextReviewUseCase,
    private val vocabularyRepository: VocabularyRepository,
    private val studyLogRepository: StudyLogRepository
) {
    suspend operator fun invoke(
        word: Word,
        quality: ReviewQuality,
        nowMillis: Long = System.currentTimeMillis()
    ): Sm2Result {
        val result = calculateNextReview(word, quality, nowMillis)
        vocabularyRepository.updateSrsState(
            wordId = word.id,
            repetitions = result.repetitions,
            easeFactor = result.easeFactor,
            intervalDays = result.intervalDays,
            nextReviewAt = result.nextReviewAt
        )
        val xpEarned = if (quality.isCorrect) XP_FLASHCARD_CORRECT else XP_FLASHCARD_RETRY
        studyLogRepository.insertLog(
            StudyLog(
                wordId = word.id,
                actionType = StudyActionType.FLASHCARD,
                isCorrect = quality.isCorrect,
                xpEarned = xpEarned,
                timestamp = nowMillis
            )
        )
        return result
    }

    companion object {
        const val XP_FLASHCARD_CORRECT = 10
        const val XP_FLASHCARD_RETRY = 5
    }
}

class AddCustomWordUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {
    suspend operator fun invoke(
        term: String,
        translation: String,
        transcription: String = "",
        example: String = "",
        explanationRu: String = "",
        categoryId: Int? = null,
        languageId: Int? = null
    ): Result<Long> {
        val cleanTerm = term.trim()
        val cleanTranslation = translation.trim()
        if (cleanTerm.isEmpty()) {
            return Result.failure(IllegalArgumentException("Термин не может быть пустым"))
        }
        if (cleanTranslation.isEmpty()) {
            return Result.failure(IllegalArgumentException("Перевод не может быть пустым"))
        }
        val targetCategoryId = categoryId ?: vocabularyRepository.getSystemCategoryId()
        val word = Word(
            id = 0,
            categoryId = targetCategoryId,
            categoryName = "",
            languageId = languageId,
            term = cleanTerm,
            transcription = transcription.trim().ifEmpty { "/${cleanTerm.lowercase()}/" },
            translation = cleanTranslation,
            example = example.trim().ifEmpty { "Use '$cleanTerm' in your daily engineering workflow." },
            explanationRu = explanationRu.trim().ifEmpty { cleanTranslation },
            isFavorite = false,
            isCustom = true,
            repetitions = 0,
            easeFactor = Sm2Engine.INITIAL_EASE_FACTOR,
            intervalDays = 0,
            nextReviewAt = 0L
        )
        return Result.success(vocabularyRepository.insertWord(word))
    }
}

class UpdateCustomWordUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {
    suspend operator fun invoke(word: Word): Result<Unit> {
        if (!word.isCustom) {
            return Result.failure(IllegalStateException("Можно редактировать только пользовательские термины"))
        }
        val cleanTerm = word.term.trim()
        val cleanTranslation = word.translation.trim()
        if (cleanTerm.isEmpty() || cleanTranslation.isEmpty()) {
            return Result.failure(IllegalArgumentException("Термин и перевод обязательны"))
        }
        vocabularyRepository.updateWord(
            word.copy(
                term = cleanTerm,
                translation = cleanTranslation,
                transcription = word.transcription.trim(),
                example = word.example.trim(),
                explanationRu = word.explanationRu.trim()
            )
        )
        return Result.success(Unit)
    }
}

class DeleteCustomWordUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {
    suspend operator fun invoke(wordId: Int): Boolean =
        vocabularyRepository.deleteCustomWord(wordId)
}

class SearchWordsUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {
    operator fun invoke(query: String): Flow<List<Word>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            vocabularyRepository.observeAllWords()
        } else {
            vocabularyRepository.searchWords(trimmed)
        }
    }
}

class ToggleFavoriteWordUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {
    suspend operator fun invoke(wordId: Int, isFavorite: Boolean) {
        vocabularyRepository.setFavorite(wordId, isFavorite)
    }
}

/**
 * Adds a tapped word from Technical Documentation, Code Breakdown, or Work Scenarios
 * into the user's personal SRS deck («Мои слова»), making it immediately available for review.
 */
class AddWordFromDocUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {
    suspend operator fun invoke(
        term: String,
        translation: String,
        transcription: String = "",
        example: String = "",
        explanationRu: String = "",
        languageId: Int? = null
    ): Result<Long> {
        val cleanTerm = term.trim()
        if (cleanTerm.isEmpty()) {
            return Result.failure(IllegalArgumentException("Пустой термин"))
        }
        val existing = vocabularyRepository.findByExactTerm(cleanTerm)
        if (existing != null) {
            // Make sure existing word is starred and immediately due for SRS review
            vocabularyRepository.setFavorite(existing.id, true)
            vocabularyRepository.updateSrsState(
                wordId = existing.id,
                repetitions = existing.repetitions,
                easeFactor = existing.easeFactor,
                intervalDays = existing.intervalDays,
                nextReviewAt = 0L
            )
            return Result.success(existing.id.toLong())
        }
        val systemCategoryId = vocabularyRepository.getSystemCategoryId()
        val insertedId = vocabularyRepository.insertWord(
            Word(
                id = 0,
                categoryId = systemCategoryId,
                categoryName = "Мои слова",
                languageId = languageId,
                term = cleanTerm,
                transcription = transcription.trim().ifEmpty { "/${cleanTerm.lowercase()}/" },
                translation = translation.trim().ifEmpty { cleanTerm },
                example = example.trim().ifEmpty { "Encountered '$cleanTerm' in technical documentation." },
                explanationRu = explanationRu.trim().ifEmpty { translation.trim() },
                isFavorite = true,
                isCustom = true,
                repetitions = 0,
                easeFactor = Sm2Engine.INITIAL_EASE_FACTOR,
                intervalDays = 0,
                nextReviewAt = 0L
            )
        )
        return Result.success(insertedId)
    }
}
