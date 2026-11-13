package com.novafiles.feature.files

import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig

data class BreadcrumbItem(
    val name: String,
    val path: String
)

enum class ClipboardOp { COPY, MOVE }

enum class FileTypeFilter(val label: String) {
    ALL("All"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Docs & PDF"),
    ARCHIVES("Archives"),
    APKS("APKs")
}

data class ClipboardState(
    val files: List<FileItem> = emptyList(),
    val op: ClipboardOp = ClipboardOp.COPY
)

data class FilesUiState(
    val currentPath: String = "",
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),
    val folders: List<FileItem> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val sortConfig: FileSortConfig = FileSortConfig(),
    val isGridView: Boolean = false,
    val isLoading: Boolean = false,
    val isOperating: Boolean = false,
    val operationMessage: String? = null,
    val clipboard: ClipboardState? = null,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val activeFilter: FileTypeFilter = FileTypeFilter.ALL,
    val bookmarks: List<BreadcrumbItem> = emptyList(),
    val hasStoragePermission: Boolean = true
) {
    val selectedCount: Int get() = selectedIds.size
    val isAllSelected: Boolean get() = (folders.size + files.size) > 0 && selectedIds.size == (folders.size + files.size)
}
