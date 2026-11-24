package com.techenglish.domain

import com.techenglish.domain.model.Category
import com.techenglish.domain.model.CategoryProgress
import com.techenglish.domain.model.CodeSnippet
import com.techenglish.domain.model.DocArticle
import com.techenglish.domain.model.ProgrammingLanguage
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.model.Word
import com.techenglish.domain.model.WorkPhrase
import com.techenglish.domain.model.WorkScenario
import com.techenglish.domain.repository.DocumentationRepository
import com.techenglish.domain.repository.ProgrammingLanguageRepository
import com.techenglish.domain.repository.ScenarioRepository
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeVocabularyRepository(
    initialWords: List<Word> = emptyList(),
    initialCategories: List<Category> = listOf(
        Category(1, "Backend", "dns", false),
        Category(11, "Мои слова", "bookmark_added", true)
    )
) : VocabularyRepository {

    private val categoriesFlow = MutableStateFlow(initialCategories)
    private val wordsFlow = MutableStateFlow(initialWords)
    private var nextId = (initialWords.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeCategories(): Flow<List<Category>> = categoriesFlow

    override fun observeCategoryProgress(): Flow<List<CategoryProgress>> =
        wordsFlow.map { words ->
            categoriesFlow.value.map { cat ->
                val catWords = words.filter { it.categoryId == cat.id }
                CategoryProgress(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    categoryIcon = cat.icon,
                    totalWords = catWords.size,
                    learnedWords = catWords.count { it.repetitions >= 1 },
                    masteredWords = catWords.count { it.repetitions >= 3 }
                )
            }
        }

    override fun observeAllWords(): Flow<List<Word>> = wordsFlow

    override fun observeDueWords(nowMillis: Long, limit: Int): Flow<List<Word>> =
        wordsFlow.map { list -> list.filter { it.nextReviewAt <= nowMillis }.take(limit) }

    override fun observeDueCount(nowMillis: Long): Flow<Int> =
        wordsFlow.map { list -> list.count { it.nextReviewAt <= nowMillis } }

    override fun observeTotalWordCount(): Flow<Int> = wordsFlow.map { it.size }

    override fun observeLearnedWordCount(): Flow<Int> =
        wordsFlow.map { list -> list.count { it.repetitions >= 1 } }

    override fun searchWords(query: String): Flow<List<Word>> =
        wordsFlow.map { list ->
            val q = query.lowercase()
            list.filter {
                it.term.lowercase().contains(q) ||
                    it.translation.lowercase().contains(q) ||
                    it.explanationRu.lowercase().contains(q)
            }
        }

    override suspend fun getDueWords(nowMillis: Long, limit: Int, categoryId: Int?): List<Word> =
        wordsFlow.value
            .filter { it.nextReviewAt <= nowMillis && (categoryId == null || it.categoryId == categoryId) }
            .take(limit)

    override suspend fun getDueCount(nowMillis: Long): Int =
        wordsFlow.value.count { it.nextReviewAt <= nowMillis }

    override suspend fun getWordById(wordId: Int): Word? =
        wordsFlow.value.firstOrNull { it.id == wordId }

    override suspend fun findByExactTerm(term: String): Word? =
        wordsFlow.value.firstOrNull { it.term.equals(term, ignoreCase = true) }

    override suspend fun findByTermPrefix(prefix: String): Word? =
        wordsFlow.value.firstOrNull { it.term.lowercase().startsWith(prefix.lowercase()) }

    override suspend fun getRandomWords(limit: Int): List<Word> =
        wordsFlow.value.take(limit)

    override suspend fun getSystemCategoryId(): Int =
        categoriesFlow.value.firstOrNull { it.isSystem }?.id ?: 11

    override suspend fun insertWord(word: Word): Long {
        val id = if (word.id > 0) word.id else nextId++
        val saved = word.copy(id = id)
        wordsFlow.value = wordsFlow.value.filterNot { it.id == id } + saved
        return id.toLong()
    }

    override suspend fun updateWord(word: Word) {
        wordsFlow.value = wordsFlow.value.map { if (it.id == word.id) word else it }
    }

    override suspend fun updateSrsState(
        wordId: Int,
        repetitions: Int,
        easeFactor: Float,
        intervalDays: Int,
        nextReviewAt: Long
    ) {
        wordsFlow.value = wordsFlow.value.map {
            if (it.id == wordId) {
                it.copy(
                    repetitions = repetitions,
                    easeFactor = easeFactor,
                    intervalDays = intervalDays,
                    nextReviewAt = nextReviewAt
                )
            } else it
        }
    }

    override suspend fun setFavorite(wordId: Int, isFavorite: Boolean) {
        wordsFlow.value = wordsFlow.value.map {
            if (it.id == wordId) it.copy(isFavorite = isFavorite) else it
        }
    }

    override suspend fun deleteCustomWord(wordId: Int): Boolean {
        val target = wordsFlow.value.firstOrNull { it.id == wordId && it.isCustom } ?: return false
        wordsFlow.value = wordsFlow.value - target
        return true
    }
}

class FakeProgrammingLanguageRepository(
    languages: List<ProgrammingLanguage>,
    snippets: List<CodeSnippet>
) : ProgrammingLanguageRepository {

    private val languagesFlow = MutableStateFlow(languages)
    private val snippetsFlow = MutableStateFlow(snippets)

    override fun observeAllLanguages(): Flow<List<ProgrammingLanguage>> = languagesFlow
    override fun observeLanguageById(id: Int): Flow<ProgrammingLanguage?> =
        languagesFlow.map { list -> list.firstOrNull { it.id == id } }
    override fun observeSnippetsByLanguage(languageId: Int): Flow<List<CodeSnippet>> =
        snippetsFlow.map { list -> list.filter { it.languageId == languageId } }

    override suspend fun getAllLanguages(): List<ProgrammingLanguage> = languagesFlow.value
    override suspend fun getLanguageById(id: Int): ProgrammingLanguage? =
        languagesFlow.value.firstOrNull { it.id == id }
    override suspend fun getSnippetById(snippetId: Int): CodeSnippet? =
        snippetsFlow.value.firstOrNull { it.id == snippetId }
    override suspend fun getSnippetsByLanguage(languageId: Int): List<CodeSnippet> =
        snippetsFlow.value.filter { it.languageId == languageId }
    override suspend fun getSnippetByLanguageAndTask(languageId: Int, taskKey: String): CodeSnippet? =
        snippetsFlow.value.firstOrNull { it.languageId == languageId && it.taskKey == taskKey }
    override suspend fun getCommonTaskKeys(langAId: Int, langBId: Int): List<String> {
        val keysB = snippetsFlow.value.filter { it.languageId == langBId }.map { it.taskKey }.toSet()
        return snippetsFlow.value.filter { it.languageId == langAId && it.taskKey in keysB }.map { it.taskKey }
    }
}

class FakeDocumentationRepository(articles: List<DocArticle>) : DocumentationRepository {
    private val articlesFlow = MutableStateFlow(articles)

    override fun observeAllArticles(): Flow<List<DocArticle>> = articlesFlow
    override fun observeArticleById(articleId: Int): Flow<DocArticle?> =
        articlesFlow.map { list -> list.firstOrNull { it.id == articleId } }
    override fun observeCompletedCount(): Flow<Int> =
        articlesFlow.map { list -> list.count { it.isCompleted } }
    override suspend fun getArticleById(articleId: Int): DocArticle? =
        articlesFlow.value.firstOrNull { it.id == articleId }
    override suspend fun updateReadingProgress(articleId: Int, progress: Float, isCompleted: Boolean) {
        articlesFlow.value = articlesFlow.value.map {
            if (it.id == articleId) it.copy(readingProgress = progress, isCompleted = isCompleted) else it
        }
    }
}

class FakeScenarioRepository(
    scenarios: List<WorkScenario> = emptyList(),
    phrases: List<WorkPhrase> = emptyList()
) : ScenarioRepository {
    private val scenariosFlow = MutableStateFlow(scenarios)
    private val phrasesFlow = MutableStateFlow(phrases)

    override fun observeAllScenarios(): Flow<List<WorkScenario>> = scenariosFlow
    override fun observeAllPhrases(): Flow<List<WorkPhrase>> = phrasesFlow
    override suspend fun getAllScenarios(): List<WorkScenario> = scenariosFlow.value
    override suspend fun getScenarioById(id: Int): WorkScenario? =
        scenariosFlow.value.firstOrNull { it.id == id }
    override suspend fun getAllPhrases(): List<WorkPhrase> = phrasesFlow.value
}

class FakeStudyLogRepository(initialLogs: List<StudyLog> = emptyList()) : StudyLogRepository {
    private val logsFlow = MutableStateFlow(initialLogs)
    private var nextId = (initialLogs.maxOfOrNull { it.id } ?: 0) + 1

    val currentLogs: List<StudyLog> get() = logsFlow.value

    override suspend fun insertLog(log: StudyLog): Long {
        val id = nextId++
        logsFlow.value = listOf(log.copy(id = id)) + logsFlow.value
        return id.toLong()
    }

    override fun observeAllLogs(): Flow<List<StudyLog>> = logsFlow
    override fun observeRecentLogs(limit: Int): Flow<List<StudyLog>> =
        logsFlow.map { it.take(limit) }
    override fun observeCompletedScenarioIds(): Flow<Set<Int>> =
        logsFlow.map { list -> list.filter { it.scenarioId != null && it.isCorrect }.mapNotNull { it.scenarioId }.toSet() }
    override fun observeTotalXp(): Flow<Int> =
        logsFlow.map { list -> list.sumOf { it.xpEarned } }
    override fun observeTodayReviewedCards(startOfDayMillis: Long): Flow<Int> =
        logsFlow.map { list -> list.count { it.actionType == StudyActionType.FLASHCARD && it.timestamp >= startOfDayMillis } }
    override fun observeTodayActivityCount(startOfDayMillis: Long): Flow<Int> =
        logsFlow.map { list -> list.count { it.timestamp >= startOfDayMillis } }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override fun observeSettings(): Flow<UserSettings> = state
    override suspend fun setUserName(name: String) { state.value = state.value.copy(userName = name) }
    override suspend fun setDailyGoal(goal: Int) { state.value = state.value.copy(dailyGoal = goal) }
    override suspend fun setThemeMode(mode: ThemeMode) { state.value = state.value.copy(themeMode = mode) }
    override suspend fun setDynamicColors(enabled: Boolean) { state.value = state.value.copy(dynamicColors = enabled) }
    override suspend fun setReadingFontSize(sizeSp: Float) { state.value = state.value.copy(readingFontSizeSp = sizeSp) }
    override suspend fun setNotificationsEnabled(enabled: Boolean) { state.value = state.value.copy(notificationsEnabled = enabled) }
    override suspend fun setReminderTime(hour: Int, minute: Int) {
        state.value = state.value.copy(reminderHour = hour, reminderMinute = minute)
    }
    override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        state.value = state.value.copy(hapticFeedbackEnabled = enabled)
    }
    override suspend fun setReduceMotionEnabled(enabled: Boolean) {
        state.value = state.value.copy(reduceMotionEnabled = enabled)
    }
    override suspend fun setTranslationLanguage(language: TranslationLanguage) {
        state.value = state.value.copy(translationLanguage = language)
    }
    override suspend fun setAgeVerified16Plus(verified: Boolean) {
        state.value = state.value.copy(ageVerified16Plus = verified)
    }
    override suspend fun setAllowFriendPings(allowed: Boolean) {
        state.value = state.value.copy(allowFriendPings = allowed)
    }
}
