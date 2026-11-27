package com.techenglish.app.core.database

import androidx.room.withTransaction
import com.techenglish.app.core.database.entity.AcademicTermEntity
import com.techenglish.app.core.database.entity.CodeSnippetEntity
import com.techenglish.app.core.database.entity.DocArticleEntity
import com.techenglish.app.core.database.entity.ScenarioEntity
import com.techenglish.app.core.database.entity.SubcategoryEntity
import com.techenglish.app.core.database.entity.WordSourceEntity
import com.techenglish.app.core.database.entity.WorkPhraseEntity
import com.techenglish.app.core.database.entity.buildAcademicNormalizedSearch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ContentPackTermDto(
    val term: String,
    @SerialName("normalized_term") val normalizedTerm: String = "",
    @SerialName("phonetic_transcription") val phoneticTranscription: String = "",
    @SerialName("ipa_source_url") val ipaSourceUrl: String = "",
    @SerialName("ipa_license") val ipaLicense: String = "",
    @SerialName("part_of_speech") val partOfSpeech: String = "noun",
    @SerialName("translation_ru") val translationRu: String,
    @SerialName("translation_tg") val translationTg: String = "",
    @SerialName("definition_en") val definitionEn: String,
    @SerialName("definition_ru") val definitionRu: String,
    @SerialName("academic_explanation_en") val academicExplanationEn: String = "",
    @SerialName("academic_explanation_ru") val academicExplanationRu: String = "",
    @SerialName("practical_explanation") val practicalExplanation: String = "",
    @SerialName("example_en") val exampleEn: String = "",
    @SerialName("example_ru") val exampleRu: String = "",
    @SerialName("major_category") val majorCategory: String,
    @SerialName("category_id") val categoryId: Int = 101,
    @SerialName("subcategory_id") val subcategoryId: Int = 1,
    val domain: String,
    val subdomain: String = "",
    @SerialName("difficulty_level") val difficultyLevel: String = "Intermediate",
    @SerialName("cefr_level") val cefrLevel: String = "B2",
    val synonyms: String = "",
    @SerialName("related_terms") val relatedTerms: String = "",
    val abbreviations: String = "",
    @SerialName("source_reference") val sourceReference: String = "",
    @SerialName("source_id") val sourceId: Int? = null,
    @SerialName("verification_status") val verificationStatus: String = "verified",
    @SerialName("updated_at") val updatedAt: Long = 0L
)

@Serializable
data class ContentPackDocArticleDto(
    val id: Int,
    val title: String,
    @SerialName("tech_tag") val techTag: String,
    val content: String,
    @SerialName("explanation_ru") val explanationRu: String,
    val level: String = "B2",
    @SerialName("glossary_json") val glossaryJson: String = "{}",
    @SerialName("source_url") val sourceUrl: String = "",
    val license: String = "CC-BY-4.0"
)

@Serializable
data class ContentPackScenarioDto(
    val id: Int,
    val type: String,
    @SerialName("exercise_type") val exerciseType: String = "multiple_choice",
    val title: String,
    val prompt: String,
    @SerialName("prompt_ru") val promptRu: String,
    val solution: String,
    @SerialName("options_json") val optionsJson: String = "[]",
    @SerialName("explanation_ru") val explanationRu: String = "",
    @SerialName("useful_expressions_json") val usefulExpressionsJson: String = "[]",
    val difficulty: String = "Intermediate"
)

@Serializable
data class ContentPackWorkPhraseDto(
    val id: Int,
    val category: String,
    @SerialName("phrase_en") val phraseEn: String,
    @SerialName("translation_ru") val translationRu: String,
    @SerialName("context_ru") val contextRu: String = "",
    val difficulty: String = "B2"
)

