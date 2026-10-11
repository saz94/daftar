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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftar.app.R

// The look borrows from carbon-copy invoice books: carbon-blue ink on cool paper, with the pale
// yellow and pink of the duplicate sheets used for "waiting" and "money out".
private val Ink = Color(0xFF141A2E)
private val CarbonBlue = Color(0xFF2340B8)

private val Light = lightColorScheme(
    primary = CarbonBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE4FF),
    onPrimaryContainer = Color(0xFF0B1E6B),
    secondary = Color(0xFF7A5900),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBEBAA),
    onSecondaryContainer = Color(0xFF3D2D00),
    tertiary = Color(0xFFA4365A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF9DCE4),
    onTertiaryContainer = Color(0xFF4A1026),
    error = Color(0xFFC8372D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBDDDA),
    onErrorContainer = Color(0xFF5E130D),
    background = Color(0xFFF2F4F9),
    onBackground = Ink,
    surface = Color(0xFFFFFFFF),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE6EAF3),
    onSurfaceVariant = Color(0xFF5B6480),
    outline = Color(0xFFAEB6CC),
    outlineVariant = Color(0xFFDDE2EE),
    surfaceDim = Color(0xFFDCE0EB),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FC),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEEF1F7),
    surfaceContainerHighest = Color(0xFFE6EAF3),
    inverseSurface = Color(0xFF242B45),
    inverseOnSurface = Color(0xFFF0F2F9),
    inversePrimary = Color(0xFFA9B8FF),
    scrim = Color(0xFF000000),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA9B8FF),
    onPrimary = Color(0xFF0B1E6B),
    primaryContainer = Color(0xFF22307A),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFFF0D36B),
    onSecondary = Color(0xFF3D2D00),
    secondaryContainer = Color(0xFF4A3A00),
    onSecondaryContainer = Color(0xFFFBEBAA),
    tertiary = Color(0xFFF2A9BE),
    onTertiary = Color(0xFF4A1026),
    tertiaryContainer = Color(0xFF5A2136),
    onTertiaryContainer = Color(0xFFF9DCE4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5E130D),
    errorContainer = Color(0xFF7A211A),
    onErrorContainer = Color(0xFFFBDDDA),
    background = Color(0xFF0C1020),
    onBackground = Color(0xFFE9ECF7),
    surface = Color(0xFF151A2E),
    onSurface = Color(0xFFE9ECF7),
    surfaceVariant = Color(0xFF20273F),
    onSurfaceVariant = Color(0xFFA0A8C2),
    outline = Color(0xFF4A5372),
    outlineVariant = Color(0xFF262D47),
    surfaceDim = Color(0xFF0C1020),
    surfaceBright = Color(0xFF2A3250),
    surfaceContainerLowest = Color(0xFF080B17),
    surfaceContainerLow = Color(0xFF111528),
    surfaceContainer = Color(0xFF151A2E),
    surfaceContainerHigh = Color(0xFF1C2238),
    surfaceContainerHighest = Color(0xFF20273F),
    inverseSurface = Color(0xFFE9ECF7),
    inverseOnSurface = Color(0xFF242B45),
    inversePrimary = CarbonBlue,
    scrim = Color(0xFF000000),
)

/** Colours with a meaning that Material does not have a slot for. */
class MoneyColors(
    val positive: Color,
    val negative: Color,
    val due: Color,
    /** The two ends of the dark-blue headline card. */
    val heroStart: Color,
    val heroEnd: Color,
)

private val LightMoney = MoneyColors(
    positive = Color(0xFF17835A),
    negative = Color(0xFFC8372D),
    due = Color(0xFF8A6200),
    heroStart = Color(0xFF16277F),
    heroEnd = Color(0xFF2C4FD8),
)

private val DarkMoney = MoneyColors(
    positive = Color(0xFF6FD9AE),
    negative = Color(0xFFFF9C91),
    due = Color(0xFFF0D36B),
    heroStart = Color(0xFF14205F),
    heroEnd = Color(0xFF263C9E),
)

val LocalMoneyColors = staticCompositionLocalOf { LightMoney }

/** Colours a business can pick. Used for its avatar and as the accent on its invoices. */
val BusinessColors = listOf(
    Color(0xFF2340B8), Color(0xFF17835A), Color(0xFF8B2F5C),
    Color(0xFFB26A00), Color(0xFF0F7C8C), Color(0xFF3A4466),
)

/** Slices of the "where the money went" ring, in order of size. */
val CategoryColors = listOf(
    Color(0xFF2C4FD8), Color(0xFFE0789A), Color(0xFFE2B93B),
    Color(0xFF3FAE86), Color(0xFF8A7BD8), Color(0xFF8C95B0),
)

/** IBM Plex Sans Arabic: one family drawn for both Latin and Arabic, so mixed text sits together. */
val Plex = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
)

private fun TextStyle.plex(weight: FontWeight = FontWeight.Normal) = copy(fontFamily = Plex, fontWeight = weight)

private val AppTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.plex(FontWeight.SemiBold),
        displayMedium = base.displayMedium.plex(FontWeight.SemiBold),
        displaySmall = base.displaySmall.plex(FontWeight.SemiBold).copy(fontSize = 38.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.plex(FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.plex(FontWeight.SemiBold).copy(letterSpacing = (-0.3).sp),
        headlineSmall = base.headlineSmall.plex(FontWeight.SemiBold),
        titleLarge = base.titleLarge.plex(FontWeight.SemiBold),
        titleMedium = base.titleMedium.plex(FontWeight.SemiBold).copy(letterSpacing = 0.sp),
        titleSmall = base.titleSmall.plex(FontWeight.SemiBold).copy(letterSpacing = 0.sp),
        bodyLarge = base.bodyLarge.plex().copy(letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.plex().copy(letterSpacing = 0.sp),
        bodySmall = base.bodySmall.plex().copy(letterSpacing = 0.sp),
        labelLarge = base.labelLarge.plex(FontWeight.Medium).copy(letterSpacing = 0.sp),
        labelMedium = base.labelMedium.plex(FontWeight.Medium).copy(letterSpacing = 0.sp),
        labelSmall = base.labelSmall.plex(FontWeight.Medium).copy(letterSpacing = 0.sp),
    )
}

// Radius follows size: the headline card is the roundest thing, small controls the tightest.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Figures line up in columns when every digit has the same width. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

@Composable
fun DaftarTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalMoneyColors provides if (dark) DarkMoney else LightMoney) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
