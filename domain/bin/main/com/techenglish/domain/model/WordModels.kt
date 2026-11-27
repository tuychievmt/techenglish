package com.techenglish.domain.model

data class Category(
    val id: Int,
    val name: String,
    val icon: String,
    val isSystem: Boolean = false
)

enum class ReviewQuality(
    val q: Int,
    val labelRu: String
) {
    FORGOT(q = 1, labelRu = "Не помню"),
    HARD(q = 3, labelRu = "Сложно"),
    GOOD(q = 4, labelRu = "Хорошо"),
    EASY(q = 5, labelRu = "Легко");

    val isCorrect: Boolean get() = q >= 3
}

data class Sm2Result(
    val repetitions: Int,
    val easeFactor: Float,
    val intervalDays: Int,
    val nextReviewAt: Long
)

data class Word(
    val id: Int,
    val categoryId: Int,
    val categoryName: String,
    val languageId: Int? = null,
    val languageName: String? = null,
    val term: String,
    val transcription: String,
    val translation: String,
    val example: String,
    val explanationRu: String,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val nextReviewAt: Long = 0L
) {
    val isNew: Boolean get() = repetitions == 0 && nextReviewAt == 0L
    val isLearned: Boolean get() = repetitions >= 1
    val isMastered: Boolean get() = repetitions >= 3

    fun isDue(nowMillis: Long): Boolean = nextReviewAt <= nowMillis
}
