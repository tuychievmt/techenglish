package com.techenglish.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.techenglish.app.core.database.entity.CommentCacheEntity
import com.techenglish.app.core.database.entity.CommunityDeckEntity
import com.techenglish.app.core.database.entity.CommunityDeckTermEntity
import com.techenglish.app.core.database.entity.RoomMessageCacheEntity
import com.techenglish.app.core.database.entity.SquadCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentsCacheDao {
    @Query(
        """
        SELECT * FROM comments_cache
        WHERE target_type = :targetType AND target_id = :targetId AND is_hidden = 0
        ORDER BY (upvotes - downvotes) DESC, created_at DESC
        """
    )
    fun observeCommentsForTarget(targetType: String, targetId: Int): Flow<List<CommentCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComments(comments: List<CommentCacheEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComment(comment: CommentCacheEntity)

    @Query("UPDATE comments_cache SET is_hidden = 1 WHERE id = :commentId")
    suspend fun hideCommentLocally(commentId: String)

    @Query("UPDATE comments_cache SET is_hidden = 1 WHERE author_user_id = :authorUserId")
    suspend fun hideCommentsByAuthor(authorUserId: String)

    @Query("DELETE FROM comments_cache WHERE target_type = :targetType AND target_id = :targetId")
    suspend fun clearForTarget(targetType: String, targetId: Int)
}

@Dao
interface CommunityDecksDao {
    @Query("SELECT * FROM community_decks WHERE is_hidden = 0 ORDER BY rating DESC, downloads_count DESC, created_at DESC")
    fun observeVisibleDecks(): Flow<List<CommunityDeckEntity>>

    @Query("SELECT COUNT(*) FROM community_decks")
    suspend fun countDecks(): Int

    @Query("SELECT * FROM community_decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckById(deckId: String): CommunityDeckEntity?

    @Query("SELECT * FROM community_deck_terms WHERE deck_id = :deckId ORDER BY id ASC")
    suspend fun getTermsForDeck(deckId: String): List<CommunityDeckTermEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDecks(decks: List<CommunityDeckEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeck(deck: CommunityDeckEntity)

    @Query("DELETE FROM community_deck_terms WHERE deck_id = :deckId")
    suspend fun clearTermsForDeck(deckId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeckTerms(terms: List<CommunityDeckTermEntity>)

    @Query("UPDATE community_decks SET is_downloaded_locally = 1, downloads_count = downloads_count + 1 WHERE id = :deckId")
    suspend fun markDownloadedLocally(deckId: String)

    @Query("UPDATE community_decks SET rating = :rating WHERE id = :deckId")
    suspend fun updateDeckRating(deckId: String, rating: Float)

    @Query("UPDATE community_decks SET is_hidden = 1 WHERE id = :deckId")
    suspend fun hideDeckLocally(deckId: String)

    @Query("UPDATE community_decks SET is_hidden = 1 WHERE author_user_id = :authorUserId")
    suspend fun hideDecksByAuthor(authorUserId: String)
}

@Dao
interface RoomMessagesCacheDao {
    @Query(
        """
        SELECT * FROM room_messages_cache
        WHERE room_slug = :roomSlug AND is_hidden = 0
        ORDER BY created_at ASC
        LIMIT 100
        """
    )
    fun observeRoomMessages(roomSlug: String): Flow<List<RoomMessageCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<RoomMessageCacheEntity>)

    @Query("UPDATE room_messages_cache SET is_hidden = 1 WHERE id = :messageId")
    suspend fun hideMessageLocally(messageId: String)

    @Query("UPDATE room_messages_cache SET is_hidden = 1 WHERE author_user_id = :authorUserId")
    suspend fun hideMessagesByAuthor(authorUserId: String)

    @Query("DELETE FROM room_messages_cache WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)
}

@Dao
interface SquadCacheDao {
    @Query("SELECT * FROM squad_cache LIMIT 1")
    fun observeSquad(): Flow<SquadCacheEntity?>

    @Query("SELECT * FROM squad_cache LIMIT 1")
    suspend fun getSquad(): SquadCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSquad(squad: SquadCacheEntity)

    @Query("DELETE FROM squad_cache")
    suspend fun clearSquad()
}
