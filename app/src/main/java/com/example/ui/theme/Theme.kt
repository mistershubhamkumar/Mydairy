package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun MyDiaryTheme(
    themeId: String = "pink_princess",
    fontId: String = "quicksand",
    content: @Composable () -> Unit
) {
    val spec = DiaryThemes.byId(themeId)
    val colorScheme = if (spec.isDark) {
        darkColorScheme(
            primary = spec.primary,
            onPrimary = Color.White,
            primaryContainer = spec.cardSurfaceAlt,
            onPrimaryContainer = spec.accent,
            secondary = spec.secondary,
            onSecondary = Color.White,
            secondaryContainer = spec.cardSurface,
            onSecondaryContainer = spec.textPrimary,
            tertiary = spec.accent,
            onTertiary = Color(0xFF1A093B),
            background = spec.backgroundTop,
            onBackground = spec.textPrimary,
            surface = spec.cardSurface,
            onSurface = spec.textPrimary,
            surfaceVariant = spec.cardSurfaceAlt,
            onSurfaceVariant = spec.textSecondary,
            outline = spec.borderGlow,
            error = Color(0xFFFF6B8A),
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = spec.primary,
            onPrimary = Color.White,
            primaryContainer = spec.accent,
            onPrimaryContainer = spec.textPrimary,
            secondary = spec.secondary,
            onSecondary = Color.White,
            secondaryContainer = spec.cardSurfaceAlt,
            onSecondaryContainer = spec.textPrimary,
            tertiary = spec.borderGlow,
            onTertiary = Color.White,
            background = spec.backgroundTop,
            onBackground = spec.textPrimary,
            surface = spec.cardSurface,
            onSurface = spec.textPrimary,
            surfaceVariant = spec.cardSurfaceAlt,
            onSurfaceVariant = spec.textSecondary,
            outline = spec.borderGlow,
            error = Color(0xFFE11D48),
            onError = Color.White
        )
    }

    CompositionLocalProvider(LocalDiaryThemeSpec provides spec) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildDiaryTypography(fontId),
            content = content
        )
    }
}
