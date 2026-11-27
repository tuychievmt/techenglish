package com.techenglish.app.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Root atmospheric canvas supporting both Obsidian Aurora (Dark / IDE themes)
 * and Ceramic Glass (Light theme) without infinite animations.
 */
@Composable
fun AuroraScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val bg = MaterialTheme.colorScheme.background
    val primaryGlow = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val cyanGlow = if (isDark) TechColors.Cyan.copy(alpha = 0.15f)
    else TechColors.Cyan.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryGlow, Color.Transparent),
                    center = Offset(size.width * 0.22f, size.height * 0.04f),
                    radius = size.width * 0.85f
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.22f, size.height * 0.04f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(cyanGlow, Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.08f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.85f, size.height * 0.08f)
            )
        }
        content()
    }
}

/**
 * Tonal Bento glass surface with crisp hairline border and subtle top highlight.
 */
@Composable
fun BentoGlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    accentWashColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val cardModifier = if (onClick != null) {
        modifier.clip(shape).clickable(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = cardModifier,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isDark) 0.dp else 1.dp,
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Box {
            if (accentWashColor != null) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentWashColor.copy(alpha = if (isDark) 0.14f else 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.92f, size.height * 0.08f),
                            radius = size.width * 0.65f
                        ),
                        radius = size.width * 0.65f,
                        center = Offset(size.width * 0.92f, size.height * 0.08f)
                    )
                }
            }
            Column(content = content)
        }
    }
}

/**
 * Custom Canvas circular progress ring with gradient arc and luminous tip dot.
 */
@Composable
fun CircularProgressRingCanvas(
    progress: Float,
    centerLabel: String,
    startColor: Color,
    endColor: Color,
    trackColor: Color,
    tipColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    strokeWidth: Dp = 8.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f + 2.dp.toPx()
            val diameter = this.size.minDimension - inset * 2f
            val topLeft = Offset(inset, inset)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            val sweep = 360f * clamped
            if (sweep > 0.5f) {
                drawArc(
                    brush = Brush.linearGradient(listOf(startColor, endColor)),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                val angleRad = Math.toRadians((-90f + sweep).toDouble())
                val radius = diameter / 2f
                val cx = this.size.width / 2f + (radius * cos(angleRad)).toFloat()
                val cy = this.size.height / 2f + (radius * sin(angleRad)).toFloat()
                drawCircle(
                    color = startColor.copy(alpha = 0.45f),
                    radius = strokePx * 0.85f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = tipColor,
                    radius = strokePx * 0.48f,
                    center = Offset(cx, cy)
                )
            }
        }
        Text(
            text = centerLabel,
            style = MonoTagTextStyle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
    }
}

/**
 * Smooth gradient sparkline wave with area fill and peak indicator dot.
 */
@Composable
fun SparklineWaveCanvas(
    values: List<Float>,
    lineStartColor: Color,
    lineEndColor: Color,
    modifier: Modifier = Modifier
) {
    val points = if (values.size >= 2) values else listOf(0.15f, 0.25f, 0.2f, 0.45f, 0.4f, 0.72f, 0.9f)
    val maxVal = (points.maxOrNull() ?: 1f).coerceAtLeast(1f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padY = 5.dp.toPx()
        val usableH = (h - padY * 2f).coerceAtLeast(4f)
        val coords = points.mapIndexed { idx, v ->
            val x = (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * (w - 6.dp.toPx()) + 3.dp.toPx()
            val norm = (v / maxVal).coerceIn(0.05f, 1f)
            val y = padY + usableH * (1f - norm)
            Offset(x, y)
        }
        val strokePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 1 until coords.size) {
                val prev = coords[i - 1]
                val curr = coords[i]
                val midX = (prev.x + curr.x) / 2f
                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
        }
        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(coords.last().x, h)
            lineTo(coords.first().x, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineStartColor.copy(alpha = 0.34f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )
        drawPath(
            path = strokePath,
            brush = Brush.horizontalGradient(listOf(lineStartColor, lineEndColor)),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        val last = coords.last()
        drawCircle(color = lineEndColor.copy(alpha = 0.4f), radius = 6.dp.toPx(), center = last)
        drawCircle(color = Color.White, radius = 3.2.dp.toPx(), center = last)
    }
}

/**
 * Circular audio pronunciation trigger with custom Canvas 5-bar equalizer.
 */
@Composable
fun AudioEqualizerButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val bgBrush = if (isDark) {
        Brush.linearGradient(
            listOf(TechColors.Indigo.copy(alpha = 0.24f), TechColors.Cyan.copy(alpha = 0.18f))
        )
    } else {
        Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer)
        )
    }
    val borderColor = if (isDark) TechColors.Cyan.copy(alpha = 0.42f)
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val barColors = if (isDark) {
        listOf(TechColors.Cyan, TechColors.Indigo, TechColors.Violet, TechColors.Cyan, TechColors.Indigo)
    } else {
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.primary
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.dp, borderColor, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 20.dp, height = 14.dp)) {
            val heights = listOf(0.40f, 0.80f, 0.55f, 0.95f, 0.42f)
            val barW = 2.4.dp.toPx()
            val gap = (this.size.width - barW * heights.size) / (heights.size - 1)
            heights.forEachIndexed { idx, frac ->
                val bh = this.size.height * frac
                val top = (this.size.height - bh) / 2f
                drawRoundRect(
                    color = barColors[idx],
                    topLeft = Offset(idx * (barW + gap), top),
                    size = Size(barW, bh),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
            }
        }
    }
}
