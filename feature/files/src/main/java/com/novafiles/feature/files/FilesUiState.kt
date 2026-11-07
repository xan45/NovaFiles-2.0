package com.novafiles.feature.files

import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig

data class BreadcrumbItem(
    val name: String,
    val path: String
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
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val hasStoragePermission: Boolean = true
) {
    val selectedCount: Int get() = selectedIds.size
    val isAllSelected: Boolean get() = (folders.size + files.size) > 0 && selectedIds.size == (folders.size + files.size)
}
