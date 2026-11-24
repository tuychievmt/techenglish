package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "code_snippets",
    foreignKeys = [
        ForeignKey(
            entity = ProgrammingLanguageEntity::class,
            parentColumns = ["id"],
            childColumns = ["language_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(name = "idx_snippets_lang_task", value = ["language_id", "task_key"], unique = true)
    ]
)
data class CodeSnippetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "language_id")
    val languageId: Int,
    @ColumnInfo(name = "task_key")
    val taskKey: String,
    @ColumnInfo(name = "task_title_en")
    val taskTitleEn: String,
    @ColumnInfo(name = "task_title_ru")
    val taskTitleRu: String,
    @ColumnInfo(name = "code")
    val code: String,
    @ColumnInfo(name = "explanation_en")
    val explanationEn: String,
    @ColumnInfo(name = "explanation_ru")
    val explanationRu: String,
    @ColumnInfo(name = "line_explanations_json")
    val lineExplanationsJson: String,
    @ColumnInfo(name = "keywords_json")
    val keywordsJson: String
)
