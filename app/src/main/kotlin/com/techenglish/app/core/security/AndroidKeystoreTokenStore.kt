package com.techenglish.app.core.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.techenglish.domain.model.AuthProvider
import com.techenglish.domain.model.AuthSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "techenglish_secure_tokens",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

internal interface TokenCipherKeyProvider {
    fun getOrCreateKey(): SecretKey
    fun resetKey()
}

private class HardwareAndroidKeystoreKeyProvider : TokenCipherKeyProvider {
    @Volatile
    private var robolectricAesKey: SecretKey? = null

    private fun isRobolectricJvm(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }

    override fun getOrCreateKey(): SecretKey = synchronized(this) {
        if (isRobolectricJvm()) {
            return robolectricAesKey ?: KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
                .also { robolectricAesKey = it }
        }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: generateAndroidKeystoreKey()
    }

    override fun resetKey() {
        synchronized(this) {
            if (isRobolectricJvm()) {
                robolectricAesKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
                return
            }
            runCatching {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                if (keyStore.containsAlias(KEY_ALIAS)) {
                    keyStore.deleteEntry(KEY_ALIAS)
                }
                generateAndroidKeystoreKey()
            }
        }
    }

    private fun generateAndroidKeystoreKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "techenglish_jwt_aes256_gcm_key"
    }
}

/**
 * НФТ-7.3 & SP1-07b (BUG-10) — Hardware-backed Android Keystore (AES-256-GCM) + Jetpack DataStore token storage.
 * - Replaces deprecated `EncryptedSharedPreferences`.
 * - NEVER falls back to plaintext (`PLAIN:`) storage.
 * - On Keystore corruption/invalidation, resets the alias and softly clears the session so the user re-authenticates.
 */
