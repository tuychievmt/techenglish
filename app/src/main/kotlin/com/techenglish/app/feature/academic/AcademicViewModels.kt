package com.techenglish.app.feature.academic

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.AudioPlaybackState
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.audio.TtsVoiceStatus
import com.techenglish.domain.model.AcademicDashboardSummary
import com.techenglish.domain.model.AcademicSearchFilter
import com.techenglish.domain.model.AcademicSrsMode
import com.techenglish.domain.model.AcademicStudyFilter
import com.techenglish.domain.model.AcademicTerm
import com.techenglish.domain.model.AcademicTermDetail
import com.techenglish.domain.model.ComboStreakState
import com.techenglish.domain.model.ComboStreakTracker
import com.techenglish.domain.model.ReviewQuality
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.usecase.EnqueueAcademicSrsUseCase
import com.techenglish.domain.usecase.GetAcademicDashboardUseCase
import com.techenglish.domain.usecase.GetAcademicSrsDeckUseCase
import com.techenglish.domain.usecase.GetAcademicTermDetailUseCase
import com.techenglish.domain.usecase.MarkAcademicTermLearnedUseCase
import com.techenglish.domain.usecase.ReviewAcademicTermUseCase
import com.techenglish.domain.usecase.ReviewWordUseCase
import com.techenglish.domain.usecase.SearchAcademicTermsPaginatedUseCase
import com.techenglish.domain.usecase.ToggleAcademicFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ==================== 1. ACADEMIC DICTIONARY EXPLORER VIEWMODEL ====================

@Immutable
data class AcademicDictionaryUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val filter: AcademicSearchFilter = AcademicSearchFilter(),
    val items: ImmutableList<AcademicTerm> = persistentListOf(),
    val totalMatchingCount: Int = 0,
    val currentPage: Int = 0,
    val hasMorePages: Boolean = false,
    val showAdvancedFilters: Boolean = false,
    val translationLanguage: TranslationLanguage = TranslationLanguage.RU
)

