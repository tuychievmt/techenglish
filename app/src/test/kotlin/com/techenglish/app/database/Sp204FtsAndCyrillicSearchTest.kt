package com.techenglish.app.database

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.database.TechEnglishDatabase
import com.techenglish.app.core.database.entity.AcademicTermEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class Sp204FtsAndCyrillicSearchTest {

    private lateinit var context: Context
    private lateinit var db: TechEnglishDatabase
    private lateinit var dbName: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbName = "sp204_test_${UUID.randomUUID()}.db"
        db = Room.databaseBuilder(context, TechEnglishDatabase::class.java, dbName)
            .createFromAsset(TechEnglishDatabase.SEED_ASSET_PATH)
            .addMigrations(*TechEnglishDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbName)
        File(context.getDatabasePath(dbName).path).delete()
    }

    @Test
    fun `cyrillic case-insensitive search and substring search work on academic terms`() = runTest {
        // Insert a term whose Russian translation starts with UPPERCASE Cyrillic 'Балансировщик'
        // SQLite LOWER() only folds ASCII, so v5 LIKE fails to match lowercase 'балансировщик' against 'Балансировщик'!
        val dao = db.academicTermDao()
        dao.insertTerm(
            AcademicTermEntity(
                term = "ZetaLoadBalancerProbe",
                normalizedTerm = "zetaloadbalancerprobe",
                partOfSpeech = "noun",
                translationRu = "Балансировщик Нагрузки Пробный",
                definitionEn = "Distributes incoming traffic across backend servers",
                definitionRu = "Распределяет входящий трафик между серверами",
                academicExplanationEn = "Network load distribution mechanism.",
                academicExplanationRu = "Механизм распределения сетевой нагрузки.",
                practicalExplanation = "Используется перед кластером API.",
                exampleEn = "Configure the load balancer health check.",
                exampleRu = "Настройте проверку здоровья балансировщика.",
                majorCategory = "Systems",
                categoryId = 1,
                subcategoryId = 1,
                domain = "Networking",
                subdomain = "Routing",
                difficultyLevel = "Intermediate",
                cefrLevel = "B2",
                verificationStatus = "verified"
            )
        )

        // Lowercase Cyrillic query MUST match capitalized Cyrillic translation
        val lowerMatches = dao.searchPaginated(normalizedQuery = "балансировщик", limit = 20, offset = 0)
        assertTrue(
            "Case-insensitive Cyrillic search failed to find 'Балансировщик' with query 'балансировщик'",
            lowerMatches.any { it.term == "ZetaLoadBalancerProbe" }
        )

        // Uppercase Cyrillic query normalized to lowercase MUST also match
        val upperQuery = "БАЛАНСИРОВЩИК".lowercase(Locale.ROOT)
        val upperMatches = dao.searchPaginated(normalizedQuery = upperQuery, limit = 20, offset = 0)
        assertTrue(
            "Uppercase Cyrillic query failed to find 'Балансировщик'",
            upperMatches.any { it.term == "ZetaLoadBalancerProbe" }
        )

        // Mid-word Cyrillic substring search (fallback when FTS prefix has no match)
        val substringMatches = dao.searchPaginated(normalizedQuery = "лансировщ", limit = 20, offset = 0)
        assertTrue(
            "Cyrillic substring search 'лансировщ' failed",
            substringMatches.any { it.term == "ZetaLoadBalancerProbe" }
        )
    }

    @Test
    fun `database is Room v6 with prebuilt FTS4 table and sub-15ms search latency on 30k terms`() = runTest {
        val rawDb = db.openHelper.writableDatabase
        val version = rawDb.query("PRAGMA user_version").use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        assertEquals("Prebuilt DB must be upgraded to Room v6 (R12)", 6, version)

        val ftsExists = rawDb.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='academic_terms_fts'"
        ).use { it.moveToFirst() }
        assertTrue("Expected FTS4 virtual table academic_terms_fts to exist", ftsExists)

        // Top up to >= 30,000 terms to benchmark search latency at 30k scale
        val dao = db.academicTermDao()
        val currentCount = dao.getVerifiedCount()
        val needed = (30_000 - currentCount).coerceAtLeast(0)
        if (needed > 0) {
            val batch = (1..needed).map { idx ->
                AcademicTermEntity(
                    term = "SyntheticBenchTerm$idx",
                    normalizedTerm = "syntheticbenchterm$idx",
                    partOfSpeech = "noun",
                    translationRu = "Синтетический Термин $idx",
                    definitionEn = "Synthetic benchmark definition for latency test $idx",
                    definitionRu = "Синтетическое определение $idx",
                    academicExplanationEn = "Benchmark entry",
                    academicExplanationRu = "Запись бенчмарка",
                    practicalExplanation = "Benchmark",
                    exampleEn = "Example $idx",
                    exampleRu = "Пример $idx",
                    majorCategory = "Systems",
                    categoryId = 1,
                    subcategoryId = 1,
                    domain = "Bench",
                    subdomain = "Bench",
                    difficultyLevel = "Intermediate",
                    cefrLevel = "B2",
                    verificationStatus = "verified"
                )
            }
            dao.insertAllTerms(batch)
        }
        assertTrue("Expected >= 30,000 terms for benchmark", dao.getVerifiedCount() >= 30_000)

        // Warmup
        dao.searchPaginated(normalizedQuery = "idempoten", limit = 40, offset = 0)

        // Measure 10 representative prefix/word queries across Latin & Cyrillic
        val queries = listOf(
            "idempoten", "kubernetes", "deadlock", "backpressure", "syntheticbenchterm1500",
            "транзакц", "кэшир", "синтетический", "маршрутиз", "асинхрон"
        )
        val startNs = System.nanoTime()
        queries.forEach { q ->
            val res = dao.searchPaginated(normalizedQuery = q, limit = 40, offset = 0)
            assertTrue("Expected non-empty results for '$q' on 30k dataset", res.isNotEmpty() || q == "кэшир")
        }
        val avgMs = (System.nanoTime() - startNs) / 1_000_000.0 / queries.size
        println("SP2-04 30k FTS4 search avg latency: ${"%.2f".format(avgMs)} ms")
        assertTrue("Expected avg search latency < 15 ms on 30k terms, actual=${"%.2f".format(avgMs)} ms", avgMs < 15.0)

        // Verify EXPLAIN QUERY PLAN uses FTS4 virtual table index
        val plan = buildString {
            rawDb.query(
                """
                EXPLAIN QUERY PLAN
                SELECT t.* FROM academic_terms t
                INNER JOIN academic_terms_fts ON t.id = academic_terms_fts.rowid
                WHERE t.verification_status = 'verified' AND academic_terms_fts MATCH 'idempoten*'
                LIMIT 40
                """.trimIndent()
            ).use { c ->
                while (c.moveToNext()) {
                    appendLine(c.getString(c.columnCount - 1))
                }
            }
        }
        assertTrue("Expected FTS4 virtual table index in query plan, got:\n$plan", plan.contains("academic_terms_fts"))
    }
}
