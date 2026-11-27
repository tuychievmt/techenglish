package com.techenglish.domain.model

enum class AcademicSrsMode(
    val labelRu: String,
    val labelEn: String,
    val majorCategoryFilter: String?,
    val onlyCustomQueue: Boolean = false
) {
    ACADEMIC_VOCABULARY(
        labelRu = "Академический словарь (общий)",
        labelEn = "Academic Vocabulary",
        majorCategoryFilter = null
    ),
    MACHINE_LEARNING(
        labelRu = "Machine Learning & AI",
        labelEn = "Machine Learning Vocabulary",
        majorCategoryFilter = "Machine Learning & Artificial Intelligence"
    ),
    CRYPTOGRAPHY(
        labelRu = "Криптография и кибербезопасность",
        labelEn = "Cryptography Vocabulary",
        majorCategoryFilter = "Cryptography & Cybersecurity"
    ),
    COMPUTER_ARCHITECTURE(
        labelRu = "Архитектура ЭВМ и системное ПО",
        labelEn = "Computer Architecture Vocabulary",
        majorCategoryFilter = "Low-Level Programming & Computer Architecture"
    ),
    RESEARCH_PAPER(
        labelRu = "Научные публикации и стандарты",
        labelEn = "Research Paper Vocabulary",
        majorCategoryFilter = "Advanced Documentation & Research Vocabulary"
    ),
    CUSTOM_VOCABULARY(
        labelRu = "Личная очередь и избранное",
        labelEn = "Custom Vocabulary",
        majorCategoryFilter = null,
        onlyCustomQueue = true
    )
}

enum class AcademicStudyFilter(val code: Int, val labelRu: String) {
    ALL(0, "Все"),
    STUDIED(1, "Изученные"),
    NOT_STUDIED(2, "Неизученные")
}

data class AcademicSearchFilter(
    val query: String = "",
    val majorCategory: String? = null,
    val domain: String? = null,
    val subdomain: String? = null,
    val difficulty: String? = null,
    val cefr: String? = null,
    val partOfSpeech: String? = null,
    val onlyFavorites: Boolean = false,
    val onlyDue: Boolean = false,
    val onlyDifficult: Boolean = false,
    val studyFilter: AcademicStudyFilter = AcademicStudyFilter.ALL
)

data class WordSense(
    val id: Int,
    val termId: Int,
    val senseNumber: Int,
    val senseLabel: String,
    val domain: String,
    val definitionEn: String,
    val definitionRu: String,
    val exampleEn: String,
    val exampleRu: String
)

data class WordSource(
    val id: Int,
    val title: String,
    val url: String,
    val sourceType: String,
    val publisher: String,
    val license: String = ""
)

data class WordRelation(
    val id: Int,
    val termId: Int,
    val relatedTerm: String,
    val relationType: String
)

data class AcademicTerm(
    val id: Int,
    val term: String,
    val normalizedTerm: String,
    val phoneticTranscription: String = "",
    val partOfSpeech: String,
    val translationRu: String,
    val translationTg: String = "",
    val definitionEn: String,
    val definitionRu: String,
    val academicExplanationEn: String,
    val academicExplanationRu: String,
    val practicalExplanation: String,
    val exampleEn: String,
    val exampleRu: String,
    val majorCategory: String,
    val categoryId: Int,
    val subcategoryId: Int,
    val domain: String,
    val subdomain: String,
    val difficultyLevel: String,
    val cefrLevel: String,
    val synonyms: List<String> = emptyList(),
    val relatedTerms: List<String> = emptyList(),
    val abbreviations: String = "",
    val sourceReference: String = "",
    val sourceId: Int? = null,
    val verificationStatus: String = "verified",
    val isFavorite: Boolean = false,
    val isInSrs: Boolean = false,
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val nextReviewAt: Long = 0L,
    val lastStudiedAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val ipaSourceUrl: String = "",
    val ipaLicense: String = ""
) {
    val isLearned: Boolean get() = repetitions >= 1
    val isMastered: Boolean get() = repetitions >= 3
    val isDifficult: Boolean
        get() = easeFactor < 2.2f || difficultyLevel in setOf("Advanced", "Expert", "Research Level")
    val hasTajikTranslation: Boolean get() = translationTg.isNotBlank()

    fun displayTranslation(language: TranslationLanguage = TranslationLanguage.RU): String =
        if (language == TranslationLanguage.TJ && translationTg.isNotBlank()) translationTg else translationRu

    fun isDue(nowMillis: Long): Boolean = isInSrs && nextReviewAt <= nowMillis
}

data class AcademicTermDetail(
    val term: AcademicTerm,
    val senses: List<WordSense>,
    val relations: List<WordRelation>,
    val source: WordSource?
)

data class AcademicMajorCategorySummary(
    val majorCategory: String,
    val categoryId: Int,
    val termCount: Int,
    val learnedCount: Int
)

data class AcademicDomainSummary(
    val domain: String,
    val majorCategory: String,
    val termCount: Int
)

data class AcademicDashboardSummary(
    val totalVerifiedTerms: Int,
    val studiedCount: Int,
    val favoritesCount: Int,
    val difficultCount: Int,
    val dueSrsCount: Int,
    val majorCategories: List<AcademicMajorCategorySummary>,
    val domains: List<AcademicDomainSummary>,
    val recentlyStudied: List<AcademicTerm>
)

data class PaginatedAcademicTerms(
    val items: List<AcademicTerm>,
    val totalMatchingCount: Int,
    val page: Int,
    val pageSize: Int
) {
    val hasMore: Boolean get() = (page + 1) * pageSize < totalMatchingCount
}
