package com.techenglish.app.data.repository

import com.techenglish.app.core.database.dao.AcademicTermDao
import com.techenglish.app.core.database.dao.CategoryDao
import com.techenglish.app.core.database.dao.CodeSnippetDao
import com.techenglish.app.core.database.dao.CustomImportedDocDao
import com.techenglish.app.core.database.dao.DocArticleDao
import com.techenglish.app.core.database.dao.ProgrammingLanguageDao
import com.techenglish.app.core.database.dao.ScenarioDao
import com.techenglish.app.core.database.dao.StudyLogDao
import com.techenglish.app.core.database.dao.SyncQueueDao
import com.techenglish.app.core.database.dao.TimeAttackRecordDao
import com.techenglish.app.core.database.entity.SyncQueueEntity
import com.techenglish.app.core.database.dao.WordDao
import com.techenglish.app.core.database.dao.WorkPhraseDao
import com.techenglish.app.core.datastore.UserPreferencesDataStore
import com.techenglish.app.data.mapper.toDomain
import com.techenglish.app.data.mapper.toEntity
import com.techenglish.domain.model.AcademicDomainSummary
import com.techenglish.domain.model.AcademicMajorCategorySummary
import com.techenglish.domain.model.AcademicSearchFilter
import com.techenglish.domain.model.AcademicSrsMode
import com.techenglish.domain.model.AcademicTerm
import com.techenglish.domain.model.AcademicTermDetail
import com.techenglish.domain.model.Category
import com.techenglish.domain.model.CategoryProgress
import com.techenglish.domain.model.CodeSnippet
import com.techenglish.domain.model.CustomImportedDoc
import com.techenglish.domain.model.DocArticle
import com.techenglish.domain.model.ProgrammingLanguage
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.SyncEventType
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TimeAttackMode
import com.techenglish.domain.model.TimeAttackRecord
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.model.Word
import com.techenglish.domain.model.WorkPhrase
import com.techenglish.domain.model.WorkScenario
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.CustomDocRepository
import com.techenglish.domain.repository.DocumentationRepository
import com.techenglish.domain.repository.ProgrammingLanguageRepository
import com.techenglish.domain.repository.ScenarioRepository
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.repository.TimeAttackRepository
import com.techenglish.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val wordDao: WordDao
) : VocabularyRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCategoryProgress(): Flow<List<CategoryProgress>> =
        categoryDao.observeCategoryProgress().map { rows -> rows.map { it.toDomain() } }

    override fun observeAllWords(): Flow<List<Word>> =
        wordDao.observeAllWithDetails().map { list -> list.map { it.toDomain() } }

    override fun observeDueWords(nowMillis: Long, limit: Int): Flow<List<Word>> =
        wordDao.observeDueWords(nowMillis, limit).map { list -> list.map { it.toDomain() } }

    override fun observeDueCount(nowMillis: Long): Flow<Int> =
        wordDao.observeDueCount(nowMillis)

    override fun observeTotalWordCount(): Flow<Int> =
        wordDao.observeTotalWordCount()

    override fun observeLearnedWordCount(): Flow<Int> =
        wordDao.observeLearnedWordCount()

    override fun searchWords(query: String): Flow<List<Word>> =
        wordDao.searchWords(query.trim().lowercase(java.util.Locale.ROOT)).map { list -> list.map { it.toDomain() } }

    override suspend fun getDueWords(nowMillis: Long, limit: Int, categoryId: Int?): List<Word> =
        if (categoryId == null) {
            wordDao.getDueWords(nowMillis, limit).map { it.toDomain() }
        } else {
            wordDao.getDueWordsByCategory(nowMillis, categoryId, limit).map { it.toDomain() }
        }

    override suspend fun getDueCount(nowMillis: Long): Int =
        wordDao.getDueCount(nowMillis)

    override suspend fun getWordById(wordId: Int): Word? =
        wordDao.getByIdWithDetails(wordId)?.toDomain()

    override suspend fun findByExactTerm(term: String): Word? =
        wordDao.findByExactTerm(term)?.toDomain()

    override suspend fun findByTermPrefix(prefix: String): Word? =
        wordDao.findByTermPrefix(prefix)?.toDomain()

    override suspend fun getRandomWords(limit: Int): List<Word> {
        val catNames = categoryDao.getAll().associate { it.id to it.name }
        return wordDao.getRandomWords(limit).map { entity ->
            entity.toDomain(categoryName = catNames[entity.categoryId].orEmpty())
        }
    }

    override suspend fun getSystemCategoryId(): Int =
        categoryDao.getSystemCategory()?.id ?: 11

    override suspend fun insertWord(word: Word): Long =
        wordDao.insert(word.toEntity())

    override suspend fun updateWord(word: Word) =
        wordDao.update(word.toEntity())

    override suspend fun updateSrsState(
        wordId: Int,
        repetitions: Int,
        easeFactor: Float,
        intervalDays: Int,
        nextReviewAt: Long
    ) = wordDao.updateSrsState(wordId, repetitions, easeFactor, intervalDays, nextReviewAt)

    override suspend fun setFavorite(wordId: Int, isFavorite: Boolean) =
        wordDao.updateFavorite(wordId, isFavorite)

    override suspend fun deleteCustomWord(wordId: Int): Boolean =
        wordDao.deleteCustomWord(wordId) > 0
}

