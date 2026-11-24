package com.techenglish.app.feature.stage4c

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.network.PvpWsEventDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.network.WsConnectionState
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.domain.model.PvpMatchMode
import com.techenglish.domain.model.PvpMatchOutcome
import com.techenglish.domain.model.PvpMatchState
import com.techenglish.domain.model.PvpQuestion
import com.techenglish.domain.usecase.EvaluatePvpDuelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// =========================================================================
// 2. PvP 1v1 Duels ViewModel (ФТ-14.1 — 60s, 15s Training Fallback, Disconnect)
// =========================================================================

data class PvpDuelUiState(
    val matchId: String = "",
    val mode: PvpMatchMode = PvpMatchMode.RANKED_1V1,
    val matchState: PvpMatchState = PvpMatchState.IDLE,
    val matchmakingSecondsLeft: Int = EvaluatePvpDuelUseCase.MATCHMAKING_TIMEOUT_SECONDS,
    val roundSecondsLeft: Int = EvaluatePvpDuelUseCase.ROUND_DURATION_SECONDS,
    val questions: List<PvpQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: List<Int> = emptyList(),
    val playerScore: Int = 0,
    val opponentScore: Int = 0,
    val wsState: WsConnectionState = WsConnectionState.DISCONNECTED,
    val outcome: PvpMatchOutcome? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class PvpDuelViewModel @Inject constructor(
    private val apiClient: TechEnglishApiClient,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val evaluatePvpDuel: EvaluatePvpDuelUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PvpDuelUiState(isOffline = !apiClient.isNetworkAvailable()))
    val uiState: StateFlow<PvpDuelUiState> = _uiState
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var timerJob: Job? = null
    private var pvpWsJob: Job? = null
    private var outgoingWsChannel: Channel<String>? = null

    private fun closePvpWebSocket() {
        outgoingWsChannel?.close()
        outgoingWsChannel = null
        pvpWsJob?.cancel()
        pvpWsJob = null
        _uiState.value = _uiState.value.copy(wsState = WsConnectionState.DISCONNECTED)
    }

    private fun openLivePvpWebSocket(matchId: String) {
        closePvpWebSocket()
        val channel = Channel<String>(Channel.BUFFERED)
        outgoingWsChannel = channel
        pvpWsJob = viewModelScope.launch {
            runCatching {
                apiClient.connectPvpWebSocket(
                    matchId = matchId,
                    outgoingFrames = channel,
                    onEvent = { evt -> onPvpWsEvent(evt) },
                    onStatusChanged = { st -> _uiState.value = _uiState.value.copy(wsState = st) }
                )
            }
        }
    }

    internal fun onPvpWsEvent(evt: PvpWsEventDto) {
        val current = _uiState.value
        when (evt.type) {
            "match_ready" -> {
                if (evt.questions.isNotEmpty() && current.questions.isEmpty()) {
                    _uiState.value = current.copy(
                        questions = evt.questions.map {
                            PvpQuestion(it.id, it.promptEn, it.options, it.correctOptionIndex, it.categoryTag)
                        }
                    )
                }
            }
            "match_matched" -> {
                if (current.matchState == PvpMatchState.SEARCHING && current.questions.isNotEmpty()) {
                    beginActiveRound(
                        matchId = current.matchId,
                        mode = PvpMatchMode.RANKED_1V1,
                        questions = current.questions,
                        connectLiveWs = false // already connected to /ws/pvp/{matchId}
                    )
                }
            }
            "answer_ack" -> {
                _uiState.value = current.copy(playerScore = evt.score)
            }
            "opponent_score", "opponent_finished" -> {
                _uiState.value = current.copy(opponentScore = evt.score)
            }
            "opponent_disconnected" -> {
                if (current.matchState == PvpMatchState.IN_PROGRESS) {
                    timerJob?.cancel()
                    closePvpWebSocket()
                    val outcome = evaluatePvpDuel.evaluateDisconnect(
                        matchId = current.matchId,
                        mode = current.mode,
                        disconnectedByLocalPlayer = false,
                        playerScore = current.playerScore,
                        opponentScore = current.opponentScore
                    )
                    _uiState.value = current.copy(
                        matchState = outcome.finalState,
                        outcome = outcome
                    )
                    _events.tryEmit(outcome.summaryRu)
                }
            }
            "match_completed" -> {
                _uiState.value = current.copy(playerScore = evt.score)
            }
        }
    }

    fun startRankedSearch() {
        timerJob?.cancel()
        closePvpWebSocket()
        val offline = !apiClient.isNetworkAvailable()
        if (offline) {
            _uiState.value = _uiState.value.copy(isOffline = true)
            _events.tryEmit("Нет сети: запускаем тренировочный раунд без рейтинговых очков.")
            startTrainingFallbackRound()
            return
        }
        _uiState.value = PvpDuelUiState(
            mode = PvpMatchMode.RANKED_1V1,
            matchState = PvpMatchState.SEARCHING,
            matchmakingSecondsLeft = EvaluatePvpDuelUseCase.MATCHMAKING_TIMEOUT_SECONDS,
            isOffline = false
        )
        timerJob = viewModelScope.launch {
            // Warm up backend (/health) before 15s matchmaking countdown (Render Free cold start)
            runCatching { apiClient.warmupHealth() }
            val serverMatch = if (!tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching { apiClient.matchmakePvp(forceTrainingFallback = false) }.getOrNull()
            } else null

            if (serverMatch != null && serverMatch.state == "IN_PROGRESS") {
                beginActiveRound(
                    matchId = serverMatch.matchId,
                    mode = PvpMatchMode.RANKED_1V1,
                    questions = serverMatch.questions.map {
                        PvpQuestion(it.id, it.promptEn, it.options, it.correctOptionIndex, it.categoryTag)
                    },
                    connectLiveWs = true
                )
                return@launch
            }

            if (serverMatch != null && serverMatch.state == "SEARCHING") {
                _uiState.value = _uiState.value.copy(
                    matchId = serverMatch.matchId,
                    questions = serverMatch.questions.map {
                        PvpQuestion(it.id, it.promptEn, it.options, it.correctOptionIndex, it.categoryTag)
                    }
                )
                openLivePvpWebSocket(serverMatch.matchId)
            }

            // 15-second matchmaking countdown before automatic training fallback offer (ФТ-14.1, Audit #21)
            for (sec in EvaluatePvpDuelUseCase.MATCHMAKING_TIMEOUT_SECONDS downTo 1) {
                if (_uiState.value.matchState != PvpMatchState.SEARCHING) return@launch
                _uiState.value = _uiState.value.copy(matchmakingSecondsLeft = sec)
                delay(1_000L)
            }
            if (_uiState.value.matchState == PvpMatchState.SEARCHING) {
                closePvpWebSocket()
                _events.emit("Соперник не найден за 15 с — предложен тренировочный раунд (0 рейтинговых очков).")
                startTrainingFallbackRound()
            }
        }
    }

    fun startTrainingFallbackRound() {
        timerJob?.cancel()
        closePvpWebSocket()
        viewModelScope.launch {
            val serverRes = if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank()) {
                runCatching { apiClient.matchmakePvp(forceTrainingFallback = true) }.getOrNull()
            } else null

            val questions = serverRes?.questions?.map {
                PvpQuestion(it.id, it.promptEn, it.options, it.correctOptionIndex, it.categoryTag)
            }?.takeIf { it.isNotEmpty() } ?: fallbackTrainingQuestions()

            beginActiveRound(
                matchId = serverRes?.matchId ?: "training_${UUID.randomUUID()}",
                mode = PvpMatchMode.TRAINING_FALLBACK,
                questions = questions,
                connectLiveWs = false
            )
        }
    }

    private fun beginActiveRound(
        matchId: String,
        mode: PvpMatchMode,
        questions: List<PvpQuestion>,
        connectLiveWs: Boolean
    ) {
        timerJob?.cancel()
        _uiState.value = PvpDuelUiState(
            matchId = matchId,
            mode = mode,
            matchState = PvpMatchState.IN_PROGRESS,
            roundSecondsLeft = EvaluatePvpDuelUseCase.ROUND_DURATION_SECONDS,
            questions = questions,
            currentQuestionIndex = 0,
            selectedAnswers = emptyList(),
            playerScore = 0,
            opponentScore = 0,
            wsState = _uiState.value.wsState,
            outcome = null,
            isOffline = !apiClient.isNetworkAvailable()
        )
        if (connectLiveWs && mode == PvpMatchMode.RANKED_1V1 && !matchId.startsWith("training_")) {
            openLivePvpWebSocket(matchId)
        }
        timerJob = viewModelScope.launch {
            for (sec in EvaluatePvpDuelUseCase.ROUND_DURATION_SECONDS downTo 0) {
                _uiState.value = _uiState.value.copy(roundSecondsLeft = sec)
                if (sec == 0) {
                    finishCurrentMatch()
                    break
                }
                delay(1_000L)
            }
        }
    }

    fun submitAnswer(optionIndex: Int) {
        val current = _uiState.value
        if (current.matchState != PvpMatchState.IN_PROGRESS) return
        val qIdx = current.currentQuestionIndex
        val q = current.questions.getOrNull(qIdx) ?: return

        // In live Ranked 1v1, the server owns answer verification (BUG-09: no client-side answer key);
        // local scoring is only used for offline training fallback questions that carry a local key >= 0.
        val localDelta = if (q.correctOptionIndex >= 0 && optionIndex == q.correctOptionIndex) 1 else 0
        val newScore = current.playerScore + localDelta
        val newAnswers = current.selectedAnswers + optionIndex
        val nextIdx = qIdx + 1

        outgoingWsChannel?.trySend(
            """{"type":"answer","question_index":$qIdx,"option_index":$optionIndex}"""
        )

        if (nextIdx >= current.questions.size) {
            _uiState.value = current.copy(selectedAnswers = newAnswers, playerScore = newScore)
            finishCurrentMatch()
        } else {
            _uiState.value = current.copy(
                selectedAnswers = newAnswers,
                playerScore = newScore,
                currentQuestionIndex = nextIdx
            )
        }
    }

    fun handleDisconnectGracefully() {
        timerJob?.cancel()
        val current = _uiState.value
        closePvpWebSocket()
        if (current.matchState != PvpMatchState.IN_PROGRESS && current.matchState != PvpMatchState.SEARCHING) return
        val outcome = evaluatePvpDuel.evaluateDisconnect(
            matchId = current.matchId,
            mode = current.mode,
            disconnectedByLocalPlayer = true,
            playerScore = current.playerScore,
            opponentScore = current.opponentScore
        )
        _uiState.value = current.copy(
            matchState = outcome.finalState,
            outcome = outcome
        )
        viewModelScope.launch {
            if (current.matchId.isNotBlank() && !current.matchId.startsWith("training_")) {
                apiClient.disconnectPvp(current.matchId, "user_or_network_disconnect")
            }
            _events.emit(outcome.summaryRu)
        }
    }

    private fun finishCurrentMatch() {
        timerJob?.cancel()
        outgoingWsChannel?.trySend("""{"type":"finish"}""")
        val current = _uiState.value
        val provisionalOutcome = evaluatePvpDuel.evaluateCompletedMatch(
            matchId = current.matchId,
            mode = current.mode,
            playerScore = current.playerScore,
            opponentScore = if (current.mode == PvpMatchMode.TRAINING_FALLBACK) current.questions.size else current.opponentScore
        )
        _uiState.value = current.copy(
            matchState = PvpMatchState.COMPLETED,
            outcome = provisionalOutcome
        )
        viewModelScope.launch {
            if (apiClient.isNetworkAvailable() && !tokenStore.getAccessToken().isNullOrBlank() && !current.matchId.startsWith("training_")) {
                runCatching { apiClient.submitPvpAnswers(current.matchId, current.selectedAnswers) }
                    .onSuccess { res ->
                        val authoritativeOutcome = evaluatePvpDuel.evaluateCompletedMatch(
                            matchId = res.matchId,
                            mode = current.mode,
                            playerScore = res.playerScore,
                            opponentScore = if (current.mode == PvpMatchMode.TRAINING_FALLBACK) res.totalQuestions else _uiState.value.opponentScore
                        )
                        _uiState.value = _uiState.value.copy(
                            playerScore = res.playerScore,
                            outcome = authoritativeOutcome
                        )
                    }
            }
            closePvpWebSocket()
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        closePvpWebSocket()
        super.onCleared()
    }

    private fun fallbackTrainingQuestions(): List<PvpQuestion> = listOf(
        PvpQuestion("tr1", "Which term describes an operation that produces the same result when repeated?", listOf("Idempotency", "Sharding", "Deadlock", "Starvation"), 0, "API"),
        PvpQuestion("tr2", "Log: 'SQLSTATE 40P01: deadlock detected'. Root cause?", listOf("Two transactions waiting on each other's locks", "Expired TLS cert", "Disk full", "Malformed JSON"), 0, "LOGS"),
        PvpQuestion("tr3", "Best polite PR comment phrasing:", listOf("Nit: could we extract this helper for clarity?", "Wrong code, rewrite.", "Delete this.", "Bad logic."), 0, "PR"),
        PvpQuestion("tr4", "What does 'backpressure' prevent?", listOf("Fast producers overwhelming slow consumers", "SQL injection", "DNS spoofing", "Type erasure"), 0, "SYSTEMS"),
        PvpQuestion("tr5", "When a Kubernetes livenessProbe fails, kubelet:", listOf("Restarts the container", "Deletes the cluster", "Drops TLS", "Clears Git history"), 0, "DEVOPS"),
        PvpQuestion("tr6", "What is a 'canary deployment'?", listOf("Gradually routing small traffic share to a new release", "Turning off tests", "Deleting staging", "Dropping indexes"), 0, "DEVOPS"),
        PvpQuestion("tr7", "Error: 'OutOfMemoryError: Metaspace'. Affected area?", listOf("JVM class metadata region", "TCP handshake", "DNS resolver", "Git merge driver"), 0, "LOGS"),
        PvpQuestion("tr8", "What is an 'error budget' in SRE?", listOf("Allowable unreliability threshold under SLO", "Cloud billing invoice", "Compiler warning count", "Heap allocation cap"), 0, "SRE")
    )
}

