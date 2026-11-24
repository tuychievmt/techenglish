package com.techenglish.app.feature.documentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.AudioPlaybackState
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.audio.TtsVoiceStatus
import com.techenglish.domain.model.DocArticle
import com.techenglish.domain.model.WordLookupResult
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.usecase.AddWordFromDocUseCase
import com.techenglish.domain.usecase.GetDocArticleDetailUseCase
import com.techenglish.domain.usecase.GetDocArticlesUseCase
import com.techenglish.domain.usecase.LookupWordUseCase
import com.techenglish.domain.usecase.UpdateReadingProgressUseCase
import com.techenglish.domain.usecase.UpdateSettingsUseCase
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

data class DocListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val selectedTag: String? = null,
    val articles: List<DocArticle> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DocListViewModel @Inject constructor(
    private val getDocArticles: GetDocArticlesUseCase
) : ViewModel() {

    val techTags = listOf(
        "Programming", "Linux", "Git", "SQL", "REST API",
        "HTTP", "Networking", "Software Engineering", "Error Logs"
    )

    private val queryFlow = MutableStateFlow("")
    private val tagFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DocListUiState> = combine(queryFlow, tagFlow) { q, t -> q to t }
        .flatMapLatest { (q, t) ->
            getDocArticles(techTag = t, query = q).combine(MutableStateFlow(q to t)) { articles, (query, tag) ->
                DocListUiState(
                    isLoading = false,
                    query = query,
                    selectedTag = tag,
                    articles = articles
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = DocListUiState(isLoading = true)
        )

    fun onQueryChange(query: String) { queryFlow.value = query }
    fun onTagToggle(tag: String) {
        tagFlow.value = if (tagFlow.value == tag) null else tag
    }
}

data class DocReaderUiState(
    val isLoading: Boolean = true,
    val article: DocArticle? = null,
    val fontSizeSp: Float = 16f,
    val showRussianGuide: Boolean = false,
    val lookupResult: WordLookupResult? = null
)

@HiltViewModel
class DocReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getDocArticleDetail: GetDocArticleDetailUseCase,
    settingsRepository: SettingsRepository,
    private val lookupWord: LookupWordUseCase,
    private val addWordFromDoc: AddWordFromDocUseCase,
    private val updateReadingProgress: UpdateReadingProgressUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    val audioController: TechAudioController
) : ViewModel() {

    private val articleId: Int = savedStateHandle.get<Int>("articleId") ?: 1

    private val showRussianGuideFlow = MutableStateFlow(false)
    private val lookupResultFlow = MutableStateFlow<WordLookupResult?>(null)

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val playbackState: StateFlow<AudioPlaybackState> = audioController.playbackState
    val voiceStatus: StateFlow<TtsVoiceStatus> = audioController.voiceStatus

    val uiState: StateFlow<DocReaderUiState> = combine(
        getDocArticleDetail(articleId),
        settingsRepository.observeSettings(),
        showRussianGuideFlow,
        lookupResultFlow
    ) { article, settings, showRu, lookup ->
        DocReaderUiState(
            isLoading = false,
            article = article,
            fontSizeSp = settings.readingFontSizeSp,
            showRussianGuide = showRu,
            lookupResult = lookup
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DocReaderUiState(isLoading = true)
    )

    fun onWordTapped(rawToken: String) {
        val article = uiState.value.article
        viewModelScope.launch {
            lookupResultFlow.value = lookupWord(rawToken, article)
        }
    }

    fun dismissLookup() {
        lookupResultFlow.value = null
    }

    fun addLookupToVocabulary(result: WordLookupResult) {
        viewModelScope.launch {
            addWordFromDoc(
                term = result.term,
                translation = result.translation,
                transcription = result.transcription,
                example = result.example,
                explanationRu = result.explanationRu
            ).onSuccess {
                _events.emit("Слово «${result.term}» добавлено в ваш словарь SRS")
                lookupResultFlow.value = null
            }.onFailure {
                _events.emit(it.message ?: "Ошибка добавления")
            }
        }
    }

    fun toggleRussianGuide() {
        showRussianGuideFlow.value = !showRussianGuideFlow.value
    }

    fun adjustFontSize(deltaSp: Float) {
        val next = (uiState.value.fontSizeSp + deltaSp).coerceIn(13f, 24f)
        viewModelScope.launch { updateSettings.setReadingFontSize(next) }
    }

    fun onScrollProgress(progress: Float) {
        viewModelScope.launch {
            updateReadingProgress(articleId, progress, markCompleted = false)
        }
    }

    fun markCompleted() {
        viewModelScope.launch {
            updateReadingProgress(articleId, 1f, markCompleted = true)
            _events.emit("Статья отмечена прочитанной (+${UpdateReadingProgressUseCase.XP_DOC_COMPLETED} XP)")
        }
    }

    fun playArticle() {
        val text = uiState.value.article?.content ?: return
        audioController.playWithMedia3(text)
    }

    fun speakWord(text: String) {
        audioController.speakQuick(text)
    }

    override fun onCleared() {
        audioController.stop()
        super.onCleared()
    }
}
