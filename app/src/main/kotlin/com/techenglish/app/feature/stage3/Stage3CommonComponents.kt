package com.techenglish.app.feature.stage3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.techenglish.app.core.designsystem.TechColors

// =========================================================================
// Shared Stage 3 UI Components
// =========================================================================

@Composable
fun OfflineModeBanner(message: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = TechColors.Amber.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, TechColors.AmberDark.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = TechColors.AmberDark)
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun PresetAvatarBadge(avatarKey: String, sizeDp: Int) {
    val (bg, icon) = when (avatarKey) {
        "preset_kotlin" -> TechColors.Indigo to Icons.Rounded.Code
        "preset_python" -> TechColors.Sky to Icons.Rounded.Terminal
        "preset_rust" -> TechColors.AmberDark to Icons.Rounded.Shield
        "preset_linux" -> TechColors.EmeraldDark to Icons.Rounded.Badge
        "preset_architect" -> TechColors.Violet to Icons.Rounded.EmojiEvents
        else -> MaterialTheme.colorScheme.primary to Icons.Rounded.Terminal
    }
    Surface(
        shape = CircleShape,
        color = bg.copy(alpha = 0.18f),
        modifier = Modifier.size(sizeDp.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = bg,
                modifier = Modifier.size((sizeDp * 0.56f).dp)
            )
        }
    }
}

internal fun badgeIconFor(iconKey: String): ImageVector = when (iconKey) {
    "flame" -> Icons.Rounded.LocalFireDepartment
    "bolt" -> Icons.Rounded.Bolt
    "bug" -> Icons.Rounded.BugReport
    "code" -> Icons.Rounded.Code
    "terminal" -> Icons.Rounded.Terminal
    else -> Icons.Rounded.Star
}

@Composable
internal fun DeterministicQrMatrix(seed: String, sizeDp: Int) {
    val hash = remember(seed) {
        seed.fold(0x811c9dc5.toInt()) { acc, ch -> (acc xor ch.code) * 0x01000193 }
    }
    val fgColor = MaterialTheme.colorScheme.onSurface
    val bgColor = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(4.dp)
    ) {
        val cells = 7
        val cellSize = size.width / cells
        for (r in 0 until cells) {
            for (c in 0 until cells) {
                val isFinder = (r < 2 && c < 2) || (r < 2 && c >= cells - 2) || (r >= cells - 2 && c < 2)
                val bit = ((hash ushr ((r * cells + c) % 31)) and 1) == 1
                if (isFinder || bit) {
                    drawRect(
                        color = fgColor,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}
