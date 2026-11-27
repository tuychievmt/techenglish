package com.techenglish.app.feature.academic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techenglish.app.core.designsystem.AudioEqualizerButton
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.DifficultyBadge
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.PartOfSpeechBadge
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermTitleTextStyle
import com.techenglish.domain.model.AcademicDomainSummary
import com.techenglish.domain.model.AcademicSearchFilter
import com.techenglish.domain.model.AcademicStudyFilter
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
    translationLanguage: TranslationLanguage = TranslationLanguage.RU,
    modifier: Modifier = Modifier
) {
    BentoGlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AudioEqualizerButton(
                    onClick = onSpeak,
                    contentDescription = "Озвучить термин ${term.term}",
                    size = 42.dp
                )
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
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AcademicAdvancedFiltersCard(
    filter: AcademicSearchFilter,
    difficulties: List<String>,
    cefrLevels: List<String>,
    partsOfSpeech: List<String>,
    domains: List<AcademicDomainSummary>,
    onSelectDifficulty: (String) -> Unit,
    onSelectCefr: (String) -> Unit,
    onSelectPartOfSpeech: (String) -> Unit,
    onSelectStudyFilter: (AcademicStudyFilter) -> Unit,
    onSelectDomain: (String) -> Unit,
    onClearAllFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Расширенные фильтры", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onClearAllFilters) {
                    Text("Сбросить все", maxLines = 1, softWrap = false)
                }
            }

            Text("Уровень сложности:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(difficulties) { diff ->
                    FilterChip(
                        selected = filter.difficulty == diff,
                        onClick = { onSelectDifficulty(diff) },
                        label = { Text(diff, maxLines = 1, softWrap = false) }
                    )
                }
            }

            Text("Уровень CEFR и Часть речи:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cefrLevels) { cefr ->
                    FilterChip(
                        selected = filter.cefr == cefr,
                        onClick = { onSelectCefr(cefr) },
                        label = { Text(cefr, style = MonoTagTextStyle, maxLines = 1, softWrap = false) }
                    )
                }
                items(partsOfSpeech) { pos ->
                    FilterChip(
                        selected = filter.partOfSpeech == pos,
                        onClick = { onSelectPartOfSpeech(pos) },
                        label = { Text(pos, style = MonoTagTextStyle, maxLines = 1, softWrap = false) }
                    )
                }
            }

            Text("Статус изучения:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AcademicStudyFilter.entries.forEach { sf ->
                    FilterChip(
                        selected = filter.studyFilter == sf,
                        onClick = { onSelectStudyFilter(sf) },
                        label = { Text(sf.labelRu, maxLines = 1, softWrap = false) }
                    )
                }
            }

            if (domains.isNotEmpty()) {
                Text("30 предметных областей:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(domains, key = { it.domain }) { dom ->
                        FilterChip(
                            selected = filter.domain == dom.domain,
                            onClick = { onSelectDomain(dom.domain) },
                            label = { Text("${dom.domain} (${dom.termCount})", maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }
        }
    }
}
