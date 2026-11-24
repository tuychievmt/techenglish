package com.techenglish.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.techenglish.app.core.database.dao.AcademicTermDao
import com.techenglish.app.core.database.dao.CategoryDao
import com.techenglish.app.core.database.dao.CodeSnippetDao
import com.techenglish.app.core.database.dao.CommentsCacheDao
import com.techenglish.app.core.database.dao.CommunityDecksDao
import com.techenglish.app.core.database.dao.CustomImportedDocDao
import com.techenglish.app.core.database.dao.DocArticleDao
import com.techenglish.app.core.database.dao.OtaContentMetaDao
import com.techenglish.app.core.database.dao.ProgrammingLanguageDao
import com.techenglish.app.core.database.dao.RoomMessagesCacheDao
import com.techenglish.app.core.database.dao.ScenarioDao
import com.techenglish.app.core.database.dao.SocialCacheDao
import com.techenglish.app.core.database.dao.SquadCacheDao
import com.techenglish.app.core.database.dao.StudyLogDao
import com.techenglish.app.core.database.dao.SyncQueueDao
import com.techenglish.app.core.database.dao.TimeAttackRecordDao
import com.techenglish.app.core.database.dao.WordDao
import com.techenglish.app.core.database.dao.WorkPhraseDao
import com.techenglish.app.core.database.entity.AcademicTermEntity
import com.techenglish.app.core.database.entity.AcademicTermFtsEntity
import com.techenglish.app.core.database.entity.ActivityFeedCacheEntity
import com.techenglish.app.core.database.entity.buildAcademicNormalizedSearch
import com.techenglish.app.core.database.entity.buildWordNormalizedSearch
import com.techenglish.app.core.database.entity.BadgesCacheEntity
import com.techenglish.app.core.database.entity.CategoryEntity
import com.techenglish.app.core.database.entity.CodeSnippetEntity
import com.techenglish.app.core.database.entity.CommentCacheEntity
import com.techenglish.app.core.database.entity.CommunityDeckEntity
import com.techenglish.app.core.database.entity.CommunityDeckTermEntity
import com.techenglish.app.core.database.entity.CustomImportedDocEntity
import com.techenglish.app.core.database.entity.DocArticleEntity
import com.techenglish.app.core.database.entity.FriendsCacheEntity
import com.techenglish.app.core.database.entity.LeaderboardCacheEntity
import com.techenglish.app.core.database.entity.LocalAccountLinkEntity
import com.techenglish.app.core.database.entity.OtaContentMetaEntity
import com.techenglish.app.core.database.entity.ProgrammingLanguageEntity
import com.techenglish.app.core.database.entity.RoomMessageCacheEntity
import com.techenglish.app.core.database.entity.ScenarioEntity
import com.techenglish.app.core.database.entity.SquadCacheEntity
import com.techenglish.app.core.database.entity.StudyLogEntity
import com.techenglish.app.core.database.entity.SubcategoryEntity
import com.techenglish.app.core.database.entity.SyncQueueEntity
import com.techenglish.app.core.database.entity.TimeAttackRecordEntity
import com.techenglish.app.core.database.entity.UserProfileCacheEntity
import com.techenglish.app.core.database.entity.WordEntity
import com.techenglish.app.core.database.entity.WordRelationEntity
import com.techenglish.app.core.database.entity.WordSenseEntity
import com.techenglish.app.core.database.entity.WordSourceEntity
import com.techenglish.app.core.database.entity.WorkPhraseEntity

