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

private fun darkPreset(
    primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color, secondaryContainer: Color, onSecondaryContainer: Color,
    tertiary: Color, onTertiary: Color, tertiaryContainer: Color, onTertiaryContainer: Color,
    background: Color, onBackground: Color, surface: Color, onSurface: Color,
    surfaceVariant: Color, onSurfaceVariant: Color, outline: Color
) = darkColorScheme(
    primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary, secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, onTertiary = onTertiary, tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
    background = background, onBackground = onBackground, surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant, outline = outline,
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFB4AB),
    inverseSurface = onSurface, inverseOnSurface = surface, inversePrimary = primary
)

val themePresets = listOf(
    darkPreset(green_primary, green_onPrimary, green_primaryContainer, green_onPrimaryContainer,
        green_secondary, green_onSecondary, green_secondaryContainer, green_onSecondaryContainer,
        green_tertiary, green_onTertiary, green_tertiaryContainer, green_onTertiaryContainer,
        green_background, green_onBackground, green_surface, green_onSurface,
        green_surfaceVariant, green_onSurfaceVariant, green_outline),
    darkPreset(blue_primary, blue_onPrimary, blue_primaryContainer, blue_onPrimaryContainer,
        blue_secondary, blue_onSecondary, blue_secondaryContainer, blue_onSecondaryContainer,
        blue_tertiary, blue_onTertiary, blue_tertiaryContainer, blue_onTertiaryContainer,
        blue_background, blue_onBackground, blue_surface, blue_onSurface,
        blue_surfaceVariant, blue_onSurfaceVariant, blue_outline),
    darkPreset(purple_primary, purple_onPrimary, purple_primaryContainer, purple_onPrimaryContainer,
        purple_secondary, purple_onSecondary, purple_secondaryContainer, purple_onSecondaryContainer,
        purple_tertiary, purple_onTertiary, purple_tertiaryContainer, purple_onTertiaryContainer,
        purple_background, purple_onBackground, purple_surface, purple_onSurface,
        purple_surfaceVariant, purple_onSurfaceVariant, purple_outline),
    darkPreset(red_primary, red_onPrimary, red_primaryContainer, red_onPrimaryContainer,
        red_secondary, red_onSecondary, red_secondaryContainer, red_onSecondaryContainer,
        red_tertiary, red_onTertiary, red_tertiaryContainer, red_onTertiaryContainer,
        red_background, red_onBackground, red_surface, red_onSurface,
        red_surfaceVariant, red_onSurfaceVariant, red_outline),
    darkPreset(orange_primary, orange_onPrimary, orange_primaryContainer, orange_onPrimaryContainer,
        orange_secondary, orange_onSecondary, orange_secondaryContainer, orange_onSecondaryContainer,
        orange_tertiary, orange_onTertiary, orange_tertiaryContainer, orange_onTertiaryContainer,
        orange_background, orange_onBackground, orange_surface, orange_onSurface,
        orange_surfaceVariant, orange_onSurfaceVariant, orange_outline),
    darkPreset(cyan_primary, cyan_onPrimary, cyan_primaryContainer, cyan_onPrimaryContainer,
        cyan_secondary, cyan_onSecondary, cyan_secondaryContainer, cyan_onSecondaryContainer,
        cyan_tertiary, cyan_onTertiary, cyan_tertiaryContainer, cyan_onTertiaryContainer,
        cyan_background, cyan_onBackground, cyan_surface, cyan_onSurface,
        cyan_surfaceVariant, cyan_onSurfaceVariant, cyan_outline),
    darkPreset(pink_primary, pink_onPrimary, pink_primaryContainer, pink_onPrimaryContainer,
        pink_secondary, pink_onSecondary, pink_secondaryContainer, pink_onSecondaryContainer,
        pink_tertiary, pink_onTertiary, pink_tertiaryContainer, pink_onTertiaryContainer,
        pink_background, pink_onBackground, pink_surface, pink_onSurface,
        pink_surfaceVariant, pink_onSurfaceVariant, pink_outline)
)

@Composable
fun DSBMaterialTheme(
    themeIndex: Int = 0,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    useCustomFont: Boolean = false,
    fontWeight: Float = 400f,
    fontWidth: Float = 100f,
    fontOpsz: Float = 14f,
    fontSlnt: Float = 0f,
    fontGrad: Float = 0f,
    fontRond: Float = 0f,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeIndex in themePresets.indices -> themePresets[themeIndex]
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = buildTypography(
        useCustomFont = useCustomFont,
        weight = fontWeight,
        width = fontWidth,
        opsz = fontOpsz,
        slnt = fontSlnt,
        grad = fontGrad,
        rond = fontRond
    )

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
        typography = typography,
        shapes = Shapes,
        content = content
    )
}
