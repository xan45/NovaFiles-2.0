package com.novafiles.core.model

import kotlinx.serialization.Serializable

/**
 * Base appearance mode.
 * Exactly 3 primary base theme options:
 * 1. WHITE - Clean light theme with white background (#FFFFFF) and high contrast dark text.
 * 2. BLACK - True AMOLED-friendly pure black (#000000) with dark elevated surfaces.
 * 3. CUSTOM - User-customizable base background, surface, and card colors with contrast enforcement.
 */
@Serializable
enum class ThemeMode {
    WHITE,
    BLACK,
    CUSTOM
}

/**
 * Brand accent color option (6 curated accents).
 */
@Serializable
enum class AccentColor {
    INDIGO,  // Default Blue/Violet (#5B6EF5 / #8A6CFF)
    EMERALD, // Mint Green (#10B981 / #34D399)
    CORAL,   // Coral Red (#FF6B6B / #FB7185)
    CYAN,    // Neon Cyan (#2DD4BF / #22D3EE)
    AMBER,   // Warm Yellow/Orange (#FFD166 / #FF9F43)
    PINK     // Electric Pink (#F472B6 / #EC4899)
}

/**
 * Complete user theme configuration.
 */
@Serializable
data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.BLACK,
    val accent: AccentColor = AccentColor.INDIGO,
    val customBackground: Long = 0xFF121212L,
    val customSurface: Long = 0xFF1E1E1EL,
    val customCard: Long = 0xFF2A2A2AL
)
