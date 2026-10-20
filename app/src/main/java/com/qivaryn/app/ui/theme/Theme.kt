package com.qivaryn.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = LightQivarynColors.TealButton,
    onPrimary = LightQivarynColors.White,
    primaryContainer = LightQivarynColors.Mint,
    onPrimaryContainer = LightQivarynColors.TealInk,
    secondary = LightQivarynColors.MintStrong,
    onSecondary = LightQivarynColors.TealInk,
    secondaryContainer = LightQivarynColors.MintSoft,
    onSecondaryContainer = LightQivarynColors.TealInk,
    tertiary = LightQivarynColors.LogoGray,
    onTertiary = LightQivarynColors.White,
    background = LightQivarynColors.Background,
    onBackground = LightQivarynColors.Text,
    surface = LightQivarynColors.Surface,
    onSurface = LightQivarynColors.Text,
    surfaceVariant = LightQivarynColors.Panel,
    onSurfaceVariant = LightQivarynColors.TextSecondary,
    surfaceTint = LightQivarynColors.Teal,
    surfaceContainerLowest = LightQivarynColors.BackgroundTop,
    surfaceContainerLow = LightQivarynColors.Surface,
    surfaceContainer = LightQivarynColors.SurfaceAlt,
    surfaceContainerHigh = LightQivarynColors.Panel,
    surfaceContainerHighest = LightQivarynColors.PanelDeep,
    outline = LightQivarynColors.Outline,
    outlineVariant = LightQivarynColors.PanelDeep,
    error = LightQivarynColors.Danger,
    onError = LightQivarynColors.White,
    errorContainer = LightQivarynColors.DangerBg,
    onErrorContainer = LightQivarynColors.Danger
)

private val DarkColors = darkColorScheme(
    primary = DarkQivarynColors.TealButton,
    onPrimary = DarkQivarynColors.White,
    primaryContainer = DarkQivarynColors.Mint,
    onPrimaryContainer = DarkQivarynColors.TealInk,
    secondary = DarkQivarynColors.MintStrong,
    onSecondary = DarkQivarynColors.TealInk,
    secondaryContainer = DarkQivarynColors.MintSoft,
    onSecondaryContainer = DarkQivarynColors.TealInk,
    tertiary = DarkQivarynColors.LogoGray,
    onTertiary = DarkQivarynColors.White,
    background = DarkQivarynColors.Background,
    onBackground = DarkQivarynColors.Text,
    surface = DarkQivarynColors.Surface,
    onSurface = DarkQivarynColors.Text,
    surfaceVariant = DarkQivarynColors.Panel,
    onSurfaceVariant = DarkQivarynColors.TextSecondary,
    surfaceTint = DarkQivarynColors.Teal,
    surfaceContainerLowest = DarkQivarynColors.BackgroundTop,
    surfaceContainerLow = DarkQivarynColors.Surface,
    surfaceContainer = DarkQivarynColors.SurfaceAlt,
    surfaceContainerHigh = DarkQivarynColors.Panel,
    surfaceContainerHighest = DarkQivarynColors.PanelDeep,
    outline = DarkQivarynColors.Outline,
    outlineVariant = DarkQivarynColors.PanelDeep,
    error = DarkQivarynColors.Danger,
    onError = DarkQivarynColors.White,
    errorContainer = DarkQivarynColors.DangerBg,
    onErrorContainer = DarkQivarynColors.Danger
)

private val QivarynShapes = Shapes(
    extraSmall = RoundedCornerShape(18.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun QivarynTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkQivarynColors else LightQivarynColors
    CompositionLocalProvider(LocalQivarynColors provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = QivarynTypography,
            shapes = QivarynShapes,
            content = content
        )
    }
}