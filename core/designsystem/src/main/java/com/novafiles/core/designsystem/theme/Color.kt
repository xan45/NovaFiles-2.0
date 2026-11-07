package com.novafiles.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.novafiles.core.model.AccentColor
import com.novafiles.core.model.ThemeMode

// Base Dark Navy Tokens (Primary Reference: media_1794034804827_b8bf4514.jpg)
val DarkBackground = Color(0xFF0B1226)
val DarkSurface = Color(0xFF111B3A)
val DarkElevated = Color(0xFF1A2547)
val DarkCardBorder = Color(0xFF2A3A6A)

// Base AMOLED Tokens
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0A0A)
val AmoledElevated = Color(0xFF141414)
val AmoledCardBorder = Color(0xFF222222)

// Base Light Tokens
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightElevated = Color(0xFFF1F5F9)
val LightCardBorder = Color(0xFFE2E8F0)

// Category & Semantic Colors (Reference Palette)
val ColorIndigoPrimary = Color(0xFF5B6EF5)
val ColorVioletSecondary = Color(0xFF8A6CFF)
val ColorCoralRed = Color(0xFFFF6B6B)
val ColorWarmOrange = Color(0xFFFF9F43)
val ColorBrightYellow = Color(0xFFFFD166)
val ColorNeonCyan = Color(0xFF2DD4BF)
val ColorMintGreen = Color(0xFF4ADE80)
val ColorElectricPink = Color(0xFFF472B6)

// Text Colors
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextTertiaryDark = Color(0xFF64748B)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextTertiaryLight = Color(0xFF94A3B8)

@Immutable
data class NovaColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val cardBorder: Color,
    val primary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val isDark: Boolean,
    // Category Accents
    val categoryPhotos: Color = ColorVioletSecondary,
    val categoryVideos: Color = ColorCoralRed,
    val categoryDocuments: Color = ColorIndigoPrimary,
    val categoryAudio: Color = ColorWarmOrange,
    val categoryArchives: Color = ColorNeonCyan,
    val categoryApks: Color = ColorMintGreen,
    val categoryFolders: Color = ColorBrightYellow,
    val error: Color = ColorCoralRed,
    val success: Color = ColorMintGreen,
    val warning: Color = ColorWarmOrange
)

fun createNovaColors(mode: ThemeMode, accent: AccentColor): NovaColors {
    val (primary, secondary) = when (accent) {
        AccentColor.INDIGO -> ColorIndigoPrimary to ColorVioletSecondary
        AccentColor.EMERALD -> Color(0xFF10B981) to Color(0xFF34D399)
        AccentColor.CORAL -> ColorCoralRed to Color(0xFFFB7185)
        AccentColor.CYAN -> ColorNeonCyan to Color(0xFF22D3EE)
        AccentColor.AMBER -> ColorBrightYellow to ColorWarmOrange
        AccentColor.PINK -> ColorElectricPink to Color(0xFFEC4899)
    }

    return when (mode) {
        ThemeMode.DARK -> NovaColors(
            background = DarkBackground,
            surface = DarkSurface,
            surfaceElevated = DarkElevated,
            cardBorder = DarkCardBorder,
            primary = primary,
            secondary = secondary,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            textTertiary = TextTertiaryDark,
            isDark = true
        )
        ThemeMode.AMOLED -> NovaColors(
            background = AmoledBackground,
            surface = AmoledSurface,
            surfaceElevated = AmoledElevated,
            cardBorder = AmoledCardBorder,
            primary = primary,
            secondary = secondary,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            textTertiary = TextTertiaryDark,
            isDark = true
        )
        ThemeMode.LIGHT -> NovaColors(
            background = LightBackground,
            surface = LightSurface,
            surfaceElevated = LightElevated,
            cardBorder = LightCardBorder,
            primary = primary,
            secondary = secondary,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            textTertiary = TextTertiaryLight,
            isDark = false
        )
    }
}

val LocalNovaColors = staticCompositionLocalOf {
    createNovaColors(ThemeMode.DARK, AccentColor.INDIGO)
}
