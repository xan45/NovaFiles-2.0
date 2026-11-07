package com.novafiles.core.designsystem.theme

import com.novafiles.core.model.AccentColor
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
                    ThemeMode.DARK -> {
                        assertTrue(colors.isDark)
                        assertEquals(DarkBackground, colors.background)
                    }
                    ThemeMode.AMOLED -> {
                        assertTrue(colors.isDark)
                        assertEquals(AmoledBackground, colors.background)
                    }
                    ThemeMode.LIGHT -> {
                        assertFalse(colors.isDark)
                        assertEquals(LightBackground, colors.background)
                    }
                }
                count++
            }
        }
        assertEquals(18, count)
    }

    @Test
    fun testCategoryColorsConsistency() {
        val colors = createNovaColors(ThemeMode.DARK, AccentColor.INDIGO)
        assertEquals(ColorVioletSecondary, colors.categoryPhotos)
        assertEquals(ColorCoralRed, colors.categoryVideos)
        assertEquals(ColorIndigoPrimary, colors.categoryDocuments)
        assertEquals(ColorWarmOrange, colors.categoryAudio)
        assertEquals(ColorNeonCyan, colors.categoryArchives)
        assertEquals(ColorMintGreen, colors.categoryApks)
        assertEquals(ColorBrightYellow, colors.categoryFolders)
    }
}
