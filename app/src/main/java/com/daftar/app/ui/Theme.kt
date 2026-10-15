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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftar.app.R

// The look follows Saud's reference: deep indigo, a glow at the top of the screen, and two
// gradients doing all the talking. Lime-to-teal means money in, done or good; pink-to-orange means
// money out, owed or late.
private val Ink = Color(0xFF130B2E)

/** Text and icons that sit on top of a gradient. */
val OnGradient = Color(0xFF130B2E)

private val Light = lightColorScheme(
    primary = Color(0xFF0F9C8F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFF5EF),
    onPrimaryContainer = Color(0xFF00413A),
    secondary = Color(0xFF8F8600),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4F0B4),
    onSecondaryContainer = Color(0xFF3A3600),
    tertiary = Color(0xFFD63A63),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDE5),
    onTertiaryContainer = Color(0xFF5A0F25),
    error = Color(0xFFD03A3A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF5E130D),
    background = Color(0xFFF4F1FB),
    onBackground = Ink,
    surface = Color(0xFFFFFFFF),
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAE5F6),
    onSurfaceVariant = Color(0xFF625A7E),
    outline = Color(0xFFB8AFD3),
    outlineVariant = Color(0xFFE3DDF1),
    surfaceDim = Color(0xFFE2DCF0),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF8FE),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1EDFA),
    surfaceContainerHighest = Color(0xFFEAE5F6),
    inverseSurface = Color(0xFF2A2145),
    inverseOnSurface = Color(0xFFF2EFFA),
    inversePrimary = Color(0xFF3EDBC4),
    scrim = Color(0xFF000000),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF3EDBC4),
    onPrimary = Ink,
    primaryContainer = Color(0xFF173F4A),
    onPrimaryContainer = Color(0xFFB8F5EB),
    secondary = Color(0xFFE6E34A),
    onSecondary = Ink,
    secondaryContainer = Color(0xFF3A3A1A),
    onSecondaryContainer = Color(0xFFF4F0B4),
    tertiary = Color(0xFFFF6E8E),
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF4A1C3A),
    onTertiaryContainer = Color(0xFFFFD9E1),
    error = Color(0xFFFF8A80),
    onError = Ink,
    errorContainer = Color(0xFF6A1F2A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF120A2B),
    onBackground = Color(0xFFF1EEFA),
    surface = Color(0xFF1E1540),
    onSurface = Color(0xFFF1EEFA),
    surfaceVariant = Color(0xFF2B2152),
    onSurfaceVariant = Color(0xFFA79FC8),
    outline = Color(0xFF4E4479),
    outlineVariant = Color(0xFF2E2556),
    surfaceDim = Color(0xFF120A2B),
    surfaceBright = Color(0xFF342A5E),
    surfaceContainerLowest = Color(0xFF0C0620),
    surfaceContainerLow = Color(0xFF180F36),
    surfaceContainer = Color(0xFF1E1540),
    surfaceContainerHigh = Color(0xFF261C4B),
    surfaceContainerHighest = Color(0xFF2B2152),
    inverseSurface = Color(0xFFF1EEFA),
    inverseOnSurface = Color(0xFF2A2145),
    inversePrimary = Color(0xFF0F9C8F),
    scrim = Color(0xFF000000),
)

/** The two gradients and the glow. */
val Lime = Color(0xFFE9E44C)
val Teal = Color(0xFF1FD2C9)
val Pink = Color(0xFFF4507A)
val Orange = Color(0xFFFFA65B)

val BrandGradient: Brush get() = Brush.linearGradient(listOf(Lime, Teal))
val WarmGradient: Brush get() = Brush.linearGradient(listOf(Pink, Orange))

/** Colours with a meaning that Material does not have a slot for. */
class MoneyColors(
    val positive: Color,
    val negative: Color,
    val due: Color,
    /** The soft light at the top of every screen. */
    val glow: Color,
    val glow2: Color,
)

private val LightMoney = MoneyColors(
    positive = Color(0xFF0E8F82),
    negative = Color(0xFFD6345A),
    due = Color(0xFFB9650A),
    glow = Color(0x33F4507A),
    glow2 = Color(0x26FFA65B),
)

private val DarkMoney = MoneyColors(
    positive = Color(0xFF3EDBC4),
    negative = Color(0xFFFF6E8E),
    due = Color(0xFFFFB35C),
    glow = Color(0x55F4507A),
    glow2 = Color(0x40FFA65B),
)

val LocalMoneyColors = staticCompositionLocalOf { LightMoney }

/** Colours a business can pick. Used for its avatar and as the accent on its invoices. */
val BusinessColors = listOf(
    Color(0xFF6A4CE0), Color(0xFF0F9C8F), Color(0xFFD63A63),
    Color(0xFFE08A1E), Color(0xFF2E7FD6), Color(0xFF3B3266),
)

/** Slices of the "where the money went" ring, in order of size. */
val CategoryColors = listOf(
    Color(0xFF1FD2C9), Color(0xFFF4507A), Color(0xFFE9E44C),
    Color(0xFFFFA65B), Color(0xFF8A6CF0), Color(0xFF8C86A8),
)

/** IBM Plex Sans Arabic: one family drawn for both Latin and Arabic, so mixed text sits together. */
val Plex = FontFamily(
    Font(R.font.plex_light, FontWeight.Light),
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
)

private fun TextStyle.plex(weight: FontWeight = FontWeight.Normal) = copy(fontFamily = Plex, fontWeight = weight)

private val AppTypography = Typography().let { base ->
    Typography(
        displayMedium = base.displayMedium.plex(FontWeight.SemiBold),
        displaySmall = base.displaySmall.plex(FontWeight.SemiBold).copy(fontSize = 38.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp),
        // the big thin figures, like the "74%" in the reference
        displayLarge = base.displayLarge.plex(FontWeight.Light).copy(fontSize = 56.sp, lineHeight = 62.sp, letterSpacing = (-1.5).sp),
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

// Soft, generous corners, as in the reference; small controls stay tighter.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
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
