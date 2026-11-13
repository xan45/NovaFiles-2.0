package com.novafiles.feature.gallery

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class GalleryPreferencesRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val prefsFile = File(context.filesDir, "gallery_preferences.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _preferences = MutableStateFlow(loadInitialPreferences())
    val preferences: StateFlow<GalleryPreferences> = _preferences.asStateFlow()

    private fun loadInitialPreferences(): GalleryPreferences {
        return try {
            if (prefsFile.exists()) {
                val content = prefsFile.readText()
                json.decodeFromString<GalleryPreferences>(content)
            } else {
                GalleryPreferences()
            }
        } catch (_: Exception) {
            GalleryPreferences()
        }
    }

    private suspend fun saveCurrentPreferences(prefs: GalleryPreferences) {
        withContext(ioDispatcher) {
            runCatching {
                val content = json.encodeToString(prefs)
                prefsFile.writeText(content)
            }
        }
    }

    suspend fun updateThumbnailSize(size: ThumbnailSize) {
        _preferences.update { it.copy(thumbnailSize = size) }
        saveCurrentPreferences(_preferences.value)
    }

    suspend fun updateGridSpacing(spacingDp: Int) {
        _preferences.update { it.copy(gridSpacingDp = spacingDp) }
        saveCurrentPreferences(_preferences.value)
    }

    suspend fun updateRoundedThumbnails(isRounded: Boolean) {
        _preferences.update { it.copy(isRoundedThumbnails = isRounded) }
        saveCurrentPreferences(_preferences.value)
    }

    suspend fun updateSortConfig(config: GallerySortConfig) {
        _preferences.update { it.copy(sortConfig = config) }
        saveCurrentPreferences(_preferences.value)
    }

    suspend fun updateGroupByDate(groupByDate: Boolean) {
        _preferences.update { it.copy(groupByDate = groupByDate) }
        saveCurrentPreferences(_preferences.value)
    }

    suspend fun updateShowHiddenFolders(showHidden: Boolean) {
        _preferences.update { it.copy(showHiddenFolders = showHidden) }
        saveCurrentPreferences(_preferences.value)
    }

    suspend fun excludeFolder(path: String) {
        _preferences.update { it.copy(excludedFolderPaths = it.excludedFolderPaths + path) }
        saveCurrentPreferences(_preferences.value)
    }

    suspend fun restoreFolder(path: String) {
        _preferences.update { it.copy(excludedFolderPaths = it.excludedFolderPaths - path) }
        saveCurrentPreferences(_preferences.value)
    }
}
