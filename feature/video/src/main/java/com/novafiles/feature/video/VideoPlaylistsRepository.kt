package com.novafiles.feature.video

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class VideoPlaylistsRepository(context: Context) {

    private val playlistsFile = File(context.filesDir, "video_playlists.json")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _playlists = MutableStateFlow<List<VideoPlaylist>>(loadPlaylists())
    val playlists: StateFlow<List<VideoPlaylist>> = _playlists.asStateFlow()

    private fun loadPlaylists(): List<VideoPlaylist> {
        return try {
            if (playlistsFile.exists()) {
                json.decodeFromString<List<VideoPlaylist>>(playlistsFile.readText())
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun savePlaylistsInternal(playlists: List<VideoPlaylist>) = withContext(Dispatchers.IO) {
        try {
            val tmp = File(playlistsFile.parentFile, "${playlistsFile.name}.tmp")
            tmp.writeText(json.encodeToString(playlists))
            if (tmp.exists()) {
                tmp.renameTo(playlistsFile)
            }
        } catch (_: Exception) {}
    }

    suspend fun createPlaylist(name: String, initialVideoPaths: List<String> = emptyList()): VideoPlaylist {
        val newPlaylist = VideoPlaylist(name = name.trim(), videoPaths = initialVideoPaths.distinct())
        val updated = _playlists.value + newPlaylist
        _playlists.value = updated
        savePlaylistsInternal(updated)
        return newPlaylist
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) {
        val updated = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) playlist.copy(name = newName.trim()) else playlist
        }
        _playlists.value = updated
        savePlaylistsInternal(updated)
    }

    suspend fun deletePlaylist(playlistId: String) {
        val updated = _playlists.value.filterNot { it.id == playlistId }
        _playlists.value = updated
        savePlaylistsInternal(updated)
    }

    suspend fun addVideosToPlaylist(playlistId: String, videoPaths: Collection<String>) {
        val updated = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                val combined = (playlist.videoPaths + videoPaths).distinct()
                playlist.copy(videoPaths = combined)
            } else playlist
        }
        _playlists.value = updated
        savePlaylistsInternal(updated)
    }

    suspend fun removeVideosFromPlaylist(playlistId: String, videoPaths: Collection<String>) {
        val pathsToRemove = videoPaths.toSet()
        val updated = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(videoPaths = playlist.videoPaths.filterNot { it in pathsToRemove })
            } else playlist
        }
        _playlists.value = updated
        savePlaylistsInternal(updated)
    }

    suspend fun reorderPlaylist(playlistId: String, newVideoPaths: List<String>) {
        val updated = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(videoPaths = newVideoPaths)
            } else playlist
        }
        _playlists.value = updated
        savePlaylistsInternal(updated)
    }
}
