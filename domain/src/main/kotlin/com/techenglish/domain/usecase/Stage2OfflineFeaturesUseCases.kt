package com.techenglish.domain.usecase

import com.techenglish.domain.model.AcademicTerm
import com.techenglish.domain.model.CustomImportedDoc
import com.techenglish.domain.model.DebugLogChallenge
import com.techenglish.domain.model.DebugLogEvaluation
import com.techenglish.domain.model.DeckCardEntry
import com.techenglish.domain.model.DeckFormat
import com.techenglish.domain.model.DeckImportSummary
import com.techenglish.domain.model.DetectedDocTerm
import com.techenglish.domain.model.PronunciationEvaluation
import com.techenglish.domain.model.PronunciationWordStatus
import com.techenglish.domain.model.ShadowingPrompt
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.TimeAttackMode
import com.techenglish.domain.model.TimeAttackQuestion
import com.techenglish.domain.model.TimeAttackRecord
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.WidgetSnapshot
import com.techenglish.domain.model.Word
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.CustomDocRepository
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.repository.TimeAttackRepository
import com.techenglish.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

// =========================================================================
// ФТ-7.1 — Read Your Own Docs (Offline N-Gram Technical Term Scanner)
// =========================================================================

class ScanCustomDocTermsUseCase @Inject constructor(
    private val academicRepository: AcademicVocabularyRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val customDocRepository: CustomDocRepository
) {
    private data class WordSpan(
        val raw: String,
        val normalized: String,
        val start: Int,
        val end: Int
    )

    fun observeImportedDocs(): Flow<List<CustomImportedDoc>> =
        customDocRepository.observeAllCustomDocs()

    suspend fun getImportedDoc(id: Int): CustomImportedDoc? =
        customDocRepository.getCustomDocById(id)

    suspend fun deleteImportedDoc(id: Int) =
        customDocRepository.deleteCustomDoc(id)

    suspend fun scanText(content: String): List<DetectedDocTerm> {
        if (content.isBlank()) return emptyList()
        val spans = tokenize(content)
        if (spans.isEmpty()) return emptyList()

        // Generate 1..4-word candidate normalized n-grams + morphological stems
        val candidates = LinkedHashSet<String>()
        for (i in spans.indices) {
            val maxN = minOf(4, spans.size - i)
            val sb = StringBuilder()
            for (n in 0 until maxN) {
                if (n > 0) sb.append(' ')
                sb.append(spans[i + n].normalized)
                val phrase = sb.toString()
                if (phrase.length >= 2) {
                    candidates.add(phrase)
                    if (n == 0) {
                        stemVariants(phrase).forEach { candidates.add(it) }
                    }
                }
            }
        }

        val academicMatches = academicRepository.findByNormalizedTerms(candidates.toList())
            .associateBy { it.normalizedTerm }

        val detected = mutableListOf<DetectedDocTerm>()
        var cursor = 0
        while (cursor < spans.size) {
            var matchedLen = 0
            var matchedTerm: AcademicTerm? = null
            var matchedWord: Word? = null

            val maxN = minOf(4, spans.size - cursor)
            for (n in maxN downTo 1) {
                val phrase = (0 until n).joinToString(" ") { spans[cursor + it].normalized }
                if (n == 1 && phrase in STOP_WORDS) continue

                val directAcademic = academicMatches[phrase]
                    ?: if (n == 1) stemVariants(phrase).firstNotNullOfOrNull { academicMatches[it] } else null
                if (directAcademic != null) {
                    matchedTerm = directAcademic
                    matchedLen = n
                    break
                }
                if (n <= 2) {
                    val v1 = vocabularyRepository.findByExactTerm(phrase)
                    if (v1 != null) {
                        matchedWord = v1
                        matchedLen = n
                        break
                    }
                }
            }

            if (matchedLen > 0) {
                val startOffset = spans[cursor].start
                val endOffset = spans[cursor + matchedLen - 1].end
                val rawSlice = content.substring(startOffset, endOffset)
                if (matchedTerm != null) {
                    detected.add(
                        DetectedDocTerm(
                            matchedText = rawSlice,
                            normalizedTerm = matchedTerm.normalizedTerm,
                            startOffset = startOffset,
                            endOffset = endOffset,
                            termId = matchedTerm.id,
                            wordId = null,
                            headword = matchedTerm.term,
                            transcription = matchedTerm.phoneticTranscription,
                            translationRu = matchedTerm.translationRu,
                            translationTg = matchedTerm.translationTg,
                            explanationRu = matchedTerm.definitionRu,
                            domain = matchedTerm.domain
                        )
                    )
                } else if (matchedWord != null) {
                    detected.add(
                        DetectedDocTerm(
                            matchedText = rawSlice,
                            normalizedTerm = matchedWord.term.lowercase(Locale.ROOT),
                            startOffset = startOffset,
                            endOffset = endOffset,
                            termId = null,
                            wordId = matchedWord.id,
                            headword = matchedWord.term,
                            transcription = matchedWord.transcription,
                            translationRu = matchedWord.translation,
                            translationTg = "",
                            explanationRu = matchedWord.explanationRu,
                            domain = matchedWord.categoryName.ifBlank { "Core IT" }
                        )
                    )
                }
                cursor += matchedLen
            } else {
                cursor += 1
            }
        }
        return detected
    }

    suspend fun importAndSaveDocument(
        title: String,
        sourceFileName: String,
        content: String,
        nowMillis: Long = System.currentTimeMillis()
    ): CustomImportedDoc {
        val cleanContent = content.trim()
        require(cleanContent.isNotEmpty()) { "Текст документа не может быть пустым." }
        val cleanTitle = title.trim().ifEmpty { sourceFileName.ifBlank { "Импортированный документ" } }
        val detected = scanText(cleanContent)
        val doc = CustomImportedDoc(
            title = cleanTitle,
            sourceFileName = sourceFileName,
            content = cleanContent,
            detectedTerms = detected,
            createdAt = nowMillis
        )
        val savedId = customDocRepository.saveCustomDoc(doc).toInt()
        return doc.copy(id = savedId)
    }

    private fun tokenize(content: String): List<WordSpan> =
        TOKEN_REGEX.findAll(content).map { m ->
            WordSpan(
                raw = m.value,
                normalized = m.value.lowercase(Locale.ROOT),
                start = m.range.first,
                end = m.range.last + 1
            )
        }.toList()

    internal fun stemVariants(token: String): List<String> {
        if (token.length < 4) return emptyList()
        val variants = mutableListOf<String>()
        if (token.endsWith("ies") && token.length > 4) {
            variants.add(token.dropLast(3) + "y")
        }
        if (token.endsWith("es") && token.length > 4) {
            variants.add(token.dropLast(2))
        }
        if (token.endsWith("s") && !token.endsWith("ss") && token.length > 3) {
            variants.add(token.dropLast(1))
        }
        if (token.endsWith("ing") && token.length > 5) {
            val base = token.dropLast(3)
            variants.add(base)
            variants.add(base + "e")
        }
        if (token.endsWith("ed") && token.length > 4) {
            val base = token.dropLast(2)
            variants.add(base)
            variants.add(base + "e")
        }
        return variants
    }

    companion object {
        private val TOKEN_REGEX = Regex("[A-Za-z][A-Za-z0-9_/-]{1,}")
        private val STOP_WORDS = setOf(
            "the", "and", "for", "with", "that", "this", "from", "are", "was", "were",
            "have", "has", "had", "not", "but", "can", "will", "should", "must", "may",
            "into", "when", "then", "than", "also", "only", "each", "more", "some", "any",
            "all", "our", "your", "their", "its", "use", "using", "used", "run", "runs",
            "set", "get", "new", "one", "two", "see", "via", "per", "out", "off", "non"
        )
    }
}

