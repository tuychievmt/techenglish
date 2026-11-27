package com.techenglish.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techenglish.app.core.designsystem.AudioEqualizerButton
import com.techenglish.app.core.designsystem.BentoGlassCard
import com.techenglish.app.core.designsystem.CodeTextStyle
import com.techenglish.app.core.designsystem.IdeTerminalCard
import com.techenglish.app.core.designsystem.IpaTextStyle
import com.techenglish.app.core.designsystem.LocalTechInteractionConfig
import com.techenglish.app.core.designsystem.MonoTagTextStyle
import com.techenglish.app.core.designsystem.SparklineWaveCanvas
import com.techenglish.app.core.designsystem.TechColors
import com.techenglish.app.core.designsystem.TermDisplayTextStyle
import com.techenglish.app.core.designsystem.categoryIconVector
import com.techenglish.domain.model.CategoryProgress
import com.techenglish.domain.model.DayActivity
import com.techenglish.domain.model.Word

@Composable
internal fun StreakBentoTile(
    streakDays: Int,
    bestStreak: Int,
    last7Days: List<DayActivity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val amberBadgeBg = if (isDark) TechColors.AmberContainerDark else TechColors.AmberContainerLight
    val amberBadgeFg = if (isDark) TechColors.OnAmberContainerDark else TechColors.OnAmberContainerLight
    val fallbackLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    BentoGlassCard(
        modifier = modifier,
        accentWashColor = TechColors.Amber,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "СЕРИЯ", style = MonoTagTextStyle, color = amberBadgeFg)
                Surface(shape = CircleShape, color = amberBadgeBg, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (isDark) TechColors.Amber else TechColors.AmberDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "$streakDays дн.", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Рекорд: $bestStreak дн.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (idx in 0 until 7) {
                    val day = last7Days.getOrNull(idx)
                    val label = day?.dayLabel ?: fallbackLabels[idx]
                    val active = day?.count?.let { it > 0 } ?: (idx < streakDays.coerceAtMost(6))
                    val isToday = idx == 6
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        active -> TechColors.Amber
                                        isToday -> amberBadgeBg
                                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                )
                                .then(if (isToday && !active) Modifier.border(1.5.dp, TechColors.Amber, CircleShape) else Modifier)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isToday) (if (isDark) TechColors.Amber else TechColors.AmberDark)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MasteredBentoTile(
    learnedCount: Int,
    weeklyDelta: Int,
    accuracyPercent: Int,
    sparkValues: List<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val emeraldBg = if (isDark) TechColors.EmeraldContainerDark else TechColors.EmeraldContainerLight
    val emeraldFg = if (isDark) TechColors.OnEmeraldContainerDark else TechColors.OnEmeraldContainerLight

    BentoGlassCard(
        modifier = modifier,
        accentWashColor = TechColors.Emerald,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "ОСВОЕНО", style = MonoTagTextStyle, color = emeraldFg)
                Surface(shape = RoundedCornerShape(50), color = emeraldBg) {
                    Text(
                        text = "+$weeklyDelta/нед",
                        style = MonoTagTextStyle,
                        color = emeraldFg,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "$learnedCount", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "$accuracyPercent% точность", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SparklineWaveCanvas(
                values = sparkValues,
                lineStartColor = if (isDark) TechColors.Emerald else TechColors.EmeraldMid,
                lineEndColor = if (isDark) TechColors.Cyan else TechColors.EmeraldDark,
                modifier = Modifier.fillMaxWidth().height(40.dp)
            )
        }
    }
}

@Composable
internal fun WordOfTheDaySpotlightCard(
    word: Word?,
    onSpeak: (String) -> Unit,
    onOpenVocabulary: () -> Unit
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val term = word?.term ?: "idempotency"
    val ipa = word?.transcription?.takeIf { it.isNotBlank() } ?: "/ˌaɪdɛmˈpoʊtənsi/"
    val translation = word?.translation ?: "Идемпотентность"
    val explanation = word?.explanationRu?.takeIf { it.isNotBlank() }
        ?: "Свойство операции, при котором повторный запрос с тем же ключом не изменяет состояние системы."
    val badgeText = word?.categoryName?.uppercase() ?: "C1 · NOUN"
    val violetBg = if (isDark) Color(0xFF292042) else Color(0xFFEDE9FE)
    val violetFg = if (isDark) Color(0xFFEDE9FE) else Color(0xFF6D28D9)

    BentoGlassCard(onClick = onOpenVocabulary) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ТЕРМИН ДНЯ",
                    style = MonoTagTextStyle,
                    color = if (isDark) TechColors.Violet else TechColors.VioletDark
                )
                Surface(shape = RoundedCornerShape(50), color = violetBg) {
                    Text(
                        text = badgeText,
                        style = MonoTagTextStyle,
                        color = violetFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = term, style = TermDisplayTextStyle, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = ipa,
                                style = IpaTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = translation,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                AudioEqualizerButton(
                    onClick = { onSpeak(term) },
                    contentDescription = "Воспроизвести произношение $term",
                    size = 46.dp
                )
            }

            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            IdeTerminalCard(fileName = resolveTerminalFileName(word)) {
                val exampleText = word?.example?.takeIf { it.isNotBlank() }
                if (exampleText == null) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFFF472B6))) { append("POST ") }
                            withStyle(SpanStyle(color = TechColors.Cyan)) { append("/v1/payments\n") }
                            withStyle(SpanStyle(color = TechColors.Violet)) { append("Idempotency-Key: ") }
                            withStyle(SpanStyle(color = Color(0xFFF8FAFC))) { append("9f8a-4c21\n") }
                            withStyle(SpanStyle(color = TechColors.Emerald)) { append("200 OK") }
                            withStyle(SpanStyle(color = Color(0xFF94A3B8))) { append(" · cached response") }
                        },
                        style = CodeTextStyle
                    )
                } else {
                    Text(text = exampleText, style = CodeTextStyle, color = Color(0xFFF8FAFC))
                }
            }
        }
    }
}

