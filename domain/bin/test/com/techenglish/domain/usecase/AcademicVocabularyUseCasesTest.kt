package com.techenglish.domain.usecase

import com.techenglish.domain.FakeStudyLogRepository
import com.techenglish.domain.algorithm.Sm2Engine
import com.techenglish.domain.model.AcademicDomainSummary
import com.techenglish.domain.model.AcademicMajorCategorySummary
import com.techenglish.domain.model.AcademicSearchFilter
import com.techenglish.domain.model.AcademicSrsMode
import com.techenglish.domain.model.AcademicStudyFilter
import com.techenglish.domain.model.AcademicTerm
import com.techenglish.domain.model.AcademicTermDetail
import com.techenglish.domain.model.ReviewQuality
import com.techenglish.domain.model.WordRelation
import com.techenglish.domain.model.WordSense
import com.techenglish.domain.model.WordSource
import com.techenglish.domain.repository.AcademicVocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAcademicVocabularyRepository(
    initialTerms: List<AcademicTerm>
) : AcademicVocabularyRepository {

    private val termsFlow = MutableStateFlow(initialTerms)
    private val sensesMap = mapOf(
        1 to listOf(
            WordSense(1, 1, 1, "Neural Net Backprop", "Deep Learning & Neural Networks", "Gradient computation via chain rule.", "Расчёт градиента.", "Ex EN", "Пр RU")
        )
    )
    private val relationsMap = mapOf(
        1 to listOf(WordRelation(1, 1, "Gradient Descent", "RELATED"))
    )
    private val sourceMap = mapOf(
        12 to WordSource(12, "PyTorch Docs", "https://pytorch.org/docs", "Official Documentation", "PyTorch")
    )

    override fun observeVerifiedCount(): Flow<Int> = termsFlow.map { it.size }
    override fun observeStudiedCount(): Flow<Int> = termsFlow.map { list -> list.count { it.repetitions >= 1 } }
    override fun observeFavoritesCount(): Flow<Int> = termsFlow.map { list -> list.count { it.isFavorite } }
    override fun observeDifficultCount(): Flow<Int> = termsFlow.map { list -> list.count { it.isDifficult } }
    override fun observeDueSrsCount(nowMillis: Long): Flow<Int> =
        termsFlow.map { list -> list.count { it.isDue(nowMillis) } }

    override fun observeMajorCategoryCounts(): Flow<List<AcademicMajorCategorySummary>> =
        termsFlow.map { list ->
            list.groupBy { it.majorCategory to it.categoryId }.map { (key, group) ->
                AcademicMajorCategorySummary(key.first, key.second, group.size, group.count { it.isLearned })
            }
        }

    override fun observeDomainCounts(): Flow<List<AcademicDomainSummary>> =
        termsFlow.map { list ->
            list.groupBy { it.domain to it.majorCategory }.map { (key, group) ->
                AcademicDomainSummary(key.first, key.second, group.size)
            }
        }

    override fun observeRecentlyStudied(limit: Int): Flow<List<AcademicTerm>> =
        termsFlow.map { list -> list.filter { it.lastStudiedAt > 0 }.sortedByDescending { it.lastStudiedAt }.take(limit) }

    override suspend fun getVerifiedCount(): Int = termsFlow.value.size
    override suspend fun getDueSrsCount(nowMillis: Long): Int = termsFlow.value.count { it.isDue(nowMillis) }

    override suspend fun searchPaginated(
        filter: AcademicSearchFilter,
        nowMillis: Long,
        limit: Int,
        offset: Int
    ): List<AcademicTerm> = filterList(filter, nowMillis).drop(offset).take(limit)

    override suspend fun countMatching(filter: AcademicSearchFilter, nowMillis: Long): Int =
        filterList(filter, nowMillis).size

    private fun filterList(filter: AcademicSearchFilter, nowMillis: Long): List<AcademicTerm> {
        val q = filter.query.lowercase()
        return termsFlow.value.filter { t ->
            (filter.majorCategory == null || t.majorCategory == filter.majorCategory) &&
                (filter.domain == null || t.domain == filter.domain) &&
                (filter.subdomain == null || t.subdomain == filter.subdomain) &&
                (filter.difficulty == null || t.difficultyLevel == filter.difficulty) &&
                (filter.cefr == null || t.cefrLevel == filter.cefr) &&
                (filter.partOfSpeech == null || t.partOfSpeech == filter.partOfSpeech) &&
                (!filter.onlyFavorites || t.isFavorite) &&
                (!filter.onlyDue || t.isDue(nowMillis)) &&
                (!filter.onlyDifficult || t.isDifficult) &&
                (filter.studyFilter == AcademicStudyFilter.ALL ||
                    (filter.studyFilter == AcademicStudyFilter.STUDIED && t.isLearned) ||
                    (filter.studyFilter == AcademicStudyFilter.NOT_STUDIED && !t.isLearned)) &&
                (q.isEmpty() ||
                    t.normalizedTerm.contains(q) ||
                    t.translationRu.lowercase().contains(q) ||
                    t.definitionEn.lowercase().contains(q) ||
                    t.abbreviations.lowercase().contains(q) ||
                    t.synonyms.any { it.lowercase().contains(q) })
        }
    }

    override suspend fun getTermDetail(termId: Int): AcademicTermDetail? {
        val term = termsFlow.value.firstOrNull { it.id == termId } ?: return null
        return AcademicTermDetail(
            term = term,
            senses = sensesMap[termId].orEmpty(),
            relations = relationsMap[termId].orEmpty(),
            source = term.sourceId?.let { sourceMap[it] }
        )
    }

    override suspend fun findByNormalizedTerm(normalizedTerm: String): AcademicTerm? =
        termsFlow.value.firstOrNull { it.normalizedTerm == normalizedTerm }

    override suspend fun getSrsDeckForMode(
        mode: AcademicSrsMode,
        nowMillis: Long,
        limit: Int
    ): List<AcademicTerm> = termsFlow.value.filter { t ->
        (mode.majorCategoryFilter == null || t.majorCategory == mode.majorCategoryFilter) &&
            (!mode.onlyCustomQueue || t.isInSrs || t.isFavorite) &&
            t.nextReviewAt <= nowMillis
    }.take(limit)

    override suspend fun setFavorite(termId: Int, isFavorite: Boolean) {
        termsFlow.value = termsFlow.value.map { if (it.id == termId) it.copy(isFavorite = isFavorite) else it }
    }

    override suspend fun setInSrs(termId: Int, isInSrs: Boolean) {
        termsFlow.value = termsFlow.value.map { if (it.id == termId) it.copy(isInSrs = isInSrs) else it }
    }

    override suspend fun updateSrsProgress(
        termId: Int,
        repetitions: Int,
        easeFactor: Float,
        intervalDays: Int,
        nextReviewAt: Long,
        nowMillis: Long
    ) {
        termsFlow.value = termsFlow.value.map {
            if (it.id == termId) {
                it.copy(
                    isInSrs = true,
                    repetitions = repetitions,
                    easeFactor = easeFactor,
                    intervalDays = intervalDays,
                    nextReviewAt = nextReviewAt,
                    lastStudiedAt = nowMillis
                )
            } else it
        }
    }
}