@HiltViewModel
class AcademicDictionaryViewModel @Inject constructor(
    getAcademicDashboard: GetAcademicDashboardUseCase,
    private val searchPaginated: SearchAcademicTermsPaginatedUseCase,
    private val toggleFavorite: ToggleAcademicFavoriteUseCase,
    private val enqueueSrs: EnqueueAcademicSrsUseCase,
    private val settingsRepository: SettingsRepository,
    val audioController: TechAudioController
) : ViewModel() {

    val difficulties = listOf("Beginner", "Intermediate", "Advanced", "Expert", "Research Level")
    val cefrLevels = listOf("B1", "B2", "C1", "C2")
    val partsOfSpeech = listOf("noun", "noun phrase", "abbreviation")

    val dashboardState: StateFlow<AcademicDashboardSummary?> = getAcademicDashboard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null
        )

    private val _uiState = MutableStateFlow(AcademicDictionaryUiState(isLoading = true))
    val uiState: StateFlow<AcademicDictionaryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var searchJob: Job? = null

    init {
        executeSearch(resetPage = true)
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(translationLanguage = settings.translationLanguage)
            }
        }
    }

    fun toggleTranslationLanguage() {
        val next = if (_uiState.value.translationLanguage == TranslationLanguage.RU) {
            TranslationLanguage.TJ
        } else {
            TranslationLanguage.RU
        }
        viewModelScope.launch { settingsRepository.setTranslationLanguage(next) }
    }

    fun updateQuery(query: String) {
        updateFilter(_uiState.value.filter.copy(query = query))
    }

    fun toggleAdvancedFilters() {
        _uiState.value = _uiState.value.copy(showAdvancedFilters = !_uiState.value.showAdvancedFilters)
    }

    fun selectMajorCategory(category: String?) {
        val current = _uiState.value.filter.majorCategory
        val next = if (current == category) null else category
        updateFilter(_uiState.value.filter.copy(majorCategory = next, domain = null))
    }

    fun selectDomain(domain: String?) {
        val current = _uiState.value.filter.domain
        val next = if (current == domain) null else domain
        updateFilter(_uiState.value.filter.copy(domain = next))
    }

    fun selectDifficulty(difficulty: String?) {
        val current = _uiState.value.filter.difficulty
        val next = if (current == difficulty) null else difficulty
        updateFilter(_uiState.value.filter.copy(difficulty = next))
    }

    fun selectCefr(cefr: String?) {
        val current = _uiState.value.filter.cefr
        val next = if (current == cefr) null else cefr
        updateFilter(_uiState.value.filter.copy(cefr = next))
    }

    fun selectPartOfSpeech(pos: String?) {
        val current = _uiState.value.filter.partOfSpeech
        val next = if (current == pos) null else pos
        updateFilter(_uiState.value.filter.copy(partOfSpeech = next))
    }

    fun selectStudyFilter(studyFilter: AcademicStudyFilter) {
        updateFilter(_uiState.value.filter.copy(studyFilter = studyFilter))
    }

    fun toggleOnlyFavorites() {
        val f = _uiState.value.filter
        updateFilter(f.copy(onlyFavorites = !f.onlyFavorites))
    }

    fun toggleOnlyDue() {
        val f = _uiState.value.filter
        updateFilter(f.copy(onlyDue = !f.onlyDue))
    }

    fun toggleOnlyDifficult() {
        val f = _uiState.value.filter
        updateFilter(f.copy(onlyDifficult = !f.onlyDifficult))
    }

    fun clearAllFilters() {
        updateFilter(AcademicSearchFilter())
    }

    private fun updateFilter(newFilter: AcademicSearchFilter) {
        _uiState.value = _uiState.value.copy(filter = newFilter)
        executeSearch(resetPage = true)
    }

    private fun executeSearch(resetPage: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val state = _uiState.value
            val targetPage = if (resetPage) 0 else state.currentPage + 1
            if (resetPage) {
                _uiState.value = state.copy(isLoading = true)
            } else {
                _uiState.value = state.copy(isLoadingMore = true)
            }

            val result = searchPaginated(
                filter = _uiState.value.filter,
                page = targetPage
            )
            val merged = if (resetPage) result.items else (state.items + result.items).distinctBy { it.id }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoadingMore = false,
                items = merged.toImmutableList(),
                totalMatchingCount = result.totalMatchingCount,
                currentPage = targetPage,
                hasMorePages = result.hasMore
            )
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMorePages) return
        executeSearch(resetPage = false)
    }

    fun onToggleFavorite(term: AcademicTerm) {
        viewModelScope.launch {
            val next = !term.isFavorite
            toggleFavorite(term.id, next)
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.map {
                    if (it.id == term.id) it.copy(isFavorite = next) else it
                }.toImmutableList()
            )
        }
    }

    fun onToggleSrsQueue(term: AcademicTerm) {
        viewModelScope.launch {
            val next = !term.isInSrs
            enqueueSrs(term.id, next)
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.map {
                    if (it.id == term.id) it.copy(isInSrs = next) else it
                }.toImmutableList()
            )
            _events.emit(
                if (next) "«${term.term}» добавлен в очередь повторения SRS"
                else "«${term.term}» убран из очереди SRS"
            )
        }
    }

    fun speak(text: String) {
        audioController.speakQuick(text)
    }
}

// ==================== 2. ACADEMIC TERM DETAIL VIEWMODEL ====================

data class AcademicTermDetailUiState(
    val isLoading: Boolean = true,
    val detail: AcademicTermDetail? = null,
    val translationLanguage: TranslationLanguage = TranslationLanguage.RU
)

