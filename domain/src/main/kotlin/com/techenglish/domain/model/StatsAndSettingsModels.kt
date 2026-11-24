package com.techenglish.domain.model

enum class StudyActionType(val labelRu: String) {
    FLASHCARD("Повторение карточки"),
    SCENARIO("Рабочий сценарий"),
    DOC_READ("Чтение документации"),
    LISTENING("Аудирование"),
    CODE_BREAKDOWN("Разбор кода");

    companion object {
        fun fromRaw(raw: String): StudyActionType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: FLASHCARD
    }
}

data class StudyLog(
    val id: Int = 0,
    val wordId: Int? = null,
    val scenarioId: Int? = null,
    val articleId: Int? = null,
    val snippetId: Int? = null,
    val actionType: StudyActionType,
    val isCorrect: Boolean,
    val xpEarned: Int,
    val timestamp: Long
)

data class DayActivity(
    val epochDay: Long,
    val dayLabel: String,
    val count: Int,
    val correctCount: Int
) {
    val intensityLevel: Int
        get() = when {
            count <= 0 -> 0
            count <= 2 -> 1
            count <= 5 -> 2
            count <= 10 -> 3
            else -> 4
        }
}

data class CategoryProgress(
    val categoryId: Int,
    val categoryName: String,
    val categoryIcon: String,
    val totalWords: Int,
    val learnedWords: Int,
    val masteredWords: Int
) {
    val progressFraction: Float
        get() = if (totalWords <= 0) 0f else (learnedWords.toFloat() / totalWords.toFloat()).coerceIn(0f, 1f)
}

data class LevelProgress(
    val level: Int,
    val rankTitleRu: String,
    val totalXp: Int,
    val xpInCurrentLevel: Int,
    val xpForNextLevel: Int
) {
    val progressFraction: Float
        get() = if (xpForNextLevel <= 0) 0f else (xpInCurrentLevel.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)
}

data class DailyProgress(
    val reviewedCardsToday: Int,
    val totalActivitiesToday: Int,
    val dailyGoal: Int,
    val dueWordsCount: Int,
    val streakDays: Int,
    val levelProgress: LevelProgress,
    val yearlyActivity: List<DayActivity> = emptyList()
) {
    val goalProgressFraction: Float
        get() = if (dailyGoal <= 0) 0f else (reviewedCardsToday.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

    val isGoalReached: Boolean
        get() = reviewedCardsToday >= dailyGoal
}

data class UserStats(
    val totalWords: Int,
    val learnedWords: Int,
    val reviewedTotal: Int,
    val accuracyPercent: Int,
    val streakDays: Int,
    val levelProgress: LevelProgress,
    val dailyProgress: DailyProgress,
    val weeklyActivity: List<DayActivity>,
    val monthlyActivity: List<DayActivity>,
    val yearlyActivity: List<DayActivity> = emptyList(),
    val categoryProgress: List<CategoryProgress>,
    val completedDocsCount: Int,
    val completedScenariosCount: Int
)

enum class ThemeMode(
    val labelRu: String,
    val isDarkPalette: Boolean,
    val isIdePreset: Boolean = false
) {
    SYSTEM("Системная", isDarkPalette = false, isIdePreset = false),
    LIGHT("Светлая", isDarkPalette = false, isIdePreset = false),
    DARK("Тёмная", isDarkPalette = true, isIdePreset = false),
    TOKYO_NIGHT("Tokyo Night", isDarkPalette = true, isIdePreset = true),
    DRACULA("Dracula", isDarkPalette = true, isIdePreset = true),
    GITHUB_DARK("GitHub Dark", isDarkPalette = true, isIdePreset = true),
    NORD("Nord", isDarkPalette = true, isIdePreset = true),
    GRUVBOX("Gruvbox", isDarkPalette = true, isIdePreset = true);

    companion object {
        fun fromRaw(raw: String): ThemeMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SYSTEM
    }
}

enum class TranslationLanguage(val labelRu: String, val shortLabel: String) {
    RU("Русский (RU)", "RU"),
    TJ("Тоҷикӣ (TJ)", "TJ");

    companion object {
        fun fromRaw(raw: String): TranslationLanguage =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: RU
    }
}

data class UserSettings(
    val userName: String = "Разработчик",
    val dailyGoal: Int = 20,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColors: Boolean = true,
    val readingFontSizeSp: Float = 16f,
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val hapticFeedbackEnabled: Boolean = true,
    val reduceMotionEnabled: Boolean = false,
    val translationLanguage: TranslationLanguage = TranslationLanguage.RU,
    val ageVerified16Plus: Boolean = false,
    val allowFriendPings: Boolean = true,
    val aiConsentAccepted: Boolean = false
)

data class ComboStreakState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val tier: Int = 0,
    val message: String? = null
) {
    val isMilestoneActive: Boolean
        get() = tier > 0 && message != null
}

