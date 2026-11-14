package com.novafiles.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.novafiles.core.model.AccentColor
import com.novafiles.core.model.ThemeConfig
import com.novafiles.core.model.ThemeMode

@Composable
fun NovaTheme(
    config: ThemeConfig = ThemeConfig(
        mode = if (isSystemInDarkTheme()) ThemeMode.BLACK else ThemeMode.BLACK, // Black AMOLED by default
        accent = AccentColor.INDIGO
    ),
    typography: NovaTypography = NovaTypography(),
    shapes: NovaShapes = NovaShapes(),
    elevation: NovaElevation = NovaElevation(),
    content: @Composable () -> Unit
) {
    val novaColors = createNovaColors(config)

    // Bridge with MaterialTheme so system components interoperate cleanly
    val materialColorScheme = if (novaColors.isDark) {
        darkColorScheme(
            primary = novaColors.primary,
            secondary = novaColors.secondary,
            background = novaColors.background,
            surface = novaColors.surface,
            surfaceVariant = novaColors.surfaceElevated,
            onPrimary = TextPrimaryDark,
            onSecondary = TextPrimaryDark,
            onBackground = novaColors.textPrimary,
            onSurface = novaColors.textPrimary,
            error = novaColors.error
        )
    } else {
        lightColorScheme(
            primary = novaColors.primary,
            secondary = novaColors.secondary,
            background = novaColors.background,
            surface = novaColors.surface,
            surfaceVariant = novaColors.surfaceElevated,
            onPrimary = TextPrimaryLight,
            onSecondary = TextPrimaryLight,
            onBackground = novaColors.textPrimary,
            onSurface = novaColors.textPrimary,
            error = novaColors.error
        )
    }

    CompositionLocalProvider(
        LocalNovaColors provides novaColors,
        LocalNovaTypography provides typography,
        LocalNovaShapes provides shapes,
        LocalNovaElevation provides elevation
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}

/**
 * Global accessor object for NovaFiles 2.0 design system tokens.
 * Usage: NovaTheme.colors.background, NovaTheme.typography.title, NovaTheme.shapes.large, etc.
 */
object NovaTheme {
    val colors: NovaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNovaColors.current

    val typography: NovaTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNovaTypography.current

    val shapes: NovaShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalNovaShapes.current

    val elevation: NovaElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalNovaElevation.current
}
