package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "programming_languages",
    indices = [
        Index(value = ["slug"], unique = true),
        Index(value = ["name"], unique = true)
    ]
)
data class ProgrammingLanguageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "slug")
    val slug: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description_en")
    val descriptionEn: String,
    @ColumnInfo(name = "description_ru")
    val descriptionRu: String,
    @ColumnInfo(name = "history_en")
    val historyEn: String,
    @ColumnInfo(name = "history_ru")
    val historyRu: String,
    @ColumnInfo(name = "paradigms")
    val paradigms: String,
    @ColumnInfo(name = "domains")
    val domains: String,
    @ColumnInfo(name = "frameworks")
    val frameworks: String,
    @ColumnInfo(name = "typing_system")
    val typingSystem: String,
    @ColumnInfo(name = "memory_management")
    val memoryManagement: String,
    @ColumnInfo(name = "difficulty")
    val difficulty: String,
    @ColumnInfo(name = "year_created")
    val yearCreated: Int,
    @ColumnInfo(name = "creator")
    val creator: String,
    @ColumnInfo(name = "accent_color_hex")
    val accentColorHex: String
)
