package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

fun buildWordNormalizedSearch(
    term: String,
    translation: String = "",
    explanationRu: String = ""
): String = listOf(term, translation, explanationRu)
    .filter { it.isNotBlank() }
    .joinToString(" ")
    .lowercase(Locale.ROOT)
    .replace(Regex("\\s+"), " ")
    .trim()

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProgrammingLanguageEntity::class,
            parentColumns = ["id"],
            childColumns = ["language_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(name = "idx_words_next_review", value = ["next_review_at"]),
        Index(name = "idx_words_category", value = ["category_id"]),
        Index(name = "idx_words_language", value = ["language_id"]),
        Index(name = "idx_words_term_lang", value = ["term", "language_id"])
    ]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "category_id")
    val categoryId: Int,
    @ColumnInfo(name = "language_id")
    val languageId: Int? = null,
    @ColumnInfo(name = "term")
    val term: String,
    @ColumnInfo(name = "transcription")
    val transcription: String,
    @ColumnInfo(name = "translation")
    val translation: String,
    @ColumnInfo(name = "example")
    val example: String,
    @ColumnInfo(name = "explanation_ru")
    val explanationRu: String,
    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_custom", defaultValue = "0")
    val isCustom: Boolean = false,
    @ColumnInfo(name = "repetitions", defaultValue = "0")
    val repetitions: Int = 0,
    @ColumnInfo(name = "ease_factor", defaultValue = "2.5")
    val easeFactor: Float = 2.5f,
    @ColumnInfo(name = "interval_days", defaultValue = "0")
    val intervalDays: Int = 0,
    @ColumnInfo(name = "next_review_at", defaultValue = "0")
    val nextReviewAt: Long = 0L,
    @ColumnInfo(name = "normalized_search", defaultValue = "''")
    val normalizedSearch: String = buildWordNormalizedSearch(
        term = term,
        translation = translation,
        explanationRu = explanationRu
    )
)
