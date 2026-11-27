package com.techenglish.app.core.designsystem

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techenglish.app.R
import com.techenglish.domain.model.ThemeMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// Semantic accents used consistently across Obsidian Aurora, Ceramic Glass & 5 IDE themes
object TechColors {
    val Emerald = Color(0xFF34D399)
    val EmeraldMid = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF047857)
    val EmeraldContainerDark = Color(0xFF064E3B)
    val OnEmeraldContainerDark = Color(0xFFA7F3D0)
    val EmeraldContainerLight = Color(0xFFD1FAE5)
    val OnEmeraldContainerLight = Color(0xFF065F46)

    val Amber = Color(0xFFFBBF24)
    val AmberDark = Color(0xFFB45309)
    val AmberContainerDark = Color(0xFF3B2306)
    val OnAmberContainerDark = Color(0xFFFDE68A)
    val AmberContainerLight = Color(0xFFFEF3C7)
    val OnAmberContainerLight = Color(0xFF92400E)

    val Rose = Color(0xFFFB7185)
    val RoseDark = Color(0xFFDC2626)
    val RoseContainerDark = Color(0xFF3B111E)
    val OnRoseContainerDark = Color(0xFFFECDD3)
    val RoseContainerLight = Color(0xFFFEE2E2)
    val OnRoseContainerLight = Color(0xFF991B1B)

    val Sky = Color(0xFF38BDF8)
    val Cyan = Color(0xFF38BDF8)
    val CyanDark = Color(0xFF0284C7)
    val CyanContainerDark = Color(0xFF0E293D)
    val OnCyanContainerDark = Color(0xFFBAE6FD)
    val CyanContainerLight = Color(0xFFE0F2FE)
    val OnCyanContainerLight = Color(0xFF075985)

    val Indigo = Color(0xFF7AA2F7)
    val IndigoRoyal = Color(0xFF2563EB)
    val Violet = Color(0xFFBB9AF7)
    val VioletDark = Color(0xFF6D28D9)

    // Recessed IDE / Terminal colors (dark slate in both themes per Ceramic Glass + Obsidian specs)
    val TerminalObsidian = Color(0xFF080A11)
    val TerminalSlate = Color(0xFF0F172A)
    val MacRed = Color(0xFFFF5F56)
    val MacYellow = Color(0xFFFFBD2E)
    val MacGreen = Color(0xFF27C93F)

    val CodeBgDark = Color(0xFF080A11)
    val CodeGutterDark = Color(0xFF111522)
    val CodeBorderDark = Color(0xFF242B42)
    val CodeBgLight = Color(0xFFF6F8FA)
    val CodeGutterLight = Color(0xFFEFF2F5)
    val CodeBorderLight = Color(0xFFD0D7DE)
}

/**
 * Local bundled Inter font family (SIL Open Font License 1.1).
 * Covers Latin, full Cyrillic, IPA phonetic symbols, and Tajik characters (ғ, ӣ, қ, ӯ, ҳ, ҷ).
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.inter_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.inter_bold, FontWeight.Bold, FontStyle.Normal)
)

/**
 * Local bundled JetBrains Mono font family (SIL Open Font License 1.1) with Inter fallback
 * so technical terms, IPA transcriptions, tags, numbers, and multilingual text render 100% offline.
 */
val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.inter_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.inter_bold, FontWeight.Bold, FontStyle.Normal)
)

// ==================== CERAMIC GLASS (LIGHT) & OBSIDIAN AURORA (DARK) PALETTES ====================

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF6D28D9),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = Color(0xFF047857),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7AA2F7),
    onPrimary = Color(0xFF06080F),
    primaryContainer = Color(0xFF1E2A4A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFBB9AF7),
    onSecondary = Color(0xFF0B1020),
    secondaryContainer = Color(0xFF292042),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF06281E),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    error = Color(0xFFFB7185),
    onError = Color(0xFF2A090F),
    errorContainer = Color(0xFF3B111E),
    onErrorContainer = Color(0xFFFECDD3),
    background = Color(0xFF06080F),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111522),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF171C2D),
    onSurfaceVariant = Color(0xFFB2BEDB),
    surfaceContainerLowest = Color(0xFF080A11),
    surfaceContainerLow = Color(0xFF111522),
    surfaceContainer = Color(0xFF171C2D),
    surfaceContainerHigh = Color(0xFF22283B),
    surfaceContainerHighest = Color(0xFF2B3249),
    outline = Color(0xFF2E3650),
    outlineVariant = Color(0xFF242B42)
)

