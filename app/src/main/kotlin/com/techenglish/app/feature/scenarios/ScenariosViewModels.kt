package com.techenglish.app.feature.scenarios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.AudioPlaybackState
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.audio.TtsVoiceStatus
import com.techenglish.domain.model.ComboStreakState
import com.techenglish.domain.model.ComboStreakTracker
import com.techenglish.domain.model.ExerciseType
import com.techenglish.domain.model.ScenarioSubmissionResult
import com.techenglish.domain.model.ScenarioType
import com.techenglish.domain.model.UsefulExpression
import com.techenglish.domain.model.WorkPhrase
import com.techenglish.domain.model.WorkScenario
import com.techenglish.domain.repository.ScenarioRepository
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.usecase.AddWordFromDocUseCase
import com.techenglish.domain.usecase.GetScenariosUseCase
import com.techenglish.domain.usecase.GetWorkPhrasesUseCase
import com.techenglish.domain.usecase.SubmitScenarioAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ==================== PRACTICE HUB & PHRASES VIEWMODEL ====================

data class PracticeHubUiState(
    val isLoading: Boolean = true,
    val selectedTab: Int = 0, // 0 = Scenarios, 1 = Phrases
    val selectedScenarioType: ScenarioType? = null,
    val phraseQuery: String = "",
    val scenarios: List<WorkScenario> = emptyList(),
    val completedScenarioIds: Set<Int> = emptySet(),
    val phrases: List<WorkPhrase> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PracticeHubViewModel @Inject constructor(
    private val getScenarios: GetScenariosUseCase,
    private val getWorkPhrases: GetWorkPhrasesUseCase,
    private val studyLogRepository: StudyLogRepository,
    private val addWordFromDoc: AddWordFromDocUseCase,
    val audioController: TechAudioController
) : ViewModel() {

    private val tabFlow = MutableStateFlow(0)
    private val typeFlow = MutableStateFlow<ScenarioType?>(null)
    private val phraseQueryFlow = MutableStateFlow("")

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val scenariosFlow = typeFlow.flatMapLatest { getScenarios(it) }
    private val phrasesFlow = combine(typeFlow, phraseQueryFlow) { t, q -> t to q }
        .flatMapLatest { (t, q) -> getWorkPhrases(category = t, query = q) }

    val uiState: StateFlow<PracticeHubUiState> = combine(
        combine(tabFlow, typeFlow, phraseQueryFlow) { tab, type, q -> Triple(tab, type, q) },
        scenariosFlow,
        studyLogRepository.observeCompletedScenarioIds(),
        phrasesFlow
    ) { (tab, type, query), scenarios, completedIds, phrases ->
        PracticeHubUiState(
            isLoading = false,
            selectedTab = tab,
            selectedScenarioType = type,
            phraseQuery = query,
            scenarios = scenarios,
            completedScenarioIds = completedIds,
            phrases = phrases
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = PracticeHubUiState(isLoading = true)
    )

    fun selectTab(tab: Int) { tabFlow.value = tab }
    fun toggleType(type: ScenarioType) {
        typeFlow.value = if (typeFlow.value == type) null else type
    }
    fun onPhraseQueryChange(q: String) { phraseQueryFlow.value = q }
    fun speak(text: String) { audioController.speakQuick(text) }

    fun savePhraseToVocabulary(phrase: WorkPhrase) {
        viewModelScope.launch {
            addWordFromDoc(
                term = phrase.phraseEn,
                translation = phrase.translationRu,
                example = phrase.phraseEn,
                explanationRu = "${phrase.category.labelRu}: ${phrase.contextRu}"
            ).onSuccess {
                _events.emit("Фраза добавлена в личный словарь SRS")
            }
        }
    }
}

// ==================== INTERACTIVE SCENARIO EXERCISE VIEWMODEL ====================

data class ScenarioExerciseUiState(
    val isLoading: Boolean = true,
    val scenario: WorkScenario? = null,
    val nextScenarioId: Int? = null,
    val selectedOption: String? = null,
    val selectedSentenceBlocks: List<Int> = emptyList(), // indices into scenario.options
    val customBlankInput: String = "",
    val comboState: ComboStreakState = ComboStreakState(),
    val submissionResult: ScenarioSubmissionResult? = null
) {
    val assembledSentence: String
        get() {
            val opts = scenario?.options ?: return ""
            return selectedSentenceBlocks.mapNotNull { opts.getOrNull(it) }.joinToString(" ")
        }

    val currentUserAnswer: String
        get() = when (scenario?.exerciseType) {
            ExerciseType.SENTENCE_BUILD -> assembledSentence
            ExerciseType.FILL_BLANKS -> selectedOption ?: customBlankInput
            else -> selectedOption.orEmpty()
        }

    val canSubmit: Boolean
        get() = submissionResult == null && currentUserAnswer.isNotBlank()
}

@HiltViewModel
class ScenarioExerciseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scenarioRepository: ScenarioRepository,
    private val submitScenarioAnswer: SubmitScenarioAnswerUseCase,
    private val addWordFromDoc: AddWordFromDocUseCase,
    val audioController: TechAudioController
) : ViewModel() {

    private val initialScenarioId: Int = savedStateHandle.get<Int>("scenarioId") ?: 1

    private val _uiState = MutableStateFlow(ScenarioExerciseUiState(isLoading = true))
    val uiState: StateFlow<ScenarioExerciseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val playbackState: StateFlow<AudioPlaybackState> = audioController.playbackState
    val voiceStatus: StateFlow<TtsVoiceStatus> = audioController.voiceStatus

    init {
        loadScenario(initialScenarioId)
    }

    fun loadScenario(scenarioId: Int) {
        audioController.stop()
        val preservedCombo = _uiState.value.comboState
        viewModelScope.launch {
            _uiState.value = ScenarioExerciseUiState(isLoading = true, comboState = preservedCombo)
            val all = scenarioRepository.getAllScenarios()
            val idx = all.indexOfFirst { it.id == scenarioId }
            val current = all.getOrNull(idx) ?: all.firstOrNull()
            val nextId = if (idx >= 0 && idx + 1 < all.size) all[idx + 1].id else null
            _uiState.value = ScenarioExerciseUiState(
                isLoading = false,
                scenario = current,
                nextScenarioId = nextId,
                comboState = preservedCombo
            )
        }
    }

    fun selectOption(option: String) {
        if (_uiState.value.submissionResult != null) return
        _uiState.value = _uiState.value.copy(selectedOption = option, customBlankInput = option)
    }

    fun onBlankInputChange(text: String) {
        if (_uiState.value.submissionResult != null) return
        _uiState.value = _uiState.value.copy(customBlankInput = text, selectedOption = null)
    }

    fun toggleSentenceBlock(optionIndex: Int) {
        if (_uiState.value.submissionResult != null) return
        val current = _uiState.value.selectedSentenceBlocks
        val updated = if (optionIndex in current) {
            current.filterNot { it == optionIndex }
        } else {
            current + optionIndex
        }
        _uiState.value = _uiState.value.copy(selectedSentenceBlocks = updated)
    }

    fun clearSentenceBlocks() {
        if (_uiState.value.submissionResult != null) return
        _uiState.value = _uiState.value.copy(selectedSentenceBlocks = emptyList())
    }

    @Volatile
    private var isSubmittingAnswer = false

    fun submitAnswer() {
        if (isSubmittingAnswer) return
        val state = _uiState.value
        if (state.submissionResult != null) return
        val scenario = state.scenario ?: return
        val answer = state.currentUserAnswer
        if (answer.isBlank()) return
        isSubmittingAnswer = true
        viewModelScope.launch {
            try {
                val result = submitScenarioAnswer(scenario, answer)
                val latest = _uiState.value
                _uiState.value = latest.copy(
                    submissionResult = result,
                    comboState = ComboStreakTracker.nextState(latest.comboState, result.isCorrect)
                )
            } finally {
                isSubmittingAnswer = false
            }
        }
    }

    fun retry() {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedOption = null,
            selectedSentenceBlocks = emptyList(),
            customBlankInput = "",
            submissionResult = null
        )
    }

    fun playPromptAudio() {
        val prompt = _uiState.value.scenario?.prompt ?: return
        audioController.playWithMedia3(prompt)
    }

    fun speak(text: String) {
        audioController.speakQuick(text)
    }

    fun saveUsefulExpression(expression: UsefulExpression) {
        viewModelScope.launch {
            addWordFromDoc(
                term = expression.en,
                translation = expression.ru,
                example = expression.en,
                explanationRu = expression.context
            ).onSuccess {
                _events.emit("Выражение добавлено в словарь SRS")
            }
        }
    }

    override fun onCleared() {
        audioController.stop()
        super.onCleared()
    }
}