@Serializable
data class ContentPackCodeSnippetDto(
    val id: Int = 0,
    @SerialName("language_id") val languageId: Int,
    @SerialName("task_key") val taskKey: String,
    @SerialName("task_title_en") val taskTitleEn: String,
    @SerialName("task_title_ru") val taskTitleRu: String,
    val code: String,
    @SerialName("explanation_en") val explanationEn: String = "",
    @SerialName("explanation_ru") val explanationRu: String = "",
    @SerialName("line_explanations_json") val lineExplanationsJson: String = "[]",
    @SerialName("keywords_json") val keywordsJson: String = "[]"
)

@Serializable
data class ContentPackJsonDto(
    @SerialName("pack_id") val packId: String,
    val version: Int = 3,
    val domain: String = "",
    val terms: List<ContentPackTermDto> = emptyList(),
    @SerialName("doc_articles") val docArticles: List<ContentPackDocArticleDto> = emptyList(),
    val scenarios: List<ContentPackScenarioDto> = emptyList(),
    @SerialName("work_phrases") val workPhrases: List<ContentPackWorkPhraseDto> = emptyList(),
    @SerialName("code_snippets") val codeSnippets: List<ContentPackCodeSnippetDto> = emptyList()
)

data class ContentPackPayload(
    val packId: String = "default_pack",
    val sources: List<WordSourceEntity> = emptyList(),
    val subcategories: List<SubcategoryEntity> = emptyList(),
    val academicTerms: List<AcademicTermEntity> = emptyList(),
    val docArticles: List<DocArticleEntity> = emptyList(),
    val scenarios: List<ScenarioEntity> = emptyList(),
    val workPhrases: List<WorkPhraseEntity> = emptyList(),
    val codeSnippets: List<CodeSnippetEntity> = emptyList()
)

data class ContentImportReport(
    val packId: String,
    val insertedTerms: Int,
    val updatedTermsPreservingSrs: Int,
    val skippedUnverifiedTerms: Int,
    val upsertedDocs: Int,
    val upsertedScenarios: Int,
    val upsertedPhrases: Int,
    val upsertedSnippets: Int
)

/**
 * Idempotent content delivery & delta-import engine (Errata E3).
 *
 * Uses `normalized_term` as the stable identity key for `academic_terms` and performs
 * content-only UPSERTs that NEVER overwrite user SRS progress fields:
 * `repetitions`, `ease_factor`, `interval_days`, `next_review_at`, `last_studied_at`, `is_favorite`, `is_in_srs`.
 *
 * Used for:
 * 1. Upgrading existing v2 installations after `MIGRATION_2_3`.
 * 2. Importing Play Asset Delivery (PAD) domain packs.
 * 3. Importing OTA content packs (Stage 3, ФТ-14.5).
 */
