package com.techenglish.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.designsystem.DEFAULT_ACADEMIC_TERMS_COUNT
import com.techenglish.domain.model.CategoryProgress
import com.techenglish.domain.model.DailyProgress
import com.techenglish.domain.model.DayActivity
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.Word
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.DocumentationRepository
import com.techenglish.domain.repository.ProgrammingLanguageRepository
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.repository.VocabularyRepository
import com.techenglish.domain.usecase.GetDailyProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class Recommendation(
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val destination: RecommendationTarget
)

enum class RecommendationTarget {
    FLASHCARDS, DOCUMENTATION, SCENARIOS, LISTENING, COMPARE_LANGUAGES
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Content(
        val greetingRu: String,
        val userName: String,
        val dailyProgress: DailyProgress,
        val recentActivity: List<StudyLog>,
        val languagesCount: Int,
        val completedDocsCount: Int,
        val completedScenariosCount: Int,
        val recommendation: Recommendation,
        val learnedWordsCount: Int = 0,
        val totalWordsCount: Int = 0,
        val accuracyPercent: Int = 0,
        val weeklyLearnedDelta: Int = 0,
        val bestStreakDays: Int = 0,
        val wordOfTheDay: Word? = null,
        val categoryPacks: List<CategoryProgress> = emptyList(),
        val academicTermCount: Int = DEFAULT_ACADEMIC_TERMS_COUNT
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    getDailyProgress: GetDailyProgressUseCase,
    settingsRepository: SettingsRepository,
    studyLogRepository: StudyLogRepository,
    vocabularyRepository: VocabularyRepository,
    academicVocabularyRepository: AcademicVocabularyRepository,
    languageRepository: ProgrammingLanguageRepository,
    documentationRepository: DocumentationRepository,
    val audioController: TechAudioController
) : ViewModel() {

    private val coreFlow = combine(
        getDailyProgress(),
        settingsRepository.observeSettings(),
        studyLogRepository.observeAllLogs()
    ) { daily, settings, allLogs -> Triple(daily, settings, allLogs) }

    private val vocabFlow = combine(
        vocabularyRepository.observeAllWords(),
        vocabularyRepository.observeCategoryProgress(),
        academicVocabularyRepository.observeVerifiedCount()
    ) { words, packs, academicCount -> Triple(words, packs, academicCount) }

    private val modulesFlow = combine(
        languageRepository.observeAllLanguages(),
        documentationRepository.observeCompletedCount(),
        studyLogRepository.observeCompletedScenarioIds()
    ) { languages, completedDocs, completedScenarios ->
        Triple(languages.size, completedDocs, completedScenarios.size)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        coreFlow,
        vocabFlow,
        modulesFlow
    ) { (daily, settings, allLogs), (words, packs, academicCount), (languagesCount, completedDocs, completedScenarios) ->
        val recommendation = computeRecommendation(
            daily = daily,
            completedDocs = completedDocs,
            completedScenarios = completedScenarios
        )
        val totalLogs = allLogs.size
        val accuracy = if (totalLogs == 0) 0 else ((allLogs.count { it.isCorrect } * 100f) / totalLogs).toInt()
        val weekCutoff = System.currentTimeMillis() - 7L * 24L * 3600L * 1000L
        val weeklyDelta = allLogs.filter { it.timestamp >= weekCutoff && it.isCorrect && it.wordId != null }
            .mapNotNull { it.wordId }
            .distinct()
            .size
        val bestStreak = computeBestStreak(daily.yearlyActivity, daily.streakDays)
        val learnedCount = words.count { it.isLearned }
        val wordOfDay = if (words.isNotEmpty()) {
            words[LocalDate.now().dayOfYear % words.size]
        } else null

        HomeUiState.Content(
            greetingRu = timeBasedGreeting(),
            userName = settings.userName,
            dailyProgress = daily,
            recentActivity = allLogs.sortedByDescending { it.timestamp }.take(6),
            languagesCount = languagesCount,
            completedDocsCount = completedDocs,
            completedScenariosCount = completedScenarios,
            recommendation = recommendation,
            learnedWordsCount = learnedCount,
            totalWordsCount = words.size,
            accuracyPercent = accuracy,
            weeklyLearnedDelta = weeklyDelta,
            bestStreakDays = bestStreak,
            wordOfTheDay = wordOfDay,
            categoryPacks = packs,
            academicTermCount = if (academicCount > 0) academicCount else DEFAULT_ACADEMIC_TERMS_COUNT
        ) as HomeUiState
    }
        .flowOn(Dispatchers.Default)
        .catch { emit(HomeUiState.Error(it.localizedMessage ?: "Не удалось загрузить данные")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeUiState.Loading
        )

    private fun computeBestStreak(yearly: List<DayActivity>, currentStreak: Int): Int {
        var best = currentStreak
        var run = 0
        for (day in yearly) {
            if (day.count > 0) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return best
    }

    private fun timeBasedGreeting(): String = when (LocalTime.now().hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        in 18..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }

    private fun computeRecommendation(
        daily: DailyProgress,
        completedDocs: Int,
        completedScenarios: Int
    ): Recommendation = when {
        daily.dueWordsCount > 0 && !daily.isGoalReached -> Recommendation(
            title = "Повторите термины по SM-2",
            subtitle = "К повторению готово ${daily.dueWordsCount} слов. Осталось ${
                (daily.dailyGoal - daily.reviewedCardsToday).coerceAtLeast(0)
            } карточек до дневной цели.",
            actionLabel = "Начать повторение",
            destination = RecommendationTarget.FLASHCARDS
        )
        completedScenarios < 3 -> Recommendation(
            title = "Отработайте рабочие сценарии",
            subtitle = "Потренируйте фразы для Daily Standup, Code Review и технических интервью.",
            actionLabel = "Открыть сценарии",
            destination = RecommendationTarget.SCENARIOS
        )
        completedDocs < 3 -> Recommendation(
            title = "Интерактивное чтение документации",
            subtitle = "Читайте реальные спецификации и добавляйте незнакомые слова в SRS одним нажатием.",
            actionLabel = "Читать документацию",
            destination = RecommendationTarget.DOCUMENTATION
        )
        else -> Recommendation(
            title = "Тренировка аудирования и сравнения языков",
            subtitle = "Отличный темп! Закрепите восприятие терминов на слух или сравните синтаксис языков.",
            actionLabel = "Аудирование",
            destination = RecommendationTarget.LISTENING
        )
    }
}