object ComboStreakTracker {
    fun nextState(previous: ComboStreakState, isCorrect: Boolean): ComboStreakState {
        if (!isCorrect) {
            return previous.copy(currentStreak = 0, tier = 0, message = null)
        }
        val nextStreak = previous.currentStreak + 1
        val nextBest = maxOf(previous.bestStreak, nextStreak)
        val (tier, message) = when {
            nextStreak >= 10 -> 4 to "Merged to main!"
            nextStreak >= 8 -> 3 to "Zero warnings!"
            nextStreak >= 5 -> 2 to "Clean build!"
            nextStreak >= 3 -> 1 to "LGTM!"
            else -> 0 to null
        }
        return ComboStreakState(
            currentStreak = nextStreak,
            bestStreak = nextBest,
            tier = tier,
            message = message
        )
    }
}

enum class ChameleonThemeType(val labelRu: String, val badgeCode: String) {
    GITHUB_PR("GitHub Pull Request", "PR"),
    SLACK("Slack Workspace", "SLACK"),
    TERMINAL("Terminal Console", "TTY"),
    DEFAULT_IDE("IDE Workspace", "IDE")
}

object ScenarioChameleonRegistry {
    private val defaultScenarioMappings: Map<ScenarioType, ChameleonThemeType> = mapOf(
        ScenarioType.CODE_REVIEW to ChameleonThemeType.GITHUB_PR,
        ScenarioType.PR_COMMENTS to ChameleonThemeType.GITHUB_PR,
        ScenarioType.DAILY_STANDUP to ChameleonThemeType.SLACK,
        ScenarioType.TEAM_COMMUNICATION to ChameleonThemeType.SLACK,
        ScenarioType.CLIENT_COMMUNICATION to ChameleonThemeType.SLACK,
        ScenarioType.BUG_REPORTING to ChameleonThemeType.TERMINAL,
        ScenarioType.GITHUB_ISSUES to ChameleonThemeType.TERMINAL,
        ScenarioType.INCIDENT_POST_MORTEM to ChameleonThemeType.TERMINAL
    )

    private val customOverrides: MutableMap<ScenarioType, ChameleonThemeType> = mutableMapOf()

    fun resolveForScenario(
        scenarioType: ScenarioType,
        mappingTable: Map<ScenarioType, ChameleonThemeType> = currentMappingTable()
    ): ChameleonThemeType = mappingTable[scenarioType] ?: ChameleonThemeType.DEFAULT_IDE

    fun resolveForDocTag(techTag: String): ChameleonThemeType =
        if (techTag.equals("Error Logs", ignoreCase = true) ||
            techTag.contains("Log", ignoreCase = true)
        ) {
            ChameleonThemeType.TERMINAL
        } else {
            ChameleonThemeType.DEFAULT_IDE
        }

    fun registerScenarioMapping(type: ScenarioType, theme: ChameleonThemeType) {
        customOverrides[type] = theme
    }

    fun currentMappingTable(): Map<ScenarioType, ChameleonThemeType> =
        defaultScenarioMappings + customOverrides
}
