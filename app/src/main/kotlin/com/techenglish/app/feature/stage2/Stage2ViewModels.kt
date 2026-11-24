package com.techenglish.app.feature.stage2

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.OnDeviceSpeechRecognizerHelper
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.database.AnkiApkgExtractor
import com.techenglish.domain.model.ComboStreakState
import com.techenglish.domain.model.ComboStreakTracker
import com.techenglish.domain.model.CustomImportedDoc
import com.techenglish.domain.model.DebugLogChallenge
import com.techenglish.domain.model.DebugLogEvaluation
import com.techenglish.domain.model.DeckFormat
import com.techenglish.domain.model.DeckImportSummary
import com.techenglish.domain.model.DetectedDocTerm
import com.techenglish.domain.model.PronunciationEvaluation
import com.techenglish.domain.model.ShadowingPrompt
import com.techenglish.domain.model.SpeechEngineAvailability
import com.techenglish.domain.model.TimeAttackMode
import com.techenglish.domain.model.TimeAttackQuestion
import com.techenglish.domain.model.TimeAttackRecord
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.usecase.AddCustomWordUseCase
import com.techenglish.domain.usecase.DebugLogTrainerUseCase
import com.techenglish.domain.usecase.DeckImportExportUseCase
import com.techenglish.domain.usecase.EvaluatePronunciationUseCase
import com.techenglish.domain.usecase.ScanCustomDocTermsUseCase
import com.techenglish.domain.usecase.TimeAttackQuizUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

// =========================================================================
// ФТ-7.1 — Read Your Own Docs ViewModel
// =========================================================================

data class ReadOwnDocsUiState(
    val savedDocs: List<CustomImportedDoc> = emptyList(),
    val activeDoc: CustomImportedDoc? = null,
    val draftTitle: String = "",
    val draftText: String = "",
    val isScanning: Boolean = false,
    val selectedTerm: DetectedDocTerm? = null,
    val translationLanguage: TranslationLanguage = TranslationLanguage.RU,
    val statusMessage: String? = null
)

