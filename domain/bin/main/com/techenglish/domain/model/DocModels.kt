package com.techenglish.domain.model

data class GlossaryEntry(
    val term: String,
    val transcription: String,
    val translation: String,
    val explanationRu: String,
    val example: String
)

data class DocArticle(
    val id: Int,
    val title: String,
    val techTag: String,
    val content: String,
    val explanationRu: String,
    val level: String,
    val readingProgress: Float,
    val isCompleted: Boolean,
    val glossary: List<GlossaryEntry>,
    val sourceUrl: String = "",
    val license: String = ""
)

data class WordLookupResult(
    val term: String,
    val transcription: String,
    val translation: String,
    val explanationRu: String,
    val example: String,
    val existingWordId: Int? = null
) {
    val isAlreadyInVocabulary: Boolean get() = existingWordId != null
}