@Singleton
class ProgrammingLanguageRepositoryImpl @Inject constructor(
    private val languageDao: ProgrammingLanguageDao,
    private val snippetDao: CodeSnippetDao
) : ProgrammingLanguageRepository {

    override fun observeAllLanguages(): Flow<List<ProgrammingLanguage>> =
        languageDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeLanguageById(id: Int): Flow<ProgrammingLanguage?> =
        languageDao.observeById(id).map { it?.toDomain() }

    override fun observeSnippetsByLanguage(languageId: Int): Flow<List<CodeSnippet>> =
        snippetDao.observeByLanguage(languageId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAllLanguages(): List<ProgrammingLanguage> =
        languageDao.getAll().map { it.toDomain() }

    override suspend fun getLanguageById(id: Int): ProgrammingLanguage? =
        languageDao.getById(id)?.toDomain()

    override suspend fun getSnippetById(snippetId: Int): CodeSnippet? =
        snippetDao.getById(snippetId)?.toDomain()

    override suspend fun getSnippetsByLanguage(languageId: Int): List<CodeSnippet> =
        snippetDao.getByLanguage(languageId).map { it.toDomain() }

    override suspend fun getSnippetByLanguageAndTask(languageId: Int, taskKey: String): CodeSnippet? =
        snippetDao.getByLanguageAndTask(languageId, taskKey)?.toDomain()

    override suspend fun getCommonTaskKeys(langAId: Int, langBId: Int): List<String> =
        snippetDao.getCommonTaskKeys(langAId, langBId)
}

@Singleton
class DocumentationRepositoryImpl @Inject constructor(
    private val docArticleDao: DocArticleDao
) : DocumentationRepository {

    override fun observeAllArticles(): Flow<List<DocArticle>> =
        docArticleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeArticleById(articleId: Int): Flow<DocArticle?> =
        docArticleDao.observeById(articleId).map { it?.toDomain() }

    override fun observeCompletedCount(): Flow<Int> =
        docArticleDao.observeCompletedCount()

    override suspend fun getArticleById(articleId: Int): DocArticle? =
        docArticleDao.getById(articleId)?.toDomain()

    override suspend fun updateReadingProgress(articleId: Int, progress: Float, isCompleted: Boolean) =
        docArticleDao.updateReadingProgress(articleId, progress, isCompleted)
}

@Singleton
class ScenarioRepositoryImpl @Inject constructor(
    private val scenarioDao: ScenarioDao,
    private val phraseDao: WorkPhraseDao
) : ScenarioRepository {

    override fun observeAllScenarios(): Flow<List<WorkScenario>> =
        scenarioDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeAllPhrases(): Flow<List<WorkPhrase>> =
        phraseDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllScenarios(): List<WorkScenario> =
        scenarioDao.getAll().map { it.toDomain() }

    override suspend fun getScenarioById(id: Int): WorkScenario? =
        scenarioDao.getById(id)?.toDomain()

    override suspend fun getAllPhrases(): List<WorkPhrase> =
        phraseDao.getAll().map { it.toDomain() }
}

@Singleton
class StudyLogRepositoryImpl @Inject constructor(
    private val studyLogDao: StudyLogDao,
    private val syncQueueDao: SyncQueueDao
) : StudyLogRepository {

    override suspend fun insertLog(log: StudyLog): Long {
        val insertedId = studyLogDao.insert(log.toEntity())
        val eventType = when (log.actionType) {
            StudyActionType.FLASHCARD -> if (log.isCorrect) SyncEventType.FLASHCARD_CORRECT else SyncEventType.FLASHCARD_RETRY
            StudyActionType.SCENARIO -> if (log.isCorrect) SyncEventType.SCENARIO_CORRECT else SyncEventType.SCENARIO_ATTEMPT
            StudyActionType.DOC_READ -> SyncEventType.DOC_COMPLETED
            StudyActionType.LISTENING -> SyncEventType.LISTENING_CORRECT
            StudyActionType.CODE_BREAKDOWN -> SyncEventType.CODE_BREAKDOWN
        }
        val payload = """{"logId":$insertedId,"correct":${log.isCorrect},"xp":${log.xpEarned}}"""
        syncQueueDao.enqueue(
            SyncQueueEntity(
                eventId = java.util.UUID.randomUUID().toString(),
                eventType = eventType.name,
                payloadJson = payload,
                createdAt = log.timestamp,
                syncStatus = "PENDING"
            )
        )
        return insertedId
    }

    override fun observeAllLogs(): Flow<List<StudyLog>> =
        studyLogDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeRecentLogs(limit: Int): Flow<List<StudyLog>> =
        studyLogDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeCompletedScenarioIds(): Flow<Set<Int>> =
        studyLogDao.observeCompletedScenarioIds().map { it.toSet() }

    override fun observeTotalXp(): Flow<Int> =
        studyLogDao.observeTotalXp()

    override fun observeTodayReviewedCards(startOfDayMillis: Long): Flow<Int> =
        studyLogDao.observeTodayReviewedCards(startOfDayMillis)

    override fun observeTodayActivityCount(startOfDayMillis: Long): Flow<Int> =
        studyLogDao.observeTodayActivityCount(startOfDayMillis)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> =
        dataStore.preferencesFlow.map { it.toDomain() }

    override suspend fun setUserName(name: String) = dataStore.setUserName(name)
    override suspend fun setDailyGoal(goal: Int) = dataStore.setDailyGoal(goal)
    override suspend fun setThemeMode(mode: ThemeMode) = dataStore.setThemeMode(mode.name)
    override suspend fun setDynamicColors(enabled: Boolean) = dataStore.setDynamicColors(enabled)
    override suspend fun setReadingFontSize(sizeSp: Float) = dataStore.setReadingFontSize(sizeSp)
    override suspend fun setNotificationsEnabled(enabled: Boolean) = dataStore.setNotificationsEnabled(enabled)
    override suspend fun setReminderTime(hour: Int, minute: Int) = dataStore.setReminderTime(hour, minute)
    override suspend fun setHapticFeedbackEnabled(enabled: Boolean) = dataStore.setHapticFeedbackEnabled(enabled)
    override suspend fun setReduceMotionEnabled(enabled: Boolean) = dataStore.setReduceMotionEnabled(enabled)
    override suspend fun setTranslationLanguage(language: TranslationLanguage) =
        dataStore.setTranslationLanguage(language.name)
    override suspend fun setAgeVerified16Plus(verified: Boolean) =
        dataStore.setAgeVerified16Plus(verified)
    override suspend fun setAllowFriendPings(allowed: Boolean) =
        dataStore.setAllowFriendPings(allowed)
    override suspend fun setAiConsentAccepted(accepted: Boolean) =
        dataStore.setAiConsentAccepted(accepted)
    override suspend fun setInitialSetupCompleted(completed: Boolean) =
        dataStore.setInitialSetupCompleted(completed)
    override suspend fun setLocalUsername(username: String) =
        dataStore.setLocalUsername(username)
    override suspend fun setDeveloperRole(role: String) =
        dataStore.setDeveloperRole(role)
    override suspend fun setPendingProfileSync(pending: Boolean) =
        dataStore.setPendingProfileSync(pending)
    override suspend fun completeInitialSetup(
        localUsername: String,
        displayName: String,
        developerRole: String,
        dailyGoal: Int,
        translationLanguage: TranslationLanguage,
        themeMode: ThemeMode,
        pendingProfileSync: Boolean
    ) = dataStore.completeInitialSetup(
        localUsername = localUsername,
        displayName = displayName,
        developerRole = developerRole,
        dailyGoal = dailyGoal,
        translationLanguage = translationLanguage.name,
        themeMode = themeMode.name,
        pendingProfileSync = pendingProfileSync
    )
}

@Singleton
class AcademicVocabularyRepositoryImpl @Inject constructor(
    private val academicTermDao: AcademicTermDao
) : AcademicVocabularyRepository {

    override fun observeVerifiedCount(): Flow<Int> =
        academicTermDao.observeVerifiedCount()

    override fun observeStudiedCount(): Flow<Int> =
        academicTermDao.observeStudiedCount()

    override fun observeFavoritesCount(): Flow<Int> =
        academicTermDao.observeFavoritesCount()

    override fun observeDifficultCount(): Flow<Int> =
        academicTermDao.observeDifficultCount()

    override fun observeDueSrsCount(nowMillis: Long): Flow<Int> =
        academicTermDao.observeDueSrsCount(nowMillis)

    override fun observeMajorCategoryCounts(): Flow<List<AcademicMajorCategorySummary>> =
        academicTermDao.observeMajorCategoryCounts().map { rows -> rows.map { it.toDomain() } }

    override fun observeDomainCounts(): Flow<List<AcademicDomainSummary>> =
        academicTermDao.observeDomainCounts().map { rows -> rows.map { it.toDomain() } }

    override fun observeRecentlyStudied(limit: Int): Flow<List<AcademicTerm>> =
        academicTermDao.observeRecentlyStudied(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getVerifiedCount(): Int =
        academicTermDao.getVerifiedCount()

    override suspend fun getDueSrsCount(nowMillis: Long): Int =
        academicTermDao.getDueSrsCount(nowMillis)

    override suspend fun searchPaginated(
        filter: AcademicSearchFilter,
        nowMillis: Long,
        limit: Int,
        offset: Int
    ): List<AcademicTerm> = academicTermDao.searchPaginated(
        normalizedQuery = filter.query,
        majorCategory = filter.majorCategory,
        domain = filter.domain,
        subdomain = filter.subdomain,
        difficulty = filter.difficulty,
        cefr = filter.cefr,
        partOfSpeech = filter.partOfSpeech,
        onlyFavorites = if (filter.onlyFavorites) 1 else 0,
        onlyDue = if (filter.onlyDue) 1 else 0,
        onlyDifficult = if (filter.onlyDifficult) 1 else 0,
        studyFilter = filter.studyFilter.code,
        nowMillis = nowMillis,
        limit = limit,
        offset = offset
    ).map { it.toDomain() }

    override suspend fun countMatching(filter: AcademicSearchFilter, nowMillis: Long): Int =
        academicTermDao.countMatching(
            normalizedQuery = filter.query,
            majorCategory = filter.majorCategory,
            domain = filter.domain,
            subdomain = filter.subdomain,
            difficulty = filter.difficulty,
            cefr = filter.cefr,
            partOfSpeech = filter.partOfSpeech,
            onlyFavorites = if (filter.onlyFavorites) 1 else 0,
            onlyDue = if (filter.onlyDue) 1 else 0,
            onlyDifficult = if (filter.onlyDifficult) 1 else 0,
            studyFilter = filter.studyFilter.code,
            nowMillis = nowMillis
        )

    override suspend fun getTermDetail(termId: Int): AcademicTermDetail? {
        val term = academicTermDao.getById(termId)?.toDomain() ?: return null
        val senses = academicTermDao.getSensesForTerm(termId).map { it.toDomain() }
        val relations = academicTermDao.getRelationsForTerm(termId).map { it.toDomain() }
        val source = term.sourceId?.let { academicTermDao.getSourceById(it)?.toDomain() }
        return AcademicTermDetail(
            term = term,
            senses = senses,
            relations = relations,
            source = source
        )
    }

    override suspend fun findByNormalizedTerm(normalizedTerm: String): AcademicTerm? =
        academicTermDao.findByNormalizedTerm(normalizedTerm)?.toDomain()

    override suspend fun findByNormalizedTerms(normalizedTerms: List<String>): List<AcademicTerm> {
        if (normalizedTerms.isEmpty()) return emptyList()
        return normalizedTerms
            .distinct()
            .chunked(500)
            .flatMap { chunk -> academicTermDao.findByNormalizedTerms(chunk) }
            .map { it.toDomain() }
    }

    override suspend fun getRandomVerifiedTerms(limit: Int): List<AcademicTerm> =
        academicTermDao.getRandomVerifiedTerms(limit).map { it.toDomain() }

    override suspend fun getSrsDeckForMode(
        mode: AcademicSrsMode,
        nowMillis: Long,
        limit: Int
    ): List<AcademicTerm> = academicTermDao.getSrsDeckForMode(
        majorCategory = mode.majorCategoryFilter,
        onlyCustomQueue = if (mode.onlyCustomQueue) 1 else 0,
        nowMillis = nowMillis,
        limit = limit
    ).map { it.toDomain() }

    override suspend fun setFavorite(termId: Int, isFavorite: Boolean) =
        academicTermDao.updateFavorite(termId, isFavorite)

    override suspend fun setInSrs(termId: Int, isInSrs: Boolean) =
        academicTermDao.updateInSrs(termId, isInSrs)

    override suspend fun updateSrsProgress(
        termId: Int,
        repetitions: Int,
        easeFactor: Float,
        intervalDays: Int,
        nextReviewAt: Long,
        nowMillis: Long
    ) = academicTermDao.updateSrsProgress(
        termId = termId,
        repetitions = repetitions,
        easeFactor = easeFactor,
        intervalDays = intervalDays,
        nextReviewAt = nextReviewAt,
        nowMillis = nowMillis
    )
}

@Singleton
class CustomDocRepositoryImpl @Inject constructor(
    private val customImportedDocDao: CustomImportedDocDao
) : CustomDocRepository {

    override fun observeAllCustomDocs(): Flow<List<CustomImportedDoc>> =
        customImportedDocDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getCustomDocById(id: Int): CustomImportedDoc? =
        customImportedDocDao.getById(id)?.toDomain()

    override suspend fun saveCustomDoc(doc: CustomImportedDoc): Long =
        customImportedDocDao.insert(doc.toEntity())

    override suspend fun deleteCustomDoc(id: Int) =
        customImportedDocDao.deleteById(id)
}

@Singleton
class TimeAttackRepositoryImpl @Inject constructor(
    private val timeAttackRecordDao: TimeAttackRecordDao
) : TimeAttackRepository {

    override fun observeTopRecords(limit: Int): Flow<List<TimeAttackRecord>> =
        timeAttackRecordDao.observeTopRecords(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getPersonalBestScore(mode: TimeAttackMode): Int =
        timeAttackRecordDao.getBestScoreForMode(mode.name)

    override suspend fun saveRecord(record: TimeAttackRecord): Long =
        timeAttackRecordDao.insert(record.toEntity())
}
