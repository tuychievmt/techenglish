package com.techenglish.app.feature.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.navigationBarContentPadding
import com.techenglish.app.core.update.ApkDownloadState
import com.techenglish.app.core.update.UpdateGateState
import com.techenglish.app.feature.stage3.CodeSlashEmblem
import java.util.Locale

@Composable
fun ForceUpdateScreen(
    state: UpdateGateState.Required,
    downloadState: ApkDownloadState,
    onDownloadAndInstall: () -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenInstallPermissionSettings: () -> Unit = {}
) {
    // Hard WhatsApp-style block: Back button cannot dismiss the screen
    BackHandler(enabled = true) { }

    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val cleanVersion = state.latestVersionName.trim().removePrefix("v").removePrefix("V").ifBlank { "latest" }
    val auraColor = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary
    val isDownloading = downloadState is ApkDownloadState.Downloading

    AuroraScreenContainer {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding(),
                contentPadding = navigationBarContentPadding(horizontal = 20.dp, top = 28.dp, extraBottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Squircle TechEnglish Emblem
                        Box(
                            modifier = Modifier.size(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(auraColor.copy(alpha = 0.28f), Color.Transparent)
                                    )
                                )
                            }
                            Surface(
                                modifier = Modifier.size(84.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                                    else MaterialTheme.colorScheme.outline
                                ),
                                shadowElevation = if (isDark) 0.dp else 8.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CodeSlashEmblem(
                                        startColor = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                                        endColor = if (isDark) TechColors.Violet else Color(0xFF4338CA),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        // Badge: ТРЕБУЕТСЯ ОБНОВЛЕНИЕ
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdateAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "ТРЕБУЕТСЯ ОБНОВЛЕНИЕ",
                                    style = MonoTagTextStyle,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = "Версия устарела",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Вышла новая версия TechEnglish v$cleanVersion. Обновите приложение, чтобы продолжить работу",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bento Card: Что нового
                if (state.changelogRu.isNotBlank()) {
                    item {
                        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Что нового в v$cleanVersion",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = state.changelogRu,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Download progress / verification / error state
                item {
                    UpdateDownloadProgressSection(
                        downloadState = downloadState,
                        onOpenInstallPermissionSettings = onOpenInstallPermissionSettings
                    )
                }

                // Primary & Fallback CTAs
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val ctaLabel = when (downloadState) {
                            is ApkDownloadState.Downloading -> "Загрузка APK…"
                            is ApkDownloadState.ReadyToInstall -> "Установить скачанный APK →"
                            is ApkDownloadState.NeedsInstallPermission -> "Разрешить установку APK →"
                            is ApkDownloadState.Error -> "Повторить скачивание →"
                            ApkDownloadState.Idle -> "Скачать и установить →"
                        }

                        Button(
                            onClick = {
                                if (downloadState is ApkDownloadState.NeedsInstallPermission) {
                                    onOpenInstallPermissionSettings()
                                } else {
                                    onDownloadAndInstall()
                                }
                            },
                            enabled = !isDownloading,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.primary,
                                contentColor = if (isDark) Color(0xFF06080F) else MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = ctaLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        TextButton(onClick = onOpenBrowser) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Открыть страницу загрузки в браузере",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun UpdateDownloadProgressSection(
    downloadState: ApkDownloadState,
    onOpenInstallPermissionSettings: () -> Unit
) {
    when (downloadState) {
        ApkDownloadState.Idle -> Unit
        is ApkDownloadState.Downloading -> {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val hasTotal = downloadState.totalBytes > 0L
                    val pctText = if (hasTotal) {
                        "${(downloadState.progressFraction * 100f).toInt()}%"
                    } else {
                        "Загрузка…"
                    }
                    val sizeText = if (hasTotal) {
                        "${formatMegabytes(downloadState.downloadedBytes)} / ${formatMegabytes(downloadState.totalBytes)} МБ"
                    } else {
                        "${formatMegabytes(downloadState.downloadedBytes)} МБ"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sizeText,
                            style = MonoTagTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = pctText,
                            style = MonoTagTextStyle,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (hasTotal) {
                        LinearProgressIndicator(
                            progress = { downloadState.progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }
        is ApkDownloadState.ReadyToInstall -> {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = TechColors.Emerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "APK загружен и проверен по SHA-256. Подтвердите установку в системном окне.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        is ApkDownloadState.NeedsInstallPermission -> {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = TechColors.Amber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Разрешите установку обновлений из TechEnglish в настройках Android, затем вернитесь назад.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    TextButton(onClick = onOpenInstallPermissionSettings) {
                        Text("Открыть настройки разрешений")
                    }
                }
            }
        }
        is ApkDownloadState.Error -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = downloadState.messageRu,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun OptionalUpdateDialog(
    state: UpdateGateState.Optional,
    downloadState: ApkDownloadState,
    onUpdateClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onOpenInstallPermissionSettings: () -> Unit = {}
) {
    val cleanVersion = state.latestVersionName.trim().removePrefix("v").removePrefix("V").ifBlank { "latest" }
    val isDownloading = downloadState is ApkDownloadState.Downloading

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) onSnoozeClick()
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.SystemUpdateAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Доступна TechEnglish v$cleanVersion",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.changelogRu.isNotBlank()) {
                    Text(
                        text = state.changelogRu,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Доступна свежая сборка приложения с улучшениями скорости и стабильности.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                UpdateDownloadProgressSection(
                    downloadState = downloadState,
                    onOpenInstallPermissionSettings = onOpenInstallPermissionSettings
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (downloadState is ApkDownloadState.NeedsInstallPermission) {
                        onOpenInstallPermissionSettings()
                    } else {
                        onUpdateClick()
                    }
                },
                enabled = !isDownloading
            ) {
                Text(if (isDownloading) "Загрузка…" else "Обновить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSnoozeClick,
                enabled = !isDownloading
            ) {
                Text("Позже")
            }
        }
    )
}

internal fun formatMegabytes(bytes: Long): String =
    String.format(Locale.US, "%.1f", bytes.coerceAtLeast(0L).toDouble() / (1024.0 * 1024.0))