// =========================================================================
// ФТ-7.2 — Interactive «Debug the Log» Trainer
// =========================================================================

class DebugLogTrainerUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository
) {
    fun getChallenges(): List<DebugLogChallenge> = CHALLENGES

    suspend fun submitDiagnosis(
        challenge: DebugLogChallenge,
        selectedRootCause: String,
        assembledReportTokens: List<String>,
        nowMillis: Long = System.currentTimeMillis()
    ): DebugLogEvaluation {
        val isRootCauseCorrect = selectedRootCause.trim()
            .equals(challenge.correctRootCause.trim(), ignoreCase = true)
        val assembled = assembledReportTokens.joinToString(" ").trim()
        val isReportCorrect = normalizeSentence(assembled) == normalizeSentence(challenge.expectedReportEn)
        val isFullySolved = isRootCauseCorrect && isReportCorrect
        val xp = when {
            isFullySolved -> challenge.xpReward
            isRootCauseCorrect || isReportCorrect -> 10
            else -> 3
        }

        studyLogRepository.insertLog(
            StudyLog(
                actionType = StudyActionType.DOC_READ,
                isCorrect = isFullySolved,
                xpEarned = xp,
                timestamp = nowMillis
            )
        )

        return DebugLogEvaluation(
            isRootCauseCorrect = isRootCauseCorrect,
            isReportCorrect = isReportCorrect,
            isFullySolved = isFullySolved,
            xpEarned = xp,
            expectedReportEn = challenge.expectedReportEn,
            explanationRu = challenge.explanationRu
        )
    }

    private fun normalizeSentence(text: String): String =
        text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    companion object {
        private val CHALLENGES: List<DebugLogChallenge> = listOf(
            DebugLogChallenge(
                id = 1,
                title = "Gradle / Hilt MissingBinding",
                stackKind = "Gradle / Android",
                rawLog = """
                    > Task :app:kaptDebugKotlin FAILED
                    error: [Dagger/MissingBinding] com.techenglish.domain.repository.CustomDocRepository cannot be provided without an @Provides-annotated method.
                        com.techenglish.domain.repository.CustomDocRepository is injected at
                            com.techenglish.domain.usecase.ScanCustomDocTermsUseCase(customDocRepository)
                """.trimIndent(),
                rootCauseQuestionRu = "Шаг 1: В чём корневая причина падения сборки Gradle?",
                rootCauseOptions = listOf(
                    "Отсутствует @Binds/@Provides привязка CustomDocRepository в Hilt-модуле",
                    "На устройстве закончилось место на диске",
                    "Некорректный синтаксис SQL-запроса в Room DAO",
                    "Отсутствует разрешение RECORD_AUDIO в AndroidManifest.xml"
                ),
                correctRootCause = "Отсутствует @Binds/@Provides привязка CustomDocRepository в Hilt-модуле",
                reportPromptRu = "Шаг 2: Соберите каноничное описание бага на английском для Jira / GitHub Issue:",
                reportTokens = listOf("Hilt build fails", "due to missing", "@Binds binding", "for CustomDocRepository.", "network", "timeout"),
                expectedReportEn = "Hilt build fails due to missing @Binds binding for CustomDocRepository.",
                explanationRu = "Dagger/Hilt требует явной привязки (@Binds или @Provides) в @Module с @InstallIn(SingletonComponent::class) для каждого интерфейса репозитория."
            ),
            DebugLogChallenge(
                id = 2,
                title = "PostgreSQL Deadlock 40P01",
                stackKind = "SQL / PostgreSQL",
                rawLog = """
                    ERROR:  deadlock detected (SQLSTATE 40P01)
                    DETAIL:  Process 14208 waits for ShareLock on transaction 89412; blocked by process 14215.
                    Process 14215 waits for ShareLock on transaction 89411; blocked by process 14208.
                    CONTEXT:  while updating tuple (42,11) in relation "academic_terms"
                """.trimIndent(),
                rootCauseQuestionRu = "Шаг 1: Что вызвало ошибку SQLSTATE 40P01 в базе данных?",
                rootCauseOptions = listOf(
                    "Две параллельные транзакции захватили блокировки строк в противоположном порядке",
                    "Таблица academic_terms не существует в текущей схеме",
                    "Исчерпан лимит открытых файловых дескрипторов Linux",
                    "Превышена длина строки VARCHAR(64)"
                ),
                correctRootCause = "Две параллельные транзакции захватили блокировки строк в противоположном порядке",
                reportPromptRu = "Шаг 2: Соберите корректный английский баг-репорт для бэкенд-команды:",
                reportTokens = listOf("Concurrent updates", "trigger a deadlock", "because row locks", "are not acquired", "in canonical order.", "cors"),
                expectedReportEn = "Concurrent updates trigger a deadlock because row locks are not acquired in canonical order.",
                explanationRu = "Для исключения deadlock (40P01) все параллельные транзакции обязаны захватывать строки в едином каноническом порядке (ORDER BY id ASC)."
            ),
            DebugLogChallenge(
                id = 3,
                title = "Docker Container Exit 137 (OOMKilled)",
                stackKind = "Linux / Docker",
                rawLog = """
                    container_name: techenglish-worker
                    State: Exited (137) 8 seconds ago
                    OOMKilled: true
                    kernel: Memory cgroup out of memory: Killed process 1842 (java) anon-rss:524288kB
                """.trimIndent(),
                rootCauseQuestionRu = "Шаг 1: Почему контейнер завершился с кодом 137?",
                rootCauseOptions = listOf(
                    "Процесс JVM превысил лимит памяти cgroup и был принудительно убит ядром (SIGKILL)",
                    "Порт 8080 уже занят другим процессом на хосте",
                    "Dockerfile содержит синтаксическую ошибку в инструкции COPY",
                    "Отсутствует shebang #!/bin/sh в entrypoint-скрипте"
                ),
                correctRootCause = "Процесс JVM превысил лимит памяти cgroup и был принудительно убит ядром (SIGKILL)",
                reportPromptRu = "Шаг 2: Соберите английское описание инцидента для DevOps-тикета:",
                reportTokens = listOf("Worker container", "exited with code 137", "after exceeding", "its cgroup memory limit.", "rebase"),
                expectedReportEn = "Worker container exited with code 137 after exceeding its cgroup memory limit.",
                explanationRu = "Код возврата 137 = 128 + 9 (SIGKILL) с флагом OOMKilled: true. Решение: задать -XX:MaxRAMPercentage=75.0 или увеличить лимит RAM контейнера."
            ),
            DebugLogChallenge(
                id = 4,
                title = "Python UnicodeDecodeError on CSV Import",
                stackKind = "Python / Data",
                rawLog = """
                    Traceback (most recent call last):
                      File "tools/vocabulary_import/vocabulary_importer.py", line 41, in load_csv
                        for row in reader:
                    UnicodeDecodeError: 'utf-8' codec can't decode byte 0xcf in position 128: invalid continuation byte
                """.trimIndent(),
                rootCauseQuestionRu = "Шаг 1: В чём причина падения Python-скрипта импорта?",
                rootCauseOptions = listOf(
                    "Входной CSV-файл сохранён не в UTF-8, из-за чего декодер отверг некорректную байтовую последовательность",
                    "В словаре Python отсутствует ключ normalized_term",
                    "Превышена максимальная глубина рекурсии интерпретатора",
                    "База данных SQLite заблокирована другим процессом"
                ),
                correctRootCause = "Входной CSV-файл сохранён не в UTF-8, из-за чего декодер отверг некорректную байтовую последовательность",
                reportPromptRu = "Шаг 2: Соберите английский баг-репорт для Data Engineering команды:",
                reportTokens = listOf("CSV importer crashes", "with UnicodeDecodeError", "when ingesting", "non-UTF-8 files.", "deadlock"),
                expectedReportEn = "CSV importer crashes with UnicodeDecodeError when ingesting non-UTF-8 files.",
                explanationRu = "При импорте CSV всегда нормализуйте кодировку в UTF-8 (utf-8-sig для файлов с BOM) и перехватывайте UnicodeDecodeError с понятной диагностикой."
            ),
            DebugLogChallenge(
                id = 5,
                title = "HTTP CORS Preflight Blocked",
                stackKind = "HTTP / API",
                rawLog = """
                    Access to fetch at 'https://api.example.org/v1/terms' from origin 'https://app.example.org' has been blocked by CORS policy:
                    Response to preflight request doesn't pass access control check: No 'Access-Control-Allow-Origin' header is present on the requested resource.
                """.trimIndent(),
                rootCauseQuestionRu = "Шаг 1: Почему браузер заблокировал обращение к API?",
                rootCauseOptions = listOf(
                    "Бэкенд не вернул заголовок Access-Control-Allow-Origin в ответе на предварительный запрос OPTIONS",
                    "Сертификат TLS устарел и не проходит проверку PKIX",
                    "Превышен лимит частоты запросов HTTP 429 Too Many Requests",
                    "Сервер вернул 413 Content Too Large из-за размера тела запроса"
                ),
                correctRootCause = "Бэкенд не вернул заголовок Access-Control-Allow-Origin в ответе на предварительный запрос OPTIONS",
                reportPromptRu = "Шаг 2: Соберите чёткий тикет на английском для бэкенд-разработчика:",
                reportTokens = listOf("Preflight OPTIONS", "response is missing", "the Access-Control-Allow-Origin", "CORS header.", "sqlite"),
                expectedReportEn = "Preflight OPTIONS response is missing the Access-Control-Allow-Origin CORS header.",
                explanationRu = "Кросс-доменные запросы в браузере требуют корректного ответа на OPTIONS (preflight) с явным заголовком Access-Control-Allow-Origin."
            )
        )
    }
}

