package com.techenglish.app.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.database.ContentPackImporter
import com.techenglish.app.core.database.TechEnglishDatabase
import com.techenglish.app.core.database.entity.SyncQueueEntity
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.core.sync.CloudSyncEngine
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.usecase.ResolveSm2SyncConflictUseCase
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Gap04RoomOutboxIsolationTest {

    private lateinit var context: Context
    private lateinit var db: TechEnglishDatabase
    private lateinit var tokenStore: AndroidKeystoreTokenStore

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(cm).setNetworkCapabilities(cm.activeNetwork, caps)
        db = Room.inMemoryDatabaseBuilder(context, TechEnglishDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()
        tokenStore.setApiBaseUrl("https://api.techenglish.app")
        tokenStore.saveTokensAndSession(
            accessToken = "valid_access_token",
            refreshToken = "valid_refresh_token",
            userId = "u-gap04",
            username = "gap04_dev",
            email = "gap04@example.com",
            provider = AuthProvider.EMAIL
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `runFullSyncNow excludes ROOM_MESSAGE_OUTBOX from sync_events and keeps it pending`() = runBlocking {
        val syncDao = db.syncQueueDao()
        syncDao.enqueue(
            SyncQueueEntity(
                eventId = "evt-flashcard-1",
                eventType = "FLASHCARD_CORRECT",
                payloadJson = """{"word":"mutex"}""",
                createdAt = 1_700_000_000_000L,
                retryCount = 0,
                syncStatus = "PENDING",
                lastError = ""
            )
        )
        syncDao.enqueue(
            SyncQueueEntity(
                eventId = "evt-room-outbox-1",
                eventType = "ROOM_MESSAGE_OUTBOX",
                payloadJson = """{"room_id":"r-1","text":"hello room"}""",
                createdAt = 1_700_000_001_000L,
                retryCount = 0,
                syncStatus = "PENDING",
                lastError = ""
            )
        )

        var capturedSyncEventsBody = ""
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when (path) {
                "/v1/sync/events" -> {
                    capturedSyncEventsBody = request.body.toByteArray().decodeToString()
                    val accepted = listOf("evt-flashcard-1", "evt-room-outbox-1")
                        .filter { capturedSyncEventsBody.contains(it) }
                        .joinToString(",") { "\"$it\"" }
                    respond(
                        content = """
                            {
                              "accepted_event_ids": [$accepted],
                              "duplicate_event_ids": [],
                              "awarded_xp": 5,
                              "server_total_xp": 105,
                              "server_rank_title": "Intern",
                              "server_league": "BRONZE",
                              "daily_xp_capped": false,
                              "merged_sm2_count": 0
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                "/v1/sync/state" -> respond(
                    content = """
                        {
                          "user_id": "u-gap04",
                          "server_total_xp": 105,
                          "rank_title": "Intern",
                          "league": "BRONZE",
                          "sm2_states": [],
                          "favorite_terms": [],
                          "custom_words": {}
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/v1/ota/manifest" -> respond(
                    content = """{"packs":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(
                    content = """{"entries":[],"items":[],"friends":[],"badges":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val apiClient = TechEnglishApiClient(
            context = context,
            tokenStore = tokenStore,
            engineOverride = mockEngine
        )
        val importer = ContentPackImporter(database = db)
        val engine = CloudSyncEngine(
            context = context,
            syncQueueDao = syncDao,
            socialCacheDao = db.socialCacheDao(),
            otaContentMetaDao = db.otaContentMetaDao(),
            wordDao = db.wordDao(),
            contentPackImporter = importer,
            apiClient = apiClient,
            tokenStore = tokenStore,
            resolveSm2Conflict = ResolveSm2SyncConflictUseCase()
        )

        engine.runFullSyncNow()

        assertTrue("FLASHCARD_CORRECT must be sent to /v1/sync/events", capturedSyncEventsBody.contains("evt-flashcard-1"))
        assertFalse(
            "ROOM_MESSAGE_OUTBOX must NOT be sent to /v1/sync/events",
            capturedSyncEventsBody.contains("evt-room-outbox-1")
        )

        val remainingPending = syncDao.getPendingBatch(limit = 50)
        assertEquals(
            "Only ROOM_MESSAGE_OUTBOX should remain PENDING in sync_queue",
            listOf("evt-room-outbox-1"),
            remainingPending.map { it.eventId }
        )
    }

    @Test
    fun `SyncQueueDao separates pending sync events and room outbox batches`() = runBlocking {
        val syncDao = db.syncQueueDao()
        syncDao.enqueue(
            SyncQueueEntity(
                eventId = "evt-xp-1",
                eventType = "QUIZ_COMPLETED",
                payloadJson = "{}",
                createdAt = 100L,
                retryCount = 0,
                syncStatus = "PENDING",
                lastError = ""
            )
        )
        syncDao.enqueue(
            SyncQueueEntity(
                eventId = "evt-room-2",
                eventType = "ROOM_MESSAGE_OUTBOX",
                payloadJson = """{"room_slug":"python","body":"hi"}""",
                createdAt = 200L,
                retryCount = 0,
                syncStatus = "PENDING",
                lastError = ""
            )
        )

        assertEquals(listOf("evt-xp-1"), syncDao.getPendingSyncEventsBatch(limit = 100).map { it.eventId })
        assertEquals(listOf("evt-room-2"), syncDao.getPendingRoomOutboxBatch(limit = 50).map { it.eventId })
    }
}
