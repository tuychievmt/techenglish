package com.techenglish.app.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.techenglish.app.core.notifications.NotificationHelper
import com.techenglish.app.core.notifications.NotificationScheduler
import com.techenglish.app.core.notifications.ReviewReminderWorker
import com.techenglish.domain.model.AcademicDomainSummary
import com.techenglish.domain.model.AcademicMajorCategorySummary
import com.techenglish.domain.model.AcademicSearchFilter
import com.techenglish.domain.model.AcademicSrsMode
import com.techenglish.domain.model.AcademicTerm
import com.techenglish.domain.model.AcademicTermDetail
import com.techenglish.domain.model.Category
import com.techenglish.domain.model.CategoryProgress
import com.techenglish.domain.model.Word
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewReminderWorkerTest {

    @Test
    fun `reminder worker succeeds gracefully combining core and academic due words`() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        val helper = NotificationHelper(context)
        val fakeVocabRepo = object : VocabularyRepository {
            override suspend fun getDueCount(nowMillis: Long): Int = 5
            override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
            override fun observeCategoryProgress(): Flow<List<CategoryProgress>> = flowOf(emptyList())
            override fun observeAllWords(): Flow<List<Word>> = flowOf(emptyList())
            override fun observeDueWords(nowMillis: Long, limit: Int): Flow<List<Word>> = flowOf(emptyList())
            override fun observeDueCount(nowMillis: Long): Flow<Int> = flowOf(5)
            override fun observeTotalWordCount(): Flow<Int> = flowOf(5)
            override fun observeLearnedWordCount(): Flow<Int> = flowOf(0)
            override fun searchWords(query: String): Flow<List<Word>> = flowOf(emptyList())
            override suspend fun getDueWords(nowMillis: Long, limit: Int, categoryId: Int?): List<Word> = emptyList()
            override suspend fun getWordById(wordId: Int): Word? = null
            override suspend fun findByExactTerm(term: String): Word? = null
            override suspend fun findByTermPrefix(prefix: String): Word? = null
            override suspend fun getRandomWords(limit: Int): List<Word> = emptyList()
            override suspend fun getSystemCategoryId(): Int = 11
            override suspend fun insertWord(word: Word): Long = 1L
            override suspend fun updateWord(word: Word) = Unit
            override suspend fun updateSrsState(wordId: Int, repetitions: Int, easeFactor: Float, intervalDays: Int, nextReviewAt: Long) = Unit
            override suspend fun setFavorite(wordId: Int, isFavorite: Boolean) = Unit
            override suspend fun deleteCustomWord(wordId: Int): Boolean = false
        }

        val fakeAcademicRepo = object : AcademicVocabularyRepository {
            override suspend fun getDueSrsCount(nowMillis: Long): Int = 12
            override fun observeVerifiedCount(): Flow<Int> = flowOf(10308)
            override fun observeStudiedCount(): Flow<Int> = flowOf(0)
            override fun observeFavoritesCount(): Flow<Int> = flowOf(0)
            override fun observeDifficultCount(): Flow<Int> = flowOf(0)
            override fun observeDueSrsCount(nowMillis: Long): Flow<Int> = flowOf(12)
            override fun observeMajorCategoryCounts(): Flow<List<AcademicMajorCategorySummary>> = flowOf(emptyList())
            override fun observeDomainCounts(): Flow<List<AcademicDomainSummary>> = flowOf(emptyList())
            override fun observeRecentlyStudied(limit: Int): Flow<List<AcademicTerm>> = flowOf(emptyList())
            override suspend fun getVerifiedCount(): Int = 10308
            override suspend fun searchPaginated(filter: AcademicSearchFilter, nowMillis: Long, limit: Int, offset: Int): List<AcademicTerm> = emptyList()
            override suspend fun countMatching(filter: AcademicSearchFilter, nowMillis: Long): Int = 0
            override suspend fun getTermDetail(termId: Int): AcademicTermDetail? = null
            override suspend fun findByNormalizedTerm(normalizedTerm: String): AcademicTerm? = null
            override suspend fun getSrsDeckForMode(mode: AcademicSrsMode, nowMillis: Long, limit: Int): List<AcademicTerm> = emptyList()
            override suspend fun setFavorite(termId: Int, isFavorite: Boolean) = Unit
            override suspend fun setInSrs(termId: Int, isInSrs: Boolean) = Unit
            override suspend fun updateSrsProgress(termId: Int, repetitions: Int, easeFactor: Float, intervalDays: Int, nextReviewAt: Long, nowMillis: Long) = Unit
        }

        val worker = TestListenableWorkerBuilder<ReviewReminderWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker = ReviewReminderWorker(
                    appContext,
                    workerParameters,
                    fakeVocabRepo,
                    fakeAcademicRepo,
                    helper
                )
            })
            .build()

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `timezone delay computation handles same-day and next-day wrap-around accurately`() {
        val zone = ZoneId.of("Europe/Moscow")
        val morning = ZonedDateTime.of(2026, 11, 19, 10, 0, 0, 0, zone)

        // Target 19:00 same day -> 9 hours = 540 minutes
        assertEquals(540L, NotificationScheduler.computeInitialDelayMinutes(morning, 19, 0))

        // Target 09:00 already passed today -> wraps to 09:00 tomorrow (23 hours = 1380 minutes)
        assertEquals(1380L, NotificationScheduler.computeInitialDelayMinutes(morning, 9, 0))
        assertTrue(NotificationScheduler.computeInitialDelayMinutes(morning, 10, 0) >= 1L)
    }
}
