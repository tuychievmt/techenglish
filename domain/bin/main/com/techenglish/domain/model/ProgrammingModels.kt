package com.techenglish.domain.model

data class ProgrammingLanguage(
    val id: Int,
    val slug: String,
    val name: String,
    val descriptionEn: String,
    val descriptionRu: String,
    val historyEn: String,
    val historyRu: String,
    val paradigms: List<String>,
    val domains: List<String>,
    val frameworks: List<String>,
    val typingSystem: String,
    val memoryManagement: String,
    val difficulty: String,
    val yearCreated: Int,
    val creator: String,
    val accentColorHex: String
)

data class LineExplanation(
    val line: Int,
    val en: String,
    val ru: String
)

data class KeywordTooltip(
    val keyword: String,
    val en: String,
    val ru: String,
    val example: String
)

data class CodeSnippet(
    val id: Int,
    val languageId: Int,
    val taskKey: String,
    val taskTitleEn: String,
    val taskTitleRu: String,
    val code: String,
    val explanationEn: String,
    val explanationRu: String,
    val lineExplanations: List<LineExplanation>,
    val keywords: List<KeywordTooltip>
)

data class ComparisonTaskOption(
    val taskKey: String,
    val titleEn: String,
    val titleRu: String
)

data class LanguageComparison(
    val languageA: ProgrammingLanguage,
    val languageB: ProgrammingLanguage,
    val commonTasks: List<ComparisonTaskOption>,
    val selectedTaskKey: String?,
    val snippetA: CodeSnippet?,
    val snippetB: CodeSnippet?
)
