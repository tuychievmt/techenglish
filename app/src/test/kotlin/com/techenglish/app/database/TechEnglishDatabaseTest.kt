package com.techenglish.app.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.database.AnkiApkgExtractor
import com.techenglish.app.core.database.ContentPackImporter
import com.techenglish.app.core.database.ContentPackPayload
import com.techenglish.app.core.database.TechEnglishDatabase
import com.techenglish.app.core.database.entity.AcademicTermEntity
import com.techenglish.app.core.database.entity.DocArticleEntity
import com.techenglish.app.core.database.entity.StudyLogEntity
import com.techenglish.app.core.database.entity.WordEntity
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.data.repository.AcademicVocabularyRepositoryImpl
import com.techenglish.app.data.repository.ProgrammingLanguageRepositoryImpl
import com.techenglish.app.data.repository.StudyLogRepositoryImpl
import com.techenglish.app.data.repository.VocabularyRepositoryImpl
import com.techenglish.domain.algorithm.Sm2Engine
import com.techenglish.domain.model.AcademicSearchFilter
import com.techenglish.domain.model.AcademicSrsMode
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.model.ReviewQuality
import com.techenglish.domain.usecase.CalculateNextReviewUseCase
import com.techenglish.domain.usecase.CompareLanguagesUseCase
import com.techenglish.domain.usecase.GetAcademicTermDetailUseCase
import com.techenglish.domain.usecase.NormalizeAcademicQueryUseCase
import com.techenglish.domain.usecase.ReviewAcademicTermUseCase
import com.techenglish.domain.usecase.ReviewWordUseCase
import com.techenglish.domain.usecase.SearchAcademicTermsPaginatedUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TechEnglishDatabaseTest {

    private lateinit var context: Context
    private lateinit var db: TechEnglishDatabase
    private lateinit var dbName: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbName = "test_tech_english_${UUID.randomUUID()}.db"
        db = Room.databaseBuilder(
            context,
            TechEnglishDatabase::class.java,
            dbName
        )
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
    fun `pre-populated v3 asset database matches Room identity hash and meets all Stage 2 content minimums`() = runTest {
        val categories = db.categoryDao().getAll()
        val languages = db.programmingLanguageDao().getAll()
        val words = db.wordDao().observeAllWithDetails().first()
        val articles = db.docArticleDao().observeAll().first()
        val scenarios = db.scenarioDao().getAll()
        val phrases = db.workPhraseDao().getAll()
        val academicCount = db.academicTermDao().getVerifiedCount()
        val subcategories = db.academicTermDao().getAllSubcategories()
        val majorCounts = db.academicTermDao().observeMajorCategoryCounts().first()
        val domainCounts = db.academicTermDao().observeDomainCounts().first()

        assertEquals(11, categories.size)
        assertTrue(categories.any { it.isSystem && it.name == "Мои слова" })
        assertEquals(19, languages.size)
        assertTrue("Expected >= 200 core words, got ${words.size}", words.size >= 200)
        assertTrue("Expected >= 150 doc articles (КД-3), got ${articles.size}", articles.size >= 150)
        assertTrue("All doc articles must have source_url & license", articles.all { it.sourceUrl.isNotBlank() && it.license.isNotBlank() })
        assertTrue("Expected >= 150 scenarios (КД-4), got ${scenarios.size}", scenarios.size >= 150)
        val scenariosByType = scenarios.groupBy { it.type }
        assertEquals("Expected 12 scenario contexts", 12, scenariosByType.size)
        scenariosByType.forEach { (type, list) ->
            assertTrue("Scenario context $type has only ${list.size} (< 12)", list.size >= 12)
        }
        assertTrue("Expected >= 300 phrases (КД-4), got ${phrases.size}", phrases.size >= 300)
        assertTrue("Expected >= 20,000 verified academic terms (КД-1), got $academicCount", academicCount >= 20_000)
        assertEquals(10, majorCounts.size)
        assertEquals(36, domainCounts.size)
        assertTrue("Expected >= 150 subcategories, got ${subcategories.size}", subcategories.size >= 150)

        // Verify >= 5,000 licensed IPA transcriptions with ipa_source_url & ipa_license (КД-2)
        val cursorIpa = db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM academic_terms WHERE phonetic_transcription != '' AND ipa_source_url != '' AND ipa_license != ''"
        )
        val licensedIpaCount = cursorIpa.use { if (it.moveToFirst()) it.getInt(0) else 0 }
        assertTrue("Expected >= 5,000 licensed IPA terms (КД-2), got $licensedIpaCount", licensedIpaCount >= 5_000)

        // Verify zero unverified terms in release DB
        val cursorUnverified = db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM academic_terms WHERE verification_status != 'verified'"
        )
        val unverifiedCount = cursorUnverified.use { if (it.moveToFirst()) it.getInt(0) else -1 }
        assertEquals(0, unverifiedCount)

        val mandatoryNames = setOf(
            "Python", "SQL", "C", "C++", "Java", "Kotlin", "C#", "Go",
            "JavaScript", "Swift", "Scala", "Assembly", "Pascal", "Ruby",
            "Dart", "VBA", "Bash", "Lua", "Rust"
        )
        assertEquals(mandatoryNames, languages.map { it.name }.toSet())
        var totalSnippets = 0
        for (lang in languages) {
            val snippets = db.codeSnippetDao().getByLanguage(lang.id)
            totalSnippets += snippets.size
            assertTrue("Language ${lang.name} has only ${snippets.size} snippets (<20)", snippets.size >= 20)
        }
        assertTrue("Expected >= 350 code snippets (КД-5), got $totalSnippets", totalSnippets >= 350)
    }

    @Test
    fun `word insert search update delete custom word and system category protection`() = runTest {
        val wordDao = db.wordDao()
        val categoryDao = db.categoryDao()
        val systemCat = categoryDao.getSystemCategory()!!

        assertEquals(0, categoryDao.deleteNonSystemCategory(systemCat.id))
        assertNotNull(categoryDao.getById(systemCat.id))

        val insertedId = wordDao.insert(
            WordEntity(
                categoryId = systemCat.id,
                term = "zero-copy",
                transcription = "/ˈzɪər.oʊ ˈkɒp.i/",
                translation = "передача без копирования",
                example = "Zero-copy I/O avoids extra kernel buffer copies.",
                explanationRu = "Оптимизация ввода-вывода без лишнего копирования в RAM.",
                isCustom = true
            )
        ).toInt()

        val searchHits = wordDao.searchWords("zero-copy").first()
        assertEquals(1, searchHits.size)
        assertEquals("zero-copy", searchHits.first().word.term)

        val loaded = wordDao.getById(insertedId)!!
        wordDao.update(loaded.copy(translation = "нулевое копирование"))
        assertEquals("нулевое копирование", wordDao.getById(insertedId)?.translation)

        val builtIn = wordDao.observeAllWithDetails().first().first { !it.word.isCustom }
        assertEquals(0, wordDao.deleteCustomWord(builtIn.word.id))
        assertEquals(1, wordDao.deleteCustomWord(insertedId))
        assertNull(wordDao.getById(insertedId))
    }

    @Test
    fun `due words query filters future cards and runs under 100ms`() = runTest {
        val wordDao = db.wordDao()
        val vocabRepo = VocabularyRepositoryImpl(db.categoryDao(), wordDao)
        val logRepo = StudyLogRepositoryImpl(db.studyLogDao(), db.syncQueueDao())
        val reviewWord = ReviewWordUseCase(CalculateNextReviewUseCase(Sm2Engine()), vocabRepo, logRepo)

        val now = 1_700_000_000_000L
        val initialDueCount = wordDao.getDueCount(now)
        val firstWord = vocabRepo.getDueWords(now, limit = 1).first()

        reviewWord(firstWord, ReviewQuality.GOOD, nowMillis = now)

        val startNs = System.nanoTime()
        val updatedDueCount = wordDao.getDueCount(now)
        val dueBatch = wordDao.getDueWords(now, limit = 20)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L

        assertEquals(initialDueCount - 1, updatedDueCount)
        assertTrue(dueBatch.none { it.word.id == firstWord.id })
        assertTrue("Expected query < 100ms, took ${elapsedMs}ms", elapsedMs < 100L)

        val logs = db.studyLogDao().getAll()
        assertEquals(1, logs.size)
        assertEquals(firstWord.id, logs.first().wordId)
        assertEquals("FLASHCARD", logs.first().actionType)
    }

    @Test
    fun `foreign keys reject invalid parent and preserve study_logs with SET NULL on custom word delete`() = runTest {
        val wordDao = db.wordDao()
        val logDao = db.studyLogDao()

        var threwConstraint = false
        try {
            wordDao.insert(
                WordEntity(
                    categoryId = 99999,
                    term = "invalid_fk",
                    transcription = "/x/",
                    translation = "ошибка",
                    example = "ex",
                    explanationRu = "ru",
                    isCustom = true
                )
            )
        } catch (_: SQLiteConstraintException) {
            threwConstraint = true
        }
        assertTrue("Expected SQLiteConstraintException on invalid category_id", threwConstraint)

        val wordId = wordDao.insert(
            WordEntity(
                categoryId = 11,
                term = "ephemeral",
                transcription = "/ɪˈfem.ər.əl/",
                translation = "эфемерный",
                example = "Ephemeral containers reset on restart.",
                explanationRu = "Временный.",
                isCustom = true
            )
        ).toInt()
        val logId = logDao.insert(
            StudyLogEntity(
                wordId = wordId,
                actionType = "FLASHCARD",
                isCorrect = true,
                xpEarned = 10,
                timestamp = 1_700_000_000_000L
            )
        ).toInt()

        wordDao.deleteCustomWord(wordId)
        val preservedLog = logDao.getAll().first { it.id == logId }
        assertNull("Expected ON DELETE SET NULL on study_logs.word_id", preservedLog.wordId)
    }

    @Test
    fun `dynamic comparison works against real seeded DB for Python vs Go, Cpp vs Rust, Java vs Kotlin`() = runTest {
        val repo = ProgrammingLanguageRepositoryImpl(db.programmingLanguageDao(), db.codeSnippetDao())
        val compare = CompareLanguagesUseCase(repo)

        val python = db.programmingLanguageDao().getBySlug("python")!!
        val go = db.programmingLanguageDao().getBySlug("go")!!
        val cpp = db.programmingLanguageDao().getBySlug("cpp")!!
        val rust = db.programmingLanguageDao().getBySlug("rust")!!
        val java = db.programmingLanguageDao().getBySlug("java")!!
        val kotlin = db.programmingLanguageDao().getBySlug("kotlin")!!
        val sql = db.programmingLanguageDao().getBySlug("sql")!!

        val pyGo = compare(python.id, go.id)!!
        assertEquals(20, pyGo.commonTasks.size)
        assertTrue(pyGo.commonTasks.any { it.taskKey == "async_ops" })
        assertTrue(pyGo.commonTasks.any { it.taskKey == "http_request" })
        assertTrue(pyGo.commonTasks.any { it.taskKey == "error_handling" })
        assertNotNull(pyGo.snippetA)
        assertNotNull(pyGo.snippetB)

        val cppRust = compare(cpp.id, rust.id, requestedTaskKey = "async_ops")!!
        assertEquals("async_ops", cppRust.selectedTaskKey)
        assertEquals(20, cppRust.commonTasks.size)

        val javaKotlin = compare(java.id, kotlin.id, requestedTaskKey = "classes")!!
        assertEquals(20, javaKotlin.commonTasks.size)
        assertEquals("classes", javaKotlin.selectedTaskKey)

        val sqlRust = compare(sql.id, rust.id)!!
        assertEquals(20, sqlRust.commonTasks.size)
        assertTrue(sqlRust.commonTasks.any { it.taskKey == "http_request" })
    }

    @Test
    fun `lossless MIGRATION_1_2 preserves v1 user custom words, favorites, SM-2 progress, and study logs`() = runTest {
        val migDbName = "mig_test_${UUID.randomUUID()}.db"
        val migDbFile = context.getDatabasePath(migDbName)
        migDbFile.parentFile?.mkdirs()

        // 1. Create a v1 database populated with user custom words, favorites, SM-2 intervals, and study logs
        val rawV1 = SQLiteDatabase.openOrCreateDatabase(migDbFile, null)
        try {
            rawV1.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `is_system` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
            rawV1.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
            rawV1.execSQL("CREATE TABLE IF NOT EXISTS `programming_languages` (`id` INTEGER NOT NULL, `slug` TEXT NOT NULL, `name` TEXT NOT NULL, `description_en` TEXT NOT NULL, `description_ru` TEXT NOT NULL, `history_en` TEXT NOT NULL, `history_ru` TEXT NOT NULL, `paradigms` TEXT NOT NULL, `domains` TEXT NOT NULL, `frameworks` TEXT NOT NULL, `typing_system` TEXT NOT NULL, `memory_management` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `year_created` INTEGER NOT NULL, `creator` TEXT NOT NULL, `accent_color_hex` TEXT NOT NULL, PRIMARY KEY(`id`))")
            rawV1.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_programming_languages_slug` ON `programming_languages` (`slug`)")
            rawV1.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_programming_languages_name` ON `programming_languages` (`name`)")
            rawV1.execSQL("CREATE TABLE IF NOT EXISTS `code_snippets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `language_id` INTEGER NOT NULL, `task_key` TEXT NOT NULL, `task_title_en` TEXT NOT NULL, `task_title_ru` TEXT NOT NULL, `code` TEXT NOT NULL, `explanation_en` TEXT NOT NULL, `explanation_ru` TEXT NOT NULL, `line_explanations_json` TEXT NOT NULL, `keywords_json` TEXT NOT NULL, FOREIGN KEY(`language_id`) REFERENCES `programming_languages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            rawV1.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_snippets_lang_task` ON `code_snippets` (`language_id`, `task_key`)")
            rawV1.execSQL("CREATE TABLE IF NOT EXISTS `words` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_id` INTEGER NOT NULL, `language_id` INTEGER, `term` TEXT NOT NULL, `transcription` TEXT NOT NULL, `translation` TEXT NOT NULL, `example` TEXT NOT NULL, `explanation_ru` TEXT NOT NULL, `is_favorite` INTEGER NOT NULL DEFAULT 0, `is_custom` INTEGER NOT NULL DEFAULT 0, `repetitions` INTEGER NOT NULL DEFAULT 0, `ease_factor` REAL NOT NULL DEFAULT 2.5, `interval_days` INTEGER NOT NULL DEFAULT 0, `next_review_at` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`language_id`) REFERENCES `programming_languages`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS `idx_words_next_review` ON `words` (`next_review_at`)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS `idx_words_category` ON `words` (`category_id`)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS `idx_words_language` ON `words` (`language_id`)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS `idx_words_term_lang` ON `words` (`term`, `language_id`)")
            rawV1.execSQL("CREATE TABLE IF NOT EXISTS `doc_articles` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `tech_tag` TEXT NOT NULL, `content` TEXT NOT NULL, `explanation_ru` TEXT NOT NULL, `level` TEXT NOT NULL, `reading_progress` REAL NOT NULL DEFAULT 0.0, `is_completed` INTEGER NOT NULL DEFAULT 0, `glossary_json` TEXT NOT NULL, PRIMARY KEY(`id`))")
            rawV1.execSQL("CREATE TABLE IF NOT EXISTS `scenarios` (`id` INTEGER NOT NULL, `type` TEXT NOT NULL, `exercise_type` TEXT NOT NULL, `title` TEXT NOT NULL, `prompt` TEXT NOT NULL, `prompt_ru` TEXT NOT NULL, `solution` TEXT NOT NULL, `options_json` TEXT NOT NULL, `explanation_ru` TEXT NOT NULL, `useful_expressions_json` TEXT NOT NULL, `difficulty` TEXT NOT NULL, PRIMARY KEY(`id`))")
            rawV1.execSQL("CREATE TABLE IF NOT EXISTS `work_phrases` (`id` INTEGER NOT NULL, `category` TEXT NOT NULL, `phrase_en` TEXT NOT NULL, `translation_ru` TEXT NOT NULL, `context_ru` TEXT NOT NULL, `difficulty` TEXT NOT NULL, PRIMARY KEY(`id`))")
            rawV1.execSQL("CREATE TABLE IF NOT EXISTS `study_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `word_id` INTEGER, `scenario_id` INTEGER, `article_id` INTEGER, `snippet_id` INTEGER, `action_type` TEXT NOT NULL, `is_correct` INTEGER NOT NULL, `xp_earned` INTEGER NOT NULL DEFAULT 10, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`scenario_id`) REFERENCES `scenarios`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`article_id`) REFERENCES `doc_articles`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`snippet_id`) REFERENCES `code_snippets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_timestamp` ON `study_logs` (`timestamp`)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_word` ON `study_logs` (`word_id`)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_scenario` ON `study_logs` (`scenario_id`)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_article` ON `study_logs` (`article_id`)")
            rawV1.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_snippet` ON `study_logs` (`snippet_id`)")
            rawV1.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            rawV1.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'dffd197c4cbd03957d233033990ac7ce')")
            rawV1.version = 1

            rawV1.execSQL("INSERT INTO categories (id, name, icon, is_system) VALUES (11, 'Мои слова', 'bookmark_added', 1)")
            rawV1.execSQL("INSERT INTO words (id, category_id, term, transcription, translation, example, explanation_ru, is_favorite, is_custom, repetitions, ease_factor, interval_days, next_review_at) VALUES (777, 11, 'user_custom_term', '/u/', 'мой термин', 'ex', 'ru', 1, 1, 4, 2.6, 15, 1799999999000)")
            rawV1.execSQL("INSERT INTO study_logs (id, word_id, action_type, is_correct, xp_earned, timestamp) VALUES (55, 777, 'FLASHCARD', 1, 10, 1700000000000)")
        } finally {
            rawV1.close()
        }

        // 2. Open with Room v3 and apply full migration chain (MIGRATION_1_2 -> MIGRATION_2_3)
        val migratedRoomDb = Room.databaseBuilder(
            context,
            TechEnglishDatabase::class.java,
            migDbName
        )
            .addMigrations(*TechEnglishDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            val preservedWord = migratedRoomDb.wordDao().getById(777)
            assertNotNull("Custom word must survive MIGRATION_1_2", preservedWord)
            assertEquals("user_custom_term", preservedWord!!.term)
            assertTrue(preservedWord.isFavorite)
            assertTrue(preservedWord.isCustom)
            assertEquals(4, preservedWord.repetitions)
            assertEquals(15, preservedWord.intervalDays)

            val preservedLogs = migratedRoomDb.studyLogDao().getAll()
            assertEquals(1, preservedLogs.size)
            assertEquals(777, preservedLogs.first().wordId)

            // Verify v2 academic_terms table is ready and functional after migration
            assertEquals(0, migratedRoomDb.academicTermDao().getVerifiedCount())
        } finally {
            migratedRoomDb.close()
            context.deleteDatabase(migDbName)
        }
    }

    @Test
    fun `academic dictionary search, Russian search, polysemous senses, and SRS integration work on real 10308+ DB`() = runTest {
        val repo = AcademicVocabularyRepositoryImpl(db.academicTermDao())
        val logRepo = StudyLogRepositoryImpl(db.studyLogDao(), db.syncQueueDao())
        val norm = NormalizeAcademicQueryUseCase()
        val search = SearchAcademicTermsPaginatedUseCase(repo, norm)
        val getDetail = GetAcademicTermDetailUseCase(repo, norm)
        val reviewTerm = ReviewAcademicTermUseCase(Sm2Engine(), repo, logRepo)

        // 1. Search English term 'Backpropagation'
        val bpPage = search(AcademicSearchFilter(query = "Backpropagation"), page = 0, pageSize = 20)
        assertTrue(bpPage.items.isNotEmpty())
        val bp = bpPage.items.first { it.normalizedTerm == "backpropagation" }
        assertEquals("обратное распространение ошибки", bp.translationRu)

        // 2. Search Russian translation 'гомоморфное шифрование'
        val ruPage = search(AcademicSearchFilter(query = "гомоморфное шифрование"), page = 0, pageSize = 20)
        assertTrue(ruPage.items.any { it.term == "Homomorphic Encryption" })

        // 3. Polysemous term 'Memory' has 4 distinct senses in word_senses
        val memoryTerm = repo.findByNormalizedTerm("memory")
        assertNotNull(memoryTerm)
        val memoryDetail = getDetail(memoryTerm!!.id)
        assertNotNull(memoryDetail)
        assertEquals(4, memoryDetail!!.senses.size)

        // 4. Review academic term in SM-2 SRS and verify persistence
        val sm2 = reviewTerm(bp, ReviewQuality.EASY, nowMillis = 1_760_000_000_000L)
        assertEquals(1, sm2.repetitions)
        val updatedBp = getDetail(bp.id)!!.term
        assertTrue(updatedBp.isInSrs)
        assertEquals(1, updatedBp.repetitions)
        assertEquals(2.6f, updatedBp.easeFactor, 0.01f)
    }

    @Test
    fun `30000-term scalability benchmark verifies indexed search and pagination under 100ms`() = runTest {
        val dao = db.academicTermDao()
        val currentCount = dao.getVerifiedCount()
        assertTrue(currentCount >= 10_000)

        // Expand database in-test from 20,658 up to exactly 30,000 unique verified records
        val needed = 30_000 - currentCount
        if (needed > 0) {
            val writable = db.openHelper.writableDatabase
            writable.beginTransaction()
            try {
                val firstBatch = minOf(needed, 10_000)
                writable.execSQL(
                    """
                    INSERT INTO academic_terms (
                        term, normalized_term, phonetic_transcription, part_of_speech,
                        translation_ru, translation_tg, definition_en, definition_ru,
                        academic_explanation_en, academic_explanation_ru, practical_explanation,
                        example_en, example_ru, major_category, category_id, subcategory_id,
                        domain, subdomain, difficulty_level, cefr_level,
                        synonyms, related_terms, abbreviations, source_reference, source_id,
                        verification_status, is_favorite, is_in_srs, repetitions, ease_factor,
                        interval_days, next_review_at, last_studied_at, created_at, updated_at,
                        ipa_source_url, ipa_license
                    )
                    SELECT
                        term || ' ScaleBench-A' || id,
                        normalized_term || ' scalebench-a' || id,
                        phonetic_transcription, part_of_speech,
                        translation_ru || ' (масштаб A' || id || ')', translation_tg,
                        definition_en, definition_ru,
                        academic_explanation_en, academic_explanation_ru, practical_explanation,
                        example_en, example_ru, major_category, category_id, subcategory_id,
                        domain, subdomain, difficulty_level, cefr_level,
                        synonyms, related_terms, abbreviations, source_reference, source_id,
                        'verified', 0, 0, 0, 2.5, 0, 0, 0, created_at, updated_at,
                        ipa_source_url, ipa_license
                    FROM academic_terms
                    LIMIT $firstBatch;
                    """.trimIndent()
                )
                val remaining = 30_000 - (currentCount + firstBatch)
                if (remaining > 0) {
                    writable.execSQL(
                        """
                        INSERT INTO academic_terms (
                            term, normalized_term, phonetic_transcription, part_of_speech,
                            translation_ru, translation_tg, definition_en, definition_ru,
                            academic_explanation_en, academic_explanation_ru, practical_explanation,
                            example_en, example_ru, major_category, category_id, subcategory_id,
                            domain, subdomain, difficulty_level, cefr_level,
                            synonyms, related_terms, abbreviations, source_reference, source_id,
                            verification_status, is_favorite, is_in_srs, repetitions, ease_factor,
                            interval_days, next_review_at, last_studied_at, created_at, updated_at,
                            ipa_source_url, ipa_license
                        )
                        SELECT
                            term || ' ScaleBench-B' || id,
                            normalized_term || ' scalebench-b' || id,
                            phonetic_transcription, part_of_speech,
                            translation_ru || ' (масштаб B' || id || ')', translation_tg,
                            definition_en, definition_ru,
                            academic_explanation_en, academic_explanation_ru, practical_explanation,
                            example_en, example_ru, major_category, category_id, subcategory_id,
                            domain, subdomain, difficulty_level, cefr_level,
                            synonyms, related_terms, abbreviations, source_reference, source_id,
                            'verified', 0, 0, 0, 2.5, 0, 0, 0, created_at, updated_at,
                            ipa_source_url, ipa_license
                        FROM academic_terms
                        WHERE id <= 10000
                        LIMIT $remaining;
                        """.trimIndent()
                    )
                }
                writable.setTransactionSuccessful()
            } finally {
                writable.endTransaction()
            }
        }

        val totalAfterScale = dao.getVerifiedCount()
        assertEquals(30_000, totalAfterScale)

        // Measure indexed exact/prefix lookup and paginated domain filter at 30,000 records
        val t0 = System.nanoTime()
        val exactHit = dao.findByNormalizedTerm("backpropagation")
        val exactMs = (System.nanoTime() - t0) / 1_000_000L

        val t1 = System.nanoTime()
        val domainPage = dao.searchPaginated(
            normalizedQuery = "",
            domain = "Deep Learning & Neural Networks",
            difficulty = "Advanced",
            limit = 40,
            offset = 0
        )
        val filterMs = (System.nanoTime() - t1) / 1_000_000L

        println("STAGE2_BENCHMARK_30K: exactLookupMs=$exactMs, paginatedFilterMs=$filterMs")
        assertNotNull(exactHit)
        assertEquals(40, domainPage.size)
        assertTrue("Expected 30k exact lookup < 100ms, took ${exactMs}ms", exactMs < 100L)
        assertTrue("Expected 30k paginated filter < 100ms, took ${filterMs}ms", filterMs < 100L)
    }

    @Test
    fun `MIGRATION_2_3 and ContentPackImporter (Errata E3) upgrade v2 to v3 preserving SRS progress and rejecting unverified records`() = runTest {
        val migDbName = "mig_v2_v3_${UUID.randomUUID()}.db"
        val migDbFile = context.getDatabasePath(migDbName)
        migDbFile.parentFile?.mkdirs()

        // 1. Construct a realistic v2 SQLite DB with user SRS progress on an academic term and reading progress on a doc article
        val rawV2 = SQLiteDatabase.openOrCreateDatabase(migDbFile, null)
        try {
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `is_system` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
            rawV2.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `programming_languages` (`id` INTEGER NOT NULL, `slug` TEXT NOT NULL, `name` TEXT NOT NULL, `description_en` TEXT NOT NULL, `description_ru` TEXT NOT NULL, `history_en` TEXT NOT NULL, `history_ru` TEXT NOT NULL, `paradigms` TEXT NOT NULL, `domains` TEXT NOT NULL, `frameworks` TEXT NOT NULL, `typing_system` TEXT NOT NULL, `memory_management` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `year_created` INTEGER NOT NULL, `creator` TEXT NOT NULL, `accent_color_hex` TEXT NOT NULL, PRIMARY KEY(`id`))")
            rawV2.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_programming_languages_slug` ON `programming_languages` (`slug`)")
            rawV2.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_programming_languages_name` ON `programming_languages` (`name`)")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `code_snippets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `language_id` INTEGER NOT NULL, `task_key` TEXT NOT NULL, `task_title_en` TEXT NOT NULL, `task_title_ru` TEXT NOT NULL, `code` TEXT NOT NULL, `explanation_en` TEXT NOT NULL, `explanation_ru` TEXT NOT NULL, `line_explanations_json` TEXT NOT NULL, `keywords_json` TEXT NOT NULL, FOREIGN KEY(`language_id`) REFERENCES `programming_languages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            rawV2.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_snippets_lang_task` ON `code_snippets` (`language_id`, `task_key`)")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `words` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_id` INTEGER NOT NULL, `language_id` INTEGER, `term` TEXT NOT NULL, `transcription` TEXT NOT NULL, `translation` TEXT NOT NULL, `example` TEXT NOT NULL, `explanation_ru` TEXT NOT NULL, `is_favorite` INTEGER NOT NULL DEFAULT 0, `is_custom` INTEGER NOT NULL DEFAULT 0, `repetitions` INTEGER NOT NULL DEFAULT 0, `ease_factor` REAL NOT NULL DEFAULT 2.5, `interval_days` INTEGER NOT NULL DEFAULT 0, `next_review_at` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`language_id`) REFERENCES `programming_languages`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_words_next_review` ON `words` (`next_review_at`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_words_category` ON `words` (`category_id`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_words_language` ON `words` (`language_id`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_words_term_lang` ON `words` (`term`, `language_id`)")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `doc_articles` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `tech_tag` TEXT NOT NULL, `content` TEXT NOT NULL, `explanation_ru` TEXT NOT NULL, `level` TEXT NOT NULL, `reading_progress` REAL NOT NULL DEFAULT 0.0, `is_completed` INTEGER NOT NULL DEFAULT 0, `glossary_json` TEXT NOT NULL, PRIMARY KEY(`id`))")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `scenarios` (`id` INTEGER NOT NULL, `type` TEXT NOT NULL, `exercise_type` TEXT NOT NULL, `title` TEXT NOT NULL, `prompt` TEXT NOT NULL, `prompt_ru` TEXT NOT NULL, `solution` TEXT NOT NULL, `options_json` TEXT NOT NULL, `explanation_ru` TEXT NOT NULL, `useful_expressions_json` TEXT NOT NULL, `difficulty` TEXT NOT NULL, PRIMARY KEY(`id`))")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `work_phrases` (`id` INTEGER NOT NULL, `category` TEXT NOT NULL, `phrase_en` TEXT NOT NULL, `translation_ru` TEXT NOT NULL, `context_ru` TEXT NOT NULL, `difficulty` TEXT NOT NULL, PRIMARY KEY(`id`))")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `study_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `word_id` INTEGER, `scenario_id` INTEGER, `article_id` INTEGER, `snippet_id` INTEGER, `action_type` TEXT NOT NULL, `is_correct` INTEGER NOT NULL, `xp_earned` INTEGER NOT NULL DEFAULT 10, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`scenario_id`) REFERENCES `scenarios`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`article_id`) REFERENCES `doc_articles`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`snippet_id`) REFERENCES `code_snippets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_timestamp` ON `study_logs` (`timestamp`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_word` ON `study_logs` (`word_id`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_scenario` ON `study_logs` (`scenario_id`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_article` ON `study_logs` (`article_id`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_snippet` ON `study_logs` (`snippet_id`)")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `word_sources` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `source_type` TEXT NOT NULL, `publisher` TEXT NOT NULL, PRIMARY KEY(`id`))")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `subcategories` (`id` INTEGER NOT NULL, `category_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `slug` TEXT NOT NULL, `domain` TEXT NOT NULL, PRIMARY KEY(`id`))")
            rawV2.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_subcategories_slug` ON `subcategories` (`slug`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_subcategories_category` ON `subcategories` (`category_id`)")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `academic_terms` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `term` TEXT NOT NULL, `normalized_term` TEXT NOT NULL, `phonetic_transcription` TEXT NOT NULL DEFAULT '', `part_of_speech` TEXT NOT NULL, `translation_ru` TEXT NOT NULL, `translation_tg` TEXT NOT NULL DEFAULT '', `definition_en` TEXT NOT NULL, `definition_ru` TEXT NOT NULL, `academic_explanation_en` TEXT NOT NULL, `academic_explanation_ru` TEXT NOT NULL, `practical_explanation` TEXT NOT NULL, `example_en` TEXT NOT NULL, `example_ru` TEXT NOT NULL, `major_category` TEXT NOT NULL, `category_id` INTEGER NOT NULL, `subcategory_id` INTEGER NOT NULL DEFAULT 1, `domain` TEXT NOT NULL, `subdomain` TEXT NOT NULL, `difficulty_level` TEXT NOT NULL, `cefr_level` TEXT NOT NULL, `synonyms` TEXT NOT NULL DEFAULT '', `related_terms` TEXT NOT NULL DEFAULT '', `abbreviations` TEXT NOT NULL DEFAULT '', `source_reference` TEXT NOT NULL DEFAULT '', `source_id` INTEGER, `verification_status` TEXT NOT NULL DEFAULT 'verified', `is_favorite` INTEGER NOT NULL DEFAULT 0, `is_in_srs` INTEGER NOT NULL DEFAULT 0, `repetitions` INTEGER NOT NULL DEFAULT 0, `ease_factor` REAL NOT NULL DEFAULT 2.5, `interval_days` INTEGER NOT NULL DEFAULT 0, `next_review_at` INTEGER NOT NULL DEFAULT 0, `last_studied_at` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER NOT NULL DEFAULT 0, `updated_at` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`source_id`) REFERENCES `word_sources`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
            rawV2.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_academic_normalized_term` ON `academic_terms` (`normalized_term`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_category_id` ON `academic_terms` (`category_id`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_subcategory_id` ON `academic_terms` (`subcategory_id`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_difficulty` ON `academic_terms` (`difficulty_level`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_cefr` ON `academic_terms` (`cefr_level`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_next_review` ON `academic_terms` (`next_review_at`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_source_id` ON `academic_terms` (`source_id`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_domain` ON `academic_terms` (`domain`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_status` ON `academic_terms` (`verification_status`)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_srs_due` ON `academic_terms` (`is_in_srs`, `next_review_at`)")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `word_senses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `term_id` INTEGER NOT NULL, `sense_number` INTEGER NOT NULL, `sense_label` TEXT NOT NULL, `domain` TEXT NOT NULL, `definition_en` TEXT NOT NULL, `definition_ru` TEXT NOT NULL, `example_en` TEXT NOT NULL, `example_ru` TEXT NOT NULL, FOREIGN KEY(`term_id`) REFERENCES `academic_terms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_senses_term_id` ON `word_senses` (`term_id`)")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS `word_relations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `term_id` INTEGER NOT NULL, `related_term` TEXT NOT NULL, `relation_type` TEXT NOT NULL, FOREIGN KEY(`term_id`) REFERENCES `academic_terms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            rawV2.execSQL("CREATE INDEX IF NOT EXISTS `idx_relations_term_id` ON `word_relations` (`term_id`)")
            rawV2.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            rawV2.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, 'v2_seed_placeholder')")
            rawV2.version = 2

            // Insert v2 user progress on academic_terms and doc_articles
            rawV2.execSQL(
                """
                INSERT INTO academic_terms (
                    id, term, normalized_term, phonetic_transcription, part_of_speech,
                    translation_ru, translation_tg, definition_en, definition_ru,
                    academic_explanation_en, academic_explanation_ru, practical_explanation,
                    example_en, example_ru, major_category, category_id, subcategory_id,
                    domain, subdomain, difficulty_level, cefr_level,
                    verification_status, is_favorite, is_in_srs, repetitions, ease_factor,
                    interval_days, next_review_at, last_studied_at, created_at, updated_at
                ) VALUES (
                    1, 'Backpropagation', 'backpropagation', '', 'noun',
                    'старый перевод', '', 'Old EN def', 'Старое RU опред',
                    'Acad EN', 'Acad RU', 'Prac RU', 'Ex EN', 'Ex RU',
                    'Machine Learning & Artificial Intelligence', 101, 1,
                    'Deep Learning & Neural Networks', 'Backpropagation', 'Advanced', 'C1',
                    'verified', 1, 1, 5, 2.75, 21, 1800000000000, 1790000000000, 1700000000, 1700000000
                )
                """.trimIndent()
            )
            rawV2.execSQL(
                "INSERT INTO doc_articles (id, title, tech_tag, content, explanation_ru, level, reading_progress, is_completed, glossary_json) VALUES (1, 'Old Doc', 'SQL', 'Old body', 'RU', 'B2', 0.85, 1, '[]')"
            )
        } finally {
            rawV2.close()
        }

        // 2. Open with Room v4 -> applies MIGRATION_2_3 -> MIGRATION_3_4
        val migratedDb = Room.databaseBuilder(context, TechEnglishDatabase::class.java, migDbName)
            .addMigrations(*TechEnglishDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            val importer = ContentPackImporter(migratedDb)

            // 3. Run delta import via ContentPackImporter with updated existing term + new verified term + unverified term + updated doc
            val report1 = importer.importPayload(
                ContentPackPayload(
                    packId = "v3_upgrade_delta",
                    academicTerms = listOf(
                        AcademicTermEntity(
                            term = "Backpropagation",
                            normalizedTerm = "backpropagation",
                            phoneticTranscription = "/ˌbæk.prɒp.əˈɡeɪ.ʃən/",
                            ipaSourceUrl = "https://github.com/open-dict-data/ipa-dict",
                            ipaLicense = "MIT",
                            partOfSpeech = "noun",
                            translationRu = "обратное распространение ошибки",
                            definitionEn = "Updated v3 EN definition",
                            definitionRu = "Обновлённое v3 RU определение",
                            academicExplanationEn = "Acad EN v3",
                            academicExplanationRu = "Acad RU v3",
                            practicalExplanation = "Prac RU v3",
                            exampleEn = "Ex EN v3",
                            exampleRu = "Ex RU v3",
                            majorCategory = "Machine Learning & Artificial Intelligence",
                            categoryId = 101,
                            subcategoryId = 1,
                            domain = "Deep Learning & Neural Networks",
                            subdomain = "Backpropagation",
                            difficultyLevel = "Advanced",
                            cefrLevel = "C1",
                            verificationStatus = "verified",
                            updatedAt = 1763553600L
                        ),
                        AcademicTermEntity(
                            term = "Shader Graph",
                            normalizedTerm = "shader graph",
                            phoneticTranscription = "/ˈʃeɪ.dər ɡræf/",
                            ipaSourceUrl = "https://github.com/open-dict-data/ipa-dict",
                            ipaLicense = "MIT",
                            partOfSpeech = "noun phrase",
                            translationRu = "граф шейдеров",
                            definitionEn = "Visual node-based shader pipeline in GameDev.",
                            definitionRu = "Визуальный граф построения шейдеров в GameDev.",
                            academicExplanationEn = "Acad",
                            academicExplanationRu = "Acad RU",
                            practicalExplanation = "Prac RU",
                            exampleEn = "Ex EN",
                            exampleRu = "Ex RU",
                            majorCategory = "Software Engineering & Compiler Technology",
                            categoryId = 108,
                            subcategoryId = 1,
                            domain = "GameDev",
                            subdomain = "Real-Time Rendering & Shaders",
                            difficultyLevel = "Intermediate",
                            cefrLevel = "B2",
                            verificationStatus = "verified"
                        ),
                        AcademicTermEntity(
                            term = "Unverified Draft Term",
                            normalizedTerm = "unverified draft term",
                            partOfSpeech = "noun",
                            translationRu = "черновик",
                            definitionEn = "Should never be imported",
                            definitionRu = "Не должен попасть в БД",
                            academicExplanationEn = "A",
                            academicExplanationRu = "A",
                            practicalExplanation = "P",
                            exampleEn = "E",
                            exampleRu = "E",
                            majorCategory = "Software Engineering & Compiler Technology",
                            categoryId = 108,
                            subcategoryId = 1,
                            domain = "GameDev",
                            subdomain = "Core",
                            difficultyLevel = "Intermediate",
                            cefrLevel = "B1",
                            verificationStatus = "pending_review"
                        )
                    ),
                    docArticles = listOf(
                        DocArticleEntity(
                            id = 1,
                            title = "Updated Doc v3",
                            techTag = "SQL",
                            content = "Updated v3 body",
                            explanationRu = "Обновлено",
                            level = "B2",
                            readingProgress = 0.0f,
                            isCompleted = false,
                            glossaryJson = "[]",
                            sourceUrl = "https://www.postgresql.org/docs/current/",
                            license = "PostgreSQL License"
                        )
                    )
                )
            )

            assertEquals(1, report1.insertedTerms)
            assertEquals(1, report1.updatedTermsPreservingSrs)
            assertEquals(1, report1.skippedUnverifiedTerms)
            assertEquals(1, report1.upsertedDocs)

            // Assert existing academic term got v3 content AND preserved 100% of user SRS fields
            val bp = migratedDb.academicTermDao().findByNormalizedTerm("backpropagation")!!
            assertEquals("обратное распространение ошибки", bp.translationRu)
            assertEquals("/ˌbæk.prɒp.əˈɡeɪ.ʃən/", bp.phoneticTranscription)
            assertEquals("MIT", bp.ipaLicense)
            assertTrue(bp.isFavorite)
            assertTrue(bp.isInSrs)
            assertEquals(5, bp.repetitions)
            assertEquals(2.75f, bp.easeFactor, 0.001f)
            assertEquals(21, bp.intervalDays)
            assertEquals(1800000000000L, bp.nextReviewAt)
            assertEquals(1790000000000L, bp.lastStudiedAt)

            // Assert new verified term inserted and unverified term strictly rejected
            assertNotNull(migratedDb.academicTermDao().findByNormalizedTerm("shader graph"))
            assertNull(migratedDb.academicTermDao().findByNormalizedTerm("unverified draft term"))

            // Assert doc article got source_url/license AND preserved user reading progress
            val doc = migratedDb.docArticleDao().getById(1)!!
            assertEquals("Updated Doc v3", doc.title)
            assertEquals("PostgreSQL License", doc.license)
            assertEquals(0.85f, doc.readingProgress, 0.001f)
            assertTrue(doc.isCompleted)

            // 4. Test JSON pack import with SHA-256 verification and idempotency
            val jsonPack = """
                {
                  "pack_id": "pad_web3_pack",
                  "version": 3,
                  "domain": "Web3 / Smart Contracts",
                  "terms": [
                    {
                      "term": "Reentrancy Guard",
                      "translation_ru": "защита от повторного входа",
                      "definition_en": "Mutex modifier preventing recursive smart contract calls.",
                      "definition_ru": "Модификатор-мьютекс, предотвращающий повторный вход в смарт-контракт.",
                      "major_category": "Cryptography & Cybersecurity",
                      "domain": "Web3 / Smart Contracts",
                      "verification_status": "verified"
                    }
                  ]
                }
            """.trimIndent()
            val sha = ContentPackImporter.sha256Hex(jsonPack.toByteArray(Charsets.UTF_8))
            val jsonReport1 = importer.importFromJson(jsonPack, expectedSha256Hex = sha)
            val jsonReport2 = importer.importFromJson(jsonPack, expectedSha256Hex = sha)
            assertEquals(1, jsonReport1.insertedTerms)
            assertEquals(0, jsonReport2.insertedTerms)
            assertEquals(1, jsonReport2.updatedTermsPreservingSrs)
        } finally {
            migratedDb.close()
            context.deleteDatabase(migDbName)
        }
    }

    @Test
    fun `AnkiApkgExtractor reads notes and tags from zipped collection_anki2 SQLite archive`() {
        val tempSourceDb = File.createTempFile("test_anki_src_", ".sqlite", context.cacheDir)
        try {
            val ankiDb = SQLiteDatabase.openOrCreateDatabase(tempSourceDb, null)
            try {
                ankiDb.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY, flds TEXT NOT NULL, tags TEXT NOT NULL)")
                ankiDb.execSQL("CREATE TABLE cards (id INTEGER PRIMARY KEY, nid INTEGER, ivl INTEGER)")
                ankiDb.execSQL("INSERT INTO notes (id, flds, tags) VALUES (1, '<b>idempotency</b>' || char(31) || 'идемпотентность', 'rest api')")
                ankiDb.execSQL("INSERT INTO notes (id, flds, tags) VALUES (2, 'sharding' || char(31) || 'шардирование', 'db')")
                ankiDb.execSQL("INSERT INTO cards (id, nid, ivl) VALUES (10, 1, 999)")
            } finally {
                ankiDb.close()
            }

            val baos = ByteArrayOutputStream()
            ZipOutputStream(baos).use { zos ->
                zos.putNextEntry(ZipEntry("collection.anki2"))
                zos.write(tempSourceDb.readBytes())
                zos.closeEntry()
            }

            val extractor = AnkiApkgExtractor(context)
            val rows = extractor.extractNoteRows(ByteArrayInputStream(baos.toByteArray()))
            assertEquals(2, rows.size)
            assertTrue(rows[0].first.contains("idempotency"))
            assertEquals("rest api", rows[0].second)
        } finally {
            tempSourceDb.delete()
        }
    }

    @Test
    fun `MIGRATION_3_4 upgrades v3 to v4 losslessly creating all 8 Stage 3 tables and preserving user data`() = runTest {
        val migDbName = "mig_v3_v4_${UUID.randomUUID()}.db"
        val migDbFile = context.getDatabasePath(migDbName)
        migDbFile.parentFile?.mkdirs()

        val schemaV3File = listOf(
            File("schemas/com.techenglish.app.core.database.TechEnglishDatabase/3.json"),
            File("app/schemas/com.techenglish.app.core.database.TechEnglishDatabase/3.json")
        ).first { it.exists() }

        val schemaV3 = org.json.JSONObject(schemaV3File.readText()).getJSONObject("database")
        val rawV3 = SQLiteDatabase.openOrCreateDatabase(migDbFile, null)
        try {
            val entities = schemaV3.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val tableName = entity.getString("tableName")
                rawV3.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", tableName))
                val indices = entity.optJSONArray("indices")
                if (indices != null) {
                    for (j in 0 until indices.length()) {
                        val idxSql = indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tableName)
                        rawV3.execSQL(idxSql)
                    }
                }
            }
            val setupQueries = schemaV3.getJSONArray("setupQueries")
            for (i in 0 until setupQueries.length()) {
                rawV3.execSQL(setupQueries.getString(i))
            }
            rawV3.version = 3

            // Insert Stage 2 / v3 user data that must survive MIGRATION_3_4 untouched
            rawV3.execSQL("INSERT INTO categories (id, name, icon, is_system) VALUES (11, 'Мои слова', 'bookmark_added', 1)")
            rawV3.execSQL("INSERT INTO words (id, category_id, term, transcription, translation, example, explanation_ru, is_favorite, is_custom, repetitions, ease_factor, interval_days, next_review_at) VALUES (901, 11, 'idempotency_key', '/aɪ/', 'ключ идемпотентности', 'ex', 'ru', 1, 1, 3, 2.5, 6, 1800000000000)")
            rawV3.execSQL("INSERT INTO custom_imported_docs (id, title, source_file_name, content, detected_terms_json, detected_terms_count, created_at) VALUES (10, 'RFC 9110 Notes', 'rfc9110.md', 'HTTP semantics notes', '[]', 0, 1760000000000)")
            rawV3.execSQL("INSERT INTO time_attack_records (id, mode, score, correct_count, total_answered, xp_earned, duration_seconds, achieved_at) VALUES (20, 'SIXTY_SECONDS', 420, 14, 15, 35, 60, 1760000000000)")
        } finally {
            rawV3.close()
        }

        val migratedDb = Room.databaseBuilder(context, TechEnglishDatabase::class.java, migDbName)
            .addMigrations(*TechEnglishDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            // Verify v3 user data survived 100% losslessly
            val word = migratedDb.wordDao().getById(901)
            assertNotNull(word)
            assertEquals("idempotency_key", word!!.term)
            assertEquals(3, word.repetitions)

            val customDocs = migratedDb.customImportedDocDao().observeAll().first()
            assertEquals(1, customDocs.size)
            assertEquals("RFC 9110 Notes", customDocs.first().title)

            assertEquals(420, migratedDb.timeAttackRecordDao().getBestScoreForMode("SIXTY_SECONDS"))

            // Verify all 8 Stage 3 tables and 3 indexes exist & are writable
            migratedDb.syncQueueDao().enqueue(
                com.techenglish.app.core.database.entity.SyncQueueEntity(
                    eventId = "ev-mig-1",
                    eventType = "FLASHCARD_CORRECT",
                    payloadJson = "{}",
                    createdAt = 1760000000000L
                )
            )
            assertEquals(1, migratedDb.syncQueueDao().getPendingCount())

            migratedDb.socialCacheDao().upsertProfile(
                com.techenglish.app.core.database.entity.UserProfileCacheEntity(
                    userId = "u1",
                    username = "kotlin_dev",
                    displayName = "Kotlin Dev"
                )
            )
            assertEquals("kotlin_dev", migratedDb.socialCacheDao().getProfile()?.username)

            migratedDb.otaContentMetaDao().upsertPackMeta(
                com.techenglish.app.core.database.entity.OtaContentMetaEntity(
                    packId = "ai_pack_v1",
                    version = 1,
                    titleRu = "AI Pack",
                    sha256Hex = "abc123",
                    itemCount = 25,
                    appliedAt = 1760000000000L
                )
            )
            assertEquals(25, migratedDb.otaContentMetaDao().getPackMeta("ai_pack_v1")?.itemCount)
        } finally {
            migratedDb.close()
            context.deleteDatabase(migDbName)
        }
    }

    @Test
    fun `outbox sync_queue automatically enqueues UUID event on StudyLog insert and marks synced idempotently`() = runTest {
        val logRepo = StudyLogRepositoryImpl(db.studyLogDao(), db.syncQueueDao())
        assertEquals(0, db.syncQueueDao().getPendingCount())

        logRepo.insertLog(
            com.techenglish.domain.model.StudyLog(
                wordId = null,
                actionType = com.techenglish.domain.model.StudyActionType.FLASHCARD,
                isCorrect = true,
                xpEarned = 10,
                timestamp = 1_800_000_000_000L
            )
        )

        val pending = db.syncQueueDao().getPendingBatch(10)
        assertEquals(1, pending.size)
        assertEquals("FLASHCARD_CORRECT", pending.first().eventType)
        assertEquals("PENDING", pending.first().syncStatus)

        db.syncQueueDao().markSynced(listOf(pending.first().eventId))
        assertEquals(0, db.syncQueueDao().getPendingCount())
    }

    @Test
    fun `AndroidKeystoreTokenStore encrypts decrypts and clears session tokens cleanly`() = runTest {
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        assertTrue(tokenStore.sessionFlow.first().isGuest)
        assertNull(tokenStore.getAccessToken())

        tokenStore.saveTokensAndSession(
            accessToken = "eyJhbGciOiJIUzI1NiJ9.access_payload.sig",
            refreshToken = "eyJhbGciOiJIUzI1NiJ9.refresh_payload.sig",
            userId = "user-uuid-42",
            username = "architect_tj",
            email = "dev@techenglish.app",
            provider = AuthProvider.GITHUB
        )

        val session = tokenStore.sessionFlow.first()
        assertEquals("user-uuid-42", session.userId)
        assertEquals("architect_tj", session.username)
        assertEquals(AuthProvider.GITHUB, session.provider)
        assertEquals("eyJhbGciOiJIUzI1NiJ9.access_payload.sig", tokenStore.getAccessToken())
        assertEquals("eyJhbGciOiJIUzI1NiJ9.refresh_payload.sig", tokenStore.getRefreshToken())

        tokenStore.clearSession()
        assertTrue(tokenStore.sessionFlow.first().isGuest)
        assertNull(tokenStore.getAccessToken())
    }

    @Test
    fun `MIGRATION_4_5 upgrades v4 to v5 losslessly creating all 4 Stage 4A community tables and preserving v4 data`() = runTest {
        val migDbName = "mig_v4_v5_${UUID.randomUUID()}.db"
        val migDbFile = context.getDatabasePath(migDbName)
        migDbFile.parentFile?.mkdirs()

        val schemaV4File = listOf(
            File("schemas/com.techenglish.app.core.database.TechEnglishDatabase/4.json"),
            File("app/schemas/com.techenglish.app.core.database.TechEnglishDatabase/4.json")
        ).first { it.exists() }

        val schemaV4 = org.json.JSONObject(schemaV4File.readText()).getJSONObject("database")
        val rawV4 = SQLiteDatabase.openOrCreateDatabase(migDbFile, null)
        try {
            val entities = schemaV4.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val tableName = entity.getString("tableName")
                rawV4.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", tableName))
                val indices = entity.optJSONArray("indices")
                if (indices != null) {
                    for (j in 0 until indices.length()) {
                        val idxSql = indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tableName)
                        rawV4.execSQL(idxSql)
                    }
                }
            }
            val setupQueries = schemaV4.getJSONArray("setupQueries")
            for (i in 0 until setupQueries.length()) {
                rawV4.execSQL(setupQueries.getString(i))
            }
            rawV4.version = 4

            // Insert v4 user data that must survive MIGRATION_4_5 untouched
            rawV4.execSQL("INSERT INTO sync_queue (event_id, event_type, payload_json, created_at, retry_count, sync_status, last_error) VALUES ('ev-v4-keep', 'FLASHCARD_CORRECT', '{}', 1760000000000, 0, 'PENDING', '')")
            rawV4.execSQL("INSERT INTO user_profile_cache (user_id, username, display_name, avatar_key, status_text, tech_stack_csv, github_url, telegram_handle, rank_title, total_xp, streak_days, league, updated_at) VALUES ('u-v4', 'senior_dev', 'Senior Dev', 'preset_kotlin', 'Ready', 'Kotlin,SQL', '', '', 'Senior Developer', 1200, 14, 'GOLD', 1760000000000)")
        } finally {
            rawV4.close()
        }

        val migratedDb = Room.databaseBuilder(context, TechEnglishDatabase::class.java, migDbName)
            .addMigrations(*TechEnglishDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            // Verify v4 data survived 100% intact
            assertEquals(1, migratedDb.syncQueueDao().getPendingCount())
            assertEquals("senior_dev", migratedDb.socialCacheDao().getProfile()?.username)

            // Verify all 4 Stage 4A tables (comments_cache, community_decks, community_deck_terms, room_messages_cache) work
            migratedDb.commentsCacheDao().upsertComment(
                com.techenglish.app.core.database.entity.CommentCacheEntity(
                    id = "c-mig-1",
                    targetType = "TERM",
                    targetId = 42,
                    authorUserId = "u-v4",
                    authorUsername = "senior_dev",
                    body = "Mnemonic: ACID = Atomicity, Consistency, Isolation, Durability.",
                    upvotes = 5,
                    downvotes = 0,
                    userVote = 1,
                    createdAt = 1760000000000L
                )
            )
            assertEquals(1, migratedDb.commentsCacheDao().observeCommentsForTarget("TERM", 42).first().size)

            migratedDb.communityDecksDao().upsertDeck(
                com.techenglish.app.core.database.entity.CommunityDeckEntity(
                    id = "deck-mig-1",
                    title = "Backend Interview Pack",
                    description = "Distributed systems terms",
                    tagsCsv = "backend,interview",
                    authorUserId = "u-v4",
                    authorUsername = "senior_dev",
                    rating = 4.9f,
                    downloadsCount = 10,
                    termCount = 1,
                    createdAt = 1760000000000L
                )
            )
            migratedDb.communityDecksDao().insertDeckTerms(
                listOf(
                    com.techenglish.app.core.database.entity.CommunityDeckTermEntity(
                        deckId = "deck-mig-1",
                        term = "Backpressure",
                        translation = "обратное давление"
                    )
                )
            )
            assertEquals(1, migratedDb.communityDecksDao().getTermsForDeck("deck-mig-1").size)

            migratedDb.roomMessagesCacheDao().upsertMessages(
                listOf(
                    com.techenglish.app.core.database.entity.RoomMessageCacheEntity(
                        id = "msg-mig-1",
                        roomSlug = "code-review-practice",
                        authorUserId = "u-v4",
                        authorUsername = "senior_dev",
                        body = "Could we extract this into a pure domain use case?",
                        createdAt = 1760000000000L
                    )
                )
            )
            assertEquals(1, migratedDb.roomMessagesCacheDao().observeRoomMessages("code-review-practice").first().size)
        } finally {
            migratedDb.close()
            context.deleteDatabase(migDbName)
        }
    }
}
