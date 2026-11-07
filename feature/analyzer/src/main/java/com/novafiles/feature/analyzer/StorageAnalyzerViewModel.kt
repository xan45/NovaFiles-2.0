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
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import com.novafiles.core.storage.StorageVolumeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val showDeletedSheet: Boolean = false
)

class StorageAnalyzerViewModel(
    application: Application,
    private val storageVolumeManager: StorageVolumeManager = StorageVolumeManager(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StorageAnalyzerUiState())
    val uiState: StateFlow<StorageAnalyzerUiState> = _uiState.asStateFlow()

    private val trashDir = File(application.filesDir, "trash").apply { mkdirs() }

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
        val standardDirs = listOf(
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_MUSIC,
            Environment.DIRECTORY_DOCUMENTS,
            Environment.DIRECTORY_DOWNLOADS
        ).mapNotNull { Environment.getExternalStoragePublicDirectory(it) }

        val dirsToScan = if (standardDirs.any { it.exists() }) standardDirs else listOf(root)

        val categorySizes = mutableMapOf<FileType, Long>()
        val categoryCounts = mutableMapOf<FileType, Int>()
        val largeFilesFound = mutableListOf<LargeFileItem>()

        for (dir in dirsToScan) {
            if (!dir.exists() || !dir.canRead()) continue
            scanDirectoryRecursively(
                dir = dir,
                depth = 0,
                maxDepth = 5,
                categorySizes = categorySizes,
                categoryCounts = categoryCounts,
                largeFiles = largeFilesFound
            )
        }

        // Compute baseline fallback if storage has files that weren't indexed in public dirs
        val measuredUsed = categorySizes.values.sum()
        val effectiveUsed = if (usedBytes > 0) usedBytes else measuredUsed.coerceAtLeast(1L)

        // Build categories in the visual order matching reference design (Screen 3)
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

        // Read recently deleted from trash folder
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
                if (!f.name.startsWith(".")) {
                    scanDirectoryRecursively(f, depth + 1, maxDepth, categorySizes, categoryCounts, largeFiles)
                }
            } else {
                val size = f.length()
                val type = FileType.fromExtension(f.extension)
                categorySizes[type] = (categorySizes[type] ?: 0L) + size
                categoryCounts[type] = (categoryCounts[type] ?: 0) + 1

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

    fun selectCategory(type: FileType?) {
        viewModelScope.launch {
            if (type == null) {
                _uiState.update { it.copy(selectedCategory = null, selectedCategoryFiles = emptyList()) }
                return@launch
            }

            val matchingFiles = withContext(Dispatchers.IO) {
                val root = Environment.getExternalStorageDirectory()
                val result = mutableListOf<FileItem>()
                findFilesByType(root, type, 0, 4, result)
                result.sortedByDescending { it.sizeBytes }
            }

            _uiState.update {
                it.copy(
                    selectedCategory = type,
                    selectedCategoryFiles = matchingFiles
                )
            }
        }
    }

    private fun findFilesByType(dir: File, targetType: FileType, depth: Int, maxDepth: Int, output: MutableList<FileItem>) {
        if (depth > maxDepth || output.size >= 50) return
        val children = dir.listFiles() ?: return
        for (f in children) {
            if (f.isDirectory) {
                if (!f.name.startsWith(".")) {
                    findFilesByType(f, targetType, depth + 1, maxDepth, output)
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