class AcademicVocabularyUseCasesTest {

    private val now = 1_763_553_600_000L

    private fun sampleTerms() = listOf(
        AcademicTerm(
            id = 1,
            term = "Backpropagation",
            normalizedTerm = "backpropagation",
            phoneticTranscription = "/ˌbæk.prɒp.əˈɡeɪ.ʃən/",
            partOfSpeech = "noun",
            translationRu = "обратное распространение ошибки",
            definitionEn = "Gradient computation algorithm via chain rule.",
            definitionRu = "Алгоритм вычисления градиентов.",
            academicExplanationEn = "Chain rule over computational graph.",
            academicExplanationRu = "Цепное правило на графе вычислений.",
            practicalExplanation = "Обучение нейросетей.",
            exampleEn = "Weights update via backpropagation.",
            exampleRu = "Веса обновляются методом обратного распространения.",
            majorCategory = "Machine Learning & Artificial Intelligence",
            categoryId = 101,
            subcategoryId = 1,
            domain = "Deep Learning & Neural Networks",
            subdomain = "Backpropagation",
            difficultyLevel = "Advanced",
            cefrLevel = "C1",
            synonyms = listOf("Reverse-Mode Differentiation"),
            relatedTerms = listOf("Gradient Descent"),
            abbreviations = "BP",
            sourceReference = "PyTorch Docs",
            sourceId = 12
        ),
        AcademicTerm(
            id = 2,
            term = "Gradient Descent",
            normalizedTerm = "gradient descent",
            partOfSpeech = "noun phrase",
            translationRu = "градиентный спуск",
            definitionEn = "First-order iterative optimization algorithm.",
            definitionRu = "Итерационный алгоритм оптимизации первого порядка.",
            academicExplanationEn = "Steps opposite to gradient.",
            academicExplanationRu = "Шаги против градиента.",
            practicalExplanation = "Минимизация функции потерь.",
            exampleEn = "SGD minimizes loss.",
            exampleRu = "SGD минимизирует потери.",
            majorCategory = "Machine Learning & Artificial Intelligence",
            categoryId = 101,
            subcategoryId = 2,
            domain = "Artificial Intelligence & Machine Learning",
            subdomain = "Gradient Descent",
            difficultyLevel = "Intermediate",
            cefrLevel = "B2",
            abbreviations = "GD",
            sourceId = 12
        ),
        AcademicTerm(
            id = 3,
            term = "Homomorphic Encryption",
            normalizedTerm = "homomorphic encryption",
            partOfSpeech = "noun phrase",
            translationRu = "гомоморфное шифрование",
            definitionEn = "Computation directly on ciphertexts.",
            definitionRu = "Вычисления над шифротекстами.",
            academicExplanationEn = "Ring homomorphism over lattice ciphertexts.",
            academicExplanationRu = "Гомоморфизм колец над шифротекстами.",
            practicalExplanation = "Конфиденциальные облачные вычисления.",
            exampleEn = "FHE evaluates encrypted queries.",
            exampleRu = "FHE выполняет зашифрованные запросы.",
            majorCategory = "Cryptography & Cybersecurity",
            categoryId = 102,
            subcategoryId = 3,
            domain = "Cryptography & Applied Cryptography",
            subdomain = "Post-Quantum Cryptography",
            difficultyLevel = "Research Level",
            cefrLevel = "C2",
            abbreviations = "FHE",
            sourceId = 1
        )
    )

