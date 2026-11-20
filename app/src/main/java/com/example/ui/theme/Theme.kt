package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NothingDarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = PureWhite,
    primaryContainer = NothingRedMuted,
    onPrimaryContainer = PureWhite,
    secondary = PureWhite,
    onSecondary = AmoledBlack,
    secondaryContainer = GlassCardElevated,
    onSecondaryContainer = PureWhite,
    tertiary = StatusCyan,
    onTertiary = AmoledBlack,
    background = AmoledBlack,
    onBackground = PureWhite,
    surface = ObsidianSurface,
    onSurface = PureWhite,
    surfaceVariant = GlassCardSurface,
    onSurfaceVariant = SilverText,
    outline = GlassBorder,
    outlineVariant = GlassBorderSubtle,
    error = NothingRedGlow,
    onError = PureWhite
)

val HardikShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HardikBudsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NothingDarkColorScheme,
        typography = HardikTypography,
        shapes = HardikShapes,
        content = content
    )
}
