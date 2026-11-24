package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doc_articles")
data class DocArticleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "tech_tag")
    val techTag: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "explanation_ru")
    val explanationRu: String,
    @ColumnInfo(name = "level")
    val level: String,
    @ColumnInfo(name = "reading_progress", defaultValue = "0.0")
    val readingProgress: Float = 0f,
    @ColumnInfo(name = "is_completed", defaultValue = "0")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "glossary_json")
    val glossaryJson: String,
    @ColumnInfo(name = "source_url", defaultValue = "''")
    val sourceUrl: String = "",
    @ColumnInfo(name = "license", defaultValue = "'CC-BY-4.0'")
    val license: String = "CC-BY-4.0"
)
