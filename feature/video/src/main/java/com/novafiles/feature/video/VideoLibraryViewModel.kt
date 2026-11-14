package com.novafiles.feature.video

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.media.VideoMetadataHelper
import com.novafiles.core.media.VideoTechnicalDetails
import com.novafiles.core.model.ConflictResolution
import com.novafiles.core.model.SortDirection
import com.novafiles.core.storage.FileOperationsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class VideoLibraryUiState(
    val currentSection: VideoSection = VideoSection.ALL_VIDEOS,
    val allVideos: List<VideoItem> = emptyList(),
    val folders: List<VideoFolderItem> = emptyList(),
    val playlists: List<VideoPlaylist> = emptyList(),
    val selectedFolder: VideoFolderItem? = null,
    val selectedPlaylist: VideoPlaylist? = null,
    val preferences: VideoPreferences = VideoPreferences(),
    val resumePoints: Map<String, PlaybackResumePoint> = emptyMap(),
    val selectedVideoIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val activeVideoDetails: VideoItem? = null,
    val activeTechnicalDetails: VideoTechnicalDetails? = null,
    val showSettingsSheet: Boolean = false,
    val showSortSheet: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false,
    val showRenamePlaylistDialog: VideoPlaylist? = null,
    val showDeletePlaylistDialog: VideoPlaylist? = null,
    val showAddToPlaylistDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showRenameVideoDialog: VideoItem? = null,
    val statusMessage: String? = null
) {
    val selectedCount: Int get() = selectedVideoIds.size

    val activeVideoList: List<VideoItem>
        get() {
            val baseList = when {
                selectedFolder != null -> allVideos.filter {
                    val folderNorm = selectedFolder.path.replace('\\', '/')
                    val parentNorm = it.file.parent?.replace('\\', '/') ?: ""
                    parentNorm == folderNorm || it.file.parentFile?.absolutePath == File(selectedFolder.path).absolutePath
                }
                selectedPlaylist != null -> {
                    val pathMap = allVideos.associateBy { it.path }
                    selectedPlaylist.videoPaths.mapNotNull { pathMap[it] }
                }
                else -> allVideos
            }

            val searchFiltered = if (searchQuery.isBlank()) {
                baseList
            } else {
                baseList.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            // In custom playlist view, preserve custom playlist order unless sorted
            if (selectedPlaylist != null &&
                preferences.sortConfig.sortBy == VideoSortBy.DATE_MODIFIED &&
                preferences.sortConfig.direction == SortDirection.DESCENDING
            ) {
                return searchFiltered
            }

            val comparator = Comparator<VideoItem> { a, b ->
                val comp = when (preferences.sortConfig.sortBy) {
                    VideoSortBy.DATE_MODIFIED -> a.dateModified.compareTo(b.dateModified)
                    VideoSortBy.DATE_ADDED -> a.dateAdded.compareTo(b.dateAdded)
                    VideoSortBy.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                    VideoSortBy.SIZE -> a.sizeBytes.compareTo(b.sizeBytes)
                    VideoSortBy.DURATION -> a.durationMs.compareTo(b.durationMs)
                }
                if (preferences.sortConfig.direction == SortDirection.ASCENDING) comp else -comp
            }

            return searchFiltered.sortedWith(comparator)
        }

    val dateGroups: List<VideoDateGroup>
        get() {
            val items = activeVideoList
            if (items.isEmpty()) return emptyList()

            val calendar = Calendar.getInstance()
            val todayYear = calendar.get(Calendar.YEAR)
            val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

            val grouped = items.groupBy { item ->
                val time = if (item.dateModified > 0) item.dateModified else item.dateAdded
                calendar.timeInMillis = time
                val itemYear = calendar.get(Calendar.YEAR)
                val itemDay = calendar.get(Calendar.DAY_OF_YEAR)

                when {
                    itemYear == todayYear && itemDay == todayDay -> "Today"
                    itemYear == todayYear && itemDay == todayDay - 1 -> "Yesterday"
                    itemYear == todayYear -> SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(time))
                    else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(time))
                }
            }

            return grouped.map { (dateTitle, groupItems) ->
                VideoDateGroup(title = dateTitle, items = groupItems)
            }
        }
}

