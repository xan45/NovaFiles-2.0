package com.novafiles.feature.files

import com.novafiles.core.common.NovaResult
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.FileType
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.StorageStats
import com.novafiles.data.files.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeFileRepository : FileRepository {
    val sampleFiles = listOf(
        FileItem(id = "1", name = "Documents", path = "/storage/emulated/0/Documents", sizeBytes = 0, lastModifiedMs = 1000, fileType = FileType.FOLDER, isDirectory = true),
        FileItem(id = "2", name = "image.png", path = "/storage/emulated/0/image.png", sizeBytes = 2048, lastModifiedMs = 2000, fileType = FileType.IMAGE, isDirectory = false)
    )

    override fun getFiles(directoryPath: String, sortConfig: FileSortConfig, showHidden: Boolean): Flow<NovaResult<List<FileItem>>> {
        return flowOf(NovaResult.Success(sampleFiles))
    }

    override fun searchFiles(directoryPath: String, query: String): Flow<List<FileItem>> {
        return flowOf(sampleFiles)
    }

    override fun getStorageStats(): Flow<StorageStats> {
        return flowOf(StorageStats())
    }
}

class FilesViewModelTest {

    @Test
    fun testInitialUiState() {
        val repo = FakeFileRepository()
        val viewModel = FilesViewModel(repo, initialPath = "/storage/emulated/0")

        assertEquals("/storage/emulated/0", viewModel.uiState.value.currentPath)
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
        assertTrue(viewModel.uiState.value.breadcrumbs.isNotEmpty())
    }

    @Test
    fun testSelectionToggling() {
        val repo = FakeFileRepository()
        val viewModel = FilesViewModel(repo, initialPath = "/storage/emulated/0")

        viewModel.toggleSelection("item_1")
        assertTrue(viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedIds.contains("item_1"))

        viewModel.toggleSelection("item_1")
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertFalse(viewModel.uiState.value.selectedIds.contains("item_1"))
    }

    @Test
    fun testSortOrderChange() {
        val repo = FakeFileRepository()
        val viewModel = FilesViewModel(repo, initialPath = "/storage/emulated/0")

        viewModel.setSortBy(SortBy.SIZE)
        assertEquals(SortBy.SIZE, viewModel.uiState.value.sortConfig.sortBy)
    }

    @Test
    fun testSavedStateHandleRestoration() {
        val repo = FakeFileRepository()
        val handle = androidx.lifecycle.SavedStateHandle().apply {
            set("key_files_current_path", "/storage/emulated/0/Documents")
            set("key_files_search_query", "report")
            set("key_files_is_grid_view", true)
            set("key_files_sort_by", SortBy.SIZE.name)
            set("key_files_selected_ids", arrayListOf("item_1", "item_2"))
        }
        val viewModel = FilesViewModel(repo, savedStateHandle = handle)

        assertEquals("/storage/emulated/0/Documents", viewModel.uiState.value.currentPath)
        assertEquals("report", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.isGridView)
        assertEquals(SortBy.SIZE, viewModel.uiState.value.sortConfig.sortBy)
        assertTrue(viewModel.uiState.value.isSelectionMode)
        assertEquals(2, viewModel.uiState.value.selectedIds.size)
    }
}
