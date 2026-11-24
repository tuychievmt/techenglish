package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_logs",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ScenarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["scenario_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = DocArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CodeSnippetEntity::class,
            parentColumns = ["id"],
            childColumns = ["snippet_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(name = "idx_logs_timestamp", value = ["timestamp"]),
        Index(name = "idx_logs_word", value = ["word_id"]),
        Index(name = "idx_logs_scenario", value = ["scenario_id"]),
        Index(name = "idx_logs_article", value = ["article_id"]),
        Index(name = "idx_logs_snippet", value = ["snippet_id"])
    ]
)
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "word_id")
    val wordId: Int? = null,
    @ColumnInfo(name = "scenario_id")
    val scenarioId: Int? = null,
    @ColumnInfo(name = "article_id")
    val articleId: Int? = null,
    @ColumnInfo(name = "snippet_id")
    val snippetId: Int? = null,
    @ColumnInfo(name = "action_type")
    val actionType: String,
    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,
    @ColumnInfo(name = "xp_earned", defaultValue = "10")
    val xpEarned: Int = 10,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
