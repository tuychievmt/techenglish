package com.techenglish.app.feature.vocabulary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.audio.TtsVoiceStatus
import com.techenglish.domain.model.ComboStreakState
import com.techenglish.domain.model.ComboStreakTracker
import com.techenglish.domain.model.ReviewQuality
import com.techenglish.domain.model.Word
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.usecase.CalculateNextReviewUseCase
import com.techenglish.domain.usecase.GetDueWordsUseCase
import com.techenglish.domain.usecase.ReviewWordUseCase
import com.techenglish.domain.usecase.ToggleFavoriteWordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlashcardsUiState(
    val isLoading: Boolean = true,
    val queue: List<Word> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val reviewedCount: Int = 0,
    val correctCount: Int = 0,
    val xpEarned: Int = 0,
    val comboState: ComboStreakState = ComboStreakState(),
    val intervalPreviews: Map<ReviewQuality, Int> = emptyMap(),
    val isSessionFinished: Boolean = false
) {
    val currentWord: Word? get() = queue.getOrNull(currentIndex)
    val totalInSession: Int get() = queue.size
}

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDueWords: GetDueWordsUseCase,
    private val calculateNextReview: CalculateNextReviewUseCase,
    private val reviewWord: ReviewWordUseCase,
    private val toggleFavorite: ToggleFavoriteWordUseCase,
    private val settingsRepository: SettingsRepository,
    val audioController: TechAudioController
) : ViewModel() {

    private val categoryId: Int? = savedStateHandle.get<Int>("categoryId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(FlashcardsUiState(isLoading = true))
    val uiState: StateFlow<FlashcardsUiState> = _uiState.asStateFlow()

    val voiceStatus: StateFlow<TtsVoiceStatus> = audioController.voiceStatus

    init {
        loadSession()
    }

    fun loadSession() {
        viewModelScope.launch {
            _uiState.value = FlashcardsUiState(isLoading = true)
            val goal = settingsRepository.observeSettings().first().dailyGoal
            val due = getDueWords(limit = goal.coerceAtLeast(10), categoryId = categoryId)
            val firstWord = due.firstOrNull()
            _uiState.value = FlashcardsUiState(
                isLoading = false,
                queue = due,
                currentIndex = 0,
                isFlipped = false,
                intervalPreviews = firstWord?.let { calculateNextReview.previewIntervals(it) }.orEmpty(),
                isSessionFinished = due.isEmpty()
            )
        }
    }

    fun flipCard() {
        val current = _uiState.value
        if (current.currentWord == null) return
        _uiState.value = current.copy(isFlipped = !current.isFlipped)
    }

    @Volatile
    private var isRatingSubmissionInFlight = false

    fun rateCurrentCard(quality: ReviewQuality) {
        if (isRatingSubmissionInFlight) return
        val state = _uiState.value
        val word = state.currentWord ?: return
        isRatingSubmissionInFlight = true
        viewModelScope.launch {
            try {
                reviewWord(word, quality)
                val gainedXp = if (quality.isCorrect) ReviewWordUseCase.XP_FLASHCARD_CORRECT else ReviewWordUseCase.XP_FLASHCARD_RETRY
                _uiState.update { latest ->
                    val updatedQueue = latest.queue.toMutableList()
                    // If user forgot (q=1), re-queue once at end of session for immediate reinforcement
                    if (quality == ReviewQuality.FORGOT && updatedQueue.count { it.id == word.id } < 2) {
                        updatedQueue.add(
                            word.copy(repetitions = 0, intervalDays = 0, nextReviewAt = 0L)
                        )
                    }
                    val nextIdx = latest.currentIndex + 1
                    val finished = nextIdx >= updatedQueue.size
                    val nextWord = updatedQueue.getOrNull(nextIdx)
                    latest.copy(
                        queue = updatedQueue,
                        currentIndex = nextIdx,
                        isFlipped = false,
                        reviewedCount = latest.reviewedCount + 1,
                        correctCount = latest.correctCount + if (quality.isCorrect) 1 else 0,
                        xpEarned = latest.xpEarned + gainedXp,
                        comboState = ComboStreakTracker.nextState(latest.comboState, quality.isCorrect),
                        intervalPreviews = nextWord?.let { calculateNextReview.previewIntervals(it) }.orEmpty(),
                        isSessionFinished = finished
                    )
                }
            } finally {
                isRatingSubmissionInFlight = false
            }
        }
    }

    fun toggleCurrentFavorite() {
        val word = _uiState.value.currentWord ?: return
        viewModelScope.launch {
            val nextFav = !word.isFavorite
            toggleFavorite(word.id, nextFav)
            _uiState.update { latest ->
                latest.copy(
                    queue = latest.queue.map {
                        if (it.id == word.id) it.copy(isFavorite = nextFav) else it
                    }
                )
            }
        }
    }

    fun speak(text: String) {
        audioController.speakQuick(text)
    }
}
