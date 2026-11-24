package com.techenglish.app.core.designsystem

import android.content.Context
import android.os.Build
import android.provider.Settings
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

// Semantic accents used consistently across Light, Dark & 5 IDE themes
object TechColors {
    val Emerald = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF047857)
    val Amber = Color(0xFFF59E0B)
    val AmberDark = Color(0xFFB45309)
    val Rose = Color(0xFFEF4444)
    val Sky = Color(0xFF0EA5E9)
    val Indigo = Color(0xFF4F46E5)
    val Violet = Color(0xFF7C3AED)

    // IDE-inspired code surface colors
    val CodeBgDark = Color(0xFF0D1117)
    val CodeGutterDark = Color(0xFF161B22)
    val CodeBorderDark = Color(0xFF30363D)
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

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3730A3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0369A1),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF047857),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    error = Color(0xFFB91C1C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF0F0D2E),
    primaryContainer = Color(0xFF1E1B4B),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155)
)

val TokyoNightColorScheme = darkColorScheme(
    primary = Color(0xFF7AA2F7),
    onPrimary = Color(0xFF10131C),
    primaryContainer = Color(0xFF1F2335),
    onPrimaryContainer = Color(0xFFC0CAF5),
    secondary = Color(0xFFBB9AF7),
    onSecondary = Color(0xFF161324),
    secondaryContainer = Color(0xFF292244),
    onSecondaryContainer = Color(0xFFE9DDFB),
    tertiary = Color(0xFF73DACA),
    onTertiary = Color(0xFF0D211E),
    tertiaryContainer = Color(0xFF183635),
    onTertiaryContainer = Color(0xFFCCFBF1),
    error = Color(0xFFF7768E),
    onError = Color(0xFF2B0B12),
    errorContainer = Color(0xFF511B27),
    onErrorContainer = Color(0xFFFFD9E0),
    background = Color(0xFF16161E),
    onBackground = Color(0xFFC0CAF5),
    surface = Color(0xFF1A1B26),
    onSurface = Color(0xFFC0CAF5),
    surfaceVariant = Color(0xFF24283B),
    onSurfaceVariant = Color(0xFFA9B1D6),
    outline = Color(0xFF565F89),
    outlineVariant = Color(0xFF3B4261)
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
    background = Color(0xFF1E1F29),
    onBackground = Color(0xFFF8F8F2),
    surface = Color(0xFF282A36),
    onSurface = Color(0xFFF8F8F2),
    surfaceVariant = Color(0xFF343746),
    onSurfaceVariant = Color(0xFFD6DCFA),
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

/**
 * WCAG 2.1 relative luminance and contrast ratio calculator (`1.0` .. `21.0`).
 * Used to verify `>= 4.5:1` AA contrast across all 7 themes and badge palettes.
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
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // 1.45 - 1.50x line-height for definitions, explanations & documentation articles (УИ-1.3)
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
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

val TermDisplayTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 32.sp,
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
    fontSize = 13.sp,
    lineHeight = 19.sp
)

val MonoTagTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 16.sp
)

val MonoMetricTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 22.sp
)

val CodeTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 20.sp
)

val TechShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
