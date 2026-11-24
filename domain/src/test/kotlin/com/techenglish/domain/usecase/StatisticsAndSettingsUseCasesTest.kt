package com.techenglish.domain.usecase

import com.techenglish.domain.FakeDocumentationRepository
import com.techenglish.domain.FakeSettingsRepository
import com.techenglish.domain.FakeStudyLogRepository
import com.techenglish.domain.FakeVocabularyRepository
import com.techenglish.domain.model.ChameleonThemeType
import com.techenglish.domain.model.ComboStreakState
import com.techenglish.domain.model.ComboStreakTracker
import com.techenglish.domain.model.DayActivity
import com.techenglish.domain.model.DocArticle
import com.techenglish.domain.model.ScenarioChameleonRegistry
import com.techenglish.domain.model.ScenarioType
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.StudyLog
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.model.Word
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class StatisticsAndSettingsUseCasesTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 19)
    private fun millisFor(daysAgo: Long, hour: Int = 10): Long =
        today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `streak calculates strictly from study_logs history`() {
        val calcStreak = CalculateStreakUseCase()

        assertEquals(0, calcStreak(emptyList(), today, zone))
        assertEquals(0, calcStreak(listOf(millisFor(2)), today, zone)) // gap of 2 days breaks streak
        assertEquals(1, calcStreak(listOf(millisFor(1)), today, zone)) // studied yesterday still alive
        assertEquals(1, calcStreak(listOf(millisFor(0, 9), millisFor(0, 18)), today, zone)) // multiple logs same day = 1

        val fourDayLogs = listOf(millisFor(0), millisFor(1), millisFor(2), millisFor(3), millisFor(6))
        assertEquals(4, calcStreak(fourDayLogs, today, zone))
    }

    @Test
    fun `xp level calculation covers developer rank progression`() {
        val calcLevel = CalculateXpLevelUseCase()

        assertEquals(1, calcLevel(0).level)
        assertEquals("Intern Developer", calcLevel(0).rankTitleRu)
        assertEquals(3, calcLevel(300).level)
        assertEquals("Junior Developer", calcLevel(300).rankTitleRu)
        assertEquals("Middle Developer", calcLevel(750).rankTitleRu)
        assertEquals("Senior Developer", calcLevel(1500).rankTitleRu)
        assertEquals("Tech Lead", calcLevel(2250).rankTitleRu)
        assertEquals("Staff Engineer", calcLevel(3000).rankTitleRu)
        assertEquals("Principal Architect", calcLevel(5000).rankTitleRu)
    }

    @Test
    fun `daily progress and full statistics aggregate real logs accurately`() = runTest {
        val now = millisFor(0, 15)
        val logs = listOf(
            StudyLog(1, wordId = 1, actionType = StudyActionType.FLASHCARD, isCorrect = true, xpEarned = 10, timestamp = millisFor(0, 10)),
            StudyLog(2, wordId = 2, actionType = StudyActionType.FLASHCARD, isCorrect = false, xpEarned = 5, timestamp = millisFor(0, 11)),
            StudyLog(3, scenarioId = 1, actionType = StudyActionType.SCENARIO, isCorrect = true, xpEarned = 20, timestamp = millisFor(1, 14)),
            StudyLog(4, articleId = 1, actionType = StudyActionType.DOC_READ, isCorrect = true, xpEarned = 15, timestamp = millisFor(2, 12))
        )
        val vocabRepo = FakeVocabularyRepository(
            listOf(
                Word(1, 1, "Backend", term = "a", transcription = "", translation = "а", example = "", explanationRu = "", repetitions = 3, nextReviewAt = now + 10000),
                Word(2, 1, "Backend", term = "b", transcription = "", translation = "б", example = "", explanationRu = "", repetitions = 0, nextReviewAt = 0L)
            )
        )
        val docRepo = FakeDocumentationRepository(
            listOf(DocArticle(1, "Doc", "SQL", "Text", "RU", "B1", 1f, true, emptyList()))
        )
        val logRepo = FakeStudyLogRepository(logs)
        val settingsRepo = FakeSettingsRepository(UserSettings(dailyGoal = 10))
        val streakUseCase = CalculateStreakUseCase()
        val xpUseCase = CalculateXpLevelUseCase()

        val daily = GetDailyProgressUseCase(logRepo, vocabRepo, settingsRepo, streakUseCase, xpUseCase)(now, zone).first()
        assertEquals(2, daily.reviewedCardsToday)
        assertEquals(10, daily.dailyGoal)
        assertEquals(1, daily.dueWordsCount)
        assertEquals(3, daily.streakDays)
        assertEquals(50, daily.levelProgress.totalXp)
        assertFalse(daily.isGoalReached)

        val stats = GetStatisticsUseCase(logRepo, vocabRepo, docRepo, settingsRepo, streakUseCase, xpUseCase)(now, zone).first()
        assertEquals(2, stats.totalWords)
        assertEquals(1, stats.learnedWords)
        assertEquals(2, stats.reviewedTotal)
        assertEquals(75, stats.accuracyPercent) // 3 of 4 correct = 75%
        assertEquals(3, stats.streakDays)
        assertEquals(7, stats.weeklyActivity.size)
        assertEquals(30, stats.monthlyActivity.size)
        assertEquals(365, stats.yearlyActivity.size)
        assertEquals(365, daily.yearlyActivity.size)
        assertEquals(1, stats.completedDocsCount)
        assertEquals(1, stats.completedScenariosCount)
    }

    @Test
    fun `updateSettings persists all user preferences including IDE themes haptic and reduceMotion`() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val useCase = UpdateSettingsUseCase(settingsRepo)

        useCase.setUserName("Алексей")
        useCase.setDailyGoal(30)
        useCase.setThemeMode(ThemeMode.TOKYO_NIGHT)
        useCase.setDynamicColors(false)
        useCase.setReadingFontSize(18f)
        useCase.setNotificationsEnabled(false)
        useCase.setReminderTime(21, 30)
        useCase.setHapticFeedbackEnabled(false)
        useCase.setReduceMotionEnabled(true)

        val saved = settingsRepo.observeSettings().first()
        assertEquals("Алексей", saved.userName)
        assertEquals(30, saved.dailyGoal)
        assertEquals(ThemeMode.TOKYO_NIGHT, saved.themeMode)
        assertTrue(saved.themeMode.isIdePreset)
        assertTrue(saved.themeMode.isDarkPalette)
        assertFalse(saved.dynamicColors)
        assertEquals(18f, saved.readingFontSizeSp, 0.01f)
        assertFalse(saved.notificationsEnabled)
        assertEquals(21, saved.reminderHour)
        assertEquals(30, saved.reminderMinute)
        assertFalse(saved.hapticFeedbackEnabled)
        assertTrue(saved.reduceMotionEnabled)

        listOf("TOKYO_NIGHT", "DRACULA", "GITHUB_DARK", "NORD", "GRUVBOX", "LIGHT", "DARK", "SYSTEM").forEach { raw ->
            assertEquals(raw, ThemeMode.fromRaw(raw).name)
        }
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromRaw("UNKNOWN_THEME"))
    }

    @Test
    fun `combo streak tracker triggers developer humor milestones and resets on mistake`() {
        var state = ComboStreakState()
        state = ComboStreakTracker.nextState(state, true)
        state = ComboStreakTracker.nextState(state, true)
        assertFalse(state.isMilestoneActive)
        assertNull(state.message)

        // 3rd consecutive correct -> LGTM!
        state = ComboStreakTracker.nextState(state, true)
        assertTrue(state.isMilestoneActive)
        assertEquals(1, state.tier)
        assertEquals("LGTM!", state.message)

        // 5th consecutive correct -> Clean build!
        state = ComboStreakTracker.nextState(state, true)
        state = ComboStreakTracker.nextState(state, true)
        assertEquals(2, state.tier)
        assertEquals("Clean build!", state.message)

        // 8th consecutive correct -> Zero warnings!
        repeat(3) { state = ComboStreakTracker.nextState(state, true) }
        assertEquals(3, state.tier)
        assertEquals("Zero warnings!", state.message)

        // 10th consecutive correct -> Merged to main!
        repeat(2) { state = ComboStreakTracker.nextState(state, true) }
        assertEquals(4, state.tier)
        assertEquals("Merged to main!", state.message)
        assertEquals(10, state.bestStreak)

        // Mistake resets current streak but keeps bestStreak
        state = ComboStreakTracker.nextState(state, false)
        assertEquals(0, state.currentStreak)
        assertEquals(10, state.bestStreak)
        assertFalse(state.isMilestoneActive)
    }

    @Test
    fun `chameleon registry resolves scenario types and error logs and supports extensible registration`() {
        assertEquals(ChameleonThemeType.GITHUB_PR, ScenarioChameleonRegistry.resolveForScenario(ScenarioType.CODE_REVIEW))
        assertEquals(ChameleonThemeType.GITHUB_PR, ScenarioChameleonRegistry.resolveForScenario(ScenarioType.PR_COMMENTS))
        assertEquals(ChameleonThemeType.SLACK, ScenarioChameleonRegistry.resolveForScenario(ScenarioType.DAILY_STANDUP))
        assertEquals(ChameleonThemeType.SLACK, ScenarioChameleonRegistry.resolveForScenario(ScenarioType.TEAM_COMMUNICATION))
        assertEquals(ChameleonThemeType.TERMINAL, ScenarioChameleonRegistry.resolveForScenario(ScenarioType.BUG_REPORTING))
        assertEquals(ChameleonThemeType.TERMINAL, ScenarioChameleonRegistry.resolveForScenario(ScenarioType.GITHUB_ISSUES))
        assertEquals(ChameleonThemeType.DEFAULT_IDE, ScenarioChameleonRegistry.resolveForScenario(ScenarioType.INTERVIEW))

        assertEquals(ChameleonThemeType.TERMINAL, ScenarioChameleonRegistry.resolveForDocTag("Error Logs"))
        assertEquals(ChameleonThemeType.DEFAULT_IDE, ScenarioChameleonRegistry.resolveForDocTag("REST API"))

        ScenarioChameleonRegistry.registerScenarioMapping(ScenarioType.INTERVIEW, ChameleonThemeType.SLACK)
        assertEquals(ChameleonThemeType.SLACK, ScenarioChameleonRegistry.resolveForScenario(ScenarioType.INTERVIEW))
        ScenarioChameleonRegistry.registerScenarioMapping(ScenarioType.INTERVIEW, ChameleonThemeType.DEFAULT_IDE)

        assertEquals(0, DayActivity(1L, "1", 0, 0).intensityLevel)
        assertEquals(1, DayActivity(1L, "1", 2, 2).intensityLevel)
        assertEquals(2, DayActivity(1L, "1", 4, 3).intensityLevel)
        assertEquals(3, DayActivity(1L, "1", 8, 7).intensityLevel)
        assertEquals(4, DayActivity(1L, "1", 15, 14).intensityLevel)
    }
}
