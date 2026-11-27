package com.techenglish.domain.usecase

import com.techenglish.domain.model.DocArticle
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.WordLookupResult
import com.techenglish.domain.repository.DocumentationRepository
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetDocArticlesUseCase @Inject constructor(
    private val repository: DocumentationRepository
) {
    operator fun invoke(
        techTag: String? = null,
        query: String = ""
    ): Flow<List<DocArticle>> = repository.observeAllArticles().map { articles ->
        val q = query.trim().lowercase()
        articles.filter { article ->
            val matchesTag = techTag.isNullOrBlank() || article.techTag.equals(techTag, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                article.title.lowercase().contains(q) ||
                article.techTag.lowercase().contains(q) ||
                article.content.lowercase().contains(q) ||
                article.explanationRu.lowercase().contains(q)
            matchesTag && matchesQuery
        }
    }
}

class GetDocArticleDetailUseCase @Inject constructor(
    private val repository: DocumentationRepository
) {
    operator fun invoke(articleId: Int): Flow<DocArticle?> =
        repository.observeArticleById(articleId)
}

class UpdateReadingProgressUseCase @Inject constructor(
    private val documentationRepository: DocumentationRepository,
    private val studyLogRepository: StudyLogRepository
) {
    suspend operator fun invoke(
        articleId: Int,
        progress: Float,
        markCompleted: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val existing = documentationRepository.getArticleById(articleId) ?: return
        val clamped = progress.coerceIn(0f, 1f)
        val completed = markCompleted || clamped >= 0.95f || existing.isCompleted
        val newProgress = if (completed) 1f else maxOf(existing.readingProgress, clamped)
        documentationRepository.updateReadingProgress(
            articleId = articleId,
            progress = newProgress,
            isCompleted = completed
        )
        if (completed && !existing.isCompleted) {
            studyLogRepository.insertLog(
                StudyLog(
                    articleId = articleId,
                    actionType = StudyActionType.DOC_READ,
                    isCorrect = true,
                    xpEarned = XP_DOC_COMPLETED,
                    timestamp = nowMillis
                )
            )
        }
    }

    companion object {
        const val XP_DOC_COMPLETED = 15
    }
}

/**
 * Resolves any tapped English word in Technical Documentation offline:
 * 1. Checks the article's contextual glossary (exact & stemmed match).
 * 2. Searches the Room IT vocabulary database (exact, stemmed, and prefix match).
 * 3. Falls back gracefully with a clean technical card allowing the user to add it to SRS.
 */
class LookupWordUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {
    suspend operator fun invoke(
        rawToken: String,
        article: DocArticle? = null
    ): WordLookupResult {
        val cleaned = rawToken.trim().trim { !it.isLetterOrDigit() && it != '-' && it != '_' }
        val lower = cleaned.lowercase()
        val stems = buildCandidateStems(lower)

        val existingExact = stems.firstNotNullOfOrNull { stem ->
            vocabularyRepository.findByExactTerm(stem)
        }

        // 1. Article contextual glossary has tailored contextual explanation
        val glossaryHit = article?.glossary?.firstOrNull { entry ->
            val gLower = entry.term.lowercase()
            gLower == lower || stems.any { it == gLower } || gLower.split(" ").any { it == lower }
        }
        if (glossaryHit != null) {
            return WordLookupResult(
                term = glossaryHit.term,
                transcription = glossaryHit.transcription,
                translation = glossaryHit.translation,
                explanationRu = glossaryHit.explanationRu,
                example = glossaryHit.example,
                existingWordId = existingExact?.id
            )
        }

        // 2. Exact / stemmed match in Room vocabulary DB
        if (existingExact != null) {
            return WordLookupResult(
                term = existingExact.term,
                transcription = existingExact.transcription,
                translation = existingExact.translation,
                explanationRu = existingExact.explanationRu,
                example = existingExact.example,
                existingWordId = existingExact.id
            )
        }

        // 3. Prefix match in Room vocabulary DB (for longer inflected terms >= 5 chars)
        if (lower.length >= 5) {
            val prefixHit = vocabularyRepository.findByTermPrefix(lower.take(lower.length - 1))
            if (prefixHit != null && prefixHit.term.lowercase().startsWith(lower.take(4))) {
                return WordLookupResult(
                    term = prefixHit.term,
                    transcription = prefixHit.transcription,
                    translation = prefixHit.translation,
                    explanationRu = prefixHit.explanationRu,
                    example = prefixHit.example,
                    existingWordId = prefixHit.id
                )
            }
        }

        // 4. Built-in offline fallback dictionary for common technical doc words
        val commonHit = COMMON_TECH_WORDS[lower] ?: stems.firstNotNullOfOrNull { COMMON_TECH_WORDS[it] }
        if (commonHit != null) {
            return WordLookupResult(
                term = cleaned.ifEmpty { rawToken },
                transcription = commonHit.first,
                translation = commonHit.second,
                explanationRu = commonHit.third,
                example = "Found in technical documentation: \"${article?.title ?: "Documentation"}\".",
                existingWordId = null
            )
        }

        return WordLookupResult(
            term = cleaned.ifEmpty { rawToken },
            transcription = "/${lower}/",
            translation = "Технический термин (добавьте свой перевод в словарь)",
            explanationRu = "Слово встретилось в статье «${article?.title ?: "Документация"}». Вы можете добавить его в личный словарь SRS для изучения.",
            example = "Context: ${article?.title ?: "Technical documentation"}.",
            existingWordId = null
        )
    }

    private fun buildCandidateStems(word: String): List<String> {
        val result = linkedSetOf(word)
        if (word.endsWith("ies") && word.length > 4) result.add(word.dropLast(3) + "y")
        if (word.endsWith("ing") && word.length > 5) {
            result.add(word.dropLast(3))
            result.add(word.dropLast(3) + "e")
        }
        if (word.endsWith("ed") && word.length > 4) {
            result.add(word.dropLast(2))
            result.add(word.dropLast(1))
        }
        if (word.endsWith("es") && word.length > 4) result.add(word.dropLast(2))
        if (word.endsWith("s") && word.length > 3) result.add(word.dropLast(1))
        return result.toList()
    }

    companion object {
        private val COMMON_TECH_WORDS: Map<String, Triple<String, String, String>> = mapOf(
            "thread" to Triple("/θred/", "поток выполнения", "Наименьшая единица планирования CPU внутри процесса."),
            "memory" to Triple("/ˈmem.ər.i/", "память", "Оперативная память (RAM), куча (heap) или стек (stack)."),
            "query" to Triple("/ˈkwɪər.i/", "запрос", "Запрос к базе данных или поисковой системе."),
            "request" to Triple("/rɪˈkwest/", "запрос (HTTP/API)", "Сообщение, отправляемое клиентом серверу."),
            "response" to Triple("/rɪˈspɒns/", "ответ сервера", "Данные и код состояния, возвращаемые сервером клиенту."),
            "client" to Triple("/ˈklaɪ.ənt/", "клиент", "Приложение или браузер, инициирующий запрос."),
            "server" to Triple("/ˈsɜːr.vər/", "сервер", "Программа или узел, обрабатывающий входящие запросы."),
            "database" to Triple("/ˈdeɪ.tə.beɪs/", "база данных", "Организованное хранилище структурированных данных."),
            "schema" to Triple("/ˈskiː.mə/", "схема данных", "Формальное описание таблиц, столбцов, типов и связей."),
            "cache" to Triple("/kæʃ/", "кэш", "Быстрое промежуточное хранилище часто запрашиваемых данных."),
            "branch" to Triple("/brɑːntʃ/", "ветка (Git)", "Независимая линия разработки в репозитории Git."),
            "commit" to Triple("/kəˈmɪt/", "коммит (фиксация)", "Сохранённый снимок изменений в истории Git."),
            "header" to Triple("/ˈhed.ər/", "заголовок (HTTP)", "Пара ключ-значение с метаданными HTTP-сообщения."),
            "token" to Triple("/ˈtoʊ.kən/", "токен", "Криптографическая строка для аутентификации или курсора."),
            "container" to Triple("/kənˈteɪ.nər/", "контейнер", "Изолированная среда исполнения приложения и его зависимостей."),
            "kernel" to Triple("/ˈkɜːr.nəl/", "ядро ОС", "Центральный компонент операционной системы, управляющий железом."),
            "process" to Triple("/ˈprəʊ.ses/", "процесс ОС", "Запущенный экземпляр программы с собственным адресным пространством."),
            "exception" to Triple("/ɪkˈsep.ʃən/", "исключение", "Сигнал об ошибочной ситуации во время выполнения программы."),
            "compile" to Triple("/kəmˈpaɪl/", "компилировать", "Преобразовывать исходный код в машинный или байт-код."),
            "runtime" to Triple("/ˈrʌn.taɪm/", "время выполнения / рантайм", "Среда и этап непосредственного исполнения программы."),
            "layer" to Triple("/ˈleɪ.ər/", "слой архитектуры", "Логический уровень приложения (Presentation, Domain, Data)."),
            "interface" to Triple("/ˈɪn.tər.feɪs/", "интерфейс", "Контракт методов без жёсткой привязки к реализации."),
            "reliable" to Triple("/rɪˈlaɪ.ə.bəl/", "надёжный", "Гарантирующий доставку данных или устойчивость к сбоям."),
            "explicit" to Triple("/ɪkˈsplɪs.ɪt/", "явный", "Указанный в коде напрямую, без неявных предположений."),
            "automatic" to Triple("/ˌɔː.təˈmæt.ɪk/", "автоматический", "Выполняемый системой без ручного вмешательства."),
            "prevent" to Triple("/prɪˈvent/", "предотвращать", "Не допускать возникновения ошибки или утечки."),
            "ensure" to Triple("/ɪnˈʃʊər/", "гарантировать / обеспечивать", "Обеспечивать выполнение важного условия."),
            "verify" to Triple("/ˈver.ɪ.faɪ/", "проверять / валидировать", "Подтверждать корректность данных, подписи или статуса.")
        )
    }
}
