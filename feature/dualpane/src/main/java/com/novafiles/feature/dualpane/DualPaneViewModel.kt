package com.novafiles.feature.dualpane

import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.common.NovaResult
import com.novafiles.core.model.ConflictResolution
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileOperationProgress
import com.novafiles.core.storage.FileOperationsEngine
import com.novafiles.data.files.FileRepository
import com.novafiles.feature.files.BreadcrumbItem
import com.novafiles.feature.files.FilesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class ActivePane {
    PANE_A,
    PANE_B
}

data class DualPaneUiState(
    val paneA: FilesUiState = FilesUiState(),
    val paneB: FilesUiState = FilesUiState(),
    val activePane: ActivePane = ActivePane.PANE_A,
    val isOperationInProgress: Boolean = false,
    val operationProgress: FileOperationProgress? = null,
    val statusMessage: String? = null
)

class DualPaneViewModel(
    private val fileRepository: FileRepository,
    private val savedStateHandle: SavedStateHandle? = null,
    private val fileOperationsEngine: FileOperationsEngine = FileOperationsEngine()
) : ViewModel() {

    companion object {
        private const val KEY_PANE_A_PATH = "key_dual_pane_a_path"
        private const val KEY_PANE_B_PATH = "key_dual_pane_b_path"
        private const val KEY_ACTIVE_PANE = "key_dual_active_pane"
    }

    private val rootPath = runCatching { Environment.getExternalStorageDirectory().absolutePath }.getOrDefault("/storage/emulated/0")
    private val downloadsPath = "$rootPath/Download"

    private val restoredPathA = savedStateHandle?.get<String>(KEY_PANE_A_PATH) ?: rootPath
    private val restoredPathB = savedStateHandle?.get<String>(KEY_PANE_B_PATH) ?: downloadsPath
    private val restoredActivePane = savedStateHandle?.get<String>(KEY_ACTIVE_PANE)?.let { runCatching { ActivePane.valueOf(it) }.getOrNull() } ?: ActivePane.PANE_A

    private val _uiState = MutableStateFlow(
        DualPaneUiState(
            paneA = FilesUiState(currentPath = restoredPathA, breadcrumbs = buildBreadcrumbs(restoredPathA)),
            paneB = FilesUiState(currentPath = restoredPathB, breadcrumbs = buildBreadcrumbs(restoredPathB)),
            activePane = restoredActivePane
        )
    )
    val uiState: StateFlow<DualPaneUiState> = _uiState.asStateFlow()

    init {
        loadPaneA(_uiState.value.paneA.currentPath)
        loadPaneB(_uiState.value.paneB.currentPath)
    }

    fun setActivePane(pane: ActivePane) {
        savedStateHandle?.set(KEY_ACTIVE_PANE, pane.name)
        _uiState.update { it.copy(activePane = pane) }
    }

    fun loadPaneA(path: String) {
        savedStateHandle?.set(KEY_PANE_A_PATH, path)
        _uiState.update { it.copy(paneA = it.paneA.copy(currentPath = path, breadcrumbs = buildBreadcrumbs(path), isLoading = true)) }
        viewModelScope.launch {
            fileRepository.getFiles(path).collect { result ->
                if (result is NovaResult.Success) {
                    val folders = result.data.filter { it.isDirectory }
                    val files = result.data.filter { !it.isDirectory }
                    _uiState.update { it.copy(paneA = it.paneA.copy(folders = folders, files = files, isLoading = false)) }
                }
            }
        }
    }

    fun loadPaneB(path: String) {
        savedStateHandle?.set(KEY_PANE_B_PATH, path)
        _uiState.update { it.copy(paneB = it.paneB.copy(currentPath = path, breadcrumbs = buildBreadcrumbs(path), isLoading = true)) }
        viewModelScope.launch {
            fileRepository.getFiles(path).collect { result ->
                if (result is NovaResult.Success) {
                    val folders = result.data.filter { it.isDirectory }
                    val files = result.data.filter { !it.isDirectory }
                    _uiState.update { it.copy(paneB = it.paneB.copy(folders = folders, files = files, isLoading = false)) }
                }
            }
        }
    }

    fun toggleSelectionA(id: String) {
        _uiState.update { state ->
            val updated = state.paneA.selectedIds.toMutableSet()
            if (updated.contains(id)) updated.remove(id) else updated.add(id)
            state.copy(paneA = state.paneA.copy(selectedIds = updated, isSelectionMode = updated.isNotEmpty()))
        }
    }

    fun toggleSelectionB(id: String) {
        _uiState.update { state ->
            val updated = state.paneB.selectedIds.toMutableSet()
            if (updated.contains(id)) updated.remove(id) else updated.add(id)
            state.copy(paneB = state.paneB.copy(selectedIds = updated, isSelectionMode = updated.isNotEmpty()))
        }
    }

    /**
     * Copies selected files from the active pane to the opposing pane.
     */
    fun copySelectedToOpposingPane() {
        viewModelScope.launch {
            val (sourcePane, targetPath) = if (_uiState.value.activePane == ActivePane.PANE_A) {
                _uiState.value.paneA to _uiState.value.paneB.currentPath
            } else {
                _uiState.value.paneB to _uiState.value.paneA.currentPath
            }

            val selectedFiles = (sourcePane.folders + sourcePane.files)
                .filter { sourcePane.selectedIds.contains(it.id) }
                .map { File(it.path) }

            if (selectedFiles.isEmpty()) return@launch

            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Copying ${selectedFiles.size} items...") }

            fileOperationsEngine.copyFiles(
                sources = selectedFiles,
                targetDir = File(targetPath),
                conflictResolution = ConflictResolution.RENAME_NEW
            ).collect { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
                if (progress.isComplete) {
                    _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Copy completed!") }
                    // Reload both panes
                    loadPaneA(_uiState.value.paneA.currentPath)
                    loadPaneB(_uiState.value.paneB.currentPath)
                }
            }
        }
    }

    /**
     * Moves selected files from the active pane to the opposing pane.
     */
    fun moveSelectedToOpposingPane() {
        viewModelScope.launch {
            val (sourcePane, targetPath) = if (_uiState.value.activePane == ActivePane.PANE_A) {
                _uiState.value.paneA to _uiState.value.paneB.currentPath
            } else {
                _uiState.value.paneB to _uiState.value.paneA.currentPath
            }

            val selectedFiles = (sourcePane.folders + sourcePane.files)
                .filter { sourcePane.selectedIds.contains(it.id) }
                .map { File(it.path) }

            if (selectedFiles.isEmpty()) return@launch

            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Moving ${selectedFiles.size} items...") }

            fileOperationsEngine.moveFiles(
                sources = selectedFiles,
                targetDir = File(targetPath),
                conflictResolution = ConflictResolution.RENAME_NEW
            ).collect { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
                if (progress.isComplete) {
                    _uiState.update { it.copy(isOperationInProgress = false, statusMessage = "Move completed!") }
                    loadPaneA(_uiState.value.paneA.currentPath)
                    loadPaneB(_uiState.value.paneB.currentPath)
                }
            }
        }
    }

    fun syncPanes() {
        val targetPath = if (_uiState.value.activePane == ActivePane.PANE_A) {
            _uiState.value.paneA.currentPath
        } else {
            _uiState.value.paneB.currentPath
        }
        loadPaneA(targetPath)
        loadPaneB(targetPath)
    }

    fun swapPanes() {
        val pathA = _uiState.value.paneA.currentPath
        val pathB = _uiState.value.paneB.currentPath
        loadPaneA(pathB)
        loadPaneB(pathA)
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
