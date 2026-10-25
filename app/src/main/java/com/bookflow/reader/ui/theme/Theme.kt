package com.bookflow.reader.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val BookFlowDarkScheme = darkColorScheme(
    primary = DeepBlue,                  // #790D16 Burgundy
    onPrimary = WarmCream,               // #F5EFE1 Linen
    primaryContainer = DeepBlueDark,
    onPrimaryContainer = WarmCream,
    secondary = OceanBlue,               // #AEC4D4 Soft Slate
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = OceanBlue,
    tertiary = LightMint,                // #E5D3AF Parchment
    onTertiary = DarkBackground,
    background = DarkBackground,         // #140D0F Obsidian Wine
    onBackground = WarmCream,
    surface = DarkSurface,               // #1E1417 Warm Dark Slate
    onSurface = WarmCream,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightMint,
    outline = OceanBlueDark
)

val BookFlowLightScheme = lightColorScheme(
    primary = DeepBlue,                  // #790D16 Burgundy
    onPrimary = WarmCream,
    primaryContainer = LightMint,        // #E5D3AF Parchment
    onPrimaryContainer = DeepBlue,
    secondary = OceanBlue,               // #AEC4D4 Slate
    onSecondary = SoftBlack,
    secondaryContainer = LightMint,
    onSecondaryContainer = DeepBlue,
    tertiary = LightMint,
    onTertiary = SoftBlack,
    background = WarmCream,              // #F5EFE1 Linen
    onBackground = SoftBlack,
    surface = PageWhite,
    onSurface = SoftBlack,
    surfaceVariant = LightMint,
    onSurfaceVariant = DeepBlue,
    outline = OceanBlueDark
)

@Composable
fun BookFLowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BookFlowDarkScheme else BookFlowLightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BookFlowTypography,
        content = content
    )
}