@HiltViewModel
class ReadYourOwnDocsViewModel @Inject constructor(
    private val scanUseCase: ScanCustomDocTermsUseCase,
    private val academicRepository: AcademicVocabularyRepository,
    private val addCustomWordUseCase: AddCustomWordUseCase,
    private val settingsRepository: SettingsRepository,
    val audioController: TechAudioController
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadOwnDocsUiState())
    val uiState: StateFlow<ReadOwnDocsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scanUseCase.observeImportedDocs().collect { docs ->
                _uiState.update { state ->
                    state.copy(
                        savedDocs = docs,
                        activeDoc = state.activeDoc?.let { active ->
                            docs.firstOrNull { it.id == active.id } ?: active
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(translationLanguage = settings.translationLanguage) }
            }
        }
    }

    fun updateDraftTitle(title: String) {
        _uiState.update { it.copy(draftTitle = title, statusMessage = null) }
    }

    fun updateDraftText(text: String) {
        _uiState.update { it.copy(draftText = text, statusMessage = null) }
    }

    fun scanAndSaveDraft() {
        val state = _uiState.value
        if (state.draftText.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Вставьте текст или импортируйте .md / .txt файл.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, statusMessage = null) }
            val saved = withContext(Dispatchers.Default) {
                scanUseCase.importAndSaveDocument(
                    title = state.draftTitle.ifBlank { "Мой README / заметки" },
                    sourceFileName = "pasted_text.md",
                    content = state.draftText
                )
            }
            _uiState.update {
                it.copy(
                    isScanning = false,
                    activeDoc = saved,
                    draftTitle = "",
                    draftText = "",
                    statusMessage = "Найдено IT-терминов: ${saved.detectedTermsCount}"
                )
            }
        }
    }

    fun importFromUri(contentResolver: ContentResolver, uri: Uri, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, statusMessage = null) }
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    }.orEmpty()
                }
                require(text.isNotBlank()) { "Выбранный файл пуст." }
                withContext(Dispatchers.Default) {
                    scanUseCase.importAndSaveDocument(
                        title = displayName.substringBeforeLast('.').ifBlank { "Импортированный документ" },
                        sourceFileName = displayName,
                        content = text.take(120_000)
                    )
                }
            }.onSuccess { saved ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        activeDoc = saved,
                        statusMessage = "Импортировано «${saved.title}»: найдено терминов — ${saved.detectedTermsCount}"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        statusMessage = "Ошибка чтения файла: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun openSavedDoc(doc: CustomImportedDoc) {
        _uiState.update { it.copy(activeDoc = doc, selectedTerm = null, statusMessage = null) }
    }

    fun closeActiveDoc() {
        _uiState.update { it.copy(activeDoc = null, selectedTerm = null) }
    }

    fun deleteDoc(id: Int) {
        viewModelScope.launch {
            scanUseCase.deleteImportedDoc(id)
            _uiState.update { state ->
                state.copy(
                    activeDoc = if (state.activeDoc?.id == id) null else state.activeDoc,
                    statusMessage = "Документ удалён"
                )
            }
        }
    }

    fun selectTerm(term: DetectedDocTerm?) {
        _uiState.update { it.copy(selectedTerm = term) }
    }

    fun addSelectedTermToSrs() {
        val selected = _uiState.value.selectedTerm ?: return
        viewModelScope.launch {
            if (selected.termId != null) {
                academicRepository.setInSrs(selected.termId!!, true)
                _uiState.update {
                    it.copy(
                        selectedTerm = null,
                        statusMessage = "«${selected.headword}» добавлен в очередь SRS!"
                    )
                }
            } else {
                addCustomWordUseCase(
                    term = selected.headword,
                    translation = selected.translationRu,
                    transcription = selected.transcription,
                    explanationRu = selected.explanationRu
                )
                _uiState.update {
                    it.copy(
                        selectedTerm = null,
                        statusMessage = "«${selected.headword}» сохранён в «Мои слова»!"
                    )
                }
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

// =========================================================================
// ФТ-7.2 — Interactive «Debug the Log» Trainer ViewModel
// =========================================================================

data class DebugLogUiState(
    val challenges: List<DebugLogChallenge> = emptyList(),
    val currentIndex: Int = 0,
    val selectedRootCause: String? = null,
    val selectedReportTokens: List<String> = emptyList(),
    val evaluation: DebugLogEvaluation? = null,
    val comboStreak: ComboStreakState = ComboStreakState()
) {
    val currentChallenge: DebugLogChallenge? get() = challenges.getOrNull(currentIndex)
}

@HiltViewModel
class DebugTheLogViewModel @Inject constructor(
    private val trainerUseCase: DebugLogTrainerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DebugLogUiState(challenges = trainerUseCase.getChallenges())
    )
    val uiState: StateFlow<DebugLogUiState> = _uiState.asStateFlow()

    fun selectRootCause(option: String) {
        if (_uiState.value.evaluation != null) return
        _uiState.update { it.copy(selectedRootCause = option) }
    }

    fun toggleReportToken(token: String) {
        if (_uiState.value.evaluation != null) return
        _uiState.update { state ->
            val updated = if (token in state.selectedReportTokens) {
                state.selectedReportTokens - token
            } else {
                state.selectedReportTokens + token
            }
            state.copy(selectedReportTokens = updated)
        }
    }

    fun clearReportTokens() {
        if (_uiState.value.evaluation != null) return
        _uiState.update { it.copy(selectedReportTokens = emptyList()) }
    }

    fun submitDiagnosis() {
        val state = _uiState.value
        val challenge = state.currentChallenge ?: return
        val rootCause = state.selectedRootCause ?: return
        viewModelScope.launch {
            val result = trainerUseCase.submitDiagnosis(
                challenge = challenge,
                selectedRootCause = rootCause,
                assembledReportTokens = state.selectedReportTokens
            )
            _uiState.update {
                it.copy(
                    evaluation = result,
                    comboStreak = ComboStreakTracker.nextState(it.comboStreak, result.isFullySolved)
                )
            }
        }
    }

    fun nextChallenge() {
        _uiState.update { state ->
            val nextIdx = (state.currentIndex + 1) % state.challenges.size.coerceAtLeast(1)
            state.copy(
                currentIndex = nextIdx,
                selectedRootCause = null,
                selectedReportTokens = emptyList(),
                evaluation = null
            )
        }
    }
}

// =========================================================================
// ФТ-7.3 — Speaking / Shadowing Pronunciation Trainer ViewModel
// =========================================================================

data class PronunciationUiState(
    val prompts: List<ShadowingPrompt> = emptyList(),
    val currentIndex: Int = 0,
    val availability: SpeechEngineAvailability = SpeechEngineAvailability.REQUIRES_VOICE_PACK_OR_NETWORK,
    val isListening: Boolean = false,
    val manualTranscriptInput: String = "",
    val recognizedTranscript: String = "",
    val evaluation: PronunciationEvaluation? = null,
    val errorMessage: String? = null
) {
    val currentPrompt: ShadowingPrompt? get() = prompts.getOrNull(currentIndex)
}

@HiltViewModel
class PronunciationTrainerViewModel @Inject constructor(
    private val evaluateUseCase: EvaluatePronunciationUseCase,
    private val speechHelper: OnDeviceSpeechRecognizerHelper,
    val audioController: TechAudioController
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PronunciationUiState(
            prompts = evaluateUseCase.getShadowingPrompts(),
            availability = speechHelper.checkAvailability()
        )
    )
    val uiState: StateFlow<PronunciationUiState> = _uiState.asStateFlow()

    fun refreshAvailability() {
        _uiState.update { it.copy(availability = speechHelper.checkAvailability()) }
    }

    fun startRecording() {
        _uiState.update { it.copy(errorMessage = null, evaluation = null, isListening = true) }
        val started = speechHelper.startListening(
            onPartialResult = { partial ->
                _uiState.update { it.copy(recognizedTranscript = partial) }
            },
            onFinalResult = { finalText ->
                _uiState.update { it.copy(isListening = false, recognizedTranscript = finalText) }
                evaluateSpoken(finalText)
            },
            onError = { err ->
                _uiState.update { it.copy(isListening = false, errorMessage = err) }
            }
        )
        if (!started) {
            _uiState.update { it.copy(isListening = false) }
        }
    }

    fun stopRecording() {
        speechHelper.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    fun updateManualTranscript(text: String) {
        _uiState.update { it.copy(manualTranscriptInput = text, errorMessage = null) }
    }

    fun evaluateManualInput() {
        evaluateSpoken(_uiState.value.manualTranscriptInput)
    }

    private fun evaluateSpoken(spoken: String) {
        val prompt = _uiState.value.currentPrompt ?: return
        viewModelScope.launch {
            val result = evaluateUseCase.evaluate(prompt.targetEn, spoken)
            _uiState.update { it.copy(evaluation = result, recognizedTranscript = spoken) }
        }
    }

    fun selectPrompt(index: Int) {
        stopRecording()
        _uiState.update {
            it.copy(
                currentIndex = index.coerceIn(0, (it.prompts.size - 1).coerceAtLeast(0)),
                manualTranscriptInput = "",
                recognizedTranscript = "",
                evaluation = null,
                errorMessage = null
            )
        }
    }

    override fun onCleared() {
        speechHelper.stopListening()
        super.onCleared()
    }
}

// =========================================================================
// ФТ-7.5 — Time Attack / Speedrun Quiz ViewModel (60-second sprint)
// =========================================================================

data class TimeAttackUiState(
    val selectedMode: TimeAttackMode = TimeAttackMode.TERM_TRANSLATION,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val secondsLeft: Int = TimeAttackQuizUseCase.DEFAULT_DURATION_SECONDS,
    val questions: List<TimeAttackQuestion> = emptyList(),
    val questionIndex: Int = 0,
    val score: Int = 0,
    val correctCount: Int = 0,
    val totalAnswered: Int = 0,
    val comboStreak: ComboStreakState = ComboStreakState(),
    val personalBest: Int = 0,
    val lastSavedRecord: TimeAttackRecord? = null,
    val leaderboard: List<TimeAttackRecord> = emptyList()
) {
    val currentQuestion: TimeAttackQuestion? get() = questions.getOrNull(questionIndex)
}

@HiltViewModel
class TimeAttackViewModel @Inject constructor(
    private val timeAttackUseCase: TimeAttackQuizUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeAttackUiState())
    val uiState: StateFlow<TimeAttackUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            timeAttackUseCase.observeLeaderboard(10).collect { records ->
                _uiState.update { it.copy(leaderboard = records) }
            }
        }
        refreshBestScore(TimeAttackMode.TERM_TRANSLATION)
    }

    fun selectMode(mode: TimeAttackMode) {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(selectedMode = mode, isFinished = false, lastSavedRecord = null) }
        refreshBestScore(mode)
    }

    private fun refreshBestScore(mode: TimeAttackMode) {
        viewModelScope.launch {
            val best = timeAttackUseCase.getPersonalBest(mode)
            _uiState.update { it.copy(personalBest = best) }
        }
    }

    fun startRun() {
        timerJob?.cancel()
        val mode = _uiState.value.selectedMode
        viewModelScope.launch {
            val lang = settingsRepository.observeSettings().first().translationLanguage
            val qs = withContext(Dispatchers.Default) {
                timeAttackUseCase.generateQuestions(mode, lang, count = 30)
            }
            _uiState.update {
                it.copy(
                    isRunning = true,
                    isFinished = false,
                    secondsLeft = TimeAttackQuizUseCase.DEFAULT_DURATION_SECONDS,
                    questions = qs,
                    questionIndex = 0,
                    score = 0,
                    correctCount = 0,
                    totalAnswered = 0,
                    comboStreak = ComboStreakState(),
                    lastSavedRecord = null
                )
            }
            timerJob = viewModelScope.launch {
                while (_uiState.value.isRunning && _uiState.value.secondsLeft > 0) {
                    delay(1000L)
                    _uiState.update { it.copy(secondsLeft = (it.secondsLeft - 1).coerceAtLeast(0)) }
                }
                if (_uiState.value.isRunning) {
                    finishRun()
                }
            }
        }
    }

    fun answerOption(option: String) {
        val state = _uiState.value
        if (!state.isRunning) return
        val q = state.currentQuestion ?: return
        val isCorrect = option == q.correctOption
        val nextCombo = ComboStreakTracker.nextState(state.comboStreak, isCorrect)
        val deltaPoints = if (isCorrect) timeAttackUseCase.calculatePointsForCorrectAnswer(nextCombo.currentStreak) else 0
        val nextIndex = state.questionIndex + 1

        _uiState.update {
            it.copy(
                score = it.score + deltaPoints,
                correctCount = it.correctCount + if (isCorrect) 1 else 0,
                totalAnswered = it.totalAnswered + 1,
                comboStreak = nextCombo,
                questionIndex = nextIndex
            )
        }

        if (nextIndex >= state.questions.size) {
            finishRun()
        }
    }

    fun finishRun() {
        timerJob?.cancel()
        val state = _uiState.value
        if (!state.isRunning && state.isFinished) return
        _uiState.update { it.copy(isRunning = false, isFinished = true) }

        viewModelScope.launch {
            val saved = timeAttackUseCase.recordSessionResult(
                mode = state.selectedMode,
                score = state.score,
                correctCount = state.correctCount,
                totalAnswered = state.totalAnswered,
                durationSeconds = TimeAttackQuizUseCase.DEFAULT_DURATION_SECONDS - state.secondsLeft
            )
            val best = timeAttackUseCase.getPersonalBest(state.selectedMode)
            _uiState.update { it.copy(lastSavedRecord = saved, personalBest = best) }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}

// =========================================================================
// ФТ-7.6 — Deck Import & Export ViewModel (JSON / CSV / Anki TSV & .apkg)
// =========================================================================

data class DeckTransferUiState(
    val exportedPreview: String = "",
    val exportedFormat: DeckFormat? = null,
    val pastedImportText: String = "",
    val selectedImportFormat: DeckFormat = DeckFormat.ANKI_TSV,
    val lastImportSummary: DeckImportSummary? = null,
    val isBusy: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class DeckImportExportViewModel @Inject constructor(
    private val deckUseCase: DeckImportExportUseCase,
    private val ankiApkgExtractor: AnkiApkgExtractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckTransferUiState())
    val uiState: StateFlow<DeckTransferUiState> = _uiState.asStateFlow()

    fun selectImportFormat(format: DeckFormat) {
        _uiState.update { it.copy(selectedImportFormat = format, statusMessage = null) }
    }

    fun updatePastedImportText(text: String) {
        _uiState.update { it.copy(pastedImportText = text, statusMessage = null) }
    }

    fun generateExport(format: DeckFormat) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null) }
            val content = withContext(Dispatchers.IO) {
                when (format) {
                    DeckFormat.JSON -> deckUseCase.exportCustomAndLearnedWordsToJson()
                    DeckFormat.CSV -> deckUseCase.exportCustomAndLearnedWordsToCsv()
                    else -> deckUseCase.exportCustomAndLearnedWordsToJson()
                }
            }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    exportedPreview = content,
                    exportedFormat = format,
                    statusMessage = "Экспорт ${format.labelRu} сформирован (${content.length} симв.)."
                )
            }
        }
    }

    fun writeExportToUri(contentResolver: ContentResolver, uri: Uri, format: DeckFormat) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null) }
            runCatching {
                val payload = withContext(Dispatchers.IO) {
                    when (format) {
                        DeckFormat.CSV -> deckUseCase.exportCustomAndLearnedWordsToCsv()
                        else -> deckUseCase.exportCustomAndLearnedWordsToJson()
                    }
                }
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: error("Не удалось открыть файл для записи.")
                }
                payload
            }.onSuccess { payload ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        exportedPreview = payload,
                        exportedFormat = format,
                        statusMessage = "Колода успешно сохранена в файл (${format.labelRu})."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, statusMessage = "Ошибка экспорта: ${err.localizedMessage}")
                }
            }
        }
    }

    fun importFromPastedText() {
        val state = _uiState.value
        if (state.pastedImportText.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Вставьте содержимое JSON, CSV или Anki TSV для импорта.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null) }
            runCatching {
                val format = state.selectedImportFormat
                val entries = withContext(Dispatchers.Default) {
                    when (format) {
                        DeckFormat.JSON -> deckUseCase.parseJsonDeck(state.pastedImportText)
                        DeckFormat.CSV -> deckUseCase.parseCsvDeck(state.pastedImportText)
                        DeckFormat.ANKI_TSV, DeckFormat.ANKI_APKG -> deckUseCase.parseAnkiTsvDeck(state.pastedImportText)
                    }
                }
                deckUseCase.importCards(format, entries)
            }.onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        lastImportSummary = summary,
                        pastedImportText = "",
                        statusMessage = "Импорт завершён: добавлено ${summary.importedCount}, пропущено дубликатов ${summary.skippedDuplicates}."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, statusMessage = "Ошибка импорта: ${err.localizedMessage}")
                }
            }
        }
    }

    fun importFromUri(contentResolver: ContentResolver, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null) }
            runCatching {
                val lower = fileName.lowercase()
                val detectedFormat = when {
                    lower.endsWith(".apkg") -> DeckFormat.ANKI_APKG
                    lower.endsWith(".json") -> DeckFormat.JSON
                    lower.endsWith(".csv") -> DeckFormat.CSV
                    lower.endsWith(".tsv") || lower.endsWith(".txt") -> DeckFormat.ANKI_TSV
                    else -> _uiState.value.selectedImportFormat
                }
                val entries = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        when (detectedFormat) {
                            DeckFormat.ANKI_APKG -> {
                                val noteRows = ankiApkgExtractor.extractNoteRows(stream)
                                deckUseCase.parseAnkiNoteRows(noteRows)
                            }
                            DeckFormat.JSON -> {
                                val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                                deckUseCase.parseJsonDeck(text)
                            }
                            DeckFormat.CSV -> {
                                val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                                deckUseCase.parseCsvDeck(text)
                            }
                            DeckFormat.ANKI_TSV -> {
                                val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                                deckUseCase.parseAnkiTsvDeck(text)
                            }
                        }
                    } ?: error("Не удалось открыть выбранный файл.")
                }
                deckUseCase.importCards(detectedFormat, entries)
            }.onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        lastImportSummary = summary,
                        statusMessage = "Файл «$fileName» (${summary.format.labelRu}): добавлено ${summary.importedCount} из ${summary.totalParsed}."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, statusMessage = "Ошибка импорта файла: ${err.localizedMessage}")
                }
            }
        }
    }
}
