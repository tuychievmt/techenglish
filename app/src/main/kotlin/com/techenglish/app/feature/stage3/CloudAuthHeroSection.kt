package com.techenglish.app.feature.stage3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.TechColors

/**
 * Onboarding 3/3 Cloud Sync Auth Hero card matching Stitch `3_techenglish` / `3_light_ceramic_techenglish`.
 */
@Composable
fun CloudAuthHeroSection(
    onGoogleClick: () -> Unit,
    onGithubClick: () -> Unit,
    onContinueAsGuest: () -> Unit = {},
    isOnboarding: Boolean = false,
    stepBadgeText: String = "ШАГ 1 / 2",
    guestButtonText: String = "Продолжить офлайн без облака",
    activeStepIndex: Int = 0,
    totalSteps: Int = 2,
    isBusy: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isOnboarding) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = stepBadgeText,
                        style = MonoTagTextStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 84.dp Squircle Glass Emblem </> with Aura
        val auraColor = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            auraColor.copy(alpha = 0.28f),
                            Color.Transparent
                        )
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "TECHENGLISH · CLOUD SYNC",
                style = MonoTagTextStyle,
                color = if (isDark) TechColors.Cyan else Color(0xFF4338CA)
            )
            Text(
                text = "Синхронизируй прогресс",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Сохраняй колоды слов, серию SM-2 и статистику между устройствами",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // OAuth 2.0 + Offline-First БД Trust Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = if (isDark) TechColors.Emerald else TechColors.EmeraldDark,
                        modifier = Modifier.size(15.dp)
                    )
                    Text("OAuth 2.0", style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text("Offline-First БД", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Auth CTAs
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onGoogleClick,
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFFF1F5F9) else Color.White,
                    contentColor = Color(0xFF0F172A)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                GoogleLogoIcon(modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Войти через Google", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = onGithubClick,
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else Color(0xFF0F172A),
                    contentColor = Color(0xFFF8FAFC)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                GitHubOctocatIcon(tint = Color(0xFFF8FAFC), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Войти через GitHub", style = MaterialTheme.typography.labelLarge)
            }

            if (isOnboarding) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(onClick = onContinueAsGuest, enabled = !isBusy) {
                        Text(
                            text = guestButtonText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Облачную синхронизацию можно включить позже в профиле",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom step indicator (only in real onboarding)
        if (isOnboarding) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps.coerceAtLeast(1)) { idx ->
                    if (idx == activeStepIndex) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            if (isDark) TechColors.Cyan else MaterialTheme.colorScheme.primary,
                                            if (isDark) TechColors.Indigo else Color(0xFF4338CA)
                                        )
                                    )
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    val bluePath = remember {
        PathParser().parsePathString(
            "M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
        ).toPath()
    }
    val greenPath = remember {
        PathParser().parsePathString(
            "M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
        ).toPath()
    }
    val yellowPath = remember {
        PathParser().parsePathString(
            "M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
        ).toPath()
    }
    val redPath = remember {
        PathParser().parsePathString(
            "M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
        ).toPath()
    }

    Canvas(modifier = modifier) {
        withTransform({ scale(size.width / 24f, size.height / 24f, Offset.Zero) }) {
            drawPath(bluePath, Color(0xFF4285F4))
            drawPath(greenPath, Color(0xFF34A853))
            drawPath(yellowPath, Color(0xFFFBBC05))
            drawPath(redPath, Color(0xFFEA4335))
        }
    }
}

@Composable
private fun GitHubOctocatIcon(tint: Color, modifier: Modifier = Modifier) {
    val octocatPath = remember {
        PathParser().parsePathString(
            "M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
        ).toPath().apply { fillType = PathFillType.EvenOdd }
    }

    Canvas(modifier = modifier) {
        withTransform({ scale(size.width / 24f, size.height / 24f, Offset.Zero) }) {
            drawPath(octocatPath, tint)
        }
    }
}

@Composable
fun CodeSlashEmblem(
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val brush = Brush.linearGradient(listOf(startColor, endColor))
        val stroke = 2.6.dp.toPx()
        val w = size.width
        val h = size.height
        // Left '<'
        drawLine(brush, Offset(w * 0.34f, h * 0.25f), Offset(w * 0.12f, h * 0.50f), stroke, StrokeCap.Round)
        drawLine(brush, Offset(w * 0.12f, h * 0.50f), Offset(w * 0.34f, h * 0.75f), stroke, StrokeCap.Round)
        // Right '>'
        drawLine(brush, Offset(w * 0.66f, h * 0.25f), Offset(w * 0.88f, h * 0.50f), stroke, StrokeCap.Round)
        drawLine(brush, Offset(w * 0.88f, h * 0.50f), Offset(w * 0.66f, h * 0.75f), stroke, StrokeCap.Round)
        // Center '/'
        drawLine(brush, Offset(w * 0.58f, h * 0.18f), Offset(w * 0.42f, h * 0.82f), stroke, StrokeCap.Round)
    }
}
