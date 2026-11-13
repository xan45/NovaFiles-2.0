package com.novafiles.feature.dualpane

import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.common.NovaResult
import com.novafiles.core.model.ConflictResolution
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileOperationProgress
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.storage.ArchiveEngine
import com.novafiles.core.storage.FileOperationsEngine
import com.novafiles.data.files.FileRepository
import com.novafiles.feature.files.BreadcrumbItem
import com.novafiles.feature.files.ClipboardOp
import com.novafiles.feature.files.ClipboardState
import com.novafiles.feature.files.FilesUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class ActivePane {
    PANE_A,
    PANE_B
}

/**
 * Represents an independent browsing pane/tab in the Multi-Pane Manager.
 */
data class MultiPaneTab(
    val id: String = UUID.randomUUID().toString(),
    val currentPath: String,
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),
    val folders: List<FileItem> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false,
    val isGridView: Boolean = false,
    val showHiddenFiles: Boolean = false,
    val sortConfig: FileSortConfig = FileSortConfig(),
    val searchQuery: String = "",
    val historyStack: List<String> = emptyList()
) : java.io.Serializable {
    val title: String
        get() = if (currentPath.isEmpty() || currentPath == "/storage/emulated/0") "Storage"
        else File(currentPath).name.ifEmpty { "Folder" }

    val totalItemCount: Int get() = folders.size + files.size
    val selectedCount: Int get() = selectedIds.size

    val allItems: List<FileItem> get() = folders + files

    fun toFilesUiState(): FilesUiState = FilesUiState(
        currentPath = currentPath,
        breadcrumbs = breadcrumbs,
        folders = folders,
        files = files,
        selectedIds = selectedIds,
        isSelectionMode = isSelectionMode,
        sortConfig = sortConfig,
        isGridView = isGridView,
        showHiddenFiles = showHiddenFiles,
        isLoading = isLoading
    )
}

/**
 * State of the Multi-Pane Manager supporting N independently navigable tabs and cross-tab transfers.
 */
data class DualPaneUiState(
    val tabs: List<MultiPaneTab> = emptyList(),
    val activeTabIndex: Int = 0,
    val clipboard: ClipboardState? = null,
    val isOperationInProgress: Boolean = false,
    val operationProgress: FileOperationProgress? = null,
    val statusMessage: String? = null
) {
    val activeTab: MultiPaneTab? get() = tabs.getOrNull(activeTabIndex) ?: tabs.firstOrNull()

    // Backward-compatibility properties
    val paneA: FilesUiState get() = tabs.getOrNull(0)?.toFilesUiState() ?: FilesUiState()
    val paneB: FilesUiState get() = tabs.getOrNull(1)?.toFilesUiState() ?: FilesUiState()
    val activePane: ActivePane get() = if (activeTabIndex == 0) ActivePane.PANE_A else ActivePane.PANE_B
}

typealias MultiPaneUiState = DualPaneUiState

/**
 * Lifecycle-managed AndroidX ViewModel powering the Multi-Pane Manager.
 * Replaces old 2-pane vertical split with a full-screen, N-tab browsing engine.
 */
