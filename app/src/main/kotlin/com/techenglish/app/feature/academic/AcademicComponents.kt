package com.techenglish.app.feature.academic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techenglish.app.core.designsystem.DifficultyBadge
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.PartOfSpeechBadge
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermTitleTextStyle
import com.techenglish.domain.model.AcademicTerm
import com.techenglish.domain.model.AcademicTermDetail
import com.techenglish.domain.model.TranslationLanguage

enum class AcademicDetailTab(val labelRu: String) {
    EXPLANATIONS("Объяснения"),
    SENSES("Значения"),
    RELATIONS("Связи"),
    SOURCES("Источники")
}

/**
 * Compact Academic Term list row following Progressive Disclosure (УИ-1.1–1.3, УИ-2.3):
 * Displays Term -> IPA -> Translation -> Short Example/Definition -> Audio & Badges.
 */
@Composable
internal fun AcademicTermRowCard(
    term: AcademicTerm,
    onClick: () -> Unit,
    onSpeak: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleSrs: () -> Unit,
    translationLanguage: TranslationLanguage = TranslationLanguage.RU
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = term.term,
                        style = TermTitleTextStyle,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (term.abbreviations.isNotBlank()) {
                        StatusBadge(
                            text = term.abbreviations,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (term.phoneticTranscription.isNotBlank()) {
                    Text(
                        text = term.phoneticTranscription,
                        style = IpaTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = term.displayTranslation(translationLanguage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = term.exampleEn.ifBlank { term.definitionEn },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DifficultyBadge(difficulty = term.difficultyLevel)
                    PartOfSpeechBadge(partOfSpeech = term.partOfSpeech)
                    StatusBadge(text = term.cefrLevel)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onSpeak) {
                    Icon(
                        Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Озвучить термин ${term.term}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (term.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (term.isFavorite) "Убрать из избранного" else "Добавить в избранное",
                        tint = if (term.isFavorite) TechColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleSrs) {
                    Icon(
                        imageVector = if (term.isInSrs) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (term.isInSrs) "Убрать из очереди SRS" else "Добавить в очередь SRS",
                        tint = if (term.isInSrs) TechColors.Emerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun AcademicDetailSourcesCard(detail: AcademicTermDetail) {
    val term = detail.term
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Таксономия и верифицированный источник", style = MaterialTheme.typography.titleSmall)
            Text("Раздел: ${term.majorCategory}", style = MaterialTheme.typography.bodyMedium)
            Text("Предметная область: ${term.domain} → ${term.subdomain}", style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            Text(
                "Источник: ${detail.source?.title ?: term.sourceReference}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            detail.source?.let { src ->
                Text(
                    "${src.publisher} (${src.sourceType}) • ${src.url}",
                    style = MonoTagTextStyle,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Статус проверки: ${term.verificationStatus} • SM-2 повторений: ${term.repetitions} (EF ${"%.2f".format(term.easeFactor)})",
                style = MonoTagTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
