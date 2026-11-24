package com.techenglish.app.feature.stage4a

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.database.dao.CommentsCacheDao
import com.techenglish.app.core.database.dao.CommunityDecksDao
import com.techenglish.app.core.database.entity.CommentCacheEntity
import com.techenglish.app.core.database.entity.CommunityDeckEntity
import com.techenglish.app.core.database.entity.CommunityDeckTermEntity
import com.techenglish.app.core.network.CommunityDeckTermDto
import com.techenglish.app.core.network.PublishDeckBodyDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.domain.model.CommentSortOrder
import com.techenglish.domain.model.CommentTargetType
import com.techenglish.domain.model.CommunityComment
import com.techenglish.domain.model.CommunityDeck
import com.techenglish.domain.model.CommunityDeckTerm
import com.techenglish.domain.model.Word
import com.techenglish.domain.repository.VocabularyRepository
import com.techenglish.domain.usecase.PrepareDeckForLocalImportUseCase
import com.techenglish.domain.usecase.SortAndFilterCommentsUseCase
import com.techenglish.domain.usecase.ValidateUgcContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// =========================================================================
// 1. Comments & Mnemonics ViewModel (ФТ-13.1 + ФТ-9.4 UGC Moderation)
// =========================================================================

data class CommentsMnemonicsUiState(
    val targetType: CommentTargetType = CommentTargetType.TERM,
    val targetId: Int = 0,
    val targetTitle: String = "",
    val sortOrder: CommentSortOrder = CommentSortOrder.TOP_VOTED,
    val comments: List<CommunityComment> = emptyList(),
    val isOffline: Boolean = false,
    val isBusy: Boolean = false
)

