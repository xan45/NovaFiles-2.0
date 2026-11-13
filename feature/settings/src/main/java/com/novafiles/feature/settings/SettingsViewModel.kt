package com.novafiles.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.model.AccentColor
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.model.ThemeConfig
import com.novafiles.core.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: SettingsPreferencesRepository = SettingsPreferencesRepository(application)
) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.settings.value
        )

    fun setThemeMode(mode: ThemeMode) {
        val current = settings.value.themeConfig
        repository.updateThemeConfig(current.copy(mode = mode))
    }

    fun setAccentColor(accent: AccentColor) {
        val current = settings.value.themeConfig
        repository.updateThemeConfig(current.copy(accent = accent))
    }

    fun setShowHiddenFiles(show: Boolean) {
        repository.updateShowHiddenFiles(show)
    }

    fun setDefaultSort(sortBy: SortBy) {
        repository.updateDefaultSort(sortBy, settings.value.sortDirection)
    }

    fun setSortDirection(direction: SortDirection) {
        repository.updateDefaultSort(settings.value.defaultSortBy, direction)
    }

    fun setAppLock(enabled: Boolean) {
        repository.updateAppLock(enabled)
    }

    fun setBiometric(enabled: Boolean) {
        repository.updateBiometric(enabled)
    }

    fun setConfirmFileDeletion(confirm: Boolean) {
        repository.updateConfirmFileDeletion(confirm)
    }

    fun clearAppCache() {
        val cache = getApplication<Application>().cacheDir
        cache.deleteRecursively()
        cache.mkdirs()
    }
}
