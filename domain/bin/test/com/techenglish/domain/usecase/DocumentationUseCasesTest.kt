package com.techenglish.domain.usecase

import com.techenglish.domain.FakeDocumentationRepository
import com.techenglish.domain.FakeStudyLogRepository
import com.techenglish.domain.FakeVocabularyRepository
import com.techenglish.domain.model.DocArticle
import com.techenglish.domain.model.GlossaryEntry
import com.techenglish.domain.model.StudyActionType
import com.techenglish.domain.model.Word
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentationUseCasesTest {

    private val article = DocArticle(
        id = 1,
        title = "Kotlin Coroutines",
        techTag = "Programming",
        content = "Structured concurrency prevents orphaned coroutines and memory leaks.",
        explanationRu = "Структурная конкурентность.",
        level = "B2",
        readingProgress = 0f,
        isCompleted = false,
        glossary = listOf(
            GlossaryEntry(
                term = "orphaned",
                transcription = "/ˈɔːr.fənd/",
                translation = "осиротевший",
                explanationRu = "Потерявший родительскую область.",
                example = "Orphaned jobs leak memory."
            )
        )
    )

    @Test
    fun `filter doc articles by tech tag and query`() = runTest {
        val repo = FakeDocumentationRepository(listOf(article))
        val getArticles = GetDocArticlesUseCase(repo)
        val getDetail = GetDocArticleDetailUseCase(repo)

        assertEquals(1, getArticles(techTag = "Programming").first().size)
        assertEquals(0, getArticles(techTag = "Linux").first().size)
        assertEquals(1, getArticles(query = "corout").first().size)
        assertEquals("Kotlin Coroutines", getDetail(1).first()?.title)
    }

    @Test
    fun `updateReadingProgress awards XP once upon completion`() = runTest {
        val docRepo = FakeDocumentationRepository(listOf(article))
        val logRepo = FakeStudyLogRepository()
        val update = UpdateReadingProgressUseCase(docRepo, logRepo)

        update(articleId = 1, progress = 0.5f, nowMillis = 1000L)
        assertEquals(0.5f, docRepo.getArticleById(1)!!.readingProgress, 0.001f)
        assertFalse(docRepo.getArticleById(1)!!.isCompleted)
        assertEquals(0, logRepo.currentLogs.size)

        update(articleId = 1, progress = 1f, markCompleted = true, nowMillis = 2000L)
        assertTrue(docRepo.getArticleById(1)!!.isCompleted)
        assertEquals(1, logRepo.currentLogs.size)
        assertEquals(StudyActionType.DOC_READ, logRepo.currentLogs.first().actionType)

        // Second completion call does not duplicate XP log
        update(articleId = 1, progress = 1f, markCompleted = true, nowMillis = 3000L)
        assertEquals(1, logRepo.currentLogs.size)
    }

    @Test
    fun `lookupWord resolves glossary, vocabulary DB, common tech words, and fallback`() = runTest {
        val vocabRepo = FakeVocabularyRepository(
            listOf(
                Word(
                    id = 5,
                    categoryId = 8,
                    categoryName = "Programming",
                    term = "concurrency",
                    transcription = "/kənˈkʌr.ən.si/",
                    translation = "конкурентность",
                    example = "Concurrency in Kotlin.",
                    explanationRu = "Параллельная работа задач."
                )
            )
        )
        val lookup = LookupWordUseCase(vocabRepo)

        // 1. Article glossary match
        val gloss = lookup("orphaned,", article)
        assertEquals("осиротевший", gloss.translation)

        // 2. Exact Room DB match
        val dbHit = lookup("Concurrency.", article)
        assertEquals("конкурентность", dbHit.translation)
        assertTrue(dbHit.isAlreadyInVocabulary)

        // 3. Built-in common tech word with plural stem ('queries' -> 'query')
        val common = lookup("queries", article)
        assertEquals("запрос", common.translation)

        // 4. Unknown token graceful fallback
        val fallback = lookup("xyzabc", article)
        assertEquals("xyzabc", fallback.term)
        assertFalse(fallback.isAlreadyInVocabulary)
    }
}
