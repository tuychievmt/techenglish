package com.techenglish.app.ui

import android.content.Context
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.designsystem.ChameleonScenarioContainer
import com.techenglish.app.core.designsystem.CodeSyntaxHighlighter
import com.techenglish.app.core.designsystem.ComboStreakBanner
import com.techenglish.app.core.designsystem.ConfirmDialog
import com.techenglish.app.core.designsystem.DifficultyBadge
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.ErrorStateView
import com.techenglish.app.core.designsystem.GitHubContributionGraph
import com.techenglish.app.core.designsystem.HighlightedExampleQuoteCard
import com.techenglish.app.core.designsystem.PartOfSpeechBadge
import com.techenglish.app.core.designsystem.PracticalExplanationCard
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.TechEnglishTheme
import com.techenglish.app.core.designsystem.TermDisplayTextStyle
import com.techenglish.app.core.designsystem.TerminalSearchField
import com.techenglish.app.core.designsystem.TranslationContainerCard
import com.techenglish.app.core.designsystem.calculateContrastRatio
import com.techenglish.app.core.designsystem.ensureAccessibleAccent
import com.techenglish.app.core.designsystem.parseHexColor
import com.techenglish.app.core.designsystem.resolveColorSchemeForMode
import com.techenglish.app.core.designsystem.resolveDifficultyBadgeColors
import com.techenglish.app.core.designsystem.resolvePartOfSpeechBadgeColors
import com.techenglish.app.core.navigation.BottomNavTab
import com.techenglish.app.core.navigation.Routes
import com.techenglish.app.core.database.entity.LeaderboardCacheEntity
import com.techenglish.app.feature.stage3.AgeGate16PlusScreen
import com.techenglish.app.feature.stage3.LeaderboardsContent
import com.techenglish.app.feature.stage3.LeaderboardsUiState
import com.techenglish.app.feature.stage4a.CommentsMnemonicsContent
import com.techenglish.app.feature.stage4a.CommentsMnemonicsUiState
import com.techenglish.app.feature.stage4a.CommunityDecksContent
import com.techenglish.app.feature.stage4a.CommunityDecksUiState
import com.techenglish.app.feature.stage4b.AiMentorContent
import com.techenglish.app.feature.stage4b.AiMentorUiState
import com.techenglish.app.feature.stage4b.CrowdsourcePrContent
import com.techenglish.app.feature.stage4b.CrowdsourcePrUiState
import com.techenglish.app.feature.stage4b.SquadsContent
import com.techenglish.app.feature.stage4b.SquadsUiState
import com.techenglish.app.feature.stage4c.PeerReviewArenaContent
import com.techenglish.app.feature.stage4c.PeerReviewArenaUiState
import com.techenglish.app.feature.stage4c.PvpDuelContent
import com.techenglish.app.feature.stage4c.PvpDuelUiState
import com.techenglish.app.feature.stage4c.ThematicRoomsContent
import com.techenglish.app.feature.stage4c.ThematicRoomsUiState
import com.techenglish.domain.model.ChameleonThemeType
import com.techenglish.domain.model.ComboStreakState
import com.techenglish.domain.model.CommentTargetType
import com.techenglish.domain.model.CommunityComment
import com.techenglish.domain.model.CommunityDeck
import com.techenglish.domain.model.CrowdsourceEligibility
import com.techenglish.domain.model.PvpMatchMode
import com.techenglish.domain.model.PvpMatchOutcome
import com.techenglish.domain.model.PvpMatchState
import com.techenglish.domain.model.RoomChatMessage
import com.techenglish.domain.model.SquadInfo
import com.techenglish.domain.model.SquadMemberInfo
import com.techenglish.domain.model.ThematicRoomChannel
import com.techenglish.domain.model.DayActivity
import com.techenglish.domain.model.KeywordTooltip
import com.techenglish.domain.model.LeaderboardTab
import com.techenglish.domain.model.ScenarioChameleonRegistry
import com.techenglish.domain.model.ScenarioType
import com.techenglish.domain.model.ThemeMode
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import android.graphics.Typeface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TechEnglishUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `loading skeleton renders cleanly without crashing`() {
        composeRule.setContent {
            TechEnglishTheme {
                SkeletonListPlaceholder(itemCount = 3)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Compiling vocabulary...").assertIsDisplayed()
    }

    @Test
    fun `empty state displays title subtitle and triggers action click`() {
        var clicked = false
        composeRule.setContent {
            TechEnglishTheme {
                EmptyStateView(
                    title = "Слова не найдены",
                    subtitle = "Добавьте собственный термин.",
                    actionLabel = "Добавить",
                    onAction = { clicked = true }
                )
            }
        }
        composeRule.onNodeWithText("Слова не найдены").assertIsDisplayed()
        composeRule.onNodeWithText("Добавьте собственный термин.").assertIsDisplayed()
        composeRule.onNodeWithText("Добавить").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `error state displays error message and invokes retry`() {
        var retried = false
        composeRule.setContent {
            TechEnglishTheme {
                ErrorStateView(
                    message = "Ошибка чтения локальной БД",
                    onRetry = { retried = true }
                )
            }
        }
        composeRule.onNodeWithText("Произошла ошибка").assertIsDisplayed()
        composeRule.onNodeWithText("Ошибка чтения локальной БД").assertIsDisplayed()
        composeRule.onNodeWithText("Повторить").performClick()
        assertTrue(retried)
    }

    @Test
    fun `confirm dialog confirms and dismisses cleanly`() {
        var confirmed = false
        composeRule.setContent {
            TechEnglishTheme {
                ConfirmDialog(
                    title = "Удалить термин?",
                    message = "Действие необратимо.",
                    onConfirm = { confirmed = true },
                    onDismiss = {}
                )
            }
        }
        composeRule.onNodeWithText("Удалить термин?").assertIsDisplayed()
        composeRule.onNodeWithText("Удалить").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun `navigation bottom bar defines all 5 mandatory tabs and academic route builders`() {
        assertEquals(
            listOf("Главная", "Словарь", "Языки", "Практика", "Профиль"),
            BottomNavTab.entries.map { it.labelRu }
        )
        assertEquals("flashcards?categoryId=3", Routes.flashcards(3))
        assertEquals("language_detail/19", Routes.languageDetail(19))
        assertEquals("code_breakdown/42", Routes.codeBreakdown(42))
        assertEquals("language_compare?langAId=1&langBId=8", Routes.languageCompare(1, 8))
        assertEquals("doc_reader/5", Routes.docReader(5))
        assertEquals("scenario_exercise/7", Routes.scenarioExercise(7))
        assertEquals("academic_dictionary", Routes.ACADEMIC_DICTIONARY)
        assertEquals("academic_term_detail/101", Routes.academicTermDetail(101))
        assertEquals("academic_srs", Routes.ACADEMIC_SRS)
    }

    @Test
    fun `syntax highlighter annotates interactive keywords for code breakdown popups`() {
        val tooltips = listOf(
            KeywordTooltip("async", "Async", "Асинхронно", ""),
            KeywordTooltip("await", "Await", "Ожидание", ""),
            KeywordTooltip("defer", "Defer", "Отложить", ""),
            KeywordTooltip("trait", "Trait", "Трейт", ""),
            KeywordTooltip("JOIN", "Join", "Соединение", ""),
            KeywordTooltip("volatile", "Volatile", "Volatile", ""),
            KeywordTooltip("yield", "Yield", "Генератор", "")
        )
        val line = "async fun run() { await defer trait JOIN volatile yield }"
        val annotated = CodeSyntaxHighlighter.highlightLine(
            line = line,
            languageSlug = "kotlin",
            isDark = true,
            interactiveKeywords = tooltips
        )
        val tags = annotated.getStringAnnotations(
            tag = CodeSyntaxHighlighter.KEYWORD_ANNOTATION_TAG,
            start = 0,
            end = line.length
        ).map { it.item }.toSet()

        assertEquals(setOf("async", "await", "defer", "trait", "JOIN", "volatile", "yield"), tags)
    }

    @Test
    fun `audio controller handles locale switch speed pause stop and missing TTS engine without crash`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val controller = TechAudioController(context)
        try {
            controller.setSpeechLocale("en-GB")
            assertEquals("en-GB", controller.playbackState.value.speechLocaleTag)
            controller.setSpeechLocale("en-US")
            assertEquals("en-US", controller.playbackState.value.speechLocaleTag)

            controller.setSpeed(1.25f)
            assertEquals(1.25f, controller.playbackState.value.playbackSpeed, 0.01f)

            controller.speakQuick("Backpropagation")
            controller.pause()
            assertTrue(controller.playbackState.value.isPaused)

            controller.stop()
            assertFalse(controller.playbackState.value.isPlaying)
            assertFalse(controller.playbackState.value.isPaused)
        } finally {
            controller.release()
        }
    }

    @Test
    fun `all 5 IDE themes and Light and Dark pass WCAG AA contrast verification`() {
        val modes = listOf(
            ThemeMode.LIGHT,
            ThemeMode.DARK,
            ThemeMode.TOKYO_NIGHT,
            ThemeMode.DRACULA,
            ThemeMode.GITHUB_DARK,
            ThemeMode.NORD,
            ThemeMode.GRUVBOX
        )

        for (mode in modes) {
            val scheme = resolveColorSchemeForMode(mode)
            val bgRatio = calculateContrastRatio(scheme.onBackground, scheme.background)
            val surfaceRatio = calculateContrastRatio(scheme.onSurface, scheme.surface)
            val mutedOnSurfaceRatio = calculateContrastRatio(scheme.onSurfaceVariant, scheme.surface)
            val mutedOnVariantRatio = calculateContrastRatio(scheme.onSurfaceVariant, scheme.surfaceVariant)
            val primaryRatio = calculateContrastRatio(scheme.onPrimary, scheme.primary)
            val primaryContainerRatio = calculateContrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer)
            val secondaryContainerRatio = calculateContrastRatio(scheme.onSecondaryContainer, scheme.secondaryContainer)
            val tertiaryContainerRatio = calculateContrastRatio(scheme.onTertiaryContainer, scheme.tertiaryContainer)

            assertTrue("${mode.name} onBackground/background contrast $bgRatio < 4.5", bgRatio >= 4.5)
            assertTrue("${mode.name} onSurface/surface contrast $surfaceRatio < 4.5", surfaceRatio >= 4.5)
            assertTrue("${mode.name} onSurfaceVariant/surface contrast $mutedOnSurfaceRatio < 4.5", mutedOnSurfaceRatio >= 4.5)
            assertTrue("${mode.name} onSurfaceVariant/surfaceVariant contrast $mutedOnVariantRatio < 4.5", mutedOnVariantRatio >= 4.5)
            assertTrue("${mode.name} onPrimary/primary contrast $primaryRatio < 4.5", primaryRatio >= 4.5)
            assertTrue("${mode.name} onPrimaryContainer/primaryContainer contrast $primaryContainerRatio < 4.5", primaryContainerRatio >= 4.5)
            assertTrue("${mode.name} onSecondaryContainer/secondaryContainer contrast $secondaryContainerRatio < 4.5", secondaryContainerRatio >= 4.5)
            assertTrue("${mode.name} onTertiaryContainer/tertiaryContainer contrast $tertiaryContainerRatio < 4.5", tertiaryContainerRatio >= 4.5)

            // Verify dynamic language accent contrast adjustment on each theme's surface
            val rawLangAccent = parseHexColor("#F7DF1E") // JavaScript yellow
            val adjustedAccent = ensureAccessibleAccent(rawLangAccent, scheme.surface)
            val langContrast = calculateContrastRatio(adjustedAccent, scheme.surface)
            assertTrue("${mode.name} adjusted language accent contrast $langContrast < 4.5", langContrast >= 4.5)
        }

        val difficulties = listOf("Beginner", "Intermediate", "Advanced", "Expert", "Research Level")
        for (diff in difficulties) {
            for (isDark in listOf(false, true)) {
                val (bg, fg) = resolveDifficultyBadgeColors(diff, isDark)
                val ratio = calculateContrastRatio(fg, bg)
                assertTrue("Difficulty '$diff' (dark=$isDark) contrast $ratio < 4.5", ratio >= 4.5)
            }
        }

        val posList = listOf("noun", "verb", "adjective", "abbreviation")
        for (pos in posList) {
            for (isDark in listOf(false, true)) {
                val (bg, fg) = resolvePartOfSpeechBadgeColors(pos, isDark)
                val ratio = calculateContrastRatio(fg, bg)
                assertTrue("POS '$pos' (dark=$isDark) contrast $ratio < 4.5", ratio >= 4.5)
            }
        }
    }

    @Test
    fun `bundled Inter and JetBrains Mono fonts in res_font cover Cyrillic IPA and Tajik letters`() {
        val fontDir = listOf(
            File("src/main/res/font"),
            File("app/src/main/res/font")
        ).first { it.exists() }

        val interFiles = listOf(
            File(fontDir, "inter_regular.ttf"),
            File(fontDir, "inter_medium.ttf"),
            File(fontDir, "inter_semibold.ttf"),
            File(fontDir, "inter_bold.ttf")
        )
        val jbMonoFiles = listOf(
            File(fontDir, "jetbrains_mono_regular.ttf"),
            File(fontDir, "jetbrains_mono_medium.ttf"),
            File(fontDir, "jetbrains_mono_semibold.ttf"),
            File(fontDir, "jetbrains_mono_bold.ttf")
        )

        val fullMultilingualSample =
            "ASCII 0123456789 Кириллица АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ абвгдеёжзийклмнопрстуфхцчшщъыьэюя " +
                "Таҷикӣ: ғ ӣ қ ӯ ҳ ҷ Ғ Ӣ Қ Ӯ Ҳ Ҷ IPA: ˈælɡərɪðəm əˈsɪŋkrənəs ˌpɒlɪˈmɔːfɪzəm ʃ ʒ θ ð ŋ ɛ æ ɑ ɔ ʊ ʌ ə ɜ ɪ iː uː"

        for (file in interFiles) {
            assertTrue("Font file missing: ${file.path}", file.exists() && file.length() > 100_000)
            assertNotNull(Typeface.createFromFile(file))
            val missing = findMissingCmapCharacters(file, fullMultilingualSample)
            assertTrue("Missing glyphs in ${file.name}: $missing", missing.isEmpty())
        }

        val codeAndCyrillicSample =
            "fun main() { val x = 42 } Кириллица АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ ғ қ ҷ Ғ Қ Ҷ"
        for (file in jbMonoFiles) {
            assertTrue("Font file missing: ${file.path}", file.exists() && file.length() > 100_000)
            assertNotNull(Typeface.createFromFile(file))
            val missing = findMissingCmapCharacters(file, codeAndCyrillicSample)
            assertTrue("Missing glyphs in ${file.name}: $missing", missing.isEmpty())
        }

        // Render Cyrillic, Tajik letters, and IPA in Compose with bundled typography
        composeRule.setContent {
            TechEnglishTheme(themeMode = ThemeMode.TOKYO_NIGHT) {
                Column {
                    Text(text = "Таҷикӣ: ғ ӣ қ ӯ ҳ ҷ • Кириллица", style = TermDisplayTextStyle)
                }
            }
        }
        composeRule.onNodeWithText("Таҷикӣ: ғ ӣ қ ӯ ҳ ҷ • Кириллица").assertIsDisplayed()
    }

    /**
     * Parses the binary TrueType `cmap` table (Format 4 / Format 12) directly from a `.ttf` file
     * and returns any character from [text] that does not map to a valid glyph (glyphId > 0).
     */
    private fun findMissingCmapCharacters(ttfFile: File, text: String): List<Char> {
        val bytes = ttfFile.readBytes()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val numTables = buf.getShort(4).toInt() and 0xFFFF
        var cmapOffset = -1
        for (i in 0 until numTables) {
            val entryOffset = 12 + i * 16
            val tag = String(bytes, entryOffset, 4, Charsets.US_ASCII)
            if (tag == "cmap") {
                cmapOffset = buf.getInt(entryOffset + 8)
                break
            }
        }
        require(cmapOffset > 0) { "No cmap table in ${ttfFile.name}" }

        val numSubtables = buf.getShort(cmapOffset + 2).toInt() and 0xFFFF
        var format4Offset = -1
        var format12Offset = -1
        for (i in 0 until numSubtables) {
            val rec = cmapOffset + 4 + i * 8
            val subOffset = cmapOffset + buf.getInt(rec + 4)
            when (buf.getShort(subOffset).toInt() and 0xFFFF) {
                12 -> format12Offset = subOffset
                4 -> if (format4Offset < 0) format4Offset = subOffset
            }
        }

        val coveredRanges = mutableListOf<IntRange>()
        if (format12Offset > 0) {
            val numGroups = buf.getInt(format12Offset + 12)
            for (g in 0 until numGroups) {
                val gOff = format12Offset + 16 + g * 12
                val startCode = buf.getInt(gOff)
                val endCode = buf.getInt(gOff + 4)
                coveredRanges.add(startCode..endCode)
            }
        } else if (format4Offset > 0) {
            val segCount = (buf.getShort(format4Offset + 6).toInt() and 0xFFFF) / 2
            val endCountBase = format4Offset + 14
            val startCountBase = endCountBase + segCount * 2 + 2
            for (s in 0 until segCount) {
                val endCode = buf.getShort(endCountBase + s * 2).toInt() and 0xFFFF
                val startCode = buf.getShort(startCountBase + s * 2).toInt() and 0xFFFF
                if (startCode < 0xFFFF) {
                    coveredRanges.add(startCode..endCode)
                }
            }
        }

        return text.toList().filter { ch ->
            val cp = ch.code
            coveredRanges.none { cp in it }
        }
    }

    @Test
    fun `stage 1 components render with 200 percent fontScale reduceMotion chameleon themes and talkback descriptions`() {
        val yearly = List(365) { idx ->
            DayActivity(
                epochDay = idx.toLong(),
                dayLabel = "${(idx % 28) + 1}.01",
                count = idx % 12,
                correctCount = idx % 10
            )
        }
        val prTheme = ScenarioChameleonRegistry.resolveForScenario(ScenarioType.CODE_REVIEW)
        val slackTheme = ScenarioChameleonRegistry.resolveForScenario(ScenarioType.DAILY_STANDUP)
        val terminalTheme = ScenarioChameleonRegistry.resolveForDocTag("Error Logs")

        assertEquals(ChameleonThemeType.GITHUB_PR, prTheme)
        assertEquals(ChameleonThemeType.SLACK, slackTheme)
        assertEquals(ChameleonThemeType.TERMINAL, terminalTheme)

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f)
            ) {
                TechEnglishTheme(
                    themeMode = ThemeMode.DRACULA,
                    hapticFeedbackEnabled = false,
                    reduceMotionEnabled = true
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        TerminalSearchField(
                            value = "backpropagation",
                            onValueChange = {},
                            placeholderText = "Поиск..."
                        )
                        ComboStreakBanner(
                            comboState = ComboStreakState(
                                currentStreak = 5,
                                bestStreak = 5,
                                tier = 2,
                                message = "Clean build!"
                            )
                        )
                        DifficultyBadge(difficulty = "Research Level")
                        PartOfSpeechBadge(partOfSpeech = "noun phrase")
                        TranslationContainerCard(
                            translationText = "обратное распространение ошибки",
                            secondaryTranslationText = "паҳншавии баръакси хато"
                        )
                        HighlightedExampleQuoteCard(
                            exampleEn = "Backpropagation computes gradients efficiently across layers.",
                            targetTerm = "Backpropagation",
                            exampleRu = "Обратное распространение эффективно вычисляет градиенты по слоям."
                        )
                        PracticalExplanationCard(
                            explanationText = "Применяется при обучении глубоких нейронных сетей."
                        )
                        GitHubContributionGraph(yearlyActivity = yearly)
                        ChameleonScenarioContainer(
                            themeType = prTheme,
                            headerTitle = "Code Review",
                            headerSubtitle = "PR #42"
                        ) {
                            Text("Please extract this query into a DAO method.")
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("Clean build!").assertIsDisplayed()
        composeRule.onNodeWithText("обратное распространение ошибки").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Уровень сложности: Research Level").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Часть речи: noun phrase").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Комбо серия 5: Clean build!").assertIsDisplayed()
        composeRule.onNodeWithText("Please extract this query into a DAO method.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `FT-9_4 16+ Age Gate disables continue until confirmed and opens Privacy Policy dialog`() {
        var confirmed = false
        composeRule.setContent {
            TechEnglishTheme {
                AgeGate16PlusScreen(onConfirm16Plus = { confirmed = true })
            }
        }

        composeRule.onNodeWithText("Продолжить в гостевом режиме (16+)").assertIsNotEnabled()
        composeRule.onNodeWithText("Открыть Политику конфиденциальности").performClick()
        composeRule.onNodeWithText("Политика конфиденциальности (16+)").assertIsDisplayed()
        composeRule.onNodeWithText("Понятно").performClick()

        composeRule.onNodeWithText("Мне уже исполнилось 16 лет, и я ознакомлен(а) с Политикой конфиденциальности")
            .performClick()
        composeRule.onNodeWithText("Продолжить в гостевом режиме (16+)")
            .assertIsEnabled()
            .performClick()
        assertTrue(confirmed)
    }

    @Test
    fun `NFT-7_1 Leaderboards screen displays offline-mode banner and cached Room entries when offline`() {
        val cachedRows = listOf(
            LeaderboardCacheEntity(
                tab = LeaderboardTab.WEEKLY.name,
                userId = "u1",
                rank = 1,
                username = "principal_dev",
                avatarKey = "preset_architect",
                rankTitle = "Principal Architect",
                league = "DIAMOND",
                techStack = "Kotlin,Rust",
                weeklyXp = 640,
                totalXp = 4200,
                isCurrentUser = false
            ),
            LeaderboardCacheEntity(
                tab = LeaderboardTab.WEEKLY.name,
                userId = "u2",
                rank = 2,
                username = "offline_hero",
                avatarKey = "preset_terminal",
                rankTitle = "Senior Developer",
                league = "GOLD",
                techStack = "Kotlin,Python",
                weeklyXp = 510,
                totalXp = 2100,
                isCurrentUser = true
            )
        )

        composeRule.setContent {
            TechEnglishTheme {
                LeaderboardsContent(
                    state = LeaderboardsUiState(
                        selectedTab = LeaderboardTab.WEEKLY,
                        entries = cachedRows.toImmutableList(),
                        isOffline = true,
                        weekStartUtc5Label = "16.11.2026 00:00 UTC+5"
                    ),
                    onBack = {},
                    onSelectTab = {},
                    onRefresh = {}
                )
            }
        }

        composeRule.onNodeWithText("Офлайн-режим: показан кэш лидерборда из локальной БД.").assertIsDisplayed()
        composeRule.onNodeWithText("@principal_dev").assertIsDisplayed()
        composeRule.onNodeWithText("@offline_hero (вы)").assertIsDisplayed()
        composeRule.onNodeWithText("640 XP").assertIsDisplayed()
        composeRule.onNodeWithText("510 XP").assertIsDisplayed()
    }

    @Test
    fun `FT-13_1 Comments screen renders StackOverflow votes, triggers upvote and opens moderation report dialog`() {
        var votedValue = 0
        var reportedId = ""
        val sample = CommunityComment(
            id = "c-ui-1",
            targetType = CommentTargetType.TERM,
            targetId = 42,
            authorUserId = "u-1",
            authorUsername = "senior_dev",
            authorRankTitle = "Senior Developer",
            body = "Mnemonic: Idempotent = safe to retry PUT.",
            upvotes = 9,
            downvotes = 1,
            createdAtMillis = 1_760_000_000_000L
        )
        composeRule.setContent {
            TechEnglishTheme {
                CommentsMnemonicsContent(
                    state = CommentsMnemonicsUiState(
                        targetType = CommentTargetType.TERM,
                        targetId = 42,
                        targetTitle = "Idempotency",
                        comments = listOf(sample),
                        isOffline = true
                    ),
                    onBack = {},
                    onSelectSort = {},
                    onRefresh = {},
                    onPostComment = {},
                    onVote = { _, v -> votedValue = v },
                    onReportAndBlock = { c, _, _ -> reportedId = c.id }
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Mnemonic: Idempotent = safe to retry PUT."))
        composeRule.onNodeWithText("Mnemonic: Idempotent = safe to retry PUT.").assertIsDisplayed()
        composeRule.onNodeWithText("8").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Голос за").performClick()
        assertEquals(1, votedValue)

        composeRule.onNodeWithContentDescription("Пожаловаться на комментарий").performClick()
        composeRule.onNodeWithText("Отправить жалобу").performClick()
        assertEquals("c-ui-1", reportedId)
    }

    @Test
    fun `FT-13_2 Community Decks screen triggers 1-tap Room download and opens moderation report dialog`() {
        var downloadedDeckId = ""
        var reportedDeckId = ""
        val deck = CommunityDeck(
            id = "deck-ui-1",
            title = "Backend Middle Interview Pack",
            description = "System design & SQL terms",
            tags = listOf("backend", "interview"),
            authorUserId = "u-author",
            authorUsername = "techenglish_curators",
            rating = 4.9f,
            downloadsCount = 128,
            termCount = 5
        )
        composeRule.setContent {
            TechEnglishTheme {
                CommunityDecksContent(
                    state = CommunityDecksUiState(
                        decks = listOf(deck),
                        myCustomWordsCount = 4,
                        isOffline = true
                    ),
                    onBack = {},
                    onSearchChange = {},
                    onTagClick = {},
                    onRefresh = {},
                    onDownloadDeck = { downloadedDeckId = it.id },
                    onPublishDeck = { _, _, _ -> },
                    onRateDeck = { _, _ -> },
                    onReportAndBlock = { d, _, _ -> reportedDeckId = d.id }
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Скачать в Room (1 тап)"))
        composeRule.onNodeWithText("Backend Middle Interview Pack").assertIsDisplayed()
        composeRule.onNodeWithText("Скачать в Room (1 тап)").performClick()
        assertEquals("deck-ui-1", downloadedDeckId)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Пожаловаться на колоду"))
        composeRule.onNodeWithContentDescription("Пожаловаться на колоду").performClick()
        composeRule.onNodeWithText("Отправить жалобу").performClick()
        assertEquals("deck-ui-1", reportedDeckId)
    }

    @Test
    fun `FT-8_3 AI Mentor displays explicit consent gate before enabling interview and PR reviewer`() {
        var accepted = false
        composeRule.setContent {
            TechEnglishTheme {
                AiMentorContent(
                    state = AiMentorUiState(consentAccepted = false, dailyLimit = 15, remainingDailyQuota = 15),
                    onBack = {},
                    onAcceptConsent = { accepted = true },
                    onSelectTab = {},
                    onSelectRole = {},
                    onSelectLevel = {},
                    onSubmitInterview = {},
                    onSubmitPrReview = {}
                )
            }
        }
        composeRule.onNodeWithText("Согласие на обработку текста AI-прокси (ФТ-8.3)").assertIsDisplayed()
        composeRule.onNodeWithText("Принять условия и включить AI-ментора").performClick()
        assertTrue(accepted)
    }

    @Test
    fun `FT-14_4 Squads screen renders 5 to 10 member roster and non-ranked bonus multiplier notice`() {
        val squad = SquadInfo(
            id = "sq-ui-1",
            name = "Kotlin Guild",
            inviteCode = "SQ-A1B2C3",
            members = List(5) { SquadMemberInfo("u$it", "dev_$it", "Middle Developer", 220) },
            weeklyGoalXp = 1000,
            currentWeekXp = 1100,
            nextWeekBonusMultiplier = 1.25f,
            isActiveForChallenge = true
        )
        composeRule.setContent {
            TechEnglishTheme {
                SquadsContent(
                    state = SquadsUiState(squad = squad, isOffline = false),
                    onBack = {},
                    onRefresh = {},
                    onCreateSquad = {},
                    onJoinSquad = {},
                    onLeaveSquad = {}
                )
            }
        }
        composeRule.onNodeWithText("Kotlin Guild").assertIsDisplayed()
        composeRule.onNodeWithText("1.25x").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("@dev_0"))
        composeRule.onNodeWithText("@dev_0").assertIsDisplayed()
    }

    @Test
    fun `FT-14_3 Crowdsourcing PR screen enforces eligibility gates and CC BY-SA license checkbox`() {
        composeRule.setContent {
            TechEnglishTheme {
                CrowdsourcePrContent(
                    state = CrowdsourcePrUiState(
                        eligibility = CrowdsourceEligibility(
                            isEligible = false,
                            meetsRankMiddlePlus = false,
                            meetsAccountAge14Days = true,
                            meetsLearnedTerms100 = false,
                            meetsZeroConfirmedReports = true,
                            unmetReasonsRu = listOf("Требуется ранг Middle")
                        )
                    ),
                    onBack = {},
                    onRefresh = {},
                    onSubmitPr = { _, _, _, _, _, _ -> },
                    onVotePr = { _, _ -> },
                    onReportPr = { _, _ -> }
                )
            }
        }
        composeRule.onNodeWithText("Условия допуска Contributor (ФТ-14.3)").assertIsDisplayed()
        composeRule.onNodeWithText("Ранг Middle Developer и выше (≥ 600 XP)").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Отправить PR в словарь"))
        composeRule.onNodeWithText("Отправить PR в словарь").assertIsNotEnabled()
    }

    @Test
    fun `FT-13_3 Thematic Rooms screen renders 4 channels and allows reporting a room message`() {
        var reportedMsgId = ""
        val msg = RoomChatMessage(
            id = "rm-1",
            roomSlug = ThematicRoomChannel.CODE_REVIEW_PRACTICE.slug,
            authorUserId = "u-1",
            authorUsername = "review_guru",
            authorRankTitle = "Tech Lead",
            body = "Nit: could we extract this SQLite query into a DAO?",
            createdAtMillis = 1_760_000_000_000L
        )
        composeRule.setContent {
            TechEnglishTheme {
                ThematicRoomsContent(
                    state = ThematicRoomsUiState(
                        selectedRoom = ThematicRoomChannel.CODE_REVIEW_PRACTICE,
                        messages = listOf(msg),
                        isOffline = true
                    ),
                    onBack = {},
                    onSelectRoom = {},
                    onSendMessage = {},
                    onReportAndBlock = { m, _, _ -> reportedMsgId = m.id }
                )
            }
        }
        composeRule.onAllNodesWithText("#code-review-practice").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("#backend-english").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Nit: could we extract this SQLite query into a DAO?"))
        composeRule.onNodeWithContentDescription("Пожаловаться на сообщение").performClick()
        composeRule.onNodeWithText("Отправить жалобу").performClick()
        assertEquals("rm-1", reportedMsgId)
    }

    @Test
    fun `FT-14_1 PvP Duel screen displays clean disconnect outcome with 0 ranked XP in training mode`() {
        composeRule.setContent {
            TechEnglishTheme {
                PvpDuelContent(
                    state = PvpDuelUiState(
                        matchId = "m-disc",
                        mode = PvpMatchMode.TRAINING_FALLBACK,
                        matchState = PvpMatchState.DISCONNECTED_FORFEIT,
                        outcome = PvpMatchOutcome(
                            matchId = "m-disc",
                            mode = PvpMatchMode.TRAINING_FALLBACK,
                            finalState = PvpMatchState.DISCONNECTED_FORFEIT,
                            playerScore = 3,
                            opponentScore = 0,
                            rankedXpAwarded = 0,
                            ratingDelta = 0,
                            summaryRu = "Тренировочный раунд прерван из-за разрыва связи (0 рейтинговых очков)."
                        )
                    ),
                    onBack = {},
                    onStartRanked = {},
                    onStartTraining = {},
                    onSelectAnswer = {},
                    onDisconnect = {}
                )
            }
        }
        composeRule.onNodeWithText("Матч прерван (разрыв связи)").assertIsDisplayed()
        composeRule.onNodeWithText("Рейтинговый XP: +0 • Дельта рейтинга: 0").assertIsDisplayed()
    }

    @Test
    fun `FT-14_2 and Errata E5 Peer Review Arena renders daily case and 30-day S3 voice TTL notice`() {
        composeRule.setContent {
            TechEnglishTheme {
                PeerReviewArenaContent(
                    state = PeerReviewArenaUiState(voiceTtlDays = 30),
                    onBack = {},
                    onRefresh = {},
                    onSubmitResponse = { _, _, _ -> },
                    onSubmitFeedback = { _, _, _ -> },
                    onReportAndBlock = { _, _, _ -> }
                )
            }
        }
        composeRule.onNodeWithText("Incident Communication: Production Deployment Rollback").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(
            hasText("Голосовые ответы (≤ 20 с) хранятся в S3/MinIO не более 30 дней (TTL, Errata E5) и удаляются вместе с аккаунтом.")
        )
        composeRule.onNodeWithText("Голосовые ответы (≤ 20 с) хранятся в S3/MinIO не более 30 дней (TTL, Errata E5) и удаляются вместе с аккаунтом.")
            .assertIsDisplayed()
    }
}
