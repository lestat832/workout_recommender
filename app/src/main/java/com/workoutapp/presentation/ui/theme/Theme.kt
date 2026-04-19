package com.workoutapp.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WolfBlue,
    onPrimary = MoonSilver,
    primaryContainer = WolfBlueDark,
    onPrimaryContainer = MoonSilver,
    secondary = WolfBlueLight,
    onSecondary = WolfCharcoal,
    tertiary = ForestGreen,
    onTertiary = WolfCharcoalDark,
    background = NightBlack,
    onBackground = MoonSilver,
    surface = WolfCharcoal,
    onSurface = MoonSilver,
    surfaceVariant = WolfCharcoalLight,
    onSurfaceVariant = MoonSilverDark,
    error = BloodMoon,
    onError = MoonSilver,
)

private val LightColorScheme = lightColorScheme(
    primary = WolfBlueDark,
    onPrimary = Color.White,
    primaryContainer = WolfBlueLight,
    onPrimaryContainer = WolfCharcoalDark,
    secondary = WolfBlue,
    onSecondary = Color.White,
    tertiary = ForestGreen,
    onTertiary = Color.White,
    background = MoonSilver,
    onBackground = WolfCharcoal,
    surface = Color.White,
    onSurface = WolfCharcoal,
    surfaceVariant = MoonSilverDark,
    onSurfaceVariant = WolfCharcoal,
    error = BloodMoon,
    onError = Color.White,
)

@Composable
fun WorkoutAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Fortis Lupus is a brand-driven app — Material You wallpaper palettes override the wolf
    // identity on Android 12+, so dynamicColor defaults to false. Callers may opt in explicitly.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