@HiltViewModel
class AcademicTermDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTermDetail: GetAcademicTermDetailUseCase,
    private val toggleFavorite: ToggleAcademicFavoriteUseCase,
    private val enqueueSrs: EnqueueAcademicSrsUseCase,
    private val markLearned: MarkAcademicTermLearnedUseCase,
    private val settingsRepository: SettingsRepository,
    val audioController: TechAudioController
) : ViewModel() {

    private val initialTermId: Int = savedStateHandle.get<Int>("termId") ?: 1

    private val _uiState = MutableStateFlow(AcademicTermDetailUiState(isLoading = true))
    val uiState: StateFlow<AcademicTermDetailUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<AudioPlaybackState> = audioController.playbackState
    val voiceStatus: StateFlow<TtsVoiceStatus> = audioController.voiceStatus

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        loadTerm(initialTermId)
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(translationLanguage = settings.translationLanguage)
            }
        }
    }

    fun loadTerm(termId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val detail = getTermDetail(termId)
            _uiState.value = _uiState.value.copy(isLoading = false, detail = detail)
        }
    }

    fun onToggleFavorite() {
        val detail = _uiState.value.detail ?: return
        val next = !detail.term.isFavorite
        viewModelScope.launch {
            toggleFavorite(detail.term.id, next)
            _uiState.value = _uiState.value.copy(
                detail = detail.copy(term = detail.term.copy(isFavorite = next))
            )
        }
    }

    fun onToggleInSrs() {
        val detail = _uiState.value.detail ?: return
        val next = !detail.term.isInSrs
        viewModelScope.launch {
            enqueueSrs(detail.term.id, next)
            _uiState.value = _uiState.value.copy(
                detail = detail.copy(term = detail.term.copy(isInSrs = next))
            )
            _events.emit(
                if (next) "Добавлено в очередь повторения SM-2"
                else "Убрано из очереди SM-2"
            )
        }
    }

    fun onMarkAsLearned() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            markLearned(detail.term)
            val refreshed = getTermDetail(detail.term.id)
            _uiState.value = _uiState.value.copy(detail = refreshed)
            _events.emit("Термин отмечен как изученный (+10 XP)")
        }
    }

    fun onRelatedTermClicked(relatedName: String) {
        viewModelScope.launch {
            val resolvedId = getTermDetail.resolveRelatedTermId(relatedName)
            if (resolvedId != null) {
                loadTerm(resolvedId)
            } else {
                _events.emit("Связанная тема: $relatedName")
            }
        }
    }

    fun speak(text: String) {
        audioController.speakQuick(text)
    }

    fun switchAccent(localeTag: String) {
        audioController.setSpeechLocale(localeTag)
    }
}

// ==================== 3. ADVANCED SRS & DEFINITION QUIZ VIEWMODEL ====================

enum class AcademicPracticeFormat(val labelRu: String) {
    SM2_CARDS("Карточки SM-2"),
    DEFINITION_QUIZ("Тест определений")
}

data class AcademicSrsUiState(
    val isLoading: Boolean = true,
    val mode: AcademicSrsMode = AcademicSrsMode.ACADEMIC_VOCABULARY,
    val format: AcademicPracticeFormat = AcademicPracticeFormat.SM2_CARDS,
    val deck: List<AcademicTerm> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val quizOptions: List<String> = emptyList(),
    val selectedQuizOption: String? = null,
    val intervalPreviews: Map<ReviewQuality, Int> = emptyMap(),
    val reviewedCount: Int = 0,
    val correctCount: Int = 0,
    val xpEarned: Int = 0,
    val comboState: ComboStreakState = ComboStreakState(),
    val isSessionFinished: Boolean = false,
    val translationLanguage: TranslationLanguage = TranslationLanguage.RU
) {
    val currentTerm: AcademicTerm? get() = deck.getOrNull(currentIndex)
}

