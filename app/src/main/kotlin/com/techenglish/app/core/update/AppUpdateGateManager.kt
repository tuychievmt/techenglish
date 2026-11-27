package com.techenglish.app.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.techenglish.app.BuildConfig
import com.techenglish.app.core.datastore.StoredUserPreferences
import com.techenglish.app.core.datastore.UserPreferencesDataStore
import com.techenglish.app.core.network.AppVersionMetadataDto
import com.techenglish.app.core.network.TechEnglishApiClient
import com.techenglish.app.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateGateState {
    data object UpToDate : UpdateGateState

    data class Optional(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val apkUrl: String,
        val apkSha256: String,
        val changelogRu: String
    ) : UpdateGateState

    data class Required(
        val minSupportedVersionCode: Int,
        val latestVersionCode: Int,
        val latestVersionName: String,
        val apkUrl: String,
        val apkSha256: String,
        val changelogRu: String
    ) : UpdateGateState
}

sealed interface ApkDownloadState {
    data object Idle : ApkDownloadState

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progressFraction: Float
    ) : ApkDownloadState

    data class ReadyToInstall(
        val apkFile: File,
        val sha256Hex: String
    ) : ApkDownloadState

    data class NeedsInstallPermission(
        val apkFile: File
    ) : ApkDownloadState

    data class Error(
        val messageRu: String
    ) : ApkDownloadState
}

