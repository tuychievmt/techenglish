package com.techenglish.domain.usecase

import com.techenglish.domain.FakeProgrammingLanguageRepository
import com.techenglish.domain.FakeStudyLogRepository
import com.techenglish.domain.model.CodeSnippet
import com.techenglish.domain.model.KeywordTooltip
import com.techenglish.domain.model.LineExplanation
import com.techenglish.domain.model.ProgrammingLanguage
import com.techenglish.domain.model.StudyActionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgrammingAndComparisonUseCasesTest {

    private fun lang(id: Int, slug: String, name: String, domains: List<String>) = ProgrammingLanguage(
        id = id,
        slug = slug,
        name = name,
        descriptionEn = "$name language",
        descriptionRu = "Язык $name",
        historyEn = "History of $name",
        historyRu = "История $name",
        paradigms = listOf("OOP"),
        domains = domains,
        frameworks = listOf("${name}Kit"),
        typingSystem = "Static",
        memoryManagement = "GC",
        difficulty = "Intermediate",
        yearCreated = 2010,
        creator = "Team",
        accentColorHex = "#3776AB"
    )

    private fun snippet(id: Int, langId: Int, key: String) = CodeSnippet(
        id = id,
        languageId = langId,
        taskKey = key,
        taskTitleEn = key.uppercase(),
        taskTitleRu = "Задача $key",
        code = "// $key",
        explanationEn = "EN $key",
        explanationRu = "RU $key",
        lineExplanations = listOf(LineExplanation(1, "Comment", "Комментарий")),
        keywords = listOf(KeywordTooltip("async", "Async", "Асинхронно", "async fn"))
    )

    private val repo = FakeProgrammingLanguageRepository(
        languages = listOf(
            lang(1, "python", "Python", listOf("Backend", "Data/DB", "Scripting")),
            lang(4, "cpp", "C++", listOf("Systems", "Backend")),
            lang(5, "java", "Java", listOf("Backend", "Mobile")),
            lang(6, "kotlin", "Kotlin", listOf("Mobile", "Backend")),
            lang(8, "go", "Go", listOf("Backend", "Systems")),
            lang(19, "rust", "Rust", listOf("Systems", "Backend"))
        ),
        snippets = listOf(
            snippet(1, 1, "hello_world"),
            snippet(2, 1, "async_ops"),
            snippet(3, 8, "hello_world"),
            snippet(4, 8, "async_ops"),
            snippet(5, 8, "http_request"),
            snippet(6, 4, "hello_world"),
            snippet(7, 4, "async_ops"),
            snippet(8, 19, "hello_world"),
            snippet(9, 19, "async_ops"),
            snippet(10, 19, "http_request"),
            snippet(11, 5, "hello_world"),
            snippet(12, 5, "classes"),
            snippet(13, 6, "hello_world"),
            snippet(14, 6, "classes")
        )
    )

    @Test
    fun `filter languages by domain and query`() = runTest {
        val useCase = GetProgrammingLanguagesUseCase(repo)
        val systems = useCase(domainFilter = "Systems").first()
        assertEquals(listOf("C++", "Go", "Rust"), systems.map { it.name })

        val searched = useCase(query = "kot").first()
        assertEquals(listOf("Kotlin"), searched.map { it.name })
    }

    @Test
    fun `compare Python vs Go shows only shared task_keys`() = runTest {
        val compare = CompareLanguagesUseCase(repo)
        val result = compare(langAId = 1, langBId = 8)!!
        assertEquals("Python", result.languageA.name)
        assertEquals("Go", result.languageB.name)
        assertEquals(listOf("hello_world", "async_ops"), result.commonTasks.map { it.taskKey })
        assertEquals("hello_world", result.selectedTaskKey)
        assertEquals(1, result.snippetA?.languageId)
        assertEquals(8, result.snippetB?.languageId)
    }

    @Test
    fun `compare Cpp vs Rust and Java vs Kotlin select requested task_key`() = runTest {
        val compare = CompareLanguagesUseCase(repo)
        val cppRust = compare(langAId = 4, langBId = 19, requestedTaskKey = "async_ops")!!
        assertEquals("async_ops", cppRust.selectedTaskKey)
        assertEquals(listOf("hello_world", "async_ops"), cppRust.commonTasks.map { it.taskKey })

        val javaKotlin = compare(langAId = 5, langBId = 6, requestedTaskKey = "classes")!!
        assertEquals("classes", javaKotlin.selectedTaskKey)
        assertNotNull(javaKotlin.snippetA)
        assertNotNull(javaKotlin.snippetB)

        // Unknown language returns null
        assertNull(compare(langAId = 999, langBId = 6))
    }

    @Test
    fun `getCodeSnippet detail and logCodeBreakdown work`() = runTest {
        val detail = GetLanguageDetailUseCase(repo)
        assertEquals("Rust", detail.observeLanguage(19).first()?.name)
        assertEquals(3, detail.observeSnippets(19).first().size)

        val getSnippet = GetCodeSnippetUseCase(repo)
        val pair = getSnippet(8)
        assertNotNull(pair)
        assertEquals("Rust", pair!!.first.name)
        assertNull(getSnippet(999))

        val logRepo = FakeStudyLogRepository()
        LogCodeBreakdownUseCase(logRepo)(snippetId = 8, nowMillis = 12345L)
        assertEquals(1, logRepo.currentLogs.size)
        assertEquals(StudyActionType.CODE_BREAKDOWN, logRepo.currentLogs.first().actionType)
        assertEquals(8, logRepo.currentLogs.first().snippetId)
        assertTrue(logRepo.currentLogs.first().xpEarned > 0)
    }
}
