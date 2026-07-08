package dev.wolly.dsbmaterial.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary
)

private fun createDarkTheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
): ColorScheme {
    val background = Color(0xFF0E0E10)
    val surface = Color(0xFF0E0E10)
    val surfaceVariant = Color(0xFF49454F)

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onPrimary,
        secondaryContainer = secondary.copy(alpha = 0.2f),
        onSecondaryContainer = secondary,
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = primaryContainer,
        onTertiaryContainer = onPrimaryContainer,
        surface = surface,
        onSurface = Color(0xFFE3E2E6),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = Color(0xFFC4C6D0),
        outline = primary.copy(alpha = 0.5f),
        outlineVariant = primary.copy(alpha = 0.3f),
        background = background,
        onBackground = Color(0xFFE3E2E6),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6)
    )
}

val themePresets = listOf(
    DarkColorScheme, // Default (Green-ish)
    createDarkTheme(blue_primary, blue_onPrimary, blue_primaryContainer, blue_onPrimaryContainer, blue_secondary),
    createDarkTheme(purple_primary, purple_onPrimary, purple_primaryContainer, purple_onPrimaryContainer, purple_secondary),
    createDarkTheme(red_primary, red_onPrimary, red_primaryContainer, red_onPrimaryContainer, red_secondary),
    createDarkTheme(orange_primary, orange_onPrimary, orange_primaryContainer, orange_onPrimaryContainer, orange_secondary),
    createDarkTheme(cyan_primary, cyan_onPrimary, cyan_primaryContainer, cyan_onPrimaryContainer, cyan_secondary),
    createDarkTheme(pink_primary, pink_onPrimary, pink_primaryContainer, pink_onPrimaryContainer, pink_secondary)
)

@Composable
fun DSBMaterialTheme(
    themeIndex: Int = 0,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        themeIndex == 0 && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeIndex in themePresets.indices -> themePresets[themeIndex]
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
