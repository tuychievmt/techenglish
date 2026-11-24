package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room v4 (MIGRATION_3_4) entities per techenglish_v3_2_final.md §6.2:
 * - sync_queue (outbox pattern with UUID event_id)
 * - user_profile_cache
 * - local_account_link
 * - leaderboard_cache
 * - friends_cache
 * - activity_feed_cache
 * - badges_cache
 * - ota_content_meta
 */

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(name = "idx_sync_queue_status_created", value = ["sync_status", "created_at"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,
    @ColumnInfo(name = "event_type")
    val eventType: String,
    @ColumnInfo(name = "payload_json", defaultValue = "'{}'")
    val payloadJson: String = "{}",
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "retry_count", defaultValue = "0")
    val retryCount: Int = 0,
    @ColumnInfo(name = "sync_status", defaultValue = "'PENDING'")
    val syncStatus: String = "PENDING",
    @ColumnInfo(name = "last_error", defaultValue = "''")
    val lastError: String = ""
)

@Entity(tableName = "user_profile_cache")
data class UserProfileCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "avatar_key", defaultValue = "'preset_terminal'")
    val avatarKey: String = "preset_terminal",
    @ColumnInfo(name = "status_text", defaultValue = "''")
    val statusText: String = "",
    @ColumnInfo(name = "tech_stack_csv", defaultValue = "'Kotlin,Python,SQL'")
    val techStackCsv: String = "Kotlin,Python,SQL",
    @ColumnInfo(name = "github_url", defaultValue = "''")
    val githubUrl: String = "",
    @ColumnInfo(name = "telegram_handle", defaultValue = "''")
    val telegramHandle: String = "",
    @ColumnInfo(name = "rank_title", defaultValue = "'Intern Developer'")
    val rankTitle: String = "Intern Developer",
    @ColumnInfo(name = "total_xp", defaultValue = "0")
    val totalXp: Int = 0,
    @ColumnInfo(name = "streak_days", defaultValue = "0")
    val streakDays: Int = 0,
    @ColumnInfo(name = "league", defaultValue = "'BRONZE'")
    val league: String = "BRONZE",
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0L
)

@Entity(tableName = "local_account_link")
data class LocalAccountLinkEntity(
    @PrimaryKey
    @ColumnInfo(name = "local_user_id")
    val localUserId: String,
    @ColumnInfo(name = "remote_user_id")
    val remoteUserId: String,
    @ColumnInfo(name = "provider", defaultValue = "'EMAIL'")
    val provider: String = "EMAIL",
    @ColumnInfo(name = "linked_at")
    val linkedAt: Long
)

@Entity(
    tableName = "leaderboard_cache",
    primaryKeys = ["tab", "user_id"],
    indices = [
        Index(name = "idx_leaderboard_tab_rank", value = ["tab", "rank"])
    ]
)
data class LeaderboardCacheEntity(
    @ColumnInfo(name = "tab")
    val tab: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "rank")
    val rank: Int,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "avatar_key", defaultValue = "'preset_terminal'")
    val avatarKey: String = "preset_terminal",
    @ColumnInfo(name = "rank_title")
    val rankTitle: String,
    @ColumnInfo(name = "league")
    val league: String,
    @ColumnInfo(name = "tech_stack", defaultValue = "''")
    val techStack: String = "",
    @ColumnInfo(name = "weekly_xp", defaultValue = "0")
    val weeklyXp: Int = 0,
    @ColumnInfo(name = "total_xp", defaultValue = "0")
    val totalXp: Int = 0,
    @ColumnInfo(name = "is_current_user", defaultValue = "0")
    val isCurrentUser: Boolean = false,
    @ColumnInfo(name = "cached_at", defaultValue = "0")
    val cachedAt: Long = 0L
)

@Entity(tableName = "friends_cache")
data class FriendsCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "avatar_key", defaultValue = "'preset_terminal'")
    val avatarKey: String = "preset_terminal",
    @ColumnInfo(name = "rank_title")
    val rankTitle: String,
    @ColumnInfo(name = "streak_days", defaultValue = "0")
    val streakDays: Int = 0,
    @ColumnInfo(name = "accepts_pings", defaultValue = "1")
    val acceptsPings: Boolean = true,
    @ColumnInfo(name = "last_pinged_at", defaultValue = "0")
    val lastPingedAt: Long = 0L,
    @ColumnInfo(name = "cached_at", defaultValue = "0")
    val cachedAt: Long = 0L
)

@Entity(
    tableName = "activity_feed_cache",
    indices = [
        Index(name = "idx_activity_feed_created", value = ["created_at"])
    ]
)
data class ActivityFeedCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "actor_user_id")
    val actorUserId: String,
    @ColumnInfo(name = "actor_username")
    val actorUsername: String,
    @ColumnInfo(name = "summary_ru")
    val summaryRu: String,
    @ColumnInfo(name = "reaction_count", defaultValue = "0")
    val reactionCount: Int = 0,
    @ColumnInfo(name = "user_reacted", defaultValue = "0")
    val userReacted: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(tableName = "badges_cache")
data class BadgesCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title_ru")
    val titleRu: String,
    @ColumnInfo(name = "description_ru", defaultValue = "''")
    val descriptionRu: String = "",
    @ColumnInfo(name = "icon_key", defaultValue = "'star'")
    val iconKey: String = "star",
    @ColumnInfo(name = "is_unlocked", defaultValue = "0")
    val isUnlocked: Boolean = false,
    @ColumnInfo(name = "unlocked_at", defaultValue = "0")
    val unlockedAt: Long = 0L
)

@Entity(tableName = "ota_content_meta")
data class OtaContentMetaEntity(
    @PrimaryKey
    @ColumnInfo(name = "pack_id")
    val packId: String,
    @ColumnInfo(name = "version")
    val version: Int,
    @ColumnInfo(name = "title_ru")
    val titleRu: String,
    @ColumnInfo(name = "sha256_hex")
    val sha256Hex: String,
    @ColumnInfo(name = "item_count", defaultValue = "0")
    val itemCount: Int = 0,
    @ColumnInfo(name = "applied_at", defaultValue = "0")
    val appliedAt: Long = 0L,
    @ColumnInfo(name = "last_status", defaultValue = "'APPLIED'")
    val lastStatus: String = "APPLIED",
    @ColumnInfo(name = "last_error", defaultValue = "''")
    val lastError: String = "",
    @ColumnInfo(name = "last_attempt_at", defaultValue = "0")
    val lastAttemptAt: Long = 0L
)