class DualPaneViewModel(
    private val fileRepository: FileRepository,
    private val savedStateHandle: SavedStateHandle? = null,
    private val fileOperationsEngine: FileOperationsEngine = FileOperationsEngine(),
    private val archiveEngine: ArchiveEngine = ArchiveEngine()
) : ViewModel() {

    companion object {
        private const val KEY_TABS_PATHS = "key_multi_pane_tabs_paths"
        private const val KEY_ACTIVE_TAB_INDEX = "key_multi_pane_active_tab_index"
        private const val KEY_PANE_A_PATH = "key_dual_pane_a_path"
        private const val KEY_PANE_B_PATH = "key_dual_pane_b_path"
        private const val KEY_ACTIVE_PANE = "key_dual_active_pane"
    }

    private val rootPath = runCatching { Environment.getExternalStorageDirectory().absolutePath }.getOrDefault("/storage/emulated/0")
    private val downloadsPath = "$rootPath/Download"

    private var operationJob: Job? = null

    private val _uiState: MutableStateFlow<DualPaneUiState>

    init {
        val restoredPaths = savedStateHandle?.get<ArrayList<String>>(KEY_TABS_PATHS)
        val initialTabs: List<MultiPaneTab> = if (!restoredPaths.isNullOrEmpty()) {
            restoredPaths.map { path ->
                MultiPaneTab(currentPath = path, breadcrumbs = buildBreadcrumbs(path))
            }
        } else {
            val pathA = savedStateHandle?.get<String>(KEY_PANE_A_PATH) ?: rootPath
            val pathB = savedStateHandle?.get<String>(KEY_PANE_B_PATH) ?: downloadsPath
            listOf(
                MultiPaneTab(currentPath = pathA, breadcrumbs = buildBreadcrumbs(pathA)),
                MultiPaneTab(currentPath = pathB, breadcrumbs = buildBreadcrumbs(pathB))
            )
        }

        val restoredIndex = savedStateHandle?.get<Int>(KEY_ACTIVE_TAB_INDEX)
            ?: savedStateHandle?.get<String>(KEY_ACTIVE_PANE)?.let {
                if (it == ActivePane.PANE_B.name) 1 else 0
            } ?: 0

        _uiState = MutableStateFlow(
            DualPaneUiState(
                tabs = initialTabs,
                activeTabIndex = restoredIndex.coerceIn(0, (initialTabs.size - 1).coerceAtLeast(0))
            )
        )

        // Load content for all initial tabs
        initialTabs.indices.forEach { index ->
            loadDirectory(index, initialTabs[index].currentPath, addToHistory = false)
        }
    }

    val uiState: StateFlow<DualPaneUiState> = _uiState.asStateFlow()

    private fun persistState() {
        val currentTabs = _uiState.value.tabs
        val activeIndex = _uiState.value.activeTabIndex
        savedStateHandle?.set(KEY_TABS_PATHS, ArrayList(currentTabs.map { it.currentPath }))
        savedStateHandle?.set(KEY_ACTIVE_TAB_INDEX, activeIndex)
        savedStateHandle?.set(KEY_PANE_A_PATH, currentTabs.getOrNull(0)?.currentPath ?: rootPath)
        savedStateHandle?.set(KEY_PANE_B_PATH, currentTabs.getOrNull(1)?.currentPath ?: downloadsPath)
        savedStateHandle?.set(KEY_ACTIVE_PANE, if (activeIndex == 0) ActivePane.PANE_A.name else ActivePane.PANE_B.name)
    }

    // ==========================================
    // Tab Management
    // ==========================================

    /**
     * Adds a new browsing tab and selects it.
     */
    fun addTab(path: String = rootPath) {
        val newTab = MultiPaneTab(
            currentPath = path,
            breadcrumbs = buildBreadcrumbs(path)
        )
        val newIndex = _uiState.value.tabs.size
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs + newTab,
                activeTabIndex = newIndex
            )
        }
        persistState()
        loadDirectory(newIndex, path, addToHistory = false)
    }

    /**
     * Closes the tab at [index]. Retains at least one tab.
     */
    fun closeTab(index: Int) {
        if (_uiState.value.tabs.size <= 1) return
        _uiState.update { state ->
            val updatedTabs = state.tabs.toMutableList()
            if (index in updatedTabs.indices) {
                updatedTabs.removeAt(index)
                val newActiveIndex = when {
                    state.activeTabIndex >= updatedTabs.size -> updatedTabs.size - 1
                    state.activeTabIndex == index -> (index - 1).coerceAtLeast(0)
                    state.activeTabIndex > index -> state.activeTabIndex - 1
                    else -> state.activeTabIndex
                }
                state.copy(tabs = updatedTabs, activeTabIndex = newActiveIndex)
            } else state
        }
        persistState()
    }

    /**
     * Selects active tab by index.
     */
    fun selectTab(index: Int) {
        if (index in _uiState.value.tabs.indices) {
            _uiState.update { it.copy(activeTabIndex = index) }
            persistState()
        }
    }

    // ==========================================
    // Directory Loading & Navigation
    // ==========================================

    fun loadDirectory(tabIndex: Int, path: String, addToHistory: Boolean = true) {
        val tabs = _uiState.value.tabs
        if (tabIndex !in tabs.indices) return

        val currentTab = tabs[tabIndex]
        val newHistory = if (addToHistory && currentTab.currentPath.isNotBlank() && currentTab.currentPath != path) {
            currentTab.historyStack + currentTab.currentPath
        } else {
            currentTab.historyStack
        }

        _uiState.update { state ->
            val updatedTabs = state.tabs.toMutableList()
            updatedTabs[tabIndex] = currentTab.copy(
                currentPath = path,
                breadcrumbs = buildBreadcrumbs(path),
                historyStack = newHistory,
                selectedIds = emptySet(),
                isSelectionMode = false,
                isLoading = true
            )
            state.copy(tabs = updatedTabs)
        }
        persistState()

        viewModelScope.launch {
            fileRepository.getFiles(
                directoryPath = path,
                sortConfig = currentTab.sortConfig,
                showHidden = currentTab.showHiddenFiles
            ).collect { result ->
                if (result is NovaResult.Success) {
                    val allItems = result.data
                    val query = _uiState.value.tabs.getOrNull(tabIndex)?.searchQuery.orEmpty()
                    val filtered = if (query.isNotBlank()) {
                        allItems.filter { it.name.contains(query, ignoreCase = true) }
                    } else {
                        allItems
                    }
                    val folders = filtered.filter { it.isDirectory }
                    val files = filtered.filter { !it.isDirectory }

                    _uiState.update { state ->
                        if (tabIndex in state.tabs.indices) {
                            val updated = state.tabs.toMutableList()
                            updated[tabIndex] = updated[tabIndex].copy(
                                folders = folders,
                                files = files,
                                isLoading = false
                            )
                            state.copy(tabs = updated)
                        } else state
                    }
                }
            }
        }
    }

    fun refreshTab(tabIndex: Int = _uiState.value.activeTabIndex) {
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        loadDirectory(tabIndex, tab.currentPath, addToHistory = false)
    }

    fun setSearchQuery(tabIndex: Int, query: String) {
        _uiState.update { state ->
            if (tabIndex in state.tabs.indices) {
                val updated = state.tabs.toMutableList()
                updated[tabIndex] = updated[tabIndex].copy(searchQuery = query)
                state.copy(tabs = updated)
            } else state
        }
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        loadDirectory(tabIndex, tab.currentPath, addToHistory = false)
    }

    fun toggleLayoutView(tabIndex: Int = _uiState.value.activeTabIndex) {
        _uiState.update { state ->
            if (tabIndex in state.tabs.indices) {
                val updated = state.tabs.toMutableList()
                updated[tabIndex] = updated[tabIndex].copy(isGridView = !updated[tabIndex].isGridView)
                state.copy(tabs = updated)
            } else state
        }
    }

    fun toggleShowHidden(tabIndex: Int = _uiState.value.activeTabIndex) {
        _uiState.update { state ->
            if (tabIndex in state.tabs.indices) {
                val updated = state.tabs.toMutableList()
                updated[tabIndex] = updated[tabIndex].copy(showHiddenFiles = !updated[tabIndex].showHiddenFiles)
                state.copy(tabs = updated)
            } else state
        }
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        loadDirectory(tabIndex, tab.currentPath, addToHistory = false)
    }

    fun setSortBy(tabIndex: Int = _uiState.value.activeTabIndex, sortBy: SortBy) {
        _uiState.update { state ->
            if (tabIndex in state.tabs.indices) {
                val updated = state.tabs.toMutableList()
                val currentConfig = updated[tabIndex].sortConfig
                updated[tabIndex] = updated[tabIndex].copy(sortConfig = currentConfig.copy(sortBy = sortBy))
                state.copy(tabs = updated)
            } else state
        }
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        loadDirectory(tabIndex, tab.currentPath, addToHistory = false)
    }

    fun toggleSortDirection(tabIndex: Int = _uiState.value.activeTabIndex) {
        _uiState.update { state ->
            if (tabIndex in state.tabs.indices) {
                val updated = state.tabs.toMutableList()
                val currentConfig = updated[tabIndex].sortConfig
                val newDir = if (currentConfig.direction == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                updated[tabIndex] = updated[tabIndex].copy(sortConfig = currentConfig.copy(direction = newDir))
                state.copy(tabs = updated)
            } else state
        }
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        loadDirectory(tabIndex, tab.currentPath, addToHistory = false)
    }

    // ==========================================
    // Selection Management
    // ==========================================

    fun toggleSelect(tabIndex: Int, itemId: String) {
        _uiState.update { state ->
            if (tabIndex in state.tabs.indices) {
                val updated = state.tabs.toMutableList()
                val tab = updated[tabIndex]
                val newSelected = tab.selectedIds.toMutableSet()
                if (newSelected.contains(itemId)) newSelected.remove(itemId) else newSelected.add(itemId)
                updated[tabIndex] = tab.copy(
                    selectedIds = newSelected,
                    isSelectionMode = newSelected.isNotEmpty()
                )
                state.copy(tabs = updated)
            } else state
        }
    }

    fun selectAll(tabIndex: Int = _uiState.value.activeTabIndex) {
        _uiState.update { state ->
            if (tabIndex in state.tabs.indices) {
                val updated = state.tabs.toMutableList()
                val tab = updated[tabIndex]
                val allIds = (tab.folders + tab.files).map { it.id }.toSet()
                val isAllSelected = tab.selectedIds.size == allIds.size
                val newSelected = if (isAllSelected) emptySet() else allIds
                updated[tabIndex] = tab.copy(
                    selectedIds = newSelected,
                    isSelectionMode = newSelected.isNotEmpty()
                )
                state.copy(tabs = updated)
            } else state
        }
    }

    fun clearSelection(tabIndex: Int = _uiState.value.activeTabIndex) {
        _uiState.update { state ->
            if (tabIndex in state.tabs.indices) {
                val updated = state.tabs.toMutableList()
                val tab = updated[tabIndex]
                updated[tabIndex] = tab.copy(
                    selectedIds = emptySet(),
                    isSelectionMode = false
                )
                state.copy(tabs = updated)
            } else state
        }
    }

    // ==========================================
    // Cross-Tab Clipboard & File Operations
    // ==========================================

    fun copySelected(tabIndex: Int = _uiState.value.activeTabIndex) {
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        val selectedFiles = (tab.folders + tab.files).filter { tab.selectedIds.contains(it.id) }
        if (selectedFiles.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                clipboard = ClipboardState(files = selectedFiles, op = ClipboardOp.COPY),
                statusMessage = "Copied ${selectedFiles.size} items to clipboard"
            )
        }
        clearSelection(tabIndex)
    }

    fun cutSelected(tabIndex: Int = _uiState.value.activeTabIndex) {
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        val selectedFiles = (tab.folders + tab.files).filter { tab.selectedIds.contains(it.id) }
        if (selectedFiles.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                clipboard = ClipboardState(files = selectedFiles, op = ClipboardOp.MOVE),
                statusMessage = "Cut ${selectedFiles.size} items to clipboard"
            )
        }
        clearSelection(tabIndex)
    }

    fun copySingleFile(item: FileItem) {
        _uiState.update { state ->
            state.copy(
                clipboard = ClipboardState(files = listOf(item), op = ClipboardOp.COPY),
                statusMessage = "Copied ${item.name} to clipboard"
            )
        }
    }

    fun cutSingleFile(item: FileItem) {
        _uiState.update { state ->
            state.copy(
                clipboard = ClipboardState(files = listOf(item), op = ClipboardOp.MOVE),
                statusMessage = "Cut ${item.name} to clipboard"
            )
        }
    }

    fun clearClipboard() {
        _uiState.update { it.copy(clipboard = null, statusMessage = null) }
    }

    /**
     * Pastes clipboard contents into the target tab's current path (or into [targetFolder] if provided).
     * Handles duplicate files safely with ConflictResolution.RENAME_NEW.
     */
    fun pasteClipboard(
        targetTabIndex: Int = _uiState.value.activeTabIndex,
        targetFolder: String? = null
    ) {
        val clipboard = _uiState.value.clipboard ?: return
        if (clipboard.files.isEmpty()) return

        val targetPath = targetFolder ?: _uiState.value.tabs.getOrNull(targetTabIndex)?.currentPath ?: return
        val targetDir = File(targetPath)
        val sources = clipboard.files.map { File(it.path) }

        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOperationInProgress = true,
                    statusMessage = "${if (clipboard.op == ClipboardOp.COPY) "Copying" else "Moving"} ${sources.size} items..."
                )
            }

            val flow = if (clipboard.op == ClipboardOp.COPY) {
                fileOperationsEngine.copyFiles(
                    sources = sources,
                    targetDir = targetDir,
                    conflictResolution = ConflictResolution.RENAME_NEW
                )
            } else {
                fileOperationsEngine.moveFiles(
                    sources = sources,
                    targetDir = targetDir,
                    conflictResolution = ConflictResolution.RENAME_NEW
                )
            }

            flow.collect { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
                if (progress.isComplete) {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgress = null,
                            statusMessage = "Operation completed!",
                            clipboard = if (clipboard.op == ClipboardOp.MOVE) null else it.clipboard
                        )
                    }
                    // Refresh target tab and any other tabs currently looking at target or source directory
                    _uiState.value.tabs.indices.forEach { idx ->
                        refreshTab(idx)
                    }
                }
            }
        }
    }

    fun deleteSelected(tabIndex: Int = _uiState.value.activeTabIndex) {
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        val selectedFiles = (tab.folders + tab.files).filter { tab.selectedIds.contains(it.id) }
        if (selectedFiles.isEmpty()) return

        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Deleting ${selectedFiles.size} items...") }
            fileOperationsEngine.deleteFiles(selectedFiles.map { File(it.path) }).collect { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
                if (progress.isComplete) {
                    _uiState.update { it.copy(isOperationInProgress = false, operationProgress = null, statusMessage = "Deleted successfully") }
                    refreshTab(tabIndex)
                }
            }
        }
    }

    fun deleteSingleFile(item: FileItem, tabIndex: Int = _uiState.value.activeTabIndex) {
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Deleting ${item.name}...") }
            fileOperationsEngine.deleteFiles(listOf(File(item.path))).collect { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
                if (progress.isComplete) {
                    _uiState.update { it.copy(isOperationInProgress = false, operationProgress = null, statusMessage = "Deleted") }
                    refreshTab(tabIndex)
                }
            }
        }
    }

    fun renameItem(item: FileItem, newName: String, tabIndex: Int = _uiState.value.activeTabIndex) {
        viewModelScope.launch {
            val result = fileOperationsEngine.renameFile(File(item.path), newName)
            if (result.isSuccess) {
                refreshTab(tabIndex)
            } else {
                _uiState.update { it.copy(statusMessage = "Rename failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun createFolder(name: String, tabIndex: Int = _uiState.value.activeTabIndex) {
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        viewModelScope.launch {
            val result = fileOperationsEngine.createDirectory(File(tab.currentPath), name)
            if (result.isSuccess) {
                refreshTab(tabIndex)
            } else {
                _uiState.update { it.copy(statusMessage = "Create folder failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun compressItems(items: List<FileItem>, zipName: String, tabIndex: Int = _uiState.value.activeTabIndex) {
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        val targetZip = File(tab.currentPath, if (zipName.endsWith(".zip", ignoreCase = true)) zipName else "$zipName.zip")

        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Compressing items...") }
            archiveEngine.createZipArchive(items.map { File(it.path) }, targetZip).collect { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
                if (progress.isComplete) {
                    _uiState.update { it.copy(isOperationInProgress = false, operationProgress = null, statusMessage = "Archive created") }
                    refreshTab(tabIndex)
                }
            }
        }
    }

    fun extractArchive(archiveItem: FileItem, tabIndex: Int = _uiState.value.activeTabIndex) {
        val tab = _uiState.value.tabs.getOrNull(tabIndex) ?: return
        val targetDir = File(tab.currentPath, File(archiveItem.path).nameWithoutExtension)

        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Extracting archive...") }
            archiveEngine.extractArchive(File(archiveItem.path), targetDir).collect { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
                if (progress.isComplete) {
                    _uiState.update { it.copy(isOperationInProgress = false, operationProgress = null, statusMessage = "Archive extracted") }
                    refreshTab(tabIndex)
                }
            }
        }
    }

    fun cancelOperation() {
        operationJob?.cancel()
        operationJob = null
        _uiState.update {
            it.copy(
                isOperationInProgress = false,
                operationProgress = null,
                statusMessage = "Operation cancelled"
            )
        }
    }

    // ==========================================
    // Back & Hierarchy Navigation
    // ==========================================

    fun canNavigateUp(): Boolean {
        val activeTab = _uiState.value.activeTab ?: return false
        if (activeTab.isSelectionMode) return true
        if (activeTab.historyStack.isNotEmpty()) return true

        val current = File(activeTab.currentPath)
        val canonicalCurrent = runCatching { current.canonicalPath }.getOrElse { current.absolutePath }
        val canonicalRoot = runCatching { File(rootPath).canonicalPath }.getOrElse { rootPath }
        if (canonicalCurrent == canonicalRoot || canonicalCurrent == "/storage/emulated/0" || canonicalCurrent == "/") {
            return _uiState.value.tabs.size > 1
        }
        val parent = current.parentFile ?: return _uiState.value.tabs.size > 1
        return (parent.exists() && parent.canRead()) || _uiState.value.tabs.size > 1
    }

    fun navigateUp(): Boolean {
        val activeIndex = _uiState.value.activeTabIndex
        val activeTab = _uiState.value.activeTab ?: return false

        if (activeTab.isSelectionMode) {
            clearSelection(activeIndex)
            return true
        }

        if (activeTab.historyStack.isNotEmpty()) {
            val previousPath = activeTab.historyStack.last()
            val remainingHistory = activeTab.historyStack.dropLast(1)
            _uiState.update { state ->
                val updatedTabs = state.tabs.toMutableList()
                updatedTabs[activeIndex] = activeTab.copy(
                    currentPath = previousPath,
                    breadcrumbs = buildBreadcrumbs(previousPath),
                    historyStack = remainingHistory,
                    selectedIds = emptySet(),
                    isSelectionMode = false,
                    isLoading = true
                )
                state.copy(tabs = updatedTabs)
            }
            loadDirectory(activeIndex, previousPath, addToHistory = false)
            return true
        }

        val current = File(activeTab.currentPath)
        val canonicalCurrent = runCatching { current.canonicalPath }.getOrElse { current.absolutePath }
        val canonicalRoot = runCatching { File(rootPath).canonicalPath }.getOrElse { rootPath }
        if (canonicalCurrent != canonicalRoot && canonicalCurrent != "/storage/emulated/0" && canonicalCurrent != "/") {
            val parent = current.parentFile
            if (parent != null && parent.exists() && parent.canRead()) {
                loadDirectory(activeIndex, parent.absolutePath, addToHistory = false)
                return true
            }
        }

        if (_uiState.value.tabs.size > 1) {
            closeTab(activeIndex)
            return true
        }

        return false
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

    // ==========================================
    // Backward Compatibility Helpers
    // ==========================================

    fun setActivePane(pane: ActivePane) {
        selectTab(if (pane == ActivePane.PANE_A) 0 else 1)
    }

    fun loadPaneA(path: String) = loadDirectory(0, path)
    fun loadPaneB(path: String) = loadDirectory(1, path)
    fun toggleSelectionA(id: String) = toggleSelect(0, id)
    fun toggleSelectionB(id: String) = toggleSelect(1, id)

    fun copySelectedToOpposingPane() {
        val sourceIndex = _uiState.value.activeTabIndex
        val targetIndex = if (sourceIndex == 0) 1 else 0
        copySelected(sourceIndex)
        pasteClipboard(targetIndex)
    }

    fun moveSelectedToOpposingPane() {
        val sourceIndex = _uiState.value.activeTabIndex
        val targetIndex = if (sourceIndex == 0) 1 else 0
        cutSelected(sourceIndex)
        pasteClipboard(targetIndex)
    }

    fun syncPanes() {
        val activePath = _uiState.value.activeTab?.currentPath ?: return
        _uiState.value.tabs.indices.forEach { idx ->
            if (idx != _uiState.value.activeTabIndex) {
                loadDirectory(idx, activePath, addToHistory = false)
            }
        }
    }

    fun swapPanes() {
        if (_uiState.value.tabs.size < 2) return
        val path0 = _uiState.value.tabs[0].currentPath
        val path1 = _uiState.value.tabs[1].currentPath
        loadDirectory(0, path1, addToHistory = false)
        loadDirectory(1, path0, addToHistory = false)
    }
}
