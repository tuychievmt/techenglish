package com.techenglish.app.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

// ==================== SEMANTIC BADGES & EXAMPLE SYNTAX HIGHLIGHTING (УИ-1.2, УИ-1.4) ====================

fun resolveDifficultyBadgeColors(difficulty: String, isDark: Boolean): Pair<Color, Color> {
    val norm = difficulty.trim().lowercase()
    return when {
        norm.contains("beginner") || norm == "a2" || norm == "b1" -> {
            if (isDark) Color(0xFF064E3B) to Color(0xFFA7F3D0)
            else Color(0xFFD1FAE5) to Color(0xFF065F46)
        }
        norm.contains("intermediate") || norm == "b2" -> {
            if (isDark) Color(0xFF0C4A6E) to Color(0xFFBAE6FD)
            else Color(0xFFE0F2FE) to Color(0xFF075985)
        }
        norm.contains("advanced") || norm == "c1" -> {
            if (isDark) Color(0xFF78350F) to Color(0xFFFDE68A)
            else Color(0xFFFEF3C7) to Color(0xFF92400E)
        }
        norm.contains("expert") -> {
            if (isDark) Color(0xFF7F1D1D) to Color(0xFFFECACA)
            else Color(0xFFFEE2E2) to Color(0xFF991B1B)
        }
        norm.contains("research") || norm == "c2" -> {
            if (isDark) Color(0xFF3B0764) to Color(0xFFE9D5FF)
            else Color(0xFFF3E8FF) to Color(0xFF581C87)
        }
        else -> {
            if (isDark) Color(0xFF1E293B) to Color(0xFFE2E8F0)
            else Color(0xFFE2E8F0) to Color(0xFF1E293B)
        }
    }
}

fun resolvePartOfSpeechBadgeColors(pos: String, isDark: Boolean): Pair<Color, Color> {
    val norm = pos.trim().lowercase()
    return when {
        norm.contains("verb") -> {
            if (isDark) Color(0xFF1E3A8A) to Color(0xFFDBEAFE)
            else Color(0xFFDBEAFE) to Color(0xFF1E3A8A)
        }
        norm.contains("adj") || norm.contains("adv") -> {
            if (isDark) Color(0xFF134E4A) to Color(0xFFCCFBF1)
            else Color(0xFFCCFBF1) to Color(0xFF115E59)
        }
        norm.contains("abbr") -> {
            if (isDark) Color(0xFF4C1D95) to Color(0xFFEDE9FE)
            else Color(0xFFEDE9FE) to Color(0xFF4C1D95)
        }
        else -> {
            if (isDark) Color(0xFF1F2937) to Color(0xFFF3F4F6)
            else Color(0xFFF1F5F9) to Color(0xFF0F172A)
        }
    }
}

@Composable
fun DifficultyBadge(
    difficulty: String,
    modifier: Modifier = Modifier
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val (bg, fg) = resolveDifficultyBadgeColors(difficulty, isDark)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = modifier.semantics { contentDescription = "Уровень сложности: $difficulty" }
    ) {
        Text(
            text = difficulty,
            style = MonoTagTextStyle,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PartOfSpeechBadge(
    partOfSpeech: String,
    modifier: Modifier = Modifier
) {
    if (partOfSpeech.isBlank()) return
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val (bg, fg) = resolvePartOfSpeechBadgeColors(partOfSpeech, isDark)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = modifier.semantics { contentDescription = "Часть речи: $partOfSpeech" }
    ) {
        Text(
            text = partOfSpeech,
            style = MonoTagTextStyle,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Highlights [targetTerm] in [example] with [accentColor] while rendering surrounding text
 * in [mutedColor] (which is verified to meet >= 4.5:1 WCAG AA contrast).
 */
fun buildHighlightedExampleAnnotatedString(
    example: String,
    targetTerm: String,
    accentColor: Color,
    mutedColor: Color
): AnnotatedString = buildAnnotatedString {
    if (example.isBlank()) return@buildAnnotatedString
    val cleanTarget = targetTerm.trim()
    if (cleanTarget.isEmpty()) {
        withStyle(SpanStyle(color = mutedColor)) { append(example) }
        return@buildAnnotatedString
    }

    val lowerExample = example.lowercase()
    val candidates = buildList {
        add(cleanTarget.lowercase())
        if (cleanTarget.length >= 5) {
            add(cleanTarget.lowercase().removeSuffix("s").removeSuffix("ing").removeSuffix("ed"))
        }
        cleanTarget.split(" ", "-").filter { it.length >= 4 }.forEach { add(it.lowercase()) }
    }.distinct().filter { it.length >= 2 }

    val matchedRanges = mutableListOf<IntRange>()
    for (token in candidates) {
        var idx = lowerExample.indexOf(token)
        while (idx >= 0) {
            val end = idx + token.length
            val overlaps = matchedRanges.any { idx < it.last + 1 && end > it.first }
            if (!overlaps) {
                matchedRanges.add(idx until end)
            }
            idx = lowerExample.indexOf(token, startIndex = end)
        }
        if (matchedRanges.isNotEmpty()) break
    }

    if (matchedRanges.isEmpty()) {
        withStyle(SpanStyle(color = mutedColor)) { append(example) }
        return@buildAnnotatedString
    }

    val sorted = matchedRanges.sortedBy { it.first }
    var cursor = 0
    for (range in sorted) {
        if (range.first > cursor) {
            withStyle(SpanStyle(color = mutedColor)) {
                append(example.substring(cursor, range.first))
            }
        }
        withStyle(
            SpanStyle(
                color = accentColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            )
        ) {
            append(example.substring(range.first, range.last + 1))
        }
        cursor = range.last + 1
    }
    if (cursor < example.length) {
        withStyle(SpanStyle(color = mutedColor)) {
            append(example.substring(cursor))
        }
    }
}

/**
 * Quote-style usage example container with a colored vertical left bar and syntax-highlighted target term (УИ-1.2, УИ-1.4).
 */
@Composable
fun HighlightedExampleQuoteCard(
    exampleEn: String,
    targetTerm: String,
    exampleRu: String? = null,
    accentBarColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val containerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val effectiveAccent = ensureAccessibleAccent(accentBarColor, MaterialTheme.colorScheme.surface)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlighted = remember(exampleEn, targetTerm, effectiveAccent, mutedColor) {
        buildHighlightedExampleAnnotatedString(
            example = exampleEn,
            targetTerm = targetTerm,
            accentColor = effectiveAccent,
            mutedColor = mutedColor
        )
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerBg,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(effectiveAccent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ПРИМЕР ИСПОЛЬЗОВАНИЯ",
                    style = MonoTagTextStyle,
                    color = effectiveAccent
                )
                Text(
                    text = highlighted,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!exampleRu.isNullOrBlank()) {
                    Text(
                        text = exampleRu,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Dedicated container for primary Russian translation (УИ-1.4).
 */
@Composable
fun TranslationContainerCard(
    translationText: String,
    secondaryTranslationText: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "ПЕРЕВОД",
                style = MonoTagTextStyle,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = translationText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!secondaryTranslationText.isNullOrBlank()) {
                Text(
                    text = "TJ: $secondaryTranslationText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dedicated container for practical engineering explanation (УИ-1.4).
 */
@Composable
fun PracticalExplanationCard(
    title: String = "ПРАКТИЧЕСКОЕ ПРИМЕНЕНИЕ",
    explanationText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MonoTagTextStyle,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = explanationText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

