package com.novafiles.feature.files

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.common.NovaResult
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.FileType
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.storage.ArchiveEngine
import com.novafiles.core.storage.FileOperationsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.security.MessageDigest
import com.novafiles.data.files.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class FilesViewModel(
    private val fileRepository: FileRepository,
    initialPath: String? = null
) : ViewModel() {

    private val rootPath = runCatching { Environment.getExternalStorageDirectory().absolutePath }.getOrDefault("/storage/emulated/0")
    private val operationsEngine = FileOperationsEngine()
    private val archiveEngine = ArchiveEngine()
    private val _uiState = MutableStateFlow(
        FilesUiState(
            currentPath = initialPath ?: rootPath,
            breadcrumbs = buildBreadcrumbs(initialPath ?: rootPath),
            bookmarks = listOf(
                BreadcrumbItem("Device", rootPath),
                BreadcrumbItem("Download", "$rootPath/Download"),
                BreadcrumbItem("Documents", "$rootPath/Documents"),
                BreadcrumbItem("Pictures", "$rootPath/Pictures"),
                BreadcrumbItem("Music", "$rootPath/Music")
            )
        )
    )
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init {
        loadDirectory(_uiState.value.currentPath)
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentPath = path,
                    breadcrumbs = buildBreadcrumbs(path),
                    isLoading = true,
                    errorMessage = null,
                    selectedIds = emptySet(),
                    isSelectionMode = false
                )
            }

            fileRepository.getFiles(
                directoryPath = path,
                sortConfig = _uiState.value.sortConfig
            ).collect { result ->
                when (result) {
                    is NovaResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NovaResult.Success -> {
                        val query = _uiState.value.searchQuery.trim()
                        val activeFilter = _uiState.value.activeFilter
                        val queryFiltered = if (query.isNotBlank()) {
                            result.data.filter { it.name.contains(query, ignoreCase = true) }
                        } else {
                            result.data
                        }
                        val folders = if (activeFilter == FileTypeFilter.ALL) {
                            queryFiltered.filter { it.isDirectory }
                        } else {
                            emptyList()
                        }
                        val files = queryFiltered.filter { !it.isDirectory }.filter { item ->
                            when (activeFilter) {
                                FileTypeFilter.ALL -> true
                                FileTypeFilter.IMAGES -> item.fileType == FileType.IMAGE
                                FileTypeFilter.VIDEOS -> item.fileType == FileType.VIDEO
                                FileTypeFilter.AUDIO -> item.fileType == FileType.AUDIO
                                FileTypeFilter.DOCUMENTS -> item.fileType == FileType.DOCUMENT
                                FileTypeFilter.ARCHIVES -> item.fileType == FileType.ARCHIVE
                                FileTypeFilter.APKS -> item.fileType == FileType.APK
                            }
                        }
                        _uiState.update {
                            it.copy(
                                folders = folders,
                                files = files,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                    is NovaResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message ?: "Failed to read directory"
                            )
                        }
                    }
                }
            }
        }
    }

    fun navigateToFolder(path: String) {
        loadDirectory(path)
    }

    fun navigateUp(): Boolean {
        val current = File(_uiState.value.currentPath)
        val parent = current.parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            loadDirectory(parent.absolutePath)
            return true
        }
        return false
    }

    fun toggleSelection(fileId: String) {
        _uiState.update { state ->
            val updated = state.selectedIds.toMutableSet()
            if (updated.contains(fileId)) {
                updated.remove(fileId)
            } else {
                updated.add(fileId)
            }
            state.copy(
                selectedIds = updated,
                isSelectionMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            if (state.isAllSelected) {
                state.copy(selectedIds = emptySet(), isSelectionMode = false)
            } else {
                val allIds = (state.folders + state.files).map { it.id }.toSet()
                state.copy(selectedIds = allIds, isSelectionMode = true)
            }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun setSortBy(sortBy: SortBy) {
        val current = _uiState.value.sortConfig
        val newDirection = if (current.sortBy == sortBy) {
            if (current.direction == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
        } else {
            SortDirection.DESCENDING
        }
        _uiState.update { it.copy(sortConfig = current.copy(sortBy = sortBy, direction = newDirection)) }
        loadDirectory(_uiState.value.currentPath)
    }

    fun toggleLayoutMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadDirectory(_uiState.value.currentPath)
    }

    fun getSelectedFileItems(): List<FileItem> {
        val ids = _uiState.value.selectedIds
        return (_uiState.value.folders + _uiState.value.files).filter { ids.contains(it.id) }
    }

    fun copySelected() {
        val selectedFiles = getSelectedFileItems()
        if (selectedFiles.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    clipboard = ClipboardState(files = selectedFiles, op = ClipboardOp.COPY),
                    selectedIds = emptySet(),
                    isSelectionMode = false,
                    operationMessage = "Copied ${selectedFiles.size} items to clipboard"
                )
            }
        }
    }

    fun moveSelected() {
        val selectedFiles = getSelectedFileItems()
        if (selectedFiles.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    clipboard = ClipboardState(files = selectedFiles, op = ClipboardOp.MOVE),
                    selectedIds = emptySet(),
                    isSelectionMode = false,
                    operationMessage = "Cut ${selectedFiles.size} items to clipboard"
                )
            }
        }
    }

    fun pasteClipboard() {
        val clip = _uiState.value.clipboard ?: return
        val targetDir = File(_uiState.value.currentPath)
        val sources = clip.files.map { File(it.path) }

        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true, operationMessage = "Processing...") }
            try {
                if (clip.op == ClipboardOp.COPY) {
                    operationsEngine.copyFiles(sources, targetDir).collect { progress ->
                        _uiState.update { it.copy(operationMessage = "Copying ${progress.currentFileName} (${progress.processedCount}/${progress.totalCount})") }
                    }
                } else {
                    operationsEngine.moveFiles(sources, targetDir).collect { progress ->
                        _uiState.update { it.copy(operationMessage = "Moving ${progress.currentFileName} (${progress.processedCount}/${progress.totalCount})") }
                    }
                }
                _uiState.update { it.copy(clipboard = null, isOperating = false, operationMessage = "Operation completed successfully") }
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.update { it.copy(isOperating = false, errorMessage = e.message ?: "Operation failed") }
            }
        }
    }

    fun deleteSelectedFiles() {
        val selectedFiles = getSelectedFileItems()
        if (selectedFiles.isEmpty()) return
        val targets = selectedFiles.map { File(it.path) }

        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true, operationMessage = "Deleting...") }
            try {
                operationsEngine.deleteFiles(targets).collect { progress ->
                    _uiState.update { it.copy(operationMessage = "Deleting ${progress.currentFileName}...") }
                }
                _uiState.update {
                    it.copy(
                        selectedIds = emptySet(),
                        isSelectionMode = false,
                        isOperating = false,
                        operationMessage = "Deleted ${targets.size} items"
                    )
                }
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.update { it.copy(isOperating = false, errorMessage = e.message ?: "Delete failed") }
            }
        }
    }

    fun createNewFolder(name: String) {
        val targetDir = File(_uiState.value.currentPath)
        viewModelScope.launch {
            val result = operationsEngine.createDirectory(targetDir, name)
            if (result.isSuccess) {
                loadDirectory(_uiState.value.currentPath)
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to create folder") }
            }
        }
    }

    fun renameFile(fileItem: FileItem, newName: String) {
        viewModelScope.launch {
            val result = operationsEngine.renameFile(File(fileItem.path), newName)
            if (result.isSuccess) {
                clearSelection()
                loadDirectory(_uiState.value.currentPath)
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to rename") }
            }
        }
    }

    fun clearClipboard() {
        _uiState.update { it.copy(clipboard = null, operationMessage = null) }
    }

    fun setFileFilter(filter: FileTypeFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
        loadDirectory(_uiState.value.currentPath)
    }

    fun toggleSortDirection() {
        val current = _uiState.value.sortConfig
        val nextDir = if (current.direction == SortDirection.ASCENDING) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }
        _uiState.update { it.copy(sortConfig = current.copy(direction = nextDir)) }
        loadDirectory(_uiState.value.currentPath)
    }

    fun compressSelectedToZip(archiveName: String = "Archive_${System.currentTimeMillis()}.zip") {
        val selected = getSelectedFileItems().map { File(it.path) }
        if (selected.isEmpty()) return
        val safeName = if (archiveName.endsWith(".zip", ignoreCase = true)) archiveName else "$archiveName.zip"
        val targetZip = File(_uiState.value.currentPath, safeName)
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true, operationMessage = "Creating $safeName...") }
            try {
                withContext(Dispatchers.IO) {
                    archiveEngine.createZipArchive(selected, targetZip).collect { progress ->
                        _uiState.update {
                            it.copy(operationMessage = "Compressing ${progress.currentFileName} (${progress.processedCount}/${progress.totalCount})")
                        }
                    }
                }
                clearSelection()
                _uiState.update { it.copy(isOperating = false, operationMessage = "Created $safeName") }
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.update { it.copy(isOperating = false, errorMessage = e.message ?: "Failed to create ZIP") }
            }
        }
    }

    fun extractArchiveFile(fileItem: FileItem) {
        val archiveFile = File(fileItem.path)
        val destDir = File(_uiState.value.currentPath, archiveFile.nameWithoutExtension)
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true, operationMessage = "Extracting ${fileItem.name}...") }
            try {
                withContext(Dispatchers.IO) {
                    archiveEngine.extractArchive(archiveFile, destDir).collect { progress ->
                        _uiState.update {
                            it.copy(operationMessage = "Extracting ${progress.currentFileName}...")
                        }
                    }
                }
                _uiState.update { it.copy(isOperating = false, operationMessage = "Extracted to ${destDir.name}") }
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.update { it.copy(isOperating = false, errorMessage = e.message ?: "Extraction failed") }
            }
        }
    }

    suspend fun computeSha256(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) return@withContext "N/A (Folder)"
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buf = ByteArray(64 * 1024)
                var r: Int
                while (fis.read(buf).also { r = it } != -1) {
                    if (r > 0) digest.update(buf, 0, r)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun extractSingleArchiveEntry(archiveItem: FileItem, entryName: String) {
        val archiveFile = File(archiveItem.path)
        val destDir = File(_uiState.value.currentPath)
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true, operationMessage = "Extracting $entryName...") }
            try {
                val outFile = archiveEngine.extractSingleEntry(archiveFile, entryName, destDir)
                _uiState.update { it.copy(isOperating = false, operationMessage = "Extracted ${outFile.name}") }
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.update { it.copy(isOperating = false, errorMessage = e.message ?: "Entry extraction failed") }
            }
        }
    }

    fun batchRenameSelected(
        prefix: String = "",
        suffix: String = "",
        findText: String = "",
        replaceText: String = "",
        startCounter: Int? = null
    ) {
        val selected = getSelectedFileItems()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true, operationMessage = "Batch renaming ${selected.size} items...") }
            var renamedCount = 0
            selected.forEachIndexed { index, item ->
                val f = File(item.path)
                val baseName = if (item.isDirectory) f.name else f.nameWithoutExtension
                val ext = if (item.isDirectory || f.extension.isEmpty()) "" else ".${f.extension}"
                var nextBase = if (findText.isNotEmpty()) baseName.replace(findText, replaceText) else baseName
                val counterPart = startCounter?.let { "_${(it + index).toString().padStart(3, '0')}" } ?: ""
                nextBase = "$prefix$nextBase$suffix$counterPart"
                if (nextBase.isNotBlank()) {
                    val res = operationsEngine.renameFile(f, "$nextBase$ext")
                    if (res.isSuccess) renamedCount++
                }
            }
            clearSelection()
            _uiState.update { it.copy(isOperating = false, operationMessage = "Renamed $renamedCount items") }
            loadDirectory(_uiState.value.currentPath)
        }
    }

    fun moveToTrashSelected(trashDir: File) {
        val selected = getSelectedFileItems().map { File(it.path) }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true, operationMessage = "Moving ${selected.size} items to Trash...") }
            try {
                if (!trashDir.exists()) trashDir.mkdirs()
                operationsEngine.moveFiles(selected, trashDir).collect { progress ->
                    _uiState.update { it.copy(operationMessage = "Trashing ${progress.currentFileName}...") }
                }
                _uiState.update {
                    it.copy(
                        selectedIds = emptySet(),
                        isSelectionMode = false,
                        isOperating = false,
                        operationMessage = "Moved ${selected.size} items to Trash"
                    )
                }
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.update { it.copy(isOperating = false, errorMessage = e.message ?: "Move to trash failed") }
            }
        }
    }

    fun toggleBookmark(path: String, name: String) {
        _uiState.update { state ->
            val exists = state.bookmarks.any { it.path == path }
            val updated = if (exists) {
                state.bookmarks.filterNot { it.path == path }
            } else {
                state.bookmarks + BreadcrumbItem(name = name, path = path)
            }
            state.copy(bookmarks = updated)
        }
    }

    fun clearOperationMessage() {
        _uiState.update { it.copy(operationMessage = null) }
    }

    private fun buildBreadcrumbs(path: String): List<BreadcrumbItem> {
        val items = mutableListOf<BreadcrumbItem>()
        var current: File? = File(path)
        val parts = mutableListOf<BreadcrumbItem>()

        while (current != null && current.path.isNotEmpty() && current.path != "/") {
            val name = if (current.absolutePath == rootPath) "Device" else current.name
            parts.add(0, BreadcrumbItem(name = name, path = current.absolutePath))
            if (current.absolutePath == rootPath) break
            current = current.parentFile
        }

        if (parts.isEmpty()) {
            items.add(BreadcrumbItem(name = "Device", path = rootPath))
        } else {
            items.addAll(parts)
        }
        return items
    }
}
