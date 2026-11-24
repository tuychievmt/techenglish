package com.techenglish.app.core.sync

import android.content.Context
import kotlinx.coroutines.flow.first
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.techenglish.app.core.database.ContentPackImporter
import com.techenglish.app.core.database.dao.OtaContentMetaDao
import com.techenglish.app.core.database.dao.SocialCacheDao
import com.techenglish.app.core.database.dao.SyncQueueDao
import com.techenglish.app.core.database.dao.WordDao
import com.techenglish.app.core.database.entity.ActivityFeedCacheEntity
import com.techenglish.app.core.database.entity.FriendsCacheEntity
import com.techenglish.app.core.database.entity.LeaderboardCacheEntity
import com.techenglish.app.core.database.entity.OtaContentMetaEntity
import com.techenglish.app.core.database.entity.SyncQueueEntity
import com.techenglish.app.core.database.entity.WordEntity
import com.techenglish.app.core.network.Sm2ItemPayloadDto
import com.techenglish.app.core.network.SyncBatchBodyDto
import com.techenglish.app.core.network.SyncEventBodyDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.core.security.AndroidKeystoreTokenStore
import com.techenglish.domain.model.ConflictWinner
import com.techenglish.domain.model.LeaderboardTab
import com.techenglish.domain.model.Sm2SyncSnapshot
import com.techenglish.domain.model.SyncEventType
import com.techenglish.domain.usecase.ResolveSm2SyncConflictUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class SyncRunSummary(
    val isOffline: Boolean = false,
    val syncedEventsCount: Int = 0,
    val appliedOtaPacks: Int = 0,
    val messageRu: String = "",
    val hadTransientFailure: Boolean = false
)

/**
 * ФТ-9.2 & ФТ-14.5 — Bidirectional Outbox Sync & Data-Only OTA Content Delivery Engine.
 */