val TokyoNightColorScheme = darkColorScheme(
    primary = Color(0xFF7AA2F7),
    onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF1E2A4A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFBB9AF7),
    onSecondary = Color(0xFF0B1020),
    secondaryContainer = Color(0xFF292042),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF06281E),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    error = Color(0xFFFB7185),
    onError = Color(0xFF2A090F),
    errorContainer = Color(0xFF511B27),
    onErrorContainer = Color(0xFFFECDD3),
    background = Color(0xFF06080F),
    onBackground = Color(0xFFEEF2FF),
    surface = Color(0xFF111522),
    onSurface = Color(0xFFEEF2FF),
    surfaceVariant = Color(0xFF171C2D),
    onSurfaceVariant = Color(0xFFA9B1D6),
    surfaceContainerLowest = Color(0xFF080A11),
    surfaceContainerLow = Color(0xFF111522),
    surfaceContainer = Color(0xFF171C2D),
    surfaceContainerHigh = Color(0xFF22283B),
    surfaceContainerHighest = Color(0xFF2B3249),
    outline = Color(0xFF2E3650),
    outlineVariant = Color(0xFF242B42)
)

val DraculaColorScheme = darkColorScheme(
    primary = Color(0xFFBD93F9),
    onPrimary = Color(0xFF191326),
    primaryContainer = Color(0xFF382A52),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = Color(0xFF8BE9FD),
    onSecondary = Color(0xFF0E2329),
    secondaryContainer = Color(0xFF1D3B47),
    onSecondaryContainer = Color(0xFFE0F9FF),
    tertiary = Color(0xFF50FA7B),
    onTertiary = Color(0xFF0A2412),
    tertiaryContainer = Color(0xFF193D26),
    onTertiaryContainer = Color(0xFFDCFCE7),
    error = Color(0xFFFF6E6E),
    onError = Color(0xFF2D0A0A),
    errorContainer = Color(0xFF5A1A1A),
    onErrorContainer = Color(0xFFFFE4E4),
    background = Color(0xFF1A1B26),
    onBackground = Color(0xFFF8F8F2),
    surface = Color(0xFF242634),
    onSurface = Color(0xFFF8F8F2),
    surfaceVariant = Color(0xFF303346),
    onSurfaceVariant = Color(0xFFD6DCFA),
    surfaceContainerLowest = Color(0xFF14151E),
    surfaceContainerLow = Color(0xFF1E1F29),
    surfaceContainer = Color(0xFF282A36),
    surfaceContainerHigh = Color(0xFF343746),
    surfaceContainerHighest = Color(0xFF44475A),
    outline = Color(0xFF6272A4),
    outlineVariant = Color(0xFF44475A)
)

val GitHubDarkColorScheme = darkColorScheme(
    primary = Color(0xFF58A6FF),
    onPrimary = Color(0xFF061527),
    primaryContainer = Color(0xFF132C4E),
    onPrimaryContainer = Color(0xFFD2E8FF),
    secondary = Color(0xFF79C0FF),
    onSecondary = Color(0xFF07192B),
    secondaryContainer = Color(0xFF16304B),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF56D364),
    onTertiary = Color(0xFF07210B),
    tertiaryContainer = Color(0xFF143A1C),
    onTertiaryContainer = Color(0xFFDCFCE7),
    error = Color(0xFFFF7B72),
    onError = Color(0xFF2C0B09),
    errorContainer = Color(0xFF581916),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFFB1BAC4),
    surfaceContainerLowest = Color(0xFF090C10),
    surfaceContainerLow = Color(0xFF12171E),
    surfaceContainer = Color(0xFF161B22),
    surfaceContainerHigh = Color(0xFF21262D),
    surfaceContainerHighest = Color(0xFF30363D),
    outline = Color(0xFF484F58),
    outlineVariant = Color(0xFF30363D)
)

val NordColorScheme = darkColorScheme(
    primary = Color(0xFF88C0D0),
    onPrimary = Color(0xFF142228),
    primaryContainer = Color(0xFF233B45),
    onPrimaryContainer = Color(0xFFE5F4F8),
    secondary = Color(0xFF81A1C1),
    onSecondary = Color(0xFF111A24),
    secondaryContainer = Color(0xFF253447),
    onSecondaryContainer = Color(0xFFE3ECF5),
    tertiary = Color(0xFFA3BE8C),
    onTertiary = Color(0xFF162011),
    tertiaryContainer = Color(0xFF283822),
    onTertiaryContainer = Color(0xFFEAF4E2),
    error = Color(0xFFEB8791),
    onError = Color(0xFF291013),
    errorContainer = Color(0xFF512328),
    onErrorContainer = Color(0xFFFCE8EA),
    background = Color(0xFF242933),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF2E3440),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF3B4252),
    onSurfaceVariant = Color(0xFFD8DEE9),
    surfaceContainerLowest = Color(0xFF1E222B),
    surfaceContainerLow = Color(0xFF292E39),
    surfaceContainer = Color(0xFF2E3440),
    surfaceContainerHigh = Color(0xFF3B4252),
    surfaceContainerHighest = Color(0xFF434C5E),
    outline = Color(0xFF616E88),
    outlineVariant = Color(0xFF4C566A)
)

