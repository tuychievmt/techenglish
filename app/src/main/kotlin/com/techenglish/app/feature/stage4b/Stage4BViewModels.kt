package com.techenglish.app.feature.stage4b

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.database.dao.SquadCacheDao
import com.techenglish.app.core.database.entity.SquadCacheEntity
import com.techenglish.app.core.network.AiCodeReviewRequestDto
import com.techenglish.app.core.network.AiInterviewRequestDto
import com.techenglish.app.core.network.SquadDto
import com.techenglish.app.core.network.SquadMemberDto
import com.techenglish.app.core.network.SubmitDictionaryPrBodyDto
import com.techenglish.app.core.network.TechEnglishApiClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.domain.model.AiInterviewLevel
import com.techenglish.domain.model.AiInterviewRole
import com.techenglish.domain.model.AiInterviewTurn
import com.techenglish.domain.model.AiPrReviewFeedback
import com.techenglish.domain.model.CrowdsourceEligibility
import com.techenglish.domain.model.DictionaryPrItem
import com.techenglish.domain.model.DictionaryPrStatus
import com.techenglish.domain.model.SquadInfo
import com.techenglish.domain.model.SquadMemberInfo
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.repository.VocabularyRepository
import com.techenglish.domain.usecase.CheckCrowdsourceEligibilityUseCase
import com.techenglish.domain.usecase.GetDailyProgressUseCase
import com.techenglish.domain.usecase.SanitizeAiPromptPiiUseCase
import com.techenglish.domain.usecase.UpdateSettingsUseCase
import com.techenglish.domain.usecase.ValidateDictionaryPrUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// =========================================================================
// 1. AI Mock Interviewer & AI Code/PR Reviewer ViewModel (ФТ-8.1–8.3)
// =========================================================================

data class AiMentorUiState(
    val consentAccepted: Boolean = false,
    val selectedTabIndex: Int = 0, // 0 = Mock Interviewer, 1 = Code/PR Reviewer
    val role: AiInterviewRole = AiInterviewRole.BACKEND,
    val level: AiInterviewLevel = AiInterviewLevel.MIDDLE,
    val currentQuestionEn: String = "How do you ensure idempotency in a distributed payment API during network retries?",
    val interviewTurns: List<AiInterviewTurn> = emptyList(),
    val prReviewFeedback: AiPrReviewFeedback? = null,
    val remainingDailyQuota: Int = 15,
    val dailyLimit: Int = 15,
    val isOffline: Boolean = false,
    val isBusy: Boolean = false
)

