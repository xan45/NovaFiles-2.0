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
import java.util.UUID

class UserAlbumsRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val albumsFile = File(context.filesDir, "gallery_user_albums.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _albums = MutableStateFlow(loadInitialAlbums())
    val albums: StateFlow<List<UserAlbum>> = _albums.asStateFlow()

    private fun loadInitialAlbums(): List<UserAlbum> {
        return try {
            if (albumsFile.exists()) {
                val content = albumsFile.readText()
                json.decodeFromString<List<UserAlbum>>(content)
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun saveCurrentAlbums(albumsList: List<UserAlbum>) {
        withContext(ioDispatcher) {
            runCatching {
                val content = json.encodeToString(albumsList)
                albumsFile.writeText(content)
            }
        }
    }

    suspend fun createAlbum(name: String): UserAlbum {
        val newAlbum = UserAlbum(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            imagePaths = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        _albums.update { it + newAlbum }
        saveCurrentAlbums(_albums.value)
        return newAlbum
    }

    suspend fun renameAlbum(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        _albums.update { list ->
            list.map { if (it.id == id) it.copy(name = trimmed) else it }
        }
        saveCurrentAlbums(_albums.value)
    }

    suspend fun deleteAlbum(id: String) {
        _albums.update { list ->
            list.filter { it.id != id }
        }
        saveCurrentAlbums(_albums.value)
    }

    suspend fun addImagesToAlbum(id: String, paths: Collection<String>) {
        _albums.update { list ->
            list.map { album ->
                if (album.id == id) {
                    val combined = (album.imagePaths + paths).distinct()
                    album.copy(imagePaths = combined)
                } else {
                    album
                }
            }
        }
        saveCurrentAlbums(_albums.value)
    }

    suspend fun removeImagesFromAlbum(id: String, paths: Collection<String>) {
        val pathSet = paths.toSet()
        _albums.update { list ->
            list.map { album ->
                if (album.id == id) {
                    album.copy(imagePaths = album.imagePaths.filterNot { pathSet.contains(it) })
                } else {
                    album
                }
            }
        }
        saveCurrentAlbums(_albums.value)
    }

    suspend fun reorderImagesInAlbum(id: String, newOrder: List<String>) {
        _albums.update { list ->
            list.map { album ->
                if (album.id == id) {
                    album.copy(imagePaths = newOrder)
                } else {
                    album
                }
            }
        }
        saveCurrentAlbums(_albums.value)
    }
}
