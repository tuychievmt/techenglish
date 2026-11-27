package com.techenglish.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.techenglish.app.core.database.entity.ActivityFeedCacheEntity
import com.techenglish.app.core.database.entity.BadgesCacheEntity
import com.techenglish.app.core.database.entity.FriendsCacheEntity
import com.techenglish.app.core.database.entity.LeaderboardCacheEntity
import com.techenglish.app.core.database.entity.LocalAccountLinkEntity
import com.techenglish.app.core.database.entity.OtaContentMetaEntity
import com.techenglish.app.core.database.entity.SyncQueueEntity
import com.techenglish.app.core.database.entity.UserProfileCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(event: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue WHERE sync_status = 'PENDING' ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingBatch(limit: Int = 50): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE sync_status = 'PENDING' AND event_type != 'ROOM_MESSAGE_OUTBOX' ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingSyncEventsBatch(limit: Int = 50): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE sync_status = 'PENDING' AND event_type = 'ROOM_MESSAGE_OUTBOX' ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingRoomOutboxBatch(limit: Int = 50): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE sync_status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE sync_status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("UPDATE sync_queue SET sync_status = 'SYNCED', last_error = '' WHERE event_id IN (:eventIds)")
    suspend fun markSynced(eventIds: List<String>)

    @Query("UPDATE sync_queue SET retry_count = retry_count + 1, last_error = :error WHERE event_id = :eventId")
    suspend fun recordRetryFailure(eventId: String, error: String)

    @Query("UPDATE sync_queue SET sync_status = 'FAILED', last_error = :error WHERE event_id = :eventId")
    suspend fun markFailed(eventId: String, error: String)

    @Query("DELETE FROM sync_queue WHERE sync_status = 'SYNCED'")
    suspend fun pruneSynced()
}

@Dao
interface SocialCacheDao {
    // User Profile Cache
    @Query("SELECT * FROM user_profile_cache LIMIT 1")
    fun observeProfile(): Flow<UserProfileCacheEntity?>

    @Query("SELECT * FROM user_profile_cache LIMIT 1")
    suspend fun getProfile(): UserProfileCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileCacheEntity)

    @Query("DELETE FROM user_profile_cache")
    suspend fun clearProfile()

    // Local Account Link (Guest -> Authenticated)
    @Query("SELECT * FROM local_account_link LIMIT 1")
    fun observeAccountLink(): Flow<LocalAccountLinkEntity?>

    @Query("SELECT * FROM local_account_link LIMIT 1")
    suspend fun getAccountLink(): LocalAccountLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccountLink(link: LocalAccountLinkEntity)

    @Query("DELETE FROM local_account_link")
    suspend fun clearAccountLink()

    // Leaderboard Cache (4 tabs)
    @Query("SELECT * FROM leaderboard_cache WHERE tab = :tab ORDER BY rank ASC")
    fun observeLeaderboardForTab(tab: String): Flow<List<LeaderboardCacheEntity>>

    @Query("DELETE FROM leaderboard_cache WHERE tab = :tab")
    suspend fun clearLeaderboardTab(tab: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntries(entries: List<LeaderboardCacheEntity>)

    // Friends Cache
    @Query("SELECT * FROM friends_cache ORDER BY streak_days DESC, username ASC")
    fun observeFriends(): Flow<List<FriendsCacheEntity>>

    @Query("DELETE FROM friends_cache")
    suspend fun clearFriends()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendsCacheEntity>)

    @Query("UPDATE friends_cache SET last_pinged_at = :timestamp WHERE user_id = :friendId")
    suspend fun updateFriendLastPinged(friendId: String, timestamp: Long)

    // Activity Feed Cache
    @Query("SELECT * FROM activity_feed_cache ORDER BY created_at DESC LIMIT 30")
    fun observeActivityFeed(): Flow<List<ActivityFeedCacheEntity>>

    @Query("DELETE FROM activity_feed_cache")
    suspend fun clearActivityFeed()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityFeed(items: List<ActivityFeedCacheEntity>)

    @Query("UPDATE activity_feed_cache SET reaction_count = reaction_count + 1, user_reacted = 1 WHERE id = :id")
    suspend fun incrementFeedReaction(id: String)

    // Badges Cache
    @Query("SELECT * FROM badges_cache ORDER BY is_unlocked DESC, id ASC")
    fun observeBadges(): Flow<List<BadgesCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<BadgesCacheEntity>)
}

@Dao
interface OtaContentMetaDao {
    @Query("SELECT * FROM ota_content_meta ORDER BY applied_at DESC")
    fun observeAppliedPacks(): Flow<List<OtaContentMetaEntity>>

    @Query("SELECT * FROM ota_content_meta WHERE pack_id = :packId LIMIT 1")
    suspend fun getPackMeta(packId: String): OtaContentMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPackMeta(meta: OtaContentMetaEntity)
}
