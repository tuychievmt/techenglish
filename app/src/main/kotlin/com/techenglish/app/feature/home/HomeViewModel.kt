package com.techenglish.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.domain.model.DailyProgress
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.repository.DocumentationRepository
import com.techenglish.domain.repository.ProgrammingLanguageRepository
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.usecase.GetDailyProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
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
        val recommendation: Recommendation
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    getDailyProgress: GetDailyProgressUseCase,
    settingsRepository: SettingsRepository,
    studyLogRepository: StudyLogRepository,
    languageRepository: ProgrammingLanguageRepository,
    documentationRepository: DocumentationRepository
) : ViewModel() {

    private val coreFlow = combine(
        getDailyProgress(),
        settingsRepository.observeSettings(),
        studyLogRepository.observeRecentLogs(limit = 6)
    ) { daily, settings, recent -> Triple(daily, settings, recent) }

    val uiState: StateFlow<HomeUiState> = combine(
        coreFlow,
        languageRepository.observeAllLanguages(),
        documentationRepository.observeCompletedCount(),
        studyLogRepository.observeCompletedScenarioIds()
    ) { (daily, settings, recent), languages, completedDocs, completedScenarios ->
        val recommendation = computeRecommendation(
            daily = daily,
            completedDocs = completedDocs,
            completedScenarios = completedScenarios.size
        )
        HomeUiState.Content(
            greetingRu = timeBasedGreeting(),
            userName = settings.userName,
            dailyProgress = daily,
            recentActivity = recent,
            languagesCount = languages.size,
            completedDocsCount = completedDocs,
            completedScenariosCount = completedScenarios.size,
            recommendation = recommendation
        ) as HomeUiState
    }
        .flowOn(Dispatchers.Default)
        .catch { emit(HomeUiState.Error(it.localizedMessage ?: "Не удалось загрузить данные")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeUiState.Loading
        )

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
