package com.novafiles.feature.analyzer

import android.app.Application
import android.os.Environment
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.designsystem.theme.ColorBrightYellow
import com.novafiles.core.designsystem.theme.ColorCoralRed
import com.novafiles.core.designsystem.theme.ColorIndigoPrimary
import com.novafiles.core.designsystem.theme.ColorMintGreen
import com.novafiles.core.designsystem.theme.ColorNeonCyan
import com.novafiles.core.designsystem.theme.ColorVioletSecondary
import com.novafiles.core.designsystem.theme.ColorWarmOrange
import com.novafiles.core.model.ConflictResolution
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileOperationProgress
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.FileType
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.storage.ArchiveEngine
import com.novafiles.core.storage.FileOperationsEngine
import com.novafiles.core.storage.FileSystemDataSource
import com.novafiles.core.storage.StorageVolumeManager
import com.novafiles.data.files.FileRepository
import com.novafiles.data.files.FileRepositoryImpl
import com.novafiles.feature.files.ClipboardOp
import com.novafiles.feature.files.ClipboardState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class CategoryStorageItem(
    val type: FileType,
    val name: String,
    val sizeBytes: Long,
    val fileCount: Int,
    val percentageOfUsed: Float,
    val color: Color
)

data class LargeFileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val sizeBytes: Long = file.length(),
    val fileType: FileType = FileType.fromExtension(file.extension),
    val lastModified: Long = file.lastModified()
)

data class DeletedFileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val sizeBytes: Long = if (file.isDirectory) 0L else file.length(),
    val isDirectory: Boolean = file.isDirectory,
    val itemCount: Int = if (file.isDirectory) file.listFiles()?.size ?: 0 else 1,
    val deletedTimestamp: Long = file.lastModified(),
    val daysRemaining: Int = 30
)

