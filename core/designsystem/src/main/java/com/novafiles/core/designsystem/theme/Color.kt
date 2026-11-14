package com.novafiles.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.novafiles.core.model.AccentColor
import com.novafiles.core.model.ThemeConfig
import com.novafiles.core.model.ThemeMode

// Base Black (True AMOLED #000000) Tokens
val BlackBackground = Color(0xFF000000)
val BlackSurface = Color(0xFF0A0A0A)
val BlackElevated = Color(0xFF141414)
val BlackCardBorder = Color(0xFF222222)

// Base White (#FFFFFF Light) Tokens
val WhiteBackground = Color(0xFFFFFFFF)
val WhiteSurface = Color(0xFFF8FAFC)
val WhiteElevated = Color(0xFFF1F5F9)
val WhiteCardBorder = Color(0xFFE2E8F0)

// Aliases for compatibility
val AmoledBackground = BlackBackground
val AmoledSurface = BlackSurface
val AmoledElevated = BlackElevated
val AmoledCardBorder = BlackCardBorder

val LightBackground = WhiteBackground
val LightSurface = WhiteSurface
val LightElevated = WhiteElevated
val LightCardBorder = WhiteCardBorder

// Dark Navy Tokens (Fallback / Preserved)
val DarkBackground = Color(0xFF0B1226)
val DarkSurface = Color(0xFF111B3A)
val DarkElevated = Color(0xFF1A2547)
val DarkCardBorder = Color(0xFF2A3A6A)

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

fun createNovaColors(config: ThemeConfig): NovaColors {
    val (primary, secondary) = when (config.accent) {
        AccentColor.INDIGO -> ColorIndigoPrimary to ColorVioletSecondary
        AccentColor.EMERALD -> Color(0xFF10B981) to Color(0xFF34D399)
        AccentColor.CORAL -> ColorCoralRed to Color(0xFFFB7185)
        AccentColor.CYAN -> ColorNeonCyan to Color(0xFF22D3EE)
        AccentColor.AMBER -> ColorBrightYellow to ColorWarmOrange
        AccentColor.PINK -> ColorElectricPink to Color(0xFFEC4899)
    }

    return when (config.mode) {
        ThemeMode.WHITE -> NovaColors(
            background = WhiteBackground,
            surface = WhiteSurface,
            surfaceElevated = WhiteElevated,
            cardBorder = WhiteCardBorder,
            primary = primary,
            secondary = secondary,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            textTertiary = TextTertiaryLight,
            isDark = false
        )
        ThemeMode.BLACK -> NovaColors(
            background = BlackBackground,
            surface = BlackSurface,
            surfaceElevated = BlackElevated,
            cardBorder = BlackCardBorder,
            primary = primary,
            secondary = secondary,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            textTertiary = TextTertiaryDark,
            isDark = true
        )
        ThemeMode.CUSTOM -> {
            val bg = Color(config.customBackground)
            val surface = Color(config.customSurface)
            val card = Color(config.customCard)
            val lum = bg.luminance()
            val isDark = lum <= 0.5f
            val cardBorder = if (isDark) card.copy(alpha = 0.6f) else Color(0xFFCBD5E1)
            val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
            val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
            val textTertiary = if (isDark) TextTertiaryDark else TextTertiaryLight

            NovaColors(
                background = bg,
                surface = surface,
                surfaceElevated = card,
                cardBorder = cardBorder,
                primary = primary,
                secondary = secondary,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                textTertiary = textTertiary,
                isDark = isDark
            )
        }
    }
}

fun createNovaColors(mode: ThemeMode, accent: AccentColor): NovaColors {
    return createNovaColors(ThemeConfig(mode = mode, accent = accent))
}

val LocalNovaColors = staticCompositionLocalOf {
    createNovaColors(ThemeConfig(mode = ThemeMode.BLACK, accent = AccentColor.INDIGO))
}