@Singleton
class CloudSyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncQueueDao: SyncQueueDao,
    private val socialCacheDao: SocialCacheDao,
    private val otaContentMetaDao: OtaContentMetaDao,
    private val wordDao: WordDao,
    private val contentPackImporter: ContentPackImporter,
    private val apiClient: TechEnglishApiClient,
    private val tokenStore: AndroidKeystoreTokenStore,
    private val resolveSm2Conflict: ResolveSm2SyncConflictUseCase
) {
    suspend fun enqueueOutboxEvent(
        eventType: SyncEventType,
        payloadJson: String = "{}",
        createdAtMillis: Long = System.currentTimeMillis()
    ): String {
        val eventId = UUID.randomUUID().toString()
        syncQueueDao.enqueue(
            SyncQueueEntity(
                eventId = eventId,
                eventType = eventType.name,
                payloadJson = payloadJson,
                createdAt = createdAtMillis,
                retryCount = 0,
                syncStatus = "PENDING",
                lastError = ""
            )
        )
        return eventId
    }

    suspend fun runFullSyncNow(): SyncRunSummary {
        if (!apiClient.isNetworkAvailable()) {
            return SyncRunSummary(
                isOffline = true,
                messageRu = "Офлайн-режим: показан локальный кэш, события ждут в sync_queue."
            )
        }

        val pending = syncQueueDao.getPendingSyncEventsBatch(limit = 50)
        var syncedCount = 0
        var hadTransientFailure = false
        val token = tokenStore.getAccessToken()

        if (!token.isNullOrBlank()) {
            val nowMs = System.currentTimeMillis()
            val studiedWords = wordDao.getStudiedWords()
            val favoriteTerms = wordDao.getFavoriteWords().map { it.term.trim().lowercase() }.distinct()
            val customWords = wordDao.getCustomWords().associate { it.term.trim().lowercase() to it.translation }
            val sm2Updates = studiedWords.map { w ->
                Sm2ItemPayloadDto(
                    normalizedTerm = w.term.trim().lowercase(),
                    repetitions = w.repetitions,
                    easeFactor = w.easeFactor,
                    intervalDays = w.intervalDays,
                    nextReviewAtMs = w.nextReviewAt,
                    clientTimestampMs = nowMs
                )
            }

            if (pending.isNotEmpty() || sm2Updates.isNotEmpty() || favoriteTerms.isNotEmpty() || customWords.isNotEmpty()) {
                val batch = SyncBatchBodyDto(
                    events = pending.map { row ->
                        val type = SyncEventType.fromRaw(row.eventType)
                        SyncEventBodyDto(
                            eventId = row.eventId,
                            eventType = type.name,
                            clientReportedXp = type.canonicalXp,
                            clientTimestampMs = row.createdAt,
                            payloadJson = row.payloadJson
                        )
                    },
                    sm2Updates = sm2Updates,
                    favoriteTerms = favoriteTerms,
                    customWords = customWords
                )
                runCatching { apiClient.syncEvents(batch) }
                    .onSuccess { res ->
                        val doneIds = (res.acceptedEventIds + res.duplicateEventIds).distinct()
                        if (doneIds.isNotEmpty()) {
                            syncQueueDao.markSynced(doneIds)
                            syncedCount = doneIds.size
                        }
                    }
                    .onFailure { err ->
                        hadTransientFailure = true
                        pending.forEach { syncQueueDao.recordRetryFailure(it.eventId, err.localizedMessage ?: "sync_error") }
                    }
            }

            // Pull authoritative remote state via GET /v1/sync/state and merge via Errata E1 / R10
            pullAndMergeRemoteSyncState()

            // Refresh social caches
            refreshOnlineCaches()
        }

        // Check & apply data-only OTA content packs (ФТ-14.5, Errata E3 & E4)
        val otaApplied = syncOtaContentPacks()

        return SyncRunSummary(
            isOffline = false,
            syncedEventsCount = syncedCount,
            appliedOtaPacks = otaApplied,
            messageRu = "Синхронизация завершена: событий — $syncedCount, OTA-пакетов — $otaApplied.",
            hadTransientFailure = hadTransientFailure
        )
    }

    internal suspend fun pullAndMergeRemoteSyncState() {
        runCatching { apiClient.fetchSyncState() }.onSuccess { remoteState ->
            val nowMs = System.currentTimeMillis()
            for (remote in remoteState.sm2States) {
                val localWord = wordDao.findEntityByNormalizedTerm(remote.normalizedTerm) ?: continue
                val localSnapshot = Sm2SyncSnapshot(
                    normalizedTerm = localWord.term.trim().lowercase(),
                    repetitions = localWord.repetitions,
                    easeFactor = localWord.easeFactor,
                    intervalDays = localWord.intervalDays,
                    nextReviewAtMillis = localWord.nextReviewAt,
                    clientTimestampMillis = localWord.nextReviewAt.takeIf { it > 0L }?.let { nowMs - 1_000L } ?: 0L,
                    serverReceivedAtMillis = nowMs
                )
                val remoteSnapshot = Sm2SyncSnapshot(
                    normalizedTerm = remote.normalizedTerm.trim().lowercase(),
                    repetitions = remote.repetitions,
                    easeFactor = remote.easeFactor,
                    intervalDays = remote.intervalDays,
                    nextReviewAtMillis = remote.nextReviewAtMs,
                    clientTimestampMillis = remote.clientTimestampMs,
                    serverReceivedAtMillis = remote.serverReceivedMs.takeIf { it > 0L } ?: nowMs
                )
                val decision = resolveSm2Conflict(localSnapshot, remoteSnapshot)
                if (decision.winner == ConflictWinner.REMOTE) {
                    val resolved = decision.resolvedSnapshot
                    wordDao.updateSrsState(
                        wordId = localWord.id,
                        repetitions = resolved.repetitions,
                        easeFactor = resolved.easeFactor,
                        intervalDays = resolved.intervalDays,
                        nextReviewAt = resolved.nextReviewAtMillis
                    )
                }
            }

            // Favorites merge: set union
            for (favTerm in remoteState.favoriteTerms) {
                val localWord = wordDao.findEntityByNormalizedTerm(favTerm)
                if (localWord != null && !localWord.isFavorite) {
                    wordDao.updateFavorite(localWord.id, true)
                }
            }

            // Custom words («Мои слова», category_id = 11) merge: set union
            for ((term, translation) in remoteState.customWords) {
                val existing = wordDao.findEntityByNormalizedTerm(term)
                if (existing == null && term.isNotBlank()) {
                    wordDao.insert(
                        WordEntity(
                            categoryId = SYSTEM_CUSTOM_CATEGORY_ID,
                            term = term.trim(),
                            transcription = "",
                            translation = translation.ifBlank { term.trim() },
                            example = "",
                            explanationRu = "",
                            isFavorite = false,
                            isCustom = true
                        )
                    )
                }
            }
        }
    }

    suspend fun refreshOnlineCaches() {
        val now = System.currentTimeMillis()
        runCatching { apiClient.fetchMyProfile() }.onSuccess { profile ->
            if (profile.userId.isNotBlank()) {
                socialCacheDao.upsertProfile(
                    com.techenglish.app.core.database.entity.UserProfileCacheEntity(
                        userId = profile.userId,
                        username = profile.username,
                        displayName = profile.displayName,
                        avatarKey = profile.avatarKey,
                        statusText = profile.statusText,
                        techStackCsv = profile.techStack.joinToString(", "),
                        githubUrl = profile.githubUrl,
                        telegramHandle = profile.telegramHandle,
                        rankTitle = profile.rankTitle,
                        totalXp = profile.totalXp,
                        streakDays = profile.streakDays,
                        league = profile.league,
                        updatedAt = now
                    )
                )
            }
        }
        LeaderboardTab.entries.forEach { tab ->
            runCatching { apiClient.fetchLeaderboard(tab.apiKey) }.onSuccess { dto ->
                val merged = (dto.top10 + listOfNotNull(dto.currentUserEntry)).distinctBy { it.userId }
                socialCacheDao.clearLeaderboardTab(tab.name)
                socialCacheDao.insertLeaderboardEntries(
                    merged.map { row ->
                        LeaderboardCacheEntity(
                            tab = tab.name,
                            userId = row.userId,
                            rank = row.rank,
                            username = row.username,
                            avatarKey = row.avatarKey,
                            rankTitle = row.rankTitle,
                            league = row.league,
                            techStack = row.techStack,
                            weeklyXp = row.weeklyXp,
                            totalXp = row.totalXp,
                            isCurrentUser = row.isCurrentUser,
                            cachedAt = now
                        )
                    }
                )
            }
        }

        runCatching { apiClient.fetchFriends() }.onSuccess { dto ->
            socialCacheDao.clearFriends()
            socialCacheDao.insertFriends(
                dto.friends.map { f ->
                    FriendsCacheEntity(
                        userId = f.userId,
                        username = f.username,
                        avatarKey = f.avatarKey,
                        rankTitle = f.rankTitle,
                        streakDays = f.streakDays,
                        acceptsPings = f.acceptsPings,
                        lastPingedAt = f.lastPingedAtMs,
                        cachedAt = now
                    )
                }
            )
        }

        runCatching { apiClient.fetchActivityFeed() }.onSuccess { dto ->
            val locallyReactedIds = socialCacheDao.observeActivityFeed().first()
                .filter { it.userReacted }
                .map { it.id }
                .toSet()
            socialCacheDao.clearActivityFeed()
            socialCacheDao.insertActivityFeed(
                dto.items.map { item ->
                    ActivityFeedCacheEntity(
                        id = item.id,
                        actorUserId = item.actorUserId,
                        actorUsername = item.actorUsername,
                        summaryRu = item.summaryRu,
                        reactionCount = item.reactionCount,
                        userReacted = item.userReacted || item.id in locallyReactedIds,
                        createdAt = item.createdAtMs
                    )
                }
            )
        }
    }

    suspend fun syncOtaContentPacks(): Int {
        var applied = 0
        runCatching { apiClient.fetchOtaManifest() }.onSuccess { manifest ->
            for (pack in manifest.packs) {
                if (!pack.dataOnly) continue
                val existing = otaContentMetaDao.getPackMeta(pack.packId)
                val alreadyApplied = existing != null &&
                    existing.lastStatus == "APPLIED" &&
                    existing.sha256Hex.equals(pack.sha256Hex, ignoreCase = true)
                if (alreadyApplied) {
                    continue
                }
                val attemptTs = System.currentTimeMillis()
                runCatching {
                    val payloadJson = if (!pack.downloadUrl.isNullOrBlank()) {
                        apiClient.fetchExternalOtaPayload(pack.downloadUrl)
                    } else {
                        apiClient.fetchOtaPack(pack.packId).payloadJson
                    }
                    contentPackImporter.importFromJson(
                        jsonString = payloadJson,
                        expectedSha256Hex = pack.sha256Hex
                    )
                    otaContentMetaDao.upsertPackMeta(
                        OtaContentMetaEntity(
                            packId = pack.packId,
                            version = pack.version,
                            titleRu = pack.titleRu,
                            sha256Hex = pack.sha256Hex,
                            itemCount = pack.itemCount,
                            appliedAt = attemptTs,
                            lastStatus = "APPLIED",
                            lastError = "",
                            lastAttemptAt = attemptTs
                        )
                    )
                    applied++
                }.onFailure { err ->
                    otaContentMetaDao.upsertPackMeta(
                        OtaContentMetaEntity(
                            packId = pack.packId,
                            version = pack.version,
                            titleRu = pack.titleRu,
                            sha256Hex = if (existing?.lastStatus == "APPLIED") existing.sha256Hex else "",
                            itemCount = existing?.itemCount ?: 0,
                            appliedAt = existing?.appliedAt ?: 0L,
                            lastStatus = "FAILED",
                            lastError = (err.localizedMessage ?: err.javaClass.simpleName).take(240),
                            lastAttemptAt = attemptTs
                        )
                    )
                }
            }
        }
        return applied
    }

    fun scheduleBackgroundSync() {
        runCatching {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val periodic = PeriodicWorkRequestBuilder<CloudSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic
            )
        }
    }

    fun triggerImmediateSync() {
        runCatching {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val oneShot = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(oneShot)
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "techenglish_cloud_sync_periodic"
        const val SYSTEM_CUSTOM_CATEGORY_ID = 11
    }
}

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncEngine: CloudSyncEngine
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val summary = syncEngine.runFullSyncNow()
        if (summary.hadTransientFailure) Result.retry() else Result.success()
    }.getOrElse {
        Result.retry()
    }
}
