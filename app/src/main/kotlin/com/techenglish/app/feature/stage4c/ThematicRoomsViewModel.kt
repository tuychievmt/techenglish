package com.techenglish.app.feature.stage4c

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.database.dao.RoomMessagesCacheDao
import com.techenglish.app.core.database.dao.SyncQueueDao
import com.techenglish.app.core.database.entity.RoomMessageCacheEntity
import com.techenglish.app.core.database.entity.SyncQueueEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.network.WsConnectionState
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.domain.model.RoomChatMessage
import com.techenglish.domain.model.ThematicRoomChannel
import com.techenglish.domain.usecase.ValidateUgcContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// =========================================================================
// 1. Thematic Developer Rooms ViewModel (ФТ-13.3 — Ktor WebSockets + Room Cache)
// =========================================================================

@Serializable
internal data class RoomOutboxPayload(
    val roomSlug: String,
    val body: String
)

data class ThematicRoomsUiState(
    val selectedRoom: ThematicRoomChannel = ThematicRoomChannel.CODE_REVIEW_PRACTICE,
    val messages: List<RoomChatMessage> = emptyList(),
    val isOffline: Boolean = false,
    val wsConnectionState: WsConnectionState = WsConnectionState.DISCONNECTED,
    val isWebSocketConnected: Boolean = false,
    val isBusy: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ThematicRoomsViewModel @Inject constructor(
    private val roomMessagesCacheDao: RoomMessagesCacheDao,
    private val syncQueueDao: SyncQueueDao,
    private val apiClient: TechEnglishApiClient,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val validateUgc: ValidateUgcContentUseCase
) : ViewModel() {

    private val outboxJson = Json { ignoreUnknownKeys = true }
    private val selectedRoomFlow = MutableStateFlow(ThematicRoomChannel.CODE_REVIEW_PRACTICE)
    private val wsStateFlow = MutableStateFlow(WsConnectionState.DISCONNECTED)
    private val isBusyFlow = MutableStateFlow(false)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var wsJob: Job? = null

    val uiState: StateFlow<ThematicRoomsUiState> = combine(
        selectedRoomFlow,
        selectedRoomFlow.flatMapLatest { room -> roomMessagesCacheDao.observeRoomMessages(room.slug) },
        wsStateFlow,
        isBusyFlow
    ) { room, rows, wsState, busy ->
        ThematicRoomsUiState(
            selectedRoom = room,
            messages = rows.map {
                RoomChatMessage(
                    id = it.id,
                    roomSlug = it.roomSlug,
                    authorUserId = it.authorUserId,
                    authorUsername = it.authorUsername,
                    authorRankTitle = it.authorRankTitle,
                    body = it.body,
                    isHidden = it.isHidden,
                    createdAtMillis = it.createdAt
                )
            },
            isOffline = !apiClient.isNetworkAvailable(),
            wsConnectionState = wsState,
            isWebSocketConnected = wsState == WsConnectionState.CONNECTED,
            isBusy = busy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ThematicRoomsUiState(isOffline = !apiClient.isNetworkAvailable())
    )

    init {
        selectRoom(ThematicRoomChannel.CODE_REVIEW_PRACTICE)
    }

    fun selectRoom(room: ThematicRoomChannel) {
        selectedRoomFlow.value = room
        connectAndSyncRoom(room)
    }

    /**
     * Resends queued offline room messages (`local_msg_*`) through `sync_queue` upon reconnect (SP3-04 / BUG-19).
     */
    suspend fun flushPendingRoomMessages(): Int {
        if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) return 0
        val pending = syncQueueDao.getPendingRoomOutboxBatch(limit = 50)
        var sentCount = 0
        for (item in pending) {
            val parsed = runCatching { outboxJson.decodeFromString<RoomOutboxPayload>(item.payloadJson) }.getOrNull()
                ?: continue
            runCatching {
                val sent = apiClient.postRoomMessage(parsed.roomSlug, parsed.body)
                roomMessagesCacheDao.deleteMessageById(item.eventId)
                roomMessagesCacheDao.upsertMessages(
                    listOf(
                        RoomMessageCacheEntity(
                            id = sent.id,
                            roomSlug = sent.roomSlug,
                            authorUserId = sent.authorUserId,
                            authorUsername = sent.authorUsername,
                            authorRankTitle = sent.authorRankTitle,
                            body = sent.body,
                            isHidden = false,
                            createdAt = sent.createdAtMs
                        )
                    )
                )
                syncQueueDao.markSynced(listOf(item.eventId))
                sentCount++
            }.onFailure { err ->
                syncQueueDao.recordRetryFailure(item.eventId, err.localizedMessage ?: "room_outbox_retry")
            }
        }
        return sentCount
    }

    private fun connectAndSyncRoom(room: ThematicRoomChannel) {
        wsJob?.cancel()
        wsStateFlow.value = WsConnectionState.DISCONNECTED
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) return@launch
            flushPendingRoomMessages()
            runCatching {
                val res = apiClient.fetchRoomMessages(room.slug)
                roomMessagesCacheDao.upsertMessages(
                    res.messages.map { dto ->
                        RoomMessageCacheEntity(
                            id = dto.id,
                            roomSlug = dto.roomSlug,
                            authorUserId = dto.authorUserId,
                            authorUsername = dto.authorUsername,
                            authorRankTitle = dto.authorRankTitle,
                            body = dto.body,
                            isHidden = false,
                            createdAt = dto.createdAtMs
                        )
                    }
                )
            }
        }
        wsJob = viewModelScope.launch {
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) return@launch
            var attempt = 0
            while (isActive && attempt <= MAX_WS_RECONNECT_ATTEMPTS) {
                val result = runCatching {
                    apiClient.observeRoomWebSocket(
                        roomSlug = room.slug,
                        onMessage = { dto ->
                            attempt = 0
                            roomMessagesCacheDao.upsertMessages(
                                listOf(
                                    RoomMessageCacheEntity(
                                        id = dto.id,
                                        roomSlug = dto.roomSlug,
                                        authorUserId = dto.authorUserId,
                                        authorUsername = dto.authorUsername,
                                        authorRankTitle = dto.authorRankTitle,
                                        body = dto.body,
                                        isHidden = false,
                                        createdAt = dto.createdAtMs
                                    )
                                )
                            )
                        },
                        onStatusChanged = { st -> wsStateFlow.value = st }
                    )
                }
                if (result.isFailure) {
                    wsStateFlow.value = WsConnectionState.ERROR
                }
                attempt++
                if (!isActive || attempt > MAX_WS_RECONNECT_ATTEMPTS || !apiClient.isNetworkAvailable()) {
                    break
                }
                delay((2_000L shl (attempt - 1)).coerceAtMost(16_000L))
            }
        }
    }

    fun sendMessage(rawBody: String) {
        viewModelScope.launch {
            val clean = validateUgc.validateCommentBody(rawBody).getOrElse {
                _events.emit(it.message ?: "Сообщение не прошло модерацию.")
                return@launch
            }
            val room = selectedRoomFlow.value
            val session = tokenStore.sessionFlow.first()
            val now = System.currentTimeMillis()
            isBusyFlow.value = true

            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching {
                    val sent = apiClient.postRoomMessage(room.slug, clean)
                    roomMessagesCacheDao.upsertMessages(
                        listOf(
                            RoomMessageCacheEntity(
                                id = sent.id,
                                roomSlug = sent.roomSlug,
                                authorUserId = sent.authorUserId,
                                authorUsername = sent.authorUsername,
                                authorRankTitle = sent.authorRankTitle,
                                body = sent.body,
                                isHidden = false,
                                createdAt = sent.createdAtMs
                            )
                        )
                    )
                }.onFailure {
                    cacheLocalMessage(room.slug, session.userId, session.username, clean, now)
                    _events.emit("Сохранено в очереди отправки sync_queue — отправится при восстановлении сети.")
                }
            } else {
                cacheLocalMessage(room.slug, session.userId, session.username, clean, now)
                _events.emit("Офлайн: сообщение поставлено в очередь sync_queue и отправится при подключении.")
            }
            isBusyFlow.value = false
        }
    }

    internal suspend fun cacheLocalMessage(roomSlug: String, userId: String, username: String, body: String, now: Long): String {
        val localId = "local_msg_${UUID.randomUUID()}"
        roomMessagesCacheDao.upsertMessages(
            listOf(
                RoomMessageCacheEntity(
                    id = localId,
                    roomSlug = roomSlug,
                    authorUserId = userId,
                    authorUsername = username,
                    authorRankTitle = "Developer",
                    body = body,
                    isHidden = false,
                    createdAt = now
                )
            )
        )
        syncQueueDao.enqueue(
            SyncQueueEntity(
                eventId = localId,
                eventType = ROOM_MESSAGE_OUTBOX_EVENT,
                payloadJson = outboxJson.encodeToString(RoomOutboxPayload(roomSlug = roomSlug, body = body)),
                createdAt = now,
                syncStatus = "PENDING"
            )
        )
        return localId
    }

    companion object {
        const val ROOM_MESSAGE_OUTBOX_EVENT = "ROOM_MESSAGE_OUTBOX"
        private const val MAX_WS_RECONNECT_ATTEMPTS = 4
    }

    fun reportAndBlockMessage(msg: RoomChatMessage, reason: String, alsoBlockAuthor: Boolean) {
        viewModelScope.launch {
            roomMessagesCacheDao.hideMessageLocally(msg.id)
            if (alsoBlockAuthor) {
                roomMessagesCacheDao.hideMessagesByAuthor(msg.authorUserId)
            }
            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching {
                    apiClient.reportContent("ROOM_MESSAGE", msg.id, reason)
                    if (alsoBlockAuthor) apiClient.blockUser(msg.authorUserId)
                }
            }
            _events.emit(
                if (alsoBlockAuthor) "Сообщение скрыто, автор @${msg.authorUsername} заблокирован."
                else "Жалоба на сообщение отправлена, сообщение скрыто."
            )
        }
    }
}