internal fun resolveTerminalFileName(word: Word?): String {
    if (word == null) return "payments.http"

    val lang = word.languageName?.trim()?.lowercase().orEmpty()
    when {
        lang == "kotlin" -> return "snippet.kt"
        lang == "python" -> return "snippet.py"
        lang == "java" -> return "Main.java"
        lang == "javascript" -> return "index.js"
        lang == "typescript" -> return "index.ts"
        lang == "go" || lang == "golang" -> return "handler.go"
        lang == "rust" -> return "main.rs"
        lang == "c++" || lang == "cpp" -> return "main.cpp"
        lang == "c#" || lang == "csharp" -> return "Program.cs"
        lang == "swift" -> return "App.swift"
        lang == "sql" -> return "query.sql"
        lang == "bash" || lang == "shell" -> return "git-workflow.sh"
    }

    val cat = word.categoryName.trim().lowercase()
    return when {
        cat.contains("git") || cat.contains("vcs") || cat.contains("верси") -> "git-workflow.sh"
        cat.contains("sec") || cat.contains("безопас") || cat.contains("auth") || cat.contains("crypt") -> "security.conf"
        cat.contains("http") || cat.contains("api") || cat.contains("rest") || cat.contains("web") ||
            cat.contains("сеть") || cat.contains("network") || cat.contains("backend") -> "payments.http"
        cat.contains("sql") || cat.contains("db") || cat.contains("data") || cat.contains("баз") -> "query.sql"
        cat.contains("devops") || cat.contains("docker") || cat.contains("cloud") || cat.contains("ci") -> "deploy.yaml"
        cat.contains("test") || cat.contains("qa") || cat.contains("тест") -> "spec.test.ts"
        else -> "example.txt"
    }
}

@Composable
internal fun OfflinePackBentoCard(
    pack: CategoryProgress,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalTechInteractionConfig.current.isDarkTheme
    val emeraldBg = if (isDark) TechColors.EmeraldContainerDark else TechColors.EmeraldContainerLight
    val emeraldFg = if (isDark) TechColors.OnEmeraldContainerDark else TechColors.OnEmeraldContainerLight

    BentoGlassCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = 0.16f), modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = categoryIconVector(pack.categoryIcon), contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                }
                Surface(shape = RoundedCornerShape(6.dp), color = emeraldBg) {
                    Text(text = "OFFLINE", style = MonoTagTextStyle, color = emeraldFg, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = pack.categoryName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${pack.learnedWords} / ${pack.totalWords} слов", style = IpaTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(pack.progressFraction.coerceIn(0.08f, 1f)).fillMaxHeight().clip(CircleShape).background(if (isDark) TechColors.Emerald else accent)
                )
            }
        }
    }
}

@Composable
internal fun OfflinePackFallbackCard(
    title: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OfflinePackBentoCard(
        pack = CategoryProgress(0, title, "code", 100, 80, 60),
        accent = accent,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun CompactModuleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoGlassCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = 0.16f), modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