// =========================================================================
// ФТ-7.3 — Speaking / Shadowing Pronunciation Trainer
// =========================================================================

class EvaluatePronunciationUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository
) {
    fun getShadowingPrompts(): List<ShadowingPrompt> = PROMPTS

    suspend fun evaluate(
        referenceText: String,
        spokenText: String,
        nowMillis: Long = System.currentTimeMillis()
    ): PronunciationEvaluation {
        val refWords = tokenizeWords(referenceText)
        val spokenWords = tokenizeWords(spokenText)

        val statuses = refWords.mapIndexed { idx, refWord ->
            val candidate = findBestMatch(refWord, spokenWords, idx)
            val isCorrect = candidate != null
            PronunciationWordStatus(
                referenceWord = refWord,
                spokenWord = candidate,
                isCorrect = isCorrect
            )
        }

        val accuracy = if (refWords.isEmpty()) {
            0
        } else {
            ((statuses.count { it.isCorrect } * 100f) / refWords.size).toInt().coerceIn(0, 100)
        }
        val isPassed = accuracy >= 75
        val xp = if (isPassed) 20 else if (accuracy >= 40) 8 else 2

        studyLogRepository.insertLog(
            StudyLog(
                actionType = StudyActionType.LISTENING,
                isCorrect = isPassed,
                xpEarned = xp,
                timestamp = nowMillis
            )
        )

        return PronunciationEvaluation(
            referenceText = referenceText,
            spokenText = spokenText,
            accuracyPercent = accuracy,
            wordStatuses = statuses,
            isPassed = isPassed,
            xpEarned = xp
        )
    }

    private fun tokenizeWords(text: String): List<String> =
        text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9'\\s-]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim('\'', '-') }
            .filter { it.isNotEmpty() }

    private fun findBestMatch(refWord: String, spokenWords: List<String>, indexHint: Int): String? {
        if (spokenWords.isEmpty()) return null
        val exactAtHint = spokenWords.getOrNull(indexHint)
        if (exactAtHint != null && wordsMatch(refWord, exactAtHint)) return exactAtHint
        return spokenWords.firstOrNull { wordsMatch(refWord, it) }
    }

    private fun wordsMatch(a: String, b: String): Boolean {
        if (a == b) return true
        val maxDistance = if (a.length >= 6) 1 else 0
        if (maxDistance == 0) return false
        return levenshtein(a, b) <= maxDistance
    }

    internal fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = IntArray(n + 1) { it }
        for (i in 1..m) {
            var prevDiag = dp[0]
            dp[0] = i
            for (j in 1..n) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prevDiag + cost)
                prevDiag = temp
            }
        }
        return dp[n]
    }

    companion object {
        private val PROMPTS: List<ShadowingPrompt> = listOf(
            ShadowingPrompt(
                id = "term_async",
                categoryLabelRu = "Сложный IT-термин",
                targetEn = "asynchronous",
                ipa = "/eɪˈsɪŋ.krə.nəs/",
                translationRu = "асинхронный",
                focusNoteRu = "Ударение на второй слог «SING»: эй-СИНК-рэ-нэс."
            ),
            ShadowingPrompt(
                id = "term_poly",
                categoryLabelRu = "Сложный IT-термин",
                targetEn = "polymorphism",
                ipa = "/ˌpɒl.iˈmɔːr.fɪ.zəm/",
                translationRu = "полиморфизм",
                focusNoteRu = "Главное ударение на «MOR»: поли-МОР-фи-зэм."
            ),
            ShadowingPrompt(
                id = "term_vuln",
                categoryLabelRu = "Сложный IT-термин",
                targetEn = "vulnerability",
                ipa = "/ˌvʌl.nər.əˈbɪl.ə.ti/",
                translationRu = "уязвимость",
                focusNoteRu = "Чётко произносите «L-N» (вал-нэ-рэ-БИ-лэ-ти)."
            ),
            ShadowingPrompt(
                id = "term_idemp",
                categoryLabelRu = "Сложный IT-термин",
                targetEn = "idempotency",
                ipa = "/ˌaɪ.demˈpoʊ.tən.si/",
                translationRu = "идемпотентность",
                focusNoteRu = "Ударение на «POU»: ай-дем-ПОУ-тэн-си."
            ),
            ShadowingPrompt(
                id = "term_serial",
                categoryLabelRu = "Сложный IT-термин",
                targetEn = "serialization",
                ipa = "/ˌsɪr.i.əl.əˈzeɪ.ʃən/",
                translationRu = "сериализация",
                focusNoteRu = "Ударение на «ZAY»: си-ри-э-лэ-ЗЕЙ-шн."
            ),
            ShadowingPrompt(
                id = "standup_1",
                categoryLabelRu = "Daily Standup",
                targetEn = "Yesterday I finished the database migration and today I am writing unit tests",
                ipa = "/ˈjes.tər.deɪ aɪ ˈfɪn.ɪʃt ðə ˈdeɪ.tə.beɪs maɪˈɡreɪ.ʃən/",
                translationRu = "Вчера я завершил миграцию БД, а сегодня пишу юнит-тесты.",
                focusNoteRu = "Держите ровный ритм без пауз между «finished the»."
            ),
            ShadowingPrompt(
                id = "standup_2",
                categoryLabelRu = "Daily Standup",
                targetEn = "I am currently blocked on staging credentials",
                ipa = "/aɪ æm ˈkʌr.ənt.li blɒkt ɒn ˈsteɪ.dʒɪŋ krɪˈden.ʃəlz/",
                translationRu = "Я сейчас заблокирован ожиданием ключей для staging.",
                focusNoteRu = "Обратите внимание на окончание -ed в слове «blocked» (/t/)."
            ),
            ShadowingPrompt(
                id = "review_1",
                categoryLabelRu = "Code Review",
                targetEn = "Could we move this heavy query off the main thread",
                ipa = "/kʊd wi muːv ðɪs ˈhev.i ˈkwɪr.i ɒf ðə meɪn θred/",
                translationRu = "Можем перенести этот тяжёлый запрос с главного потока?",
                focusNoteRu = "Мягкий межзубный звук /θ/ в «thread»."
            )
        )
    }
}

