package com.techenglish.domain.usecase

import com.techenglish.domain.FakeScenarioRepository
import com.techenglish.domain.FakeStudyLogRepository
import com.techenglish.domain.FakeVocabularyRepository
import com.techenglish.domain.model.ExerciseType
import com.techenglish.domain.model.ListeningQuestion
import com.techenglish.domain.model.ScenarioType
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.UsefulExpression
import com.techenglish.domain.model.Word
import com.techenglish.domain.model.WorkPhrase
import com.techenglish.domain.model.WorkScenario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ScenarioAndListeningUseCasesTest {

    private val scenario = WorkScenario(
        id = 1,
        type = ScenarioType.CODE_REVIEW,
        exerciseType = ExerciseType.FILL_BLANKS,
        title = "Nitpick prefix",
        prompt = "Fill in ___:",
        promptRu = "Заполните пропуск",
        solution = "Nit",
        options = listOf("Nit", "Fatal"),
        explanationRu = "Используется для мелких замечаний.",
        usefulExpressions = listOf(UsefulExpression("Nit: rename", "Мелочь: переименовать", "Code Review")),
        difficulty = "A2"
    )

    private val phrases = (1..6).map { i ->
        WorkPhrase(
            id = i,
            category = ScenarioType.DAILY_STANDUP,
            phraseEn = "Standup phrase number $i",
            translationRu = "Фраза стендапа $i",
            contextRu = "Стендап",
            difficulty = "B1"
        )
    }

    private val words = (1..6).map { i ->
        Word(
            id = i,
            categoryId = 1,
            categoryName = "Backend",
            term = "term$i",
            transcription = "/t$i/",
            translation = "перевод$i",
            example = "Example $i",
            explanationRu = "Пояснение $i"
        )
    }

    @Test
    fun `submitScenarioAnswer checks normalized equality and logs attempt`() = runTest {
        val logRepo = FakeStudyLogRepository()
        val submit = SubmitScenarioAnswerUseCase(logRepo)

        val ok = submit(scenario, "  nit. ", nowMillis = 100L)
        assertTrue(ok.isCorrect)
        assertEquals(SubmitScenarioAnswerUseCase.XP_SCENARIO_CORRECT, ok.xpEarned)

        val wrong = submit(scenario, "Fatal", nowMillis = 200L)
        assertFalse(wrong.isCorrect)
        assertEquals(SubmitScenarioAnswerUseCase.XP_SCENARIO_ATTEMPT, wrong.xpEarned)
        assertEquals(2, logRepo.currentLogs.size)
        assertEquals(StudyActionType.SCENARIO, logRepo.currentLogs.first().actionType)
    }

    @Test
    fun `getScenarios and getWorkPhrases filter cleanly`() = runTest {
        val repo = FakeScenarioRepository(listOf(scenario), phrases)
        assertEquals(1, GetScenariosUseCase(repo)(ScenarioType.CODE_REVIEW).first().size)
        assertEquals(0, GetScenariosUseCase(repo)(ScenarioType.INTERVIEW).first().size)
        assertEquals(1, GetWorkPhrasesUseCase(repo)(query = "number 3").first().size)
    }

    @Test
    fun `listening session generation, evaluation, and logging`() = runTest {
        val vocabRepo = FakeVocabularyRepository(words)
        val scenarioRepo = FakeScenarioRepository(listOf(scenario), phrases)
        val logRepo = FakeStudyLogRepository()

        val questions = GenerateListeningSessionUseCase(vocabRepo, scenarioRepo)(
            count = 6,
            includePhrases = true,
            random = Random(42)
        )
        assertEquals(6, questions.size)
        assertTrue(questions.all { it.options.size == 4 })
        assertTrue(questions.all { it.correctOption in it.options })

        val evaluator = EvaluateListeningAnswerUseCase()
        val q = ListeningQuestion(
            id = "q1",
            audioText = "No blockers on my end.",
            translationRu = "Блокеров нет.",
            contextHintRu = "Daily Standup",
            options = listOf("Блокеров нет.", "Есть блокер."),
            correctOption = "Блокеров нет.",
            wordId = 1
        )

        val choiceOk = evaluator.evaluateChoice(q, "Блокеров нет.")
        assertTrue(choiceOk.isCorrect)
        assertEquals(100, choiceOk.similarityPercent)

        val choiceWrong = evaluator.evaluateChoice(q, "Есть блокер.")
        assertFalse(choiceWrong.isCorrect)

        val dictExact = evaluator.evaluateDictation(q, "no blockers on my end")
        assertTrue(dictExact.isCorrect)
        assertEquals(100, dictExact.similarityPercent)

        val dictTypo = evaluator.evaluateDictation(q, "no blockrs on my end")
        assertTrue("Expected minor typo to pass >=85%, got ${dictTypo.similarityPercent}", dictTypo.isCorrect)

        val dictWrong = evaluator.evaluateDictation(q, "completely different words")
        assertFalse(dictWrong.isCorrect)

        LogListeningAttemptUseCase(logRepo)(q.wordId, dictExact, nowMillis = 500L)
        assertEquals(1, logRepo.currentLogs.size)
        assertEquals(StudyActionType.LISTENING, logRepo.currentLogs.first().actionType)
    }
}
