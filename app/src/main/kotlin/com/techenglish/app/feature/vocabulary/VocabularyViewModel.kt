package com.techenglish.app.feature.vocabulary

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.audio.TtsVoiceStatus
import com.techenglish.app.core.designsystem.DEFAULT_ACADEMIC_TERMS_COUNT
import com.techenglish.domain.model.Category
import com.techenglish.domain.model.ProgrammingLanguage
import com.techenglish.domain.model.Word
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.ProgrammingLanguageRepository
import com.techenglish.domain.repository.VocabularyRepository
import com.techenglish.domain.usecase.AddCustomWordUseCase
import com.techenglish.domain.usecase.DeleteCustomWordUseCase
import com.techenglish.domain.usecase.SearchWordsUseCase
import com.techenglish.domain.usecase.ToggleFavoriteWordUseCase
import com.techenglish.domain.usecase.UpdateCustomWordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VocabQuickFilter(val labelRu: String) {
    ALL("Все"),
    DUE("К повторению"),
    FAVORITES("Избранное"),
    CUSTOM("Мои термины")
}

@Immutable
data class VocabularyUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val selectedCategoryId: Int? = null,
    val quickFilter: VocabQuickFilter = VocabQuickFilter.ALL,
    val categories: ImmutableList<Category> = persistentListOf(),
    val languages: ImmutableList<ProgrammingLanguage> = persistentListOf(),
    val words: ImmutableList<Word> = persistentListOf(),
    val dueCount: Int = 0,
    val academicTermCount: Int = DEFAULT_ACADEMIC_TERMS_COUNT
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    academicVocabularyRepository: AcademicVocabularyRepository,
    languageRepository: ProgrammingLanguageRepository,
    private val searchWords: SearchWordsUseCase,
    private val addCustomWord: AddCustomWordUseCase,
    private val updateCustomWord: UpdateCustomWordUseCase,
    private val deleteCustomWord: DeleteCustomWordUseCase,
    private val toggleFavorite: ToggleFavoriteWordUseCase,
    val audioController: TechAudioController
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val categoryIdFlow = MutableStateFlow<Int?>(null)
    private val quickFilterFlow = MutableStateFlow(VocabQuickFilter.ALL)

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val voiceStatus: StateFlow<TtsVoiceStatus> = audioController.voiceStatus

    private val filteredWordsFlow = queryFlow.flatMapLatest { searchWords(it) }

    private val vocabMetaFlow = combine(
        vocabularyRepository.observeCategories(),
        languageRepository.observeAllLanguages(),
        vocabularyRepository.observeDueCount(System.currentTimeMillis()),
        academicVocabularyRepository.observeVerifiedCount()
    ) { categories, languages, dueCount, verifiedCount ->
        VocabMeta(categories, languages, dueCount, verifiedCount)
    }

    val uiState: StateFlow<VocabularyUiState> = combine(
        combine(queryFlow, categoryIdFlow, quickFilterFlow) { q, c, f -> Triple(q, c, f) },
        filteredWordsFlow,
        vocabMetaFlow
    ) { (query, selectedCatId, quickFilter), words, meta ->
        val now = System.currentTimeMillis()
        val filtered = words.filter { word ->
            val matchesCategory = selectedCatId == null || word.categoryId == selectedCatId
            val matchesQuick = when (quickFilter) {
                VocabQuickFilter.ALL -> true
                VocabQuickFilter.DUE -> word.isDue(now)
                VocabQuickFilter.FAVORITES -> word.isFavorite
                VocabQuickFilter.CUSTOM -> word.isCustom
            }
            matchesCategory && matchesQuick
        }
        VocabularyUiState(
            isLoading = false,
            query = query,
            selectedCategoryId = selectedCatId,
            quickFilter = quickFilter,
            categories = meta.categories.toImmutableList(),
            languages = meta.languages.toImmutableList(),
            words = filtered.toImmutableList(),
            dueCount = meta.dueCount,
            academicTermCount = if (meta.verifiedCount > 0) meta.verifiedCount else DEFAULT_ACADEMIC_TERMS_COUNT
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = VocabularyUiState(isLoading = true)
    )

    private data class VocabMeta(
        val categories: List<Category>,
        val languages: List<ProgrammingLanguage>,
        val dueCount: Int,
        val verifiedCount: Int
    )

    fun onQueryChange(query: String) {
        queryFlow.value = query
    }

    fun onCategorySelect(categoryId: Int?) {
        categoryIdFlow.value = if (categoryIdFlow.value == categoryId) null else categoryId
    }

    fun onQuickFilterSelect(filter: VocabQuickFilter) {
        quickFilterFlow.value = filter
    }

    fun onToggleFavorite(word: Word) {
        viewModelScope.launch {
            toggleFavorite(word.id, !word.isFavorite)
        }
    }

    fun onSpeak(text: String) {
        audioController.speakQuick(text)
    }

    fun onSaveCustomWord(
        editingWord: Word?,
        term: String,
        transcription: String,
        translation: String,
        example: String,
        explanationRu: String,
        categoryId: Int?,
        languageId: Int?
    ) {
        viewModelScope.launch {
            val result = if (editingWord == null) {
                addCustomWord(
                    term = term,
                    translation = translation,
                    transcription = transcription,
                    example = example,
                    explanationRu = explanationRu,
                    categoryId = categoryId,
                    languageId = languageId
                ).map { Unit }
            } else {
                updateCustomWord(
                    editingWord.copy(
                        term = term,
                        transcription = transcription,
                        translation = translation,
                        example = example,
                        explanationRu = explanationRu,
                        categoryId = categoryId ?: editingWord.categoryId,
                        languageId = languageId
                    )
                )
            }

            result
                .onSuccess {
                    _events.emit(
                        if (editingWord == null) "Термин «${term.trim()}» добавлен в словарь"
                        else "Термин «${term.trim()}» обновлён"
                    )
                }
                .onFailure {
                    _events.emit(it.message ?: "Ошибка сохранения слова")
                }
        }
    }

    fun onDeleteCustomWord(word: Word) {
        viewModelScope.launch {
            val deleted = deleteCustomWord(word.id)
            if (deleted) {
                _events.emit("Термин «${word.term}» удалён")
            } else {
                _events.emit("Нельзя удалить встроенный системный термин")
            }
        }
    }
}
