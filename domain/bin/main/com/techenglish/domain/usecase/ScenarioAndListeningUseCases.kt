package com.techenglish.domain.usecase

import com.techenglish.domain.model.ListeningEvaluation
import com.techenglish.domain.model.ListeningQuestion
import com.techenglish.domain.model.ScenarioSubmissionResult
import com.techenglish.domain.model.ScenarioType
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.WorkPhrase
import com.techenglish.domain.model.WorkScenario
import com.techenglish.domain.repository.ScenarioRepository
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import javax.inject.Inject

class GetScenariosUseCase @Inject constructor(
    private val scenarioRepository: ScenarioRepository
) {
    operator fun invoke(type: ScenarioType? = null): Flow<List<WorkScenario>> =
        scenarioRepository.observeAllScenarios().map { all ->
            if (type == null) all else all.filter { it.type == type }
        }
}

class SubmitScenarioAnswerUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository
) {
    suspend operator fun invoke(
        scenario: WorkScenario,
        userAnswer: String,
        nowMillis: Long = System.currentTimeMillis()
    ): ScenarioSubmissionResult {
        val isCorrect = normalizeAnswer(userAnswer) == normalizeAnswer(scenario.solution)
        val xpEarned = if (isCorrect) XP_SCENARIO_CORRECT else XP_SCENARIO_ATTEMPT

        studyLogRepository.insertLog(
            StudyLog(
                scenarioId = scenario.id,
                actionType = StudyActionType.SCENARIO,
                isCorrect = isCorrect,
                xpEarned = xpEarned,
                timestamp = nowMillis
            )
        )

        return ScenarioSubmissionResult(
            isCorrect = isCorrect,
            userAnswer = userAnswer.trim(),
            expectedAnswer = scenario.solution,
            explanationRu = scenario.explanationRu,
            usefulExpressions = scenario.usefulExpressions,
            xpEarned = xpEarned
        )
    }

    private fun normalizeAnswer(text: String): String =
        text.trim()
            .lowercase()
            .replace(Regex("[\\s]+"), " ")
            .replace(Regex("[.,!?;:\"'`]+$"), "")

    companion object {
        const val XP_SCENARIO_CORRECT = 20
        const val XP_SCENARIO_ATTEMPT = 5
    }
}

class GetWorkPhrasesUseCase @Inject constructor(
    private val scenarioRepository: ScenarioRepository
) {
    operator fun invoke(
        category: ScenarioType? = null,
        query: String = ""
    ): Flow<List<WorkPhrase>> = scenarioRepository.observeAllPhrases().map { phrases ->
        val q = query.trim().lowercase()
        phrases.filter { phrase ->
            val matchesCat = category == null || phrase.category == category
            val matchesQ = q.isEmpty() ||
                phrase.phraseEn.lowercase().contains(q) ||
                phrase.translationRu.lowercase().contains(q) ||
                phrase.contextRu.lowercase().contains(q)
            matchesCat && matchesQ
        }
    }
}

/**
 * Generates an interactive offline Listening session drawing from both
 * IT vocabulary terms and workplace communication phrases.
 */
class GenerateListeningSessionUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val scenarioRepository: ScenarioRepository
) {
    suspend operator fun invoke(
        count: Int = 8,
        includePhrases: Boolean = true,
        random: Random = Random.Default
    ): List<ListeningQuestion> {
        val words = vocabularyRepository.getRandomWords(limit = max(24, count * 4))
        val phrases = if (includePhrases) scenarioRepository.getAllPhrases().shuffled(random) else emptyList()

        val questions = mutableListOf<ListeningQuestion>()
        val phraseQuota = if (includePhrases && phrases.size >= 4) count / 2 else 0
        val wordQuota = count - phraseQuota

        // Build phrase listening questions
        phrases.take(phraseQuota).forEachIndexed { idx, phrase ->
            val distractors = phrases
                .filter { it.id != phrase.id }
                .shuffled(random)
                .take(3)
                .map { it.translationRu }
            val options = (distractors + phrase.translationRu).shuffled(random)
            questions.add(
                ListeningQuestion(
                    id = "phrase_${phrase.id}_$idx",
                    audioText = phrase.phraseEn,
                    translationRu = phrase.translationRu,
                    contextHintRu = "${phrase.category.labelRu} • ${phrase.contextRu}",
                    options = options,
                    correctOption = phrase.translationRu,
                    wordId = null
                )
            )
        }

        // Build vocabulary term listening questions
        words.take(wordQuota).forEachIndexed { idx, word ->
            val distractors = words
                .filter { it.id != word.id }
                .shuffled(random)
                .take(3)
                .map { "${it.term} — ${it.translation}" }
            val correctLabel = "${word.term} — ${word.translation}"
            val options = (distractors + correctLabel).shuffled(random)
            questions.add(
                ListeningQuestion(
                    id = "word_${word.id}_$idx",
                    audioText = word.term,
                    translationRu = word.translation,
                    contextHintRu = "${word.categoryName} • ${word.transcription}",
                    options = options,
                    correctOption = correctLabel,
                    wordId = word.id
                )
            )
        }

        return questions.shuffled(random)
    }
}

class EvaluateListeningAnswerUseCase @Inject constructor() {

    fun evaluateChoice(question: ListeningQuestion, selectedOption: String): ListeningEvaluation {
        val isCorrect = selectedOption.trim() == question.correctOption.trim()
        return ListeningEvaluation(
            isCorrect = isCorrect,
            similarityPercent = if (isCorrect) 100 else 0,
            expectedText = question.correctOption,
            userInput = selectedOption,
            xpEarned = if (isCorrect) XP_LISTENING_CORRECT else XP_LISTENING_ATTEMPT
        )
    }

    fun evaluateDictation(question: ListeningQuestion, typedText: String): ListeningEvaluation {
        val expectedNorm = normalizeForDictation(question.audioText)
        val inputNorm = normalizeForDictation(typedText)
        val similarity = computeSimilarityPercent(expectedNorm, inputNorm)
        val isCorrect = similarity >= DICTATION_PASS_THRESHOLD
        return ListeningEvaluation(
            isCorrect = isCorrect,
            similarityPercent = similarity,
            expectedText = question.audioText,
            userInput = typedText.trim(),
            xpEarned = if (isCorrect) XP_LISTENING_CORRECT else XP_LISTENING_ATTEMPT
        )
    }

    private fun normalizeForDictation(text: String): String =
        text.lowercase()
            .replace(Regex("[^a-z0-9\\s']"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun computeSimilarityPercent(a: String, b: String): Int {
        if (a == b) return 100
        if (a.isEmpty() || b.isEmpty()) return 0
        val dist = levenshtein(a, b)
        val maxLen = max(a.length, b.length)
        return (((maxLen - dist).toFloat() / maxLen.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            curr.copyInto(prev)
        }
        return prev[b.length]
    }

    companion object {
        const val DICTATION_PASS_THRESHOLD = 85
        const val XP_LISTENING_CORRECT = 15
        const val XP_LISTENING_ATTEMPT = 5
    }
}

class LogListeningAttemptUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository
) {
    suspend operator fun invoke(
        wordId: Int?,
        evaluation: ListeningEvaluation,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        studyLogRepository.insertLog(
            StudyLog(
                wordId = wordId,
                actionType = StudyActionType.LISTENING,
                isCorrect = evaluation.isCorrect,
                xpEarned = evaluation.xpEarned,
                timestamp = nowMillis
            )
        )
    }
}
