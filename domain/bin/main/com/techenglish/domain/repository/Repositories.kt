package com.techenglish.domain.repository

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
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TimeAttackMode
import com.techenglish.domain.model.TimeAttackRecord
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.model.Word
import com.techenglish.domain.model.WorkPhrase
import com.techenglish.domain.model.WorkScenario
import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeCategoryProgress(): Flow<List<CategoryProgress>>
    fun observeAllWords(): Flow<List<Word>>
    fun observeDueWords(nowMillis: Long, limit: Int): Flow<List<Word>>
    fun observeDueCount(nowMillis: Long): Flow<Int>
    fun observeTotalWordCount(): Flow<Int>
    fun observeLearnedWordCount(): Flow<Int>
    fun searchWords(query: String): Flow<List<Word>>

    suspend fun getDueWords(nowMillis: Long, limit: Int, categoryId: Int? = null): List<Word>
    suspend fun getDueCount(nowMillis: Long): Int
    suspend fun getWordById(wordId: Int): Word?
    suspend fun findByExactTerm(term: String): Word?
    suspend fun findByTermPrefix(prefix: String): Word?
    suspend fun getRandomWords(limit: Int): List<Word>
    suspend fun getSystemCategoryId(): Int

    suspend fun insertWord(word: Word): Long
    suspend fun updateWord(word: Word)
    suspend fun updateSrsState(
        wordId: Int,
        repetitions: Int,
        easeFactor: Float,
        intervalDays: Int,
        nextReviewAt: Long
    )
    suspend fun setFavorite(wordId: Int, isFavorite: Boolean)
    suspend fun deleteCustomWord(wordId: Int): Boolean
}

interface ProgrammingLanguageRepository {
    fun observeAllLanguages(): Flow<List<ProgrammingLanguage>>
    fun observeLanguageById(id: Int): Flow<ProgrammingLanguage?>
    fun observeSnippetsByLanguage(languageId: Int): Flow<List<CodeSnippet>>

    suspend fun getAllLanguages(): List<ProgrammingLanguage>
    suspend fun getLanguageById(id: Int): ProgrammingLanguage?
    suspend fun getSnippetById(snippetId: Int): CodeSnippet?
    suspend fun getSnippetsByLanguage(languageId: Int): List<CodeSnippet>
    suspend fun getSnippetByLanguageAndTask(languageId: Int, taskKey: String): CodeSnippet?
    suspend fun getCommonTaskKeys(langAId: Int, langBId: Int): List<String>
}

interface DocumentationRepository {
    fun observeAllArticles(): Flow<List<DocArticle>>
    fun observeArticleById(articleId: Int): Flow<DocArticle?>
    fun observeCompletedCount(): Flow<Int>
    suspend fun getArticleById(articleId: Int): DocArticle?
    suspend fun updateReadingProgress(articleId: Int, progress: Float, isCompleted: Boolean)
}

interface ScenarioRepository {
    fun observeAllScenarios(): Flow<List<WorkScenario>>
    fun observeAllPhrases(): Flow<List<WorkPhrase>>
    suspend fun getAllScenarios(): List<WorkScenario>
    suspend fun getScenarioById(id: Int): WorkScenario?
    suspend fun getAllPhrases(): List<WorkPhrase>
}

interface StudyLogRepository {
    suspend fun insertLog(log: StudyLog): Long
    fun observeAllLogs(): Flow<List<StudyLog>>
    fun observeRecentLogs(limit: Int): Flow<List<StudyLog>>
    fun observeCompletedScenarioIds(): Flow<Set<Int>>
    fun observeTotalXp(): Flow<Int>
    fun observeTodayReviewedCards(startOfDayMillis: Long): Flow<Int>
    fun observeTodayActivityCount(startOfDayMillis: Long): Flow<Int>
}

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun setUserName(name: String)
    suspend fun setDailyGoal(goal: Int)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColors(enabled: Boolean)
    suspend fun setReadingFontSize(sizeSp: Float)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setReminderTime(hour: Int, minute: Int)
    suspend fun setHapticFeedbackEnabled(enabled: Boolean)
    suspend fun setReduceMotionEnabled(enabled: Boolean)
    suspend fun setTranslationLanguage(language: TranslationLanguage)
    suspend fun setAgeVerified16Plus(verified: Boolean) = Unit
    suspend fun setAllowFriendPings(allowed: Boolean) = Unit
    suspend fun setAiConsentAccepted(accepted: Boolean) = Unit
    suspend fun setInitialSetupCompleted(completed: Boolean) = Unit
    suspend fun setLocalUsername(username: String) = Unit
    suspend fun setDeveloperRole(role: String) = Unit
    suspend fun setPendingProfileSync(pending: Boolean) = Unit
    suspend fun completeInitialSetup(
        localUsername: String,
        displayName: String,
        developerRole: String,
        dailyGoal: Int,
        translationLanguage: TranslationLanguage,
        themeMode: ThemeMode,
        pendingProfileSync: Boolean
    ) = Unit
}

interface CustomDocRepository {
    fun observeAllCustomDocs(): Flow<List<CustomImportedDoc>>
    suspend fun getCustomDocById(id: Int): CustomImportedDoc?
    suspend fun saveCustomDoc(doc: CustomImportedDoc): Long
    suspend fun deleteCustomDoc(id: Int)
}

interface TimeAttackRepository {
    fun observeTopRecords(limit: Int = 10): Flow<List<TimeAttackRecord>>
    suspend fun getPersonalBestScore(mode: TimeAttackMode): Int
    suspend fun saveRecord(record: TimeAttackRecord): Long
}

interface AcademicVocabularyRepository {
    fun observeVerifiedCount(): Flow<Int>
    fun observeStudiedCount(): Flow<Int>
    fun observeFavoritesCount(): Flow<Int>
    fun observeDifficultCount(): Flow<Int>
    fun observeDueSrsCount(nowMillis: Long): Flow<Int>
    fun observeMajorCategoryCounts(): Flow<List<AcademicMajorCategorySummary>>
    fun observeDomainCounts(): Flow<List<AcademicDomainSummary>>
    fun observeRecentlyStudied(limit: Int = 10): Flow<List<AcademicTerm>>

    suspend fun getVerifiedCount(): Int
    suspend fun getDueSrsCount(nowMillis: Long): Int
    suspend fun searchPaginated(
        filter: AcademicSearchFilter,
        nowMillis: Long,
        limit: Int,
        offset: Int
    ): List<AcademicTerm>
    suspend fun countMatching(filter: AcademicSearchFilter, nowMillis: Long): Int
    suspend fun getTermDetail(termId: Int): AcademicTermDetail?
    suspend fun findByNormalizedTerm(normalizedTerm: String): AcademicTerm?
    suspend fun findByNormalizedTerms(normalizedTerms: List<String>): List<AcademicTerm> =
        normalizedTerms.mapNotNull { findByNormalizedTerm(it) }
    suspend fun getRandomVerifiedTerms(limit: Int): List<AcademicTerm> = emptyList()
    suspend fun getSrsDeckForMode(
        mode: AcademicSrsMode,
        nowMillis: Long,
        limit: Int
    ): List<AcademicTerm>

    suspend fun setFavorite(termId: Int, isFavorite: Boolean)
    suspend fun setInSrs(termId: Int, isInSrs: Boolean)
    suspend fun updateSrsProgress(
        termId: Int,
        repetitions: Int,
        easeFactor: Float,
        intervalDays: Int,
        nextReviewAt: Long,
        nowMillis: Long
    )
}