// =========================================================================
// ФТ-7.4 — Home Screen Widget Snapshot Builder (Jetpack Glance)
// =========================================================================

class BuildWidgetSnapshotUseCase @Inject constructor(
    private val academicRepository: AcademicVocabularyRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val studyLogRepository: StudyLogRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateStreak: CalculateStreakUseCase
) {
    suspend operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WidgetSnapshot {
        val settings = settingsRepository.observeSettings().first()
        val lang = settings.translationLanguage
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val epochDay = today.toEpochDay()

        val logs = studyLogRepository.observeAllLogs().first()
        val streak = calculateStreak(logs.map { it.timestamp }, today, zoneId)
        val dueWords = vocabularyRepository.getDueCount(nowMillis)
        val dueAcademic = academicRepository.getDueSrsCount(nowMillis)
        val totalDue = dueWords + dueAcademic

        val randomCandidates = academicRepository.getRandomVerifiedTerms(limit = 5)
        val pickedAcademic = if (randomCandidates.isNotEmpty()) {
            randomCandidates[(epochDay % randomCandidates.size).toInt().coerceAtLeast(0)]
        } else null

        if (pickedAcademic != null) {
            return WidgetSnapshot(
                wordOfTheDayTerm = pickedAcademic.term,
                wordOfTheDayIpa = pickedAcademic.phoneticTranscription,
                wordOfTheDayTranslation = pickedAcademic.displayTranslation(lang),
                dueCardsCount = totalDue,
                streakDays = streak,
                language = lang
            )
        }

        val fallbackWord = vocabularyRepository.getRandomWords(limit = 1).firstOrNull()
        return WidgetSnapshot(
            wordOfTheDayTerm = fallbackWord?.term ?: "idempotency",
            wordOfTheDayIpa = fallbackWord?.transcription ?: "/ˌaɪ.demˈpoʊ.tən.si/",
            wordOfTheDayTranslation = fallbackWord?.translation ?: "идемпотентность",
            dueCardsCount = totalDue,
            streakDays = streak,
            language = lang
        )
    }
}

// =========================================================================
// ФТ-7.5 — Time Attack / Speedrun Quiz (60-second Sprint Mode)
// =========================================================================

class TimeAttackQuizUseCase @Inject constructor(
    private val academicRepository: AcademicVocabularyRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val timeAttackRepository: TimeAttackRepository,
    private val studyLogRepository: StudyLogRepository
) {
    fun observeLeaderboard(limit: Int = 10): Flow<List<TimeAttackRecord>> =
        timeAttackRepository.observeTopRecords(limit)

    suspend fun getPersonalBest(mode: TimeAttackMode): Int =
        timeAttackRepository.getPersonalBestScore(mode)

    suspend fun generateQuestions(
        mode: TimeAttackMode,
        language: TranslationLanguage = TranslationLanguage.RU,
        count: Int = 25
    ): List<TimeAttackQuestion> {
        if (mode == TimeAttackMode.PR_ERROR_SPOTTER) {
            return PR_ERROR_QUESTIONS.shuffled().take(count)
        }

        val pool = academicRepository.getRandomVerifiedTerms(limit = maxOf(count * 2, 40))
        if (pool.size >= 4) {
            return pool.take(count).mapIndexed { idx, target ->
                val correct = target.displayTranslation(language)
                val distractors = pool
                    .asSequence()
                    .filter { it.id != target.id }
                    .map { it.displayTranslation(language) }
                    .filter { it != correct && it.isNotBlank() }
                    .distinct()
                    .take(3)
                    .toList()
                val options = (distractors + correct).shuffled()
                TimeAttackQuestion(
                    id = "ta_term_${target.id}_$idx",
                    mode = mode,
                    promptEn = target.term,
                    promptSubtitleRu = "${target.domain} · ${target.cefrLevel}",
                    options = options,
                    correctOption = correct
                )
            }
        }

        val coreWords = vocabularyRepository.getRandomWords(limit = maxOf(count, 12))
        return coreWords.mapIndexed { idx, w ->
            val distractors = coreWords
                .filter { it.id != w.id }
                .map { it.translation }
                .distinct()
                .take(3)
            TimeAttackQuestion(
                id = "ta_core_${w.id}_$idx",
                mode = mode,
                promptEn = w.term,
                promptSubtitleRu = w.categoryName.ifBlank { "IT Vocabulary" },
                options = (distractors + w.translation).shuffled(),
                correctOption = w.translation
            )
        }
    }

    fun calculatePointsForCorrectAnswer(comboCount: Int): Int =
        10 + minOf(comboCount * 2, 20)

    suspend fun recordSessionResult(
        mode: TimeAttackMode,
        score: Int,
        correctCount: Int,
        totalAnswered: Int,
        durationSeconds: Int = DEFAULT_DURATION_SECONDS,
        nowMillis: Long = System.currentTimeMillis()
    ): TimeAttackRecord {
        val xp = maxOf(5, score / 4)
        val record = TimeAttackRecord(
            mode = mode,
            score = score,
            correctCount = correctCount,
            totalAnswered = totalAnswered,
            xpEarned = xp,
            durationSeconds = durationSeconds,
            achievedAt = nowMillis
        )
        val id = timeAttackRepository.saveRecord(record).toInt()
        studyLogRepository.insertLog(
            StudyLog(
                actionType = StudyActionType.FLASHCARD,
                isCorrect = correctCount > 0 && correctCount * 2 >= totalAnswered,
                xpEarned = xp,
                timestamp = nowMillis
            )
        )
        return record.copy(id = id)
    }

    companion object {
        const val DEFAULT_DURATION_SECONDS = 60

        private val PR_ERROR_QUESTIONS: List<TimeAttackQuestion> = listOf(
            TimeAttackQuestion(
                id = "pr_1",
                mode = TimeAttackMode.PR_ERROR_SPOTTER,
                promptEn = "PR says: \"This migration is 100% backward compatible\" — Diff runs: DROP TABLE words;",
                promptSubtitleRu = "Найдите главное противоречие в описании PR:",
                options = listOf(
                    "DROP TABLE ломает обратную совместимость и удаляет данные",
                    "Опечатка в слове migration",
                    "Таблица words не может использоваться в SQLite",
                    "Противоречий нет"
                ),
                correctOption = "DROP TABLE ломает обратную совместимость и удаляет данные"
            ),
            TimeAttackQuestion(
                id = "pr_2",
                mode = TimeAttackMode.PR_ERROR_SPOTTER,
                promptEn = "PR says: \"Zero main-thread I/O\" — Diff calls: dao.searchBlocking(q) inside @Composable",
                promptSubtitleRu = "Найдите ошибку в PR:",
                options = listOf(
                    "Синхронный запрос к БД вызывается прямо в UI-композиции",
                    "Нужно заменить @Composable на @Entity",
                    "DAO не поддерживает поиск по строке",
                    "Всё корректно"
                ),
                correctOption = "Синхронный запрос к БД вызывается прямо в UI-композиции"
            ),
            TimeAttackQuestion(
                id = "pr_3",
                mode = TimeAttackMode.PR_ERROR_SPOTTER,
                promptEn = "PR says: \"Preserves user SRS state\" — Diff uses: @Insert(onConflict = REPLACE)",
                promptSubtitleRu = "Почему этот PR опасен?",
                options = listOf(
                    "REPLACE удаляет строку и сбрасывает repetitions/ease_factor",
                    "REPLACE запрещён синтаксисом Kotlin",
                    "Аннотация @Insert не работает в Room",
                    "PR безопасен"
                ),
                correctOption = "REPLACE удаляет строку и сбрасывает repetitions/ease_factor"
            ),
            TimeAttackQuestion(
                id = "pr_4",
                mode = TimeAttackMode.PR_ERROR_SPOTTER,
                promptEn = "PR says: \"No secrets committed\" — Diff adds: const val GEMINI_API_KEY = \"AIzaSy...\"",
                promptSubtitleRu = "Найдите критическое нарушение безопасности:",
                options = listOf(
                    "API-ключ захардкожен в исходном коде и попадёт в APK",
                    "Ключ должен быть типа Int",
                    "Константа должна называться key1",
                    "Нарушений нет"
                ),
                correctOption = "API-ключ захардкожен в исходном коде и попадёт в APK"
            ),
            TimeAttackQuestion(
                id = "pr_5",
                mode = TimeAttackMode.PR_ERROR_SPOTTER,
                promptEn = "PR says: \"Respects coroutine cancellation\" — Diff has: catch (e: Throwable) { log(e) }",
                promptSubtitleRu = "Какая ошибка допущена в обработке исключений?",
                options = listOf(
                    "Блок catch(Throwable) подавляет CancellationException без повторного проброса",
                    "Логирование запрещено в Kotlin",
                    "Throwable не существует в JVM",
                    "Ошибок нет"
                ),
                correctOption = "Блок catch(Throwable) подавляет CancellationException без повторного проброса"
            ),
            TimeAttackQuestion(
                id = "pr_6",
                mode = TimeAttackMode.PR_ERROR_SPOTTER,
                promptEn = "PR says: \"Supports 200% fontScale\" — Diff sets: Modifier.height(48.dp) on multi-line card",
                promptSubtitleRu = "В чём проблема с доступностью (a11y)?",
                options = listOf(
                    "Жёсткая высота 48.dp обрежет текст при fontScale = 2.0f; нужен heightIn(min = 48.dp)",
                    "dp нельзя использовать в Jetpack Compose",
                    "Карточки не поддерживают текст",
                    "Проблем нет"
                ),
                correctOption = "Жёсткая высота 48.dp обрежет текст при fontScale = 2.0f; нужен heightIn(min = 48.dp)"
            ),
            TimeAttackQuestion(
                id = "pr_7",
                mode = TimeAttackMode.PR_ERROR_SPOTTER,
                promptEn = "PR says: \"Only verified terms imported\" — Diff skips checking verification_status",
                promptSubtitleRu = "Какой инвариант нарушен?",
                options = listOf(
                    "Записи со статусом pending_review или draft попадут в пользовательский словарь",
                    "Поле verification_status нужно удалить из БД",
                    "Импортер станет работать слишком быстро",
                    "Инвариант соблюдён"
                ),
                correctOption = "Записи со статусом pending_review или draft попадут в пользовательский словарь"
            ),
            TimeAttackQuestion(
                id = "pr_8",
                mode = TimeAttackMode.PR_ERROR_SPOTTER,
                promptEn = "PR says: \"Works on API 26+\" — Diff calls createOnDeviceSpeechRecognizer() without SDK_INT guard",
                promptSubtitleRu = "Что произойдёт на Android 8–12 (API 26–32)?",
                options = listOf(
                    "Приложение упадёт с NoSuchMethodError, так как API добавлен только в Android 13 (API 33)",
                    "Приложение автоматически обновит ОС пользователя",
                    "Речь переведётся на латынь",
                    "Ничего не произойдёт"
                ),
                correctOption = "Приложение упадёт с NoSuchMethodError, так как API добавлен только в Android 13 (API 33)"
            )
        )
    }
}

// =========================================================================
// ФТ-7.6 — Deck Import & Export (JSON / CSV / Anki TSV / Anki .apkg notes)
// =========================================================================

class DeckImportExportUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {
    suspend fun exportCustomAndLearnedWordsToJson(): String {
        val words = vocabularyRepository.observeAllWords().first()
            .filter { it.isCustom || it.isFavorite || it.repetitions > 0 }
        return serializeToJson(words)
    }

    suspend fun exportCustomAndLearnedWordsToCsv(): String {
        val words = vocabularyRepository.observeAllWords().first()
            .filter { it.isCustom || it.isFavorite || it.repetitions > 0 }
        return serializeToCsv(words)
    }

    fun serializeToJson(words: List<Word>): String {
        val sb = StringBuilder()
        sb.append("{\n  \"format\": \"techenglish-deck-v3\",\n  \"count\": ")
            .append(words.size)
            .append(",\n  \"cards\": [\n")
        words.forEachIndexed { index, w ->
            sb.append("    {")
            sb.append("\"term\": ").append(jsonQuote(w.term)).append(", ")
            sb.append("\"transcription\": ").append(jsonQuote(w.transcription)).append(", ")
            sb.append("\"translation\": ").append(jsonQuote(w.translation)).append(", ")
            sb.append("\"example\": ").append(jsonQuote(w.example)).append(", ")
            sb.append("\"explanationRu\": ").append(jsonQuote(w.explanationRu)).append(", ")
            sb.append("\"repetitions\": ").append(w.repetitions).append(", ")
            sb.append("\"easeFactor\": ").append(w.easeFactor).append(", ")
            sb.append("\"intervalDays\": ").append(w.intervalDays)
            sb.append("}")
            if (index < words.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n}")
        return sb.toString()
    }

    fun serializeToCsv(words: List<Word>): String {
        val sb = StringBuilder()
        sb.append("term,transcription,translation,example,explanation_ru,repetitions,ease_factor,interval_days\n")
        words.forEach { w ->
            sb.append(csvEscape(w.term)).append(',')
            sb.append(csvEscape(w.transcription)).append(',')
            sb.append(csvEscape(w.translation)).append(',')
            sb.append(csvEscape(w.example)).append(',')
            sb.append(csvEscape(w.explanationRu)).append(',')
            sb.append(w.repetitions).append(',')
            sb.append(w.easeFactor).append(',')
            sb.append(w.intervalDays).append('\n')
        }
        return sb.toString()
    }

    fun parseJsonDeck(rawJson: String): List<DeckCardEntry> {
        val objectRegex = Regex("\\{[^{}]*\"term\"[^{}]*\\}")
        return objectRegex.findAll(rawJson).mapNotNull { match ->
            val obj = match.value
            val term = extractJsonString(obj, "term").trim()
            val translation = extractJsonString(obj, "translation").trim()
            if (term.isEmpty() || translation.isEmpty()) return@mapNotNull null
            DeckCardEntry(
                term = term,
                transcription = extractJsonString(obj, "transcription").trim(),
                translation = translation,
                example = extractJsonString(obj, "example").trim(),
                explanationRu = extractJsonString(obj, "explanationRu").ifBlank {
                    extractJsonString(obj, "explanation_ru")
                }.trim()
            )
        }.toList()
    }

    fun parseCsvDeck(rawCsv: String): List<DeckCardEntry> {
        val lines = rawCsv.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        val startIdx = if (lines.first().lowercase(Locale.ROOT).startsWith("term,")) 1 else 0
        return lines.drop(startIdx).mapNotNull { line ->
            val cols = splitCsvLine(line)
            if (cols.size < 2) return@mapNotNull null
            // Supports both (term, translation, ...) and (term, transcription, translation, example, explanation)
            val term = cols[0].trim()
            val (transcription, translation, example, expl) = if (cols.size >= 3 && (cols[1].startsWith("/") || cols[1].isEmpty())) {
                Quad(cols[1].trim(), cols[2].trim(), cols.getOrNull(3).orEmpty().trim(), cols.getOrNull(4).orEmpty().trim())
            } else {
                Quad("", cols[1].trim(), cols.getOrNull(2).orEmpty().trim(), cols.getOrNull(3).orEmpty().trim())
            }
            if (term.isEmpty() || translation.isEmpty()) return@mapNotNull null
            DeckCardEntry(
                term = term,
                transcription = transcription,
                translation = translation,
                example = example,
                explanationRu = expl
            )
        }
    }

    fun parseAnkiTsvDeck(rawTsv: String): List<DeckCardEntry> {
        return rawTsv.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val cols = line.split('\t')
                if (cols.size < 2) return@mapNotNull null
                val front = stripHtml(cols[0]).trim()
                val back = stripHtml(cols[1]).trim()
                val tags = cols.getOrNull(2).orEmpty()
                    .split(Regex("\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (front.isEmpty() || back.isEmpty()) return@mapNotNull null
                DeckCardEntry(
                    term = front,
                    translation = back,
                    explanationRu = if (tags.isNotEmpty()) "Импортировано из Anki (${tags.joinToString(", ")})" else "Импортировано из Anki TSV",
                    tags = tags
                )
            }
    }

    /**
     * Parses raw `(flds, tags)` rows extracted from Anki `.apkg` (`collection.anki2` `notes` table).
     * Strictly ignores Anki `cards` scheduling so TechEnglish SM-2 manages all review intervals.
     */
    fun parseAnkiNoteRows(rawNoteRows: List<Pair<String, String>>): List<DeckCardEntry> {
        return rawNoteRows.mapNotNull { (flds, rawTags) ->
            val fields = flds.split('\u001F')
            if (fields.size < 2) return@mapNotNull null
            val front = stripHtml(fields[0]).trim()
            val back = stripHtml(fields[1]).trim()
            val example = fields.getOrNull(2)?.let { stripHtml(it).trim() }.orEmpty()
            val tags = rawTags.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (front.isEmpty() || back.isEmpty()) return@mapNotNull null
            DeckCardEntry(
                term = front,
                translation = back,
                example = example,
                explanationRu = if (tags.isNotEmpty()) {
                    "Импортировано из Anki .apkg (${tags.joinToString(", ")})"
                } else {
                    "Импортировано из Anki .apkg"
                },
                tags = tags
            )
        }
    }

    suspend fun importCards(
        format: DeckFormat,
        entries: List<DeckCardEntry>
    ): DeckImportSummary {
        val systemCategoryId = vocabularyRepository.getSystemCategoryId()
        var imported = 0
        var skipped = 0
        val seenInBatch = HashSet<String>()

        for (entry in entries) {
            val cleanTerm = entry.term.trim()
            val cleanTranslation = entry.translation.trim()
            val key = cleanTerm.lowercase(Locale.ROOT)
            if (cleanTerm.isEmpty() || cleanTranslation.isEmpty() || !seenInBatch.add(key)) {
                skipped++
                continue
            }
            val existing = vocabularyRepository.findByExactTerm(cleanTerm)
            if (existing != null) {
                skipped++
                continue
            }
            vocabularyRepository.insertWord(
                Word(
                    id = 0,
                    categoryId = systemCategoryId,
                    categoryName = "Мои слова",
                    term = cleanTerm,
                    transcription = entry.transcription,
                    translation = cleanTranslation,
                    example = entry.example,
                    explanationRu = entry.explanationRu.ifBlank { "Импортировано (${format.labelRu})" },
                    isFavorite = false,
                    isCustom = true,
                    repetitions = 0,
                    easeFactor = 2.5f,
                    intervalDays = 0,
                    nextReviewAt = 0L
                )
            )
            imported++
        }

        return DeckImportSummary(
            format = format,
            totalParsed = entries.size,
            importedCount = imported,
            skippedDuplicates = skipped
        )
    }

    private data class Quad(val a: String, val b: String, val c: String, val d: String)

    private fun stripHtml(raw: String): String =
        raw.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")

    private fun jsonQuote(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun extractJsonString(jsonObj: String, key: String): String {
        val regex = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        val raw = regex.find(jsonObj)?.groupValues?.getOrNull(1) ?: return ""
        return raw
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        cur.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    out.add(cur.toString())
                    cur.setLength(0)
                }
                else -> cur.append(ch)
            }
            i++
        }
        out.add(cur.toString())
        return out
    }
}
