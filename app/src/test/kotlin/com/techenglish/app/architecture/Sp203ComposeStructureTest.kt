package com.techenglish.app.architecture

import com.techenglish.app.feature.academic.AcademicDictionaryUiState
import com.techenglish.app.feature.programming.LanguagesCatalogUiState
import com.techenglish.app.feature.stage3.FriendsAndFeedUiState
import com.techenglish.app.feature.stage3.LeaderboardsUiState
import com.techenglish.app.feature.vocabulary.VocabularyUiState
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Sp203ComposeStructureTest {

    private fun findAppKotlinRoot(): File {
        val candidates = listOf(
            File("src/main/kotlin/com/techenglish/app"),
            File("app/src/main/kotlin/com/techenglish/app")
        )
        return candidates.first { it.isDirectory }
    }

    @Test
    fun `no Compose screen or designsystem file exceeds 900 lines and split files stay under 450 lines`() {
        val root = findAppKotlinRoot()
        val uiDirs = listOf(
            File(root, "feature"),
            File(root, "core/designsystem")
        )
        val offendersOver900 = mutableListOf<String>()
        uiDirs.forEach { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val lines = file.readLines().size
                    if (lines > 900) {
                        offendersOver900 += "${file.relativeTo(root).path}: $lines lines"
                    }
                }
        }
        assertTrue(
            "Compose monolith files > 900 lines found:\n${offendersOver900.joinToString("\n")}",
            offendersOver900.isEmpty()
        )

        val splitDirs = listOf(
            File(root, "feature/stage3"),
            File(root, "feature/stage2"),
            File(root, "feature/academic"),
            File(root, "feature/programming"),
            File(root, "core/designsystem")
        )
        val screenFilesOver450 = mutableListOf<String>()
        splitDirs.forEach { dir ->
            dir.walkTopDown()
                .filter {
                    it.isFile && it.extension == "kt" &&
                        !it.name.endsWith("ViewModels.kt") &&
                        !it.name.endsWith("ViewModel.kt") &&
                        it.name != "Theme.kt"
                }
                .forEach { file ->
                    val lines = file.readLines().size
                    if (lines > 450) {
                        screenFilesOver450 += "${file.relativeTo(root).path}: $lines lines"
                    }
                }
        }
        assertTrue(
            "Split Compose screen/component files should be <= 450 lines:\n${screenFilesOver450.joinToString("\n")}",
            screenFilesOver450.isEmpty()
        )
    }

    @Test
    fun `key UiState models are annotated @Immutable and use ImmutableList`() {
        val root = findAppKotlinRoot()
        val stateClasses: List<Class<*>> = listOf(
            VocabularyUiState::class.java,
            AcademicDictionaryUiState::class.java,
            LanguagesCatalogUiState::class.java,
            LeaderboardsUiState::class.java,
            FriendsAndFeedUiState::class.java
        )

        val listFieldNotImmutable = mutableListOf<String>()
        stateClasses.forEach { cls ->
            cls.declaredFields.forEach { f ->
                if (List::class.java.isAssignableFrom(f.type)) {
                    val isImmutableList = f.type.name.startsWith("kotlinx.collections.immutable.")
                    if (!isImmutableList) {
                        listFieldNotImmutable += "${cls.simpleName}.${f.name}: ${f.type.name}"
                    }
                }
            }
        }
        assertTrue(
            "UiState List fields not using kotlinx.collections.immutable:\n${listFieldNotImmutable.joinToString("\n")}",
            listFieldNotImmutable.isEmpty()
        )

        val vmFiles = listOf(
            File(root, "feature/vocabulary/VocabularyViewModel.kt"),
            File(root, "feature/academic/AcademicViewModels.kt"),
            File(root, "feature/programming/ProgrammingViewModels.kt"),
            File(root, "feature/stage3/Stage3ViewModels.kt")
        )
        val missingImmutableAnno = vmFiles.filter { !it.readText().contains("@Immutable") }.map { it.name }
        assertTrue(
            "ViewModel files missing @Immutable on UiState: $missingImmutableAnno",
            missingImmutableAnno.isEmpty()
        )
    }

    @Test
    fun `edge-to-edge navigation avoids double system insets and syncs status bar style`() {
        val root = findAppKotlinRoot()
        val navSrc = File(root, "core/navigation/AppNavigation.kt").readText()
        assertTrue(
            "AppNavigation.kt NavHost must call consumeWindowInsets(innerPadding) to prevent double system insets",
            navSrc.contains("consumeWindowInsets")
        )

        val featureDir = File(root, "feature")
        val missingZeroInsets = mutableListOf<String>()
        featureDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                val scaffoldCount = Regex("""\bScaffold\s*\(""").findAll(text).count()
                if (scaffoldCount > 0) {
                    val zeroInsetsCount = Regex("""contentWindowInsets\s*=\s*WindowInsets\(\s*0""").findAll(text).count()
                    if (zeroInsetsCount < scaffoldCount) {
                        missingZeroInsets += "${file.relativeTo(root).path}: $zeroInsetsCount/$scaffoldCount Scaffolds set contentWindowInsets = WindowInsets(0, 0, 0, 0)"
                    }
                }
            }
        assertTrue(
            "Nested feature Scaffolds missing contentWindowInsets = WindowInsets(0, 0, 0, 0):\n${missingZeroInsets.joinToString("\n")}",
            missingZeroInsets.isEmpty()
        )

        val themeSrc = File(root, "core/designsystem/Theme.kt").readText()
        val mainActivitySrc = File(root, "MainActivity.kt").readText()
        val combined = themeSrc + "\n" + mainActivitySrc
        assertTrue(
            "Theme.kt or MainActivity.kt must sync edge-to-edge system bar style via DisposableEffect",
            combined.contains("DisposableEffect") && combined.contains("SystemBarStyle")
        )
    }

    @Test
    fun `no raw text emojis or checkmark and star unicode symbols remain in UI screens or Glance widget`() {
        val root = findAppKotlinRoot()
        val forbidden = listOf(
            "\u25B6", // ▶
            "\u25BA", // ►
            "\u2605", // ★
            "\u2606", // ☆
            "\u2713", // ✓
            "\u2714", // ✔
            "\u2717", // ✗
            "\u2715", // ✕
            "\uD83D\uDD25", // 🔥
            "\u26A1", // ⚡
            "\uD83C\uDFAF", // 🎯
            "\uD83D\uDCDA" // 📚
        )
        val offenders = mutableListOf<String>()
        File(root, "feature").walkTopDown()
            .filter {
                it.isFile && it.extension == "kt" &&
                    !it.name.endsWith("ViewModel.kt") &&
                    !it.name.endsWith("ViewModels.kt")
            }
            .forEach { file ->
                val text = file.readText()
                val found = forbidden.filter { text.contains(it) }
                if (found.isNotEmpty()) {
                    offenders += "${file.relativeTo(root).path}: $found"
                }
            }
        assertTrue(
            "UI screens/widget must use vector icons instead of raw emojis/symbols:\n${offenders.joinToString("\n")}",
            offenders.isEmpty()
        )
    }

    @Test
    fun `design system consistency regression checks for Vocabulary, AcademicDictionary, and TerminalSearchField`() {
        val root = findAppKotlinRoot()
        val vocabSrc = File(root, "feature/vocabulary/VocabularyScreen.kt").readText()
        assertTrue(
            "VocabularyScreen.kt must not hardcode stale '20 658' count",
            !vocabSrc.contains("20 658") && !vocabSrc.contains("20658")
        )

        val academicCompSrc = File(root, "feature/academic/AcademicComponents.kt").readText()
        val academicDictSrc = File(root, "feature/academic/AcademicDictionaryScreen.kt").readText()
        assertTrue(
            "AcademicComponents.kt must use flagship AudioEqualizerButton and BentoGlassCard",
            academicCompSrc.contains("AudioEqualizerButton") && academicCompSrc.contains("BentoGlassCard")
        )
        assertTrue(
            "AcademicDictionaryScreen.kt must use Icons.Rounded.PlayArrow and BentoGlassCard",
            academicDictSrc.contains("Icons.Rounded.PlayArrow") && academicDictSrc.contains("BentoGlassCard")
        )

        val searchSrc = File(root, "core/designsystem/TechIconsAndHaptics.kt").readText()
        assertTrue(
            "TerminalSearchField must enforce singleLine = true and TextOverflow.Ellipsis",
            searchSrc.contains("singleLine = true") && searchSrc.contains("TextOverflow.Ellipsis")
        )
    }
}
