package com.techenglish.app.core.notifications

import android.content.Context
import com.techenglish.app.core.network.TechEnglishApiClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SP2-10 / R3: Client-side abstraction for push notifications (FCM HTTP v1 + local fallback).
 * Allows compiling and running cleanly without `google-services.json` while seamlessly activating
 * FCM delivery once Firebase project resources are provisioned by the owner.
 */
interface PushNotificationGateway {
    val isFcmEnabled: Boolean

    suspend fun registerDeviceToken(token: String): Boolean

    suspend fun unregisterDeviceToken(token: String? = null): Boolean

    fun handleIncomingPush(
        type: String,
        title: String,
        body: String,
        destination: String? = null
    ): Boolean
}

@Singleton
class DefaultPushNotificationGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: TechEnglishApiClient,
    private val notificationHelper: NotificationHelper
) : PushNotificationGateway {

    override val isFcmEnabled: Boolean
        get() = runCatching {
            val resId = context.resources.getIdentifier("google_app_id", "string", context.packageName)
            resId != 0 && context.getString(resId).isNotBlank()
        }.getOrDefault(false)

    override suspend fun registerDeviceToken(token: String): Boolean {
        val clean = token.trim()
        if (clean.length < 8) return false
        return runCatching { apiClient.registerPushDeviceToken(clean) }.getOrDefault(false)
    }

    override suspend fun unregisterDeviceToken(token: String?): Boolean {
        return runCatching { apiClient.unregisterPushDeviceToken(token?.trim()?.ifBlank { null }) }
            .getOrDefault(false)
    }

    override fun handleIncomingPush(
        type: String,
        title: String,
        body: String,
        destination: String?
    ): Boolean {
        val resolvedDestination = destination ?: when (type) {
            "friend_ping" -> NotificationConstants.DEST_FLASHCARDS
            else -> null
        }
        return notificationHelper.showPushNotification(
            title = title,
            body = body,
            destination = resolvedDestination
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PushNotificationModule {
    @Binds
    @Singleton
    abstract fun bindPushNotificationGateway(
        impl: DefaultPushNotificationGateway
    ): PushNotificationGateway
}
