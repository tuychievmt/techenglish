package com.techenglish.app.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Sp206NavigationAndHubTest {

    private fun findAppKotlinRoot(): File {
        val candidates = listOf(
            File("src/main/kotlin/com/techenglish/app"),
            File("app/src/main/kotlin/com/techenglish/app")
        )
        return candidates.first { it.isDirectory }
    }

    @Test
    fun `Community and Arena hub route exists and is wired in AppNavigation`() {
        val root = findAppKotlinRoot()
        val navSrc = File(root, "core/navigation/AppNavigation.kt").readText()
        assertTrue("Expected Routes.COMMUNITY_ARENA_HUB in AppNavigation.kt", navSrc.contains("COMMUNITY_ARENA_HUB"))

        val hubFile = File(root, "feature/stage3/CommunityArenaHubScreen.kt")
        assertTrue("Expected dedicated CommunityArenaHubScreen.kt", hubFile.exists())
        val hubSrc = hubFile.readText()
        assertTrue(hubSrc.contains("Сообщество и Арена"))
        assertTrue(hubSrc.contains("onOpenPvpDuel"))
        assertTrue(hubSrc.contains("onOpenPeerReview"))
        assertTrue(hubSrc.contains("onOpenAiMentor"))
    }

    @Test
    fun `ProfileScreen is slimmed to 5 sections and PracticeHubScreen removes 3 duplicate routes`() {
        val root = findAppKotlinRoot()
        val profileSrc = File(root, "feature/profile/ProfileScreen.kt").readText()
        assertTrue("ProfileScreen must expose single hub entry onOpenCommunityArenaHub", profileSrc.contains("onOpenCommunityArenaHub"))
        assertFalse("ProfileScreen must not directly host onOpenPvpDuel", profileSrc.contains("onOpenPvpDuel"))
        assertFalse("ProfileScreen must not directly host onOpenPeerReview", profileSrc.contains("onOpenPeerReview"))
        assertFalse("ProfileScreen must not directly host onOpenAiMentor", profileSrc.contains("onOpenAiMentor"))
        assertFalse("ProfileScreen must not directly host onOpenSquads", profileSrc.contains("onOpenSquads"))

        // Count LazyColumn section items (excluding the 1 header title item -> max 6 items total = 1 title + 5 sections)
        val itemCount = Regex("""\bitem\s*\{""").findAll(profileSrc).count()
        assertTrue(
            "Expected ProfileScreen to have <= 6 LazyColumn items (1 header + 5 sections), found $itemCount",
            itemCount <= 6
        )

        val practiceSrc = File(root, "feature/scenarios/ScenariosScreens.kt").readText()
        assertFalse("PracticeHubScreen must not duplicate onOpenAiMentor", practiceSrc.contains("onOpenAiMentor"))
        assertFalse("PracticeHubScreen must not duplicate onOpenPvpDuel", practiceSrc.contains("onOpenPvpDuel"))
        assertFalse("PracticeHubScreen must not duplicate onOpenPeerReview", practiceSrc.contains("onOpenPeerReview"))
    }
}
