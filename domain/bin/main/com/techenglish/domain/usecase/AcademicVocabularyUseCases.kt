package com.techenglish.domain.usecase

import com.techenglish.domain.algorithm.Sm2Engine
import com.techenglish.domain.model.AcademicDashboardSummary
import com.techenglish.domain.model.AcademicSearchFilter
import com.techenglish.domain.model.AcademicSrsMode
import com.techenglish.domain.model.AcademicTerm
import com.techenglish.domain.model.AcademicTermDetail
import com.techenglish.domain.model.PaginatedAcademicTerms
import com.techenglish.domain.model.ReviewQuality
import com.techenglish.domain.model.Sm2Result
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.StudyLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class NormalizeAcademicQueryUseCase @Inject constructor() {
    operator fun invoke(rawQuery: String): String =
        rawQuery.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
}

class GetAcademicDashboardUseCase @Inject constructor(
    private val repository: AcademicVocabularyRepository
) {
    operator fun invoke(nowMillis: Long = System.currentTimeMillis()): Flow<AcademicDashboardSummary> {
        val countsFlow = combine(
            repository.observeVerifiedCount(),
            repository.observeStudiedCount(),
            repository.observeFavoritesCount(),
            repository.observeDifficultCount(),
            repository.observeDueSrsCount(nowMillis)
        ) { total, studied, favs, diff, due ->
            intArrayOf(total, studied, favs, diff, due)
        }

        return combine(
            countsFlow,
            repository.observeMajorCategoryCounts(),
            repository.observeDomainCounts(),
            repository.observeRecentlyStudied(limit = 8)
        ) { counts, majorCats, domains, recent ->
            AcademicDashboardSummary(
                totalVerifiedTerms = counts[0],
                studiedCount = counts[1],
                favoritesCount = counts[2],
                difficultCount = counts[3],
                dueSrsCount = counts[4],
                majorCategories = majorCats,
                domains = domains,
                recentlyStudied = recent
            )
        }
    }
}

class SearchAcademicTermsPaginatedUseCase @Inject constructor(
    private val repository: AcademicVocabularyRepository,
    private val normalizeQuery: NormalizeAcademicQueryUseCase
) {
    suspend operator fun invoke(
        filter: AcademicSearchFilter,
        page: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        nowMillis: Long = System.currentTimeMillis()
    ): PaginatedAcademicTerms {
        val safePage = page.coerceAtLeast(0)
        val safeSize = pageSize.coerceIn(10, 100)
        val normalizedFilter = filter.copy(query = normalizeQuery(filter.query))
        val items = repository.searchPaginated(
            filter = normalizedFilter,
            nowMillis = nowMillis,
            limit = safeSize,
            offset = safePage * safeSize
        )
        val total = repository.countMatching(normalizedFilter, nowMillis)
        return PaginatedAcademicTerms(
            items = items,
            totalMatchingCount = total,
            page = safePage,
            pageSize = safeSize
        )
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 40
    }
}

class GetAcademicTermDetailUseCase @Inject constructor(
    private val repository: AcademicVocabularyRepository,
    private val normalizeQuery: NormalizeAcademicQueryUseCase
) {
    suspend operator fun invoke(termId: Int): AcademicTermDetail? =
        repository.getTermDetail(termId)

    suspend fun resolveRelatedTermId(relatedTermName: String): Int? =
        repository.findByNormalizedTerm(normalizeQuery(relatedTermName))?.id
}

class ToggleAcademicFavoriteUseCase @Inject constructor(
    private val repository: AcademicVocabularyRepository
) {
    suspend operator fun invoke(termId: Int, isFavorite: Boolean) {
        repository.setFavorite(termId, isFavorite)
    }
}

class EnqueueAcademicSrsUseCase @Inject constructor(
    private val repository: AcademicVocabularyRepository
) {
    suspend operator fun invoke(termId: Int, inSrs: Boolean) {
        repository.setInSrs(termId, inSrs)
    }
}

class MarkAcademicTermLearnedUseCase @Inject constructor(
    private val repository: AcademicVocabularyRepository,
    private val studyLogRepository: StudyLogRepository
) {
    suspend operator fun invoke(
        term: AcademicTerm,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val intervalDays = 15
        val nextReviewAt = nowMillis + intervalDays * Sm2Engine.MILLIS_PER_DAY
        repository.updateSrsProgress(
            termId = term.id,
            repetitions = maxOf(3, term.repetitions + 1),
            easeFactor = maxOf(2.5f, term.easeFactor),
            intervalDays = intervalDays,
            nextReviewAt = nextReviewAt,
            nowMillis = nowMillis
        )
        studyLogRepository.insertLog(
            StudyLog(
                actionType = StudyActionType.FLASHCARD,
                isCorrect = true,
                xpEarned = ReviewWordUseCase.XP_FLASHCARD_CORRECT,
                timestamp = nowMillis
            )
        )
    }
}

class GetAcademicSrsDeckUseCase @Inject constructor(
    private val repository: AcademicVocabularyRepository
) {
    suspend operator fun invoke(
        mode: AcademicSrsMode,
        limit: Int = 20,
        nowMillis: Long = System.currentTimeMillis()
    ): List<AcademicTerm> = repository.getSrsDeckForMode(
        mode = mode,
        nowMillis = nowMillis,
        limit = limit.coerceIn(1, 100)
    )
}

class ReviewAcademicTermUseCase @Inject constructor(
    private val sm2Engine: Sm2Engine,
    private val repository: AcademicVocabularyRepository,
    private val studyLogRepository: StudyLogRepository
) {
    suspend operator fun invoke(
        term: AcademicTerm,
        quality: ReviewQuality,
        nowMillis: Long = System.currentTimeMillis()
    ): Sm2Result {
        val result = sm2Engine.calculate(
            currentRepetitions = term.repetitions,
            currentEaseFactor = term.easeFactor,
            currentIntervalDays = term.intervalDays,
            quality = quality,
            nowMillis = nowMillis
        )
        repository.updateSrsProgress(
            termId = term.id,
            repetitions = result.repetitions,
            easeFactor = result.easeFactor,
            intervalDays = result.intervalDays,
            nextReviewAt = result.nextReviewAt,
            nowMillis = nowMillis
        )
        val xp = if (quality.isCorrect) ReviewWordUseCase.XP_FLASHCARD_CORRECT else ReviewWordUseCase.XP_FLASHCARD_RETRY
        studyLogRepository.insertLog(
            StudyLog(
                actionType = StudyActionType.FLASHCARD,
                isCorrect = quality.isCorrect,
                xpEarned = xp,
                timestamp = nowMillis
            )
        )
        return result
    }

    fun previewIntervals(
        term: AcademicTerm,
        nowMillis: Long = System.currentTimeMillis()
    ): Map<ReviewQuality, Int> = sm2Engine.previewIntervals(
        currentRepetitions = term.repetitions,
        currentEaseFactor = term.easeFactor,
        currentIntervalDays = term.intervalDays,
        nowMillis = nowMillis
    )
}
