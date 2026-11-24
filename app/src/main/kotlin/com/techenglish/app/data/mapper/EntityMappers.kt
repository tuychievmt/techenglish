package com.techenglish.app.data.mapper

import com.techenglish.app.core.database.dao.CategoryProgressRow
import com.techenglish.app.core.database.dao.DomainTermCountRow
import com.techenglish.app.core.database.dao.MajorCategoryCountRow
import com.techenglish.app.core.database.dao.WordWithDetails
import com.techenglish.app.core.database.entity.AcademicTermEntity
import com.techenglish.app.core.database.entity.CategoryEntity
import com.techenglish.app.core.database.entity.CodeSnippetEntity
import com.techenglish.app.core.database.entity.CustomImportedDocEntity
import com.techenglish.app.core.database.entity.DocArticleEntity
import com.techenglish.app.core.database.entity.ProgrammingLanguageEntity
import com.techenglish.app.core.database.entity.ScenarioEntity
import com.techenglish.app.core.database.entity.StudyLogEntity
import com.techenglish.app.core.database.entity.TimeAttackRecordEntity
import com.techenglish.app.core.database.entity.WordEntity
import com.techenglish.app.core.database.entity.WordRelationEntity
import com.techenglish.app.core.database.entity.WordSenseEntity
import com.techenglish.app.core.database.entity.WordSourceEntity
import com.techenglish.app.core.database.entity.WorkPhraseEntity
import com.techenglish.app.core.datastore.StoredUserPreferences
import com.techenglish.domain.model.AcademicDomainSummary
import com.techenglish.domain.model.AcademicMajorCategorySummary
import com.techenglish.domain.model.AcademicTerm
import com.techenglish.domain.model.Category
import com.techenglish.domain.model.CategoryProgress
import com.techenglish.domain.model.CodeSnippet
import com.techenglish.domain.model.CustomImportedDoc
import com.techenglish.domain.model.DetectedDocTerm
import com.techenglish.domain.model.DocArticle
import com.techenglish.domain.model.ExerciseType
import com.techenglish.domain.model.GlossaryEntry
import com.techenglish.domain.model.KeywordTooltip
import com.techenglish.domain.model.LineExplanation
import com.techenglish.domain.model.ProgrammingLanguage
import com.techenglish.domain.model.ScenarioType
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TimeAttackMode
import com.techenglish.domain.model.TimeAttackRecord
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.UsefulExpression
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.model.Word
import com.techenglish.domain.model.WordRelation
import com.techenglish.domain.model.WordSense
import com.techenglish.domain.model.WordSource
import com.techenglish.domain.model.WorkPhrase
import com.techenglish.domain.model.WorkScenario
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val mapperJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
private data class LineExplanationDto(
    val line: Int,
    val en: String,
    val ru: String
)

@Serializable
private data class KeywordTooltipDto(
    val keyword: String,
    val en: String,
    val ru: String,
    val example: String = ""
)

@Serializable
private data class GlossaryEntryDto(
    val term: String,
    val transcription: String = "",
    val translation: String,
    val explanationRu: String = "",
    val example: String = ""
)

@Serializable
private data class UsefulExpressionDto(
    val en: String,
    val ru: String,
    val context: String = ""
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    isSystem = isSystem
)

fun CategoryProgressRow.toDomain(): CategoryProgress = CategoryProgress(
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    totalWords = totalWords,
    learnedWords = learnedWords,
    masteredWords = masteredWords
)

fun WordWithDetails.toDomain(): Word = Word(
    id = word.id,
    categoryId = word.categoryId,
    categoryName = categoryName,
    languageId = word.languageId,
    languageName = languageName,
    term = word.term,
    transcription = word.transcription,
    translation = word.translation,
    example = word.example,
    explanationRu = word.explanationRu,
    isFavorite = word.isFavorite,
    isCustom = word.isCustom,
    repetitions = word.repetitions,
    easeFactor = word.easeFactor,
    intervalDays = word.intervalDays,
    nextReviewAt = word.nextReviewAt
)

fun WordEntity.toDomain(categoryName: String = "", languageName: String? = null): Word = Word(
    id = id,
    categoryId = categoryId,
    categoryName = categoryName,
    languageId = languageId,
    languageName = languageName,
    term = term,
    transcription = transcription,
    translation = translation,
    example = example,
    explanationRu = explanationRu,
    isFavorite = isFavorite,
    isCustom = isCustom,
    repetitions = repetitions,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    nextReviewAt = nextReviewAt
)

fun Word.toEntity(): WordEntity = WordEntity(
    id = id,
    categoryId = categoryId,
    languageId = languageId,
    term = term,
    transcription = transcription,
    translation = translation,
    example = example,
    explanationRu = explanationRu,
    isFavorite = isFavorite,
    isCustom = isCustom,
    repetitions = repetitions,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    nextReviewAt = nextReviewAt
)