    @Test
    fun `normalize query strips extra spaces and lowercases`() {
        val norm = NormalizeAcademicQueryUseCase()
        assertEquals("backpropagation algorithm", norm("  Backpropagation   Algorithm  "))
    }

    @Test
    fun `dashboard and paginated search filter by query, domain, difficulty, and study state`() = runTest {
        val repo = FakeAcademicVocabularyRepository(sampleTerms())
        val norm = NormalizeAcademicQueryUseCase()
        val dashboardUseCase = GetAcademicDashboardUseCase(repo)
        val searchUseCase = SearchAcademicTermsPaginatedUseCase(repo, norm)

        val dash = dashboardUseCase(now).first()
        assertEquals(3, dash.totalVerifiedTerms)
        assertEquals(2, dash.majorCategories.size)
        assertEquals(3, dash.domains.size)
        assertEquals(2, dash.difficultCount)

        // Search by Russian translation
        val ruHit = searchUseCase(AcademicSearchFilter(query = "гомоморфное"), page = 0, pageSize = 10, nowMillis = now)
        assertEquals(1, ruHit.totalMatchingCount)
        assertEquals("Homomorphic Encryption", ruHit.items.first().term)
        assertFalse(ruHit.hasMore)

        // Search by abbreviation
        val abbrHit = searchUseCase(AcademicSearchFilter(query = "BP"), page = 0, pageSize = 10, nowMillis = now)
        assertEquals(1, abbrHit.items.size)
        assertEquals("Backpropagation", abbrHit.items.first().term)

        // Filter by difficulty
        val researchHit = searchUseCase(AcademicSearchFilter(difficulty = "Research Level"), nowMillis = now)
        assertEquals(1, researchHit.items.size)

        // Pagination check with pageSize = 2 -> page 0 hasMore = true
        val page0 = searchUseCase(AcademicSearchFilter(), page = 0, pageSize = 10, nowMillis = now)
        assertEquals(3, page0.items.size)
    }

