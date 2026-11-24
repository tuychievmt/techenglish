package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_phrases")
data class WorkPhraseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "phrase_en")
    val phraseEn: String,
    @ColumnInfo(name = "translation_ru")
    val translationRu: String,
    @ColumnInfo(name = "context_ru")
    val contextRu: String,
    @ColumnInfo(name = "difficulty")
    val difficulty: String
)
