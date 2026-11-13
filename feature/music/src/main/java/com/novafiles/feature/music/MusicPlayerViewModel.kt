package com.novafiles.feature.music

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.common.NovaResult
import com.novafiles.core.media.AudioMetadataHelper
import com.novafiles.core.media.MusicPlaybackManager
import com.novafiles.core.media.MusicState
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import com.novafiles.core.storage.StoragePermissionHelper
import com.novafiles.data.files.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class MusicLibraryTab {
    TRACKS,
    ALBUMS,
    ARTISTS,
    FOLDERS
}

data class MusicTrack(
    val fileItem: FileItem,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val format: String,
    val folderPath: String,
    val folderName: String
) : java.io.Serializable

data class MusicAlbum(
    val name: String,
    val artist: String,
    val trackCount: Int,
    val tracks: List<MusicTrack>,
    val firstTrackFile: File
) : java.io.Serializable

data class MusicArtist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val tracks: List<MusicTrack>
) : java.io.Serializable

data class MusicFolder(
    val name: String,
    val path: String,
    val trackCount: Int,
    val totalSizeBytes: Long,
    val tracks: List<MusicTrack>
) : java.io.Serializable

data class MusicPlayerUiState(
    val playback: MusicState = MusicState(),
    val libraryTracks: List<FileItem> = emptyList(),
    val tracks: List<MusicTrack> = emptyList(),
    val albums: List<MusicAlbum> = emptyList(),
    val artists: List<MusicArtist> = emptyList(),
    val folders: List<MusicFolder> = emptyList(),
    val currentTab: MusicLibraryTab = MusicLibraryTab.TRACKS,
    val isLoading: Boolean = false,
    val isPermissionRequired: Boolean = false,
    val errorMessage: String? = null,
    val isQueueSheetVisible: Boolean = false,
    val searchQuery: String = "",
    val sleepTimerMinutesRemaining: Int? = null,
    val selectedSubItemTitle: String? = null,
    val selectedSubItemTracks: List<MusicTrack>? = null,
    val isNowPlayingExpanded: Boolean = false
) {
    val filteredTracks: List<MusicTrack>
        get() = if (searchQuery.isBlank()) tracks else tracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }

    val filteredAlbums: List<MusicAlbum>
        get() = if (searchQuery.isBlank()) albums else albums.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }

    val filteredArtists: List<MusicArtist>
        get() = if (searchQuery.isBlank()) artists else artists.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }

    val filteredFolders: List<MusicFolder>
        get() = if (searchQuery.isBlank()) folders else folders.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.path.contains(searchQuery, ignoreCase = true)
        }
}