    @Test
    fun `term detail loads senses relations source and resolves related term navigation`() = runTest {
        val repo = FakeAcademicVocabularyRepository(sampleTerms())
        val detailUseCase = GetAcademicTermDetailUseCase(repo, NormalizeAcademicQueryUseCase())

        val detail = detailUseCase(1)
        assertNotNull(detail)
        assertEquals("Backpropagation", detail!!.term.term)
        assertEquals(1, detail.senses.size)
        assertEquals(1, detail.relations.size)
        assertEquals("PyTorch Docs", detail.source?.title)

        val resolvedId = detailUseCase.resolveRelatedTermId("  Gradient   Descent ")
        assertEquals(2, resolvedId)
        assertNull(detailUseCase.resolveRelatedTermId("Nonexistent Term"))
    }

    @Test
    fun `favorites, SRS queue, markLearned, and 6-mode SM2 review update progress and logs`() = runTest {
        val repo = FakeAcademicVocabularyRepository(sampleTerms())
        val logRepo = FakeStudyLogRepository()
        val favUseCase = ToggleAcademicFavoriteUseCase(repo)
        val srsQueueUseCase = EnqueueAcademicSrsUseCase(repo)
        val markLearnedUseCase = MarkAcademicTermLearnedUseCase(repo, logRepo)
        val getDeckUseCase = GetAcademicSrsDeckUseCase(repo)
        val reviewUseCase = ReviewAcademicTermUseCase(Sm2Engine(), repo, logRepo)

        favUseCase(1, true)
        srsQueueUseCase(3, true)

        val customDeck = getDeckUseCase(AcademicSrsMode.CUSTOM_VOCABULARY, limit = 10, nowMillis = now)
        assertEquals(2, customDeck.size)

        val mlDeck = getDeckUseCase(AcademicSrsMode.MACHINE_LEARNING, limit = 10, nowMillis = now)
        assertEquals(2, mlDeck.size)

        val cryptoDeck = getDeckUseCase(AcademicSrsMode.CRYPTOGRAPHY, limit = 10, nowMillis = now)
        assertEquals(1, cryptoDeck.size)

        // Preview intervals and review
        val term1 = mlDeck.first()
        val previews = reviewUseCase.previewIntervals(term1, now)
        assertEquals(1, previews[ReviewQuality.GOOD])

        val sm2Res = reviewUseCase(term1, ReviewQuality.GOOD, nowMillis = now)
        assertEquals(1, sm2Res.repetitions)
        assertEquals(1, sm2Res.intervalDays)
        assertTrue(repo.getTermDetail(term1.id)!!.term.isLearned)
        assertEquals(1, logRepo.currentLogs.size)

        // Mark term 2 as learned directly
        val term2 = repo.getTermDetail(2)!!.term
        markLearnedUseCase(term2, nowMillis = now)
        val updated2 = repo.getTermDetail(2)!!.term
        assertTrue(updated2.isMastered)
        assertFalse(updated2.isDue(now))
        assertEquals(2, logRepo.currentLogs.size)
    }
}