fun ProgrammingLanguageEntity.toDomain(): ProgrammingLanguage = ProgrammingLanguage(
    id = id,
    slug = slug,
    name = name,
    descriptionEn = descriptionEn,
    descriptionRu = descriptionRu,
    historyEn = historyEn,
    historyRu = historyRu,
    paradigms = paradigms.splitCsv(),
    domains = domains.splitCsv(),
    frameworks = frameworks.splitCsv(),
    typingSystem = typingSystem,
    memoryManagement = memoryManagement,
    difficulty = difficulty,
    yearCreated = yearCreated,
    creator = creator,
    accentColorHex = accentColorHex
)

fun CodeSnippetEntity.toDomain(): CodeSnippet {
    val lines = runCatching {
        mapperJson.decodeFromString<List<LineExplanationDto>>(lineExplanationsJson)
            .map { LineExplanation(it.line, it.en, it.ru) }
    }.getOrDefault(emptyList())

    val kws = runCatching {
        mapperJson.decodeFromString<List<KeywordTooltipDto>>(keywordsJson)
            .map { KeywordTooltip(it.keyword, it.en, it.ru, it.example) }
    }.getOrDefault(emptyList())

    return CodeSnippet(
        id = id,
        languageId = languageId,
        taskKey = taskKey,
        taskTitleEn = taskTitleEn,
        taskTitleRu = taskTitleRu,
        code = code,
        explanationEn = explanationEn,
        explanationRu = explanationRu,
        lineExplanations = lines,
        keywords = kws
    )
}

fun DocArticleEntity.toDomain(): DocArticle {
    val glossary = runCatching {
        mapperJson.decodeFromString<List<GlossaryEntryDto>>(glossaryJson)
            .map { GlossaryEntry(it.term, it.transcription, it.translation, it.explanationRu, it.example) }
    }.getOrDefault(emptyList())

    return DocArticle(
        id = id,
        title = title,
        techTag = techTag,
        content = content,
        explanationRu = explanationRu,
        level = level,
        readingProgress = readingProgress,
        isCompleted = isCompleted,
        glossary = glossary,
        sourceUrl = sourceUrl,
        license = license
    )
}

fun ScenarioEntity.toDomain(): WorkScenario {
    val options = runCatching {
        mapperJson.decodeFromString<List<String>>(optionsJson)
    }.getOrDefault(emptyList())

    val useful = runCatching {
        mapperJson.decodeFromString<List<UsefulExpressionDto>>(usefulExpressionsJson)
            .map { UsefulExpression(it.en, it.ru, it.context) }
    }.getOrDefault(emptyList())

    return WorkScenario(
        id = id,
        type = ScenarioType.fromRaw(type),
        exerciseType = ExerciseType.fromRaw(exerciseType),
        title = title,
        prompt = prompt,
        promptRu = promptRu,
        solution = solution,
        options = options,
        explanationRu = explanationRu,
        usefulExpressions = useful,
        difficulty = difficulty
    )
}

fun WorkPhraseEntity.toDomain(): WorkPhrase = WorkPhrase(
    id = id,
    category = ScenarioType.fromRaw(category),
    phraseEn = phraseEn,
    translationRu = translationRu,
    contextRu = contextRu,
    difficulty = difficulty
)

fun StudyLogEntity.toDomain(): StudyLog = StudyLog(
    id = id,
    wordId = wordId,
    scenarioId = scenarioId,
    articleId = articleId,
    snippetId = snippetId,
    actionType = StudyActionType.fromRaw(actionType),
    isCorrect = isCorrect,
    xpEarned = xpEarned,
    timestamp = timestamp
)

fun StudyLog.toEntity(): StudyLogEntity = StudyLogEntity(
    id = id,
    wordId = wordId,
    scenarioId = scenarioId,
    articleId = articleId,
    snippetId = snippetId,
    actionType = actionType.name,
    isCorrect = isCorrect,
    xpEarned = xpEarned,
    timestamp = timestamp
)

fun StoredUserPreferences.toDomain(): UserSettings = UserSettings(
    userName = userName,
    dailyGoal = dailyGoal,
    themeMode = ThemeMode.fromRaw(themeMode),
    dynamicColors = dynamicColors,
    readingFontSizeSp = readingFontSizeSp,
    notificationsEnabled = notificationsEnabled,
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    hapticFeedbackEnabled = hapticFeedbackEnabled,
    reduceMotionEnabled = reduceMotionEnabled,
    translationLanguage = TranslationLanguage.fromRaw(translationLanguage),
    ageVerified16Plus = ageVerified16Plus,
    allowFriendPings = allowFriendPings,
    aiConsentAccepted = aiConsentAccepted
)

