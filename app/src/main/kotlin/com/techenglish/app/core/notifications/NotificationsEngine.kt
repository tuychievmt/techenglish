package com.techenglish.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.techenglish.app.MainActivity
import com.techenglish.app.R
import com.techenglish.domain.repository.AcademicVocabularyRepository
import com.techenglish.domain.repository.VocabularyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

object NotificationConstants {
    const val CHANNEL_ID = "techenglish_srs_reminders"
    const val CHANNEL_SOCIAL_ID = "techenglish_social_pings"
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_ID_PUSH = 1002
    const val UNIQUE_WORK_NAME = "techenglish_daily_srs_reminder"
    const val EXTRA_DESTINATION = "open_destination"
    const val DEST_FLASHCARDS = "flashcards"
}

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val srsChannel = NotificationChannel(
            NotificationConstants.CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        val socialChannel = NotificationChannel(
            NotificationConstants.CHANNEL_SOCIAL_ID,
            "Социальные уведомления и пинги",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Пинги от друзей и уведомления сообщества TechEnglish"
        }
        manager.createNotificationChannel(srsChannel)
        manager.createNotificationChannel(socialChannel)
    }

    fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun showDueWordsNotification(dueCount: Int): Boolean {
        if (dueCount <= 0 || !hasNotificationPermission()) return false
        ensureChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationConstants.EXTRA_DESTINATION, NotificationConstants.DEST_FLASHCARDS)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Пора повторить IT-термины")
            .setContentText("К повторению по SM-2 готово карточек: $dueCount. Займёт пару минут!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(
                NotificationConstants.NOTIFICATION_ID,
                notification
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showPushNotification(
        title: String,
        body: String,
        destination: String? = null
    ): Boolean {
        if (title.isBlank() && body.isBlank()) return false
        if (!hasNotificationPermission()) return false
        ensureChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!destination.isNullOrBlank()) {
                putExtra(NotificationConstants.EXTRA_DESTINATION, destination)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NotificationConstants.NOTIFICATION_ID_PUSH,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_SOCIAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title.ifBlank { "TechEnglish" })
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(
                NotificationConstants.NOTIFICATION_ID_PUSH,
                notification
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

@HiltWorker
class ReviewReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val vocabularyRepository: VocabularyRepository,
    private val academicVocabularyRepository: AcademicVocabularyRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val coreDue = vocabularyRepository.getDueCount(now)
            val academicDue = runCatching { academicVocabularyRepository.getDueSrsCount(now) }.getOrDefault(0)
            val totalDue = coreDue + academicDue
            if (totalDue > 0) {
                notificationHelper.showDueWordsNotification(totalDue)
            }
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationHelper: NotificationHelper
) {
    fun scheduleDailyReminder(
        hour: Int,
        minute: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        notificationHelper.ensureChannel()
        val now = ZonedDateTime.now(zoneId)
        val initialDelayMinutes = computeInitialDelayMinutes(now, hour, minute)

        val request = PeriodicWorkRequestBuilder<ReviewReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NotificationConstants.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(NotificationConstants.UNIQUE_WORK_NAME)
    }

    companion object {
        fun computeInitialDelayMinutes(
            now: ZonedDateTime,
            hour: Int,
            minute: Int
        ): Long {
            var nextTrigger = now
                .withHour(hour.coerceIn(0, 23))
                .withMinute(minute.coerceIn(0, 59))
                .withSecond(0)
                .withNano(0)
            if (!nextTrigger.isAfter(now)) {
                nextTrigger = nextTrigger.plusDays(1)
            }
            return Duration.between(now, nextTrigger).toMinutes().coerceAtLeast(1L)
        }
    }
}
