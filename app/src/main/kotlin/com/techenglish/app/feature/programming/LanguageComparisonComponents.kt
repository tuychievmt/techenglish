package com.techenglish.app.feature.programming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.IdeTerminalCard
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.ensureAccessibleAccent
import com.techenglish.app.core.designsystem.parseHexColor
import com.techenglish.domain.model.CodeSnippet
import com.techenglish.domain.model.ProgrammingLanguage

@Composable
internal fun LanguageSelectorPill(
    selected: ProgrammingLanguage,
    all: List<ProgrammingLanguage>,
    accentDot: Color,
    alignEndPadding: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (alignEndPadding) 12.dp else 22.dp,
                        end = if (alignEndPadding) 22.dp else 12.dp,
                        top = 12.dp,
                        bottom = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accentDot))
                    Text(
                        text = selected.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            all.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.name) },
                    onClick = {
                        onSelect(lang.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun SpecsMatrixRow(
    label: String,
    valueA: String,
    valueB: String,
    colorA: Color,
    colorB: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.9f)
        )
        Text(
            text = valueA,
            style = MonoTagTextStyle,
            color = colorA,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.05f)
        )
        Text(
            text = valueB,
            style = MonoTagTextStyle,
            color = colorB,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.05f)
        )
    }
}

@Composable
internal fun CodeWorkspacePanel(
    language: ProgrammingLanguage,
    snippet: CodeSnippet,
    inRussian: Boolean,
    onOpenBreakdown: () -> Unit,
    isSplitMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val accent = remember(language.accentColorHex, isDark) {
        ensureAccessibleAccent(parseHexColor(language.accentColorHex), Color(0xFF0F172A))
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IdeTerminalCard(
            fileName = language.slug,
            badgeText = language.name,
            trailingActionLabel = "Разбор кода →",
            onTrailingAction = onOpenBreakdown,
            compactHeader = isSplitMode
        ) {
            InteractiveCodeBlock(
                code = snippet.code,
                languageSlug = language.slug,
                keywords = snippet.keywords,
                selectedLine = null,
                isDark = true,
                onLineClick = { onOpenBreakdown() },
                onKeywordClick = { onOpenBreakdown() }
            )
        }

        BentoGlassCard {
            Column(
                modifier = Modifier.padding(if (isSplitMode) 10.dp else 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (inRussian) "РАЗБОР СИНТАКСИСА" else "SYNTAX BREAKDOWN",
                        style = MonoTagTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = snippet.taskTitleEn,
                        style = MonoTagTextStyle,
                        color = accent.takeIf { isDark } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                Text(
                    text = if (inRussian) snippet.explanationRu else snippet.explanationEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
