package com.novafiles.feature.gallery

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

data class GalleryUiState(
    val currentSection: GallerySection = GallerySection.ALL_IMAGES,
    val allItems: List<GalleryItem> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
    val userAlbums: List<UserAlbum> = emptyList(),
    val selectedFolder: FolderItem? = null,
    val selectedUserAlbum: UserAlbum? = null,
    val preferences: GalleryPreferences = GalleryPreferences(),
    val selectedItemIds: Set<String> = emptySet(),
    val favorites: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val isOnlyFavorites: Boolean = false,
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val activeViewerIndex: Int? = null,
    val showSettingsSheet: Boolean = false,
    val showAddToAlbumDialog: Boolean = false,
    val showCreateAlbumDialog: Boolean = false,
    val showRenameAlbumDialog: UserAlbum? = null,
    val showDeleteAlbumDialog: UserAlbum? = null,
    val showExcludeFolderDialog: FolderItem? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val showMovePickerDialog: Boolean = false,
    val showCopyPickerDialog: Boolean = false,
    val showRenameImageDialog: GalleryItem? = null,
    val statusMessage: String? = null
) {
    val selectedCount: Int get() = selectedItemIds.size

    val activeImageList: List<GalleryItem>
        get() {
            val baseList = when {
                selectedFolder != null -> allItems.filter {
                    val folderNorm = selectedFolder.path.replace('\\', '/')
                    val parentNorm = it.file.parent?.replace('\\', '/') ?: ""
                    parentNorm == folderNorm || it.file.parentFile?.absolutePath == File(selectedFolder.path).absolutePath
                }
                selectedUserAlbum != null -> {
                    val pathMap = allItems.associateBy { it.id }
                    selectedUserAlbum.imagePaths.mapNotNull { pathMap[it] }
                }
                else -> allItems
            }

            val searchFiltered = if (searchQuery.isBlank()) {
                baseList
            } else {
                baseList.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            val favFiltered = if (isOnlyFavorites && selectedUserAlbum == null && selectedFolder == null) {
                searchFiltered.filter { favorites.contains(it.id) }
            } else {
                searchFiltered
            }

            // In user albums, preserve custom order unless specifically sorted
            if (selectedUserAlbum != null && preferences.sortConfig.sortBy == GallerySortBy.DATE_MODIFIED && preferences.sortConfig.direction == SortDirection.DESCENDING) {
                // Return in album order
                return favFiltered
            }

            val comparator = Comparator<GalleryItem> { a, b ->
                val comp = when (preferences.sortConfig.sortBy) {
                    GallerySortBy.DATE_MODIFIED -> a.lastModified.compareTo(b.lastModified)
                    GallerySortBy.DATE_TAKEN -> (a.dateTaken ?: a.lastModified).compareTo(b.dateTaken ?: b.lastModified)
                    GallerySortBy.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                    GallerySortBy.SIZE -> a.sizeBytes.compareTo(b.sizeBytes)
                }
                if (preferences.sortConfig.direction == SortDirection.DESCENDING) -comp else comp
            }
            return favFiltered.sortedWith(comparator)
        }

    val dateGroups: List<DateGroup>
        get() {
            if (!preferences.groupByDate || selectedFolder != null || selectedUserAlbum != null) {
                return emptyList()
            }
            val items = activeImageList
            if (items.isEmpty()) return emptyList()

            val calendar = Calendar.getInstance()
            val todayYear = calendar.get(Calendar.YEAR)
            val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

            val grouped = items.groupBy { item ->
                val time = item.dateTaken ?: item.lastModified
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
                DateGroup(title = dateTitle, items = groupItems)
            }
        }
}

class GalleryViewModel @JvmOverloads constructor(
    application: Application,
    private val preferencesRepo: GalleryPreferencesRepository = GalleryPreferencesRepository(application),
    private val albumsRepo: UserAlbumsRepository = UserAlbumsRepository(application),
    private val fileOperationsEngine: FileOperationsEngine = FileOperationsEngine()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val favoritesFile = File(application.filesDir, "gallery_favorites.json")

    init {
        loadFavorites()
        observePreferences()
        observeUserAlbums()
        loadMedia()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepo.preferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
                // Rescan or re-filter when hidden/excluded folders toggle
                recomputeFoldersAndFiltered()
            }
        }
    }

    private fun observeUserAlbums() {
        viewModelScope.launch {
            albumsRepo.albums.collect { albums ->
                _uiState.update { it.copy(userAlbums = albums) }
            }
        }
    }

    private fun loadFavorites() {
        try {
            if (favoritesFile.exists()) {
                val lines = favoritesFile.readLines().filter { it.isNotBlank() }
                _uiState.update { it.copy(favorites = lines.toSet()) }
            }
        } catch (_: Exception) {}
    }

    private fun saveFavorites() {
        try {
            favoritesFile.writeText(_uiState.value.favorites.joinToString("\n"))
        } catch (_: Exception) {}
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = withContext(Dispatchers.IO) {
                scanImages(getApplication())
            }

            _uiState.update { state ->
                state.copy(
                    allItems = items,
                    isLoading = false
                )
            }
            recomputeFoldersAndFiltered()
        }
    }

    private fun recomputeFoldersAndFiltered() {
        val state = _uiState.value
        val showHidden = state.preferences.showHiddenFolders
        val excluded = state.preferences.excludedFolderPaths

        // Filter items based on hidden & excluded folders
        val visibleItems = state.allItems.filterNot { item ->
            val parent = item.file.parentFile ?: return@filterNot false
            isFolderHiddenOrExcluded(parent, showHidden, excluded)
        }

        // Group into folders
        val folderMap = mutableMapOf<String, MutableList<GalleryItem>>()
        visibleItems.forEach { item ->
            val parent = item.file.parentFile
            if (parent != null) {
                folderMap.getOrPut(parent.absolutePath) { mutableListOf() }.add(item)
            }
        }

        val foldersList = folderMap.map { (path, folderItems) ->
            val folderFile = File(path)
            val sortedByMod = folderItems.sortedByDescending { it.lastModified }
            val isHidden = folderFile.name.startsWith(".") || File(folderFile, ".nomedia").exists()
            FolderItem(
                path = path,
                name = folderFile.name,
                coverItem = sortedByMod.firstOrNull(),
                imageCount = folderItems.size,
                totalSizeBytes = folderItems.sumOf { it.sizeBytes },
                lastModified = sortedByMod.firstOrNull()?.lastModified ?: folderFile.lastModified(),
                isHidden = isHidden
            )
        }.sortedByDescending { it.imageCount }

        _uiState.update { current ->
            current.copy(folders = foldersList)
        }
    }

    private fun isFolderHiddenOrExcluded(dir: File, showHidden: Boolean, excludedPaths: Set<String>): Boolean {
        val pathNorm = dir.absolutePath.replace('\\', '/')
        if (excludedPaths.any { it.replace('\\', '/') == pathNorm }) return true
        if (!showHidden) {
            var current: File? = dir
            while (current != null && current.parent != null) {
                val name = current.name
                if (name.startsWith(".") ||
                    name.equals(".thumbnails", ignoreCase = true) ||
                    name.equals("thumbnails", ignoreCase = true) ||
                    name.equals(".cache", ignoreCase = true) ||
                    name.equals("cache", ignoreCase = true)
                ) {
                    return true
                }
                if (File(current, ".nomedia").exists()) {
                    return true
                }
                current = current.parentFile
            }
        }
        return false
    }

    private fun scanImages(context: Context): List<GalleryItem> {
        val results = mutableListOf<GalleryItem>()
        val seenPaths = mutableSetOf<String>()

        // 1. MediaStore query with rich projection
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.MIME_TYPE
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            val query = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            query?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val modCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val takenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (file.exists() && file.length() > 0) {
                        seenPaths.add(file.absolutePath)
                        val album = if (bucketCol >= 0) cursor.getString(bucketCol) ?: file.parentFile?.name ?: "Pictures" else file.parentFile?.name ?: "Pictures"
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else file.length()
                        val mod = if (modCol >= 0) cursor.getLong(modCol) * 1000L else file.lastModified()
                        val taken = if (takenCol >= 0) cursor.getLong(takenCol).takeIf { it > 0 } else null
                        val width = if (widthCol >= 0) cursor.getInt(widthCol).takeIf { it > 0 } else null
                        val height = if (heightCol >= 0) cursor.getInt(heightCol).takeIf { it > 0 } else null
                        val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null

                        results.add(
                            GalleryItem(
                                file = file,
                                album = album,
                                sizeBytes = size,
                                lastModified = mod,
                                dateTaken = taken,
                                width = width,
                                height = height,
                                mimeType = mime
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Direct directory fallback (DCIM, Pictures, Download)
        val candidateDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        )

        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "avif")

        for (dir in candidateDirs) {
            if (dir != null && dir.exists()) {
                dir.walkTopDown().maxDepth(5).filter { it.isFile && it.extension.lowercase() in imageExtensions }.forEach { f ->
                    if (!seenPaths.contains(f.absolutePath) && f.length() > 0) {
                        seenPaths.add(f.absolutePath)
                        val album = f.parentFile?.name ?: "Pictures"
                        results.add(
                            GalleryItem(
                                file = f,
                                album = album,
                                sizeBytes = f.length(),
                                lastModified = f.lastModified(),
                                mimeType = "image/${f.extension.lowercase()}"
                            )
                        )
                    }
                }
            }
        }

        return results.sortedByDescending { it.lastModified }
    }

    // ==========================================
    // Navigation & Section Management
    // ==========================================

    fun selectSection(section: GallerySection) {
        _uiState.update {
            it.copy(
                currentSection = section,
                selectedFolder = null,
                selectedUserAlbum = null,
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun selectFolder(folder: FolderItem?) {
        _uiState.update {
            it.copy(
                selectedFolder = folder,
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun selectUserAlbum(album: UserAlbum?) {
        _uiState.update {
            it.copy(
                selectedUserAlbum = album,
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch(isOpen: Boolean) {
        _uiState.update {
            it.copy(
                isSearchOpen = isOpen,
                searchQuery = if (!isOpen) "" else it.searchQuery
            )
        }
    }

    fun toggleOnlyFavorites() {
        _uiState.update { it.copy(isOnlyFavorites = !it.isOnlyFavorites) }
    }

    // ==========================================
    // Selection Management
    // ==========================================

    fun toggleItemSelection(id: String) {
        _uiState.update { state ->
            val updated = state.selectedItemIds.toMutableSet()
            if (updated.contains(id)) {
                updated.remove(id)
            } else {
                updated.add(id)
            }
            state.copy(
                selectedItemIds = updated,
                isSelectionMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.activeImageList.map { it.id }.toSet()
            state.copy(
                selectedItemIds = allIds,
                isSelectionMode = allIds.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    // ==========================================
    // Favorites Management
    // ==========================================

    fun toggleFavorite(id: String) {
        _uiState.update { state ->
            val favs = state.favorites.toMutableSet()
            if (favs.contains(id)) {
                favs.remove(id)
            } else {
                favs.add(id)
            }
            state.copy(favorites = favs)
        }
        saveFavorites()
    }

    fun isFavorite(id: String): Boolean = _uiState.value.favorites.contains(id)

    // ==========================================
    // Viewer Management
    // ==========================================

    fun openViewer(index: Int) {
        _uiState.update { it.copy(activeViewerIndex = index) }
    }

    fun closeViewer() {
        _uiState.update { it.copy(activeViewerIndex = null) }
    }

    // ==========================================
    // Dialog Controls
    // ==========================================

    fun toggleSettingsSheet(show: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = show) }
    }

    fun toggleAddToAlbumDialog(show: Boolean) {
        _uiState.update { it.copy(showAddToAlbumDialog = show) }
    }

    fun toggleCreateAlbumDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateAlbumDialog = show) }
    }

    fun showRenameAlbumDialog(album: UserAlbum?) {
        _uiState.update { it.copy(showRenameAlbumDialog = album) }
    }

    fun showDeleteAlbumDialog(album: UserAlbum?) {
        _uiState.update { it.copy(showDeleteAlbumDialog = album) }
    }

    fun showExcludeFolderDialog(folder: FolderItem?) {
        _uiState.update { it.copy(showExcludeFolderDialog = folder) }
    }

    fun showDeleteConfirm(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmDialog = show) }
    }

    fun showRenameImageDialog(item: GalleryItem?) {
        _uiState.update { it.copy(showRenameImageDialog = item) }
    }

    fun showMovePicker(show: Boolean) {
        _uiState.update { it.copy(showMovePickerDialog = show) }
    }

    fun showCopyPicker(show: Boolean) {
        _uiState.update { it.copy(showCopyPickerDialog = show) }
    }

    // ==========================================
    // Album Operations
    // ==========================================

    fun createAlbum(name: String) {
        viewModelScope.launch {
            val album = albumsRepo.createAlbum(name)
            if (_uiState.value.selectedItemIds.isNotEmpty()) {
                albumsRepo.addImagesToAlbum(album.id, _uiState.value.selectedItemIds)
                clearSelection()
            }
            toggleCreateAlbumDialog(false)
            _uiState.update { it.copy(statusMessage = "Album \"$name\" created") }
        }
    }

    fun renameAlbum(albumId: String, newName: String) {
        viewModelScope.launch {
            albumsRepo.renameAlbum(albumId, newName)
            showRenameAlbumDialog(null)
            if (_uiState.value.selectedUserAlbum?.id == albumId) {
                _uiState.update { it.copy(selectedUserAlbum = it.selectedUserAlbum?.copy(name = newName)) }
            }
            _uiState.update { it.copy(statusMessage = "Album renamed") }
        }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            albumsRepo.deleteAlbum(albumId)
            showDeleteAlbumDialog(null)
            if (_uiState.value.selectedUserAlbum?.id == albumId) {
                _uiState.update { it.copy(selectedUserAlbum = null) }
            }
            _uiState.update { it.copy(statusMessage = "Album deleted") }
        }
    }

    fun addSelectedToAlbum(albumId: String) {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            albumsRepo.addImagesToAlbum(albumId, selectedIds)
            clearSelection()
            toggleAddToAlbumDialog(false)
            _uiState.update { it.copy(statusMessage = "Added ${selectedIds.size} images to album") }
        }
    }

    fun removeSelectedFromCurrentAlbum() {
        val album = _uiState.value.selectedUserAlbum ?: return
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            albumsRepo.removeImagesFromAlbum(album.id, selectedIds)
            clearSelection()
            // Update active selected album
            val updated = album.copy(imagePaths = album.imagePaths.filterNot { selectedIds.contains(it) })
            _uiState.update { it.copy(selectedUserAlbum = updated, statusMessage = "Removed ${selectedIds.size} images from album") }
        }
    }

    // ==========================================
    // Folder Operations
    // ==========================================

    fun excludeFolder(path: String) {
        viewModelScope.launch {
            preferencesRepo.excludeFolder(path)
            showExcludeFolderDialog(null)
            if (_uiState.value.selectedFolder?.path == path) {
                _uiState.update { it.copy(selectedFolder = null) }
            }
            _uiState.update { it.copy(statusMessage = "Folder excluded from gallery") }
        }
    }

    fun restoreFolder(path: String) {
        viewModelScope.launch {
            preferencesRepo.restoreFolder(path)
            _uiState.update { it.copy(statusMessage = "Folder restored to gallery") }
        }
    }

    // ==========================================
    // Preferences Management
    // ==========================================

    fun setThumbnailSize(size: ThumbnailSize) {
        viewModelScope.launch { preferencesRepo.updateThumbnailSize(size) }
    }

    fun setGridSpacing(spacingDp: Int) {
        viewModelScope.launch { preferencesRepo.updateGridSpacing(spacingDp) }
    }

    fun setRoundedThumbnails(isRounded: Boolean) {
        viewModelScope.launch { preferencesRepo.updateRoundedThumbnails(isRounded) }
    }

    fun setSortConfig(config: GallerySortConfig) {
        viewModelScope.launch { preferencesRepo.updateSortConfig(config) }
    }

    fun setGroupByDate(groupByDate: Boolean) {
        viewModelScope.launch { preferencesRepo.updateGroupByDate(groupByDate) }
    }

    fun setShowHiddenFolders(showHidden: Boolean) {
        viewModelScope.launch { preferencesRepo.updateShowHiddenFolders(showHidden) }
    }

    // ==========================================
    // File Operations (Rename, Delete, Copy, Move, Share)
    // ==========================================

    fun renameImage(item: GalleryItem, newName: String, onDone: (GalleryItem?) -> Unit = {}) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == item.name) {
            showRenameImageDialog(null)
            return
        }

        val parent = item.file.parentFile ?: return
        val ext = item.file.extension
        val finalName = if (ext.isNotEmpty() && !trimmed.endsWith(".$ext", ignoreCase = true)) {
            "$trimmed.$ext"
        } else {
            trimmed
        }

        val targetFile = File(parent, finalName)
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                item.file.renameTo(targetFile)
            }
            if (success) {
                val newItem = item.copy(
                    file = targetFile,
                    id = targetFile.absolutePath,
                    name = targetFile.name
                )

                // Update favorites & albums
                if (_uiState.value.favorites.contains(item.id)) {
                    val updatedFavs = (_uiState.value.favorites - item.id) + newItem.id
                    _uiState.update { it.copy(favorites = updatedFavs) }
                    saveFavorites()
                }

                // Update state items
                _uiState.update { state ->
                    val updatedAll = state.allItems.map { if (it.id == item.id) newItem else it }
                    state.copy(
                        allItems = updatedAll,
                        statusMessage = "Renamed to ${newItem.name}"
                    )
                }
                recomputeFoldersAndFiltered()
                showRenameImageDialog(null)
                onDone(newItem)
            } else {
                _uiState.update { it.copy(statusMessage = "Failed to rename image") }
                showRenameImageDialog(null)
                onDone(null)
            }
        }
    }

    fun deleteSingleImage(item: GalleryItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fileOperationsEngine.deleteFiles(listOf(item.file)).collect()
            }
            _uiState.update { state ->
                state.copy(
                    allItems = state.allItems.filter { it.id != item.id },
                    favorites = state.favorites - item.id,
                    statusMessage = "Deleted ${item.name}"
                )
            }
            saveFavorites()
            recomputeFoldersAndFiltered()
            onDone()
        }
    }

    fun deleteSelected(onDone: () -> Unit = {}) {
        val selectedIds = _uiState.value.selectedItemIds
        val filesToDelete = _uiState.value.allItems.filter { selectedIds.contains(it.id) }.map { it.file }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fileOperationsEngine.deleteFiles(filesToDelete).collect()
            }
            _uiState.update { state ->
                state.copy(
                    allItems = state.allItems.filterNot { selectedIds.contains(it.id) },
                    favorites = state.favorites - selectedIds,
                    selectedItemIds = emptySet(),
                    isSelectionMode = false,
                    showDeleteConfirmDialog = false,
                    statusMessage = "Deleted ${filesToDelete.size} images"
                )
            }
            saveFavorites()
            recomputeFoldersAndFiltered()
            onDone()
        }
    }

    fun copySelected(targetDir: File, onDone: () -> Unit = {}) {
        val selectedIds = _uiState.value.selectedItemIds
        val filesToCopy = _uiState.value.allItems.filter { selectedIds.contains(it.id) }.map { it.file }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fileOperationsEngine.copyFiles(filesToCopy, targetDir, ConflictResolution.RENAME_NEW).collect()
            }
            clearSelection()
            showCopyPicker(false)
            loadMedia()
            onDone()
        }
    }

    fun moveSelected(targetDir: File, onDone: () -> Unit = {}) {
        val selectedIds = _uiState.value.selectedItemIds
        val filesToMove = _uiState.value.allItems.filter { selectedIds.contains(it.id) }.map { it.file }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fileOperationsEngine.moveFiles(filesToMove, targetDir, ConflictResolution.RENAME_NEW).collect()
            }
            clearSelection()
            showMovePicker(false)
            loadMedia()
            onDone()
        }
    }

    fun shareSelected(context: Context) {
        val selectedIds = _uiState.value.selectedItemIds
        val filesToShare = _uiState.value.allItems.filter { selectedIds.contains(it.id) }.map { it.file }
        if (filesToShare.isEmpty()) return

        val uris = ArrayList<Uri>()
        filesToShare.forEach { file ->
            val uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
            uris.add(uri)
        }

        val shareIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        val chooser = Intent.createChooser(shareIntent, "Share Images").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
