package com.novafiles.feature.video

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class VideoPreferencesRepository(context: Context) {

    private val prefsFile = File(context.filesDir, "video_preferences.json")
    private val resumeFile = File(context.filesDir, "video_playback_positions.json")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<VideoPreferences> = _preferences.asStateFlow()

    private val _resumePoints = MutableStateFlow(loadResumePoints())
    val resumePoints: StateFlow<Map<String, PlaybackResumePoint>> = _resumePoints.asStateFlow()

    private fun loadPreferences(): VideoPreferences {
        return try {
            if (prefsFile.exists()) {
                json.decodeFromString<VideoPreferences>(prefsFile.readText())
            } else {
                VideoPreferences()
            }
        } catch (_: Exception) {
            VideoPreferences()
        }
    }

    private fun loadResumePoints(): Map<String, PlaybackResumePoint> {
        return try {
            if (resumeFile.exists()) {
                json.decodeFromString<Map<String, PlaybackResumePoint>>(resumeFile.readText())
            } else {
                emptyMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private suspend fun savePreferencesInternal(prefs: VideoPreferences) = withContext(Dispatchers.IO) {
        try {
            val tmp = File(prefsFile.parentFile, "${prefsFile.name}.tmp")
            tmp.writeText(json.encodeToString(prefs))
            if (tmp.exists()) {
                tmp.renameTo(prefsFile)
            }
        } catch (_: Exception) {}
    }

    private suspend fun saveResumePointsInternal(points: Map<String, PlaybackResumePoint>) = withContext(Dispatchers.IO) {
        try {
            val tmp = File(resumeFile.parentFile, "${resumeFile.name}.tmp")
            tmp.writeText(json.encodeToString(points))
            if (tmp.exists()) {
                tmp.renameTo(resumeFile)
            }
        } catch (_: Exception) {}
    }

    suspend fun updatePreferences(transform: (VideoPreferences) -> VideoPreferences) {
        val updated = transform(_preferences.value)
        _preferences.value = updated
        savePreferencesInternal(updated)
    }

    suspend fun updateViewMode(viewMode: VideoViewMode) {
        updatePreferences { it.copy(viewMode = viewMode) }
    }

    suspend fun updateThumbnailSize(size: VideoThumbnailSize) {
        updatePreferences { it.copy(thumbnailSize = size) }
    }

    suspend fun updateGridSpacing(spacingDp: Int) {
        updatePreferences { it.copy(gridSpacingDp = spacingDp) }
    }

    suspend fun updateShowDurationOverlay(show: Boolean) {
        updatePreferences { it.copy(showDurationOverlay = show) }
    }

    suspend fun updateSortConfig(config: VideoSortConfig) {
        updatePreferences { it.copy(sortConfig = config) }
    }

    suspend fun updateGroupByDate(group: Boolean) {
        updatePreferences { it.copy(groupByDate = group) }
    }

    suspend fun updateRememberPlaybackPosition(remember: Boolean) {
        updatePreferences { it.copy(rememberPlaybackPosition = remember) }
    }

    suspend fun updateDefaultSubtitleEnabled(enabled: Boolean) {
        updatePreferences { it.copy(defaultSubtitleEnabled = enabled) }
    }

    suspend fun updateDefaultPlaybackSpeed(speed: Float) {
        updatePreferences { it.copy(defaultPlaybackSpeed = speed) }
    }

    suspend fun updateOrientationPreference(pref: VideoOrientationPref) {
        updatePreferences { it.copy(orientationPreference = pref) }
    }

    suspend fun savePlaybackPosition(videoPath: String, positionMs: Long, durationMs: Long) {
        if (!_preferences.value.rememberPlaybackPosition) return
        // Do not save if near end (within last 3 seconds)
        val targetPos = if (durationMs > 0 && positionMs >= durationMs - 3000L) 0L else positionMs
        val updated = _resumePoints.value.toMutableMap().apply {
            if (targetPos > 2000L) {
                put(videoPath, PlaybackResumePoint(targetPos, durationMs))
            } else {
                remove(videoPath)
            }
        }
        _resumePoints.value = updated
        saveResumePointsInternal(updated)
    }

    fun getPlaybackPosition(videoPath: String): Long {
        if (!_preferences.value.rememberPlaybackPosition) return 0L
        return _resumePoints.value[videoPath]?.positionMs ?: 0L
    }
}
