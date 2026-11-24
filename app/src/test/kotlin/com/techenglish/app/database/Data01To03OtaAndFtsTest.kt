package com.techenglish.app.database

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.database.ContentPackImporter
import com.techenglish.app.core.database.TechEnglishDatabase
import com.techenglish.app.core.database.entity.AcademicTermEntity
import com.techenglish.app.core.database.entity.ProgrammingLanguageEntity
import com.techenglish.app.core.network.OtaPackManifestItemDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class Data01To03OtaAndFtsTest {

    private lateinit var db: TechEnglishDatabase
    private lateinit var importer: ContentPackImporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TechEnglishDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        importer = ContentPackImporter(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `ota term update preserves SRS fields and updates normalized_search for FTS4 lookup`() = runTest {
        val dao = db.academicTermDao()
        dao.insertTerm(
            AcademicTermEntity(
                term = "Backpressure",
                normalizedTerm = "backpressure",
                partOfSpeech = "noun",
                translationRu = "обратное давление",
                translationTg = "",
                definitionEn = "Flow control mechanism slowing fast producers when consumers lag.",
                definitionRu = "Механизм управления потоком для замедления быстрых производителей.",
                academicExplanationEn = "Flow control in reactive streams.",
                academicExplanationRu = "Управление потоком в реактивных потоках.",
                practicalExplanation = "Используется в Kafka и Kotlin Flow.",
                exampleEn = "Configure backpressure buffers.",
                exampleRu = "Настройте буферы обратного давления.",
                majorCategory = "Distributed Systems & Cloud Computing",
                categoryId = 106,
                subcategoryId = 1,
                domain = "Distributed Systems",
                subdomain = "Streaming",
                difficultyLevel = "Advanced",
                cefrLevel = "C1",
                isFavorite = true,
                isInSrs = true,
                repetitions = 5,
                easeFactor = 2.7f,
                intervalDays = 21
            )
        )

        val otaJson = """
            {
              "pack_id": "ota_tg_fts_v6",
              "version": 6,
              "domain": "Distributed Systems",
              "terms": [
                {
                  "term": "Backpressure",
                  "normalized_term": "backpressure",
                  "part_of_speech": "noun",
                  "translation_ru": "обратное давление потока",
                  "translation_tg": "фишори баръакс",
                  "definition_en": "Flow control mechanism slowing fast producers when consumers lag.",
                  "definition_ru": "Механизм управления потоком для замедления быстрых производителей.",
                  "major_category": "Distributed Systems & Cloud Computing",
                  "category_id": 106,
                  "subcategory_id": 1,
                  "domain": "Distributed Systems",
                  "verification_status": "verified",
                  "updated_at": 1764000000
                }
              ]
            }
        """.trimIndent()

        val report = importer.importFromJson(otaJson)
        assertEquals(1, report.updatedTermsPreservingSrs)

        val updated = dao.findByNormalizedTerm("backpressure")
        assertNotNull(updated)
        assertEquals("фишори баръакс", updated!!.translationTg)
        assertTrue("SRS repetitions must be preserved", updated.repetitions == 5 && updated.isFavorite && updated.isInSrs)
        assertTrue(
            "normalized_search must include newly imported Tajik/Russian translation",
            updated.normalizedSearch.contains("фишори баръакс")
        )
    }

    @Test
    fun `importFromJson imports docArticles scenarios workPhrases and codeSnippets in addition to terms`() = runTest {
        db.programmingLanguageDao().insertAll(
            listOf(
                ProgrammingLanguageEntity(
                    id = 1,
                    slug = "kotlin",
                    name = "Kotlin",
                    descriptionEn = "Modern language for Android and JVM",
                    descriptionRu = "Современный язык для Android и JVM",
                    historyEn = "Created by JetBrains.",
                    historyRu = "Создан JetBrains.",
                    paradigms = "Multi-paradigm",
                    domains = "Android, Backend",
                    frameworks = "Compose, Ktor",
                    typingSystem = "Static",
                    memoryManagement = "GC",
                    difficulty = "Intermediate",
                    yearCreated = 2011,
                    creator = "JetBrains",
                    accentColorHex = "#7F52FF"
                )
            )
        )

        val multiTypeJson = """
            {
              "pack_id": "ota_multitype_v6",
              "version": 6,
              "domain": "Software Engineering",
              "terms": [],
              "doc_articles": [
                {
                  "id": 9001,
                  "title": "Structured Concurrency Guide",
                  "tech_tag": "Kotlin",
                  "content": "Coroutine scopes bound child lifecycles.",
                  "explanation_ru": "Области видимости корутин ограничивают жизненный цикл.",
                  "level": "B2",
                  "glossary_json": "{}",
                  "source_url": "https://kotlinlang.org/docs/coroutines-basics.html",
                  "license": "Apache-2.0"
                }
              ],
              "scenarios": [
                {
                  "id": 9001,
                  "type": "code_review",
                  "exercise_type": "multiple_choice",
                  "title": "Polite PR Nit",
                  "prompt": "Suggest extracting a helper function.",
                  "prompt_ru": "Предложите вынести вспомогательную функцию.",
                  "solution": "Nit: could we extract this into a small helper?",
                  "options_json": "[\"Nit: could we extract this into a small helper?\"]",
                  "explanation_ru": "Используйте мягкий маркер Nit.",
                  "useful_expressions_json": "[\"Nit: could we...\"]",
                  "difficulty": "Intermediate"
                }
              ],
              "work_phrases": [
                {
                  "id": 9001,
                  "category": "standup",
                  "phrase_en": "I am currently unblocked and wrapping up tests.",
                  "translation_ru": "Блокеров нет, завершаю тесты.",
                  "context_ru": "Ежедневный стендап",
                  "difficulty": "B1"
                }
              ],
              "code_snippets": [
                {
                  "language_id": 1,
                  "task_key": "ota_structured_scope",
                  "task_title_en": "Structured Scope",
                  "task_title_ru": "Структурированная область",
                  "code": "coroutineScope { launch { work() } }",
                  "explanation_en": "Waits for all child coroutines.",
                  "explanation_ru": "Ожидает завершения всех дочерних корутин.",
                  "line_explanations_json": "[]",
                  "keywords_json": "[\"coroutineScope\"]"
                }
              ]
            }
        """.trimIndent()

        val report = importer.importFromJson(multiTypeJson)
        assertEquals(1, report.upsertedDocs)
        assertEquals(1, report.upsertedScenarios)
        assertEquals(1, report.upsertedPhrases)
        assertEquals(1, report.upsertedSnippets)
        assertNotNull(db.docArticleDao().getById(9001))
    }

    @Test
    fun `OtaPackManifestItemDto parses optional external download_url`() {
        val json = Json { ignoreUnknownKeys = true }
        val dto = json.decodeFromString<OtaPackManifestItemDto>(
            """
            {
              "pack_id": "ota_r2_pack",
              "version": 6,
              "title_ru": "Внешний пакет",
              "domain": "DevOps",
              "item_count": 5,
              "sha256_hex": "abc123",
              "data_only": true,
              "download_url": "https://cdn.techenglish.app/packs/ota_r2_pack.json"
            }
            """.trimIndent()
        )
        assertEquals("https://cdn.techenglish.app/packs/ota_r2_pack.json", dto.downloadUrl)
    }
}
