package com.techenglish.domain.model

/**
 * ФТ-7.1 — Read Your Own Docs: matched technical term span inside a user-imported document.
 */
data class DetectedDocTerm(
    val matchedText: String,
    val normalizedTerm: String,
    val startOffset: Int,
    val endOffset: Int,
    val termId: Int? = null,
    val wordId: Int? = null,
    val headword: String,
    val transcription: String = "",
    val translationRu: String,
    val translationTg: String = "",
    val explanationRu: String,
    val domain: String
) {
    fun displayTranslation(language: TranslationLanguage = TranslationLanguage.RU): String =
        if (language == TranslationLanguage.TJ && translationTg.isNotBlank()) translationTg else translationRu
}

data class CustomImportedDoc(
    val id: Int = 0,
    val title: String,
    val sourceFileName: String = "",
    val content: String,
    val detectedTerms: List<DetectedDocTerm> = emptyList(),
    val createdAt: Long = 0L
) {
    val detectedTermsCount: Int get() = detectedTerms.size
}

/**
 * ФТ-7.2 — Interactive «Debug the Log» Trainer models.
 */
data class DebugLogChallenge(
    val id: Int,
    val title: String,
    val stackKind: String,
    val rawLog: String,
    val rootCauseQuestionRu: String,
    val rootCauseOptions: List<String>,
    val correctRootCause: String,
    val reportPromptRu: String,
    val reportTokens: List<String>,
    val expectedReportEn: String,
    val explanationRu: String,
    val xpReward: Int = 25
)

data class DebugLogEvaluation(
    val isRootCauseCorrect: Boolean,
    val isReportCorrect: Boolean,
    val isFullySolved: Boolean,
    val xpEarned: Int,
    val expectedReportEn: String,
    val explanationRu: String
)

/**
 * ФТ-7.3 — Speaking / Shadowing Pronunciation Trainer models.
 */
enum class SpeechEngineAvailability(val isAvailable: Boolean, val statusBannerRu: String?) {
    AVAILABLE_ON_DEVICE(
        isAvailable = true,
        statusBannerRu = null
    ),
    REQUIRES_VOICE_PACK_OR_NETWORK(
        isAvailable = false,
        statusBannerRu = "Нужен голосовой пакет или сеть (локальный движок Android 13+ SpeechRecognizer недоступен)"
    )
}

data class PronunciationWordStatus(
    val referenceWord: String,
    val spokenWord: String?,
    val isCorrect: Boolean
)

data class PronunciationEvaluation(
    val referenceText: String,
    val spokenText: String,
    val accuracyPercent: Int,
    val wordStatuses: List<PronunciationWordStatus>,
    val isPassed: Boolean,
    val xpEarned: Int
)

data class ShadowingPrompt(
    val id: String,
    val categoryLabelRu: String,
    val targetEn: String,
    val ipa: String,
    val translationRu: String,
    val focusNoteRu: String
)

/**
 * ФТ-7.4 — Home Screen Widget (Jetpack Glance) snapshot model.
 */
data class WidgetSnapshot(
    val wordOfTheDayTerm: String,
    val wordOfTheDayIpa: String,
    val wordOfTheDayTranslation: String,
    val dueCardsCount: Int,
    val streakDays: Int,
    val language: TranslationLanguage
)

/**
 * ФТ-7.5 — Time Attack / Speedrun Quiz (60-second mode) models.
 */
enum class TimeAttackMode(val labelRu: String) {
    TERM_TRANSLATION("Перевод IT-терминов"),
    PR_ERROR_SPOTTER("Поиск ошибок в PR");

    companion object {
        fun fromRaw(raw: String): TimeAttackMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TERM_TRANSLATION
    }
}

data class TimeAttackQuestion(
    val id: String,
    val mode: TimeAttackMode,
    val promptEn: String,
    val promptSubtitleRu: String,
    val options: List<String>,
    val correctOption: String
)

data class TimeAttackRecord(
    val id: Int = 0,
    val mode: TimeAttackMode,
    val score: Int,
    val correctCount: Int,
    val totalAnswered: Int,
    val xpEarned: Int,
    val durationSeconds: Int = 60,
    val achievedAt: Long
)

/**
 * ФТ-7.6 — Deck Import & Export (JSON / CSV / Anki TSV / Anki .apkg) models.
 */
enum class DeckFormat(val extension: String, val labelRu: String) {
    JSON("json", "JSON (TechEnglish)"),
    CSV("csv", "CSV (Таблица)"),
    ANKI_TSV("tsv", "Anki TSV (Tab-Separated)"),
    ANKI_APKG("apkg", "Anki Deck (.apkg)")
}

data class DeckCardEntry(
    val term: String,
    val transcription: String = "",
    val translation: String,
    val example: String = "",
    val explanationRu: String = "",
    val tags: List<String> = emptyList(),
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0
)

data class DeckImportSummary(
    val format: DeckFormat,
    val totalParsed: Int,
    val importedCount: Int,
    val skippedDuplicates: Int
)