class VideoLibraryViewModel @JvmOverloads constructor(
    application: Application,
    private val preferencesRepo: VideoPreferencesRepository = VideoPreferencesRepository(application),
    private val playlistsRepo: VideoPlaylistsRepository = VideoPlaylistsRepository(application),
    private val fileOperationsEngine: FileOperationsEngine = FileOperationsEngine()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoLibraryUiState())
    val uiState: StateFlow<VideoLibraryUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        observePlaylists()
        observeResumePoints()
        loadVideos()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepo.preferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            playlistsRepo.playlists.collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun observeResumePoints() {
        viewModelScope.launch {
            preferencesRepo.resumePoints.collect { points ->
                _uiState.update { it.copy(resumePoints = points) }
            }
        }
    }

    fun loadVideoLibrary() = loadVideos()

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val videos = withContext(Dispatchers.IO) {
                scanVideos(getApplication())
            }
            _uiState.update { current ->
                current.copy(
                    allVideos = videos,
                    isLoading = false
                )
            }
            recomputeFolders()
        }
    }

    private fun recomputeFolders() {
        val videos = _uiState.value.allVideos
        val folderMap = mutableMapOf<String, MutableList<VideoItem>>()
        for (item in videos) {
            val parent = item.file.parentFile ?: continue
            folderMap.getOrPut(parent.absolutePath) { mutableListOf() }.add(item)
        }

        val foldersList = folderMap.map { (path, folderVideos) ->
            val folderFile = File(path)
            val sortedByMod = folderVideos.sortedByDescending { it.dateModified }
            VideoFolderItem(
                path = path,
                name = folderFile.name.ifBlank { "Videos" },
                coverItem = sortedByMod.firstOrNull(),
                videoCount = folderVideos.size,
                totalDurationMs = folderVideos.sumOf { it.durationMs },
                totalSizeBytes = folderVideos.sumOf { it.sizeBytes },
                lastModified = sortedByMod.firstOrNull()?.dateModified ?: folderFile.lastModified()
            )
        }.sortedByDescending { it.lastModified }

        _uiState.update { current ->
            current.copy(folders = foldersList)
        }
    }

    private fun scanVideos(context: Context): List<VideoItem> {
        val result = mutableListOf<VideoItem>()
        val seenPaths = mutableSetOf<String>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE
        )

        try {
            val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val cursor = context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val bucketCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateModCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val dateAddCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val widthCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                while (it.moveToNext()) {
                    val path = it.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.exists() || file.length() == 0L) continue

                    val id = it.getLong(idCol).toString()
                    val name = it.getString(nameCol) ?: file.name
                    val bucket = it.getString(bucketCol) ?: file.parentFile?.name ?: "Videos"
                    val size = it.getLong(sizeCol)
                    val duration = it.getLong(durationCol)
                    val dateMod = it.getLong(dateModCol) * 1000L
                    val dateAdd = it.getLong(dateAddCol) * 1000L
                    val width = it.getInt(widthCol)
                    val height = it.getInt(heightCol)
                    val mime = it.getString(mimeCol) ?: "video/mp4"

                    seenPaths.add(file.absolutePath)
                    result.add(
                        VideoItem(
                            id = id,
                            path = path,
                            name = name,
                            durationMs = duration,
                            sizeBytes = if (size > 0) size else file.length(),
                            dateModified = if (dateMod > 0) dateMod else file.lastModified(),
                            dateAdded = dateAdd,
                            width = width,
                            height = height,
                            mimeType = mime,
                            folderPath = file.parentFile?.absolutePath ?: "",
                            folderName = bucket
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Filesystem scan fallback for common video locations
        val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "flv", "wmv", "3gp", "ts", "m4v")
        val dirsToScan = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File(Environment.getExternalStorageDirectory(), "Movies"),
            File(Environment.getExternalStorageDirectory(), "DCIM/Camera"),
            File(Environment.getExternalStorageDirectory(), "Download"),
            Environment.getExternalStorageDirectory()
        )

        for (dir in dirsToScan) {
            if (!dir.exists() || !dir.isDirectory) continue
            dir.listFiles()?.forEach { f ->
                if (f.isFile && f.extension.lowercase() in videoExtensions && f.absolutePath !in seenPaths) {
                    seenPaths.add(f.absolutePath)
                    result.add(
                        VideoItem(
                            id = f.absolutePath.hashCode().toString(),
                            path = f.absolutePath,
                            name = f.name,
                            durationMs = 0L,
                            sizeBytes = f.length(),
                            dateModified = f.lastModified(),
                            dateAdded = f.lastModified(),
                            width = 0,
                            height = 0,
                            mimeType = "video/${f.extension.lowercase()}",
                            folderPath = f.parentFile?.absolutePath ?: "",
                            folderName = f.parentFile?.name ?: "Videos"
                        )
                    )
                }
            }
        }

        return result
    }

    // ==========================================
    // Navigation & Section Management
    // ==========================================

    fun selectSection(section: VideoSection) {
        _uiState.update {
            it.copy(
                currentSection = section,
                selectedFolder = null,
                selectedPlaylist = null,
                selectedVideoIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun selectFolder(folder: VideoFolderItem?) {
        _uiState.update {
            it.copy(
                selectedFolder = folder,
                selectedPlaylist = null,
                selectedVideoIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun selectPlaylist(playlist: VideoPlaylist?) {
        _uiState.update {
            it.copy(
                selectedPlaylist = playlist,
                selectedFolder = null,
                selectedVideoIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    // ==========================================
    // Selection Management
    // ==========================================

    fun toggleSelection(videoId: String) {
        _uiState.update { current ->
            val updated = current.selectedVideoIds.toMutableSet()
            if (updated.contains(videoId)) {
                updated.remove(videoId)
            } else {
                updated.add(videoId)
            }
            current.copy(
                selectedVideoIds = updated,
                isSelectionMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        val allIds = _uiState.value.activeVideoList.map { it.id }.toSet()
        _uiState.update { it.copy(selectedVideoIds = allIds, isSelectionMode = true) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedVideoIds = emptySet(), isSelectionMode = false) }
    }

    // ==========================================
    // Search & Filter
    // ==========================================

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch(open: Boolean) {
        _uiState.update { it.copy(isSearchOpen = open, searchQuery = if (!open) "" else it.searchQuery) }
    }

    // ==========================================
    // Settings & Dialogs
    // ==========================================

    fun showSettingsSheet(show: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = show) }
    }

    fun showSortSheet(show: Boolean) {
        _uiState.update { it.copy(showSortSheet = show) }
    }

    fun showCreatePlaylistDialog(show: Boolean) {
        _uiState.update { it.copy(showCreatePlaylistDialog = show) }
    }

    fun showRenamePlaylistDialog(playlist: VideoPlaylist?) {
        _uiState.update { it.copy(showRenamePlaylistDialog = playlist) }
    }

    fun showDeletePlaylistDialog(playlist: VideoPlaylist?) {
        _uiState.update { it.copy(showDeletePlaylistDialog = playlist) }
    }

    fun showAddToPlaylistDialog(show: Boolean) {
        _uiState.update { it.copy(showAddToPlaylistDialog = show) }
    }

    fun showDeleteConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmDialog = show) }
    }

    fun showRenameVideoDialog(video: VideoItem?) {
        _uiState.update { it.copy(showRenameVideoDialog = video) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    // ==========================================
    // Video Metadata & Technical Details
    // ==========================================

    fun showVideoDetails(video: VideoItem?) {
        _uiState.update { it.copy(activeVideoDetails = video, activeTechnicalDetails = null) }
        if (video != null) {
            viewModelScope.launch {
                val details = VideoMetadataHelper.extractTechnicalDetails(video.file)
                _uiState.update { it.copy(activeTechnicalDetails = details) }
            }
        }
    }

    // ==========================================
    // Playlist Operations
    // ==========================================

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val playlist = playlistsRepo.createPlaylist(name)
            if (_uiState.value.selectedVideoIds.isNotEmpty()) {
                val selectedPaths = _uiState.value.allVideos
                    .filter { it.id in _uiState.value.selectedVideoIds }
                    .map { it.path }
                playlistsRepo.addVideosToPlaylist(playlist.id, selectedPaths)
                clearSelection()
            }
            showCreatePlaylistDialog(false)
            _uiState.update { it.copy(statusMessage = "Playlist \"$name\" created") }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            playlistsRepo.renamePlaylist(playlistId, newName)
            showRenamePlaylistDialog(null)
            if (_uiState.value.selectedPlaylist?.id == playlistId) {
                _uiState.update { it.copy(selectedPlaylist = it.selectedPlaylist?.copy(name = newName)) }
            }
            _uiState.update { it.copy(statusMessage = "Playlist renamed") }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistsRepo.deletePlaylist(playlistId)
            showDeletePlaylistDialog(null)
            if (_uiState.value.selectedPlaylist?.id == playlistId) {
                _uiState.update { it.copy(selectedPlaylist = null) }
            }
            _uiState.update { it.copy(statusMessage = "Playlist deleted") }
        }
    }

    fun addSelectedToPlaylist(playlistId: String) {
        val selectedPaths = _uiState.value.allVideos
            .filter { it.id in _uiState.value.selectedVideoIds }
            .map { it.path }
        if (selectedPaths.isNotEmpty()) {
            viewModelScope.launch {
                playlistsRepo.addVideosToPlaylist(playlistId, selectedPaths)
                showAddToPlaylistDialog(false)
                clearSelection()
                _uiState.update { it.copy(statusMessage = "Added ${selectedPaths.size} videos to playlist") }
            }
        }
    }

    fun removeSelectedFromCurrentPlaylist() {
        val playlist = _uiState.value.selectedPlaylist ?: return
        val selectedPaths = _uiState.value.allVideos
            .filter { it.id in _uiState.value.selectedVideoIds }
            .map { it.path }
        if (selectedPaths.isNotEmpty()) {
            viewModelScope.launch {
                playlistsRepo.removeVideosFromPlaylist(playlist.id, selectedPaths)
                val updatedPaths = playlist.videoPaths.filterNot { it in selectedPaths.toSet() }
                _uiState.update {
                    it.copy(
                        selectedPlaylist = playlist.copy(videoPaths = updatedPaths),
                        statusMessage = "Removed ${selectedPaths.size} videos from playlist"
                    )
                }
                clearSelection()
            }
        }
    }

    // ==========================================
    // File Operations
    // ==========================================

    fun renameVideo(video: VideoItem, newName: String) {
        val ext = video.file.extension
        val finalName = if (newName.contains(".")) newName else if (ext.isNotEmpty()) "$newName.$ext" else newName

        viewModelScope.launch {
            val result = fileOperationsEngine.renameFile(video.file, finalName)
            if (result.isSuccess) {
                showRenameVideoDialog(null)
                loadVideos()
                _uiState.update { it.copy(statusMessage = "Video renamed to $finalName") }
            } else {
                _uiState.update { it.copy(statusMessage = "Failed to rename: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun deleteSingleVideo(video: VideoItem) {
        viewModelScope.launch {
            try {
                fileOperationsEngine.deleteFiles(listOf(video.file)).collect()
                loadVideos()
                _uiState.update { it.copy(statusMessage = "Video deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Delete failed: ${e.message}") }
            }
        }
    }

    fun deleteSelected() {
        val selectedFiles = _uiState.value.allVideos
            .filter { it.id in _uiState.value.selectedVideoIds }
            .map { it.file }
        if (selectedFiles.isEmpty()) return

        viewModelScope.launch {
            try {
                fileOperationsEngine.deleteFiles(selectedFiles).collect()
                showDeleteConfirmDialog(false)
                clearSelection()
                loadVideos()
                _uiState.update { it.copy(statusMessage = "Deleted ${selectedFiles.size} videos") }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Delete failed: ${e.message}") }
            }
        }
    }

    fun shareSelected(context: Context) {
        val selectedFiles = _uiState.value.allVideos
            .filter { it.id in _uiState.value.selectedVideoIds }
            .map { it.file }
        if (selectedFiles.isEmpty()) return

        try {
            val uris = selectedFiles.map { file ->
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }

            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "video/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(intent, "Share Videos"))
        } catch (_: Exception) {}
    }

    // ==========================================
    // Preferences Updates
    // ==========================================

    fun setViewMode(viewMode: VideoViewMode) {
        viewModelScope.launch { preferencesRepo.updateViewMode(viewMode) }
    }

    fun setThumbnailSize(size: VideoThumbnailSize) {
        viewModelScope.launch { preferencesRepo.updateThumbnailSize(size) }
    }

    fun setGridSpacing(spacingDp: Int) {
        viewModelScope.launch { preferencesRepo.updateGridSpacing(spacingDp) }
    }

    fun setShowDurationOverlay(show: Boolean) {
        viewModelScope.launch { preferencesRepo.updateShowDurationOverlay(show) }
    }

    fun setSortConfig(config: VideoSortConfig) {
        viewModelScope.launch { preferencesRepo.updateSortConfig(config) }
    }

    fun setGroupByDate(group: Boolean) {
        viewModelScope.launch { preferencesRepo.updateGroupByDate(group) }
    }

    fun setRememberPlaybackPosition(remember: Boolean) {
        viewModelScope.launch { preferencesRepo.updateRememberPlaybackPosition(remember) }
    }

    fun setDefaultSubtitleEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.updateDefaultSubtitleEnabled(enabled) }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch { preferencesRepo.updateDefaultPlaybackSpeed(speed) }
    }

    fun setOrientationPreference(pref: VideoOrientationPref) {
        viewModelScope.launch { preferencesRepo.updateOrientationPreference(pref) }
    }
}
