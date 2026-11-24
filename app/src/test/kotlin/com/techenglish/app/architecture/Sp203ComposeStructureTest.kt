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
}
