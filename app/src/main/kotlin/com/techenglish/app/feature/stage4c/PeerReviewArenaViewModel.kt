package com.techenglish.app.feature.stage4c

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.MAX_PEER_VOICE_SECONDS
import com.techenglish.app.core.audio.PeerVoiceClipRecorder
import com.techenglish.app.core.audio.RecordedVoiceClip
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.network.PeerReviewSubmitBodyDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.domain.model.PeerReviewCase
import com.techenglish.domain.model.PeerReviewFeedbackItem
import com.techenglish.domain.model.PeerReviewSubmission
import com.techenglish.domain.usecase.ValidatePeerReviewSubmissionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// =========================================================================
// 3. Peer Review Arena ViewModel (ФТ-14.2 & Errata E5 — 30-day Voice TTL)
// =========================================================================

data class PeerReviewArenaUiState(
    val dailyCase: PeerReviewCase = PeerReviewCase(
        caseId = "case_${LocalDate.now()}",
        dateIso = LocalDate.now().toString(),
        titleEn = "Incident Communication: Production Deployment Rollback",
        promptEn = "Explain to your engineering team in 3 clear English sentences why the production deployment failed (missing DB migration index caused lock contention) and what mitigation steps are underway.",
        promptRu = "Объясните команде в 3 чётких предложениях на английском, почему упал продакшн-деплой (отсутствующий индекс миграции вызвал блокировки БД) и какие шаги по восстановлению выполняются."
    ),
    val submissions: List<PeerReviewSubmission> = emptyList(),
    val voiceTtlDays: Int = ValidatePeerReviewSubmissionUseCase.VOICE_TTL_DAYS.toInt(),
    val isRecordingVoice: Boolean = false,
    val recordingSecondsElapsed: Int = 0,
    val recordedVoiceClip: RecordedVoiceClip? = null,
    val playingSubmissionId: String? = null,
    val micPermissionError: String? = null,
    val isOffline: Boolean = false,
    val isBusy: Boolean = false
)

