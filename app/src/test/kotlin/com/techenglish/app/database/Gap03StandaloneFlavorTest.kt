package com.techenglish.app.database

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Gap03StandaloneFlavorTest {

    private fun findProjectRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, "settings.gradle.kts").exists()) cwd else cwd.parentFile
    }

    @Test
    fun `app gradle defines distribution flavors pad and standalone with bundled content_pack assets`() {
        val root = findProjectRoot()
        val appGradle = File(root, "app/build.gradle.kts").readText()

        assertTrue(
            "Expected flavorDimensions += \"distribution\" in app/build.gradle.kts",
            appGradle.contains("flavorDimensions += \"distribution\"")
        )
        assertTrue(
            "Expected pad productFlavor for Play Store AAB",
            appGradle.contains("create(\"pad\")")
        )
        assertTrue(
            "Expected standalone productFlavor for tester APK distribution",
            appGradle.contains("create(\"standalone\")")
        )
        assertTrue(
            "Expected standalone sourceSet to include content_pack/src/main/assets",
            Regex("""getByName\("standalone"\)\s*\{[^}]*content_pack/src/main/assets""", RegexOption.DOT_MATCHES_ALL)
                .containsMatchIn(appGradle)
        )
        assertFalse(
            "Prebuilt DB must remain in content_pack and not be permanently committed under app/src/main/assets",
            File(root, "app/src/main/assets/databases/tech_english.db").exists()
        )
    }
}
