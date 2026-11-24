package com.techenglish.app.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.database.ContentPackImporter
import com.techenglish.app.core.database.TechEnglishDatabase
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.core.sync.CloudSyncEngine
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.usecase.ResolveSm2SyncConflictUseCase
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class Gap07ProfileAndFeedReactTest {

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
            accessToken = "valid_jwt",
            refreshToken = "valid_refresh",
            userId = "u-gap07",
            username = "gap07_dev",
            email = "gap07@example.com",
            provider = AuthProvider.EMAIL
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `refreshOnlineCaches pulls GET v1 profile me and caches user profile`() = runBlocking {
        var profileMeRequested = false
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path == "/v1/profile/me" && request.method == HttpMethod.Get -> {
                    profileMeRequested = true
                    respond(
                        content = """
                            {
                              "user_id": "u-gap07",
                              "username": "gap07_dev",
                              "display_name": "Gap Seven",
                              "avatar_key": "preset_terminal",
                              "status_text": "Shipping v3.1",
                              "tech_stack": ["Kotlin", "FastAPI"],
                              "github_url": "https://github.com/gap07",
                              "telegram_handle": "@gap07",
                              "rank_title": "Middle Developer",
                              "total_xp": 750,
                              "streak_days": 9,
                              "league": "GOLD",
                              "accepts_pings": true,
                              "badges": []
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else -> respond(
                    content = """{"top_10":[],"friends":[],"items":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = mockEngine)
        val engine = CloudSyncEngine(
            context = context,
            syncQueueDao = db.syncQueueDao(),
            socialCacheDao = db.socialCacheDao(),
            otaContentMetaDao = db.otaContentMetaDao(),
            wordDao = db.wordDao(),
            contentPackImporter = ContentPackImporter(database = db),
            apiClient = apiClient,
            tokenStore = tokenStore,
            resolveSm2Conflict = ResolveSm2SyncConflictUseCase()
        )

        engine.refreshOnlineCaches()

        assertTrue("refreshOnlineCaches must call GET /v1/profile/me", profileMeRequested)
        val cached = db.socialCacheDao().getProfile()
        assertNotNull(cached)
        assertEquals("gap07_dev", cached!!.username)
        assertEquals("Gap Seven", cached.displayName)
        assertEquals("Kotlin, FastAPI", cached.techStackCsv)
        assertEquals("GOLD", cached.league)
    }

    @Test
    fun `updateMyProfile sends PATCH and reactToFeedEvent sends POST to backend`() = runBlocking {
        var patchCalled = false
        var reactCalled = false
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path == "/v1/profile/me" && request.method == HttpMethod.Patch -> {
                    patchCalled = true
                    respond(
                        content = """
                            {
                              "user_id": "u-gap07",
                              "username": "gap07_dev",
                              "display_name": "Updated Dev",
                              "avatar_key": "preset_terminal",
                              "status_text": "Ready",
                              "tech_stack": ["Kotlin"],
                              "github_url": "",
                              "telegram_handle": "",
                              "rank_title": "Junior Developer",
                              "total_xp": 200,
                              "streak_days": 3,
                              "league": "BRONZE",
                              "accepts_pings": false,
                              "badges": []
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                path == "/v1/friends/feed/evt-99/react" && request.method == HttpMethod.Post -> {
                    reactCalled = true
                    respond(
                        content = """{"id":"evt-99","reaction_count":4,"user_reacted":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else -> respond(
                    content = """{"detail":"Unexpected $path"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        val apiClient = TechEnglishApiClient(context, tokenStore, engineOverride = mockEngine)
        val updated = apiClient.updateMyProfile(
            com.techenglish.app.core.network.ProfileUpdateBodyDto(
                displayName = "Updated Dev",
                acceptsPings = false
            )
        )
        assertTrue(patchCalled)
        assertEquals("Updated Dev", updated.displayName)
        assertEquals(false, updated.acceptsPings)

        val reactRes = apiClient.reactToFeedEvent("evt-99")
        assertTrue(reactCalled)
        assertEquals("evt-99", reactRes.id)
        assertEquals(4, reactRes.reactionCount)
        assertTrue(reactRes.userReacted)
    }
}