@HiltViewModel
class AcademicSrsViewModel @Inject constructor(
    private val getSrsDeck: GetAcademicSrsDeckUseCase,
    private val reviewTerm: ReviewAcademicTermUseCase,
    private val settingsRepository: SettingsRepository,
    val audioController: TechAudioController
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademicSrsUiState(isLoading = true))
    val uiState: StateFlow<AcademicSrsUiState> = _uiState.asStateFlow()

    init {
        loadDeck(AcademicSrsMode.ACADEMIC_VOCABULARY)
    }

    fun selectMode(mode: AcademicSrsMode) {
        loadDeck(mode, _uiState.value.format)
    }

    fun selectFormat(format: AcademicPracticeFormat) {
        val current = _uiState.value
        val term = current.currentTerm
        _uiState.value = current.copy(
            format = format,
            isFlipped = false,
            selectedQuizOption = null,
            quizOptions = if (term != null) buildQuizOptions(term, current.deck) else emptyList()
        )
    }

    fun loadDeck(
        mode: AcademicSrsMode = _uiState.value.mode,
        format: AcademicPracticeFormat = _uiState.value.format
    ) {
        viewModelScope.launch {
            _uiState.value = AcademicSrsUiState(isLoading = true, mode = mode, format = format)
            val settings = settingsRepository.observeSettings().first()
            val dailyGoal = settings.dailyGoal.coerceAtLeast(10)
            val lang = settings.translationLanguage
            val deck = getSrsDeck(mode = mode, limit = dailyGoal)
            val first = deck.firstOrNull()
            _uiState.value = AcademicSrsUiState(
                isLoading = false,
                mode = mode,
                format = format,
                deck = deck,
                currentIndex = 0,
                isFlipped = false,
                quizOptions = if (first != null) buildQuizOptions(first, deck, lang) else emptyList(),
                intervalPreviews = first?.let { reviewTerm.previewIntervals(it) }.orEmpty(),
                isSessionFinished = deck.isEmpty(),
                translationLanguage = lang
            )
        }
    }

    fun flipCard() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun rateCard(quality: ReviewQuality) {
        val state = _uiState.value
        val term = state.currentTerm ?: return
        viewModelScope.launch {
            reviewTerm(term, quality)
            val xp = if (quality.isCorrect) ReviewWordUseCase.XP_FLASHCARD_CORRECT else ReviewWordUseCase.XP_FLASHCARD_RETRY
            advanceToNext(
                isCorrect = quality.isCorrect,
                xpDelta = xp
            )
        }
    }

    fun answerQuiz(option: String) {
        val state = _uiState.value
        val term = state.currentTerm ?: return
        if (state.selectedQuizOption != null) return
        val correctLabel = "${term.term} — ${term.displayTranslation(state.translationLanguage)}"
        val isCorrect = option == correctLabel
        val quality = if (isCorrect) ReviewQuality.GOOD else ReviewQuality.FORGOT
        viewModelScope.launch {
            reviewTerm(term, quality)
            val xp = if (isCorrect) ReviewWordUseCase.XP_FLASHCARD_CORRECT else ReviewWordUseCase.XP_FLASHCARD_RETRY
            _uiState.value = state.copy(
                selectedQuizOption = option,
                reviewedCount = state.reviewedCount + 1,
                correctCount = state.correctCount + if (isCorrect) 1 else 0,
                xpEarned = state.xpEarned + xp,
                comboState = ComboStreakTracker.nextState(state.comboState, isCorrect)
            )
        }
    }

    fun nextAfterQuiz() {
        val state = _uiState.value
        val nextIdx = state.currentIndex + 1
        val nextTerm = state.deck.getOrNull(nextIdx)
        _uiState.value = state.copy(
            currentIndex = nextIdx,
            selectedQuizOption = null,
            quizOptions = if (nextTerm != null) buildQuizOptions(nextTerm, state.deck) else emptyList(),
            intervalPreviews = nextTerm?.let { reviewTerm.previewIntervals(it) }.orEmpty(),
            isSessionFinished = nextIdx >= state.deck.size
        )
    }

    private fun advanceToNext(isCorrect: Boolean, xpDelta: Int) {
        val state = _uiState.value
        val nextIdx = state.currentIndex + 1
        val nextTerm = state.deck.getOrNull(nextIdx)
        _uiState.value = state.copy(
            currentIndex = nextIdx,
            isFlipped = false,
            selectedQuizOption = null,
            quizOptions = if (nextTerm != null) buildQuizOptions(nextTerm, state.deck) else emptyList(),
            intervalPreviews = nextTerm?.let { reviewTerm.previewIntervals(it) }.orEmpty(),
            reviewedCount = state.reviewedCount + 1,
            correctCount = state.correctCount + if (isCorrect) 1 else 0,
            xpEarned = state.xpEarned + xpDelta,
            comboState = ComboStreakTracker.nextState(state.comboState, isCorrect),
            isSessionFinished = nextIdx >= state.deck.size
        )
    }

    private fun buildQuizOptions(
        target: AcademicTerm,
        pool: List<AcademicTerm>,
        language: TranslationLanguage = _uiState.value.translationLanguage
    ): List<String> {
        val correct = "${target.term} — ${target.displayTranslation(language)}"
        val distractors = pool
            .filter { it.id != target.id }
            .shuffled()
            .take(3)
            .map { "${it.term} — ${it.displayTranslation(language)}" }
        return (distractors + correct).shuffled()
    }

    fun speak(text: String) {
        audioController.speakQuick(text)
    }
}
