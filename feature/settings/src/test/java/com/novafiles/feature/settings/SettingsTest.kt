package com.novafiles.feature.settings

import com.novafiles.core.model.AccentColor
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.model.ThemeConfig
import com.novafiles.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    @Test
    fun testAppSettingsDefaults() {
        val settings = AppSettings()
        assertEquals(ThemeMode.BLACK, settings.themeConfig.mode)
        assertEquals(AccentColor.INDIGO, settings.themeConfig.accent)
        assertFalse(settings.showHiddenFiles)
        assertEquals(SortBy.NAME, settings.defaultSortBy)
        assertEquals(SortDirection.ASCENDING, settings.sortDirection)
        assertFalse(settings.isAppLockEnabled)
        assertTrue(settings.confirmFileDeletion)
    }

    @Test
    fun testThemeConfigCombinations() {
        val totalCombinations = ThemeMode.entries.size * AccentColor.entries.size
        assertEquals(18, totalCombinations)

        val blackCoral = ThemeConfig(mode = ThemeMode.BLACK, accent = AccentColor.CORAL)
        assertEquals(ThemeMode.BLACK, blackCoral.mode)
        assertEquals(AccentColor.CORAL, blackCoral.accent)

        val customCyan = ThemeConfig(
            mode = ThemeMode.CUSTOM,
            accent = AccentColor.CYAN,
            customBackground = 0xFF121212L,
            customSurface = 0xFF1E1E1EL,
            customCard = 0xFF2A2A2AL
        )
        assertEquals(ThemeMode.CUSTOM, customCyan.mode)
        assertEquals(AccentColor.CYAN, customCyan.accent)
        assertEquals(0xFF121212L, customCyan.customBackground)
    }
}