@HiltViewModel
class PeerReviewArenaViewModel @Inject constructor(
    private val apiClient: TechEnglishApiClient,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val validatePeer: ValidatePeerReviewSubmissionUseCase,
    private val voiceClipRecorder: PeerVoiceClipRecorder,
    private val audioController: TechAudioController
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeerReviewArenaUiState(isOffline = !apiClient.isNetworkAvailable()))
    val uiState: StateFlow<PeerReviewArenaUiState> = _uiState
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var recordingTimerJob: Job? = null

    init {
        refresh()
    }

    fun startVoiceRecording() {
        _uiState.value = _uiState.value.copy(micPermissionError = null)
        voiceClipRecorder.startRecording()
            .onSuccess {
                _uiState.value = _uiState.value.copy(
                    isRecordingVoice = true,
                    recordingSecondsElapsed = 0,
                    recordedVoiceClip = null
                )
                recordingTimerJob?.cancel()
                recordingTimerJob = viewModelScope.launch {
                    for (sec in 1..MAX_PEER_VOICE_SECONDS) {
                        delay(1_000L)
                        if (!_uiState.value.isRecordingVoice) return@launch
                        _uiState.value = _uiState.value.copy(recordingSecondsElapsed = sec)
                    }
                    stopVoiceRecording()
                }
            }
            .onFailure { err ->
                val msg = "Не удалось начать запись голоса: ${err.localizedMessage ?: "проверьте доступ к микрофону"}"
                _uiState.value = _uiState.value.copy(isRecordingVoice = false, micPermissionError = msg)
                _events.tryEmit(msg)
            }
    }

    fun stopVoiceRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        voiceClipRecorder.stopRecording()
            .onSuccess { clip ->
                _uiState.value = _uiState.value.copy(
                    isRecordingVoice = false,
                    recordedVoiceClip = clip,
                    recordingSecondsElapsed = clip.durationSeconds
                )
                _events.tryEmit("Голосовой ответ записан: ${clip.durationSeconds} с (${(clip.byteSize / 1024).coerceAtLeast(1)} КБ).")
            }
            .onFailure { err ->
                _uiState.value = _uiState.value.copy(isRecordingVoice = false)
                _events.tryEmit(err.localizedMessage ?: "Ошибка сохранения голосовой записи.")
            }
    }

    fun clearRecordedVoice() {
        recordingTimerJob?.cancel()
        voiceClipRecorder.cancelRecording()
        _uiState.value = _uiState.value.copy(
            isRecordingVoice = false,
            recordingSecondsElapsed = 0,
            recordedVoiceClip = null
        )
    }

    fun onMicPermissionDenied() {
        val msg = "Нет разрешения RECORD_AUDIO: разрешите доступ к микрофону в настройках устройства для записи голосового ответа."
        _uiState.value = _uiState.value.copy(
            isRecordingVoice = false,
            micPermissionError = msg
        )
        _events.tryEmit(msg)
    }

    fun playSubmissionVoice(submissionId: String) {
        viewModelScope.launch {
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) {
                _events.emit("Воспроизведение голосового ответа требует интернета и входа в аккаунт.")
                return@launch
            }
            runCatching { apiClient.fetchPeerReviewVoiceUrl(submissionId) }
                .onSuccess { res ->
                    if (res.downloadUrl.isBlank()) {
                        _events.emit("Ссылка на аудиофайл недоступна.")
                        return@onSuccess
                    }
                    _uiState.value = _uiState.value.copy(playingSubmissionId = submissionId)
                    audioController.playRemoteAudioUrl(res.downloadUrl, "Peer Review #$submissionId")
                }
                .onFailure { err ->
                    _events.emit("Не удалось загрузить голосовой ответ: ${err.localizedMessage ?: "ошибка"}")
                }
        }
    }

    override fun onCleared() {
        recordingTimerJob?.cancel()
        voiceClipRecorder.cancelRecording()
        super.onCleared()
    }

    fun refresh() {
        viewModelScope.launch {
            val offline = !apiClient.isNetworkAvailable()
            _uiState.value = _uiState.value.copy(isOffline = offline)
            if (offline) return@launch
            _uiState.value = _uiState.value.copy(isBusy = true)
            runCatching { apiClient.fetchDailyPeerCase() }.onSuccess { dto ->
                _uiState.value = _uiState.value.copy(
                    dailyCase = PeerReviewCase(
                        caseId = dto.caseId,
                        dateIso = dto.dateIso,
                        titleEn = dto.titleEn,
                        promptEn = dto.promptEn,
                        promptRu = dto.promptRu,
                        targetSentencesCount = dto.targetSentencesCount
                    ),
                    voiceTtlDays = dto.voiceTtlDays
                )
            }
            if (!tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching { apiClient.fetchPeerSubmissions() }.onSuccess { res ->
                    _uiState.value = _uiState.value.copy(
                        submissions = res.items.map { s ->
                            PeerReviewSubmission(
                                id = s.id,
                                caseId = s.caseId,
                                authorUserId = s.authorUserId,
                                authorUsername = s.authorUsername,
                                authorRankTitle = s.authorRankTitle,
                                textResponse = s.textResponse,
                                hasVoiceClip = s.hasVoiceClip,
                                voiceDurationSeconds = s.voiceDurationSeconds,
                                voiceExpiresAtMillis = s.voiceExpiresAtMs,
                                createdAtMillis = s.createdAtMs,
                                feedbackList = s.feedback.map { f ->
                                    PeerReviewFeedbackItem(
                                        id = f.id,
                                        reviewerUsername = f.reviewerUsername,
                                        reviewerRankTitle = f.reviewerRankTitle,
                                        ratingStars = f.ratingStars,
                                        commentText = f.commentText,
                                        createdAtMillis = f.createdAtMs
                                    )
                                }
                            )
                        }
                    )
                }
            }
            _uiState.value = _uiState.value.copy(isBusy = false)
        }
    }

    fun submitResponse(rawText: String, voiceBase64: String? = null, voiceDurationSeconds: Int = 0) {
        viewModelScope.launch {
            val attachedClip = _uiState.value.recordedVoiceClip
            val resolvedVoiceB64 = voiceBase64 ?: attachedClip?.base64Payload
            val resolvedDuration = if (voiceBase64 != null) voiceDurationSeconds else (attachedClip?.durationSeconds ?: 0)
            val resolvedContentType = attachedClip?.contentType ?: "audio/mp4"
            val hasVoice = !resolvedVoiceB64.isNullOrBlank()
            val estimatedBytes = attachedClip?.byteSize ?: if (hasVoice) (resolvedVoiceB64!!.length * 3) / 4 else 0
            val cleanText = validatePeer.validateSubmission(rawText, hasVoice, resolvedDuration, estimatedBytes).getOrElse {
                _events.emit(it.message ?: "Проверьте текст или длительность голосовой записи (≤ 20 с).")
                return@launch
            }
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) {
                _events.emit("Отправка ответа на Peer Review Arena требует подключения к серверу и входа в аккаунт.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isBusy = true)
            runCatching {
                apiClient.submitPeerReview(
                    PeerReviewSubmitBodyDto(
                        caseId = _uiState.value.dailyCase.caseId,
                        textResponse = cleanText,
                        voiceBase64 = resolvedVoiceB64,
                        voiceDurationSeconds = resolvedDuration,
                        voiceContentType = resolvedContentType
                    )
                )
                clearRecordedVoice()
                refresh()
                _events.emit("Ответ отправлен на Peer Review Arena!")
            }.onFailure { err ->
                _events.emit("Ошибка отправки ответа: ${err.localizedMessage ?: "повторите"}")
            }
            _uiState.value = _uiState.value.copy(isBusy = false)
        }
    }

    fun submitFeedback(submissionId: String, stars: Int, comment: String) {
        viewModelScope.launch {
            if (comment.trim().length < 5) {
                _events.emit("Напишите конструктивный отзыв (минимум 5 символов).")
                return@launch
            }
            if (!apiClient.isNetworkAvailable() || tokenStore.getAccessToken().isNullOrBlank()) {
                _events.emit("Оценка ответов доступна онлайн после входа в аккаунт.")
                return@launch
            }
            runCatching {
                apiClient.postPeerFeedback(submissionId, stars.coerceIn(1, 5), comment.trim())
                refresh()
                _events.emit("Отзыв опубликован!")
            }.onFailure { err ->
                _events.emit("Не удалось отправить отзыв: ${err.localizedMessage ?: "ошибка"}")
            }
        }
    }

    fun reportAndBlockSubmission(sub: PeerReviewSubmission, reason: String, alsoBlockAuthor: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                submissions = _uiState.value.submissions.filterNot {
                    it.id == sub.id || (alsoBlockAuthor && it.authorUserId == sub.authorUserId)
                }
            )
            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching {
                    apiClient.reportContent("PEER_REVIEW", sub.id, reason)
                    if (alsoBlockAuthor) apiClient.blockUser(sub.authorUserId)
                }
            }
            _events.emit(
                if (alsoBlockAuthor) "Ответ скрыт, автор @${sub.authorUsername} заблокирован."
                else "Жалоба на ответ отправлена на модерацию."
            )
        }
    }
}
