package com.techenglish.domain.usecase

import com.techenglish.domain.model.DailyProgress
import com.techenglish.domain.model.DayActivity
import com.techenglish.domain.model.LevelProgress
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.UserStats
import com.techenglish.domain.repository.DocumentationRepository
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.repository.StudyLogRepository
import com.techenglish.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Computes user streak strictly from real `study_logs` history.
 * Opening the app without completing any study action never increments the streak.
 */
class CalculateStreakUseCase @Inject constructor() {

    operator fun invoke(
        timestampsMillis: List<Long>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        if (timestampsMillis.isEmpty()) return 0

        val activeDays = timestampsMillis
            .map { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
            .toSet()

        val startDay = when {
            today in activeDays -> today
            today.minusDays(1) in activeDays -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        var cursor = startDay
        while (cursor in activeDays) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}

class CalculateXpLevelUseCase @Inject constructor() {

    operator fun invoke(totalXp: Int): LevelProgress {
        val safeXp = totalXp.coerceAtLeast(0)
        val level = 1 + (safeXp / XP_PER_LEVEL)
        val xpInLevel = safeXp % XP_PER_LEVEL
        val rank = when {
            level <= 2 -> "Intern Developer"
            level <= 4 -> "Junior Developer"
            level <= 7 -> "Middle Developer"
            level <= 11 -> "Senior Developer"
            level <= 16 -> "Tech Lead"
            level <= 22 -> "Staff Engineer"
            else -> "Principal Architect"
        }
        return LevelProgress(
            level = level,
            rankTitleRu = rank,
            totalXp = safeXp,
            xpInCurrentLevel = xpInLevel,
            xpForNextLevel = XP_PER_LEVEL
        )
    }

    companion object {
        const val XP_PER_LEVEL = 150
    }
}

internal fun buildYearlyActivityRange(
    logsByDay: Map<LocalDate, List<StudyLog>>,
    today: LocalDate,
    daysCount: Int = 365
): List<DayActivity> = ((daysCount - 1) downTo 0).map { offset ->
    val day = today.minusDays(offset.toLong())
    val dayLogs = logsByDay[day].orEmpty()
    DayActivity(
        epochDay = day.toEpochDay(),
        dayLabel = "${day.dayOfMonth}.${"%02d".format(day.monthValue)}",
        count = dayLogs.size,
        correctCount = dayLogs.count { it.isCorrect }
    )
}

class GetDailyProgressUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateStreak: CalculateStreakUseCase,
    private val calculateXpLevel: CalculateXpLevelUseCase
) {
    operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<DailyProgress> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val startOfDayMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        return combine(
            studyLogRepository.observeAllLogs(),
            vocabularyRepository.observeDueCount(nowMillis),
            settingsRepository.observeSettings()
        ) { logs, dueCount, settings ->
            val todayLogs = logs.filter { it.timestamp >= startOfDayMillis }
            val reviewedCardsToday = todayLogs.count { it.actionType == StudyActionType.FLASHCARD }
            val totalXp = logs.sumOf { it.xpEarned }
            val streak = calculateStreak(logs.map { it.timestamp }, today, zoneId)
            val logsByDay = logs.groupBy {
                Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
            }
            val yearly = buildYearlyActivityRange(logsByDay, today, daysCount = 365)

            DailyProgress(
                reviewedCardsToday = reviewedCardsToday,
                totalActivitiesToday = todayLogs.size,
                dailyGoal = settings.dailyGoal,
                dueWordsCount = dueCount,
                streakDays = streak,
                levelProgress = calculateXpLevel(totalXp),
                yearlyActivity = yearly
            )
        }
    }
}

class GetStatisticsUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val documentationRepository: DocumentationRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateStreak: CalculateStreakUseCase,
    private val calculateXpLevel: CalculateXpLevelUseCase
) {
    operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<UserStats> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val startOfDayMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val coreStatsFlow = combine(
            studyLogRepository.observeAllLogs(),
            vocabularyRepository.observeTotalWordCount(),
            vocabularyRepository.observeLearnedWordCount(),
            vocabularyRepository.observeDueCount(nowMillis),
            settingsRepository.observeSettings()
        ) { logs, totalWords, learnedWords, dueCount, settings ->
            CoreStatsBundle(logs, totalWords, learnedWords, dueCount, settings.dailyGoal)
        }

        return combine(
            coreStatsFlow,
            vocabularyRepository.observeCategoryProgress(),
            documentationRepository.observeCompletedCount(),
            studyLogRepository.observeCompletedScenarioIds()
        ) { core, categoryProgress, completedDocs, completedScenarios ->
            val logs = core.logs
            val totalLogs = logs.size
            val correctLogs = logs.count { it.isCorrect }
            val accuracy = if (totalLogs == 0) 0 else ((correctLogs * 100f) / totalLogs).toInt()
            val totalXp = logs.sumOf { it.xpEarned }
            val streak = calculateStreak(logs.map { it.timestamp }, today, zoneId)
            val levelProgress = calculateXpLevel(totalXp)

            val todayLogs = logs.filter { it.timestamp >= startOfDayMillis }
            val reviewedToday = todayLogs.count { it.actionType == StudyActionType.FLASHCARD }

            val logsByDay = logs.groupBy {
                Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
            }

            val ruLocale = Locale.forLanguageTag("ru")
            val weekly = (6 downTo 0).map { offset ->
                val day = today.minusDays(offset.toLong())
                val dayLogs = logsByDay[day].orEmpty()
                val label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, ruLocale)
                    .replaceFirstChar { it.uppercase() }
                DayActivity(
                    epochDay = day.toEpochDay(),
                    dayLabel = label,
                    count = dayLogs.size,
                    correctCount = dayLogs.count { it.isCorrect }
                )
            }

            val monthly = (29 downTo 0).map { offset ->
                val day = today.minusDays(offset.toLong())
                val dayLogs = logsByDay[day].orEmpty()
                DayActivity(
                    epochDay = day.toEpochDay(),
                    dayLabel = day.dayOfMonth.toString(),
                    count = dayLogs.size,
                    correctCount = dayLogs.count { it.isCorrect }
                )
            }

            val yearly = buildYearlyActivityRange(logsByDay, today, daysCount = 365)

            UserStats(
                totalWords = core.totalWords,
                learnedWords = core.learnedWords,
                reviewedTotal = logs.count { it.actionType == StudyActionType.FLASHCARD },
                accuracyPercent = accuracy,
                streakDays = streak,
                levelProgress = levelProgress,
                dailyProgress = DailyProgress(
                    reviewedCardsToday = reviewedToday,
                    totalActivitiesToday = todayLogs.size,
                    dailyGoal = core.dailyGoal,
                    dueWordsCount = core.dueCount,
                    streakDays = streak,
                    levelProgress = levelProgress,
                    yearlyActivity = yearly
                ),
                weeklyActivity = weekly,
                monthlyActivity = monthly,
                yearlyActivity = yearly,
                categoryProgress = categoryProgress,
                completedDocsCount = completedDocs,
                completedScenariosCount = completedScenarios.size
            )
        }
    }

    private data class CoreStatsBundle(
        val logs: List<StudyLog>,
        val totalWords: Int,
        val learnedWords: Int,
        val dueCount: Int,
        val dailyGoal: Int
    )
}

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend fun setUserName(name: String) = settingsRepository.setUserName(name)
    suspend fun setDailyGoal(goal: Int) = settingsRepository.setDailyGoal(goal)
    suspend fun setThemeMode(mode: ThemeMode) = settingsRepository.setThemeMode(mode)
    suspend fun setDynamicColors(enabled: Boolean) = settingsRepository.setDynamicColors(enabled)
    suspend fun setReadingFontSize(sizeSp: Float) = settingsRepository.setReadingFontSize(sizeSp)
    suspend fun setNotificationsEnabled(enabled: Boolean) = settingsRepository.setNotificationsEnabled(enabled)
    suspend fun setReminderTime(hour: Int, minute: Int) = settingsRepository.setReminderTime(hour, minute)
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) = settingsRepository.setHapticFeedbackEnabled(enabled)
    suspend fun setReduceMotionEnabled(enabled: Boolean) = settingsRepository.setReduceMotionEnabled(enabled)
    suspend fun setTranslationLanguage(language: TranslationLanguage) =
        settingsRepository.setTranslationLanguage(language)
    suspend fun setAgeVerified16Plus(verified: Boolean) =
        settingsRepository.setAgeVerified16Plus(verified)
    suspend fun setAllowFriendPings(allowed: Boolean) =
        settingsRepository.setAllowFriendPings(allowed)
    suspend fun setAiConsentAccepted(accepted: Boolean) =
        settingsRepository.setAiConsentAccepted(accepted)
}
