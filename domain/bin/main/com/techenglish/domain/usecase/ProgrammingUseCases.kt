package com.techenglish.domain.usecase

import com.techenglish.domain.model.CodeSnippet
import com.techenglish.domain.model.ComparisonTaskOption
import com.techenglish.domain.model.LanguageComparison
import com.techenglish.domain.model.ProgrammingLanguage
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.repository.ProgrammingLanguageRepository
import com.techenglish.domain.repository.StudyLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetProgrammingLanguagesUseCase @Inject constructor(
    private val repository: ProgrammingLanguageRepository
) {
    operator fun invoke(
        domainFilter: String? = null,
        query: String = ""
    ): Flow<List<ProgrammingLanguage>> = repository.observeAllLanguages().map { list ->
        val normalizedQuery = query.trim().lowercase()
        list.filter { lang ->
            val matchesDomain = domainFilter.isNullOrBlank() ||
                lang.domains.any { it.equals(domainFilter, ignoreCase = true) }
            val matchesQuery = normalizedQuery.isEmpty() ||
                lang.name.lowercase().contains(normalizedQuery) ||
                lang.descriptionEn.lowercase().contains(normalizedQuery) ||
                lang.descriptionRu.lowercase().contains(normalizedQuery) ||
                lang.frameworks.any { it.lowercase().contains(normalizedQuery) } ||
                lang.paradigms.any { it.lowercase().contains(normalizedQuery) }
            matchesDomain && matchesQuery
        }
    }
}

class GetLanguageDetailUseCase @Inject constructor(
    private val repository: ProgrammingLanguageRepository
) {
    fun observeLanguage(languageId: Int): Flow<ProgrammingLanguage?> =
        repository.observeLanguageById(languageId)

    fun observeSnippets(languageId: Int): Flow<List<CodeSnippet>> =
        repository.observeSnippetsByLanguage(languageId)
}

class GetCodeSnippetUseCase @Inject constructor(
    private val repository: ProgrammingLanguageRepository
) {
    suspend operator fun invoke(snippetId: Int): Pair<ProgrammingLanguage, CodeSnippet>? {
        val snippet = repository.getSnippetById(snippetId) ?: return null
        val language = repository.getLanguageById(snippet.languageId) ?: return null
        return language to snippet
    }
}

/**
 * Dynamically compares ANY two programming languages using common `task_key` intersection.
 * Returns only tasks for which implementations exist in BOTH languages.
 */
class CompareLanguagesUseCase @Inject constructor(
    private val repository: ProgrammingLanguageRepository
) {
    suspend operator fun invoke(
        langAId: Int,
        langBId: Int,
        requestedTaskKey: String? = null
    ): LanguageComparison? {
        val langA = repository.getLanguageById(langAId) ?: return null
        val langB = repository.getLanguageById(langBId) ?: return null

        val commonKeys = repository.getCommonTaskKeys(langAId, langBId)
        val snippetsA = repository.getSnippetsByLanguage(langAId).associateBy { it.taskKey }
        val snippetsB = repository.getSnippetsByLanguage(langBId).associateBy { it.taskKey }

        val commonTasks = commonKeys.mapNotNull { key ->
            val snA = snippetsA[key] ?: return@mapNotNull null
            if (!snippetsB.containsKey(key)) return@mapNotNull null
            ComparisonTaskOption(
                taskKey = key,
                titleEn = snA.taskTitleEn,
                titleRu = snA.taskTitleRu
            )
        }

        val activeKey = when {
            requestedTaskKey != null && commonTasks.any { it.taskKey == requestedTaskKey } -> requestedTaskKey
            else -> commonTasks.firstOrNull()?.taskKey
        }

        return LanguageComparison(
            languageA = langA,
            languageB = langB,
            commonTasks = commonTasks,
            selectedTaskKey = activeKey,
            snippetA = activeKey?.let { snippetsA[it] },
            snippetB = activeKey?.let { snippetsB[it] }
        )
    }
}

class LogCodeBreakdownUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository
) {
    suspend operator fun invoke(
        snippetId: Int,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        studyLogRepository.insertLog(
            StudyLog(
                snippetId = snippetId,
                actionType = StudyActionType.CODE_BREAKDOWN,
                isCorrect = true,
                xpEarned = XP_CODE_BREAKDOWN,
                timestamp = nowMillis
            )
        )
    }

    companion object {
        const val XP_CODE_BREAKDOWN = 10
    }
}