@HiltViewModel
class AiMentorViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val updateSettings: UpdateSettingsUseCase,
    private val apiClient: TechEnglishApiClient,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val sanitizePii: SanitizeAiPromptPiiUseCase
) : ViewModel() {

    private val internalState = MutableStateFlow(AiMentorUiState(isOffline = !apiClient.isNetworkAvailable()))
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<AiMentorUiState> = combine(
        internalState,
        settingsRepository.observeSettings()
    ) { local, settings ->
        local.copy(
            consentAccepted = settings.aiConsentAccepted,
            isOffline = !apiClient.isNetworkAvailable()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AiMentorUiState(isOffline = !apiClient.isNetworkAvailable())
    )

    init {
        refreshQuota()
    }

    fun acceptAiConsent() {
        viewModelScope.launch {
            updateSettings.setAiConsentAccepted(true)
            refreshQuota()
            _events.emit("Согласие сохранено. Текст очищается от PII, аудио не сохраняется.")
        }
    }

    fun selectTab(index: Int) {
        internalState.value = internalState.value.copy(selectedTabIndex = index)
    }

    fun selectRole(role: AiInterviewRole) {
        val starterQ = when (role) {
            AiInterviewRole.BACKEND -> "How do you ensure idempotency in a distributed payment API during network retries?"
            AiInterviewRole.ANDROID -> "How do structured concurrency and StateFlow prevent memory leaks across configuration changes?"
            AiInterviewRole.SYSTEM_DESIGN -> "How would you design a globally distributed rate limiter with low latency?"
            AiInterviewRole.DEVOPS_SRE -> "How do you structure a blameless postmortem after a production database outage?"
            AiInterviewRole.ML_AI -> "How do you detect and mitigate data drift and concept drift in production ML pipelines?"
        }
        internalState.value = internalState.value.copy(role = role, currentQuestionEn = starterQ)
    }

    fun selectLevel(level: AiInterviewLevel) {
        internalState.value = internalState.value.copy(level = level)
    }

    fun refreshQuota() {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) return@launch
            runCatching { apiClient.fetchAiQuota() }.onSuccess { q ->
                internalState.value = internalState.value.copy(
                    dailyLimit = q.dailyLimit,
                    remainingDailyQuota = q.remainingToday
                )
            }
        }
    }

    fun submitInterviewAnswer(rawAnswer: String) {
        viewModelScope.launch {
            val state = uiState.value
            if (!state.consentAccepted) {
                _events.emit("Сначала подтвердите согласие на отправку обезличенного текста на AI-прокси.")
                return@launch
            }
            val cleanText = sanitizePii(rawAnswer)
            if (cleanText.length < 5) {
                _events.emit("Введите ответ минимум из 5 символов (текстом или голосом).")
                return@launch
            }
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: AI-собеседование работает через серверный прокси при наличии сети.")
                return@launch
            }
            if (tokenStore.getAccessToken().isNullOrBlank()) {
                _events.emit("Войдите в облачный аккаунт (Профиль → Аккаунт), чтобы использовать AI-прокси.")
                return@launch
            }
            internalState.value = internalState.value.copy(isBusy = true)
            runCatching {
                val res = apiClient.submitAiInterview(
                    AiInterviewRequestDto(
                        role = state.role.apiKey,
                        level = state.level.apiKey,
                        questionEn = state.currentQuestionEn,
                        userAnswerText = cleanText,
                        turnIndex = state.interviewTurns.size,
                        consentAccepted = state.consentAccepted
                    )
                )
                val turn = AiInterviewTurn(
                    id = UUID.randomUUID().toString(),
                    questionEn = res.questionEn.ifBlank { state.currentQuestionEn },
                    userAnswerEn = cleanText,
                    englishScore = res.englishScore,
                    technicalScore = res.technicalScore,
                    feedbackRu = res.feedbackRu,
                    improvedAnswerEn = res.improvedAnswerEn,
                    nextQuestionEn = res.nextQuestionEn,
                    isFallback = res.isFallback == true,
                    fallbackReason = res.fallbackReason
                )
                internalState.value = internalState.value.copy(
                    interviewTurns = listOf(turn) + state.interviewTurns,
                    currentQuestionEn = res.nextQuestionEn.ifBlank { state.currentQuestionEn },
                    remainingDailyQuota = res.remainingDailyQuota
                )
            }.onFailure { err ->
                _events.emit("AI-прокси недоступен или исчерпан дневной лимит: ${err.localizedMessage ?: "ошибка"}")
            }
            internalState.value = internalState.value.copy(isBusy = false)
        }
    }

    fun submitPrReview(rawDraft: String, context: String = "pull_request") {
        viewModelScope.launch {
            val state = uiState.value
            if (!state.consentAccepted) {
                _events.emit("Сначала подтвердите согласие на отправку обезличенного текста на AI-прокси.")
                return@launch
            }
            val cleanText = sanitizePii(rawDraft)
            if (cleanText.length < 5) {
                _events.emit("Введите текст PR-комментария или коммита (минимум 5 символов).")
                return@launch
            }
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: AI Code/PR Reviewer требует подключения к серверному прокси.")
                return@launch
            }
            if (tokenStore.getAccessToken().isNullOrBlank()) {
                _events.emit("Войдите в облачный аккаунт, чтобы отправлять запросы к AI Code Reviewer.")
                return@launch
            }
            internalState.value = internalState.value.copy(isBusy = true)
            runCatching {
                val res = apiClient.submitAiCodeReview(
                    AiCodeReviewRequestDto(
                        draftText = cleanText,
                        context = context,
                        consentAccepted = state.consentAccepted
                    )
                )
                internalState.value = internalState.value.copy(
                    prReviewFeedback = AiPrReviewFeedback(
                        toneAssessmentRu = res.toneAssessmentRu,
                        improvedEnglishText = res.improvedEnglishText,
                        keySuggestionsRu = res.keySuggestionsRu,
                        remainingDailyQuota = res.remainingDailyQuota,
                        isFallback = res.isFallback == true,
                        fallbackReason = res.fallbackReason
                    ),
                    remainingDailyQuota = res.remainingDailyQuota
                )
            }.onFailure { err ->
                _events.emit("Ошибка AI Code Reviewer: ${err.localizedMessage ?: "проверьте лимит/сеть"}")
            }
            internalState.value = internalState.value.copy(isBusy = false)
        }
    }
}

