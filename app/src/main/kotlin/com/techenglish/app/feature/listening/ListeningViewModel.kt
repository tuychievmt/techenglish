package com.techenglish.app.feature.listening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.AudioPlaybackState
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.audio.TtsVoiceStatus
import com.techenglish.domain.model.ListeningEvaluation
import com.techenglish.domain.model.ListeningMode
import com.techenglish.domain.model.ListeningQuestion
import com.techenglish.domain.usecase.EvaluateListeningAnswerUseCase
import com.techenglish.domain.usecase.GenerateListeningSessionUseCase
import com.techenglish.domain.usecase.LogListeningAttemptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListeningUiState(
    val isLoading: Boolean = true,
    val mode: ListeningMode = ListeningMode.CHOOSE,
    val includePhrases: Boolean = true,
    val questions: List<ListeningQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOption: String? = null,
    val dictationInput: String = "",
    val evaluation: ListeningEvaluation? = null,
    val correctCount: Int = 0,
    val xpEarned: Int = 0,
    val isFinished: Boolean = false
) {
    val currentQuestion: ListeningQuestion? get() = questions.getOrNull(currentIndex)
}

@HiltViewModel
class ListeningViewModel @Inject constructor(
    private val generateSession: GenerateListeningSessionUseCase,
    private val evaluateAnswer: EvaluateListeningAnswerUseCase,
    private val logAttempt: LogListeningAttemptUseCase,
    val audioController: TechAudioController
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListeningUiState(isLoading = true))
    val uiState: StateFlow<ListeningUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<AudioPlaybackState> = audioController.playbackState
    val voiceStatus: StateFlow<TtsVoiceStatus> = audioController.voiceStatus

    init {
        audioController.ensureInitialized()
        startNewSession()
    }

    fun startNewSession(
        mode: ListeningMode = _uiState.value.mode,
        includePhrases: Boolean = _uiState.value.includePhrases
    ) {
        audioController.stop()
        viewModelScope.launch {
            _uiState.value = ListeningUiState(
                isLoading = true,
                mode = mode,
                includePhrases = includePhrases
            )
            val questions = generateSession(count = 8, includePhrases = includePhrases)
            _uiState.value = ListeningUiState(
                isLoading = false,
                mode = mode,
                includePhrases = includePhrases,
                questions = questions,
                isFinished = questions.isEmpty()
            )
        }
    }

    fun setMode(mode: ListeningMode) {
        if (mode == _uiState.value.mode) return
        _uiState.value = _uiState.value.copy(
            mode = mode,
            selectedOption = null,
            dictationInput = "",
            evaluation = null
        )
    }

    fun setIncludePhrases(include: Boolean) {
        if (include == _uiState.value.includePhrases) return
        startNewSession(mode = _uiState.value.mode, includePhrases = include)
    }

    fun playCurrentAudio() {
        val text = _uiState.value.currentQuestion?.audioText ?: return
        audioController.playWithMedia3(text)
    }

    fun onSelectOption(option: String) {
        if (_uiState.value.evaluation != null) return
        _uiState.value = _uiState.value.copy(selectedOption = option)
    }

    fun onDictationChange(text: String) {
        if (_uiState.value.evaluation != null) return
        _uiState.value = _uiState.value.copy(dictationInput = text)
    }

    fun submitCurrentAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.evaluation != null) return

        val eval = when (state.mode) {
            ListeningMode.CHOOSE -> {
                val chosen = state.selectedOption ?: return
                evaluateAnswer.evaluateChoice(question, chosen)
            }
            ListeningMode.DICTATION -> {
                if (state.dictationInput.isBlank()) return
                evaluateAnswer.evaluateDictation(question, state.dictationInput)
            }
        }

        viewModelScope.launch {
            logAttempt(question.wordId, eval)
            _uiState.value = state.copy(
                evaluation = eval,
                correctCount = state.correctCount + if (eval.isCorrect) 1 else 0,
                xpEarned = state.xpEarned + eval.xpEarned
            )
        }
    }

    fun nextQuestion() {
        audioController.stop()
        val state = _uiState.value
        val nextIdx = state.currentIndex + 1
        _uiState.value = state.copy(
            currentIndex = nextIdx,
            selectedOption = null,
            dictationInput = "",
            evaluation = null,
            isFinished = nextIdx >= state.questions.size
        )
    }

    override fun onCleared() {
        audioController.stop()
        super.onCleared()
    }
}
