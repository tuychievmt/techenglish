package com.techenglish.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.techenglish.app.core.database.entity.CodeSnippetEntity
import com.techenglish.app.core.database.entity.ProgrammingLanguageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgrammingLanguageDao {
    @Query("SELECT * FROM programming_languages ORDER BY id ASC")
    fun observeAll(): Flow<List<ProgrammingLanguageEntity>>

    @Query("SELECT * FROM programming_languages ORDER BY id ASC")
    suspend fun getAll(): List<ProgrammingLanguageEntity>

    @Query("SELECT * FROM programming_languages WHERE id = :id")
    fun observeById(id: Int): Flow<ProgrammingLanguageEntity?>

    @Query("SELECT * FROM programming_languages WHERE id = :id")
    suspend fun getById(id: Int): ProgrammingLanguageEntity?

    @Query("SELECT * FROM programming_languages WHERE LOWER(slug) = LOWER(:slug) LIMIT 1")
    suspend fun getBySlug(slug: String): ProgrammingLanguageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(languages: List<ProgrammingLanguageEntity>)
}

@Dao
interface CodeSnippetDao {
    @Query("SELECT * FROM code_snippets WHERE language_id = :languageId ORDER BY id ASC")
    fun observeByLanguage(languageId: Int): Flow<List<CodeSnippetEntity>>

    @Query("SELECT * FROM code_snippets WHERE language_id = :languageId ORDER BY id ASC")
    suspend fun getByLanguage(languageId: Int): List<CodeSnippetEntity>

    @Query("SELECT * FROM code_snippets WHERE id = :snippetId")
    suspend fun getById(snippetId: Int): CodeSnippetEntity?

    @Query("SELECT * FROM code_snippets WHERE language_id = :languageId AND task_key = :taskKey LIMIT 1")
    suspend fun getByLanguageAndTask(languageId: Int, taskKey: String): CodeSnippetEntity?

    @Query(
        """
        SELECT s1.task_key FROM code_snippets s1
        INNER JOIN code_snippets s2 ON s1.task_key = s2.task_key
        WHERE s1.language_id = :langAId AND s2.language_id = :langBId
        ORDER BY s1.id ASC
        """
    )
    suspend fun getCommonTaskKeys(langAId: Int, langBId: Int): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snippets: List<CodeSnippetEntity>)
}
