package com.techenglish.domain.usecase

import com.techenglish.domain.FakeSettingsRepository
import com.techenglish.domain.FakeStudyLogRepository
import com.techenglish.domain.FakeVocabularyRepository
import com.techenglish.domain.model.AcademicDomainSummary
import com.techenglish.domain.model.AcademicMajorCategorySummary
import com.techenglish.domain.model.AcademicSearchFilter
import com.techenglish.domain.model.AcademicSrsMode
import com.techenglish.domain.model.AcademicTerm
import com.techenglish.domain.model.AcademicTermDetail
import com.techenglish.domain.model.ChameleonThemeType
import com.techenglish.domain.model.CustomImportedDoc
import com.techenglish.domain.model.DeckFormat
import com.techenglish.domain.model.ScenarioChameleonRegistry
import com.techenglish.domain.model.ScenarioType
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.TimeAttackMode
import com.techenglish.domain.model.TimeAttackRecord
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.model.Word
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.CustomDocRepository
import com.techenglish.domain.repository.TimeAttackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class Stage2OfflineFeaturesUseCasesTest {

    private class FakeStage2AcademicRepo(initial: List<AcademicTerm>) : AcademicVocabularyRepository {
        private val list = initial.toMutableList()
        override fun observeVerifiedCount(): Flow<Int> = MutableStateFlow(list.size)
        override fun observeStudiedCount(): Flow<Int> = MutableStateFlow(list.count { it.repetitions > 0 })
        override fun observeFavoritesCount(): Flow<Int> = MutableStateFlow(list.count { it.isFavorite })
        override fun observeDifficultCount(): Flow<Int> = MutableStateFlow(list.count { it.isDifficult })
        override fun observeDueSrsCount(nowMillis: Long): Flow<Int> = MutableStateFlow(list.count { it.isDue(nowMillis) })
        override fun observeMajorCategoryCounts(): Flow<List<AcademicMajorCategorySummary>> = MutableStateFlow(emptyList())
        override fun observeDomainCounts(): Flow<List<AcademicDomainSummary>> = MutableStateFlow(emptyList())
        override fun observeRecentlyStudied(limit: Int): Flow<List<AcademicTerm>> = MutableStateFlow(emptyList())
        override suspend fun getVerifiedCount(): Int = list.size
        override suspend fun getDueSrsCount(nowMillis: Long): Int = list.count { it.isDue(nowMillis) }
        override suspend fun searchPaginated(filter: AcademicSearchFilter, nowMillis: Long, limit: Int, offset: Int): List<AcademicTerm> = list.take(limit)
        override suspend fun countMatching(filter: AcademicSearchFilter, nowMillis: Long): Int = list.size
        override suspend fun getTermDetail(termId: Int): AcademicTermDetail? = null
        override suspend fun findByNormalizedTerm(normalizedTerm: String): AcademicTerm? =
            list.firstOrNull { it.normalizedTerm == normalizedTerm }
        override suspend fun findByNormalizedTerms(normalizedTerms: List<String>): List<AcademicTerm> {
            val set = normalizedTerms.toSet()
            return list.filter { it.normalizedTerm in set }
        }
        override suspend fun getRandomVerifiedTerms(limit: Int): List<AcademicTerm> = list.take(limit)
        override suspend fun getSrsDeckForMode(mode: AcademicSrsMode, nowMillis: Long, limit: Int): List<AcademicTerm> = emptyList()
        override suspend fun setFavorite(termId: Int, isFavorite: Boolean) = Unit
        override suspend fun setInSrs(termId: Int, isInSrs: Boolean) = Unit
        override suspend fun updateSrsProgress(termId: Int, repetitions: Int, easeFactor: Float, intervalDays: Int, nextReviewAt: Long, nowMillis: Long) = Unit
    }

    private class FakeCustomDocRepo : CustomDocRepository {
        private val flow = MutableStateFlow<List<CustomImportedDoc>>(emptyList())
        private var nextId = 1
        override fun observeAllCustomDocs(): Flow<List<CustomImportedDoc>> = flow
        override suspend fun getCustomDocById(id: Int): CustomImportedDoc? = flow.value.firstOrNull { it.id == id }
        override suspend fun saveCustomDoc(doc: CustomImportedDoc): Long {
            val id = if (doc.id > 0) doc.id else nextId++
            flow.value = listOf(doc.copy(id = id)) + flow.value.filterNot { it.id == id }
            return id.toLong()
        }
        override suspend fun deleteCustomDoc(id: Int) {
            flow.value = flow.value.filterNot { it.id == id }
        }
    }

    private class FakeTimeAttackRepo : TimeAttackRepository {
        private val flow = MutableStateFlow<List<TimeAttackRecord>>(emptyList())
        private var nextId = 1
        override fun observeTopRecords(limit: Int): Flow<List<TimeAttackRecord>> =
            flow.map { list -> list.sortedByDescending { it.score }.take(limit) }
        override suspend fun getPersonalBestScore(mode: TimeAttackMode): Int =
            flow.value.filter { it.mode == mode }.maxOfOrNull { it.score } ?: 0
        override suspend fun saveRecord(record: TimeAttackRecord): Long {
            val id = nextId++
            flow.value = flow.value + record.copy(id = id)
            return id.toLong()
        }
    }

    private fun sampleTerm(
        id: Int,
        term: String,
        ru: String,
        tg: String = "",
        ipa: String = "/test/",
        domain: String = "Distributed Systems",
        isInSrs: Boolean = false
    ) = AcademicTerm(
        id = id,
        term = term,
        normalizedTerm = term.lowercase(),
        phoneticTranscription = ipa,
        partOfSpeech = "noun",
        translationRu = ru,
        translationTg = tg,
        definitionEn = "Definition for $term",
        definitionRu = "Определение для $ru",
        academicExplanationEn = "Academic EN",
        academicExplanationRu = "Academic RU",
        practicalExplanation = "Practical RU",
        exampleEn = "Example EN",
        exampleRu = "Example RU",
        majorCategory = "Databases & Distributed Systems",
        categoryId = 106,
        subcategoryId = 1,
        domain = domain,
        subdomain = "Core",
        difficultyLevel = "Intermediate",
        cefrLevel = "B2",
        isInSrs = isInSrs,
        nextReviewAt = 0L
    )

    @Test
    fun `FT-7_1 scans multi-word n-grams, inflected stems, and persists custom docs`() = runTest {
        val academicRepo = FakeStage2AcademicRepo(
            listOf(
                sampleTerm(1, "eventual consistency", "согласованность в конечном счёте", "мувофиқати ниҳоӣ"),
                sampleTerm(2, "deadlock", "взаимная блокировка"),
                sampleTerm(3, "cache", "кэш"),
                sampleTerm(4, "repository", "репозиторий")
            )
        )
        val vocabRepo = FakeVocabularyRepository(
            listOf(
                Word(10, 1, "Backend", term = "idempotency", transcription = "/aɪ/", translation = "идемпотентность", example = "", explanationRu = "Повторный вызов безопасен")
            )
        )
        val customDocRepo = FakeCustomDocRepo()
        val useCase = ScanCustomDocTermsUseCase(academicRepo, vocabRepo, customDocRepo)

        val saved = useCase.importAndSaveDocument(
            title = "README.md",
            sourceFileName = "README.md",
            content = "Our repositories use eventual consistency to avoid deadlocks while caching and enforcing idempotency.",
            nowMillis = 1000L
        )

        assertTrue(saved.id > 0)
        assertTrue(saved.detectedTermsCount >= 4)
        val normalizedMatches = saved.detectedTerms.map { it.normalizedTerm }.toSet()
        assertTrue("eventual consistency" in normalizedMatches)
        assertTrue("deadlock" in normalizedMatches)
        assertTrue("cache" in normalizedMatches)
        assertTrue("repository" in normalizedMatches)
        assertTrue("idempotency" in normalizedMatches)

        val ecSpan = saved.detectedTerms.first { it.normalizedTerm == "eventual consistency" }
        assertEquals("согласованность в конечном счёте", ecSpan.displayTranslation(TranslationLanguage.RU))
        assertEquals("мувофиқати ниҳоӣ", ecSpan.displayTranslation(TranslationLanguage.TJ))

        val loaded = useCase.getImportedDoc(saved.id)
        assertNotNull(loaded)
        assertEquals(1, useCase.observeImportedDocs().first().size)
        useCase.deleteImportedDoc(saved.id)
        assertEquals(0, useCase.observeImportedDocs().first().size)
        assertTrue(useCase.scanText("   ").isEmpty())
    }

    @Test
    fun `FT-7_2 debug the log trainer grades root cause and English bug report assembly`() = runTest {
        val logRepo = FakeStudyLogRepository()
        val useCase = DebugLogTrainerUseCase(logRepo)
        val challenges = useCase.getChallenges()
        assertTrue(challenges.size >= 5)

        val first = challenges.first()
        val solved = useCase.submitDiagnosis(
            challenge = first,
            selectedRootCause = first.correctRootCause,
            assembledReportTokens = first.expectedReportEn.split(" "),
            nowMillis = 2000L
        )
        assertTrue(solved.isRootCauseCorrect)
        assertTrue(solved.isReportCorrect)
        assertTrue(solved.isFullySolved)
        assertEquals(first.xpReward, solved.xpEarned)

        val partial = useCase.submitDiagnosis(
            challenge = first,
            selectedRootCause = first.correctRootCause,
            assembledReportTokens = listOf("wrong", "report"),
            nowMillis = 3000L
        )
        assertTrue(partial.isRootCauseCorrect)
        assertFalse(partial.isReportCorrect)
        assertFalse(partial.isFullySolved)
        assertEquals(10, partial.xpEarned)

        val failed = useCase.submitDiagnosis(
            challenge = first,
            selectedRootCause = "Wrong option",
            assembledReportTokens = listOf("wrong"),
            nowMillis = 4000L
        )
        assertFalse(failed.isRootCauseCorrect)
        assertEquals(3, failed.xpEarned)
        assertEquals(3, logRepo.currentLogs.size)
    }

    @Test
    fun `FT-7_3 pronunciation evaluator computes word-by-word diff and accuracy`() = runTest {
        val logRepo = FakeStudyLogRepository()
        val useCase = EvaluatePronunciationUseCase(logRepo)
        assertTrue(useCase.getShadowingPrompts().size >= 8)

        val exact = useCase.evaluate(
            referenceText = "Yesterday I finished the database migration",
            spokenText = "Yesterday I finished the database migration",
            nowMillis = 5000L
        )
        assertEquals(100, exact.accuracyPercent)
        assertTrue(exact.isPassed)
        assertTrue(exact.wordStatuses.all { it.isCorrect })
        assertEquals(20, exact.xpEarned)

        val fuzzy = useCase.evaluate(
            referenceText = "asynchronous polymorphism vulnerability",
            spokenText = "asynchronus polymorphism wrongword",
            nowMillis = 6000L
        )
        // asynchronous <-> asynchronus (edit distance 1, len >= 6 => match!), polymorphism => match, vulnerability => miss
        assertEquals(66, fuzzy.accuracyPercent)
        assertFalse(fuzzy.isPassed)
        assertTrue(fuzzy.wordStatuses[0].isCorrect)
        assertTrue(fuzzy.wordStatuses[1].isCorrect)
        assertFalse(fuzzy.wordStatuses[2].isCorrect)
        assertEquals(8, fuzzy.xpEarned)

        val empty = useCase.evaluate("asynchronous", "", nowMillis = 7000L)
        assertEquals(0, empty.accuracyPercent)
        assertEquals(2, empty.xpEarned)
    }

    @Test
    fun `FT-7_4 widget snapshot selects Word of the Day with RU-TJ fallback, due count, and streak`() = runTest {
        val zone = ZoneOffset.UTC
        val now = LocalDate.of(2026, 11, 19).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val academicRepo = FakeStage2AcademicRepo(
            listOf(
                sampleTerm(1, "ciphertext", "шифротекст", "матни рамзшуда", "/ˈsaɪ.fər.tekst/", isInSrs = true)
            )
        )
        val vocabRepo = FakeVocabularyRepository(
            listOf(Word(1, 1, "Core", term = "mutex", transcription = "/m/", translation = "мьютекс", example = "", explanationRu = "", nextReviewAt = 0L))
        )
        val logRepo = FakeStudyLogRepository(
            listOf(StudyLog(1, actionType = StudyActionType.FLASHCARD, isCorrect = true, xpEarned = 10, timestamp = now))
        )
        val settingsRepo = FakeSettingsRepository(UserSettings(translationLanguage = TranslationLanguage.TJ))
        val useCase = BuildWidgetSnapshotUseCase(academicRepo, vocabRepo, logRepo, settingsRepo, CalculateStreakUseCase())

        val snapshotTj = useCase(nowMillis = now, zoneId = zone)
        assertEquals("ciphertext", snapshotTj.wordOfTheDayTerm)
        assertEquals("матни рамзшуда", snapshotTj.wordOfTheDayTranslation)
        assertEquals(2, snapshotTj.dueCardsCount)
        assertEquals(1, snapshotTj.streakDays)
        assertEquals(TranslationLanguage.TJ, snapshotTj.language)

        // Fallback when TJ translation is empty
        val fallbackAcademicRepo = FakeStage2AcademicRepo(listOf(sampleTerm(2, "deadlock", "взаимная блокировка", "")))
        val snapshotFallback = BuildWidgetSnapshotUseCase(fallbackAcademicRepo, vocabRepo, logRepo, settingsRepo, CalculateStreakUseCase())(now, zone)
        assertEquals("взаимная блокировка", snapshotFallback.wordOfTheDayTranslation)
    }

    @Test
    fun `FT-7_5 time attack generates questions, computes combo points, and saves personal best`() = runTest {
        val academicRepo = FakeStage2AcademicRepo(
            listOf(
                sampleTerm(1, "deadlock", "взаимная блокировка"),
                sampleTerm(2, "idempotency", "идемпотентность"),
                sampleTerm(3, "polymorphism", "полиморфизм"),
                sampleTerm(4, "serialization", "сериализация"),
                sampleTerm(5, "concurrency", "конкурентность")
            )
        )
        val vocabRepo = FakeVocabularyRepository()
        val taRepo = FakeTimeAttackRepo()
        val logRepo = FakeStudyLogRepository()
        val useCase = TimeAttackQuizUseCase(academicRepo, vocabRepo, taRepo, logRepo)

        val termQs = useCase.generateQuestions(TimeAttackMode.TERM_TRANSLATION, TranslationLanguage.RU, count = 4)
        assertEquals(4, termQs.size)
        assertTrue(termQs.all { it.correctOption in it.options && it.options.size == 4 })

        val prQs = useCase.generateQuestions(TimeAttackMode.PR_ERROR_SPOTTER, count = 6)
        assertEquals(6, prQs.size)
        assertTrue(prQs.all { it.correctOption in it.options })

        assertEquals(10, useCase.calculatePointsForCorrectAnswer(0))
        assertEquals(16, useCase.calculatePointsForCorrectAnswer(3))
        assertEquals(30, useCase.calculatePointsForCorrectAnswer(15))

        val r1 = useCase.recordSessionResult(TimeAttackMode.TERM_TRANSLATION, score = 140, correctCount = 10, totalAnswered = 12, nowMillis = 1000L)
        val r2 = useCase.recordSessionResult(TimeAttackMode.TERM_TRANSLATION, score = 210, correctCount = 15, totalAnswered = 16, nowMillis = 2000L)
        assertEquals(210, useCase.getPersonalBest(TimeAttackMode.TERM_TRANSLATION))
        assertEquals(2, useCase.observeLeaderboard().first().size)
        assertEquals(r2.score, useCase.observeLeaderboard().first().first().score)
        assertTrue(r1.xpEarned > 0)
        assertEquals(2, logRepo.currentLogs.size)
    }

    @Test
    fun `FT-7_6 deck export and import supports JSON, CSV, Anki TSV, and Anki apkg notes`() = runTest {
        val vocabRepo = FakeVocabularyRepository(
            listOf(
                Word(1, 11, "Мои слова", term = "mutex", transcription = "/ˈmjuː.teks/", translation = "мьютекс, взаимное исключение", example = "Lock the \"mutex\"", explanationRu = "Синхронизация", isCustom = true, repetitions = 2, easeFactor = 2.6f, intervalDays = 6)
            )
        )
        val useCase = DeckImportExportUseCase(vocabRepo)

        // JSON export & round-trip parse
        val json = useCase.exportCustomAndLearnedWordsToJson()
        val parsedJson = useCase.parseJsonDeck(json)
        assertEquals(1, parsedJson.size)
        assertEquals("mutex", parsedJson.first().term)
        assertEquals("Lock the \"mutex\"", parsedJson.first().example)

        // CSV export & RFC 4180 quoted-comma round-trip parse
        val csv = useCase.exportCustomAndLearnedWordsToCsv()
        val parsedCsv = useCase.parseCsvDeck(csv)
        assertEquals(1, parsedCsv.size)
        assertEquals("мьютекс, взаимное исключение", parsedCsv.first().translation)

        // Anki TSV parse (strips HTML and # metadata)
        val tsv = "#separator:tab\n<b>backoff</b>\t<div>отсрочка повтора</div>\tsre networking\n"
        val parsedTsv = useCase.parseAnkiTsvDeck(tsv)
        assertEquals(1, parsedTsv.size)
        assertEquals("backoff", parsedTsv.first().term)
        assertEquals("отсрочка повтора", parsedTsv.first().translation)

        // Anki .apkg notes (0x1F unit separator, ignores Anki cards scheduling)
        val apkgNotes = listOf(
            "<i>sharding</i>\u001Fшардирование\u001FHorizontal partition" to "db distributed",
            "mutex\u001Fдубликат" to "dup"
        )
        val parsedApkg = useCase.parseAnkiNoteRows(apkgNotes)
        assertEquals(2, parsedApkg.size)

        val summary = useCase.importCards(DeckFormat.ANKI_APKG, parsedApkg)
        assertEquals(2, summary.totalParsed)
        assertEquals(1, summary.importedCount) // sharding added, mutex skipped as duplicate
        assertEquals(1, summary.skippedDuplicates)

        val imported = vocabRepo.findByExactTerm("sharding")
        assertNotNull(imported)
        assertTrue(imported!!.isCustom)
        assertEquals(0, imported.repetitions) // fresh TechEnglish SM-2 state
        assertEquals(2.5f, imported.easeFactor, 0.001f)
    }

    @Test
    fun `Errata E6 and KD-4 cover RU-TJ fallback and all 12 scenario contexts`() = runTest {
        assertEquals(12, ScenarioType.entries.size)
        assertEquals(ChameleonThemeType.SLACK, ScenarioChameleonRegistry.resolveForScenario(ScenarioType.CLIENT_COMMUNICATION))
        assertEquals(ChameleonThemeType.TERMINAL, ScenarioChameleonRegistry.resolveForScenario(ScenarioType.INCIDENT_POST_MORTEM))
        assertEquals(TranslationLanguage.TJ, TranslationLanguage.fromRaw("TJ"))
        assertEquals(TranslationLanguage.RU, TranslationLanguage.fromRaw("UNKNOWN"))

        val withTj = sampleTerm(1, "algorithm", "алгоритм", "алгоритм (тоҷ.)")
        val withoutTj = sampleTerm(2, "shard", "шард", "")
        assertEquals("алгоритм (тоҷ.)", withTj.displayTranslation(TranslationLanguage.TJ))
        assertEquals("алгоритм", withTj.displayTranslation(TranslationLanguage.RU))
        assertEquals("шард", withoutTj.displayTranslation(TranslationLanguage.TJ))
        assertTrue(withTj.hasTajikTranslation)
        assertFalse(withoutTj.hasTajikTranslation)

        val settingsRepo = FakeSettingsRepository()
        val updateSettings = UpdateSettingsUseCase(settingsRepo)
        updateSettings.setTranslationLanguage(TranslationLanguage.TJ)
        assertEquals(TranslationLanguage.TJ, settingsRepo.observeSettings().first().translationLanguage)
    }
}
