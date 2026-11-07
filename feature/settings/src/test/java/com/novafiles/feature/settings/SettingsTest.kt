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
        assertEquals(ThemeMode.DARK, settings.themeConfig.mode)
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

        val amoledCoral = ThemeConfig(mode = ThemeMode.AMOLED, accent = AccentColor.CORAL)
        assertEquals(ThemeMode.AMOLED, amoledCoral.mode)
        assertEquals(AccentColor.CORAL, amoledCoral.accent)
    }
}
