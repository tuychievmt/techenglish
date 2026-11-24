package com.techenglish.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.techenglish.app.feature.academic.AcademicDictionaryScreen
import com.techenglish.app.feature.academic.AcademicSrsScreen
import com.techenglish.app.feature.academic.AcademicTermDetailScreen
import com.techenglish.app.feature.documentation.DocReaderScreen
import com.techenglish.app.feature.documentation.DocumentationListScreen
import com.techenglish.app.feature.home.HomeScreen
import com.techenglish.app.feature.listening.ListeningScreen
import com.techenglish.app.feature.profile.ProfileScreen
import com.techenglish.app.feature.programming.CodeBreakdownScreen
import com.techenglish.app.feature.programming.LanguageComparisonScreen
import com.techenglish.app.feature.programming.LanguageDetailScreen
import com.techenglish.app.feature.programming.LanguagesCatalogScreen
import com.techenglish.app.feature.scenarios.PracticeHubScreen
import com.techenglish.app.feature.scenarios.ScenarioExerciseScreen
import com.techenglish.app.feature.stage2.DebugTheLogScreen
import com.techenglish.app.feature.stage2.DeckImportExportScreen
import com.techenglish.app.feature.stage2.PronunciationTrainerScreen
import com.techenglish.app.feature.stage2.ReadYourOwnDocsScreen
import com.techenglish.app.feature.stage2.TimeAttackScreen
import com.techenglish.app.feature.stage3.CloudAccountSyncScreen
import com.techenglish.app.feature.stage3.CommunityArenaHubScreen
import com.techenglish.app.feature.stage3.DeveloperProfileCardScreen
import com.techenglish.app.feature.stage3.FriendsAndFeedScreen
import com.techenglish.app.feature.stage3.LeaderboardsScreen
import com.techenglish.app.feature.stage4a.CommentsMnemonicsScreen
import com.techenglish.app.feature.stage4a.CommunityDecksScreen
import com.techenglish.app.feature.stage4b.AiMentorScreen
import com.techenglish.app.feature.stage4b.CrowdsourcePrScreen
import com.techenglish.app.feature.stage4b.SquadsScreen
import com.techenglish.app.feature.stage4c.PeerReviewArenaScreen
import com.techenglish.app.feature.stage4c.PvpDuelScreen
import com.techenglish.app.feature.stage4c.ThematicRoomsScreen
import com.techenglish.app.feature.statistics.StatisticsScreen
import com.techenglish.app.feature.vocabulary.FlashcardsScreen
import com.techenglish.app.feature.vocabulary.VocabularyScreen

object Routes {
    const val HOME = "home"
    const val VOCABULARY = "vocabulary"
    const val LANGUAGES = "languages"
    const val PRACTICE = "practice"
    const val PROFILE = "profile"

    const val FLASHCARDS = "flashcards?categoryId={categoryId}"
    const val LANGUAGE_DETAIL = "language_detail/{languageId}"
    const val CODE_BREAKDOWN = "code_breakdown/{snippetId}"
    const val LANGUAGE_COMPARE = "language_compare?langAId={langAId}&langBId={langBId}"
    const val DOCS_LIST = "docs_list"
    const val DOC_READER = "doc_reader/{articleId}"
    const val SCENARIO_EXERCISE = "scenario_exercise/{scenarioId}"
    const val LISTENING = "listening"
    const val STATISTICS = "statistics"
    const val ACADEMIC_DICTIONARY = "academic_dictionary"
    const val ACADEMIC_TERM_DETAIL = "academic_term_detail/{termId}"
    const val ACADEMIC_SRS = "academic_srs"

    // Stage 2 offline trainers (ФТ-7.1 — ФТ-7.6)
    const val READ_OWN_DOCS = "read_own_docs"
    const val DEBUG_THE_LOG = "debug_the_log"
    const val PRONUNCIATION_TRAINER = "pronunciation_trainer"
    const val TIME_ATTACK = "time_attack"
    const val DECK_IMPORT_EXPORT = "deck_import_export"

    // Stage 3 cloud, sync, social & OTA (ФТ-9 — ФТ-12, ФТ-14.5)
    const val COMMUNITY_ARENA_HUB = "community_arena_hub"
    const val CLOUD_ACCOUNT_SYNC = "cloud_account_sync"
    const val DEVELOPER_CARD = "developer_card"
    const val LEADERBOARDS = "leaderboards"
    const val FRIENDS_HUB = "friends_hub"

    // Stage 4A community comments & decks (ФТ-13.1, ФТ-13.2)
    const val COMMENTS = "comments/{targetType}/{targetId}?targetTitle={targetTitle}"
    const val COMMUNITY_DECKS = "community_decks"

    // Stage 4B AI Mentor Proxy, Squads & Dictionary Crowdsourcing (ФТ-8.1–8.3, ФТ-14.3–14.4)
    const val AI_MENTOR = "ai_mentor"
    const val SQUADS_HUB = "squads_hub"
    const val CROWDSOURCE_PR = "crowdsource_pr"

