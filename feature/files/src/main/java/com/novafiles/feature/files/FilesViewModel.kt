package com.novafiles.feature.files

import android.content.Context
import android.os.Environment
import androidx.lifecycle.SavedStateHandle
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
import com.novafiles.data.files.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class FilesViewModel(
    private val fileRepository: FileRepository,
    context: Context? = null,
    private val savedStateHandle: SavedStateHandle? = null,
    initialPath: String? = null
) : ViewModel() {

    companion object {
        private const val KEY_CURRENT_PATH = "key_files_current_path"
        private const val KEY_SEARCH_QUERY = "key_files_search_query"
        private const val KEY_IS_GRID_VIEW = "key_files_is_grid_view"
        private const val KEY_SHOW_HIDDEN = "key_files_show_hidden"
        private const val KEY_SORT_BY = "key_files_sort_by"
        private const val KEY_SORT_DIR = "key_files_sort_dir"
        private const val KEY_ACTIVE_FILTER = "key_files_active_filter"
        private const val KEY_SELECTED_IDS = "key_files_selected_ids"
    }

    private val prefsRepo = context?.let { FilesPreferencesRepository(it) }
    private val initialPrefs = prefsRepo?.preferences?.value ?: FilesPreferences()

    private val rootPath = runCatching { Environment.getExternalStorageDirectory().absolutePath }.getOrDefault("/storage/emulated/0")
    private val operationsEngine = FileOperationsEngine()
    private val archiveEngine = ArchiveEngine()
    private var operationJob: Job? = null

    private val restoredPath = savedStateHandle?.get<String>(KEY_CURRENT_PATH) ?: initialPath ?: rootPath
    private val restoredQuery = savedStateHandle?.get<String>(KEY_SEARCH_QUERY) ?: ""
    private val restoredGridView = savedStateHandle?.get<Boolean>(KEY_IS_GRID_VIEW) ?: initialPrefs.isGridView
    private val restoredShowHidden = savedStateHandle?.get<Boolean>(KEY_SHOW_HIDDEN) ?: initialPrefs.showHiddenFiles
    private val restoredSortBy = savedStateHandle?.get<String>(KEY_SORT_BY)?.let { runCatching { SortBy.valueOf(it) }.getOrNull() } ?: initialPrefs.sortConfig.sortBy
    private val restoredSortDir = savedStateHandle?.get<String>(KEY_SORT_DIR)?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() } ?: initialPrefs.sortConfig.direction
    private val restoredFilter = savedStateHandle?.get<String>(KEY_ACTIVE_FILTER)?.let { runCatching { FileTypeFilter.valueOf(it) }.getOrNull() } ?: FileTypeFilter.ALL
    private val restoredSelectedIds = savedStateHandle?.get<ArrayList<String>>(KEY_SELECTED_IDS)?.toSet() ?: emptySet()

    private val _uiState = MutableStateFlow(
        FilesUiState(
            currentPath = restoredPath,
            breadcrumbs = buildBreadcrumbs(restoredPath),
            searchQuery = restoredQuery,
            isGridView = restoredGridView,
            showHiddenFiles = restoredShowHidden,
            sortConfig = FileSortConfig(sortBy = restoredSortBy, direction = restoredSortDir, foldersFirst = true),
            activeFilter = restoredFilter,
            selectedIds = restoredSelectedIds,
            isSelectionMode = restoredSelectedIds.isNotEmpty(),
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
        loadDirectory(_uiState.value.currentPath, preserveSelection = restoredSelectedIds.isNotEmpty())
    }

    fun loadDirectory(path: String, preserveSelection: Boolean = false) {
        savedStateHandle?.set(KEY_CURRENT_PATH, path)
        if (!preserveSelection) {
            savedStateHandle?.set(KEY_SELECTED_IDS, arrayListOf<String>())
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentPath = path,
                    breadcrumbs = buildBreadcrumbs(path),
                    isLoading = true,
                    errorMessage = null,
                    selectedIds = if (preserveSelection) it.selectedIds else emptySet(),
                    isSelectionMode = if (preserveSelection) it.selectedIds.isNotEmpty() else false
                )
            }

            fileRepository.getFiles(
                directoryPath = path,
                sortConfig = _uiState.value.sortConfig,
                showHidden = _uiState.value.showHiddenFiles
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

    fun refresh() {
        loadDirectory(_uiState.value.currentPath, preserveSelection = true)
    }

    fun navigateToFolder(path: String) {
        loadDirectory(path)
    }

    fun canNavigateUp(): Boolean {
        val current = File(_uiState.value.currentPath)
        val canonicalCurrent = runCatching { current.canonicalPath }.getOrElse { current.absolutePath }
        val canonicalRoot = runCatching { File(rootPath).canonicalPath }.getOrElse { rootPath }
        if (canonicalCurrent == canonicalRoot || canonicalCurrent == "/storage/emulated/0" || canonicalCurrent == "/") {
            return false
        }
        val parent = current.parentFile ?: return false
        return parent.exists() && parent.canRead()
    }

    fun navigateUp(): Boolean {
        if (!canNavigateUp()) return false
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
            savedStateHandle?.set(KEY_SELECTED_IDS, ArrayList(updated))
            state.copy(
                selectedIds = updated,
                isSelectionMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            if (state.isAllSelected) {
                savedStateHandle?.set(KEY_SELECTED_IDS, arrayListOf<String>())
                state.copy(selectedIds = emptySet(), isSelectionMode = false)
            } else {
                val allIds = (state.folders + state.files).map { it.id }.toSet()
                savedStateHandle?.set(KEY_SELECTED_IDS, ArrayList(allIds))
                state.copy(selectedIds = allIds, isSelectionMode = true)
            }
        }
    }

    fun clearSelection() {
        savedStateHandle?.set(KEY_SELECTED_IDS, arrayListOf<String>())
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun toggleShowHiddenFiles() {
        val newValue = !_uiState.value.showHiddenFiles
        savedStateHandle?.set(KEY_SHOW_HIDDEN, newValue)
        prefsRepo?.updateShowHiddenFiles(newValue)
        _uiState.update { it.copy(showHiddenFiles = newValue) }
        loadDirectory(_uiState.value.currentPath, preserveSelection = true)
    }

    fun toggleLayoutMode() {
        val nextMode = !_uiState.value.isGridView
        savedStateHandle?.set(KEY_IS_GRID_VIEW, nextMode)
        prefsRepo?.updateGridView(nextMode)
        _uiState.update { it.copy(isGridView = nextMode) }
    }

    fun setSortBy(sortBy: SortBy) {
        val current = _uiState.value.sortConfig
        val newDirection = if (current.sortBy == sortBy) {
            if (current.direction == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
        } else {
            SortDirection.ASCENDING
        }
        val newConfig = current.copy(sortBy = sortBy, direction = newDirection, foldersFirst = true)
        savedStateHandle?.set(KEY_SORT_BY, sortBy.name)
        savedStateHandle?.set(KEY_SORT_DIR, newDirection.name)
        prefsRepo?.updateSortConfig(newConfig)
        _uiState.update { it.copy(sortConfig = newConfig) }
        loadDirectory(_uiState.value.currentPath, preserveSelection = true)
    }

    fun toggleSortDirection() {
        val current = _uiState.value.sortConfig
        val nextDir = if (current.direction == SortDirection.ASCENDING) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }
        val newConfig = current.copy(direction = nextDir, foldersFirst = true)
        savedStateHandle?.set(KEY_SORT_DIR, nextDir.name)
        prefsRepo?.updateSortConfig(newConfig)
        _uiState.update { it.copy(sortConfig = newConfig) }
        loadDirectory(_uiState.value.currentPath, preserveSelection = true)
    }

    fun setSearchQuery(query: String) {
        savedStateHandle?.set(KEY_SEARCH_QUERY, query)
        _uiState.update { it.copy(searchQuery = query) }
        loadDirectory(_uiState.value.currentPath, preserveSelection = true)
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

    fun copyFile(item: FileItem) {
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(files = listOf(item), op = ClipboardOp.COPY),
                operationMessage = "Copied ${item.name} to clipboard"
            )
        }
    }

    fun cutFile(item: FileItem) {
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(files = listOf(item), op = ClipboardOp.MOVE),
                operationMessage = "Cut ${item.name} to clipboard"
            )
        }
    }

    fun pasteIntoFolder(targetFolder: FileItem) {
        val clip = _uiState.value.clipboard ?: return
        val targetDir = File(targetFolder.path)
        val sources = clip.files.map { File(it.path) }

        operationJob = viewModelScope.launch {
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
                _uiState.update { it.copy(clipboard = null, isOperating = false, operationMessage = "Pasted into ${targetFolder.name}") }
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.update { it.copy(isOperating = false, errorMessage = e.message ?: "Operation failed") }
            }
        }
    }

    fun pasteClipboard() {
        val clip = _uiState.value.clipboard ?: return
        val targetDir = File(_uiState.value.currentPath)
        val sources = clip.files.map { File(it.path) }

        operationJob = viewModelScope.launch {
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

    fun cancelOperation() {
        operationJob?.cancel()
        operationJob = null
        _uiState.update { it.copy(isOperating = false, operationMessage = "Operation cancelled") }
    }

    fun deleteSingleFile(fileItem: FileItem) {
        val target = File(fileItem.path)
        operationJob = viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true, operationMessage = "Deleting ${fileItem.name}...") }
            try {
                operationsEngine.deleteFiles(listOf(target)).collect { progress ->
                    _uiState.update { it.copy(operationMessage = "Deleting ${progress.currentFileName}...") }
                }
                _uiState.update {
                    it.copy(
                        selectedIds = it.selectedIds - fileItem.id,
                        isOperating = false,
                        operationMessage = "Deleted ${fileItem.name}"
                    )
                }
                loadDirectory(_uiState.value.currentPath)
            } catch (e: Exception) {
                _uiState.update { it.copy(isOperating = false, errorMessage = e.message ?: "Delete failed") }
            }
        }
    }

    fun deleteSelectedFiles() {
        val selectedFiles = getSelectedFileItems()
        if (selectedFiles.isEmpty()) return
        val targets = selectedFiles.map { File(it.path) }

        operationJob = viewModelScope.launch {
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
        savedStateHandle?.set(KEY_ACTIVE_FILTER, filter.name)
        _uiState.update { it.copy(activeFilter = filter) }
        loadDirectory(_uiState.value.currentPath, preserveSelection = true)
    }

    fun compressSelectedToZip(archiveName: String = "Archive_${System.currentTimeMillis()}.zip") {
        val selected = getSelectedFileItems().map { File(it.path) }
        if (selected.isEmpty()) return
        val safeName = if (archiveName.endsWith(".zip", ignoreCase = true)) archiveName else "$archiveName.zip"
        val targetZip = File(_uiState.value.currentPath, safeName)
        operationJob = viewModelScope.launch {
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

    fun compressSingleFile(fileItem: FileItem, archiveName: String) {
        val targetFile = File(fileItem.path)
        val safeName = if (archiveName.endsWith(".zip", ignoreCase = true)) archiveName else "$archiveName.zip"
        val targetZip = File(_uiState.value.currentPath, safeName)
        operationJob = viewModelScope.launch {
            _uiState.update { it.copy(isOperating = true, operationMessage = "Creating $safeName...") }
            try {
                withContext(Dispatchers.IO) {
                    archiveEngine.createZipArchive(listOf(targetFile), targetZip).collect { progress ->
                        _uiState.update {
                            it.copy(operationMessage = "Compressing ${progress.currentFileName} (${progress.processedCount}/${progress.totalCount})")
                        }
                    }
                }
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
        operationJob = viewModelScope.launch {
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
        operationJob = viewModelScope.launch {
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
        operationJob = viewModelScope.launch {
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
        operationJob = viewModelScope.launch {
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
