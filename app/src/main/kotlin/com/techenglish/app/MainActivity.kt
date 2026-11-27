package com.techenglish.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.database.DatabaseBootstrapManager
import com.techenglish.app.core.database.DatabaseBootstrapState
import com.techenglish.app.core.designsystem.TechEnglishTheme
import com.techenglish.app.core.navigation.TechEnglishNavShell
import com.techenglish.app.core.navigation.resolveStartDestination
import com.techenglish.app.core.notifications.NotificationConstants
import com.techenglish.app.core.notifications.NotificationScheduler
import com.techenglish.app.core.security.OAuthCallbackHandler
import com.techenglish.app.core.sync.CloudSyncEngine
import com.techenglish.app.core.update.AppUpdateGateManager
import com.techenglish.app.core.update.UpdateGateState
import com.techenglish.app.di.ApplicationScope
import com.techenglish.app.feature.update.ForceUpdateScreen
import com.techenglish.app.feature.update.OptionalUpdateDialog
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var cloudSyncEngine: CloudSyncEngine

    @Inject
    lateinit var oauthCallbackHandler: OAuthCallbackHandler

    @Inject
    lateinit var databaseBootstrapManager: DatabaseBootstrapManager

    @Inject
    lateinit var appUpdateGateManager: AppUpdateGateManager

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    internal val pendingDestination = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleDestinationIntentIfPresent(intent)
        handleOAuthDeepLinkIfPresent(intent)
        appUpdateGateManager.checkForUpdatesAsync(force = true)

        // Ensure first-run DB unpack from PAD asset pack runs off the main thread (SP2-05 / R4)
        // in application scope so rotation does not cancel unpacking.
        appScope.launch {
            runCatching { databaseBootstrapManager.ensureDatabaseReady(Dispatchers.IO) }
            val prefs = runCatching { settingsRepository.observeSettings().first() }.getOrNull()
            if (prefs?.notificationsEnabled == true) {
                notificationScheduler.scheduleDailyReminder(prefs.reminderHour, prefs.reminderMinute)
            }
            cloudSyncEngine.scheduleBackgroundSync()
        }

        setContent {
            val loadedSettings by settingsRepository.observeSettings()
                .collectAsStateWithLifecycle(initialValue = null)
            val bootstrapState by databaseBootstrapManager.state.collectAsStateWithLifecycle()
            val updateGateState by appUpdateGateManager.gateState.collectAsStateWithLifecycle()
            val apkDownloadState by appUpdateGateManager.downloadState.collectAsStateWithLifecycle()
            val destination by pendingDestination.collectAsStateWithLifecycle()
            val settings = loadedSettings ?: UserSettings()
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        appUpdateGateManager.onHostResumed(this@MainActivity)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            TechEnglishTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColors,
                hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
                reduceMotionEnabled = settings.reduceMotionEnabled
            ) {
                when (val gate = updateGateState) {
                    is UpdateGateState.Required -> {
                        ForceUpdateScreen(
                            state = gate,
                            downloadState = apkDownloadState,
                            onDownloadAndInstall = {
                                appUpdateGateManager.startDownloadAndInstall(
                                    activityContext = this@MainActivity,
                                    apkUrl = gate.apkUrl,
                                    expectedSha256 = gate.apkSha256
                                )
                            },
                            onOpenBrowser = {
                                appUpdateGateManager.openDownloadPageInBrowser(
                                    activityContext = this@MainActivity,
                                    apkUrl = gate.apkUrl
                                )
                            },
                            onOpenInstallPermissionSettings = {
                                appUpdateGateManager.openInstallUnknownAppsSettings(this@MainActivity)
                            }
                        )
                    }
                    else -> {
                        when (val boot = bootstrapState) {
                            is DatabaseBootstrapState.Idle -> {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 32.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                        Text(
                                            text = "Инициализация офлайн-словаря TechEnglish…",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                            is DatabaseBootstrapState.Unpacking -> {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 32.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Подготовка офлайн-словаря TechEnglish…",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        LinearProgressIndicator(
                                            progress = { boot.progress },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "${(boot.progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            is DatabaseBootstrapState.Error -> {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 32.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Не удалось подготовить офлайн-базу словаря",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = boot.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = {
                                                appScope.launch {
                                                    runCatching { databaseBootstrapManager.ensureDatabaseReady(Dispatchers.IO) }
                                                }
                                            }
                                        ) {
                                            Text("Повторить")
                                        }
                                    }
                                }
                            }
                            is DatabaseBootstrapState.Ready -> {
                                val readySettings = loadedSettings
                                if (readySettings == null) {
                                    Surface(modifier = Modifier.fillMaxSize()) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                } else {
                                    val initialStartDestination by remember {
                                        mutableStateOf(resolveStartDestination(readySettings.isInitialSetupCompleted))
                                    }
                                    TechEnglishNavShell(
                                        startDestination = initialStartDestination,
                                        openFlashcardsOnLaunch = destination == NotificationConstants.DEST_FLASHCARDS,
                                        pendingDestination = destination,
                                        onDestinationConsumed = { pendingDestination.value = null }
                                    )
                                }
                            }
                        }

                        if (gate is UpdateGateState.Optional) {
                            OptionalUpdateDialog(
                                state = gate,
                                downloadState = apkDownloadState,
                                onUpdateClick = {
                                    appUpdateGateManager.startDownloadAndInstall(
                                        activityContext = this@MainActivity,
                                        apkUrl = gate.apkUrl,
                                        expectedSha256 = gate.apkSha256
                                    )
                                },
                                onSnoozeClick = {
                                    appScope.launch {
                                        appUpdateGateManager.snoozeOptionalFor24Hours()
                                    }
                                },
                                onOpenInstallPermissionSettings = {
                                    appUpdateGateManager.openInstallUnknownAppsSettings(this@MainActivity)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDestinationIntentIfPresent(intent)
        handleOAuthDeepLinkIfPresent(intent)
    }

    internal fun handleDestinationIntentIfPresent(intent: Intent?) {
        val dest = extractDestinationFromIntent(intent)
        if (!dest.isNullOrBlank()) {
            pendingDestination.value = dest
            intent?.removeExtra(NotificationConstants.EXTRA_DESTINATION)
        }
    }

    private fun handleOAuthDeepLinkIfPresent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (!oauthCallbackHandler.isOAuthCallbackUri(uri)) return
        // Consume deep link data immediately so configuration changes do not replay the OAuth callback
        intent.data = null
        appScope.launch {
            val res = oauthCallbackHandler.handleCallbackUri(uri)
            if (res.isSuccess) {
                runCatching { databaseBootstrapManager.ensureDatabaseReady(Dispatchers.IO) }
                cloudSyncEngine.scheduleBackgroundSync()
                runCatching { cloudSyncEngine.runFullSyncNow() }
            }
        }
    }

    companion object {
        fun extractDestinationFromIntent(intent: Intent?): String? {
            return intent?.getStringExtra(NotificationConstants.EXTRA_DESTINATION)?.takeIf { it.isNotBlank() }
        }
    }
}
