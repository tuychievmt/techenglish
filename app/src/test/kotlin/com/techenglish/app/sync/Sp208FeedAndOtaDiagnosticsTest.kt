package com.techenglish.app.sync

import android.content.Context
import android.os.Build
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
import java.security.MessageDigest
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class Sp208FeedAndOtaDiagnosticsTest {

    private lateinit var context: Context
    private lateinit var db: TechEnglishDatabase
    private lateinit var dbName: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbName = "sp208_test_${UUID.randomUUID()}.db"
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

    private fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    @Test
    fun `refreshOnlineCaches preserves user_reacted and syncOtaContentPacks logs error then retries corrupt pack`() = runTest {
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.saveTokensAndSession(
            accessToken = "valid_jwt",
            refreshToken = "valid_refresh",
            userId = "u1",
            username = "tester",
            email = "tester@example.com",
            provider = AuthProvider.EMAIL
        )

        val validPayload = """{"pack_id":"ota_test_pack","version":2,"domain":"Core","terms":[]}"""
        val expectedSha = sha256Hex(validPayload)
        var serveCorruptPack = true

        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path == "/v1/profile/me" -> respond(
                    content = """{"user_id":"u1","username":"tester","display_name":"Tester","badges":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                path.startsWith("/v1/leaderboards/") -> respond(
                    content = """{"tab":"weekly","top_10":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                path == "/v1/friends" -> respond(
                    content = """{"friends":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                path == "/v1/friends/feed" -> respond(
                    content = """
                        {
                          "items": [
                            {
                              "id": "feed-reacted-1",
                              "actor_user_id": "u2",
                              "actor_username": "alice",
                              "summary_ru": "Закрыла цель дня",
                              "reaction_count": 3,
                              "user_reacted": true,
                              "created_at_ms": 1800000000000
                            },
                            {
                              "id": "feed-unreacted-2",
                              "actor_user_id": "u3",
                              "actor_username": "bob",
                              "summary_ru": "Изучил 15 терминов",
                              "reaction_count": 1,
                              "user_reacted": false,
                              "created_at_ms": 1800000000001
                            }
                          ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                path == "/v1/ota/manifest" -> respond(
                    content = """
                        {
                          "packs": [
                            {
                              "pack_id": "ota_test_pack",
                              "version": 2,
                              "title_ru": "Тестовый пакет OTA",
                              "sha256_hex": "$expectedSha",
                              "item_count": 0,
                              "data_only": true
                            }
                          ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                path == "/v1/ota/packs/ota_test_pack" -> {
                    val payloadToSend = if (serveCorruptPack) "{CORRUPT_PAYLOAD" else validPayload
                    val escaped = payloadToSend.replace("\"", "\\\"")
                    respond(
                        content = """{"pack_id":"ota_test_pack","version":2,"sha256_hex":"$expectedSha","payload_json":"$escaped"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else -> respond(
                    content = """{}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        val apiClient = TechEnglishApiClient(context = context, tokenStore = tokenStore, engineOverride = mockEngine)
        val syncEngine = CloudSyncEngine(
            context = context,
            apiClient = apiClient,
            tokenStore = tokenStore,
            syncQueueDao = db.syncQueueDao(),
            socialCacheDao = db.socialCacheDao(),
            otaContentMetaDao = db.otaContentMetaDao(),
            wordDao = db.wordDao(),
            contentPackImporter = ContentPackImporter(db),
            resolveSm2Conflict = ResolveSm2SyncConflictUseCase()
        )

        // 1. Verify feed sync preserves user_reacted = true from backend (BUG-25)
        syncEngine.refreshOnlineCaches()
        val cachedFeed = db.socialCacheDao().observeActivityFeed().first()
        val item1 = cachedFeed.first { it.id == "feed-reacted-1" }
        val item2 = cachedFeed.first { it.id == "feed-unreacted-2" }
        assertTrue("Expected userReacted = true to be preserved from backend", item1.userReacted)
        assertFalse("Expected userReacted = false for unreacted item", item2.userReacted)

        // 2. First OTA sync serves corrupt payload -> persists FAILED status & non-empty error
        val appliedFirst = syncEngine.syncOtaContentPacks()
        assertEquals(0, appliedFirst)
        val failedMeta = db.otaContentMetaDao().getPackMeta("ota_test_pack")
        assertNotNull("Failed OTA attempt must be persisted in ota_content_meta for diagnostics", failedMeta)
        assertEquals("FAILED", failedMeta!!.lastStatus)
        assertTrue("Expected diagnostic lastError message", failedMeta.lastError.isNotBlank())

        // 3. Second OTA sync serves valid payload -> retries previously failed pack and marks APPLIED
        serveCorruptPack = false
        val appliedSecond = syncEngine.syncOtaContentPacks()
        assertEquals(1, appliedSecond)
        val appliedMeta = db.otaContentMetaDao().getPackMeta("ota_test_pack")!!
        assertEquals("APPLIED", appliedMeta.lastStatus)
        assertEquals("", appliedMeta.lastError)
        assertEquals(expectedSha, appliedMeta.sha256Hex)
    }
}
