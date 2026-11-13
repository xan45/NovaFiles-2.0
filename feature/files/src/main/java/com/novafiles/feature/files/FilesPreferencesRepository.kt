package com.novafiles.feature.files

import android.content.Context
import android.content.SharedPreferences
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FilesPreferences(
    val showHiddenFiles: Boolean = false,
    val isGridView: Boolean = false,
    val sortConfig: FileSortConfig = FileSortConfig(sortBy = SortBy.NAME, direction = SortDirection.ASCENDING, foldersFirst = true)
)

class FilesPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("novafiles_file_browser_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<FilesPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): FilesPreferences {
        val showHidden = prefs.getBoolean("show_hidden_files", false)
        val isGrid = prefs.getBoolean("is_grid_view", false)
        val sortByName = prefs.getString("sort_by", SortBy.NAME.name) ?: SortBy.NAME.name
        val sortDirName = prefs.getString("sort_direction", SortDirection.ASCENDING.name) ?: SortDirection.ASCENDING.name
        val sortBy = runCatching { SortBy.valueOf(sortByName) }.getOrDefault(SortBy.NAME)
        val sortDir = runCatching { SortDirection.valueOf(sortDirName) }.getOrDefault(SortDirection.ASCENDING)

        return FilesPreferences(
            showHiddenFiles = showHidden,
            isGridView = isGrid,
            sortConfig = FileSortConfig(sortBy = sortBy, direction = sortDir, foldersFirst = true)
        )
    }

    fun updateShowHiddenFiles(show: Boolean) {
        prefs.edit().putBoolean("show_hidden_files", show).apply()
        _preferences.update { it.copy(showHiddenFiles = show) }
    }

    fun updateGridView(isGrid: Boolean) {
        prefs.edit().putBoolean("is_grid_view", isGrid).apply()
        _preferences.update { it.copy(isGridView = isGrid) }
    }

    fun updateSortConfig(config: FileSortConfig) {
        prefs.edit()
            .putString("sort_by", config.sortBy.name)
            .putString("sort_direction", config.direction.name)
            .apply()
        _preferences.update { it.copy(sortConfig = config) }
    }
}
