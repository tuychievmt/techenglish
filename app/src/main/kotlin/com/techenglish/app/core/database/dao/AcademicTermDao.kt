package com.techenglish.app.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.techenglish.app.core.database.entity.AcademicTermEntity
import com.techenglish.app.core.database.entity.SubcategoryEntity
import com.techenglish.app.core.database.entity.WordRelationEntity
import com.techenglish.app.core.database.entity.WordSenseEntity
import com.techenglish.app.core.database.entity.WordSourceEntity
import kotlinx.coroutines.flow.Flow
import java.util.Locale

data class DomainTermCountRow(
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "major_category") val majorCategory: String,
    @ColumnInfo(name = "term_count") val termCount: Int
)

data class MajorCategoryCountRow(
    @ColumnInfo(name = "major_category") val majorCategory: String,
    @ColumnInfo(name = "category_id") val categoryId: Int,
    @ColumnInfo(name = "term_count") val termCount: Int,
    @ColumnInfo(name = "learned_count") val learnedCount: Int
)

@Dao
interface AcademicTermDao {

    @Query("SELECT COUNT(*) FROM academic_terms WHERE verification_status = 'verified'")
    fun observeVerifiedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM academic_terms WHERE verification_status = 'verified'")
    suspend fun getVerifiedCount(): Int

