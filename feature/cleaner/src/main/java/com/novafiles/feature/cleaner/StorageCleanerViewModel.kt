package com.novafiles.feature.cleaner

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

enum class JunkCategoryType {
    CACHE,
    TEMP_LOGS,
    EMPTY_FOLDERS,
    OBSOLETE_APKS,
    DUPLICATES
}

data class JunkItem(
    val file: File,
    val category: JunkCategoryType,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val sizeBytes: Long = if (file.isDirectory) 0L else file.length(),
    val isDirectory: Boolean = file.isDirectory
)

data class JunkCategory(
    val type: JunkCategoryType,
    val title: String,
    val description: String,
    val items: List<JunkItem>,
    val totalSizeBytes: Long = items.sumOf { it.sizeBytes },
    val isSelected: Boolean = true,
    val isExpanded: Boolean = false
)

enum class CleanerState {
    IDLE,
    SCANNING,
    READY_TO_CLEAN,
    CLEANING,
    FINISHED
}

data class StorageCleanerUiState(
    val state: CleanerState = CleanerState.SCANNING,
    val scanProgress: Float = 0f,
    val currentScanningPath: String = "",
    val categories: List<JunkCategory> = emptyList(),
    val totalCleanableBytes: Long = 0L,
    val selectedCleanableBytes: Long = 0L,
    val cleanedBytes: Long = 0L,
    val cleanedItemsCount: Int = 0
)

class StorageCleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StorageCleanerUiState())
    val uiState: StateFlow<StorageCleanerUiState> = _uiState.asStateFlow()

    init {
        startScan()
    }

    fun startScan() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    state = CleanerState.SCANNING,
                    scanProgress = 0.1f,
                    currentScanningPath = "Initializing junk scanner..."
                )
            }

            val categories = withContext(Dispatchers.IO) {
                scanJunkCategories()
            }

            val totalBytes = categories.sumOf { it.totalSizeBytes }
            val selectedBytes = categories.filter { it.isSelected }.sumOf { it.totalSizeBytes }

            _uiState.update {
                it.copy(
                    state = CleanerState.READY_TO_CLEAN,
                    scanProgress = 1.0f,
                    categories = categories,
                    totalCleanableBytes = totalBytes,
                    selectedCleanableBytes = selectedBytes
                )
            }
        }
    }

    private fun scanJunkCategories(): List<JunkCategory> {
        val root = Environment.getExternalStorageDirectory()

        val cacheItems = mutableListOf<JunkItem>()
        val tempLogItems = mutableListOf<JunkItem>()
        val emptyFolderItems = mutableListOf<JunkItem>()
        val obsoleteApkItems = mutableListOf<JunkItem>()
        val allSmallFiles = mutableListOf<File>()

        // 1. App Cache directories
        val cacheDirs = listOf(
            File(root, "Android/data"),
            File(root, ".thumbnails"),
            File(root, "DCIM/.thumbnails"),
            getApplication<Application>().cacheDir,
            getApplication<Application>().externalCacheDir
        ).filterNotNull()

        for (cDir in cacheDirs) {
            if (cDir.exists() && cDir.canRead()) {
                cDir.walkTopDown().maxDepth(3).forEach { f ->
                    if (f.isFile && (f.name.endsWith(".thumb") || f.parent?.contains("cache", ignoreCase = true) == true)) {
                        cacheItems.add(JunkItem(file = f, category = JunkCategoryType.CACHE))
                    }
                }
            }
        }

        // 2. Scan Downloads & Documents for temp, logs, APKs, empty folders, duplicates
        val targetDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            File(root, "Download")
        ).filterNotNull()

        for (tDir in targetDirs) {
            if (!tDir.exists() || !tDir.canRead()) continue
            tDir.walkTopDown().maxDepth(4).forEach { f ->
                if (f.isDirectory) {
                    val contents = f.listFiles()
                    if (contents != null && contents.isEmpty() && !f.name.startsWith(".")) {
                        emptyFolderItems.add(JunkItem(file = f, category = JunkCategoryType.EMPTY_FOLDERS))
                    }
                } else if (f.isFile) {
                    val ext = f.extension.lowercase()
                    when (ext) {
                        "tmp", "temp", "log", "bak" -> {
                            tempLogItems.add(JunkItem(file = f, category = JunkCategoryType.TEMP_LOGS))
                        }
                        "apk", "apks", "xapk" -> {
                            obsoleteApkItems.add(JunkItem(file = f, category = JunkCategoryType.OBSOLETE_APKS))
                        }
                    }

                    if (f.length() in 100_000L..50_000_000L) {
                        allSmallFiles.add(f)
                    }
                }
            }
        }

        // 3. Duplicate detection by size grouping and full SHA-256 stream checksum
        val duplicates = mutableListOf<JunkItem>()
        val bySize = allSmallFiles.groupBy { it.length() }.filter { it.value.size > 1 }
        for ((_, files) in bySize) {
            val byHash = files.groupBy { computeFileChecksum(it) }
            for ((hash, matchingFiles) in byHash) {
                if (hash.isNotEmpty() && matchingFiles.size > 1) {
                    // Keep original (first), mark subsequent as duplicate junk
                    matchingFiles.drop(1).forEach { dup ->
                        duplicates.add(JunkItem(file = dup, category = JunkCategoryType.DUPLICATES))
                    }
                }
            }
        }

        return listOf(
            JunkCategory(
                type = JunkCategoryType.CACHE,
                title = "App Cache & Thumbnails",
                description = "Temporary cached media and UI thumbnails",
                items = cacheItems,
                isSelected = true
            ),
            JunkCategory(
                type = JunkCategoryType.TEMP_LOGS,
                title = "Temporary & Log Files",
                description = "Residual log and temporary session files",
                items = tempLogItems,
                isSelected = true
            ),
            JunkCategory(
                type = JunkCategoryType.OBSOLETE_APKS,
                title = "Obsolete APK Packages",
                description = "Previously downloaded Android packages",
                items = obsoleteApkItems,
                isSelected = true
            ),
            JunkCategory(
                type = JunkCategoryType.EMPTY_FOLDERS,
                title = "Empty Folders",
                description = "Abandoned directories containing no files",
                items = emptyFolderItems,
                isSelected = true
            ),
            JunkCategory(
                type = JunkCategoryType.DUPLICATES,
                title = "Duplicate Files",
                description = "Identical files wasting redundant storage space",
                items = duplicates,
                isSelected = false // duplicates unselected by default for safety
            )
        )
    }

    private fun computeFileChecksum(file: File): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    if (read > 0) md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    fun toggleCategorySelection(type: JunkCategoryType) {
        val updated = _uiState.value.categories.map {
            if (it.type == type) it.copy(isSelected = !it.isSelected) else it
        }
        val selectedBytes = updated.filter { it.isSelected }.sumOf { it.totalSizeBytes }
        _uiState.update {
            it.copy(
                categories = updated,
                selectedCleanableBytes = selectedBytes
            )
        }
    }

    fun toggleCategoryExpanded(type: JunkCategoryType) {
        val updated = _uiState.value.categories.map {
            if (it.type == type) it.copy(isExpanded = !it.isExpanded) else it
        }
        _uiState.update { it.copy(categories = updated) }
    }

    fun cleanSelected() {
        val selectedCategories = _uiState.value.categories.filter { it.isSelected }
        if (selectedCategories.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(state = CleanerState.CLEANING) }

            var freedBytes = 0L
            var itemsDeleted = 0

            withContext(Dispatchers.IO) {
                for (cat in selectedCategories) {
                    for (item in cat.items) {
                        try {
                            if (item.file.exists()) {
                                val size = item.sizeBytes
                                val deleted = if (item.isDirectory) item.file.deleteRecursively() else item.file.delete()
                                if (deleted) {
                                    freedBytes += size
                                    itemsDeleted++
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore permission-restricted entries
                        }
                    }
                }
            }

            _uiState.update {
                it.copy(
                    state = CleanerState.FINISHED,
                    cleanedBytes = freedBytes,
                    cleanedItemsCount = itemsDeleted
                )
            }
        }
    }

    fun reset() {
        _uiState.update { it.copy(state = CleanerState.SCANNING) }
        startScan()
    }
}
