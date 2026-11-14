package com.novafiles.core.designsystem.theme

import com.novafiles.core.model.AccentColor
import com.novafiles.core.model.ThemeConfig
import com.novafiles.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {

    @Test
    fun testAllEighteenThemeCombinations() {
        var count = 0
        for (mode in ThemeMode.entries) {
            for (accent in AccentColor.entries) {
                val colors = createNovaColors(mode, accent)
                assertNotNull(colors)
                assertNotNull(colors.background)
                assertNotNull(colors.surface)
                assertNotNull(colors.surfaceElevated)
                assertNotNull(colors.primary)
                assertNotNull(colors.secondary)
                assertNotNull(colors.textPrimary)

                when (mode) {
                    ThemeMode.WHITE -> {
                        assertFalse(colors.isDark)
                        assertEquals(WhiteBackground, colors.background)
                        assertEquals(TextPrimaryLight, colors.textPrimary)
                    }
                    ThemeMode.BLACK -> {
                        assertTrue(colors.isDark)
                        assertEquals(BlackBackground, colors.background)
                        assertEquals(TextPrimaryDark, colors.textPrimary)
                    }
                    ThemeMode.CUSTOM -> {
                        // Default custom is dark slate #121212
                        assertTrue(colors.isDark)
                        assertEquals(TextPrimaryDark, colors.textPrimary)
                    }
                }
                count++
            }
        }
        assertEquals(18, count)
    }

    @Test
    fun testCustomThemeContrastCalculation() {
        // Test light custom background
        val lightConfig = ThemeConfig(
            mode = ThemeMode.CUSTOM,
            accent = AccentColor.CORAL,
            customBackground = 0xFFFFFFFFL,
            customSurface = 0xFFF0F0F0L,
            customCard = 0xFFE0E0E0L
        )
        val lightColors = createNovaColors(lightConfig)
        assertFalse(lightColors.isDark)
        assertEquals(TextPrimaryLight, lightColors.textPrimary)

        // Test dark custom background
        val darkConfig = ThemeConfig(
            mode = ThemeMode.CUSTOM,
            accent = AccentColor.CYAN,
            customBackground = 0xFF050B14L,
            customSurface = 0xFF101B2BL,
            customCard = 0xFF1E2E42L
        )
        val darkColors = createNovaColors(darkConfig)
        assertTrue(darkColors.isDark)
        assertEquals(TextPrimaryDark, darkColors.textPrimary)
    }

    @Test
    fun testCategoryColorsConsistency() {
        val colors = createNovaColors(ThemeMode.BLACK, AccentColor.INDIGO)
        assertEquals(ColorVioletSecondary, colors.categoryPhotos)
        assertEquals(ColorCoralRed, colors.categoryVideos)
        assertEquals(ColorIndigoPrimary, colors.categoryDocuments)
        assertEquals(ColorWarmOrange, colors.categoryAudio)
        assertEquals(ColorNeonCyan, colors.categoryArchives)
        assertEquals(ColorMintGreen, colors.categoryApks)
        assertEquals(ColorBrightYellow, colors.categoryFolders)
    }
}