// =========================================================================
// 2. Squads ViewModel (ФТ-14.4: 5–10 devs, Weekly Goal, Non-Ranked Bonus)
// =========================================================================

data class SquadsUiState(
    val squad: SquadInfo? = null,
    val isOffline: Boolean = false,
    val isBusy: Boolean = false
)

@HiltViewModel
class SquadsViewModel @Inject constructor(
    private val apiClient: TechEnglishApiClient,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val squadCacheDao: SquadCacheDao
) : ViewModel() {

    private val squadJson = Json { ignoreUnknownKeys = true }
    private val _uiState = MutableStateFlow(SquadsUiState(isOffline = !apiClient.isNetworkAvailable()))
    val uiState: StateFlow<SquadsUiState> = _uiState
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            loadCachedSquad()
            refresh()
        }
    }

    internal suspend fun loadCachedSquad() {
        val cached = squadCacheDao.getSquad()
        if (cached != null) {
            _uiState.value = _uiState.value.copy(squad = cached.toDomain())
        }
    }

    private suspend fun persistSquadToCache(dto: SquadDto?) {
        if (dto == null) {
            squadCacheDao.clearSquad()
            return
        }
        squadCacheDao.clearSquad()
        squadCacheDao.upsertSquad(
            SquadCacheEntity(
                id = dto.id,
                name = dto.name,
                inviteCode = dto.inviteCode,
                membersJson = squadJson.encodeToString(dto.members),
                weeklyGoalXp = dto.weeklyGoalXp,
                currentWeekXp = dto.currentWeekXp,
                nextWeekBonusMultiplier = dto.nextWeekBonusMultiplier,
                isActiveForChallenge = dto.isActiveForChallenge,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun refresh() {
        viewModelScope.launch {
            val offline = !apiClient.isNetworkAvailable()
            _uiState.value = _uiState.value.copy(isOffline = offline)
            loadCachedSquad()
            if (offline || tokenStore.getAccessToken().isNullOrBlank()) return@launch
            _uiState.value = _uiState.value.copy(isBusy = true)
            runCatching { apiClient.fetchMySquad() }
                .onSuccess { res ->
                    persistSquadToCache(res.squad)
                    _uiState.value = _uiState.value.copy(squad = res.squad?.toDomain())
                }
            _uiState.value = _uiState.value.copy(isBusy = false)
        }
    }

    fun createSquad(name: String, weeklyGoalXp: Int = 1000) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) {
                _events.emit("Офлайн-режим: создание отряда требует подключения к серверу.")
                return@launch
            }
            if (tokenStore.getAccessToken().isNullOrBlank()) {
                _events.emit("Войдите в облачный аккаунт, чтобы создать Squad.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isBusy = true)
            runCatching { apiClient.createSquad(name.trim(), weeklyGoalXp) }
                .onSuccess { dto ->
                    persistSquadToCache(dto)
                    _uiState.value = _uiState.value.copy(squad = dto.toDomain())
                    _events.emit("Отряд «${dto.name}» создан! Код приглашения: ${dto.inviteCode}")
                }
                .onFailure { err -> _events.emit("Не удалось создать отряд: ${err.localizedMessage ?: "ошибка"}") }
            _uiState.value = _uiState.value.copy(isBusy = false)
        }
    }

    fun joinSquad(inviteCode: String) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) {
                _events.emit("Требуется подключение к сети и вход в облачный аккаунт.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isBusy = true)
            runCatching { apiClient.joinSquad(inviteCode.trim()) }
                .onSuccess { dto ->
                    persistSquadToCache(dto)
                    _uiState.value = _uiState.value.copy(squad = dto.toDomain())
                    _events.emit("Вы присоединились к отряду «${dto.name}»!")
                }
                .onFailure { err -> _events.emit("Не удалось вступить: ${err.localizedMessage ?: "проверьте код"}") }
            _uiState.value = _uiState.value.copy(isBusy = false)
        }
    }

    fun leaveSquad() {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) return@launch
            _uiState.value = _uiState.value.copy(isBusy = true)
            runCatching { apiClient.leaveSquad() }
                .onSuccess {
                    persistSquadToCache(null)
                    _uiState.value = _uiState.value.copy(squad = null)
                    _events.emit("Вы покинули отряд.")
                }
                .onFailure { err -> _events.emit("Ошибка выхода: ${err.localizedMessage ?: "повторите"}") }
            _uiState.value = _uiState.value.copy(isBusy = false)
        }
    }

    private fun SquadCacheEntity.toDomain(): SquadInfo {
        val parsedMembers = runCatching {
            squadJson.decodeFromString<List<SquadMemberDto>>(membersJson)
        }.getOrDefault(emptyList())
        return SquadInfo(
            id = id,
            name = name,
            inviteCode = inviteCode,
            members = parsedMembers.map { SquadMemberInfo(it.userId, it.username, it.rankTitle, it.weeklyXp) },
            weeklyGoalXp = weeklyGoalXp,
            currentWeekXp = currentWeekXp,
            nextWeekBonusMultiplier = nextWeekBonusMultiplier,
            isActiveForChallenge = isActiveForChallenge
        )
    }

    private fun SquadDto.toDomain() = SquadInfo(
        id = id,
        name = name,
        inviteCode = inviteCode,
        members = members.map { SquadMemberInfo(it.userId, it.username, it.rankTitle, it.weeklyXp) },
        weeklyGoalXp = weeklyGoalXp,
        currentWeekXp = currentWeekXp,
        nextWeekBonusMultiplier = nextWeekBonusMultiplier,
        isActiveForChallenge = isActiveForChallenge
    )
}

