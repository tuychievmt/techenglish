package com.techenglish.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techenglish.app.core.audio.TechAudioController
import com.techenglish.app.core.audio.TtsVoiceStatus
import com.techenglish.app.core.notifications.NotificationHelper
import com.techenglish.app.core.notifications.NotificationScheduler
import com.techenglish.domain.model.DailyProgress
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.UserSettings
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.repository.VocabularyRepository
import com.techenglish.domain.usecase.GetDailyProgressUseCase
import com.techenglish.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val settings: UserSettings = UserSettings(),
    val dailyProgress: DailyProgress? = null,
    val hasNotificationPermission: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    getDailyProgress: GetDailyProgressUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val vocabularyRepository: VocabularyRepository,
    private val notificationScheduler: NotificationScheduler,
    private val notificationHelper: NotificationHelper,
    val audioController: TechAudioController
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val voiceStatus: StateFlow<TtsVoiceStatus> = audioController.voiceStatus

    val uiState: StateFlow<ProfileUiState> = combine(
        settingsRepository.observeSettings(),
        getDailyProgress()
    ) { settings, daily ->
        ProfileUiState(
            settings = settings,
            dailyProgress = daily,
            hasNotificationPermission = notificationHelper.hasNotificationPermission()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ProfileUiState()
    )

    init {
        audioController.ensureInitialized()
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            updateSettings.setUserName(name)
            _events.emit("Имя профиля обновлено")
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            updateSettings.setDailyGoal(goal)
            _events.emit("Ежедневная цель: $goal карточек")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { updateSettings.setThemeMode(mode) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { updateSettings.setDynamicColors(enabled) }
    }

    fun setReadingFontSize(sizeSp: Float) {
        viewModelScope.launch { updateSettings.setReadingFontSize(sizeSp) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateSettings.setHapticFeedbackEnabled(enabled)
            _events.emit(if (enabled) "Тактильный отклик включён" else "Тактильный отклик отключён")
        }
    }

    fun setReduceMotionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateSettings.setReduceMotionEnabled(enabled)
            _events.emit(if (enabled) "Анимации отключены (Reduce Motion)" else "Анимации включены")
        }
    }

    fun setTranslationLanguage(language: TranslationLanguage) {
        viewModelScope.launch {
            updateSettings.setTranslationLanguage(language)
            _events.emit("Язык перевода терминов: ${language.labelRu}")
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateSettings.setNotificationsEnabled(enabled)
            val s = uiState.value.settings
            if (enabled) {
                notificationScheduler.scheduleDailyReminder(s.reminderHour, s.reminderMinute)
                _events.emit("Напоминания включены (${"%02d:%02d".format(s.reminderHour, s.reminderMinute)})")
            } else {
                notificationScheduler.cancelReminders()
                _events.emit("Напоминания отключены")
            }
        }
    }

    fun setReminderHour(hour: Int) {
        viewModelScope.launch {
            updateSettings.setReminderTime(hour, 0)
            if (uiState.value.settings.notificationsEnabled) {
                notificationScheduler.scheduleDailyReminder(hour, 0)
            }
            _events.emit("Время напоминания: ${"%02d:00".format(hour)}")
        }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            val due = vocabularyRepository.getDueCount(System.currentTimeMillis()).coerceAtLeast(1)
            val sent = notificationHelper.showDueWordsNotification(due)
            if (sent) {
                _events.emit("Тестовое уведомление отправлено (к повторению: $due)")
            } else {
                _events.emit("Разрешите отправку уведомлений для Android 13+")
            }
        }
    }
}