@Singleton
class AndroidKeystoreTokenStore internal constructor(
    private val context: Context,
    private val keyProvider: TokenCipherKeyProvider
) {
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(context = context, keyProvider = HardwareAndroidKeystoreKeyProvider())

    private object Keys {
        val ACCESS_TOKEN_ENC = stringPreferencesKey("access_token_enc")
        val REFRESH_TOKEN_ENC = stringPreferencesKey("refresh_token_enc")
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val PROVIDER = stringPreferencesKey("provider")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val OAUTH_PENDING_STATE = stringPreferencesKey("oauth_pending_state")
        val OAUTH_PENDING_PROVIDER = stringPreferencesKey("oauth_pending_provider")
    }

    private val _reauthRequired = MutableStateFlow(false)
    val reauthRequired: StateFlow<Boolean> = _reauthRequired.asStateFlow()

    val sessionFlow: Flow<AuthSession> = context.tokenDataStore.data
        .catch { err ->
            if (err is IOException) emit(emptyPreferences()) else throw err
        }
        .map { prefs ->
            val uid = prefs[Keys.USER_ID]
            if (uid.isNullOrBlank()) {
                AuthSession()
            } else {
                AuthSession(
                    userId = uid,
                    username = prefs[Keys.USERNAME] ?: "dev_user",
                    email = prefs[Keys.EMAIL].orEmpty(),
                    provider = AuthProvider.fromRaw(prefs[Keys.PROVIDER] ?: "EMAIL"),
                    isGuest = false
                )
            }
        }

    val apiBaseUrlFlow: Flow<String> = context.tokenDataStore.data
        .catch { err ->
            if (err is IOException) emit(emptyPreferences()) else throw err
        }
        .map { prefs ->
            prefs[Keys.API_BASE_URL] ?: DEFAULT_API_BASE_URL
        }

    suspend fun getAccessToken(): String? {
        val prefs = runCatching { context.tokenDataStore.data.first() }.getOrNull() ?: return null
        val enc = prefs[Keys.ACCESS_TOKEN_ENC] ?: return null
        val decrypted = decrypt(enc)
        if (decrypted == null) {
            handleKeystoreRecovery()
        }
        return decrypted
    }

    suspend fun getRefreshToken(): String? {
        val prefs = runCatching { context.tokenDataStore.data.first() }.getOrNull() ?: return null
        val enc = prefs[Keys.REFRESH_TOKEN_ENC] ?: return null
        val decrypted = decrypt(enc)
        if (decrypted == null) {
            handleKeystoreRecovery()
        }
        return decrypted
    }

    suspend fun getApiBaseUrl(): String = apiBaseUrlFlow.first()

    suspend fun setApiBaseUrl(url: String) {
        val clean = url.trim().trimEnd('/')
        val allowLocalCleartext = com.techenglish.app.BuildConfig.DEBUG
        if (
            clean.startsWith("https://") ||
            (allowLocalCleartext && (
                clean.startsWith("http://10.0.2.2") ||
                clean.startsWith("http://localhost") ||
                clean.startsWith("http://127.0.0.1")
            ))
        ) {
            context.tokenDataStore.edit { it[Keys.API_BASE_URL] = clean }
        }
    }

    suspend fun saveTokensAndSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        username: String,
        email: String,
        provider: AuthProvider
    ) {
        val (encAccess, encRefresh) = encryptPairOrRecover(accessToken, refreshToken) ?: run {
            handleKeystoreRecovery()
            return
        }
        _reauthRequired.value = false
        context.tokenDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN_ENC] = encAccess
            prefs[Keys.REFRESH_TOKEN_ENC] = encRefresh
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USERNAME] = username
            prefs[Keys.EMAIL] = email
            prefs[Keys.PROVIDER] = provider.name
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String): Boolean {
        val (encAccess, encRefresh) = encryptPairOrRecover(accessToken, refreshToken) ?: run {
            handleKeystoreRecovery()
            return false
        }
        _reauthRequired.value = false
        context.tokenDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN_ENC] = encAccess
            prefs[Keys.REFRESH_TOKEN_ENC] = encRefresh
        }
        return true
    }

    suspend fun updateSessionProfile(username: String? = null, email: String? = null) {
        context.tokenDataStore.edit { prefs ->
            if (!username.isNullOrBlank()) {
                prefs[Keys.USERNAME] = username.trim()
            }
            if (!email.isNullOrBlank()) {
                prefs[Keys.EMAIL] = email.trim()
            }
        }
    }

    suspend fun savePendingOAuthState(state: String, provider: AuthProvider) {
        context.tokenDataStore.edit { prefs ->
            prefs[Keys.OAUTH_PENDING_STATE] = state
            prefs[Keys.OAUTH_PENDING_PROVIDER] = provider.name
        }
    }

    suspend fun consumePendingOAuthState(): Pair<String, AuthProvider>? {
        var result: Pair<String, AuthProvider>? = null
        context.tokenDataStore.edit { prefs ->
            val state = prefs[Keys.OAUTH_PENDING_STATE]
            val providerRaw = prefs[Keys.OAUTH_PENDING_PROVIDER]
            prefs.remove(Keys.OAUTH_PENDING_STATE)
            prefs.remove(Keys.OAUTH_PENDING_PROVIDER)
            if (!state.isNullOrBlank()) {
                result = state to AuthProvider.fromRaw(providerRaw ?: "GITHUB")
            }
        }
        return result
    }

    /**
     * Atomically verifies [returnedState] against the stored pending OAuth state and consumes it ONLY on match,
     * protecting an in-flight Custom Tabs OAuth flow from being cancelled by forged/foreign deep links.
     */
    suspend fun consumePendingOAuthStateIfMatches(returnedState: String): OAuthStateMatchResult {
        if (returnedState.isBlank()) return OAuthStateMatchResult.MISMATCH
        var matchResult: OAuthStateMatchResult = OAuthStateMatchResult.MISSING
        context.tokenDataStore.edit { prefs ->
            val expectedState = prefs[Keys.OAUTH_PENDING_STATE]
            if (expectedState.isNullOrBlank()) {
                matchResult = OAuthStateMatchResult.MISSING
                return@edit
            }
            val matches = MessageDigest.isEqual(
                expectedState.toByteArray(Charsets.UTF_8),
                returnedState.toByteArray(Charsets.UTF_8)
            )
            if (matches) {
                val providerRaw = prefs[Keys.OAUTH_PENDING_PROVIDER]
                prefs.remove(Keys.OAUTH_PENDING_STATE)
                prefs.remove(Keys.OAUTH_PENDING_PROVIDER)
                matchResult = OAuthStateMatchResult.Matched(AuthProvider.fromRaw(providerRaw ?: "GITHUB"))
            } else {
                matchResult = OAuthStateMatchResult.MISMATCH
            }
        }
        return matchResult
    }

    sealed interface OAuthStateMatchResult {
        data class Matched(val provider: AuthProvider) : OAuthStateMatchResult
        data object MISSING : OAuthStateMatchResult
        data object MISMATCH : OAuthStateMatchResult
    }

    suspend fun clearSession() {
        context.tokenDataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN_ENC)
            prefs.remove(Keys.REFRESH_TOKEN_ENC)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.PROVIDER)
            prefs.remove(Keys.OAUTH_PENDING_STATE)
            prefs.remove(Keys.OAUTH_PENDING_PROVIDER)
        }
    }

    private suspend fun handleKeystoreRecovery() {
        runCatching { keyProvider.resetKey() }
        clearSession()
        _reauthRequired.value = true
    }

    private fun encryptPairOrRecover(first: String, second: String): Pair<String, String>? {
        runCatching { encrypt(first) to encrypt(second) }.onSuccess { return it }
        runCatching { keyProvider.resetKey() }
        return runCatching { encrypt(first) to encrypt(second) }.getOrNull()
    }

    internal fun encrypt(plaintext: String): String {
        val key = keyProvider.getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ctB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "GCM:$ivB64:$ctB64"
    }

    internal fun decrypt(encoded: String): String? {
        return runCatching {
            if (!encoded.startsWith("GCM:")) return@runCatching null
            val parts = encoded.removePrefix("GCM:").split(":", limit = 2)
            if (parts.size != 2) return@runCatching null
            val key = keyProvider.getOrCreateKey()
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ct = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        }.getOrNull()
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        val DEFAULT_API_BASE_URL: String = com.techenglish.app.BuildConfig.API_BASE_URL
    }
}
