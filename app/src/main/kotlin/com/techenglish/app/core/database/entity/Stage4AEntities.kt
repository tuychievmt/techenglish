package com.techenglish.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room v5 (MIGRATION_4_5) entities per techenglish_v3_2_final.md §6.2 & Master Prompt §6:
 * - comments_cache (target_type = TERM | SNIPPET, target_id, author, upvotes/downvotes)
 * - community_decks
 * - community_deck_terms (FK -> community_decks ON DELETE CASCADE)
 * - room_messages_cache
 */

@Entity(
    tableName = "comments_cache",
    indices = [
        Index(name = "idx_comments_target", value = ["target_type", "target_id", "created_at"])
    ]
)
data class CommentCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "target_type")
    val targetType: String, // TERM | SNIPPET
    @ColumnInfo(name = "target_id")
    val targetId: Int,
    @ColumnInfo(name = "author_user_id")
    val authorUserId: String,
    @ColumnInfo(name = "author_username")
    val authorUsername: String,
    @ColumnInfo(name = "author_rank_title", defaultValue = "'Intern Developer'")
    val authorRankTitle: String = "Intern Developer",
    @ColumnInfo(name = "body")
    val body: String,
    @ColumnInfo(name = "upvotes", defaultValue = "0")
    val upvotes: Int = 0,
    @ColumnInfo(name = "downvotes", defaultValue = "0")
    val downvotes: Int = 0,
    @ColumnInfo(name = "user_vote", defaultValue = "0")
    val userVote: Int = 0,
    @ColumnInfo(name = "is_hidden", defaultValue = "0")
    val isHidden: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(
    tableName = "community_decks",
    indices = [
        Index(name = "idx_community_decks_rating", value = ["rating"])
    ]
)
data class CommunityDeckEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description", defaultValue = "''")
    val description: String = "",
    @ColumnInfo(name = "tags_csv", defaultValue = "''")
    val tagsCsv: String = "",
    @ColumnInfo(name = "author_user_id")
    val authorUserId: String,
    @ColumnInfo(name = "author_username")
    val authorUsername: String,
    @ColumnInfo(name = "rating", defaultValue = "0.0")
    val rating: Float = 0.0f,
    @ColumnInfo(name = "downloads_count", defaultValue = "0")
    val downloadsCount: Int = 0,
    @ColumnInfo(name = "term_count", defaultValue = "0")
    val termCount: Int = 0,
    @ColumnInfo(name = "is_downloaded_locally", defaultValue = "0")
    val isDownloadedLocally: Boolean = false,
    @ColumnInfo(name = "is_hidden", defaultValue = "0")
    val isHidden: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(
    tableName = "community_deck_terms",
    foreignKeys = [
        ForeignKey(
            entity = CommunityDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(name = "idx_deck_terms_deck_id", value = ["deck_id"])
    ]
)
data class CommunityDeckTermEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "deck_id")
    val deckId: String,
    @ColumnInfo(name = "term")
    val term: String,
    @ColumnInfo(name = "transcription", defaultValue = "''")
    val transcription: String = "",
    @ColumnInfo(name = "translation")
    val translation: String,
    @ColumnInfo(name = "example", defaultValue = "''")
    val example: String = "",
    @ColumnInfo(name = "explanation_ru", defaultValue = "''")
    val explanationRu: String = ""
)

@Entity(
    tableName = "room_messages_cache",
    indices = [
        Index(name = "idx_room_messages_room_created", value = ["room_slug", "created_at"])
    ]
)
data class RoomMessageCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "room_slug")
    val roomSlug: String,
    @ColumnInfo(name = "author_user_id")
    val authorUserId: String,
    @ColumnInfo(name = "author_username")
    val authorUsername: String,
    @ColumnInfo(name = "author_rank_title", defaultValue = "'Intern Developer'")
    val authorRankTitle: String = "Intern Developer",
    @ColumnInfo(name = "body")
    val body: String,
    @ColumnInfo(name = "is_hidden", defaultValue = "0")
    val isHidden: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(tableName = "squad_cache")
data class SquadCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "invite_code")
    val inviteCode: String,
    @ColumnInfo(name = "members_json", defaultValue = "'[]'")
    val membersJson: String = "[]",
    @ColumnInfo(name = "weekly_goal_xp", defaultValue = "1000")
    val weeklyGoalXp: Int = 1000,
    @ColumnInfo(name = "current_week_xp", defaultValue = "0")
    val currentWeekXp: Int = 0,
    @ColumnInfo(name = "next_week_bonus_multiplier", defaultValue = "1.0")
    val nextWeekBonusMultiplier: Float = 1.0f,
    @ColumnInfo(name = "is_active_for_challenge", defaultValue = "0")
    val isActiveForChallenge: Boolean = false,
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0L
)
