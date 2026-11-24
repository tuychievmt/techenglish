package com.techenglish.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.techenglish.app.core.database.entity.ScenarioEntity
import com.techenglish.app.core.database.entity.WorkPhraseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioDao {
    @Query("SELECT * FROM scenarios ORDER BY id ASC")
    fun observeAll(): Flow<List<ScenarioEntity>>

    @Query("SELECT * FROM scenarios WHERE type = :type ORDER BY id ASC")
    fun observeByType(type: String): Flow<List<ScenarioEntity>>

    @Query("SELECT * FROM scenarios WHERE id = :id")
    suspend fun getById(id: Int): ScenarioEntity?

    @Query("SELECT * FROM scenarios ORDER BY id ASC")
    suspend fun getAll(): List<ScenarioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scenarios: List<ScenarioEntity>)
}

@Dao
interface WorkPhraseDao {
    @Query("SELECT * FROM work_phrases ORDER BY id ASC")
    fun observeAll(): Flow<List<WorkPhraseEntity>>

    @Query("SELECT * FROM work_phrases ORDER BY id ASC")
    suspend fun getAll(): List<WorkPhraseEntity>

    @Query("SELECT * FROM work_phrases WHERE category = :category ORDER BY id ASC")
    fun observeByCategory(category: String): Flow<List<WorkPhraseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(phrases: List<WorkPhraseEntity>)
}
