package com.novafiles.core.model

import kotlinx.serialization.Serializable

/**
 * Base appearance mode.
 */
@Serializable
enum class ThemeMode {
    DARK,
    LIGHT,
    AMOLED
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
    val mode: ThemeMode = ThemeMode.DARK,
    val accent: AccentColor = AccentColor.INDIGO
)
