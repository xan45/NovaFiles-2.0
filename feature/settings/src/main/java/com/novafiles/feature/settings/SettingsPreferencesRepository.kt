package com.novafiles.feature.settings

import android.content.Context
import android.content.SharedPreferences
import com.novafiles.core.model.AccentColor
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.model.ThemeConfig
import com.novafiles.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppSettings(
    val themeConfig: ThemeConfig = ThemeConfig(mode = ThemeMode.BLACK, accent = AccentColor.INDIGO),
    val showHiddenFiles: Boolean = false,
    val defaultSortBy: SortBy = SortBy.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val isAppLockEnabled: Boolean = false,
    val confirmFileDeletion: Boolean = true
)

class SettingsPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("novafiles_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val modeName = prefs.getString("theme_mode", ThemeMode.BLACK.name) ?: ThemeMode.BLACK.name
        val accentName = prefs.getString("theme_accent", AccentColor.INDIGO.name) ?: AccentColor.INDIGO.name
        val mode = when (modeName.uppercase()) {
            "LIGHT", "WHITE" -> ThemeMode.WHITE
            "DARK", "AMOLED", "BLACK" -> ThemeMode.BLACK
            "CUSTOM" -> ThemeMode.CUSTOM
            else -> ThemeMode.BLACK
        }
        val accent = runCatching { AccentColor.valueOf(accentName) }.getOrDefault(AccentColor.INDIGO)

        val customBg = prefs.getLong("custom_bg", 0xFF121212L)
        val customSurface = prefs.getLong("custom_surface", 0xFF1E1E1EL)
        val customCard = prefs.getLong("custom_card", 0xFF2A2A2AL)

        val showHidden = prefs.getBoolean("show_hidden_files", false)
        val sortByName = prefs.getString("default_sort_by", SortBy.NAME.name) ?: SortBy.NAME.name
        val sortDirName = prefs.getString("sort_direction", SortDirection.ASCENDING.name) ?: SortDirection.ASCENDING.name
        val sortBy = runCatching { SortBy.valueOf(sortByName) }.getOrDefault(SortBy.NAME)
        val sortDir = runCatching { SortDirection.valueOf(sortDirName) }.getOrDefault(SortDirection.ASCENDING)

        val appLock = prefs.getBoolean("is_app_lock_enabled", false)
        val confirmDelete = prefs.getBoolean("confirm_file_deletion", true)

        return AppSettings(
            themeConfig = ThemeConfig(
                mode = mode,
                accent = accent,
                customBackground = customBg,
                customSurface = customSurface,
                customCard = customCard
            ),
            showHiddenFiles = showHidden,
            defaultSortBy = sortBy,
            sortDirection = sortDir,
            isAppLockEnabled = appLock,
            confirmFileDeletion = confirmDelete
        )
    }

    fun updateThemeConfig(config: ThemeConfig) {
        prefs.edit()
            .putString("theme_mode", config.mode.name)
            .putString("theme_accent", config.accent.name)
            .putLong("custom_bg", config.customBackground)
            .putLong("custom_surface", config.customSurface)
            .putLong("custom_card", config.customCard)
            .apply()
        _settings.update { it.copy(themeConfig = config) }
    }

    fun updateShowHiddenFiles(show: Boolean) {
        prefs.edit().putBoolean("show_hidden_files", show).apply()
        _settings.update { it.copy(showHiddenFiles = show) }
    }

    fun updateDefaultSort(sortBy: SortBy, direction: SortDirection) {
        prefs.edit()
            .putString("default_sort_by", sortBy.name)
            .putString("sort_direction", direction.name)
            .apply()
        _settings.update { it.copy(defaultSortBy = sortBy, sortDirection = direction) }
    }

    fun updateAppLock(enabled: Boolean) {
        prefs.edit().putBoolean("is_app_lock_enabled", enabled).apply()
        _settings.update { it.copy(isAppLockEnabled = enabled) }
    }

    fun updateConfirmFileDeletion(confirm: Boolean) {
        prefs.edit().putBoolean("confirm_file_deletion", confirm).apply()
        _settings.update { it.copy(confirmFileDeletion = confirm) }
    }
}
