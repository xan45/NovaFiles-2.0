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
}