@HiltViewModel
class CommentsMnemonicsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val commentsCacheDao: CommentsCacheDao,
    private val apiClient: TechEnglishApiClient,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val validateUgc: ValidateUgcContentUseCase,
    private val sortComments: SortAndFilterCommentsUseCase
) : ViewModel() {

    private val targetType = CommentTargetType.fromRaw(savedStateHandle.get<String>("targetType") ?: "TERM")
    private val targetId = savedStateHandle.get<Int>("targetId") ?: 0
    private val targetTitle = savedStateHandle.get<String>("targetTitle").orEmpty().ifBlank {
        if (targetType == CommentTargetType.TERM) "Термин #$targetId" else "Сниппет #$targetId"
    }

    private val sortOrderFlow = MutableStateFlow(CommentSortOrder.TOP_VOTED)
    private val blockedAuthorsFlow = MutableStateFlow<Set<String>>(emptySet())
    private val isBusyFlow = MutableStateFlow(false)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<CommentsMnemonicsUiState> = combine(
        commentsCacheDao.observeCommentsForTarget(targetType.apiKey, targetId),
        sortOrderFlow,
        blockedAuthorsFlow,
        isBusyFlow
    ) { rows, sortOrder, blocked, busy ->
        val domainList = rows.map { it.toDomain() }
        val sorted = sortComments(domainList, blockedUserIds = blocked, sortOrder = sortOrder)
        CommentsMnemonicsUiState(
            targetType = targetType,
            targetId = targetId,
            targetTitle = targetTitle,
            sortOrder = sortOrder,
            comments = sorted,
            isOffline = !apiClient.isNetworkAvailable(),
            isBusy = busy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CommentsMnemonicsUiState(
            targetType = targetType,
            targetId = targetId,
            targetTitle = targetTitle,
            isOffline = !apiClient.isNetworkAvailable()
        )
    )

    init {
        refresh()
    }

    fun setSortOrder(order: CommentSortOrder) {
        sortOrderFlow.value = order
    }

    fun refresh() {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) return@launch
            val token = tokenStore.getAccessToken()
            if (token.isNullOrBlank()) return@launch
            isBusyFlow.value = true
            runCatching {
                val res = apiClient.fetchComments(targetType.apiKey, targetId, sortOrderFlow.value.apiKey)
                commentsCacheDao.clearForTarget(targetType.apiKey, targetId)
                commentsCacheDao.upsertComments(
                    res.items.map { dto ->
                        CommentCacheEntity(
                            id = dto.id,
                            targetType = dto.targetType,
                            targetId = dto.targetId,
                            authorUserId = dto.authorUserId,
                            authorUsername = dto.authorUsername,
                            authorRankTitle = dto.authorRankTitle,
                            body = dto.body,
                            upvotes = dto.upvotes,
                            downvotes = dto.downvotes,
                            userVote = dto.userVote,
                            isHidden = false,
                            createdAt = dto.createdAtMs
                        )
                    }
                )
            }
            isBusyFlow.value = false
        }
    }

    fun postComment(rawBody: String) {
        viewModelScope.launch {
            val cleanBody = validateUgc.validateCommentBody(rawBody).getOrElse {
                _events.emit(it.message ?: "Комментарий не прошёл модерацию.")
                return@launch
            }
            isBusyFlow.value = true
            val session = tokenStore.sessionFlow.first()
            val now = System.currentTimeMillis()
            val canSyncRemote = apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()

            if (canSyncRemote) {
                runCatching {
                    val created = apiClient.postComment(targetType.apiKey, targetId, cleanBody)
                    commentsCacheDao.upsertComment(
                        CommentCacheEntity(
                            id = created.id,
                            targetType = created.targetType,
                            targetId = created.targetId,
                            authorUserId = created.authorUserId,
                            authorUsername = created.authorUsername,
                            authorRankTitle = created.authorRankTitle,
                            body = created.body,
                            upvotes = created.upvotes,
                            downvotes = created.downvotes,
                            userVote = created.userVote,
                            isHidden = false,
                            createdAt = created.createdAtMs
                        )
                    )
                    _events.emit("Комментарий опубликован в сообществе!")
                }.onFailure {
                    saveLocalComment(session.userId, session.username, cleanBody, now)
                    _events.emit("Сохранено локально в кэше комментариев.")
                }
            } else {
                saveLocalComment(session.userId, session.username, cleanBody, now)
                _events.emit("Офлайн/гостевой режим: мнемоника сохранена локально.")
            }
            isBusyFlow.value = false
        }
    }

    private suspend fun saveLocalComment(userId: String, username: String, body: String, now: Long) {
        commentsCacheDao.upsertComment(
            CommentCacheEntity(
                id = "local_${UUID.randomUUID()}",
                targetType = targetType.apiKey,
                targetId = targetId,
                authorUserId = userId,
                authorUsername = username,
                authorRankTitle = "Developer",
                body = body,
                upvotes = 1,
                downvotes = 0,
                userVote = 1,
                isHidden = false,
                createdAt = now
            )
        )
    }

    fun voteComment(comment: CommunityComment, vote: Int) {
        viewModelScope.launch {
            val updated = sortComments.applyVoteToggle(comment, vote)
            commentsCacheDao.upsertComment(updated.toEntity())
            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank() && !comment.id.startsWith("local_")) {
                runCatching { apiClient.voteComment(comment.id, updated.userVote) }
            }
        }
    }

    fun reportAndBlockComment(comment: CommunityComment, reason: String, alsoBlockAuthor: Boolean) {
        viewModelScope.launch {
            commentsCacheDao.hideCommentLocally(comment.id)
            if (alsoBlockAuthor) {
                blockedAuthorsFlow.value = blockedAuthorsFlow.value + comment.authorUserId
                commentsCacheDao.hideCommentsByAuthor(comment.authorUserId)
            }
            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching {
                    apiClient.reportContent("COMMENT", comment.id, reason)
                    if (alsoBlockAuthor) {
                        apiClient.blockUser(comment.authorUserId)
                    }
                }
            }
            _events.emit(
                if (alsoBlockAuthor) "Жалоба отправлена, автор @${comment.authorUsername} заблокирован."
                else "Жалоба отправлена на модерацию, комментарий скрыт."
            )
        }
    }

    private fun CommentCacheEntity.toDomain() = CommunityComment(
        id = id,
        targetType = CommentTargetType.fromRaw(targetType),
        targetId = targetId,
        authorUserId = authorUserId,
        authorUsername = authorUsername,
        authorRankTitle = authorRankTitle,
        body = body,
        upvotes = upvotes,
        downvotes = downvotes,
        userVote = userVote,
        isHidden = isHidden,
        createdAtMillis = createdAt
    )

    private fun CommunityComment.toEntity() = CommentCacheEntity(
        id = id,
        targetType = targetType.apiKey,
        targetId = targetId,
        authorUserId = authorUserId,
        authorUsername = authorUsername,
        authorRankTitle = authorRankTitle,
        body = body,
        upvotes = upvotes,
        downvotes = downvotes,
        userVote = userVote,
        isHidden = isHidden,
        createdAt = createdAtMillis
    )
}

