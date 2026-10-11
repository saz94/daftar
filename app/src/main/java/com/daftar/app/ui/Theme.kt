package com.daftar.app.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette: warm ledger paper, ink, and a deep green for money. Amber marks things that are owed.
private val Light = lightColorScheme(
    primary = Color(0xFF0E5A4B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5EBE4),
    onPrimaryContainer = Color(0xFF07382E),
    secondary = Color(0xFF8A5A12),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF6E3C1),
    onSecondaryContainer = Color(0xFF3F2A05),
    tertiary = Color(0xFF3F5F7A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7E5F1),
    onTertiaryContainer = Color(0xFF14293A),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF5C120E),
    background = Color(0xFFF5F3EE),
    onBackground = Color(0xFF171A1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171A1C),
    surfaceVariant = Color(0xFFEAE6DD),
    onSurfaceVariant = Color(0xFF5A6064),
    outline = Color(0xFFB5B0A4),
    outlineVariant = Color(0xFFDEDAD0),
    surfaceDim = Color(0xFFE2DED5),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFAF7),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1EEE7),
    surfaceContainerHighest = Color(0xFFEAE6DD),
    inverseSurface = Color(0xFF2B3033),
    inverseOnSurface = Color(0xFFF1F0EC),
    inversePrimary = Color(0xFF7AD6BC),
    scrim = Color(0xFF000000),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7AD6BC),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF12493D),
    onPrimaryContainer = Color(0xFFC6F0E3),
    secondary = Color(0xFFE8B866),
    onSecondary = Color(0xFF3F2A05),
    secondaryContainer = Color(0xFF4A3510),
    onSecondaryContainer = Color(0xFFF6E3C1),
    tertiary = Color(0xFFA9C8E0),
    onTertiary = Color(0xFF14293A),
    tertiaryContainer = Color(0xFF2B4458),
    onTertiaryContainer = Color(0xFFD7E5F1),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF7A1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF0F1213),
    onBackground = Color(0xFFECEDEA),
    surface = Color(0xFF181C1D),
    onSurface = Color(0xFFECEDEA),
    surfaceVariant = Color(0xFF262C2D),
    onSurfaceVariant = Color(0xFFA7AEAC),
    outline = Color(0xFF596162),
    outlineVariant = Color(0xFF2E3536),
    surfaceDim = Color(0xFF0F1213),
    surfaceBright = Color(0xFF2B3132),
    surfaceContainerLowest = Color(0xFF0B0D0E),
    surfaceContainerLow = Color(0xFF151819),
    surfaceContainer = Color(0xFF181C1D),
    surfaceContainerHigh = Color(0xFF202627),
    surfaceContainerHighest = Color(0xFF262C2D),
    inverseSurface = Color(0xFFECEDEA),
    inverseOnSurface = Color(0xFF2B3033),
    inversePrimary = Color(0xFF0E5A4B),
    scrim = Color(0xFF000000),
)

/** Colours with a meaning that Material does not have a slot for. */
class MoneyColors(val positive: Color, val negative: Color, val due: Color)

val LocalMoneyColors = staticCompositionLocalOf {
    MoneyColors(Color(0xFF1B7A5A), Color(0xFFB4412F), Color(0xFF8A5A12))
}

/** Avatar colours for telling businesses apart at a glance. */
val BusinessColors = listOf(
    Color(0xFF0E5A4B), Color(0xFF8A5A12), Color(0xFF3F5F7A),
    Color(0xFF8B3D4F), Color(0xFF5B4B8A), Color(0xFF3D6B2E),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 42.sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.6.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Figures line up in columns when every digit has the same width. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

@Composable
fun DaftarTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val money = if (dark) {
        MoneyColors(Color(0xFF7AD6BC), Color(0xFFF2988A), Color(0xFFE8B866))
    } else {
        MoneyColors(Color(0xFF1B7A5A), Color(0xFFB4412F), Color(0xFF8A5A12))
    }
    CompositionLocalProvider(LocalMoneyColors provides money) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
