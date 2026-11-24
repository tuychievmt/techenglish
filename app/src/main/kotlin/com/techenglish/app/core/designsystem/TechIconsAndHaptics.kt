package com.techenglish.app.core.designsystem

import androidx.compose.runtime.getValue
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

fun categoryIconVector(iconName: String): ImageVector = when (iconName.lowercase()) {
    "dns" -> Icons.Rounded.Dns
    "web" -> Icons.Rounded.Web
    "merge_type" -> Icons.AutoMirrored.Rounded.MergeType
    "account_tree" -> Icons.Rounded.AccountTree
    "groups" -> Icons.Rounded.Groups
    "cloud_sync" -> Icons.Rounded.CloudSync
    "storage" -> Icons.Rounded.Storage
    "code" -> Icons.Rounded.Code
    "lan" -> Icons.Rounded.Lan
    "shield" -> Icons.Rounded.Shield
    "bookmark_added" -> Icons.Rounded.BookmarkAdded
    else -> Icons.Rounded.Code
}

fun parseHexColor(hex: String, fallback: Color = TechColors.Indigo): Color = try {
    val clean = hex.removePrefix("#")
    val value = clean.toLong(16)
    if (clean.length == 6) Color(0xFF000000 or value) else Color(value)
} catch (_: Throwable) {
    fallback
}

/**
 * Ensures a dynamic language accent color meets WCAG AA contrast (>= 4.5:1) against [background].
 */
fun ensureAccessibleAccent(rawColor: Color, background: Color): Color {
    if (calculateContrastRatio(rawColor, background) >= 4.5) return rawColor
    val target = if (background.relativeLuminance() < 0.5) Color.White else Color.Black
    for (step in 1..20) {
        val candidate = lerp(rawColor, target, step / 20f)
        if (calculateContrastRatio(candidate, background) >= 4.55) {
            return candidate
        }
    }
    return target
}

// ==================== HAPTIC FEEDBACK (УИ-3.1) ====================

class TechHapticsController(
    private val enabled: Boolean,
    private val performViewHaptic: (Int) -> Unit
) {
    fun onCardFlip() {
        if (enabled) performViewHaptic(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun onSwipe() {
        if (enabled) performViewHaptic(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun onCorrectAnswer() {
        if (enabled) performViewHaptic(HapticFeedbackConstants.CONFIRM)
    }

    fun onWordTap() {
        if (enabled) performViewHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

@Composable
fun rememberTechHaptics(): TechHapticsController {
    val config = LocalTechInteractionConfig.current
    val view = LocalView.current
    return remember(config.hapticFeedbackEnabled, view) {
        TechHapticsController(
            enabled = config.hapticFeedbackEnabled,
            performViewHaptic = { constant ->
                runCatching { view.performHapticFeedback(constant) }
            }
        )
    }
}

// ==================== SKELETON & DEVELOPER LOADING STATUSES (УИ-3.3, НФТ-7.5) ====================

private val DeveloperLoadingStatuses = listOf(
    "Compiling vocabulary...",
    "Resolving dependencies...",
    "Linking AST symbols..."
)

@Composable
fun SkeletonListPlaceholder(
    itemCount: Int = 5,
    statusMessage: String = DeveloperLoadingStatuses[0],
    modifier: Modifier = Modifier
) {
    val interactionConfig = LocalTechInteractionConfig.current
    val alpha = if (interactionConfig.reduceMotionEnabled) {
        0.45f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val animated by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeleton_alpha"
        )
        animated
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics { contentDescription = statusMessage },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = statusMessage,
                style = MonoTagTextStyle,
                color = MaterialTheme.colorScheme.primary
            )
        }
        repeat(itemCount) { idx ->
            val subStatus = DeveloperLoadingStatuses[idx % DeveloperLoadingStatuses.size]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                    .padding(12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                if (idx == 0) {
                    Text(
                        text = subStatus,
                        style = MonoTagTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==================== TERMINAL SEARCH FIELD WITH BLINKING CURSOR '_' (УИ-3.3) ====================

@Composable
fun TerminalSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val interactionConfig = LocalTechInteractionConfig.current
    val cursorAlpha = if (interactionConfig.reduceMotionEnabled) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "terminal_cursor")
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 550),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cursor_blink"
        )
        animated
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        leadingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Поиск",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "_",
                    style = CodeTextStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha)
                )
            }
        },
        placeholder = {
            Text(
                text = "$placeholderText _",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onClear?.invoke() ?: onValueChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Очистить поиск")
                }
            }
        }
    )
}

