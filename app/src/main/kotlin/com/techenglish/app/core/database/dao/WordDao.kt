package com.techenglish.app.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.techenglish.app.core.database.entity.WordEntity
import kotlinx.coroutines.flow.Flow

data class WordWithDetails(
    @Embedded val word: WordEntity,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "language_name") val languageName: String?
)

@Dao
interface WordDao {
    @Query(
        """
        SELECT w.*, c.name AS category_name, pl.name AS language_name
        FROM words w
        INNER JOIN categories c ON c.id = w.category_id
        LEFT JOIN programming_languages pl ON pl.id = w.language_id
        ORDER BY w.term COLLATE NOCASE ASC
        """
    )
    fun observeAllWithDetails(): Flow<List<WordWithDetails>>

    @Query(
        """
        SELECT w.*, c.name AS category_name, pl.name AS language_name
        FROM words w
        INNER JOIN categories c ON c.id = w.category_id
        LEFT JOIN programming_languages pl ON pl.id = w.language_id
        WHERE w.next_review_at <= :nowMillis
        ORDER BY 
            CASE WHEN w.next_review_at = 0 THEN 1 ELSE 0 END ASC,
            w.next_review_at ASC,
            w.id ASC
        LIMIT :limit
        """
    )
    fun observeDueWords(nowMillis: Long, limit: Int): Flow<List<WordWithDetails>>

    @Query(
        """
        SELECT w.*, c.name AS category_name, pl.name AS language_name
        FROM words w
        INNER JOIN categories c ON c.id = w.category_id
        LEFT JOIN programming_languages pl ON pl.id = w.language_id
        WHERE w.next_review_at <= :nowMillis
        ORDER BY 
            CASE WHEN w.next_review_at = 0 THEN 1 ELSE 0 END ASC,
            w.next_review_at ASC,
            w.id ASC
        LIMIT :limit
        """
    )
    suspend fun getDueWords(nowMillis: Long, limit: Int): List<WordWithDetails>

    @Query(
        """
        SELECT w.*, c.name AS category_name, pl.name AS language_name
        FROM words w
        INNER JOIN categories c ON c.id = w.category_id
        LEFT JOIN programming_languages pl ON pl.id = w.language_id
        WHERE w.next_review_at <= :nowMillis AND w.category_id = :categoryId
        ORDER BY 
            CASE WHEN w.next_review_at = 0 THEN 1 ELSE 0 END ASC,
            w.next_review_at ASC
        LIMIT :limit
        """
    )
    suspend fun getDueWordsByCategory(nowMillis: Long, categoryId: Int, limit: Int): List<WordWithDetails>

    @Query("SELECT COUNT(*) FROM words WHERE next_review_at <= :nowMillis")
    fun observeDueCount(nowMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE next_review_at <= :nowMillis")
    suspend fun getDueCount(nowMillis: Long): Int

    @Query("SELECT COUNT(*) FROM words")
    fun observeTotalWordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE repetitions >= 1")
    fun observeLearnedWordCount(): Flow<Int>

    @Query(
        """
        SELECT w.*, c.name AS category_name, pl.name AS language_name
        FROM words w
        INNER JOIN categories c ON c.id = w.category_id
        LEFT JOIN programming_languages pl ON pl.id = w.language_id
        WHERE w.normalized_search LIKE '%' || :query || '%'
           OR LOWER(w.term) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(w.translation) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(w.explanation_ru) LIKE '%' || LOWER(:query) || '%'
        ORDER BY w.term COLLATE NOCASE ASC
        """
    )
    fun searchWords(query: String): Flow<List<WordWithDetails>>

    @Query(
        """
        SELECT w.*, c.name AS category_name, pl.name AS language_name
        FROM words w
        INNER JOIN categories c ON c.id = w.category_id
        LEFT JOIN programming_languages pl ON pl.id = w.language_id
        WHERE LOWER(w.term) = LOWER(:term)
        LIMIT 1
        """
    )
    suspend fun findByExactTerm(term: String): WordWithDetails?

    @Query(
        """
        SELECT w.*, c.name AS category_name, pl.name AS language_name
        FROM words w
        INNER JOIN categories c ON c.id = w.category_id
        LEFT JOIN programming_languages pl ON pl.id = w.language_id
        WHERE LOWER(w.term) LIKE LOWER(:prefix) || '%'
        ORDER BY LENGTH(w.term) ASC
        LIMIT 1
        """
    )
    suspend fun findByTermPrefix(prefix: String): WordWithDetails?

    @Query("SELECT * FROM words WHERE id = :wordId")
    suspend fun getById(wordId: Int): WordEntity?

    @Query("SELECT * FROM words WHERE repetitions >= 1 OR next_review_at > 0")
    suspend fun getStudiedWords(): List<WordEntity>

    @Query("SELECT * FROM words WHERE is_favorite = 1")
    suspend fun getFavoriteWords(): List<WordEntity>

    @Query("SELECT * FROM words WHERE is_custom = 1")
    suspend fun getCustomWords(): List<WordEntity>

    @Query("SELECT * FROM words WHERE LOWER(term) = LOWER(:term) LIMIT 1")
    suspend fun findEntityByNormalizedTerm(term: String): WordEntity?

    @Query(
        """
        SELECT w.*, c.name AS category_name, pl.name AS language_name
        FROM words w
        INNER JOIN categories c ON c.id = w.category_id
        LEFT JOIN programming_languages pl ON pl.id = w.language_id
        WHERE w.id = :wordId
        """
    )
    suspend fun getByIdWithDetails(wordId: Int): WordWithDetails?

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWords(limit: Int): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Update
    suspend fun update(word: WordEntity)

    @Query(
        """
        UPDATE words
        SET repetitions = :repetitions,
            ease_factor = :easeFactor,
            interval_days = :intervalDays,
            next_review_at = :nextReviewAt
        WHERE id = :wordId
        """
    )
    suspend fun updateSrsState(
        wordId: Int,
        repetitions: Int,
        easeFactor: Float,
        intervalDays: Int,
        nextReviewAt: Long
    )

    @Query("UPDATE words SET is_favorite = :isFavorite WHERE id = :wordId")
    suspend fun updateFavorite(wordId: Int, isFavorite: Boolean)

    @Query("DELETE FROM words WHERE id = :wordId AND is_custom = 1")
    suspend fun deleteCustomWord(wordId: Int): Int

    @Query("DELETE FROM words WHERE id = :wordId")
    suspend fun deleteById(wordId: Int): Int
}
