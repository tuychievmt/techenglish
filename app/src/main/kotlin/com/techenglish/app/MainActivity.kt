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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.techenglish.app.core.database.DatabaseBootstrapManager
import com.techenglish.app.core.database.DatabaseBootstrapState
import com.techenglish.app.core.designsystem.TechEnglishTheme
import com.techenglish.app.core.navigation.TechEnglishNavShell
import com.techenglish.app.core.notifications.NotificationConstants
import com.techenglish.app.core.notifications.NotificationScheduler
import com.techenglish.app.core.security.OAuthCallbackHandler
import com.techenglish.app.core.sync.CloudSyncEngine
import com.techenglish.app.feature.stage3.AgeGate16PlusScreen
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
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

    internal val pendingDestination = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleDestinationIntentIfPresent(intent)
        handleOAuthDeepLinkIfPresent(intent)

        // Ensure first-run DB unpack from PAD asset pack runs off the main thread (SP2-05 / R4),
        // then ensure daily SRS reminder & WorkManager outbox sync match persisted settings.
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { databaseBootstrapManager.ensureDatabaseReady(Dispatchers.IO) }
            val prefs = settingsRepository.observeSettings().first()
            if (prefs.notificationsEnabled) {
                notificationScheduler.scheduleDailyReminder(prefs.reminderHour, prefs.reminderMinute)
            }
            cloudSyncEngine.scheduleBackgroundSync()
        }

        setContent {
            val loadedSettings by settingsRepository.observeSettings()
                .collectAsStateWithLifecycle(initialValue = null)
            val bootstrapState by databaseBootstrapManager.state.collectAsStateWithLifecycle()
            val destination by pendingDestination.collectAsStateWithLifecycle()
            val settings = loadedSettings ?: UserSettings()

            TechEnglishTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColors,
                hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
                reduceMotionEnabled = settings.reduceMotionEnabled
            ) {
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
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            databaseBootstrapManager.ensureDatabaseReady()
                                        }
                                    }
                                ) {
                                    Text("Повторить")
                                }
                            }
                        }
                    }
                    is DatabaseBootstrapState.Ready -> {
                        if (loadedSettings != null && !loadedSettings!!.ageVerified16Plus) {
                            AgeGate16PlusScreen(
                                onConfirm16Plus = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        settingsRepository.setAgeVerified16Plus(true)
                                    }
                                }
                            )
                        } else {
                            TechEnglishNavShell(
                                openFlashcardsOnLaunch = destination == NotificationConstants.DEST_FLASHCARDS,
                                pendingDestination = destination,
                                onDestinationConsumed = { pendingDestination.value = null }
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
        }
    }

    private fun handleOAuthDeepLinkIfPresent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (!oauthCallbackHandler.isOAuthCallbackUri(uri)) return
        lifecycleScope.launch(Dispatchers.IO) {
            val res = oauthCallbackHandler.handleCallbackUri(uri)
            if (res.isSuccess) {
                cloudSyncEngine.scheduleBackgroundSync()
                cloudSyncEngine.runFullSyncNow()
            }
        }
    }

    companion object {
        fun extractDestinationFromIntent(intent: Intent?): String? {
            return intent?.getStringExtra(NotificationConstants.EXTRA_DESTINATION)?.takeIf { it.isNotBlank() }
        }
    }
}