@Database(
    entities = [
        CategoryEntity::class,
        ProgrammingLanguageEntity::class,
        CodeSnippetEntity::class,
        WordEntity::class,
        DocArticleEntity::class,
        ScenarioEntity::class,
        WorkPhraseEntity::class,
        StudyLogEntity::class,
        SubcategoryEntity::class,
        WordSourceEntity::class,
        AcademicTermEntity::class,
        AcademicTermFtsEntity::class,
        WordSenseEntity::class,
        WordRelationEntity::class,
        CustomImportedDocEntity::class,
        TimeAttackRecordEntity::class,
        SyncQueueEntity::class,
        UserProfileCacheEntity::class,
        LocalAccountLinkEntity::class,
        LeaderboardCacheEntity::class,
        FriendsCacheEntity::class,
        ActivityFeedCacheEntity::class,
        BadgesCacheEntity::class,
        OtaContentMetaEntity::class,
        CommentCacheEntity::class,
        CommunityDeckEntity::class,
        CommunityDeckTermEntity::class,
        RoomMessageCacheEntity::class,
        SquadCacheEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class TechEnglishDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun programmingLanguageDao(): ProgrammingLanguageDao
    abstract fun codeSnippetDao(): CodeSnippetDao
    abstract fun wordDao(): WordDao
    abstract fun docArticleDao(): DocArticleDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun workPhraseDao(): WorkPhraseDao
    abstract fun studyLogDao(): StudyLogDao
    abstract fun academicTermDao(): AcademicTermDao
    abstract fun customImportedDocDao(): CustomImportedDocDao
    abstract fun timeAttackRecordDao(): TimeAttackRecordDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun socialCacheDao(): SocialCacheDao
    abstract fun otaContentMetaDao(): OtaContentMetaDao
    abstract fun commentsCacheDao(): CommentsCacheDao
    abstract fun communityDecksDao(): CommunityDecksDao
    abstract fun roomMessagesCacheDao(): RoomMessagesCacheDao
    abstract fun squadCacheDao(): SquadCacheDao

    companion object {
        const val DATABASE_NAME = "tech_english.db"
        const val SEED_ASSET_PATH = "databases/tech_english.db"

        /**
         * Lossless forward migration from v1 to v2.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subcategories` (
                        `id` INTEGER NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `slug` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_subcategories_slug` ON `subcategories` (`slug`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_subcategories_category` ON `subcategories` (`category_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_sources` (
                        `id` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `source_type` TEXT NOT NULL,
                        `publisher` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `academic_terms` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `term` TEXT NOT NULL,
                        `normalized_term` TEXT NOT NULL,
                        `phonetic_transcription` TEXT NOT NULL DEFAULT '',
                        `part_of_speech` TEXT NOT NULL,
                        `translation_ru` TEXT NOT NULL,
                        `translation_tg` TEXT NOT NULL DEFAULT '',
                        `definition_en` TEXT NOT NULL,
                        `definition_ru` TEXT NOT NULL,
                        `academic_explanation_en` TEXT NOT NULL,
                        `academic_explanation_ru` TEXT NOT NULL,
                        `practical_explanation` TEXT NOT NULL,
                        `example_en` TEXT NOT NULL,
                        `example_ru` TEXT NOT NULL,
                        `major_category` TEXT NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `subcategory_id` INTEGER NOT NULL,
                        `domain` TEXT NOT NULL,
                        `subdomain` TEXT NOT NULL,
                        `difficulty_level` TEXT NOT NULL,
                        `cefr_level` TEXT NOT NULL,
                        `synonyms` TEXT NOT NULL DEFAULT '',
                        `related_terms` TEXT NOT NULL DEFAULT '',
                        `abbreviations` TEXT NOT NULL DEFAULT '',
                        `source_reference` TEXT NOT NULL DEFAULT '',
                        `source_id` INTEGER,
                        `verification_status` TEXT NOT NULL DEFAULT 'verified',
                        `is_favorite` INTEGER NOT NULL DEFAULT 0,
                        `is_in_srs` INTEGER NOT NULL DEFAULT 0,
                        `repetitions` INTEGER NOT NULL DEFAULT 0,
                        `ease_factor` REAL NOT NULL DEFAULT 2.5,
                        `interval_days` INTEGER NOT NULL DEFAULT 0,
                        `next_review_at` INTEGER NOT NULL DEFAULT 0,
                        `last_studied_at` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`source_id`) REFERENCES `word_sources`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_academic_normalized_term` ON `academic_terms` (`normalized_term`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_category_id` ON `academic_terms` (`category_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_subcategory_id` ON `academic_terms` (`subcategory_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_difficulty` ON `academic_terms` (`difficulty_level`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_cefr` ON `academic_terms` (`cefr_level`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_next_review` ON `academic_terms` (`next_review_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_source_id` ON `academic_terms` (`source_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_domain` ON `academic_terms` (`domain`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_status` ON `academic_terms` (`verification_status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_academic_srs_due` ON `academic_terms` (`is_in_srs`, `next_review_at`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_senses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `term_id` INTEGER NOT NULL,
                        `sense_number` INTEGER NOT NULL,
                        `sense_label` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `definition_en` TEXT NOT NULL,
                        `definition_ru` TEXT NOT NULL,
                        `example_en` TEXT NOT NULL,
                        `example_ru` TEXT NOT NULL,
                        FOREIGN KEY(`term_id`) REFERENCES `academic_terms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_senses_term_id` ON `word_senses` (`term_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_relations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `term_id` INTEGER NOT NULL,
                        `related_term` TEXT NOT NULL,
                        `relation_type` TEXT NOT NULL,
                        FOREIGN KEY(`term_id`) REFERENCES `academic_terms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_relations_term_id` ON `word_relations` (`term_id`)")
            }
        }

        /**
         * Lossless forward migration from v2 to v3 (Stage 2).
         * Preserves 100% of v1 and v2 user data (`words`, `academic_terms`, `study_logs`, `doc_articles`, etc.)
         * while adding `custom_imported_docs`, `time_attack_records`, and source/license attribution columns.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_imported_docs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `source_file_name` TEXT NOT NULL DEFAULT '',
                        `content` TEXT NOT NULL,
                        `detected_terms_json` TEXT NOT NULL DEFAULT '[]',
                        `detected_terms_count` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_custom_docs_created` ON `custom_imported_docs` (`created_at`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `time_attack_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `score` INTEGER NOT NULL,
                        `correct_count` INTEGER NOT NULL,
                        `total_answered` INTEGER NOT NULL,
                        `xp_earned` INTEGER NOT NULL,
                        `duration_seconds` INTEGER NOT NULL DEFAULT 60,
                        `achieved_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_time_attack_score` ON `time_attack_records` (`score`)")

                db.execSQL("ALTER TABLE `doc_articles` ADD COLUMN `source_url` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `doc_articles` ADD COLUMN `license` TEXT NOT NULL DEFAULT 'CC-BY-4.0'")
                db.execSQL("ALTER TABLE `word_sources` ADD COLUMN `license` TEXT NOT NULL DEFAULT 'Public Specification / Standard'")
                db.execSQL("ALTER TABLE `academic_terms` ADD COLUMN `ipa_source_url` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `academic_terms` ADD COLUMN `ipa_license` TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Lossless forward migration from v3 to v4 (Stage 3: Hybrid Sync Outbox & Online Caches).
         * Adds `sync_queue`, `user_profile_cache`, `local_account_link`, `leaderboard_cache`,
         * `friends_cache`, `activity_feed_cache`, `badges_cache`, and `ota_content_meta`.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `event_id` TEXT NOT NULL,
                        `event_type` TEXT NOT NULL,
                        `payload_json` TEXT NOT NULL DEFAULT '{}',
                        `created_at` INTEGER NOT NULL,
                        `retry_count` INTEGER NOT NULL DEFAULT 0,
                        `sync_status` TEXT NOT NULL DEFAULT 'PENDING',
                        `last_error` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`event_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_sync_queue_status_created` ON `sync_queue` (`sync_status`, `created_at`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_profile_cache` (
                        `user_id` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `avatar_key` TEXT NOT NULL DEFAULT 'preset_terminal',
                        `status_text` TEXT NOT NULL DEFAULT '',
                        `tech_stack_csv` TEXT NOT NULL DEFAULT 'Kotlin,Python,SQL',
                        `github_url` TEXT NOT NULL DEFAULT '',
                        `telegram_handle` TEXT NOT NULL DEFAULT '',
                        `rank_title` TEXT NOT NULL DEFAULT 'Intern Developer',
                        `total_xp` INTEGER NOT NULL DEFAULT 0,
                        `streak_days` INTEGER NOT NULL DEFAULT 0,
                        `league` TEXT NOT NULL DEFAULT 'BRONZE',
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`user_id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_account_link` (
                        `local_user_id` TEXT NOT NULL,
                        `remote_user_id` TEXT NOT NULL,
                        `provider` TEXT NOT NULL DEFAULT 'EMAIL',
                        `linked_at` INTEGER NOT NULL,
                        PRIMARY KEY(`local_user_id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `leaderboard_cache` (
                        `tab` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `rank` INTEGER NOT NULL,
                        `username` TEXT NOT NULL,
                        `avatar_key` TEXT NOT NULL DEFAULT 'preset_terminal',
                        `rank_title` TEXT NOT NULL,
                        `league` TEXT NOT NULL,
                        `tech_stack` TEXT NOT NULL DEFAULT '',
                        `weekly_xp` INTEGER NOT NULL DEFAULT 0,
                        `total_xp` INTEGER NOT NULL DEFAULT 0,
                        `is_current_user` INTEGER NOT NULL DEFAULT 0,
                        `cached_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`tab`, `user_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_leaderboard_tab_rank` ON `leaderboard_cache` (`tab`, `rank`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `friends_cache` (
                        `user_id` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `avatar_key` TEXT NOT NULL DEFAULT 'preset_terminal',
                        `rank_title` TEXT NOT NULL,
                        `streak_days` INTEGER NOT NULL DEFAULT 0,
                        `accepts_pings` INTEGER NOT NULL DEFAULT 1,
                        `last_pinged_at` INTEGER NOT NULL DEFAULT 0,
                        `cached_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`user_id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `activity_feed_cache` (
                        `id` TEXT NOT NULL,
                        `actor_user_id` TEXT NOT NULL,
                        `actor_username` TEXT NOT NULL,
                        `summary_ru` TEXT NOT NULL,
                        `reaction_count` INTEGER NOT NULL DEFAULT 0,
                        `user_reacted` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_activity_feed_created` ON `activity_feed_cache` (`created_at`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `badges_cache` (
                        `id` TEXT NOT NULL,
                        `title_ru` TEXT NOT NULL,
                        `description_ru` TEXT NOT NULL DEFAULT '',
                        `icon_key` TEXT NOT NULL DEFAULT 'star',
                        `is_unlocked` INTEGER NOT NULL DEFAULT 0,
                        `unlocked_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ota_content_meta` (
                        `pack_id` TEXT NOT NULL,
                        `version` INTEGER NOT NULL,
                        `title_ru` TEXT NOT NULL,
                        `sha256_hex` TEXT NOT NULL,
                        `item_count` INTEGER NOT NULL DEFAULT 0,
                        `applied_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`pack_id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Lossless forward migration from v4 to v5 (Stage 4A community tables per §6.2):
         * adds comments_cache, community_decks, community_deck_terms, and room_messages_cache
         * while preserving 100% of v1–v4 user progress, SRS states, sync_queue, and caches.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `comments_cache` (
                        `id` TEXT NOT NULL,
                        `target_type` TEXT NOT NULL,
                        `target_id` INTEGER NOT NULL,
                        `author_user_id` TEXT NOT NULL,
                        `author_username` TEXT NOT NULL,
                        `author_rank_title` TEXT NOT NULL DEFAULT 'Intern Developer',
                        `body` TEXT NOT NULL,
                        `upvotes` INTEGER NOT NULL DEFAULT 0,
                        `downvotes` INTEGER NOT NULL DEFAULT 0,
                        `user_vote` INTEGER NOT NULL DEFAULT 0,
                        `is_hidden` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_comments_target` ON `comments_cache` (`target_type`, `target_id`, `created_at`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `community_decks` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `tags_csv` TEXT NOT NULL DEFAULT '',
                        `author_user_id` TEXT NOT NULL,
                        `author_username` TEXT NOT NULL,
                        `rating` REAL NOT NULL DEFAULT 0.0,
                        `downloads_count` INTEGER NOT NULL DEFAULT 0,
                        `term_count` INTEGER NOT NULL DEFAULT 0,
                        `is_downloaded_locally` INTEGER NOT NULL DEFAULT 0,
                        `is_hidden` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_community_decks_rating` ON `community_decks` (`rating`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `community_deck_terms` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `deck_id` TEXT NOT NULL,
                        `term` TEXT NOT NULL,
                        `transcription` TEXT NOT NULL DEFAULT '',
                        `translation` TEXT NOT NULL,
                        `example` TEXT NOT NULL DEFAULT '',
                        `explanation_ru` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`deck_id`) REFERENCES `community_decks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_deck_terms_deck_id` ON `community_deck_terms` (`deck_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `room_messages_cache` (
                        `id` TEXT NOT NULL,
                        `room_slug` TEXT NOT NULL,
                        `author_user_id` TEXT NOT NULL,
                        `author_username` TEXT NOT NULL,
                        `author_rank_title` TEXT NOT NULL DEFAULT 'Intern Developer',
                        `body` TEXT NOT NULL,
                        `is_hidden` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_room_messages_room_created` ON `room_messages_cache` (`room_slug`, `created_at`)")
            }
        }

        /**
         * Lossless forward migration from v5 to v6 (SP2-04 / BUG-15 / R6):
         * Adds Unicode-lowercased `normalized_search` to `academic_terms` and `words`,
         * backfills existing rows (preserving Cyrillic/Tajik case folding on all OEM SQLite builds),
         * creates `@Fts4` external-content table `academic_terms_fts` with Room content-sync triggers,
         * and rebuilds the inverted index.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `academic_terms` ADD COLUMN `normalized_search` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `words` ADD COLUMN `normalized_search` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `ota_content_meta` ADD COLUMN `last_status` TEXT NOT NULL DEFAULT 'APPLIED'")
                db.execSQL("ALTER TABLE `ota_content_meta` ADD COLUMN `last_error` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `ota_content_meta` ADD COLUMN `last_attempt_at` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `squad_cache` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `invite_code` TEXT NOT NULL,
                        `members_json` TEXT NOT NULL DEFAULT '[]',
                        `weekly_goal_xp` INTEGER NOT NULL DEFAULT 1000,
                        `current_week_xp` INTEGER NOT NULL DEFAULT 0,
                        `next_week_bonus_multiplier` REAL NOT NULL DEFAULT 1.0,
                        `is_active_for_challenge` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                // Backfill academic_terms.normalized_search with Unicode lowercase (Cyrillic + Latin + Tajik)
                val academicUpdates = mutableListOf<Pair<Int, String>>()
                db.query(
                    """
                    SELECT id, term, abbreviations, translation_ru, translation_tg,
                           synonyms, definition_en, definition_ru
                    FROM academic_terms
                    """.trimIndent()
                ).use { c ->
                    while (c.moveToNext()) {
                        val id = c.getInt(0)
                        val normalized = buildAcademicNormalizedSearch(
                            term = c.getString(1).orEmpty(),
                            abbreviations = c.getString(2).orEmpty(),
                            translationRu = c.getString(3).orEmpty(),
                            translationTg = c.getString(4).orEmpty(),
                            synonyms = c.getString(5).orEmpty(),
                            definitionEn = c.getString(6).orEmpty(),
                            definitionRu = c.getString(7).orEmpty()
                        )
                        academicUpdates += id to normalized
                    }
                }
                val acadStmt = db.compileStatement("UPDATE academic_terms SET normalized_search = ? WHERE id = ?")
                try {
                    academicUpdates.forEach { (id, normalized) ->
                        acadStmt.clearBindings()
                        acadStmt.bindString(1, normalized)
                        acadStmt.bindLong(2, id.toLong())
                        acadStmt.executeUpdateDelete()
                    }
                } finally {
                    acadStmt.close()
                }

                // Backfill words.normalized_search with Unicode lowercase
                val wordUpdates = mutableListOf<Pair<Int, String>>()
                db.query("SELECT id, term, translation, explanation_ru FROM words").use { c ->
                    while (c.moveToNext()) {
                        val id = c.getInt(0)
                        val normalized = buildWordNormalizedSearch(
                            term = c.getString(1).orEmpty(),
                            translation = c.getString(2).orEmpty(),
                            explanationRu = c.getString(3).orEmpty()
                        )
                        wordUpdates += id to normalized
                    }
                }
                val wordStmt = db.compileStatement("UPDATE words SET normalized_search = ? WHERE id = ?")
                try {
                    wordUpdates.forEach { (id, normalized) ->
                        wordStmt.clearBindings()
                        wordStmt.bindString(1, normalized)
                        wordStmt.bindLong(2, id.toLong())
                        wordStmt.executeUpdateDelete()
                    }
                } finally {
                    wordStmt.close()
                }

                // Create Room @Fts4 external-content virtual table & sync triggers
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `academic_terms_fts` USING FTS4(`normalized_search` TEXT NOT NULL, tokenize=unicode61, content=`academic_terms`)"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_academic_terms_fts_BEFORE_UPDATE BEFORE UPDATE ON `academic_terms` BEGIN DELETE FROM `academic_terms_fts` WHERE `docid`=OLD.`rowid`; END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_academic_terms_fts_BEFORE_DELETE BEFORE DELETE ON `academic_terms` BEGIN DELETE FROM `academic_terms_fts` WHERE `docid`=OLD.`rowid`; END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_academic_terms_fts_AFTER_UPDATE AFTER UPDATE ON `academic_terms` BEGIN INSERT INTO `academic_terms_fts`(`docid`, `normalized_search`) VALUES (NEW.`rowid`, NEW.`normalized_search`); END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_academic_terms_fts_AFTER_INSERT AFTER INSERT ON `academic_terms` BEGIN INSERT INTO `academic_terms_fts`(`docid`, `normalized_search`) VALUES (NEW.`rowid`, NEW.`normalized_search`); END"
                )
                db.execSQL("INSERT INTO `academic_terms_fts`(`academic_terms_fts`) VALUES('rebuild')")
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )
    }
}
