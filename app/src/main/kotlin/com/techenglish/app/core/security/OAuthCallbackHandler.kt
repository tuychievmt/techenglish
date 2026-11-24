package com.techenglish.app.core.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import com.techenglish.app.core.network.AuthResponseDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.domain.model.AuthProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SP1-03b / BUG-04 — Client-side OAuth 2.0 Custom Tabs + DataStore state + one-time ticket exchange.
 *
 * Flow:
 * 1. Generates cryptographically random `client_state` and persists it strictly in DataStore.
 * 2. Launches Android Custom Tabs to `/v1/auth/oauth/{provider}/authorize?redirect_uri=techenglish://auth/callback&client_state=...`.
 * 3. Server handles PKCE + provider `client_secret` exchange and redirects back to `techenglish://auth/callback?ticket=...&state=...`.
 * 4. [handleCallbackUri] atomically consumes `client_state` from DataStore, rejects foreign/replayed state,
 *    exchanges the single-use `ticket` for JWTs via `POST /v1/auth/oauth/ticket/exchange`, and persists session.
 */
@Singleton
class OAuthCallbackHandler internal constructor(
    private val tokenStore: AndroidKeystoreTokenStore,
    private val ticketExchanger: suspend (String) -> AuthResponseDto
) {
    @Inject
    constructor(
        tokenStore: AndroidKeystoreTokenStore,
        apiClient: TechEnglishApiClient
    ) : this(
        tokenStore = tokenStore,
        ticketExchanger = { ticket -> apiClient.exchangeOAuthTicket(ticket) }
    )

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    suspend fun prepareAuthorizeUrl(provider: AuthProvider): Uri {
        val stateBytes = ByteArray(24)
        SecureRandom().nextBytes(stateBytes)
        val clientState = Base64.encodeToString(
            stateBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        tokenStore.savePendingOAuthState(clientState, provider)

        val base = tokenStore.getApiBaseUrl().trimEnd('/')
        val providerSlug = if (provider == AuthProvider.GITHUB) "github" else "google"
        return Uri.parse("$base/v1/auth/oauth/$providerSlug/authorize")
            .buildUpon()
            .appendQueryParameter("redirect_uri", CALLBACK_REDIRECT_URI)
            .appendQueryParameter("client_state", clientState)
            .build()
    }

    fun launchCustomTab(context: Context, authorizeUri: Uri) {
        val launched = runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, authorizeUri)
        }.isSuccess
        if (!launched) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, authorizeUri))
            }
        }
    }

    fun isOAuthCallbackUri(uri: Uri?): Boolean {
        return uri != null &&
            uri.scheme == CALLBACK_SCHEME &&
            uri.host == CALLBACK_HOST &&
            uri.path == CALLBACK_PATH
    }

    suspend fun handleCallbackUri(uri: Uri): Result<AuthResponseDto> {
        if (!isOAuthCallbackUri(uri)) {
            return Result.failure(IllegalArgumentException("Unsupported OAuth callback URI"))
        }
        val pending = tokenStore.consumePendingOAuthState()
            ?: return Result.failure(IllegalStateException("Missing or already consumed OAuth state"))
        val (expectedState, _) = pending

        val returnedState = uri.getQueryParameter("state")
        if (returnedState.isNullOrBlank() ||
            !MessageDigest.isEqual(
                expectedState.toByteArray(Charsets.UTF_8),
                returnedState.toByteArray(Charsets.UTF_8)
            )
        ) {
            _events.tryEmit("Ошибка OAuth: неверный параметр state.")
            return Result.failure(SecurityException("OAuth state mismatch"))
        }

        val oauthError = uri.getQueryParameter("error")
        if (!oauthError.isNullOrBlank()) {
            _events.tryEmit("Вход через OAuth отменён или отклонён ($oauthError).")
            return Result.failure(IllegalStateException("OAuth error: $oauthError"))
        }

        val ticket = uri.getQueryParameter("ticket")
        if (ticket.isNullOrBlank()) {
            _events.tryEmit("Ошибка OAuth: сервер не вернул одноразовый тикет.")
            return Result.failure(IllegalArgumentException("Missing OAuth ticket"))
        }

        return runCatching {
            val auth = ticketExchanger(ticket)
            tokenStore.saveTokensAndSession(
                accessToken = auth.accessToken,
                refreshToken = auth.refreshToken,
                userId = auth.userId,
                username = auth.username,
                email = "",
                provider = AuthProvider.fromRaw(auth.provider)
            )
            _events.tryEmit("Вход через ${auth.provider} выполнен (@${auth.username}).")
            auth
        }.onFailure { err ->
            _events.tryEmit("Ошибка OAuth-обмена: ${err.localizedMessage ?: "повторите попытку"}")
        }
    }

    companion object {
        const val CALLBACK_SCHEME = "techenglish"
        const val CALLBACK_HOST = "auth"
        const val CALLBACK_PATH = "/callback"
        const val CALLBACK_REDIRECT_URI = "techenglish://auth/callback"
    }
}