    @Query("SELECT COUNT(*) FROM academic_terms WHERE is_favorite = 1 AND verification_status = 'verified'")
    fun observeFavoritesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM academic_terms WHERE repetitions >= 1 AND verification_status = 'verified'")
    fun observeStudiedCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM academic_terms
        WHERE verification_status = 'verified'
          AND (ease_factor < 2.2 OR difficulty_level IN ('Advanced', 'Expert', 'Research Level'))
        """
    )
    fun observeDifficultCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM academic_terms
        WHERE verification_status = 'verified'
          AND is_in_srs = 1
          AND next_review_at <= :nowMillis
        """
    )
    fun observeDueSrsCount(nowMillis: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM academic_terms
        WHERE verification_status = 'verified'
          AND is_in_srs = 1
          AND next_review_at <= :nowMillis
        """
    )
    suspend fun getDueSrsCount(nowMillis: Long): Int

    @Query(
        """
        SELECT major_category, category_id,
               COUNT(*) AS term_count,
               SUM(CASE WHEN repetitions >= 1 THEN 1 ELSE 0 END) AS learned_count
        FROM academic_terms
        WHERE verification_status = 'verified'
        GROUP BY major_category, category_id
        ORDER BY category_id ASC
        """
    )
    fun observeMajorCategoryCounts(): Flow<List<MajorCategoryCountRow>>

    @Query(
        """
        SELECT domain, major_category, COUNT(*) AS term_count
        FROM academic_terms
        WHERE verification_status = 'verified'
        GROUP BY domain
        ORDER BY domain ASC
        """
    )
    fun observeDomainCounts(): Flow<List<DomainTermCountRow>>

    @RawQuery
    suspend fun searchPaginatedRaw(query: SupportSQLiteQuery): List<AcademicTermEntity>

    @RawQuery
    suspend fun countMatchingRaw(query: SupportSQLiteQuery): Int

    /**
     * Indexed & paginated search across 20,000+ to 50,000+ academic terms (SP2-04 / BUG-15 / R6).
     * - Primary fast path: Room @Fts4 external content (`academic_terms_fts`, unicode61 tokenizer)
     *   over pre-lowercased Unicode `normalized_search` (Latin, Cyrillic, Tajik), achieving < 15 ms on 30k terms.
     * - Substring fallback: when FTS4 prefix match returns 0 rows (e.g., mid-word Cyrillic substring),
     *   transparently falls back to `normalized_search LIKE '%query%'`.
     */
    suspend fun searchPaginated(
        normalizedQuery: String,
        majorCategory: String? = null,
        domain: String? = null,
        subdomain: String? = null,
        difficulty: String? = null,
        cefr: String? = null,
        partOfSpeech: String? = null,
        onlyFavorites: Int = 0,
        onlyDue: Int = 0,
        onlyDifficult: Int = 0,
        studyFilter: Int = 0, // 0 = all, 1 = studied, 2 = not studied
        nowMillis: Long = 0L,
        limit: Int = 40,
        offset: Int = 0
    ): List<AcademicTermEntity> {
        val cleanQuery = normalizedQuery.trim().lowercase(Locale.ROOT)
        val ftsQuery = buildFts4MatchExpression(cleanQuery)
        if (ftsQuery != null) {
            val ftsResults = executeSearchPaginated(
                cleanQuery = cleanQuery,
                ftsMatchExpr = ftsQuery,
                useSubstringFallback = false,
                majorCategory = majorCategory,
                domain = domain,
                subdomain = subdomain,
                difficulty = difficulty,
                cefr = cefr,
                partOfSpeech = partOfSpeech,
                onlyFavorites = onlyFavorites,
                onlyDue = onlyDue,
                onlyDifficult = onlyDifficult,
                studyFilter = studyFilter,
                nowMillis = nowMillis,
                limit = limit,
                offset = offset
            )
            if (ftsResults.isNotEmpty()) {
                return ftsResults
            }
            if (offset > 0) {
                val hasAnyFtsMatch = executeSearchPaginated(
                    cleanQuery = cleanQuery,
                    ftsMatchExpr = ftsQuery,
                    useSubstringFallback = false,
                    majorCategory = majorCategory,
                    domain = domain,
                    subdomain = subdomain,
                    difficulty = difficulty,
                    cefr = cefr,
                    partOfSpeech = partOfSpeech,
                    onlyFavorites = onlyFavorites,
                    onlyDue = onlyDue,
                    onlyDifficult = onlyDifficult,
                    studyFilter = studyFilter,
                    nowMillis = nowMillis,
                    limit = 1,
                    offset = 0
                ).isNotEmpty()
                if (hasAnyFtsMatch) {
                    return emptyList()
                }
            }
        }
        return executeSearchPaginated(
            cleanQuery = cleanQuery,
            ftsMatchExpr = null,
            useSubstringFallback = cleanQuery.isNotEmpty(),
            majorCategory = majorCategory,
            domain = domain,
            subdomain = subdomain,
            difficulty = difficulty,
            cefr = cefr,
            partOfSpeech = partOfSpeech,
            onlyFavorites = onlyFavorites,
            onlyDue = onlyDue,
            onlyDifficult = onlyDifficult,
            studyFilter = studyFilter,
            nowMillis = nowMillis,
            limit = limit,
            offset = offset
        )
    }

    private suspend fun executeSearchPaginated(
        cleanQuery: String,
        ftsMatchExpr: String?,
        useSubstringFallback: Boolean,
        majorCategory: String?,
        domain: String?,
        subdomain: String?,
        difficulty: String?,
        cefr: String?,
        partOfSpeech: String?,
        onlyFavorites: Int,
        onlyDue: Int,
        onlyDifficult: Int,
        studyFilter: Int,
        nowMillis: Long,
        limit: Int,
        offset: Int
    ): List<AcademicTermEntity> {
        val (fromClause, whereClause, args) = buildSearchQueryParts(
            cleanQuery = cleanQuery,
            ftsMatchExpr = ftsMatchExpr,
            useSubstringFallback = useSubstringFallback,
            majorCategory = majorCategory,
            domain = domain,
            subdomain = subdomain,
            difficulty = difficulty,
            cefr = cefr,
            partOfSpeech = partOfSpeech,
            onlyFavorites = onlyFavorites,
            onlyDue = onlyDue,
            onlyDifficult = onlyDifficult,
            studyFilter = studyFilter,
            nowMillis = nowMillis
        )
        val escapedPrefix = escapeLikePattern(cleanQuery) + "%"
        val orderBy = if (cleanQuery.isNotEmpty()) {
            args.add(cleanQuery)
            args.add(cleanQuery)
            args.add(escapedPrefix)
            """
            ORDER BY
              CASE
                WHEN t.normalized_term = ? THEN 0
                WHEN LOWER(t.abbreviations) = ? THEN 1
                WHEN t.normalized_term LIKE ? ESCAPE '\' THEN 2
                ELSE 3
              END ASC,
              t.normalized_term ASC
            """.trimIndent()
        } else {
            "ORDER BY t.normalized_term ASC"
        }
        args.add(limit)
        args.add(offset)
        val sql = "SELECT t.* FROM $fromClause WHERE $whereClause $orderBy LIMIT ? OFFSET ?"
        return searchPaginatedRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    suspend fun countMatching(
        normalizedQuery: String,
        majorCategory: String? = null,
        domain: String? = null,
        subdomain: String? = null,
        difficulty: String? = null,
        cefr: String? = null,
        partOfSpeech: String? = null,
        onlyFavorites: Int = 0,
        onlyDue: Int = 0,
        onlyDifficult: Int = 0,
        studyFilter: Int = 0,
        nowMillis: Long = 0L
    ): Int {
        val cleanQuery = normalizedQuery.trim().lowercase(Locale.ROOT)
        val ftsQuery = buildFts4MatchExpression(cleanQuery)
        if (ftsQuery != null) {
            val (fromClause, whereClause, args) = buildSearchQueryParts(
                cleanQuery = cleanQuery,
                ftsMatchExpr = ftsQuery,
                useSubstringFallback = false,
                majorCategory = majorCategory,
                domain = domain,
                subdomain = subdomain,
                difficulty = difficulty,
                cefr = cefr,
                partOfSpeech = partOfSpeech,
                onlyFavorites = onlyFavorites,
                onlyDue = onlyDue,
                onlyDifficult = onlyDifficult,
                studyFilter = studyFilter,
                nowMillis = nowMillis
            )
            val ftsCount = countMatchingRaw(
                SimpleSQLiteQuery("SELECT COUNT(*) FROM $fromClause WHERE $whereClause", args.toTypedArray())
            )
            if (ftsCount > 0) return ftsCount
        }
        val (fromClause, whereClause, args) = buildSearchQueryParts(
            cleanQuery = cleanQuery,
            ftsMatchExpr = null,
            useSubstringFallback = cleanQuery.isNotEmpty(),
            majorCategory = majorCategory,
            domain = domain,
            subdomain = subdomain,
            difficulty = difficulty,
            cefr = cefr,
            partOfSpeech = partOfSpeech,
            onlyFavorites = onlyFavorites,
            onlyDue = onlyDue,
            onlyDifficult = onlyDifficult,
            studyFilter = studyFilter,
            nowMillis = nowMillis
        )
        return countMatchingRaw(
            SimpleSQLiteQuery("SELECT COUNT(*) FROM $fromClause WHERE $whereClause", args.toTypedArray())
        )
    }

    private fun buildFts4MatchExpression(cleanQuery: String): String? {
        if (cleanQuery.isEmpty()) return null
        val tokens = cleanQuery
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(8)
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }

    private fun buildSearchQueryParts(
        cleanQuery: String,
        ftsMatchExpr: String?,
        useSubstringFallback: Boolean,
        majorCategory: String?,
        domain: String?,
        subdomain: String?,
        difficulty: String?,
        cefr: String?,
        partOfSpeech: String?,
        onlyFavorites: Int,
        onlyDue: Int,
        onlyDifficult: Int,
        studyFilter: Int,
        nowMillis: Long
    ): Triple<String, String, MutableList<Any>> {
        val clauses = mutableListOf("t.verification_status = 'verified'")
        val args = mutableListOf<Any>()
        val fromClause = if (ftsMatchExpr != null) {
            clauses.add("academic_terms_fts MATCH ?")
            args.add(ftsMatchExpr)
            "academic_terms t INNER JOIN academic_terms_fts ON t.id = academic_terms_fts.rowid"
        } else {
            "academic_terms t"
        }

        if (!majorCategory.isNullOrEmpty()) {
            clauses.add("t.major_category = ?")
            args.add(majorCategory)
        }
        if (!domain.isNullOrEmpty()) {
            clauses.add("t.domain = ?")
            args.add(domain)
        }
        if (!subdomain.isNullOrEmpty()) {
            clauses.add("t.subdomain = ?")
            args.add(subdomain)
        }
        if (!difficulty.isNullOrEmpty()) {
            clauses.add("t.difficulty_level = ?")
            args.add(difficulty)
        }
        if (!cefr.isNullOrEmpty()) {
            clauses.add("t.cefr_level = ?")
            args.add(cefr)
        }
        if (!partOfSpeech.isNullOrEmpty()) {
            clauses.add("t.part_of_speech = ?")
            args.add(partOfSpeech)
        }
        if (onlyFavorites != 0) {
            clauses.add("t.is_favorite = 1")
        }
        if (onlyDue != 0) {
            clauses.add("t.is_in_srs = 1 AND t.next_review_at <= ?")
            args.add(nowMillis)
        }
        if (onlyDifficult != 0) {
            clauses.add("(t.ease_factor < 2.2 OR t.difficulty_level IN ('Advanced', 'Expert', 'Research Level'))")
        }
        when (studyFilter) {
            1 -> clauses.add("t.repetitions >= 1")
            2 -> clauses.add("t.repetitions = 0")
        }
        if (useSubstringFallback && cleanQuery.isNotEmpty()) {
            val contains = "%${escapeLikePattern(cleanQuery)}%"
            clauses.add("t.normalized_search LIKE ? ESCAPE '\\'")
            args.add(contains)
        }
        return Triple(fromClause, clauses.joinToString(" AND "), args)
    }

    private fun escapeLikePattern(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    @Query("SELECT * FROM academic_terms WHERE id = :id")
    suspend fun getById(id: Int): AcademicTermEntity?

    @Query("SELECT * FROM academic_terms WHERE id = :id")
    fun observeById(id: Int): Flow<AcademicTermEntity?>

    @Query("SELECT * FROM academic_terms WHERE normalized_term = :normalizedTerm LIMIT 1")
    suspend fun findByNormalizedTerm(normalizedTerm: String): AcademicTermEntity?

    @Query(
        """
        SELECT * FROM academic_terms
        WHERE verification_status = 'verified'
          AND normalized_term IN (:normalizedTerms)
        """
    )
    suspend fun findByNormalizedTerms(normalizedTerms: List<String>): List<AcademicTermEntity>

    @Query(
        """
        SELECT * FROM academic_terms
        WHERE verification_status = 'verified'
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    suspend fun getRandomVerifiedTerms(limit: Int): List<AcademicTermEntity>

    @Query(
        """
        UPDATE academic_terms
        SET term = :term,
            phonetic_transcription = CASE WHEN :phoneticTranscription != '' THEN :phoneticTranscription ELSE phonetic_transcription END,
            ipa_source_url = CASE WHEN :ipaSourceUrl != '' THEN :ipaSourceUrl ELSE ipa_source_url END,
            ipa_license = CASE WHEN :ipaLicense != '' THEN :ipaLicense ELSE ipa_license END,
            part_of_speech = :partOfSpeech,
            translation_ru = :translationRu,
            translation_tg = CASE WHEN :translationTg != '' THEN :translationTg ELSE translation_tg END,
            definition_en = :definitionEn,
            definition_ru = :definitionRu,
            academic_explanation_en = :academicExplanationEn,
            academic_explanation_ru = :academicExplanationRu,
            practical_explanation = :practicalExplanation,
            example_en = :exampleEn,
            example_ru = :exampleRu,
            major_category = :majorCategory,
            category_id = :categoryId,
            subcategory_id = :subcategoryId,
            domain = :domain,
            subdomain = :subdomain,
            difficulty_level = :difficultyLevel,
            cefr_level = :cefrLevel,
            synonyms = :synonyms,
            related_terms = :relatedTerms,
            abbreviations = :abbreviations,
            source_reference = :sourceReference,
            source_id = :sourceId,
            verification_status = :verificationStatus,
            updated_at = :updatedAt,
            normalized_search = :normalizedSearch
        WHERE normalized_term = :normalizedTerm
        """
    )
    suspend fun updateContentPreservingSrs(
        normalizedTerm: String,
        term: String,
        phoneticTranscription: String,
        ipaSourceUrl: String,
        ipaLicense: String,
        partOfSpeech: String,
        translationRu: String,
        translationTg: String,
        definitionEn: String,
        definitionRu: String,
        academicExplanationEn: String,
        academicExplanationRu: String,
        practicalExplanation: String,
        exampleEn: String,
        exampleRu: String,
        majorCategory: String,
        categoryId: Int,
        subcategoryId: Int,
        domain: String,
        subdomain: String,
        difficultyLevel: String,
        cefrLevel: String,
        synonyms: String,
        relatedTerms: String,
        abbreviations: String,
        sourceReference: String,
        sourceId: Int?,
        verificationStatus: String,
        updatedAt: Long,
        normalizedSearch: String
    ): Int

    @Query(
        """
        SELECT * FROM academic_terms
        WHERE verification_status = 'verified' AND last_studied_at > 0
        ORDER BY last_studied_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentlyStudied(limit: Int = 10): Flow<List<AcademicTermEntity>>

    /**
     * Fetches due or new academic terms for a specific Advanced SRS mode without forcing the user
     * to study all 10,000+ terms. Prioritizes user's queued SRS items first, then introduces new terms
     * from the selected academic track up to the daily goal limit.
     */
    @Query(
        """
        SELECT * FROM academic_terms
        WHERE verification_status = 'verified'
          AND (:majorCategory IS NULL OR major_category = :majorCategory)
          AND (:onlyCustomQueue = 0 OR is_in_srs = 1 OR is_favorite = 1)
          AND (next_review_at <= :nowMillis)
        ORDER BY
          CASE WHEN is_in_srs = 1 THEN 0 ELSE 1 END ASC,
          CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END ASC,
          CASE WHEN next_review_at > 0 THEN 0 ELSE 1 END ASC,
          next_review_at ASC,
          id ASC
        LIMIT :limit
        """
    )
    suspend fun getSrsDeckForMode(
        majorCategory: String?,
        onlyCustomQueue: Int,
        nowMillis: Long,
        limit: Int
    ): List<AcademicTermEntity>

    @Query("SELECT * FROM word_senses WHERE term_id = :termId ORDER BY sense_number ASC")
    suspend fun getSensesForTerm(termId: Int): List<WordSenseEntity>

    @Query("SELECT * FROM word_relations WHERE term_id = :termId ORDER BY id ASC")
    suspend fun getRelationsForTerm(termId: Int): List<WordRelationEntity>

    @Query("SELECT * FROM word_sources WHERE id = :sourceId LIMIT 1")
    suspend fun getSourceById(sourceId: Int): WordSourceEntity?

    @Query("SELECT * FROM subcategories ORDER BY category_id ASC, id ASC")
    suspend fun getAllSubcategories(): List<SubcategoryEntity>

    @Query("UPDATE academic_terms SET is_favorite = :isFavorite WHERE id = :termId")
    suspend fun updateFavorite(termId: Int, isFavorite: Boolean)

    @Query("UPDATE academic_terms SET is_in_srs = :isInSrs WHERE id = :termId")
    suspend fun updateInSrs(termId: Int, isInSrs: Boolean)

    @Query(
        """
        UPDATE academic_terms
        SET is_in_srs = 1,
            repetitions = :repetitions,
            ease_factor = :easeFactor,
            interval_days = :intervalDays,
            next_review_at = :nextReviewAt,
            last_studied_at = :nowMillis
        WHERE id = :termId
        """
    )
    suspend fun updateSrsProgress(
        termId: Int,
        repetitions: Int,
        easeFactor: Float,
        intervalDays: Int,
        nextReviewAt: Long,
        nowMillis: Long
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerm(term: AcademicTermEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTerms(terms: List<AcademicTermEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSenses(senses: List<WordSenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSources(sources: List<WordSourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSubcategories(subcategories: List<SubcategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRelations(relations: List<WordRelationEntity>)

    @Update
    suspend fun updateTerm(term: AcademicTermEntity)
}
