package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccessibilitySettings

val LocalAccessibilitySettings = staticCompositionLocalOf { AccessibilitySettings() }

private val EditorialLightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = OnTerracottaPrimary,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = LibraryGreenSecondary,
    onSecondary = OnLibraryGreenSecondary,
    secondaryContainer = LibraryGreenContainer,
    onSecondaryContainer = OnLibraryGreenContainer,
    tertiary = AntiqueGoldTertiary,
    onTertiary = OnAntiqueGoldTertiary,
    tertiaryContainer = AntiqueGoldContainer,
    onTertiaryContainer = OnAntiqueGoldContainer,
    background = ParchmentBackground,
    onBackground = LetterpressInk,
    surface = VellumSurface,
    onSurface = LetterpressInk,
    surfaceVariant = PressedLinenVariant,
    onSurfaceVariant = SepiaMutedInk,
    outline = WarmOutline
)

private val EditorialDarkColorScheme = darkColorScheme(
    primary = CopperPrimaryDark,
    onPrimary = OnCopperPrimaryDark,
    primaryContainer = CopperContainerDark,
    onPrimaryContainer = OnCopperContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = OnSageSecondaryDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = BrassTertiaryDark,
    onTertiary = OnBrassTertiaryDark,
    tertiaryContainer = BrassContainerDark,
    onTertiaryContainer = OnBrassContainerDark,
    background = NocturnalBackground,
    onBackground = ParchmentTextDark,
    surface = ObsidianSurface,
    onSurface = ParchmentTextDark,
    surfaceVariant = SmokedWalnutVariant,
    onSurfaceVariant = MutedTextDark,
    outline = DarkOutline
)

// WCAG AAA High-Contrast Light Palette (Pure Black Ink on Pure White Vellum — up to 21:1 contrast)
val HighContrastLightColorScheme = lightColorScheme(
    primary = Color(0xFF5A0E05),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE8E2),
    onPrimaryContainer = Color(0xFF140200),
    secondary = Color(0xFF063B20),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCF7E7),
    onSecondaryContainer = Color(0xFF00170A),
    tertiary = Color(0xFF422C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFEFC4),
    onTertiaryContainer = Color(0xFF140C00),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF2EFE9),
    onSurfaceVariant = Color(0xFF050505),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF141414)
)

// WCAG AAA High-Contrast Dark Palette (Luminous Gold & Pure Alabaster on Pitch Obsidian — up to 21:1 contrast)
val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFDF94),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2E1F00),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF94FFBE),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF002B15),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFFC2AD),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF380C00),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF060608),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF141418),
    onSurfaceVariant = Color(0xFFFFFFFF),
    outline = Color(0xFFFFDF94),
    outlineVariant = Color(0xFFFFFFFF)
)

/**
 * Computes WCAG 2.1 relative luminance for a Compose [Color].
 */
fun calculateRelativeLuminance(color: Color): Double {
    fun channelLuminance(c: Float): Double {
        val v = c.toDouble().coerceIn(0.0, 1.0)
        return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
    }
    val r = channelLuminance(color.red)
    val g = channelLuminance(color.green)
    val b = channelLuminance(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/**
 * Computes the WCAG 2.1 contrast ratio (1.0 to 21.0) between [foreground] and [background].
 */
fun calculateContrastRatio(foreground: Color, background: Color): Double {
    val lum1 = calculateRelativeLuminance(foreground)
    val lum2 = calculateRelativeLuminance(background)
    val brightest = maxOf(lum1, lum2)
    val darkest = minOf(lum1, lum2)
    return (brightest + 0.05) / (darkest + 0.05)
}

/**
 * Returns true if the [foreground] and [background] pair meets WCAG AAA (>= 7.0:1 contrast ratio).
 */
fun meetsWcagAaa(foreground: Color, background: Color, minimumRatio: Double = 7.0): Boolean {
    return calculateContrastRatio(foreground, background) >= minimumRatio
}

val EditorialShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

private fun TextStyle.scaleForAccessibility(
    scale: Float,
    dyslexiaFriendly: Boolean
): TextStyle {
    val safeScale = scale.coerceIn(0.9f, 1.5f)
    val baseTracking = if (letterSpacing.isSp) letterSpacing.value else 0f
    val newTracking = (baseTracking + if (dyslexiaFriendly) 0.6f else 0f).sp
    val lineHeightBoost = if (dyslexiaFriendly) 1.18f else 1.0f
    return copy(
        fontSize = fontSize * safeScale,
        lineHeight = lineHeight * safeScale * lineHeightBoost,
        letterSpacing = newTracking
    )
}

fun buildAccessibleTypography(settings: AccessibilitySettings): Typography {
    if (settings.fontScaleMultiplier == 1.0f && !settings.dyslexiaFriendlySpacing) {
        return Typography
    }
    val s = settings.fontScaleMultiplier
    val d = settings.dyslexiaFriendlySpacing
    return Typography(
        displayLarge = Typography.displayLarge.scaleForAccessibility(s, d),
        displayMedium = Typography.displayMedium.scaleForAccessibility(s, d),
        headlineLarge = Typography.headlineLarge.scaleForAccessibility(s, d),
        headlineMedium = Typography.headlineMedium.scaleForAccessibility(s, d),
        headlineSmall = Typography.headlineSmall.scaleForAccessibility(s, d),
        titleLarge = Typography.titleLarge.scaleForAccessibility(s, d),
        titleMedium = Typography.titleMedium.scaleForAccessibility(s, d),
        titleSmall = Typography.titleSmall.scaleForAccessibility(s, d),
        bodyLarge = Typography.bodyLarge.scaleForAccessibility(s, d),
        bodyMedium = Typography.bodyMedium.scaleForAccessibility(s, d),
        bodySmall = Typography.bodySmall.scaleForAccessibility(s, d),
        labelLarge = Typography.labelLarge.scaleForAccessibility(s, d),
        labelMedium = Typography.labelMedium.scaleForAccessibility(s, d),
        labelSmall = Typography.labelSmall.scaleForAccessibility(s, d)
    )
}

@Composable
fun CommonplaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accessibilitySettings: AccessibilitySettings = AccessibilitySettings(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        accessibilitySettings.highContrastMode && (accessibilitySettings.preferDarkHighContrast || darkTheme) ->
            HighContrastDarkColorScheme
        accessibilitySettings.highContrastMode ->
            HighContrastLightColorScheme
        darkTheme ->
            EditorialDarkColorScheme
        else ->
            EditorialLightColorScheme
    }

    val effectiveTypography = buildAccessibleTypography(accessibilitySettings)

    CompositionLocalProvider(LocalAccessibilitySettings provides accessibilitySettings) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = effectiveTypography,
            shapes = EditorialShapes,
            content = content
        )
    }
}