    // Stage 4C Thematic Rooms WS, PvP 1v1 WS & Peer Review Arena (ФТ-13.3, ФТ-14.1, ФТ-14.2)
    const val THEMATIC_ROOMS = "thematic_rooms"
    const val PVP_DUEL = "pvp_duel"
    const val PEER_REVIEW_ARENA = "peer_review_arena"

    fun comments(targetType: String, targetId: Int, targetTitle: String = ""): String =
        "comments/$targetType/$targetId?targetTitle=${android.net.Uri.encode(targetTitle)}"

    fun flashcards(categoryId: Int? = null): String =
        "flashcards?categoryId=${categoryId ?: -1}"

    fun academicTermDetail(termId: Int): String = "academic_term_detail/$termId"

    fun languageDetail(languageId: Int): String = "language_detail/$languageId"
    fun codeBreakdown(snippetId: Int): String = "code_breakdown/$snippetId"
    fun languageCompare(langAId: Int = 1, langBId: Int = 8): String =
        "language_compare?langAId=$langAId&langBId=$langBId"

    fun docReader(articleId: Int): String = "doc_reader/$articleId"
    fun scenarioExercise(scenarioId: Int): String = "scenario_exercise/$scenarioId"
}

enum class BottomNavTab(
    val route: String,
    val labelRu: String,
    val icon: ImageVector
) {
    HOME(Routes.HOME, "Главная", Icons.Rounded.Home),
    VOCABULARY(Routes.VOCABULARY, "Словарь", Icons.Rounded.Style),
    LANGUAGES(Routes.LANGUAGES, "Языки", Icons.Rounded.Code),
    PRACTICE(Routes.PRACTICE, "Практика", Icons.Rounded.Forum),
    PROFILE(Routes.PROFILE, "Профиль", Icons.Rounded.Person)
}