val GruvboxColorScheme = darkColorScheme(
    primary = Color(0xFFFABD2F),
    onPrimary = Color(0xFF231903),
    primaryContainer = Color(0xFF4A3710),
    onPrimaryContainer = Color(0xFFFDF0C4),
    secondary = Color(0xFF83A598),
    onSecondary = Color(0xFF111B17),
    secondaryContainer = Color(0xFF253831),
    onSecondaryContainer = Color(0xFFE2F0EA),
    tertiary = Color(0xFFB8BB26),
    onTertiary = Color(0xFF1B1C03),
    tertiaryContainer = Color(0xFF36380D),
    onTertiaryContainer = Color(0xFFF2F4C8),
    error = Color(0xFFFB6B5B),
    onError = Color(0xFF280B07),
    errorContainer = Color(0xFF541C16),
    onErrorContainer = Color(0xFFFFE2DE),
    background = Color(0xFF1D2021),
    onBackground = Color(0xFFEBDBB2),
    surface = Color(0xFF282828),
    onSurface = Color(0xFFEBDBB2),
    surfaceVariant = Color(0xFF3C3836),
    onSurfaceVariant = Color(0xFFD5C4A1),
    surfaceContainerLowest = Color(0xFF161819),
    surfaceContainerLow = Color(0xFF222323),
    surfaceContainer = Color(0xFF282828),
    surfaceContainerHigh = Color(0xFF3C3836),
    surfaceContainerHighest = Color(0xFF504945),
    outline = Color(0xFF7C6F64),
    outlineVariant = Color(0xFF504945)
)

fun resolveColorSchemeForMode(
    themeMode: ThemeMode,
    systemDark: Boolean = false
): ColorScheme = when (themeMode) {
    ThemeMode.LIGHT -> LightColorScheme
    ThemeMode.DARK -> DarkColorScheme
    ThemeMode.TOKYO_NIGHT -> TokyoNightColorScheme
    ThemeMode.DRACULA -> DraculaColorScheme
    ThemeMode.GITHUB_DARK -> GitHubDarkColorScheme
    ThemeMode.NORD -> NordColorScheme
    ThemeMode.GRUVBOX -> GruvboxColorScheme
    ThemeMode.SYSTEM -> if (systemDark) DarkColorScheme else LightColorScheme
}

@Immutable
data class HeroCardPalette(
    val gradientStart: Color,
    val gradientMid: Color,
    val gradientEnd: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val badgeBg: Color,
    val badgeContent: Color,
    val ctaBackground: Color,
    val onCta: Color,
    val ringStart: Color,
    val ringEnd: Color
)

fun resolveHeroPalette(
    themeMode: ThemeMode,
    systemDark: Boolean = false
): HeroCardPalette {
    val resolved = if (themeMode == ThemeMode.SYSTEM) {
        if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT
    } else {
        themeMode
    }
    return when (resolved) {
        ThemeMode.LIGHT, ThemeMode.SYSTEM -> HeroCardPalette(
            gradientStart = Color(0xFF1E3A8A),
            gradientMid = Color(0xFF312E81),
            gradientEnd = Color(0xFF4338CA),
            onHero = Color(0xFFFFFFFF),
            onHeroMuted = Color(0xFFE0E7FF),
            badgeBg = Color(0x26FFFFFF),
            badgeContent = Color(0xFFFFFFFF),
            ctaBackground = Color(0xFFFFFFFF),
            onCta = Color(0xFF1E3A8A),
            ringStart = Color(0xFF38BDF8),
            ringEnd = Color(0xFFA5F3FC)
        )
        ThemeMode.DARK, ThemeMode.TOKYO_NIGHT -> HeroCardPalette(
            gradientStart = Color(0xFF1B2559),
            gradientMid = Color(0xFF311B54),
            gradientEnd = Color(0xFF111625),
            onHero = Color(0xFFF8FAFC),
            onHeroMuted = Color(0xFFCBD5E1),
            badgeBg = Color(0x1AFFFFFF),
            badgeContent = Color(0xFF38BDF8),
            ctaBackground = Color(0xFFF1F5F9),
            onCta = Color(0xFF06080F),
            ringStart = Color(0xFF38BDF8),
            ringEnd = Color(0xFF7AA2F7)
        )
        ThemeMode.DRACULA -> HeroCardPalette(
            gradientStart = Color(0xFF2E204D),
            gradientMid = Color(0xFF1D3550),
            gradientEnd = Color(0xFF1A1B26),
            onHero = Color(0xFFF8F8F2),
            onHeroMuted = Color(0xFFD6DCFA),
            badgeBg = Color(0x1AFFFFFF),
            badgeContent = Color(0xFF8BE9FD),
            ctaBackground = Color(0xFFF8F8F2),
            onCta = Color(0xFF191326),
            ringStart = Color(0xFF8BE9FD),
            ringEnd = Color(0xFFBD93F9)
        )
        ThemeMode.GITHUB_DARK -> HeroCardPalette(
            gradientStart = Color(0xFF132C4E),
            gradientMid = Color(0xFF16304B),
            gradientEnd = Color(0xFF0D1117),
            onHero = Color(0xFFF0F6FC),
            onHeroMuted = Color(0xFFC9D1D9),
            badgeBg = Color(0x1AFFFFFF),
            badgeContent = Color(0xFF79C0FF),
            ctaBackground = Color(0xFFF0F6FC),
            onCta = Color(0xFF061527),
            ringStart = Color(0xFF58A6FF),
            ringEnd = Color(0xFF56D364)
        )
        ThemeMode.NORD -> HeroCardPalette(
            gradientStart = Color(0xFF233B45),
            gradientMid = Color(0xFF253447),
            gradientEnd = Color(0xFF1E222B),
            onHero = Color(0xFFECEFF4),
            onHeroMuted = Color(0xFFD8DEE9),
            badgeBg = Color(0x1AFFFFFF),
            badgeContent = Color(0xFF88C0D0),
            ctaBackground = Color(0xFFECEFF4),
            onCta = Color(0xFF142228),
            ringStart = Color(0xFF88C0D0),
            ringEnd = Color(0xFFA3BE8C)
        )
        ThemeMode.GRUVBOX -> HeroCardPalette(
            gradientStart = Color(0xFF3C2E14),
            gradientMid = Color(0xFF2B342E),
            gradientEnd = Color(0xFF1D2021),
            onHero = Color(0xFFFBF1C7),
            onHeroMuted = Color(0xFFEBDBB2),
            badgeBg = Color(0x1AFFFFFF),
            badgeContent = Color(0xFFFABD2F),
            ctaBackground = Color(0xFFFBF1C7),
            onCta = Color(0xFF1D2021),
            ringStart = Color(0xFFFABD2F),
            ringEnd = Color(0xFFB8BB26)
        )
    }
}

/**
 * WCAG 2.1 relative luminance and contrast ratio calculator (`1.0` .. `21.0`).
 * Used to verify `>= 4.5:1` AA contrast across all 7 themes, Hero cards, CTAs, and badge palettes.
 */
fun Color.relativeLuminance(): Double {
    fun channel(c: Float): Double {
        val v = c.toDouble().coerceIn(0.0, 1.0)
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
}

fun calculateContrastRatio(foreground: Color, background: Color): Double {
    val l1 = foreground.relativeLuminance()
    val l2 = background.relativeLuminance()
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Typography scale using Inter for UI/prose (line-height 1.4-1.5x in definitions & docs)
 * and JetBrains Mono for technical terms, IPA, tags, and numeric indicators (УИ-1.1, УИ-1.3).
 */
val TechTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

val TermDisplayTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.3).sp
)

val TermTitleTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 17.sp,
    lineHeight = 24.sp
)

val IpaTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 17.sp
)

val MonoTagTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.4.sp,
    fontFeatureSettings = "tnum"
)

val MonoMetricTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.3).sp,
    fontFeatureSettings = "tnum"
)

val CodeTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 20.sp
)

val TechShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Immutable
data class TechInteractionConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDarkTheme: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val reduceMotionEnabled: Boolean = false
)

val LocalTechInteractionConfig = staticCompositionLocalOf { TechInteractionConfig() }

fun isSystemReduceMotionEnabled(context: Context): Boolean = runCatching {
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    scale == 0f
}.getOrDefault(false)

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

@Composable
fun TechEnglishTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    reduceMotionEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
        else -> true
    }

    val colorScheme = when {
        !themeMode.isIdePreset && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> resolveColorSchemeForMode(themeMode, systemDark)
    }

    val activity = remember(context) { context.findComponentActivity() }
    DisposableEffect(activity, darkTheme) {
        if (activity != null) {
            runCatching {
                activity.enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }
        }
        onDispose {}
    }

    val systemReduceMotion = remember(context) { isSystemReduceMotionEnabled(context) }
    val interactionConfig = remember(themeMode, darkTheme, hapticFeedbackEnabled, reduceMotionEnabled, systemReduceMotion) {
        TechInteractionConfig(
            themeMode = themeMode,
            isDarkTheme = darkTheme,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            reduceMotionEnabled = reduceMotionEnabled || systemReduceMotion
        )
    }

    CompositionLocalProvider(LocalTechInteractionConfig provides interactionConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TechTypography,
            shapes = TechShapes,
            content = content
        )
    }
}