fun AcademicTermEntity.toDomain(): AcademicTerm = AcademicTerm(
    id = id,
    term = term,
    normalizedTerm = normalizedTerm,
    phoneticTranscription = phoneticTranscription,
    partOfSpeech = partOfSpeech,
    translationRu = translationRu,
    translationTg = translationTg,
    definitionEn = definitionEn,
    definitionRu = definitionRu,
    academicExplanationEn = academicExplanationEn,
    academicExplanationRu = academicExplanationRu,
    practicalExplanation = practicalExplanation,
    exampleEn = exampleEn,
    exampleRu = exampleRu,
    majorCategory = majorCategory,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    domain = domain,
    subdomain = subdomain,
    difficultyLevel = difficultyLevel,
    cefrLevel = cefrLevel,
    synonyms = synonyms.splitCsv(),
    relatedTerms = relatedTerms.splitCsv(),
    abbreviations = abbreviations,
    sourceReference = sourceReference,
    sourceId = sourceId,
    verificationStatus = verificationStatus,
    isFavorite = isFavorite,
    isInSrs = isInSrs,
    repetitions = repetitions,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    nextReviewAt = nextReviewAt,
    lastStudiedAt = lastStudiedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    ipaSourceUrl = ipaSourceUrl,
    ipaLicense = ipaLicense
)

fun WordSenseEntity.toDomain(): WordSense = WordSense(
    id = id,
    termId = termId,
    senseNumber = senseNumber,
    senseLabel = senseLabel,
    domain = domain,
    definitionEn = definitionEn,
    definitionRu = definitionRu,
    exampleEn = exampleEn,
    exampleRu = exampleRu
)

fun WordSourceEntity.toDomain(): WordSource = WordSource(
    id = id,
    title = title,
    url = url,
    sourceType = sourceType,
    publisher = publisher,
    license = license
)

@Serializable
private data class DetectedDocTermDto(
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
    val explanationRu: String = "",
    val domain: String = ""
)

fun CustomImportedDocEntity.toDomain(): CustomImportedDoc {
    val terms = runCatching {
        mapperJson.decodeFromString<List<DetectedDocTermDto>>(detectedTermsJson).map {
            DetectedDocTerm(
                matchedText = it.matchedText,
                normalizedTerm = it.normalizedTerm,
                startOffset = it.startOffset,
                endOffset = it.endOffset,
                termId = it.termId,
                wordId = it.wordId,
                headword = it.headword,
                transcription = it.transcription,
                translationRu = it.translationRu,
                translationTg = it.translationTg,
                explanationRu = it.explanationRu,
                domain = it.domain
            )
        }
    }.getOrDefault(emptyList())

    return CustomImportedDoc(
        id = id,
        title = title,
        sourceFileName = sourceFileName,
        content = content,
        detectedTerms = terms,
        createdAt = createdAt
    )
}

fun CustomImportedDoc.toEntity(): CustomImportedDocEntity {
    val dtos = detectedTerms.map {
        DetectedDocTermDto(
            matchedText = it.matchedText,
            normalizedTerm = it.normalizedTerm,
            startOffset = it.startOffset,
            endOffset = it.endOffset,
            termId = it.termId,
            wordId = it.wordId,
            headword = it.headword,
            transcription = it.transcription,
            translationRu = it.translationRu,
            translationTg = it.translationTg,
            explanationRu = it.explanationRu,
            domain = it.domain
        )
    }
    return CustomImportedDocEntity(
        id = id,
        title = title,
        sourceFileName = sourceFileName,
        content = content,
        detectedTermsJson = mapperJson.encodeToString(dtos),
        detectedTermsCount = detectedTerms.size,
        createdAt = createdAt
    )
}

fun TimeAttackRecordEntity.toDomain(): TimeAttackRecord = TimeAttackRecord(
    id = id,
    mode = TimeAttackMode.fromRaw(mode),
    score = score,
    correctCount = correctCount,
    totalAnswered = totalAnswered,
    xpEarned = xpEarned,
    durationSeconds = durationSeconds,
    achievedAt = achievedAt
)

fun TimeAttackRecord.toEntity(): TimeAttackRecordEntity = TimeAttackRecordEntity(
    id = id,
    mode = mode.name,
    score = score,
    correctCount = correctCount,
    totalAnswered = totalAnswered,
    xpEarned = xpEarned,
    durationSeconds = durationSeconds,
    achievedAt = achievedAt
)

fun WordRelationEntity.toDomain(): WordRelation = WordRelation(
    id = id,
    termId = termId,
    relatedTerm = relatedTerm,
    relationType = relationType
)

fun MajorCategoryCountRow.toDomain(): AcademicMajorCategorySummary = AcademicMajorCategorySummary(
    majorCategory = majorCategory,
    categoryId = categoryId,
    termCount = termCount,
    learnedCount = learnedCount
)

fun DomainTermCountRow.toDomain(): AcademicDomainSummary = AcademicDomainSummary(
    domain = domain,
    majorCategory = majorCategory,
    termCount = termCount
)

private fun String.splitCsv(): List<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }
