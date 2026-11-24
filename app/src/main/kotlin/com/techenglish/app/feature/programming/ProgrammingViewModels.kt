package com.techenglish.app.feature.programming

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.domain.model.CodeSnippet
import com.techenglish.domain.model.KeywordTooltip
import com.techenglish.domain.model.LanguageComparison
import com.techenglish.domain.model.ProgrammingLanguage
import com.techenglish.domain.repository.ProgrammingLanguageRepository
import com.techenglish.domain.usecase.AddWordFromDocUseCase
import com.techenglish.domain.usecase.CompareLanguagesUseCase
import com.techenglish.domain.usecase.GetCodeSnippetUseCase
import com.techenglish.domain.usecase.GetLanguageDetailUseCase
import com.techenglish.domain.usecase.GetProgrammingLanguagesUseCase
import com.techenglish.domain.usecase.LogCodeBreakdownUseCase
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ==================== 1. CATALOG VIEWMODEL ====================

@Immutable
data class LanguagesCatalogUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val selectedDomain: String? = null,
    val languages: ImmutableList<ProgrammingLanguage> = persistentListOf()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LanguagesCatalogViewModel @Inject constructor(
    private val getProgrammingLanguages: GetProgrammingLanguagesUseCase
) : ViewModel() {

    val domains = listOf("Backend", "Frontend", "Mobile", "Systems", "Scripting", "Data/DB")

    private val queryFlow = MutableStateFlow("")
    private val domainFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LanguagesCatalogUiState> = combine(queryFlow, domainFlow) { q, d -> q to d }
        .flatMapLatest { (q, d) ->
            getProgrammingLanguages(domainFilter = d, query = q).combine(MutableStateFlow(q to d)) { list, (query, domain) ->
                LanguagesCatalogUiState(
                    isLoading = false,
                    query = query,
                    selectedDomain = domain,
                    languages = list.toImmutableList()
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = LanguagesCatalogUiState(isLoading = true)
        )

    fun onQueryChange(value: String) { queryFlow.value = value }
    fun onDomainToggle(domain: String) {
        domainFlow.value = if (domainFlow.value == domain) null else domain
    }
}

// ==================== 2. LANGUAGE DETAIL VIEWMODEL ====================

@Immutable
data class LanguageDetailUiState(
    val isLoading: Boolean = true,
    val language: ProgrammingLanguage? = null,
    val snippets: ImmutableList<CodeSnippet> = persistentListOf(),
    val showRussian: Boolean = true
)

@HiltViewModel
class LanguageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getLanguageDetail: GetLanguageDetailUseCase
) : ViewModel() {

    private val languageId: Int = savedStateHandle.get<Int>("languageId") ?: 1
    private val showRussianFlow = MutableStateFlow(true)

    val uiState: StateFlow<LanguageDetailUiState> = combine(
        getLanguageDetail.observeLanguage(languageId),
        getLanguageDetail.observeSnippets(languageId),
        showRussianFlow
    ) { lang, snippets, showRu ->
        LanguageDetailUiState(
            isLoading = false,
            language = lang,
            snippets = snippets.toImmutableList(),
            showRussian = showRu
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LanguageDetailUiState(isLoading = true)
    )

    fun toggleLanguageMode(showRussian: Boolean) {
        showRussianFlow.value = showRussian
    }
}

// ==================== 3. CODE BREAKDOWN VIEWMODEL ====================

data class CodeBreakdownUiState(
    val isLoading: Boolean = true,
    val language: ProgrammingLanguage? = null,
    val snippet: CodeSnippet? = null,
    val selectedLine: Int? = null,
    val selectedKeyword: KeywordTooltip? = null,
    val showRussian: Boolean = true,
    val isLoggedCompleted: Boolean = false
)

@HiltViewModel
class CodeBreakdownViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCodeSnippet: GetCodeSnippetUseCase,
    private val addWordFromDoc: AddWordFromDocUseCase,
    private val logCodeBreakdown: LogCodeBreakdownUseCase,
    val audioController: TechAudioController
) : ViewModel() {

    private val snippetId: Int = savedStateHandle.get<Int>("snippetId") ?: 1

    private val _uiState = MutableStateFlow(CodeBreakdownUiState(isLoading = true))
    val uiState: StateFlow<CodeBreakdownUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val pair = getCodeSnippet(snippetId)
            _uiState.value = CodeBreakdownUiState(
                isLoading = false,
                language = pair?.first,
                snippet = pair?.second,
                selectedLine = pair?.second?.lineExplanations?.firstOrNull()?.line
            )
        }
    }

    fun selectLine(lineNumber: Int) {
        _uiState.value = _uiState.value.copy(selectedLine = lineNumber)
    }

    fun openKeyword(keywordName: String) {
        val kw = _uiState.value.snippet?.keywords?.firstOrNull {
            it.keyword.equals(keywordName, ignoreCase = true)
        } ?: return
        _uiState.value = _uiState.value.copy(selectedKeyword = kw)
    }

    fun dismissKeyword() {
        _uiState.value = _uiState.value.copy(selectedKeyword = null)
    }

    fun setShowRussian(showRu: Boolean) {
        _uiState.value = _uiState.value.copy(showRussian = showRu)
    }

    fun speak(text: String) {
        audioController.speakQuick(text)
    }

    fun addKeywordToVocabulary(tooltip: KeywordTooltip) {
        viewModelScope.launch {
            val langId = _uiState.value.language?.id
            addWordFromDoc(
                term = tooltip.keyword,
                translation = tooltip.ru,
                example = tooltip.example.ifBlank { tooltip.en },
                explanationRu = "${tooltip.ru} (${tooltip.en})",
                languageId = langId
            ).onSuccess {
                _events.emit("Ключевое слово «${tooltip.keyword}» добавлено в ваш словарь SRS")
            }.onFailure {
                _events.emit(it.message ?: "Не удалось добавить слово")
            }
        }
    }

    fun markBreakdownStudied() {
        val sn = _uiState.value.snippet ?: return
        if (_uiState.value.isLoggedCompleted) return
        viewModelScope.launch {
            logCodeBreakdown(sn.id)
            _uiState.value = _uiState.value.copy(isLoggedCompleted = true)
            _events.emit("Разбор кода засчитан (+${LogCodeBreakdownUseCase.XP_CODE_BREAKDOWN} XP)")
        }
    }
}

// ==================== 4. SIDE-BY-SIDE COMPARISON VIEWMODEL ====================

data class LanguageComparisonUiState(
    val isLoading: Boolean = true,
    val allLanguages: List<ProgrammingLanguage> = emptyList(),
    val comparison: LanguageComparison? = null,
    val explanationInRussian: Boolean = true
)

@HiltViewModel
class LanguageComparisonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val languageRepository: ProgrammingLanguageRepository,
    private val compareLanguages: CompareLanguagesUseCase
) : ViewModel() {

    private val initialLangAId: Int = savedStateHandle.get<Int>("langAId")?.takeIf { it > 0 } ?: 1 // Python
    private val initialLangBId: Int = savedStateHandle.get<Int>("langBId")?.takeIf { it > 0 } ?: 8 // Go

    private val _uiState = MutableStateFlow(LanguageComparisonUiState(isLoading = true))
    val uiState: StateFlow<LanguageComparisonUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val all = languageRepository.getAllLanguages()
            val langA = all.firstOrNull { it.id == initialLangAId }?.id ?: all.firstOrNull()?.id ?: 1
            val langB = all.firstOrNull { it.id == initialLangBId && it.id != langA }?.id
                ?: all.firstOrNull { it.id != langA }?.id ?: langA
            val comparison = compareLanguages(langA, langB)
            _uiState.value = LanguageComparisonUiState(
                isLoading = false,
                allLanguages = all,
                comparison = comparison
            )
        }
    }

    fun selectLanguageA(langId: Int) {
        val currentB = _uiState.value.comparison?.languageB?.id ?: return
        val currentTask = _uiState.value.comparison?.selectedTaskKey
        refreshComparison(langId, currentB, currentTask)
    }

    fun selectLanguageB(langId: Int) {
        val currentA = _uiState.value.comparison?.languageA?.id ?: return
        val currentTask = _uiState.value.comparison?.selectedTaskKey
        refreshComparison(currentA, langId, currentTask)
    }

    fun swapLanguages() {
        val comp = _uiState.value.comparison ?: return
        refreshComparison(comp.languageB.id, comp.languageA.id, comp.selectedTaskKey)
    }

    fun selectTask(taskKey: String) {
        val comp = _uiState.value.comparison ?: return
        refreshComparison(comp.languageA.id, comp.languageB.id, taskKey)
    }

    fun setExplanationLanguage(inRussian: Boolean) {
        _uiState.value = _uiState.value.copy(explanationInRussian = inRussian)
    }

    private fun refreshComparison(langAId: Int, langBId: Int, taskKey: String?) {
        viewModelScope.launch {
            val updated = compareLanguages(langAId, langBId, taskKey)
            _uiState.value = _uiState.value.copy(comparison = updated)
        }
    }
}
