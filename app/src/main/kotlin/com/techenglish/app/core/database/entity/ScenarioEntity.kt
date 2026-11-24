package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scenarios")
data class ScenarioEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "exercise_type")
    val exerciseType: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "prompt")
    val prompt: String,
    @ColumnInfo(name = "prompt_ru")
    val promptRu: String,
    @ColumnInfo(name = "solution")
    val solution: String,
    @ColumnInfo(name = "options_json")
    val optionsJson: String,
    @ColumnInfo(name = "explanation_ru")
    val explanationRu: String,
    @ColumnInfo(name = "useful_expressions_json")
    val usefulExpressionsJson: String,
    @ColumnInfo(name = "difficulty")
    val difficulty: String
)
