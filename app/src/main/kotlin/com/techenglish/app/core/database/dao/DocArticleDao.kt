package com.techenglish.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.techenglish.app.core.database.entity.DocArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocArticleDao {
    @Query("SELECT * FROM doc_articles ORDER BY id ASC")
    fun observeAll(): Flow<List<DocArticleEntity>>

    @Query("SELECT * FROM doc_articles WHERE id = :id")
    fun observeById(id: Int): Flow<DocArticleEntity?>

    @Query("SELECT * FROM doc_articles WHERE id = :id")
    suspend fun getById(id: Int): DocArticleEntity?

    @Query("UPDATE doc_articles SET reading_progress = :progress, is_completed = :isCompleted WHERE id = :articleId")
    suspend fun updateReadingProgress(articleId: Int, progress: Float, isCompleted: Boolean)

    @Query("SELECT COUNT(*) FROM doc_articles WHERE is_completed = 1")
    fun observeCompletedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<DocArticleEntity>)

    @Query(
        """
        UPDATE doc_articles
        SET title = :title,
            tech_tag = :techTag,
            content = :content,
            explanation_ru = :explanationRu,
            level = :level,
            glossary_json = :glossaryJson,
            source_url = :sourceUrl,
            license = :license
        WHERE id = :id
        """
    )
    suspend fun updateContentPreservingProgress(
        id: Int,
        title: String,
        techTag: String,
        content: String,
        explanationRu: String,
        level: String,
        glossaryJson: String,
        sourceUrl: String,
        license: String
    ): Int
}