data class StorageAnalyzerUiState(
    val isLoading: Boolean = true,
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val categories: List<CategoryStorageItem> = emptyList(),
    val largeFiles: List<LargeFileItem> = emptyList(),
    val recentlyDeleted: List<DeletedFileItem> = emptyList(),
    val selectedCategory: FileType? = null,
    val selectedCategoryFiles: List<FileItem> = emptyList(),
    val isCategoryLoading: Boolean = false,
    val isGridView: Boolean = false,
    val sortConfig: FileSortConfig = FileSortConfig(sortBy = SortBy.DATE, direction = SortDirection.DESCENDING),
    val searchQuery: String = "",
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val clipboard: ClipboardState? = null,
    val isOperationInProgress: Boolean = false,
    val operationProgress: FileOperationProgress? = null,
    val statusMessage: String? = null,
    val showDeletedSheet: Boolean = false
) {
    val displayedCategoryFiles: List<FileItem>
        get() {
            val list = if (searchQuery.isBlank()) {
                selectedCategoryFiles
            } else {
                selectedCategoryFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
            val comparator = Comparator<FileItem> { a, b ->
                val comp = when (sortConfig.sortBy) {
                    SortBy.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                    SortBy.SIZE -> a.sizeBytes.compareTo(b.sizeBytes)
                    SortBy.DATE -> a.lastModifiedMs.compareTo(b.lastModifiedMs)
                    SortBy.TYPE -> a.fileType.name.compareTo(b.fileType.name)
                }
                if (sortConfig.direction == SortDirection.DESCENDING) -comp else comp
            }
            return list.sortedWith(comparator)
        }
}

class StorageAnalyzerViewModel @JvmOverloads constructor(
    application: Application,
    private val storageVolumeManager: StorageVolumeManager = StorageVolumeManager(application),
    private val fileRepository: FileRepository = FileRepositoryImpl(FileSystemDataSource(), storageVolumeManager),
    private val fileOperationsEngine: FileOperationsEngine = FileOperationsEngine(),
    private val archiveEngine: ArchiveEngine = ArchiveEngine()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StorageAnalyzerUiState())
    val uiState: StateFlow<StorageAnalyzerUiState> = _uiState.asStateFlow()

    private val trashDir = File(application.filesDir, "trash").apply { mkdirs() }
    private val categoryFilesCache = ConcurrentHashMap<FileType, CopyOnWriteArrayList<FileItem>>()
    private var operationJob: Job? = null

    init {
        scanStorage()
    }

    fun scanStorage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val stats = withContext(Dispatchers.IO) {
                storageVolumeManager.getPrimaryStorageStats()
            }

            val (categories, largeFiles, deleted) = withContext(Dispatchers.IO) {
                performDeepScan(stats.usedBytes, stats.totalBytes)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    usedBytes = stats.usedBytes,
                    totalBytes = stats.totalBytes,
                    freeBytes = stats.freeBytes,
                    categories = categories,
                    largeFiles = largeFiles,
                    recentlyDeleted = deleted
                )
            }
        }
    }

    private fun performDeepScan(
        usedBytes: Long,
        totalBytes: Long
    ): Triple<List<CategoryStorageItem>, List<LargeFileItem>, List<DeletedFileItem>> {
        val root = Environment.getExternalStorageDirectory()

        val categorySizes = mutableMapOf<FileType, Long>()
        val categoryCounts = mutableMapOf<FileType, Int>()
        val largeFilesFound = mutableListOf<LargeFileItem>()
        categoryFilesCache.clear()

        scanDirectoryRecursively(
            dir = root,
            depth = 0,
            maxDepth = 5,
            categorySizes = categorySizes,
            categoryCounts = categoryCounts,
            largeFiles = largeFilesFound
        )

        // Compute baseline fallback if storage has files that weren't indexed in public dirs
        val measuredUsed = categorySizes.values.sum()
        val effectiveUsed = if (usedBytes > 0) usedBytes else measuredUsed.coerceAtLeast(1L)

        // Build categories in visual order matching reference design
        val categoryList = listOf(
            CategoryStorageItem(
                type = FileType.IMAGE,
                name = "Photos",
                sizeBytes = categorySizes[FileType.IMAGE] ?: 0L,
                fileCount = categoryCounts[FileType.IMAGE] ?: 0,
                percentageOfUsed = ((categorySizes[FileType.IMAGE] ?: 0L).toFloat() / effectiveUsed.toFloat()).coerceIn(0f, 1f),
                color = ColorVioletSecondary
            ),
            CategoryStorageItem(
                type = FileType.VIDEO,
                name = "Videos",
                sizeBytes = categorySizes[FileType.VIDEO] ?: 0L,
                fileCount = categoryCounts[FileType.VIDEO] ?: 0,
                percentageOfUsed = ((categorySizes[FileType.VIDEO] ?: 0L).toFloat() / effectiveUsed.toFloat()).coerceIn(0f, 1f),
                color = ColorCoralRed
            ),
            CategoryStorageItem(
                type = FileType.DOCUMENT,
                name = "Documents",
                sizeBytes = categorySizes[FileType.DOCUMENT] ?: 0L,
                fileCount = categoryCounts[FileType.DOCUMENT] ?: 0,
                percentageOfUsed = ((categorySizes[FileType.DOCUMENT] ?: 0L).toFloat() / effectiveUsed.toFloat()).coerceIn(0f, 1f),
                color = ColorIndigoPrimary
            ),
            CategoryStorageItem(
                type = FileType.AUDIO,
                name = "Audio",
                sizeBytes = categorySizes[FileType.AUDIO] ?: 0L,
                fileCount = categoryCounts[FileType.AUDIO] ?: 0,
                percentageOfUsed = ((categorySizes[FileType.AUDIO] ?: 0L).toFloat() / effectiveUsed.toFloat()).coerceIn(0f, 1f),
                color = ColorWarmOrange
            ),
            CategoryStorageItem(
                type = FileType.APK,
                name = "APKs",
                sizeBytes = categorySizes[FileType.APK] ?: 0L,
                fileCount = categoryCounts[FileType.APK] ?: 0,
                percentageOfUsed = ((categorySizes[FileType.APK] ?: 0L).toFloat() / effectiveUsed.toFloat()).coerceIn(0f, 1f),
                color = ColorMintGreen
            ),
            CategoryStorageItem(
                type = FileType.ARCHIVE,
                name = "Archives",
                sizeBytes = categorySizes[FileType.ARCHIVE] ?: 0L,
                fileCount = categoryCounts[FileType.ARCHIVE] ?: 0,
                percentageOfUsed = ((categorySizes[FileType.ARCHIVE] ?: 0L).toFloat() / effectiveUsed.toFloat()).coerceIn(0f, 1f),
                color = ColorNeonCyan
            ),
            CategoryStorageItem(
                type = FileType.OTHER,
                name = "Other",
                sizeBytes = categorySizes[FileType.OTHER] ?: 0L,
                fileCount = categoryCounts[FileType.OTHER] ?: 0,
                percentageOfUsed = ((categorySizes[FileType.OTHER] ?: 0L).toFloat() / effectiveUsed.toFloat()).coerceIn(0f, 1f),
                color = ColorBrightYellow
            )
        )

        val trashFiles = trashDir.listFiles()?.map { file ->
            DeletedFileItem(
                file = file,
                daysRemaining = calculateDaysRemaining(file.lastModified())
            )
        } ?: emptyList()

        return Triple(
            categoryList,
            largeFilesFound.sortedByDescending { it.sizeBytes }.take(30),
            trashFiles
        )
    }

    private fun scanDirectoryRecursively(
        dir: File,
        depth: Int,
        maxDepth: Int,
        categorySizes: MutableMap<FileType, Long>,
        categoryCounts: MutableMap<FileType, Int>,
        largeFiles: MutableList<LargeFileItem>
    ) {
        if (depth > maxDepth) return
        val files = dir.listFiles() ?: return

        for (f in files) {
            if (f.isDirectory) {
                val name = f.name
                if (!name.startsWith(".") && !name.equals("Android", ignoreCase = true)) {
                    scanDirectoryRecursively(f, depth + 1, maxDepth, categorySizes, categoryCounts, largeFiles)
                }
            } else {
                val size = f.length()
                val type = FileType.fromExtension(f.extension)
                categorySizes[type] = (categorySizes[type] ?: 0L) + size
                categoryCounts[type] = (categoryCounts[type] ?: 0) + 1

                val list = categoryFilesCache.getOrPut(type) { CopyOnWriteArrayList() }
                list.add(
                    FileItem(
                        id = f.absolutePath,
                        name = f.name,
                        path = f.absolutePath,
                        sizeBytes = size,
                        lastModifiedMs = f.lastModified(),
                        fileType = type
                    )
                )

                if (size > 50 * 1024 * 1024L) { // > 50 MB
                    largeFiles.add(LargeFileItem(file = f))
                }
            }
        }
    }

    private fun calculateDaysRemaining(deletedMs: Long): Int {
        val ageMs = System.currentTimeMillis() - deletedMs
        val remainingDays = 30 - (ageMs / (24 * 3600 * 1000L)).toInt()
        return remainingDays.coerceIn(0, 30)
    }

    // ==========================================
    // Full-Screen Category Browser Management
    // ==========================================

    fun selectCategory(type: FileType?) {
        if (type == null) {
            clearCategorySelection()
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedCategory = type,
                    isCategoryLoading = true,
                    searchQuery = "",
                    selectedIds = emptySet(),
                    isSelectionMode = false
                )
            }

            val cached = categoryFilesCache[type]?.filter { File(it.path).exists() } ?: emptyList()
            val files = if (cached.isNotEmpty()) {
                cached
            } else {
                withContext(Dispatchers.IO) {
                    scanCategoryFiles(type)
                }
            }

            _uiState.update {
                it.copy(
                    selectedCategory = type,
                    selectedCategoryFiles = files,
                    isCategoryLoading = false
                )
            }
        }
    }

    private fun scanCategoryFiles(targetType: FileType): List<FileItem> {
        val root = Environment.getExternalStorageDirectory()
        val output = mutableListOf<FileItem>()
        scanDirectoryForType(root, targetType, 0, 5, output)
        val list = CopyOnWriteArrayList(output)
        categoryFilesCache[targetType] = list
        return output
    }

    private fun scanDirectoryForType(
        dir: File,
        targetType: FileType,
        depth: Int,
        maxDepth: Int,
        output: MutableList<FileItem>
    ) {
        if (depth > maxDepth) return
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                val name = f.name
                if (!name.startsWith(".") && !name.equals("Android", ignoreCase = true)) {
                    scanDirectoryForType(f, targetType, depth + 1, maxDepth, output)
                }
            } else {
                if (FileType.fromExtension(f.extension) == targetType) {
                    output.add(
                        FileItem(
                            id = f.absolutePath,
                            name = f.name,
                            path = f.absolutePath,
                            sizeBytes = f.length(),
                            lastModifiedMs = f.lastModified(),
                            fileType = targetType
                        )
                    )
                }
            }
        }
    }

    fun clearCategorySelection() {
        _uiState.update {
            it.copy(
                selectedCategory = null,
                selectedCategoryFiles = emptyList(),
                searchQuery = "",
                selectedIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun toggleLayoutMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun setSortConfig(config: FileSortConfig) {
        _uiState.update { it.copy(sortConfig = config) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSelect(id: String) {
        _uiState.update { state ->
            val newSelected = if (state.selectedIds.contains(id)) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(
                selectedIds = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.displayedCategoryFiles.map { it.id }.toSet()
            state.copy(
                selectedIds = allIds,
                isSelectionMode = allIds.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    // ==========================================
    // File Operations: Cut, Copy, Paste, Delete, Rename, Compress
    // ==========================================

    fun copySelected() {
        val selectedFiles = _uiState.value.displayedCategoryFiles.filter { _uiState.value.selectedIds.contains(it.id) }
        if (selectedFiles.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(files = selectedFiles, op = ClipboardOp.COPY),
                statusMessage = "Copied ${selectedFiles.size} items to clipboard"
            )
        }
        clearSelection()
    }

    fun cutSelected() {
        val selectedFiles = _uiState.value.displayedCategoryFiles.filter { _uiState.value.selectedIds.contains(it.id) }
        if (selectedFiles.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(files = selectedFiles, op = ClipboardOp.MOVE),
                statusMessage = "Cut ${selectedFiles.size} items to clipboard"
            )
        }
        clearSelection()
    }

    fun copySingleFile(item: FileItem) {
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(files = listOf(item), op = ClipboardOp.COPY),
                statusMessage = "Copied ${item.name} to clipboard"
            )
        }
    }

    fun cutSingleFile(item: FileItem) {
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(files = listOf(item), op = ClipboardOp.MOVE),
                statusMessage = "Cut ${item.name} to clipboard"
            )
        }
    }

    fun clearClipboard() {
        _uiState.update { it.copy(clipboard = null, statusMessage = null) }
    }

    fun pasteClipboard(targetDir: File? = null) {
        val clipboard = _uiState.value.clipboard ?: return
        if (clipboard.files.isEmpty()) return

        val destination = targetDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
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
                fileOperationsEngine.copyFiles(sources, destination, ConflictResolution.RENAME_NEW)
            } else {
                fileOperationsEngine.moveFiles(sources, destination, ConflictResolution.RENAME_NEW)
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
                    refreshAfterModification()
                }
            }
        }
    }

    fun deleteSelected() {
        val selectedFiles = _uiState.value.displayedCategoryFiles.filter { _uiState.value.selectedIds.contains(it.id) }
        if (selectedFiles.isEmpty()) return

        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Deleting ${selectedFiles.size} items...") }
            fileOperationsEngine.deleteFiles(selectedFiles.map { File(it.path) }).collect { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
                if (progress.isComplete) {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgress = null,
                            statusMessage = "Deleted successfully"
                        )
                    }
                    clearSelection()
                    removeFilesFromCategoryAndStats(selectedFiles)
                }
            }
        }
    }

    fun deleteSingleFile(item: FileItem) {
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Deleting ${item.name}...") }
            fileOperationsEngine.deleteFiles(listOf(File(item.path))).collect { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
                if (progress.isComplete) {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgress = null,
                            statusMessage = "Deleted ${item.name}"
                        )
                    }
                    removeFilesFromCategoryAndStats(listOf(item))
                }
            }
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            val result = fileOperationsEngine.renameFile(File(item.path), newName)
            if (result.isSuccess) {
                val oldFile = File(item.path)
                val newFile = File(oldFile.parentFile, newName)
                val updatedItem = item.copy(
                    id = newFile.absolutePath,
                    name = newName,
                    path = newFile.absolutePath,
                    fileType = FileType.fromExtension(newFile.extension),
                    lastModifiedMs = newFile.lastModified()
                )
                updateFileInState(item, updatedItem)
            } else {
                _uiState.update { it.copy(statusMessage = "Rename failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun compressItems(items: List<FileItem>, zipName: String) {
        if (items.isEmpty()) return
        val parentDir = File(items.first().path).parentFile ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val finalName = if (zipName.endsWith(".zip", ignoreCase = true)) zipName else "$zipName.zip"
        val targetZip = File(parentDir, finalName)

        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.update { it.copy(isOperationInProgress = true, statusMessage = "Compressing items...") }
            archiveEngine.createZipArchive(items.map { File(it.path) }, targetZip).collect { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
                if (progress.isComplete) {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgress = null,
                            statusMessage = "Archive created: $finalName"
                        )
                    }
                    clearSelection()
                    refreshAfterModification()
                }
            }
        }
    }

    fun cancelOperation() {
        operationJob?.cancel()
        _uiState.update {
            it.copy(
                isOperationInProgress = false,
                operationProgress = null,
                statusMessage = "Operation cancelled"
            )
        }
    }

    private fun removeFilesFromCategoryAndStats(deletedItems: List<FileItem>) {
        val deletedIds = deletedItems.map { it.id }.toSet()
        val deletedSize = deletedItems.sumOf { it.sizeBytes }
        val deletedCount = deletedItems.size
        val currentCat = _uiState.value.selectedCategory

        val updatedFiles = _uiState.value.selectedCategoryFiles.filterNot { deletedIds.contains(it.id) }
        if (currentCat != null) {
            categoryFilesCache[currentCat]?.removeAll { deletedIds.contains(it.id) }
        }

        val effUsed = (_uiState.value.usedBytes - deletedSize).coerceAtLeast(1L)
        val updatedCategories = _uiState.value.categories.map { cat ->
            if (cat.type == currentCat) {
                val newSize = (cat.sizeBytes - deletedSize).coerceAtLeast(0L)
                val newCount = (cat.fileCount - deletedCount).coerceAtLeast(0)
                cat.copy(
                    sizeBytes = newSize,
                    fileCount = newCount,
                    percentageOfUsed = (newSize.toFloat() / effUsed.toFloat()).coerceIn(0f, 1f)
                )
            } else {
                cat.copy(
                    percentageOfUsed = (cat.sizeBytes.toFloat() / effUsed.toFloat()).coerceIn(0f, 1f)
                )
            }
        }

        val newUsed = (_uiState.value.usedBytes - deletedSize).coerceAtLeast(0L)
        val newFree = _uiState.value.freeBytes + deletedSize

        _uiState.update { state ->
            state.copy(
                selectedCategoryFiles = updatedFiles,
                categories = updatedCategories,
                usedBytes = newUsed,
                freeBytes = newFree,
                largeFiles = state.largeFiles.filterNot { deletedIds.contains(it.path) }
            )
        }
    }

    private fun updateFileInState(oldItem: FileItem, newItem: FileItem) {
        val currentCat = _uiState.value.selectedCategory
        val updatedFiles = _uiState.value.selectedCategoryFiles.map { if (it.id == oldItem.id) newItem else it }
        if (currentCat != null) {
            val cached = categoryFilesCache[currentCat]
            val idx = cached?.indexOfFirst { it.id == oldItem.id } ?: -1
            if (idx >= 0 && cached != null) {
                cached[idx] = newItem
            }
        }
        _uiState.update { it.copy(selectedCategoryFiles = updatedFiles) }
    }

    fun refreshAfterModification() {
        viewModelScope.launch {
            val stats = withContext(Dispatchers.IO) {
                storageVolumeManager.getPrimaryStorageStats()
            }
            val (categories, largeFiles, deleted) = withContext(Dispatchers.IO) {
                performDeepScan(stats.usedBytes, stats.totalBytes)
            }
            val currentCat = _uiState.value.selectedCategory
            val refreshedFiles = if (currentCat != null) {
                categoryFilesCache[currentCat]?.filter { File(it.path).exists() } ?: emptyList()
            } else emptyList()

            _uiState.update {
                it.copy(
                    usedBytes = stats.usedBytes,
                    totalBytes = stats.totalBytes,
                    freeBytes = stats.freeBytes,
                    categories = categories,
                    largeFiles = largeFiles,
                    recentlyDeleted = deleted,
                    selectedCategoryFiles = refreshedFiles
                )
            }
        }
    }

    // ==========================================
    // Trash / Recycle Bin Operations
    // ==========================================

    fun restoreDeletedItem(item: DeletedFileItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (item.file.exists()) {
                    val restoredDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    item.file.renameTo(File(restoredDir, item.name))
                }
            }
            _uiState.update { current ->
                current.copy(recentlyDeleted = current.recentlyDeleted.filter { it.path != item.path })
            }
        }
    }

    fun permanentlyDeleteTrashItem(item: DeletedFileItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (item.file.exists()) {
                    if (item.file.isDirectory) item.file.deleteRecursively() else item.file.delete()
                }
            }
            _uiState.update { current ->
                current.copy(recentlyDeleted = current.recentlyDeleted.filter { it.path != item.path })
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                trashDir.listFiles()?.forEach {
                    if (it.isDirectory) it.deleteRecursively() else it.delete()
                }
            }
            _uiState.update { it.copy(recentlyDeleted = emptyList()) }
        }
    }

    fun toggleDeletedSheet(show: Boolean) {
        _uiState.update { it.copy(showDeletedSheet = show) }
    }
}
