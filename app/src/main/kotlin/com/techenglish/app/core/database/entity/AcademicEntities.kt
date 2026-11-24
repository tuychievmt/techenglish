package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

fun buildAcademicNormalizedSearch(
    term: String,
    abbreviations: String = "",
    translationRu: String = "",
    translationTg: String = "",
    synonyms: String = "",
    definitionEn: String = "",
    definitionRu: String = ""
): String = listOf(
    term,
    abbreviations,
    translationRu,
    translationTg,
    synonyms,
    definitionEn,
    definitionRu
)
    .filter { it.isNotBlank() }
    .joinToString(" ")
    .lowercase(Locale.ROOT)
    .replace(Regex("\\s+"), " ")
    .trim()

@Entity(
    tableName = "subcategories",
    indices = [
        Index(name = "idx_subcategories_slug", value = ["slug"], unique = true),
        Index(name = "idx_subcategories_category", value = ["category_id"])
    ]
)
data class SubcategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "category_id")
    val categoryId: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "slug")
    val slug: String,
    @ColumnInfo(name = "domain")
    val domain: String
)

@Entity(tableName = "word_sources")
data class WordSourceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "source_type")
    val sourceType: String,
    @ColumnInfo(name = "publisher")
    val publisher: String,
    @ColumnInfo(name = "license", defaultValue = "'Public Specification / Standard'")
    val license: String = "Public Specification / Standard"
)

@Entity(
    tableName = "academic_terms",
    foreignKeys = [
        ForeignKey(
            entity = WordSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(name = "idx_academic_normalized_term", value = ["normalized_term"], unique = true),
        Index(name = "idx_academic_category_id", value = ["category_id"]),
        Index(name = "idx_academic_subcategory_id", value = ["subcategory_id"]),
        Index(name = "idx_academic_difficulty", value = ["difficulty_level"]),
        Index(name = "idx_academic_cefr", value = ["cefr_level"]),
        Index(name = "idx_academic_next_review", value = ["next_review_at"]),
        Index(name = "idx_academic_source_id", value = ["source_id"]),
        Index(name = "idx_academic_domain", value = ["domain"]),
        Index(name = "idx_academic_status", value = ["verification_status"]),
        Index(name = "idx_academic_srs_due", value = ["is_in_srs", "next_review_at"])
    ]
)
data class AcademicTermEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "term")
    val term: String,
    @ColumnInfo(name = "normalized_term")
    val normalizedTerm: String,
    @ColumnInfo(name = "phonetic_transcription", defaultValue = "''")
    val phoneticTranscription: String = "",
    @ColumnInfo(name = "part_of_speech")
    val partOfSpeech: String,
    @ColumnInfo(name = "translation_ru")
    val translationRu: String,
    @ColumnInfo(name = "translation_tg", defaultValue = "''")
    val translationTg: String = "",
    @ColumnInfo(name = "definition_en")
    val definitionEn: String,
    @ColumnInfo(name = "definition_ru")
    val definitionRu: String,
    @ColumnInfo(name = "academic_explanation_en")
    val academicExplanationEn: String,
    @ColumnInfo(name = "academic_explanation_ru")
    val academicExplanationRu: String,
    @ColumnInfo(name = "practical_explanation")
    val practicalExplanation: String,
    @ColumnInfo(name = "example_en")
    val exampleEn: String,
    @ColumnInfo(name = "example_ru")
    val exampleRu: String,
    @ColumnInfo(name = "major_category")
    val majorCategory: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Int,
    @ColumnInfo(name = "subcategory_id")
    val subcategoryId: Int,
    @ColumnInfo(name = "domain")
    val domain: String,
    @ColumnInfo(name = "subdomain")
    val subdomain: String,
    @ColumnInfo(name = "difficulty_level")
    val difficultyLevel: String,
    @ColumnInfo(name = "cefr_level")
    val cefrLevel: String,
    @ColumnInfo(name = "synonyms", defaultValue = "''")
    val synonyms: String = "",
    @ColumnInfo(name = "related_terms", defaultValue = "''")
    val relatedTerms: String = "",
    @ColumnInfo(name = "abbreviations", defaultValue = "''")
    val abbreviations: String = "",
    @ColumnInfo(name = "source_reference", defaultValue = "''")
    val sourceReference: String = "",
    @ColumnInfo(name = "source_id")
    val sourceId: Int? = null,
    @ColumnInfo(name = "verification_status", defaultValue = "'verified'")
    val verificationStatus: String = "verified",
    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_in_srs", defaultValue = "0")
    val isInSrs: Boolean = false,
    @ColumnInfo(name = "repetitions", defaultValue = "0")
    val repetitions: Int = 0,
    @ColumnInfo(name = "ease_factor", defaultValue = "2.5")
    val easeFactor: Float = 2.5f,
    @ColumnInfo(name = "interval_days", defaultValue = "0")
    val intervalDays: Int = 0,
    @ColumnInfo(name = "next_review_at", defaultValue = "0")
    val nextReviewAt: Long = 0L,
    @ColumnInfo(name = "last_studied_at", defaultValue = "0")
    val lastStudiedAt: Long = 0L,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0L,
    @ColumnInfo(name = "ipa_source_url", defaultValue = "''")
    val ipaSourceUrl: String = "",
    @ColumnInfo(name = "ipa_license", defaultValue = "''")
    val ipaLicense: String = "",
    @ColumnInfo(name = "normalized_search", defaultValue = "''")
    val normalizedSearch: String = buildAcademicNormalizedSearch(
        term = term,
        abbreviations = abbreviations,
        translationRu = translationRu,
        translationTg = translationTg,
        synonyms = synonyms,
        definitionEn = definitionEn,
        definitionRu = definitionRu
    )
)

@Fts4(
    contentEntity = AcademicTermEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61
)
@Entity(tableName = "academic_terms_fts")
data class AcademicTermFtsEntity(
    @ColumnInfo(name = "normalized_search")
    val normalizedSearch: String
)

@Entity(
    tableName = "word_senses",
    foreignKeys = [
        ForeignKey(
            entity = AcademicTermEntity::class,
            parentColumns = ["id"],
            childColumns = ["term_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(name = "idx_senses_term_id", value = ["term_id"])
    ]
)
data class WordSenseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "term_id")
    val termId: Int,
    @ColumnInfo(name = "sense_number")
    val senseNumber: Int,
    @ColumnInfo(name = "sense_label")
    val senseLabel: String,
    @ColumnInfo(name = "domain")
    val domain: String,
    @ColumnInfo(name = "definition_en")
    val definitionEn: String,
    @ColumnInfo(name = "definition_ru")
    val definitionRu: String,
    @ColumnInfo(name = "example_en")
    val exampleEn: String,
    @ColumnInfo(name = "example_ru")
    val exampleRu: String
)

@Entity(
    tableName = "word_relations",
    foreignKeys = [
        ForeignKey(
            entity = AcademicTermEntity::class,
            parentColumns = ["id"],
            childColumns = ["term_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(name = "idx_relations_term_id", value = ["term_id"])
    ]
)
data class WordRelationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "term_id")
    val termId: Int,
    @ColumnInfo(name = "related_term")
    val relatedTerm: String,
    @ColumnInfo(name = "relation_type")
    val relationType: String
)
