package com.techenglish.app.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.techenglish.app.MainActivity
import com.techenglish.app.core.network.AuthResponseDto
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.app.core.security.OAuthCallbackHandler
import com.techenglish.domain.model.AuthProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Sp103bOAuthDeepLinkTest {

    @Test
    fun `manifest resolves techenglish auth callback deep link to MainActivity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val deepLink = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("techenglish://auth/callback?ticket=abc123&state=st_xyz")
        )
        val resolved = context.packageManager.queryIntentActivities(deepLink, 0)
        assertTrue(
            "Expected MainActivity to handle techenglish://auth/callback",
            resolved.any { it.activityInfo.name == MainActivity::class.java.name }
        )
    }

    @Test
    fun `oauth handler persists state in DataStore rejects foreign state and exchanges valid ticket`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = AndroidKeystoreTokenStore(context)
        tokenStore.clearSession()

        var exchangedTicket: String? = null
        val handler = OAuthCallbackHandler(
            tokenStore = tokenStore,
            ticketExchanger = { ticket ->
                exchangedTicket = ticket
                AuthResponseDto(
                    accessToken = "jwt_access_oauth_1",
                    refreshToken = "jwt_refresh_oauth_1",
                    userId = "u-oauth-42",
                    username = "oauth_dev",
                    provider = "GITHUB",
                    isEmailVerified = true
                )
            }
        )

        // 1. Prepare authorize URL -> persists state in DataStore
        val authorizeUri = handler.prepareAuthorizeUrl(AuthProvider.GITHUB)
        assertTrue(authorizeUri.toString().contains("/v1/auth/oauth/github/authorize"))
        val expectedState = authorizeUri.getQueryParameter("client_state")
        assertNotNull("client_state must be present", expectedState)
        assertEquals("techenglish://auth/callback", authorizeUri.getQueryParameter("redirect_uri"))

        // 2. Foreign state is rejected and ticket is NOT exchanged
        val foreignResult = handler.handleCallbackUri(
            Uri.parse("techenglish://auth/callback?ticket=evil_ticket&state=forged_foreign_state")
        )
        assertTrue("Foreign state must be rejected", foreignResult.isFailure)
        assertEquals(null, exchangedTicket)

        // 3. Re-prepare fresh state and complete valid callback -> exchanges ticket and saves session
        val authorizeUri2 = handler.prepareAuthorizeUrl(AuthProvider.GITHUB)
        val validState = authorizeUri2.getQueryParameter("client_state")!!
        val okResult = handler.handleCallbackUri(
            Uri.parse("techenglish://auth/callback?ticket=valid_ticket_999&state=$validState")
        )
        assertTrue("Valid state & ticket must succeed: ${okResult.exceptionOrNull()}", okResult.isSuccess)
        assertEquals("valid_ticket_999", exchangedTicket)
        assertEquals("jwt_access_oauth_1", tokenStore.getAccessToken())

        // 4. Replaying the same state fails (consumed from DataStore)
        val replayResult = handler.handleCallbackUri(
            Uri.parse("techenglish://auth/callback?ticket=valid_ticket_999&state=$validState")
        )
        assertTrue("Replayed state must be rejected", replayResult.isFailure)
    }
}