class MusicPlayerViewModel(
    private val fileRepository: FileRepository,
    private val context: Context? = null,
    private val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    companion object {
        private const val KEY_MUSIC_TAB = "key_music_current_tab"
    }

    private val restoredTab = savedStateHandle?.get<String>(KEY_MUSIC_TAB)?.let {
        runCatching { MusicLibraryTab.valueOf(it) }.getOrNull()
    } ?: MusicLibraryTab.TRACKS

    private val _uiState = MutableStateFlow(MusicPlayerUiState(currentTab = restoredTab))
    val uiState: StateFlow<MusicPlayerUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        viewModelScope.launch {
            MusicPlaybackManager.state.collect { playback ->
                _uiState.update { it.copy(playback = playback) }
            }
        }
        loadMusicLibrary()
    }

    fun loadMusicLibrary(forceRefresh: Boolean = false) {
        if (scanJob?.isActive == true) return
        if (!forceRefresh && _uiState.value.tracks.isNotEmpty()) {
            return
        }

        val ctx = context
        scanJob = viewModelScope.launch {
            if (ctx != null && !StoragePermissionHelper.hasAudioPermission(ctx)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPermissionRequired = true,
                        errorMessage = null
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = true,
                    isPermissionRequired = false,
                    errorMessage = null
                )
            }

            try {
                val scannedTracks = withContext(Dispatchers.IO) {
                    scanAudioFiles()
                }

                val albums = scannedTracks
                    .groupBy { it.album.ifBlank { "Unknown Album" } }
                    .map { (albumName, albumTracks) ->
                        val artistName = albumTracks.firstOrNull { it.artist != "Unknown Artist" }?.artist ?: "Unknown Artist"
                        MusicAlbum(
                            name = albumName,
                            artist = artistName,
                            trackCount = albumTracks.size,
                            tracks = albumTracks.sortedBy { it.title.lowercase() },
                            firstTrackFile = File(albumTracks.first().fileItem.path)
                        )
                    }
                    .sortedBy { it.name.lowercase() }

                val artists = scannedTracks
                    .groupBy { it.artist.ifBlank { "Unknown Artist" } }
                    .map { (artistName, artistTracks) ->
                        val distinctAlbums = artistTracks.map { it.album }.distinct().size
                        MusicArtist(
                            name = artistName,
                            trackCount = artistTracks.size,
                            albumCount = distinctAlbums,
                            tracks = artistTracks.sortedBy { it.title.lowercase() }
                        )
                    }
                    .sortedBy { it.name.lowercase() }

                val folders = scannedTracks
                    .groupBy { it.folderPath }
                    .map { (folderPath, folderTracks) ->
                        val folderName = folderTracks.firstOrNull()?.folderName ?: File(folderPath).name
                        MusicFolder(
                            name = folderName,
                            path = folderPath,
                            trackCount = folderTracks.size,
                            totalSizeBytes = folderTracks.sumOf { it.fileItem.sizeBytes },
                            tracks = folderTracks.sortedBy { it.title.lowercase() }
                        )
                    }
                    .sortedBy { it.name.lowercase() }

                val sortedTracks = scannedTracks.sortedBy { it.title.lowercase() }
                val libraryFiles = sortedTracks.map { it.fileItem }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPermissionRequired = false,
                        errorMessage = null,
                        tracks = sortedTracks,
                        libraryTracks = libraryFiles,
                        albums = albums,
                        artists = artists,
                        folders = folders
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to scan audio library"
                    )
                }
            }
        }
    }

    private suspend fun scanAudioFiles(): List<MusicTrack> = withContext(Dispatchers.IO) {
        val tracksList = mutableListOf<MusicTrack>()
        val seenPaths = mutableSetOf<String>()

        // 1. MediaStore query
        val ctx = context
        if (ctx != null) {
            try {
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DATE_MODIFIED
                )
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
                val selection = "${MediaStore.Audio.Media.SIZE} > 0"

                ctx.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                    val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                    val dateModCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                        if (path.isNullOrBlank()) continue
                        val file = File(path)
                        if (!file.exists() || file.length() <= 0) continue

                        val canonical = runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
                        if (!seenPaths.add(canonical)) continue

                        val displayName = if (nameCol >= 0) cursor.getString(nameCol) ?: file.name else file.name
                        val rawTitle = if (titleCol >= 0) cursor.getString(titleCol) else null
                        val rawArtist = if (artistCol >= 0) cursor.getString(artistCol) else null
                        val rawAlbum = if (albumCol >= 0) cursor.getString(albumCol) else null
                        val durationMs = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                        val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else file.length()
                        val lastModified = if (dateModCol >= 0) cursor.getLong(dateModCol) * 1000L else file.lastModified()

                        val title = if (!rawTitle.isNullOrBlank()) rawTitle else file.nameWithoutExtension
                        val artist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") rawArtist else "Unknown Artist"
                        val album = if (!rawAlbum.isNullOrBlank() && rawAlbum != "<unknown>") rawAlbum else "Unknown Album"
                        val format = file.extension.uppercase().ifBlank { "AUDIO" }
                        val parentFile = file.parentFile
                        val folderName = parentFile?.name ?: "Music"
                        val folderPath = parentFile?.absolutePath ?: file.parent ?: ""

                        val fileItem = FileItem(
                            id = canonical,
                            name = displayName,
                            path = canonical,
                            sizeBytes = sizeBytes,
                            lastModifiedMs = lastModified,
                            isDirectory = false,
                            fileType = FileType.AUDIO,
                            mimeType = "audio/*"
                        )

                        tracksList.add(
                            MusicTrack(
                                fileItem = fileItem,
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = durationMs,
                                format = format,
                                folderPath = folderPath,
                                folderName = folderName
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 2. Direct storage scan fallback & additional directory search
        val audioExtensions = setOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "opus", "wma")
        val storageRoot = Environment.getExternalStorageDirectory()
        val searchRoots = listOf(
            File(storageRoot, "Music"),
            File(storageRoot, "Download"),
            File(storageRoot, "Podcasts"),
            File(storageRoot, "Audiobooks"),
            File(storageRoot, "Ringtones"),
            File(storageRoot, "Notifications"),
            File(storageRoot, "Alarms"),
            File(storageRoot, "Recordings")
        )

        for (root in searchRoots) {
            if (!root.exists() || !root.canRead()) continue
            scanDirectoryForAudio(
                dir = root,
                depth = 0,
                maxDepth = 5,
                audioExtensions = audioExtensions,
                seenPaths = seenPaths,
                outList = tracksList
            )
        }

        // 3. Fallback for repository mock testing when context is null and disk is empty
        if (tracksList.isEmpty()) {
            try {
                val musicDir = File(storageRoot, "Music")
                val targetDir = if (musicDir.exists()) musicDir.absolutePath else storageRoot.absolutePath
                fileRepository.getFiles(targetDir).collect { result ->
                    if (result is NovaResult.Success) {
                        for (item in result.data) {
                            if (item.fileType == FileType.AUDIO || audioExtensions.contains(item.extension.lowercase())) {
                                val canonical = item.path
                                if (seenPaths.add(canonical)) {
                                    val f = File(item.path)
                                    tracksList.add(
                                        MusicTrack(
                                            fileItem = item,
                                            title = item.name.substringBeforeLast('.'),
                                            artist = "Unknown Artist",
                                            album = f.parentFile?.name ?: "Music",
                                            durationMs = 0L,
                                            format = item.extension.uppercase().ifBlank { "MP3" },
                                            folderPath = f.parent ?: "",
                                            folderName = f.parentFile?.name ?: "Music"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        tracksList
    }

    private suspend fun scanDirectoryForAudio(
        dir: File,
        depth: Int,
        maxDepth: Int,
        audioExtensions: Set<String>,
        seenPaths: MutableSet<String>,
        outList: MutableList<MusicTrack>
    ) {
        if (depth > maxDepth) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.name.startsWith(".")) continue
            if (file.isDirectory) {
                scanDirectoryForAudio(file, depth + 1, maxDepth, audioExtensions, seenPaths, outList)
            } else {
                val ext = file.extension.lowercase()
                if (audioExtensions.contains(ext) && file.length() > 0) {
                    val canonical = runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
                    if (seenPaths.add(canonical)) {
                        val ctx = context
                        val meta = if (ctx != null) {
                            runCatching { AudioMetadataHelper.extractMetadata(ctx, file) }.getOrNull()
                        } else null

                        val title = meta?.title ?: file.nameWithoutExtension
                        val artist = meta?.artist ?: "Unknown Artist"
                        val album = meta?.album ?: (file.parentFile?.name ?: "Unknown Album")
                        val durationMs = meta?.durationMs ?: 0L
                        val format = ext.uppercase()
                        val parentFile = file.parentFile
                        val folderName = parentFile?.name ?: "Music"
                        val folderPath = parentFile?.absolutePath ?: file.parent ?: ""

                        val fileItem = FileItem(
                            id = canonical,
                            name = file.name,
                            path = canonical,
                            sizeBytes = file.length(),
                            lastModifiedMs = file.lastModified(),
                            isDirectory = false,
                            fileType = FileType.AUDIO,
                            mimeType = "audio/*"
                        )

                        outList.add(
                            MusicTrack(
                                fileItem = fileItem,
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = durationMs,
                                format = format,
                                folderPath = folderPath,
                                folderName = folderName
                            )
                        )
                    }
                }
            }
        }
    }

    fun playTrack(track: FileItem) {
        val tracks = _uiState.value.libraryTracks.map { File(it.path) }
        val index = tracks.indexOfFirst { it.absolutePath == track.path }.coerceAtLeast(0)
        _uiState.update { it.copy(isNowPlayingExpanded = true) }
        MusicPlaybackManager.playQueue(tracks, index)
    }

    fun playTracks(tracks: List<MusicTrack>, startIndex: Int = 0) {
        val files = tracks.map { File(it.fileItem.path) }
        val index = startIndex.coerceIn(0, (files.size - 1).coerceAtLeast(0))
        _uiState.update { it.copy(isNowPlayingExpanded = true) }
        MusicPlaybackManager.playQueue(files, index)
    }

    fun expandNowPlaying() {
        _uiState.update { it.copy(isNowPlayingExpanded = true) }
    }

    fun collapseNowPlaying() {
        _uiState.update { it.copy(isNowPlayingExpanded = false) }
    }

    fun selectSubItem(title: String, tracks: List<MusicTrack>) {
        _uiState.update { it.copy(selectedSubItemTitle = title, selectedSubItemTracks = tracks) }
    }

    fun dismissSubItem() {
        _uiState.update { it.copy(selectedSubItemTitle = null, selectedSubItemTracks = null) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun togglePlayPause() = MusicPlaybackManager.togglePlayPause()
    fun seekTo(positionMs: Long) = MusicPlaybackManager.seekTo(positionMs)
    fun skipToNext() = MusicPlaybackManager.skipToNext()
    fun skipToPrevious() = MusicPlaybackManager.skipToPrevious()
    fun toggleRepeat() = MusicPlaybackManager.toggleRepeat()
    fun toggleShuffle() = MusicPlaybackManager.toggleShuffle()
    fun toggleFavorite() = MusicPlaybackManager.toggleFavorite()

    fun setLibraryTab(tab: MusicLibraryTab) {
        savedStateHandle?.set(KEY_MUSIC_TAB, tab.name)
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.update { it.copy(sleepTimerMinutesRemaining = null) }
            return
        }
        _uiState.update { it.copy(sleepTimerMinutesRemaining = minutes) }
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (isActive && remaining > 0) {
                delay(60_000L)
                remaining--
                _uiState.update { it.copy(sleepTimerMinutesRemaining = if (remaining > 0) remaining else null) }
            }
            if (isActive && MusicPlaybackManager.state.value.isPlaying) {
                MusicPlaybackManager.togglePlayPause()
            }
        }
    }

    fun toggleQueueSheet(visible: Boolean) {
        _uiState.update { it.copy(isQueueSheetVisible = visible) }
    }
}