@Singleton
class ContentPackImporter @Inject constructor(
    private val database: TechEnglishDatabase
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun importPayload(payload: ContentPackPayload): ContentImportReport {
        val academicDao = database.academicTermDao()
        val docDao = database.docArticleDao()
        val scenarioDao = database.scenarioDao()
        val phraseDao = database.workPhraseDao()
        val snippetDao = database.codeSnippetDao()

        var insertedTerms = 0
        var updatedTerms = 0
        var skippedUnverified = 0
        var upsertedDocs = 0

        database.withTransaction {
            if (payload.sources.isNotEmpty()) {
                academicDao.insertAllSources(payload.sources)
            }
            if (payload.subcategories.isNotEmpty()) {
                academicDao.insertAllSubcategories(payload.subcategories)
            }

            for (candidate in payload.academicTerms) {
                if (!candidate.verificationStatus.equals("verified", ignoreCase = true)) {
                    skippedUnverified++
                    continue
                }
                val normKey = candidate.normalizedTerm.trim().lowercase().replace(Regex("\\s+"), " ")
                if (normKey.isEmpty()) {
                    skippedUnverified++
                    continue
                }
                val existing = academicDao.findByNormalizedTerm(normKey)
                if (existing == null) {
                    academicDao.insertTerm(
                        candidate.copy(
                            id = 0,
                            normalizedTerm = normKey,
                            verificationStatus = "verified"
                        )
                    )
                    insertedTerms++
                } else {
                    val effectiveTg = candidate.translationTg.ifBlank { existing.translationTg }
                    val updatedNormalizedSearch = buildAcademicNormalizedSearch(
                        term = candidate.term,
                        abbreviations = candidate.abbreviations,
                        translationRu = candidate.translationRu,
                        translationTg = effectiveTg,
                        synonyms = candidate.synonyms,
                        definitionEn = candidate.definitionEn,
                        definitionRu = candidate.definitionRu
                    )
                    academicDao.updateContentPreservingSrs(
                        normalizedTerm = normKey,
                        term = candidate.term,
                        phoneticTranscription = candidate.phoneticTranscription,
                        ipaSourceUrl = candidate.ipaSourceUrl,
                        ipaLicense = candidate.ipaLicense,
                        partOfSpeech = candidate.partOfSpeech,
                        translationRu = candidate.translationRu,
                        translationTg = candidate.translationTg,
                        definitionEn = candidate.definitionEn,
                        definitionRu = candidate.definitionRu,
                        academicExplanationEn = candidate.academicExplanationEn,
                        academicExplanationRu = candidate.academicExplanationRu,
                        practicalExplanation = candidate.practicalExplanation,
                        exampleEn = candidate.exampleEn,
                        exampleRu = candidate.exampleRu,
                        majorCategory = candidate.majorCategory,
                        categoryId = candidate.categoryId,
                        subcategoryId = candidate.subcategoryId,
                        domain = candidate.domain,
                        subdomain = candidate.subdomain,
                        difficultyLevel = candidate.difficultyLevel,
                        cefrLevel = candidate.cefrLevel,
                        synonyms = candidate.synonyms,
                        relatedTerms = candidate.relatedTerms,
                        abbreviations = candidate.abbreviations,
                        sourceReference = candidate.sourceReference,
                        sourceId = candidate.sourceId,
                        verificationStatus = "verified",
                        updatedAt = if (candidate.updatedAt > 0L) candidate.updatedAt else existing.updatedAt,
                        normalizedSearch = updatedNormalizedSearch
                    )
                    updatedTerms++
                }
            }

            for (article in payload.docArticles) {
                val existingArticle = docDao.getById(article.id)
                if (existingArticle == null) {
                    docDao.insertAll(listOf(article))
                } else {
                    docDao.updateContentPreservingProgress(
                        id = article.id,
                        title = article.title,
                        techTag = article.techTag,
                        content = article.content,
                        explanationRu = article.explanationRu,
                        level = article.level,
                        glossaryJson = article.glossaryJson,
                        sourceUrl = article.sourceUrl,
                        license = article.license
                    )
                }
                upsertedDocs++
            }

            if (payload.scenarios.isNotEmpty()) {
                scenarioDao.upsertAll(payload.scenarios)
            }
            if (payload.workPhrases.isNotEmpty()) {
                phraseDao.upsertAll(payload.workPhrases)
            }
            if (payload.codeSnippets.isNotEmpty()) {
                snippetDao.upsertAll(payload.codeSnippets)
            }
        }

        return ContentImportReport(
            packId = payload.packId,
            insertedTerms = insertedTerms,
            updatedTermsPreservingSrs = updatedTerms,
            skippedUnverifiedTerms = skippedUnverified,
            upsertedDocs = upsertedDocs,
            upsertedScenarios = payload.scenarios.size,
            upsertedPhrases = payload.workPhrases.size,
            upsertedSnippets = payload.codeSnippets.size
        )
    }

    suspend fun importFromJson(
        jsonString: String,
        expectedSha256Hex: String? = null
    ): ContentImportReport {
        if (!expectedSha256Hex.isNullOrBlank()) {
            val actualHash = sha256Hex(jsonString.toByteArray(Charsets.UTF_8))
            require(actualHash.equals(expectedSha256Hex, ignoreCase = true)) {
                "Content pack SHA-256 mismatch: expected=$expectedSha256Hex, actual=$actualHash"
            }
        }
        val dto = json.decodeFromString<ContentPackJsonDto>(jsonString)
        val entities = dto.terms.map { t ->
            val norm = t.normalizedTerm.ifBlank {
                t.term.trim().lowercase().replace(Regex("\\s+"), " ")
            }
            AcademicTermEntity(
                term = t.term,
                normalizedTerm = norm,
                phoneticTranscription = t.phoneticTranscription,
                ipaSourceUrl = t.ipaSourceUrl,
                ipaLicense = t.ipaLicense,
                partOfSpeech = t.partOfSpeech,
                translationRu = t.translationRu,
                translationTg = t.translationTg,
                definitionEn = t.definitionEn,
                definitionRu = t.definitionRu,
                academicExplanationEn = t.academicExplanationEn.ifBlank { t.definitionEn },
                academicExplanationRu = t.academicExplanationRu.ifBlank { t.definitionRu },
                practicalExplanation = t.practicalExplanation.ifBlank { t.definitionRu },
                exampleEn = t.exampleEn.ifBlank { "Engineering teams apply ${t.term} in ${t.domain}." },
                exampleRu = t.exampleRu.ifBlank { "Инженеры применяют «${t.translationRu}» в области «${t.domain}»." },
                majorCategory = t.majorCategory,
                categoryId = t.categoryId,
                subcategoryId = t.subcategoryId,
                domain = t.domain,
                subdomain = t.subdomain.ifBlank { t.domain },
                difficultyLevel = t.difficultyLevel,
                cefrLevel = t.cefrLevel,
                synonyms = t.synonyms,
                relatedTerms = t.relatedTerms,
                abbreviations = t.abbreviations,
                sourceReference = t.sourceReference,
                sourceId = t.sourceId,
                verificationStatus = t.verificationStatus,
                updatedAt = t.updatedAt
            )
        }
        val docEntities = dto.docArticles.map { d ->
            DocArticleEntity(
                id = d.id,
                title = d.title,
                techTag = d.techTag,
                content = d.content,
                explanationRu = d.explanationRu,
                level = d.level,
                glossaryJson = d.glossaryJson,
                sourceUrl = d.sourceUrl,
                license = d.license
            )
        }
        val scenarioEntities = dto.scenarios.map { s ->
            ScenarioEntity(
                id = s.id,
                type = s.type,
                exerciseType = s.exerciseType,
                title = s.title,
                prompt = s.prompt,
                promptRu = s.promptRu,
                solution = s.solution,
                optionsJson = s.optionsJson,
                explanationRu = s.explanationRu,
                usefulExpressionsJson = s.usefulExpressionsJson,
                difficulty = s.difficulty
            )
        }
        val phraseEntities = dto.workPhrases.map { w ->
            WorkPhraseEntity(
                id = w.id,
                category = w.category,
                phraseEn = w.phraseEn,
                translationRu = w.translationRu,
                contextRu = w.contextRu,
                difficulty = w.difficulty
            )
        }
        val snippetEntities = dto.codeSnippets.map { c ->
            CodeSnippetEntity(
                id = c.id,
                languageId = c.languageId,
                taskKey = c.taskKey,
                taskTitleEn = c.taskTitleEn,
                taskTitleRu = c.taskTitleRu,
                code = c.code,
                explanationEn = c.explanationEn,
                explanationRu = c.explanationRu,
                lineExplanationsJson = c.lineExplanationsJson,
                keywordsJson = c.keywordsJson
            )
        }
        return importPayload(
            ContentPackPayload(
                packId = dto.packId,
                academicTerms = entities,
                docArticles = docEntities,
                scenarios = scenarioEntities,
                workPhrases = phraseEntities,
                codeSnippets = snippetEntities
            )
        )
    }

    companion object {
        fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
