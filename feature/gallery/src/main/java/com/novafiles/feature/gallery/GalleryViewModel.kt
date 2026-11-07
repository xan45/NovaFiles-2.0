package com.novafiles.feature.gallery

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.model.ConflictResolution
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

data class GalleryItem(
    val file: File,
    val id: String = file.absolutePath,
    val name: String = file.name,
    val album: String,
    val sizeBytes: Long = file.length(),
    val lastModified: Long = file.lastModified()
)

data class GalleryUiState(
    val allItems: List<GalleryItem> = emptyList(),
    val filteredItems: List<GalleryItem> = emptyList(),
    val availableAlbums: List<String> = listOf("All"),
    val selectedAlbum: String = "All",
    val selectedItemIds: Set<String> = emptySet(),
    val favorites: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val activeViewerIndex: Int? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val showMovePickerDialog: Boolean = false,
    val showCopyPickerDialog: Boolean = false
) {
    val selectedCount: Int get() = selectedItemIds.size
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val fileOperationsEngine = FileOperationsEngine()
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = withContext(Dispatchers.IO) {
                scanImages(getApplication())
            }

            val albums = mutableListOf("All")
            albums.addAll(items.map { it.album }.distinct().sorted())

            _uiState.update { state ->
                val filtered = if (state.selectedAlbum == "All") items else items.filter { it.album == state.selectedAlbum }
                state.copy(
                    allItems = items,
                    filteredItems = filtered,
                    availableAlbums = albums,
                    isLoading = false
                )
            }
        }
    }

    private fun scanImages(context: Context): List<GalleryItem> {
        val results = mutableListOf<GalleryItem>()
        val seenPaths = mutableSetOf<String>()

        // 1. MediaStore query
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED
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

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (file.exists() && file.length() > 0) {
                        val album = if (bucketCol >= 0) cursor.getString(bucketCol) ?: file.parentFile?.name ?: "Pictures" else file.parentFile?.name ?: "Pictures"
                        seenPaths.add(file.absolutePath)
                        results.add(
                            GalleryItem(
                                file = file,
                                album = album
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Direct directory fallback (DCIM, Pictures, Download)
        val candidateDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )

        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")

        for (dir in candidateDirs) {
            if (dir != null && dir.exists()) {
                dir.walkTopDown().maxDepth(3).filter { it.isFile && it.extension.lowercase() in imageExtensions }.forEach { f ->
                    if (!seenPaths.contains(f.absolutePath)) {
                        seenPaths.add(f.absolutePath)
                        val album = f.parentFile?.name ?: "Pictures"
                        results.add(GalleryItem(file = f, album = album))
                    }
                }
            }
        }

        return results.sortedByDescending { it.lastModified }
    }

    fun selectAlbum(albumName: String) {
        _uiState.update { state ->
            val filtered = if (albumName == "All") state.allItems else state.allItems.filter { it.album == albumName }
            state.copy(
                selectedAlbum = albumName,
                filteredItems = filtered,
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

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
            val allIds = state.filteredItems.map { it.id }.toSet()
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
    }

    fun isFavorite(id: String): Boolean = _uiState.value.favorites.contains(id)

    fun openViewer(index: Int) {
        _uiState.update { it.copy(activeViewerIndex = index) }
    }

    fun closeViewer() {
        _uiState.update { it.copy(activeViewerIndex = null) }
    }

    fun showDeleteConfirm(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmDialog = show) }
    }

    fun showMovePicker(show: Boolean) {
        _uiState.update { it.copy(showMovePickerDialog = show) }
    }

    fun showCopyPicker(show: Boolean) {
        _uiState.update { it.copy(showCopyPickerDialog = show) }
    }

    fun deleteSelected(onDone: () -> Unit = {}) {
        val selectedIds = _uiState.value.selectedItemIds
        val filesToDelete = _uiState.value.allItems.filter { selectedIds.contains(it.id) }.map { it.file }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fileOperationsEngine.deleteFiles(filesToDelete).collect()
            }
            clearSelection()
            showDeleteConfirm(false)
            loadMedia()
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
}
