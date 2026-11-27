package com.techenglish.app.feature.programming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techenglish.app.core.designsystem.AuroraScreenContainer
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.EmptyStateView
import com.techenglish.app.core.designsystem.JetBrainsMonoFontFamily
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SkeletonListPlaceholder
import com.techenglish.app.core.designsystem.StatusBadge
import com.techenglish.app.core.designsystem.TerminalSearchField
import com.techenglish.app.core.designsystem.ensureAccessibleAccent
import com.techenglish.app.core.designsystem.parseHexColor
import com.techenglish.domain.model.ProgrammingLanguage

fun resolveLanguageTicker(slug: String, name: String): String = when (slug.trim().lowercase()) {
    "kotlin" -> "KT"
    "python" -> "PY"
    "rust" -> "RS"
    "go" -> "GO"
    "javascript" -> "JS"
    "typescript" -> "TS"
    "java" -> "JV"
    "cpp" -> "C++"
    "csharp" -> "C#"
    "c" -> "C"
    "swift" -> "SW"
    "scala" -> "SC"
    "sql" -> "SQL"
    "assembly" -> "ASM"
    "pascal" -> "PAS"
    "ruby" -> "RB"
    "dart" -> "DT"
    "vba" -> "VBA"
    "bash" -> "SH"
    "lua" -> "LUA"
    else -> name.take(2).uppercase()
}

@Composable
fun LanguagesCatalogScreen(
    onOpenLanguage: (Int) -> Unit,
    onOpenCompare: (Int, Int) -> Unit,
    viewModel: LanguagesCatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = LocalTechInteractionConfig.current.isDarkTheme

    AuroraScreenContainer {
        if (state.isLoading) {
            SkeletonListPlaceholder(modifier = Modifier.statusBarsPadding())
            return@AuroraScreenContainer
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Языки кода",
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "Справочник · ${state.languages.size} языков",
                                style = MonoTagTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { onOpenCompare(1, 8) },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.primary,
                            contentColor = if (isDark) Color(0xFF06080F) else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Сравнить", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                }
            }

            item {
                TerminalSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholderText = "Поиск языка, фреймворка, парадигмы...",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.domains) { domain ->
                        val selected = state.selectedDomain == domain
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            onClick = { viewModel.onDomainToggle(domain) }
                        ) {
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (state.languages.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Языки не найдены",
                        subtitle = "Сбросьте фильтр области применения или измените поисковый запрос.",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(state.languages, key = { it.id }) { lang ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        LanguageCatalogCard(lang = lang, onClick = { onOpenLanguage(lang.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageCatalogCard(lang: ProgrammingLanguage, onClick: () -> Unit) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val accent = remember(lang.accentColorHex, surfaceColor) {
        ensureAccessibleAccent(parseHexColor(lang.accentColorHex), surfaceColor)
    }
    val ticker = remember(lang.slug, lang.name) { resolveLanguageTicker(lang.slug, lang.name) }

    BentoGlassCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = ticker,
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(lang.name, style = MaterialTheme.typography.titleMedium)
                    StatusBadge(text = lang.difficulty)
                    Text(
                        text = "${lang.yearCreated}",
                        style = MonoTagTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = lang.descriptionRu,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lang.domains.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
