package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

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

val EditorialShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun CommonplaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) EditorialDarkColorScheme else EditorialLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = EditorialShapes,
        content = content
    )
}
