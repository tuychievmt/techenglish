package com.techenglish.app.feature.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.techenglish.app.MainActivity
import com.techenglish.app.core.designsystem.resolveColorSchemeForMode
import com.techenglish.app.core.notifications.NotificationConstants
import com.techenglish.domain.model.ThemeMode
import com.techenglish.domain.model.TranslationLanguage
import com.techenglish.domain.model.WidgetSnapshot
import com.techenglish.domain.repository.SettingsRepository
import com.techenglish.domain.usecase.BuildWidgetSnapshotUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun buildWidgetSnapshotUseCase(): BuildWidgetSnapshotUseCase
    fun settingsRepository(): SettingsRepository
}

data class WidgetPalette(
    val surfaceBg: Color,
    val accentColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val badgeBg: Color,
    val streakColor: Color
)

/**
 * Resolves the Glance widget color palette dynamically from the user's active app theme
 * stored in DataStore (`SettingsRepository`), eliminating hardcoded theme colors (SP2-07 / BUG-24).
 */
fun resolveWidgetPalette(themeMode: ThemeMode, systemDark: Boolean): WidgetPalette {
    val scheme = resolveColorSchemeForMode(themeMode = themeMode, systemDark = systemDark)
    return WidgetPalette(
        surfaceBg = scheme.surface,
        accentColor = scheme.primary,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        badgeBg = scheme.surfaceVariant,
        streakColor = scheme.tertiary
    )
}

class RefreshWidgetWordAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        TechEnglishGlanceWidget().update(context, glanceId)
    }
}

class TechEnglishGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val (snapshot, activeThemeMode) = withContext(Dispatchers.IO) {
            runCatching {
                val ep = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java
                )
                val settings = ep.settingsRepository().observeSettings().first()
                val snap = ep.buildWidgetSnapshotUseCase()()
                snap to settings.themeMode
            }.getOrElse {
                WidgetSnapshot(
                    wordOfTheDayTerm = "idempotency",
                    wordOfTheDayIpa = "/ˌaɪ.demˈpoʊ.tən.si/",
                    wordOfTheDayTranslation = "идемпотентность",
                    dueCardsCount = 0,
                    streakDays = 0,
                    language = TranslationLanguage.RU
                ) to ThemeMode.SYSTEM
            }
        }

        val palette = resolveWidgetPalette(themeMode = activeThemeMode, systemDark = systemDark)

        val launchFlashcardsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(NotificationConstants.EXTRA_DESTINATION, NotificationConstants.DEST_FLASHCARDS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    snapshot = snapshot,
                    palette = palette,
                    launchIntent = launchFlashcardsIntent
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        snapshot: WidgetSnapshot,
        palette: WidgetPalette,
        launchIntent: Intent
    ) {
        val surfaceBg = ColorProvider(palette.surfaceBg)
        val accentColor = ColorProvider(palette.accentColor)
        val textPrimary = ColorProvider(palette.textPrimary)
        val textSecondary = ColorProvider(palette.textSecondary)
        val badgeBg = ColorProvider(palette.badgeBg)
        val streakColor = ColorProvider(palette.streakColor)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceBg)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(launchIntent))
                .padding(14.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Слово дня · ${snapshot.language.shortLabel}",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "🔥 ${snapshot.streakDays} дн.",
                    style = TextStyle(
                        color = streakColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "↻",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier
                        .background(badgeBg)
                        .cornerRadius(8.dp)
                        .clickable(actionRunCallback<RefreshWidgetWordAction>())
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = snapshot.wordOfTheDayTerm,
                style = TextStyle(
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            if (snapshot.wordOfTheDayIpa.isNotBlank()) {
                Text(
                    text = snapshot.wordOfTheDayIpa,
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }

            Text(
                text = snapshot.wordOfTheDayTranslation,
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 13.sp
                ),
                maxLines = 2
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(badgeBg)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "К повторению: ${snapshot.dueCardsCount}",
                    style = TextStyle(
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "Повторить →",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class TechEnglishGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TechEnglishGlanceWidget()
}
