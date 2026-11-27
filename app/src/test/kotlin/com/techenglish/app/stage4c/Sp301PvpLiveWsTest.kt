package com.techenglish.app.stage4c

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.network.PvpQuestionDto
import com.techenglish.app.core.network.PvpWsEventDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.feature.stage4c.PvpDuelViewModel
import com.techenglish.domain.model.PvpMatchState
import com.techenglish.domain.usecase.EvaluatePvpDuelUseCase
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class Sp301PvpLiveWsTest {

    @Test
    fun `PvP WS uses single-use ticket, questions omit answer key, and opponent_score updates live`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        val apiClient = TechEnglishApiClient(context, tokenStore)

        val wsUrl = apiClient.buildPvpWsUrl("https://api.techenglish.dev", "match-42", "single_use_ticket_99")
        assertEquals("wss://api.techenglish.dev/ws/pvp/match-42?ticket=single_use_ticket_99", wsUrl)
        assertFalse("WS URL must never carry JWT ?token=", wsUrl.contains("token="))

        // Deserializing sanitized server question (without correct_option_index) defaults to -1
        val json = Json { ignoreUnknownKeys = true }
        val qDto = json.decodeFromString<PvpQuestionDto>(
            """{"id":"pvp_q1","prompt_en":"What is idempotency?","options":["A","B","C","D"],"category_tag":"API"}"""
        )
        assertEquals(-1, qDto.correctOptionIndex)

        // ViewModel handles live WS events: answer_ack + opponent_score + opponent_disconnected
        val vm = PvpDuelViewModel(
            apiClient = apiClient,
            tokenStore = tokenStore,
            evaluatePvpDuel = EvaluatePvpDuelUseCase()
        )
        vm.onPvpWsEvent(PvpWsEventDto(type = "answer_ack", questionIndex = 0, isCorrect = true, correctOptionIndex = 0, score = 2))
        assertEquals(2, vm.uiState.value.playerScore)

        vm.onPvpWsEvent(PvpWsEventDto(type = "opponent_score", matchId = "match-42", score = 4))
        assertEquals(4, vm.uiState.value.opponentScore)

        // Training fallback remains unranked (0 ranked XP, 0 rating delta)
        val outcome = EvaluatePvpDuelUseCase().evaluateCompletedMatch(
            matchId = "training_1",
            mode = com.techenglish.domain.model.PvpMatchMode.TRAINING_FALLBACK,
            playerScore = 8,
            opponentScore = 8
        )
        assertEquals(0, outcome.rankedXpAwarded)
        assertEquals(0, outcome.ratingDelta)
        assertTrue(vm.uiState.value.matchState == PvpMatchState.IDLE)
    }

    @Test
    fun `Training fallback round increments player and bot scores on each question and finishes cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        val apiClient = TechEnglishApiClient(context, tokenStore)
        val vm = PvpDuelViewModel(
            apiClient = apiClient,
            tokenStore = tokenStore,
            evaluatePvpDuel = EvaluatePvpDuelUseCase()
        )

        vm.startLocalTrainingRoundNow(matchId = "training_test_score")
        assertEquals(PvpMatchState.IN_PROGRESS, vm.uiState.value.matchState)
        assertEquals(0, vm.uiState.value.playerScore)
        assertEquals(0, vm.uiState.value.opponentScore)

        val q0 = vm.uiState.value.questions[0]
        vm.submitAnswer(q0.correctOptionIndex)
        assertEquals(1, vm.uiState.value.playerScore)
        assertEquals(1, vm.uiState.value.currentQuestionIndex)

        val q1 = vm.uiState.value.questions[1]
        vm.submitAnswer(q1.correctOptionIndex)
        assertEquals(2, vm.uiState.value.playerScore)
        assertEquals(2, vm.uiState.value.currentQuestionIndex)

        // Answer remaining questions (3rd wrong, rest right) and verify final score & bot simulation
        val q2 = vm.uiState.value.questions[2]
        val wrongIdx = (q2.correctOptionIndex + 1) % q2.options.size
        vm.submitAnswer(wrongIdx)
        assertEquals(2, vm.uiState.value.playerScore)

        while (vm.uiState.value.matchState == PvpMatchState.IN_PROGRESS) {
            val curIdx = vm.uiState.value.currentQuestionIndex
            val curQ = vm.uiState.value.questions[curIdx]
            vm.submitAnswer(curQ.correctOptionIndex)
        }

        assertEquals(PvpMatchState.COMPLETED, vm.uiState.value.matchState)
        assertEquals(7, vm.uiState.value.playerScore)
        assertTrue("Simulated bot should score > 0 across 8 questions", vm.uiState.value.opponentScore in 1..8)
        assertEquals(vm.uiState.value.opponentScore, vm.uiState.value.outcome?.opponentScore)
    }
}
