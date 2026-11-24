package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_imported_docs",
    indices = [
        Index(name = "idx_custom_docs_created", value = ["created_at"])
    ]
)
data class CustomImportedDocEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "source_file_name", defaultValue = "''")
    val sourceFileName: String = "",
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "detected_terms_json", defaultValue = "'[]'")
    val detectedTermsJson: String = "[]",
    @ColumnInfo(name = "detected_terms_count", defaultValue = "0")
    val detectedTermsCount: Int = 0,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = 0L
)

@Entity(
    tableName = "time_attack_records",
    indices = [
        Index(name = "idx_time_attack_score", value = ["score"])
    ]
)
data class TimeAttackRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "mode")
    val mode: String,
    @ColumnInfo(name = "score")
    val score: Int,
    @ColumnInfo(name = "correct_count")
    val correctCount: Int,
    @ColumnInfo(name = "total_answered")
    val totalAnswered: Int,
    @ColumnInfo(name = "xp_earned")
    val xpEarned: Int,
    @ColumnInfo(name = "duration_seconds", defaultValue = "60")
    val durationSeconds: Int = 60,
    @ColumnInfo(name = "achieved_at")
    val achievedAt: Long
)
