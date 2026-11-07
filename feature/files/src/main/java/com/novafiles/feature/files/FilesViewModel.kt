package com.novafiles.feature.files

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.common.NovaResult
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.storage.FileOperationsEngine
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
    private val _uiState = MutableStateFlow(
        FilesUiState(
            currentPath = initialPath ?: rootPath,
            breadcrumbs = buildBreadcrumbs(initialPath ?: rootPath)
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
                        val filteredData = if (query.isNotBlank()) {
                            result.data.filter { it.name.contains(query, ignoreCase = true) }
                        } else {
                            result.data
                        }
                        val folders = filteredData.filter { it.isDirectory }
                        val files = filteredData.filter { !it.isDirectory }
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