@Composable
fun TechEnglishNavShell(
    openFlashcardsOnLaunch: Boolean = false,
    pendingDestination: String? = null,
    onDestinationConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val topLevelRoutes = BottomNavTab.entries.map { it.route }.toSet()
    val showBottomBar = currentDestination?.route in topLevelRoutes

    LaunchedEffect(openFlashcardsOnLaunch, pendingDestination) {
        val target = pendingDestination ?: if (openFlashcardsOnLaunch) "flashcards" else null
        if (target == "flashcards") {
            navController.navigate(Routes.flashcards()) { launchSingleTop = true }
            onDestinationConsumed()
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavTab.entries.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTopLevel(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.labelRu) },
                            label = { Text(tab.labelRu, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onStartLearning = { navController.navigate(Routes.flashcards()) },
                    onOpenVocabulary = { navigateTopLevel(Routes.VOCABULARY) },
                    onOpenAcademicDictionary = { navController.navigate(Routes.ACADEMIC_DICTIONARY) },
                    onOpenLanguages = { navigateTopLevel(Routes.LANGUAGES) },
                    onOpenCompare = { navController.navigate(Routes.languageCompare(1, 8)) },
                    onOpenDocs = { navController.navigate(Routes.DOCS_LIST) },
                    onOpenScenarios = { navigateTopLevel(Routes.PRACTICE) },
                    onOpenListening = { navController.navigate(Routes.LISTENING) },
                    onOpenStatistics = { navController.navigate(Routes.STATISTICS) },
                    onOpenLeaderboards = { navController.navigate(Routes.LEADERBOARDS) },
                    onOpenFriends = { navController.navigate(Routes.FRIENDS_HUB) }
                )
            }

            composable(Routes.VOCABULARY) {
                VocabularyScreen(
                    onStartFlashcards = { catId -> navController.navigate(Routes.flashcards(catId)) },
                    onOpenAcademicDictionary = { navController.navigate(Routes.ACADEMIC_DICTIONARY) },
                    onOpenDeckImportExport = { navController.navigate(Routes.DECK_IMPORT_EXPORT) },
                    onOpenCommunityDecks = { navController.navigate(Routes.COMMUNITY_DECKS) }
                )
            }

            composable(Routes.ACADEMIC_DICTIONARY) {
                AcademicDictionaryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTermDetail = { id -> navController.navigate(Routes.academicTermDetail(id)) },
                    onOpenAcademicSrs = { navController.navigate(Routes.ACADEMIC_SRS) }
                )
            }

            composable(
                route = Routes.ACADEMIC_TERM_DETAIL,
                arguments = listOf(navArgument("termId") { type = NavType.IntType })
            ) {
                AcademicTermDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenComments = { termId, termTitle ->
                        navController.navigate(Routes.comments("TERM", termId, termTitle))
                    }
                )
            }

            composable(Routes.ACADEMIC_SRS) {
                AcademicSrsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.FLASHCARDS,
                arguments = listOf(navArgument("categoryId") { type = NavType.IntType; defaultValue = -1 })
            ) {
                FlashcardsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.LANGUAGES) {
                LanguagesCatalogScreen(
                    onOpenLanguage = { id -> navController.navigate(Routes.languageDetail(id)) },
                    onOpenCompare = { a, b -> navController.navigate(Routes.languageCompare(a, b)) }
                )
            }

            composable(
                route = Routes.LANGUAGE_DETAIL,
                arguments = listOf(navArgument("languageId") { type = NavType.IntType })
            ) {
                LanguageDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSnippet = { snippetId -> navController.navigate(Routes.codeBreakdown(snippetId)) },
                    onCompareWith = { langId ->
                        val otherId = if (langId == 1) 8 else 1
                        navController.navigate(Routes.languageCompare(langId, otherId))
                    }
                )
            }

            composable(
                route = Routes.CODE_BREAKDOWN,
                arguments = listOf(navArgument("snippetId") { type = NavType.IntType })
            ) {
                CodeBreakdownScreen(
                    onBack = { navController.popBackStack() },
                    onOpenComments = { snippetId, snippetTitle ->
                        navController.navigate(Routes.comments("SNIPPET", snippetId, snippetTitle))
                    }
                )
            }

            composable(
                route = Routes.LANGUAGE_COMPARE,
                arguments = listOf(
                    navArgument("langAId") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("langBId") { type = NavType.IntType; defaultValue = 8 }
                )
            ) {
                LanguageComparisonScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSnippetBreakdown = { snippetId -> navController.navigate(Routes.codeBreakdown(snippetId)) }
                )
            }

            composable(Routes.PRACTICE) {
                PracticeHubScreen(
                    onOpenScenario = { scenarioId -> navController.navigate(Routes.scenarioExercise(scenarioId)) },
                    onOpenDocs = { navController.navigate(Routes.DOCS_LIST) },
                    onOpenListening = { navController.navigate(Routes.LISTENING) },
                    onOpenReadOwnDocs = { navController.navigate(Routes.READ_OWN_DOCS) },
                    onOpenDebugTheLog = { navController.navigate(Routes.DEBUG_THE_LOG) },
                    onOpenPronunciation = { navController.navigate(Routes.PRONUNCIATION_TRAINER) },
                    onOpenTimeAttack = { navController.navigate(Routes.TIME_ATTACK) }
                )
            }

            composable(
                route = Routes.SCENARIO_EXERCISE,
                arguments = listOf(navArgument("scenarioId") { type = NavType.IntType })
            ) {
                ScenarioExerciseScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.DOCS_LIST) {
                DocumentationListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenArticle = { id -> navController.navigate(Routes.docReader(id)) }
                )
            }

            composable(
                route = Routes.DOC_READER,
                arguments = listOf(navArgument("articleId") { type = NavType.IntType })
            ) {
                DocReaderScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.LISTENING) {
                ListeningScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenStatistics = { navController.navigate(Routes.STATISTICS) },
                    onOpenCloudSync = { navController.navigate(Routes.CLOUD_ACCOUNT_SYNC) },
                    onOpenDeveloperCard = { navController.navigate(Routes.DEVELOPER_CARD) },
                    onOpenCommunityArenaHub = { navController.navigate(Routes.COMMUNITY_ARENA_HUB) }
                )
            }

            composable(Routes.COMMUNITY_ARENA_HUB) {
                CommunityArenaHubScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLeaderboards = { navController.navigate(Routes.LEADERBOARDS) },
                    onOpenFriends = { navController.navigate(Routes.FRIENDS_HUB) },
                    onOpenCommunityDecks = { navController.navigate(Routes.COMMUNITY_DECKS) },
                    onOpenAiMentor = { navController.navigate(Routes.AI_MENTOR) },
                    onOpenSquads = { navController.navigate(Routes.SQUADS_HUB) },
                    onOpenCrowdsourcePr = { navController.navigate(Routes.CROWDSOURCE_PR) },
                    onOpenThematicRooms = { navController.navigate(Routes.THEMATIC_ROOMS) },
                    onOpenPvpDuel = { navController.navigate(Routes.PVP_DUEL) },
                    onOpenPeerReview = { navController.navigate(Routes.PEER_REVIEW_ARENA) }
                )
            }

            composable(Routes.STATISTICS) {
                StatisticsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.READ_OWN_DOCS) {
                ReadYourOwnDocsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.DEBUG_THE_LOG) {
                DebugTheLogScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PRONUNCIATION_TRAINER) {
                PronunciationTrainerScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.TIME_ATTACK) {
                TimeAttackScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.DECK_IMPORT_EXPORT) {
                DeckImportExportScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CLOUD_ACCOUNT_SYNC) {
                CloudAccountSyncScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.DEVELOPER_CARD) {
                DeveloperProfileCardScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.LEADERBOARDS) {
                LeaderboardsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.FRIENDS_HUB) {
                FriendsAndFeedScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.COMMENTS,
                arguments = listOf(
                    navArgument("targetType") { type = NavType.StringType; defaultValue = "TERM" },
                    navArgument("targetId") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("targetTitle") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                CommentsMnemonicsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.COMMUNITY_DECKS) {
                CommunityDecksScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.AI_MENTOR) {
                AiMentorScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SQUADS_HUB) {
                SquadsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CROWDSOURCE_PR) {
                CrowdsourcePrScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.THEMATIC_ROOMS) {
                ThematicRoomsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PVP_DUEL) {
                PvpDuelScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PEER_REVIEW_ARENA) {
                PeerReviewArenaScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
