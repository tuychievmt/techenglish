package com.techenglish.app.stage4c

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowNetworkCapabilities
import com.techenglish.app.core.database.TechEnglishDatabase
import com.techenglish.app.core.database.entity.SquadCacheEntity
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.network.WsConnectionState
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.feature.stage4b.SquadsViewModel
import com.techenglish.app.feature.stage4c.ThematicRoomsViewModel
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.usecase.ValidateUgcContentUseCase
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class Sp304OfflineRoomsAndSquadCacheTest {

    private lateinit var context: Context
    private lateinit var db: TechEnglishDatabase
    private lateinit var dbName: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbName = "sp304_test_${UUID.randomUUID()}.db"
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
    fun `offline room messages enqueue in sync_queue and resend on reconnect, WS status is truthful, and Squad is cached in Room`() = runTest {
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.saveTokensAndSession(
            accessToken = "valid_jwt",
            refreshToken = "valid_refresh",
            userId = "u1",
            username = "tester",
            email = "tester@example.com",
            provider = AuthProvider.EMAIL
        )

        var postedRoomMessage = false
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path == "/v1/rooms/backend-english/messages" && request.method == HttpMethod.Post -> {
                    postedRoomMessage = true
                    respond(
                        content = """{"id":"srv-msg-99","room_slug":"backend-english","author_user_id":"u1","author_username":"tester","author_rank_title":"Senior Developer","body":"Offline queued message","created_at_ms":1800000000000}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else -> respond(
                    content = """{"messages":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }
        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = mockEngine)

        // 1. Offline room message saves local_msg_* AND enqueues in sync_queue -> flushes on reconnect!
        val roomsVm = ThematicRoomsViewModel(
            roomMessagesCacheDao = db.roomMessagesCacheDao(),
            syncQueueDao = db.syncQueueDao(),
            apiClient = apiClient,
            tokenStore = tokenStore,
            validateUgc = ValidateUgcContentUseCase()
        )
        val localId = roomsVm.cacheLocalMessage("backend-english", "u1", "tester", "Offline queued message", 1_800_000_000_000L)
        assertTrue(localId.startsWith("local_msg_"))
        assertEquals(1, db.syncQueueDao().getPendingCount())

        // Simulate network reconnect via Robolectric ConnectivityManager shadow
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = ShadowNetworkCapabilities.newInstance()
        Shadows.shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Shadows.shadowOf(cm).setNetworkCapabilities(cm.activeNetwork, caps)

        val flushed = roomsVm.flushPendingRoomMessages()
        assertEquals(1, flushed)
        assertTrue(postedRoomMessage)
        assertEquals(0, db.syncQueueDao().getPendingCount())

        val roomRows = db.roomMessagesCacheDao().observeRoomMessages("backend-english").first()
        assertTrue("Expected server-acknowledged message srv-msg-99", roomRows.any { it.id == "srv-msg-99" })
        assertFalse("Temporary local_msg_* row must be replaced after resend", roomRows.any { it.id == localId })

        // 2. Real WS status starts DISCONNECTED (never hardcoded CONNECTED before live handshake)
        assertEquals(WsConnectionState.DISCONNECTED, roomsVm.uiState.value.wsConnectionState)
        assertFalse(roomsVm.uiState.value.isWebSocketConnected)

        // 3. Squad data cached locally in Room & restored offline by SquadsViewModel
        db.squadCacheDao().upsertSquad(
            SquadCacheEntity(
                id = "sq-1",
                name = "Kotlin Ninjas",
                inviteCode = "KOTLIN99",
                membersJson = """[{"user_id":"u1","username":"tester","rank_title":"Senior Developer","weekly_xp":450}]""",
                weeklyGoalXp = 2000,
                currentWeekXp = 450,
                nextWeekBonusMultiplier = 1.2f,
                isActiveForChallenge = true,
                updatedAt = 1_800_000_000_000L
            )
        )
        val squadsVm = SquadsViewModel(apiClient = apiClient, tokenStore = tokenStore, squadCacheDao = db.squadCacheDao())
        squadsVm.loadCachedSquad()
        val cachedSquad = squadsVm.uiState.value.squad
        assertNotNull("Expected Squad to load from local Room cache offline", cachedSquad)
        assertEquals("Kotlin Ninjas", cachedSquad!!.name)
        assertEquals("KOTLIN99", cachedSquad.inviteCode)
        assertEquals(1, cachedSquad.members.size)
        assertEquals("tester", cachedSquad.members.first().username)
    }
}