// =========================================================================
// 3. Dictionary Crowdsourcing «PR в словарь» ViewModel (ФТ-14.3)
// =========================================================================

data class CrowdsourcePrUiState(
    val eligibility: CrowdsourceEligibility = CrowdsourceEligibility(
        isEligible = false,
        meetsRankMiddlePlus = false,
        meetsAccountAge14Days = false,
        meetsLearnedTerms100 = false,
        meetsZeroConfirmedReports = true,
        unmetReasonsRu = emptyList()
    ),
    val prs: List<DictionaryPrItem> = emptyList(),
    val isOffline: Boolean = false,
    val isBusy: Boolean = false
)

@HiltViewModel
class CrowdsourcePrViewModel @Inject constructor(
    getDailyProgress: GetDailyProgressUseCase,
    vocabularyRepository: VocabularyRepository,
    academicVocabularyRepository: AcademicVocabularyRepository,
    private val apiClient: TechEnglishApiClient,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val checkEligibility: CheckCrowdsourceEligibilityUseCase,
    private val validatePr: ValidateDictionaryPrUseCase
) : ViewModel() {

    private val prsFlow = MutableStateFlow<List<DictionaryPrItem>>(emptyList())
    private val serverEligibilityFlow = MutableStateFlow<CrowdsourceEligibility?>(null)
    private val isBusyFlow = MutableStateFlow(false)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<CrowdsourcePrUiState> = combine(
        getDailyProgress(),
        vocabularyRepository.observeLearnedWordCount(),
        academicVocabularyRepository.observeStudiedCount(),
        combine(prsFlow, serverEligibilityFlow, isBusyFlow) { prs, srvElig, busy -> Triple(prs, srvElig, busy) }
    ) { daily, learnedBasic, learnedAcademic, extra ->
        val (prs, srvElig, busy) = extra
        val totalLearned = learnedBasic + learnedAcademic
        val elig = srvElig ?: checkEligibility(
            totalXp = daily.levelProgress.totalXp,
            accountAgeDays = 0,
            learnedTermsCount = totalLearned,
            confirmedReportsCount = 0
        )
        CrowdsourcePrUiState(
            eligibility = elig,
            prs = prs,
            isOffline = !apiClient.isNetworkAvailable(),
            isBusy = busy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CrowdsourcePrUiState(isOffline = !apiClient.isNetworkAvailable())
    )

    init {
        refreshPrs()
    }

    fun refreshPrs() {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) return@launch
            isBusyFlow.value = true
            runCatching { apiClient.fetchCrowdsourceEligibility() }.onSuccess { dto ->
                serverEligibilityFlow.value = CrowdsourceEligibility(
                    isEligible = dto.isEligible,
                    meetsRankMiddlePlus = dto.meetsRankMiddlePlus,
                    meetsAccountAge14Days = dto.meetsAccountAge14Days,
                    meetsLearnedTerms100 = dto.meetsLearnedTerms100,
                    meetsZeroConfirmedReports = dto.meetsZeroConfirmedReports,
                    unmetReasonsRu = dto.unmetReasonsRu
                )
            }
            runCatching { apiClient.fetchDictionaryPrs() }.onSuccess { res ->
                prsFlow.value = res.items.map { dto ->
                    DictionaryPrItem(
                        id = dto.id,
                        term = dto.term,
                        proposedTranslationRu = dto.proposedTranslationRu,
                        proposedTranslationTg = dto.proposedTranslationTg,
                        proposedDefinitionRu = dto.proposedDefinitionRu,
                        sourceReference = dto.sourceReference,
                        authorUsername = dto.authorUsername,
                        status = DictionaryPrStatus.fromRaw(dto.status),
                        upvotes = dto.upvotes,
                        downvotes = dto.downvotes,
                        licenseAccepted = dto.licenseAccepted,
                        createdAtMillis = dto.createdAtMs
                    )
                }
            }
            isBusyFlow.value = false
        }
    }

    fun submitPr(
        term: String,
        translationRu: String,
        translationTg: String,
        definitionRu: String,
        sourceReference: String,
        licenseAccepted: Boolean
    ) {
        viewModelScope.launch {
            validatePr(term, translationRu, translationTg, sourceReference, licenseAccepted).getOrElse {
                _events.emit(it.message ?: "Проверьте поля PR и согласие с лицензией.")
                return@launch
            }
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) {
                _events.emit("Отправка PR в словарь требует подключения к серверу и входа в аккаунт.")
                return@launch
            }
            isBusyFlow.value = true
            runCatching {
                apiClient.submitDictionaryPr(
                    SubmitDictionaryPrBodyDto(
                        term = term.trim(),
                        proposedTranslationRu = translationRu.trim(),
                        proposedTranslationTg = translationTg.trim(),
                        proposedDefinitionRu = definitionRu.trim(),
                        sourceReference = sourceReference.trim(),
                        licenseAccepted = true
                    )
                )
                refreshPrs()
                _events.emit("PR в словарь отправлен на проверку сообщества (лицензия CC BY-SA 4.0).")
            }.onFailure { err ->
                _events.emit("PR отклонён сервером: ${err.localizedMessage ?: "проверьте критерии допуска ФТ-14.3"}")
            }
            isBusyFlow.value = false
        }
    }

    fun votePr(prId: String, vote: Int) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) {
                _events.emit("Голосование за PR доступно онлайн после входа в аккаунт.")
                return@launch
            }
            runCatching {
                apiClient.voteDictionaryPr(prId, vote)
                refreshPrs()
            }
        }
    }

    fun reportPr(prId: String, reason: String) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) {
                _events.emit("Офлайн-режим: отправка жалобы требует подключения к сети.")
                return@launch
            }
            runCatching { apiClient.reportContent(PR_REPORT_TARGET_TYPE, prId, reason) }
                .onSuccess { _events.emit("Жалоба на PR отправлена в очередь модерации.") }
        }
    }

    companion object {
        const val PR_REPORT_TARGET_TYPE = "DICTIONARY_PR"
    }
}
