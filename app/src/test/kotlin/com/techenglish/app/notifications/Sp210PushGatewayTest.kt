package com.techenglish.app.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.notifications.DefaultPushNotificationGateway
import com.techenglish.app.core.notifications.NotificationHelper
import com.techenglish.app.core.notifications.PushNotificationGateway
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.domain.model.AuthProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class Sp210PushGatewayTest {

    @Test
    fun `push gateway registers and unregisters token via API and shows local notification on friend ping`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = NotificationHelper(context)
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.saveTokensAndSession(
            accessToken = "valid_jwt_access",
            refreshToken = "valid_jwt_refresh",
            userId = "u1",
            username = "tester",
            email = "tester@example.com",
            provider = AuthProvider.EMAIL
        )

        val calledEndpoints = mutableListOf<Pair<HttpMethod, String>>()
        val mockEngine = MockEngine { request ->
            calledEndpoints += request.method to request.url.encodedPath
            when (request.method) {
                HttpMethod.Post -> respond(
                    content = """{"registered":true,"platform":"ANDROID"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                HttpMethod.Delete -> respond(
                    content = """{"removed":1}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
                else -> respond(
                    content = """{"detail":"unexpected"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        val apiClient = TechEnglishApiClient(
            context = context,
            tokenStore = tokenStore,
            engineOverride = mockEngine
        )
        val gateway: PushNotificationGateway = DefaultPushNotificationGateway(
            context = context,
            apiClient = apiClient,
            notificationHelper = helper
        )

        // Without google-services.json configured in resources, FCM reports disabled cleanly
        assertFalse(gateway.isFcmEnabled)

        val registered = gateway.registerDeviceToken("fcm_test_token_android_001")
        assertTrue(registered)
        assertEquals(HttpMethod.Post to "/v1/notifications/device-token", calledEndpoints.first())

        val shown = gateway.handleIncomingPush(
            type = "friend_ping",
            title = "Пинг от @alice",
            body = "Пора закрыть дневную цель по IT-терминам!",
            destination = "flashcards"
        )
        assertTrue(shown)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowManager = Shadows.shadowOf(manager)
        assertTrue("Expected at least one active push notification", shadowManager.allNotifications.isNotEmpty())

        val unregistered = gateway.unregisterDeviceToken("fcm_test_token_android_001")
        assertTrue(unregistered)
        assertEquals(HttpMethod.Delete to "/v1/notifications/device-token", calledEndpoints.last())
    }
}
