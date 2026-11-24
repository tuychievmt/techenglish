package com.techenglish.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.techenglish.app.core.database.entity.CustomImportedDocEntity
import com.techenglish.app.core.database.entity.TimeAttackRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomImportedDocDao {
    @Query("SELECT * FROM custom_imported_docs ORDER BY created_at DESC, id DESC")
    fun observeAll(): Flow<List<CustomImportedDocEntity>>

    @Query("SELECT * FROM custom_imported_docs ORDER BY created_at DESC, id DESC")
    suspend fun getAll(): List<CustomImportedDocEntity>

    @Query("SELECT * FROM custom_imported_docs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): CustomImportedDocEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doc: CustomImportedDocEntity): Long

    @Query("DELETE FROM custom_imported_docs WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface TimeAttackRecordDao {
    @Query("SELECT * FROM time_attack_records ORDER BY score DESC, achieved_at DESC LIMIT :limit")
    fun observeTopRecords(limit: Int = 20): Flow<List<TimeAttackRecordEntity>>

    @Query("SELECT * FROM time_attack_records ORDER BY score DESC, achieved_at DESC LIMIT :limit")
    suspend fun getTopRecords(limit: Int = 20): List<TimeAttackRecordEntity>

    @Query("SELECT COALESCE(MAX(score), 0) FROM time_attack_records WHERE mode = :mode")
    fun observeBestScoreForMode(mode: String): Flow<Int>

    @Query("SELECT COALESCE(MAX(score), 0) FROM time_attack_records WHERE mode = :mode")
    suspend fun getBestScoreForMode(mode: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TimeAttackRecordEntity): Long
}
