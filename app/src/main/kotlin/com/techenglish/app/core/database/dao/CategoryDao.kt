package com.techenglish.app.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.techenglish.app.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

data class CategoryProgressRow(
    @ColumnInfo(name = "category_id") val categoryId: Int,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "category_icon") val categoryIcon: String,
    @ColumnInfo(name = "total_words") val totalWords: Int,
    @ColumnInfo(name = "learned_words") val learnedWords: Int,
    @ColumnInfo(name = "mastered_words") val masteredWords: Int
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY is_system ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY is_system ASC, id ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Int): CategoryEntity?

    @Query("SELECT * FROM categories WHERE is_system = 1 LIMIT 1")
    suspend fun getSystemCategory(): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    /**
     * Enforces protection of system category («Мои слова») per Spec §3.3.
     */
    @Query("DELETE FROM categories WHERE id = :id AND is_system = 0")
    suspend fun deleteNonSystemCategory(id: Int): Int

    @Query(
        """
        SELECT 
            c.id AS category_id,
            c.name AS category_name,
            c.icon AS category_icon,
            COUNT(w.id) AS total_words,
            SUM(CASE WHEN w.repetitions >= 1 THEN 1 ELSE 0 END) AS learned_words,
            SUM(CASE WHEN w.repetitions >= 3 THEN 1 ELSE 0 END) AS mastered_words
        FROM categories c
        LEFT JOIN words w ON w.category_id = c.id
        GROUP BY c.id
        ORDER BY c.is_system ASC, c.id ASC
        """
    )
    fun observeCategoryProgress(): Flow<List<CategoryProgressRow>>
}
