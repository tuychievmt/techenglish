package com.techenglish.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.techenglish.app.core.database.entity.StudyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: StudyLogEntity): Long

    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs WHERE timestamp >= :sinceMillis ORDER BY timestamp DESC")
    fun observeSince(sinceMillis: Long): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<StudyLogEntity>

    @Query("SELECT DISTINCT scenario_id FROM study_logs WHERE scenario_id IS NOT NULL AND is_correct = 1")
    fun observeCompletedScenarioIds(): Flow<List<Int>>

    @Query("SELECT COALESCE(SUM(xp_earned), 0) FROM study_logs")
    fun observeTotalXp(): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_logs WHERE action_type = 'FLASHCARD' AND timestamp >= :startOfDayMillis")
    fun observeTodayReviewedCards(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_logs WHERE timestamp >= :startOfDayMillis")
    fun observeTodayActivityCount(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_logs")
    fun observeTotalLogsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_logs WHERE is_correct = 1")
    fun observeCorrectLogsCount(): Flow<Int>
}