// =========================================================================
// 2. Community Decks ViewModel (ФТ-13.2 + ФТ-9.4 UGC Moderation)
// =========================================================================

data class CommunityDecksUiState(
    val decks: List<CommunityDeck> = emptyList(),
    val selectedTag: String = "",
    val searchQuery: String = "",
    val myCustomWordsCount: Int = 0,
    val isOffline: Boolean = false,
    val isBusy: Boolean = false
)

@HiltViewModel
class CommunityDecksViewModel @Inject constructor(
    private val communityDecksDao: CommunityDecksDao,
    private val vocabularyRepository: VocabularyRepository,
    private val apiClient: TechEnglishApiClient,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val validateUgc: ValidateUgcContentUseCase,
    private val prepareImport: PrepareDeckForLocalImportUseCase
) : ViewModel() {

    private val selectedTagFlow = MutableStateFlow("")
    private val searchQueryFlow = MutableStateFlow("")
    private val isBusyFlow = MutableStateFlow(false)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<CommunityDecksUiState> = combine(
        communityDecksDao.observeVisibleDecks(),
        vocabularyRepository.observeAllWords(),
        selectedTagFlow,
        searchQueryFlow,
        isBusyFlow
    ) { deckEntities, allWords, tag, query, busy ->
        val customCount = allWords.count { it.isCustom }
        val filtered = deckEntities.map { e ->
            CommunityDeck(
                id = e.id,
                title = e.title,
                description = e.description,
                tags = e.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                authorUserId = e.authorUserId,
                authorUsername = e.authorUsername,
                rating = e.rating,
                downloadsCount = e.downloadsCount,
                termCount = e.termCount,
                isDownloadedLocally = e.isDownloadedLocally,
                isHidden = e.isHidden,
                createdAtMillis = e.createdAt
            )
        }.filter { deck ->
            val matchesTag = tag.isBlank() || deck.tags.any { it.equals(tag, ignoreCase = true) }
            val matchesQuery = query.isBlank() ||
                deck.title.contains(query, ignoreCase = true) ||
                deck.description.contains(query, ignoreCase = true) ||
                deck.tags.any { it.contains(query, ignoreCase = true) }
            matchesTag && matchesQuery
        }

        CommunityDecksUiState(
            decks = filtered,
            selectedTag = tag,
            searchQuery = query,
            myCustomWordsCount = customCount,
            isOffline = !apiClient.isNetworkAvailable(),
            isBusy = busy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CommunityDecksUiState(isOffline = !apiClient.isNetworkAvailable())
    )

    init {
        viewModelScope.launch {
            seedStarterDecksIfEmpty()
            refreshFromServer()
        }
    }

    fun setTagFilter(tag: String) {
        selectedTagFlow.value = if (selectedTagFlow.value == tag) "" else tag
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun refreshFromServer() {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable()) return@launch
            if (tokenStore.getAccessToken().isNullOrBlank()) return@launch
            isBusyFlow.value = true
            runCatching {
                val res = apiClient.fetchCommunityDecks()
                val existingIds = communityDecksDao.observeVisibleDecks().first()
                    .filter { it.isDownloadedLocally }
                    .map { it.id }
                    .toSet()
                communityDecksDao.upsertDecks(
                    res.decks.map { dto ->
                        CommunityDeckEntity(
                            id = dto.id,
                            title = dto.title,
                            description = dto.description,
                            tagsCsv = dto.tags.joinToString(","),
                            authorUserId = dto.authorUserId,
                            authorUsername = dto.authorUsername,
                            rating = dto.rating,
                            downloadsCount = dto.downloadsCount,
                            termCount = dto.termCount,
                            isDownloadedLocally = dto.id in existingIds,
                            isHidden = false,
                            createdAt = dto.createdAtMs
                        )
                    }
                )
            }
            isBusyFlow.value = false
        }
    }

    /**
     * ФТ-13.2 — 1-tap download of a Community Deck into local Room «Мои слова» for offline SM-2 study.
     */
    fun downloadDeckToRoom(deck: CommunityDeck) {
        viewModelScope.launch {
            isBusyFlow.value = true
            runCatching {
                var terms = communityDecksDao.getTermsForDeck(deck.id).map {
                    CommunityDeckTerm(
                        term = it.term,
                        transcription = it.transcription,
                        translation = it.translation,
                        example = it.example,
                        explanationRu = it.explanationRu
                    )
                }

                if (terms.isEmpty() && apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                    val downloaded = apiClient.downloadCommunityDeck(deck.id)
                    terms = downloaded.terms.map {
                        CommunityDeckTerm(it.term, it.transcription, it.translation, it.example, it.explanationRu)
                    }
                    communityDecksDao.clearTermsForDeck(deck.id)
                    communityDecksDao.insertDeckTerms(
                        terms.map {
                            CommunityDeckTermEntity(
                                deckId = deck.id,
                                term = it.term,
                                transcription = it.transcription,
                                translation = it.translation,
                                example = it.example,
                                explanationRu = it.explanationRu
                            )
                        }
                    )
                }

                val myWordsCategoryId = vocabularyRepository.getSystemCategoryId()

                val existingTerms = vocabularyRepository
                    .observeAllWords()
                    .first()
                    .map { it.term.trim().lowercase() }
                    .toSet()

                val plan = prepareImport(
                    deckTerms = terms,
                    existingNormalizedTerms = existingTerms,
                    targetCategoryId = myWordsCategoryId
                )
                plan.wordsToInsert.forEach { w: Word ->
                    vocabularyRepository.insertWord(w)
                }
                communityDecksDao.markDownloadedLocally(deck.id)
                _events.emit(
                    "Колода «${deck.title}» сохранена в Room: добавлено ${plan.wordsToInsert.size} слов (пропущено дублей: ${plan.skippedDuplicatesCount})."
                )
            }.onFailure { err ->
                _events.emit("Ошибка скачивания колоды: ${err.localizedMessage ?: "повторите попытку"}")
            }
            isBusyFlow.value = false
        }
    }

    /**
     * ФТ-13.2 — Publish user's custom words from «Мои слова» as a Community Deck.
     */
    fun publishFromMyWords(rawTitle: String, rawDescription: String, rawTagsCsv: String) {
        viewModelScope.launch {
            val customWords = vocabularyRepository
                .observeAllWords()
                .first()
                .filter { it.isCustom }

            val deckTerms = customWords.map {
                CommunityDeckTerm(
                    term = it.term,
                    transcription = it.transcription,
                    translation = it.translation,
                    example = it.example,
                    explanationRu = it.explanationRu
                )
            }
            val rawTags = rawTagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val validated = validateUgc.validateDeckPublish(rawTitle, rawDescription, rawTags, deckTerms).getOrElse {
                _events.emit(it.message ?: "Проверьте название и состав колоды.")
                return@launch
            }
            val (title, desc, tags) = validated
            isBusyFlow.value = true

            val session = tokenStore.sessionFlow.first()
            val now = System.currentTimeMillis()
            var deckId = "local_deck_${UUID.randomUUID()}"

            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching {
                    val created = apiClient.publishCommunityDeck(
                        PublishDeckBodyDto(
                            title = title,
                            description = desc,
                            tags = tags,
                            terms = deckTerms.map {
                                CommunityDeckTermDto(it.term, it.transcription, it.translation, it.example, it.explanationRu)
                            }
                        )
                    )
                    deckId = created.id
                }
            }

            communityDecksDao.upsertDeck(
                CommunityDeckEntity(
                    id = deckId,
                    title = title,
                    description = desc,
                    tagsCsv = tags.joinToString(","),
                    authorUserId = session.userId,
                    authorUsername = session.username,
                    rating = 5.0f,
                    downloadsCount = 1,
                    termCount = deckTerms.size,
                    isDownloadedLocally = true,
                    isHidden = false,
                    createdAt = now
                )
            )
            communityDecksDao.clearTermsForDeck(deckId)
            communityDecksDao.insertDeckTerms(
                deckTerms.map {
                    CommunityDeckTermEntity(
                        deckId = deckId,
                        term = it.term,
                        transcription = it.transcription,
                        translation = it.translation,
                        example = it.example,
                        explanationRu = it.explanationRu
                    )
                }
            )
            _events.emit("Колода «$title» (${deckTerms.size} слов) опубликована!")
            isBusyFlow.value = false
        }
    }

    fun rateDeck(deck: CommunityDeck, stars: Int) {
        viewModelScope.launch {
            val safeStars = stars.coerceIn(1, 5)
            var updatedRating = safeStars.toFloat()
            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank() && !deck.id.startsWith("starter_") && !deck.id.startsWith("local_")) {
                runCatching {
                    updatedRating = apiClient.rateCommunityDeck(deck.id, safeStars).rating
                }
            }
            communityDecksDao.updateDeckRating(deck.id, updatedRating)
            _events.emit("Оценка ${safeStars}★ сохранена для «${deck.title}».")
        }
    }

    fun reportAndBlockDeck(deck: CommunityDeck, reason: String, alsoBlockAuthor: Boolean) {
        viewModelScope.launch {
            communityDecksDao.hideDeckLocally(deck.id)
            if (alsoBlockAuthor) {
                communityDecksDao.hideDecksByAuthor(deck.authorUserId)
            }
            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching {
                    apiClient.reportContent("DECK", deck.id, reason)
                    if (alsoBlockAuthor) {
                        apiClient.blockUser(deck.authorUserId)
                    }
                }
            }
            _events.emit(
                if (alsoBlockAuthor) "Колода скрыта, автор @${deck.authorUsername} заблокирован."
                else "Жалоба на колоду отправлена, колода скрыта."
            )
        }
    }

    private suspend fun seedStarterDecksIfEmpty() {
        if (communityDecksDao.countDecks() > 0) return
        val now = System.currentTimeMillis()
        val starterDecks = listOf(
            CommunityDeckEntity(
                id = "starter_backend_middle",
                title = "Backend Middle Interview Pack",
                description = "Ключевые термины системного дизайна, БД и сетевых контрактов для собеседований.",
                tagsCsv = "backend,interview,system-design",
                authorUserId = "community_editorial",
                authorUsername = "techenglish_curators",
                rating = 4.9f,
                downloadsCount = 128,
                termCount = 5,
                isDownloadedLocally = false,
                isHidden = false,
                createdAt = now - 86_400_000L
            ),
            CommunityDeckEntity(
                id = "starter_android_internals",
                title = "Android & Kotlin Internals",
                description = "Coroutines, Room, Compose Recomposition и управление жизненным циклом.",
                tagsCsv = "android,kotlin,mobile",
                authorUserId = "community_editorial",
                authorUsername = "kotlin_architect",
                rating = 4.8f,
                downloadsCount = 94,
                termCount = 4,
                isDownloadedLocally = false,
                isHidden = false,
                createdAt = now - 172_800_000L
            ),
            CommunityDeckEntity(
                id = "starter_devops_sre",
                title = "DevOps, Kubernetes & SRE Essentials",
                description = "Словарь для инцидентов, постмортемов, наблюдаемости и CI/CD пайплайнов.",
                tagsCsv = "devops,sre,cloud",
                authorUserId = "community_editorial",
                authorUsername = "sre_lead",
                rating = 4.7f,
                downloadsCount = 76,
                termCount = 4,
                isDownloadedLocally = false,
                isHidden = false,
                createdAt = now - 259_200_000L
            )
        )
        communityDecksDao.upsertDecks(starterDecks)
        communityDecksDao.insertDeckTerms(
            listOf(
                CommunityDeckTermEntity(deckId = "starter_backend_middle", term = "Idempotency Key", transcription = "/ˌaɪ.dɛmˈpoʊ.tən.si kiː/", translation = "ключ идемпотентности", example = "Pass an Idempotency-Key header to make payment retries safe.", explanationRu = "Уникальный ключ запроса, защищающий от двойного списания при повторах."),
                CommunityDeckTermEntity(deckId = "starter_backend_middle", term = "Backpressure", transcription = "/ˈbæk.preʃ.ər/", translation = "обратное давление", example = "Apply backpressure when downstream consumers fall behind.", explanationRu = "Сигнал замедления поставщику данных при перегрузке обработчика."),
                CommunityDeckTermEntity(deckId = "starter_backend_middle", term = "Circuit Breaker", transcription = "/ˈsɜːr.kɪt ˈbreɪ.kər/", translation = "автоматический предохранитель", example = "The circuit breaker opened after five consecutive timeouts.", explanationRu = "Паттерн отказоустойчивости, временно отсекающий вызовы к упавшему сервису."),
                CommunityDeckTermEntity(deckId = "starter_backend_middle", term = "Write-Ahead Log", transcription = "/raɪt əˈhɛd lɒɡ/", translation = "журнал предзаписи (WAL)", example = "PostgreSQL persists every transaction to the write-ahead log first.", explanationRu = "Журнал изменений на диске для гарантированного восстановления БД после сбоя."),
                CommunityDeckTermEntity(deckId = "starter_backend_middle", term = "Eventual Consistency", transcription = "/ɪˈvɛn.tʃu.əl kənˈsɪs.tən.si/", translation = "согласованность в конечном счёте", example = "Read replicas operate under eventual consistency.", explanationRu = "Модель репликации, при которой все узлы сходятся к одному состоянию со временем."),

                CommunityDeckTermEntity(deckId = "starter_android_internals", term = "Recomposition", transcription = "/ˌriː.kɒm.pəˈzɪʃ.ən/", translation = "рекомпозиция", example = "Hoist state properly to avoid unnecessary recomposition.", explanationRu = "Повторный вызов Composable-функций при изменении наблюдаемого состояния."),
                CommunityDeckTermEntity(deckId = "starter_android_internals", term = "Structured Concurrency", transcription = "/ˈstrʌk.tʃərd kənˈkʌr.ən.si/", translation = "структурированная конкурентность", example = "Kotlin coroutines enforce structured concurrency via CoroutineScope.", explanationRu = "Привязка времени жизни дочерних корутин к родительскому scope."),
                CommunityDeckTermEntity(deckId = "starter_android_internals", term = "Cold Flow", transcription = "/koʊld floʊ/", translation = "холодный поток", example = "A cold flow starts emitting only when a collector subscribes.", explanationRu = "Поток данных, который запускает вычисления заново для каждого подписчика."),
                CommunityDeckTermEntity(deckId = "starter_android_internals", term = "ANR (Application Not Responding)", transcription = "/ˌeɪ.ɛnˈɑːr/", translation = "зависание главного потока UI", example = "Move SQLite queries off the main thread to prevent ANR dialogs.", explanationRu = "Ошибка блокировки UI-потока Android более чем на 5 секунд."),

                CommunityDeckTermEntity(deckId = "starter_devops_sre", term = "Blameless Postmortem", transcription = "/ˈbleɪm.ləs poʊstˈmɔːr.təm/", translation = "разбор инцидента без поиска виновных", example = "We ran a blameless postmortem to identify systemic gaps.", explanationRu = "Анализ причин сбоя с фокусом на улучшение процессов, а не наказание людей."),
                CommunityDeckTermEntity(deckId = "starter_devops_sre", term = "Error Budget", transcription = "/ˈɛr.ər ˈbʌdʒ.ɪt/", translation = "бюджет ошибок", example = "Freeze feature releases once the monthly error budget is exhausted.", explanationRu = "Допустимый лимит простоя/ошибок в рамках SLO."),
                CommunityDeckTermEntity(deckId = "starter_devops_sre", term = "Canary Deployment", transcription = "/kəˈnɛə.ri dɪˈplɔɪ.mənt/", translation = "канареечный релиз", example = "Route 5% of production traffic to the canary deployment first.", explanationRu = "Поэтапная выкатка новой версии на малую долю пользователей."),
                CommunityDeckTermEntity(deckId = "starter_devops_sre", term = "Liveness Probe", transcription = "/ˈlaɪv.nəs proʊb/", translation = "проверка жизнеспособности пода", example = "Kubernetes restarts the container when the liveness probe fails.", explanationRu = "Автоматическая проверка того, что контейнер не завис в deadlock.")
            )
        )
    }
}
