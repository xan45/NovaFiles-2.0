package com.novafiles.feature.dualpane

import com.novafiles.core.common.NovaResult
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.FileType
import com.novafiles.core.model.StorageStats
import com.novafiles.data.files.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeDualPaneFileRepository : FileRepository {
    val sampleFiles = listOf(
        FileItem(id = "1", name = "Test", path = "/storage/emulated/0/Test", sizeBytes = 100, lastModifiedMs = 1000, fileType = FileType.FOLDER, isDirectory = true)
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

class DualPaneViewModelTest {

    @Test
    fun testInitialDualPaneState() {
        val repo = FakeDualPaneFileRepository()
        val viewModel = DualPaneViewModel(repo)

        assertEquals(ActivePane.PANE_A, viewModel.uiState.value.activePane)
        assertTrue(viewModel.uiState.value.paneA.currentPath.isNotBlank())
        assertTrue(viewModel.uiState.value.paneB.currentPath.isNotBlank())
    }

    @Test
    fun testSetActivePane() {
        val repo = FakeDualPaneFileRepository()
        val viewModel = DualPaneViewModel(repo)

        viewModel.setActivePane(ActivePane.PANE_B)
        assertEquals(ActivePane.PANE_B, viewModel.uiState.value.activePane)

        viewModel.setActivePane(ActivePane.PANE_A)
        assertEquals(ActivePane.PANE_A, viewModel.uiState.value.activePane)
    }

    @Test
    fun testSwapPanes() {
        val repo = FakeDualPaneFileRepository()
        val viewModel = DualPaneViewModel(repo)

        val initialPathA = viewModel.uiState.value.paneA.currentPath
        val initialPathB = viewModel.uiState.value.paneB.currentPath

        viewModel.swapPanes()

        assertEquals(initialPathB, viewModel.uiState.value.paneA.currentPath)
        assertEquals(initialPathA, viewModel.uiState.value.paneB.currentPath)
    }

    @Test
    fun testSyncPanes() {
        val repo = FakeDualPaneFileRepository()
        val viewModel = DualPaneViewModel(repo)

        viewModel.setActivePane(ActivePane.PANE_A)
        viewModel.syncPanes()

        assertEquals(
            viewModel.uiState.value.paneA.currentPath,
            viewModel.uiState.value.paneB.currentPath
        )
    }

    @Test
    fun testSavedStateHandleRestoration() {
        val repo = FakeDualPaneFileRepository()
        val handle = androidx.lifecycle.SavedStateHandle().apply {
            set("key_dual_pane_a_path", "/storage/emulated/0/Music")
            set("key_dual_pane_b_path", "/storage/emulated/0/Pictures")
            set("key_dual_active_pane", ActivePane.PANE_B.name)
        }
        val viewModel = DualPaneViewModel(repo, savedStateHandle = handle)

        assertEquals(ActivePane.PANE_B, viewModel.uiState.value.activePane)
        assertEquals("/storage/emulated/0/Music", viewModel.uiState.value.paneA.currentPath)
        assertEquals("/storage/emulated/0/Pictures", viewModel.uiState.value.paneB.currentPath)
    }
}