@Singleton
class AppUpdateGateManager internal constructor(
    private val context: Context,
    private val apiClient: TechEnglishApiClient,
    private val userPrefs: UserPreferencesDataStore,
    private val appScope: CoroutineScope,
    private val currentVersionCode: Int,
    private val currentVersionName: String,
    private val fallbackManifestUrl: String,
    private val clockMs: () -> Long
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        apiClient: TechEnglishApiClient,
        userPrefs: UserPreferencesDataStore,
        @ApplicationScope appScope: CoroutineScope
    ) : this(
        context = context,
        apiClient = apiClient,
        userPrefs = userPrefs,
        appScope = appScope,
        currentVersionCode = BuildConfig.VERSION_CODE,
        currentVersionName = BuildConfig.VERSION_NAME,
        fallbackManifestUrl = BuildConfig.OTA_MANIFEST_FALLBACK_URL,
        clockMs = { System.currentTimeMillis() }
    )

    private val checkMutex = Mutex()
    private var activeDownloadJob: Job? = null

    @Volatile
    private var lastCheckTimestampMs: Long = 0L

    private val _gateState = MutableStateFlow<UpdateGateState>(UpdateGateState.UpToDate)
    val gateState: StateFlow<UpdateGateState> = _gateState.asStateFlow()

    private val _downloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
    val downloadState: StateFlow<ApkDownloadState> = _downloadState.asStateFlow()

    init {
        appScope.launch {
            val prefs = runCatching { userPrefs.preferencesFlow.first() }.getOrNull()
            if (prefs != null) {
                _gateState.value = evaluateGateState(
                    currentVersionCode = currentVersionCode,
                    remoteMeta = null,
                    cachedPrefs = prefs,
                    nowMs = clockMs()
                )
            }
        }
    }

    fun checkForUpdatesAsync(force: Boolean = false) {
        appScope.launch {
            checkForUpdates(force = force)
        }
    }

    suspend fun checkForUpdates(force: Boolean = false): UpdateGateState = checkMutex.withLock {
        val now = clockMs()
        val cachedPrefs = runCatching { userPrefs.preferencesFlow.first() }.getOrDefault(StoredUserPreferences())

        // Always evaluate cached floor first so offline Required gate applies immediately
        val cachedState = evaluateGateState(
            currentVersionCode = currentVersionCode,
            remoteMeta = null,
            cachedPrefs = cachedPrefs,
            nowMs = now
        )
        if (_gateState.value == UpdateGateState.UpToDate && cachedState is UpdateGateState.Required) {
            _gateState.value = cachedState
        }

        if (!force && lastCheckTimestampMs > 0L && (now - lastCheckTimestampMs) < THROTTLE_INTERVAL_MS) {
            return _gateState.value
        }
        lastCheckTimestampMs = now

        val remoteMeta = fetchRemoteMetadataWithFallback()
        if (remoteMeta != null && remoteMeta.latestVersionCode > 0) {
            val remoteEffectiveMin = if (remoteMeta.forceUpdate) {
                maxOf(remoteMeta.minSupportedVersionCode, remoteMeta.latestVersionCode)
            } else {
                remoteMeta.minSupportedVersionCode
            }
            val effectiveMin = maxOf(remoteEffectiveMin, cachedPrefs.cachedMinSupportedVersionCode)
            val resolvedApkUrl = remoteMeta.apkUrl.ifBlank { cachedPrefs.cachedLatestApkUrl }.ifBlank { DEFAULT_RELEASES_URL }
            val resolvedVersionName = remoteMeta.latestVersionName.ifBlank { cachedPrefs.cachedLatestVersionName }
            val resolvedSha256 = remoteMeta.apkSha256.trim().lowercase()
            val resolvedChangelog = remoteMeta.changelogRu.ifBlank { cachedPrefs.cachedChangelogRu }

            userPrefs.saveCachedUpdateMetadata(
                minSupportedVersionCode = effectiveMin,
                latestVersionCode = maxOf(remoteMeta.latestVersionCode, cachedPrefs.cachedLatestVersionCode),
                latestVersionName = resolvedVersionName,
                latestApkUrl = resolvedApkUrl,
                apkSha256 = resolvedSha256,
                changelogRu = resolvedChangelog
            )

            val updatedPrefs = cachedPrefs.copy(
                cachedMinSupportedVersionCode = effectiveMin,
                cachedLatestVersionCode = maxOf(remoteMeta.latestVersionCode, cachedPrefs.cachedLatestVersionCode),
                cachedLatestVersionName = resolvedVersionName,
                cachedLatestApkUrl = resolvedApkUrl,
                cachedApkSha256 = resolvedSha256,
                cachedChangelogRu = resolvedChangelog
            )
            val nextState = evaluateGateState(
                currentVersionCode = currentVersionCode,
                remoteMeta = remoteMeta,
                cachedPrefs = updatedPrefs,
                nowMs = now
            )
            _gateState.value = nextState
            return nextState
        }

        _gateState.value = cachedState
        return cachedState
    }

    private suspend fun fetchRemoteMetadataWithFallback(): AppVersionMetadataDto? {
        try {
            val primary = apiClient.fetchAppVersionFromBackend()
            if (primary.latestVersionCode > 0 || primary.minSupportedVersionCode > 0) {
                return primary
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            // Render cold start, timeout, or offline -> fall back to static OTA manifest
        }

        if (fallbackManifestUrl.isBlank()) return null
        return try {
            val fallback = apiClient.fetchAppVersionFromFallback(fallbackManifestUrl)
            fallback.takeIf { it.latestVersionCode > 0 || it.minSupportedVersionCode > 0 }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            null
        }
    }

    suspend fun snoozeOptionalFor24Hours() {
        val until = clockMs() + OPTIONAL_SNOOZE_DURATION_MS
        userPrefs.setOptionalUpdateSnoozedUntil(until)
        if (_gateState.value is UpdateGateState.Optional) {
            _gateState.value = UpdateGateState.UpToDate
        }
    }

    fun startDownloadAndInstall(
        activityContext: Context,
        apkUrl: String,
        expectedSha256: String
    ) {
        if (activeDownloadJob?.isActive == true) return
        val currentDl = _downloadState.value
        if (currentDl is ApkDownloadState.ReadyToInstall && currentDl.apkFile.exists()) {
            triggerInstallApk(activityContext, currentDl.apkFile)
            return
        }
        if (currentDl is ApkDownloadState.NeedsInstallPermission && currentDl.apkFile.exists()) {
            triggerInstallApk(activityContext, currentDl.apkFile)
            return
        }

        activeDownloadJob = appScope.launch {
            val result = downloadAndVerifyApk(apkUrl = apkUrl, expectedSha256 = expectedSha256)
            result.onSuccess { apkFile ->
                withContext(Dispatchers.Main) {
                    triggerInstallApk(activityContext, apkFile)
                }
            }
        }
    }

    suspend fun downloadAndVerifyApk(
        apkUrl: String,
        expectedSha256: String
    ): Result<File> = withContext(Dispatchers.IO) {
        val cleanUrl = apkUrl.trim()
        if (cleanUrl.isBlank() || (!cleanUrl.startsWith("https://") && !cleanUrl.startsWith("http://"))) {
            val msg = "Некорректная ссылка на APK-обновление."
            _downloadState.value = ApkDownloadState.Error(msg)
            return@withContext Result.failure(IllegalArgumentException(msg))
        }

        val updatesDir = File(context.cacheDir, APK_CACHE_SUBDIR).apply { mkdirs() }
        val partFile = File(updatesDir, "$APK_FILE_NAME.part")
        val finalFile = File(updatesDir, APK_FILE_NAME)
        partFile.delete()

        try {
            _downloadState.value = ApkDownloadState.Downloading(0L, -1L, 0f)
            apiClient.downloadApkToFile(cleanUrl, partFile) { downloaded, total ->
                val fraction = if (total > 0L) {
                    (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                _downloadState.value = ApkDownloadState.Downloading(
                    downloadedBytes = downloaded,
                    totalBytes = total,
                    progressFraction = fraction
                )
            }

            if (!partFile.exists() || partFile.length() == 0L) {
                partFile.delete()
                val msg = "Загруженный файл обновления пуст или повреждён."
                _downloadState.value = ApkDownloadState.Error(msg)
                return@withContext Result.failure(IllegalStateException(msg))
            }

            val actualSha = computeFileSha256(partFile)
            val expectedClean = expectedSha256.trim().lowercase()
            if (expectedClean.isNotEmpty() && !actualSha.equals(expectedClean, ignoreCase = true)) {
                partFile.delete()
                finalFile.delete()
                val msg = "Ошибка проверки целостности APK (SHA-256 не совпадает). Установка заблокирована."
                _downloadState.value = ApkDownloadState.Error(msg)
                return@withContext Result.failure(SecurityException("SHA-256 mismatch: expected=$expectedClean actual=$actualSha"))
            }

            if (finalFile.exists()) finalFile.delete()
            val moved = partFile.renameTo(finalFile)
            val targetApk = if (moved) finalFile else partFile.copyTo(finalFile, overwrite = true).also { partFile.delete() }

            _downloadState.value = ApkDownloadState.ReadyToInstall(targetApk, actualSha)
            Result.success(targetApk)
        } catch (ce: CancellationException) {
            partFile.delete()
            _downloadState.value = ApkDownloadState.Idle
            throw ce
        } catch (err: Exception) {
            partFile.delete()
            val msg = "Не удалось скачать обновление: ${err.localizedMessage ?: "проверьте соединение с сетью"}"
            _downloadState.value = ApkDownloadState.Error(msg)
            Result.failure(err)
        }
    }

    fun triggerInstallApk(activityContext: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) {
            _downloadState.value = ApkDownloadState.Error("Файл обновления не найден. Скачайте APK повторно.")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
            if (!canInstall) {
                _downloadState.value = ApkDownloadState.NeedsInstallPermission(apkFile)
                openInstallUnknownAppsSettings(activityContext)
                return false
            }
        }

        return try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activityContext.startActivity(installIntent)
            true
        } catch (err: Exception) {
            _downloadState.value = ApkDownloadState.Error(
                "Не удалось запустить установщик Android: ${err.localizedMessage ?: "откройте загрузку в браузере"}"
            )
            false
        }
    }

    fun openInstallUnknownAppsSettings(activityContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activityContext.startActivity(settingsIntent)
            }
        }
    }

    fun onHostResumed(activityContext: Context) {
        val dl = _downloadState.value
        if (dl is ApkDownloadState.NeedsInstallPermission && dl.apkFile.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
            ) {
                _downloadState.value = ApkDownloadState.ReadyToInstall(dl.apkFile, "")
                triggerInstallApk(activityContext, dl.apkFile)
            }
        }
        checkForUpdatesAsync(force = false)
    }

    fun openDownloadPageInBrowser(activityContext: Context, apkUrl: String) {
        val target = apkUrl.trim().takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?: DEFAULT_RELEASES_URL
        runCatching {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activityContext.startActivity(browserIntent)
        }
    }

    companion object {
        const val THROTTLE_INTERVAL_MS = 5L * 60L * 1000L
        const val OPTIONAL_SNOOZE_DURATION_MS = 24L * 60L * 60L * 1000L
        const val APK_CACHE_SUBDIR = "apk_updates"
        const val APK_FILE_NAME = "techenglish-update.apk"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val DEFAULT_RELEASES_URL = "https://github.com/tuychievmt/techenglish/releases/latest"

        fun evaluateGateState(
            currentVersionCode: Int,
            remoteMeta: AppVersionMetadataDto?,
            cachedPrefs: StoredUserPreferences,
            nowMs: Long
        ): UpdateGateState {
            val remoteEffectiveMin = if (remoteMeta != null) {
                if (remoteMeta.forceUpdate) {
                    maxOf(remoteMeta.minSupportedVersionCode, remoteMeta.latestVersionCode)
                } else {
                    remoteMeta.minSupportedVersionCode
                }
            } else {
                0
            }

            val effectiveMinSupported = maxOf(remoteEffectiveMin, cachedPrefs.cachedMinSupportedVersionCode)
            val resolvedLatestCode = maxOf(
                remoteMeta?.latestVersionCode ?: 0,
                cachedPrefs.cachedLatestVersionCode,
                effectiveMinSupported
            )
            val resolvedVersionName = remoteMeta?.latestVersionName?.takeIf { it.isNotBlank() }
                ?: cachedPrefs.cachedLatestVersionName.ifBlank { "новой версии" }
            val resolvedApkUrl = remoteMeta?.apkUrl?.takeIf { it.isNotBlank() }
                ?: cachedPrefs.cachedLatestApkUrl.ifBlank { DEFAULT_RELEASES_URL }
            val resolvedSha256 = remoteMeta?.apkSha256?.trim()?.lowercase()
                ?: cachedPrefs.cachedApkSha256.trim().lowercase()
            val resolvedChangelog = remoteMeta?.changelogRu?.takeIf { it.isNotBlank() }
                ?: cachedPrefs.cachedChangelogRu.ifBlank {
                    "• Критические улучшения стабильности, безопасности и синхронизации"
                }

            if (currentVersionCode < effectiveMinSupported) {
                return UpdateGateState.Required(
                    minSupportedVersionCode = effectiveMinSupported,
                    latestVersionCode = resolvedLatestCode,
                    latestVersionName = resolvedVersionName,
                    apkUrl = resolvedApkUrl,
                    apkSha256 = resolvedSha256,
                    changelogRu = resolvedChangelog
                )
            }

            if (remoteMeta != null && currentVersionCode < remoteMeta.latestVersionCode) {
                val isSnoozed = nowMs < cachedPrefs.optionalUpdateSnoozedUntilMs
                if (!isSnoozed) {
                    return UpdateGateState.Optional(
                        latestVersionCode = remoteMeta.latestVersionCode,
                        latestVersionName = resolvedVersionName,
                        apkUrl = resolvedApkUrl,
                        apkSha256 = resolvedSha256,
                        changelogRu = resolvedChangelog
                    )
                }
            }

            return UpdateGateState.UpToDate
        }

        fun computeFileSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val read = fis.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
