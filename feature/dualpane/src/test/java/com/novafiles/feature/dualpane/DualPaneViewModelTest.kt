package com.novafiles.feature.dualpane

import com.novafiles.core.common.NovaResult
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.FileType
import com.novafiles.core.model.StorageStats
import com.novafiles.data.files.FileRepository
import com.novafiles.feature.files.ClipboardOp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeDualPaneFileRepository : FileRepository {
    val sampleFiles = listOf(
        FileItem(id = "1", name = "Test", path = "/storage/emulated/0/Test", sizeBytes = 100, lastModifiedMs = 1000, fileType = FileType.FOLDER, isDirectory = true),
        FileItem(id = "2", name = "doc.pdf", path = "/storage/emulated/0/doc.pdf", sizeBytes = 2048, lastModifiedMs = 2000, fileType = FileType.DOCUMENT, isDirectory = false)
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
        assertEquals(2, viewModel.uiState.value.tabs.size)
        assertTrue(viewModel.uiState.value.paneA.currentPath.isNotBlank())
        assertTrue(viewModel.uiState.value.paneB.currentPath.isNotBlank())
    }

    @Test
    fun testSetActivePane() {
        val repo = FakeDualPaneFileRepository()
        val viewModel = DualPaneViewModel(repo)

        viewModel.setActivePane(ActivePane.PANE_B)
        assertEquals(ActivePane.PANE_B, viewModel.uiState.value.activePane)
        assertEquals(1, viewModel.uiState.value.activeTabIndex)

        viewModel.setActivePane(ActivePane.PANE_A)
        assertEquals(ActivePane.PANE_A, viewModel.uiState.value.activePane)
        assertEquals(0, viewModel.uiState.value.activeTabIndex)
    }

    @Test
    fun testAddAndCloseTabs() {
        val repo = FakeDualPaneFileRepository()
        val viewModel = DualPaneViewModel(repo)

        // Starts with 2 tabs
        assertEquals(2, viewModel.uiState.value.tabs.size)

        // Add 3rd and 4th tab (Phase 3A requirement: at least 4 simultaneously open tabs)
        viewModel.addTab("/storage/emulated/0/Pictures")
        assertEquals(3, viewModel.uiState.value.tabs.size)
        assertEquals(2, viewModel.uiState.value.activeTabIndex)

        viewModel.addTab("/storage/emulated/0/DCIM")
        assertEquals(4, viewModel.uiState.value.tabs.size)
        assertEquals(3, viewModel.uiState.value.activeTabIndex)

        // Switch to tab 1
        viewModel.selectTab(1)
        assertEquals(1, viewModel.uiState.value.activeTabIndex)

        // Close tab 3
        viewModel.closeTab(3)
        assertEquals(3, viewModel.uiState.value.tabs.size)

        // Close down to 1 tab
        viewModel.closeTab(2)
        viewModel.closeTab(1)
        assertEquals(1, viewModel.uiState.value.tabs.size)

        // Cannot close the last tab
        viewModel.closeTab(0)
        assertEquals(1, viewModel.uiState.value.tabs.size)
    }

    @Test
    fun testIndependentFolderNavigation() {
        val repo = FakeDualPaneFileRepository()
        val viewModel = DualPaneViewModel(repo)

        viewModel.loadDirectory(0, "/storage/emulated/0/Music")
        viewModel.loadDirectory(1, "/storage/emulated/0/DCIM")

        assertEquals("/storage/emulated/0/Music", viewModel.uiState.value.tabs[0].currentPath)
        assertEquals("/storage/emulated/0/DCIM", viewModel.uiState.value.tabs[1].currentPath)
    }

    @Test
    fun testIndependentSelectionAndClipboard() {
        val repo = FakeDualPaneFileRepository()
        val viewModel = DualPaneViewModel(repo)

        // Select item in tab 0
        viewModel.toggleSelect(0, "1")
        assertTrue(viewModel.uiState.value.tabs[0].selectedIds.contains("1"))
        assertTrue(viewModel.uiState.value.tabs[0].isSelectionMode)
        assertFalse(viewModel.uiState.value.tabs[1].isSelectionMode)

        // Copy selected
        viewModel.copySelected(0)
        assertNotNull(viewModel.uiState.value.clipboard)
        assertEquals(ClipboardOp.COPY, viewModel.uiState.value.clipboard?.op)
        assertFalse(viewModel.uiState.value.tabs[0].isSelectionMode)

        // Clear clipboard
        viewModel.clearClipboard()
        assertEquals(null, viewModel.uiState.value.clipboard)
    }

    @Test
    fun testSwapPanes() {
        val repo = FakeDualPaneFileRepository()
        val viewModel = DualPaneViewModel(repo)

        val initialPathA = viewModel.uiState.value.tabs[0].currentPath
        val initialPathB = viewModel.uiState.value.tabs[1].currentPath

        viewModel.swapPanes()

        assertEquals(initialPathB, viewModel.uiState.value.tabs[0].currentPath)
        assertEquals(initialPathA, viewModel.uiState.value.tabs[1].currentPath)
    }

    @Test
    fun testSyncPanes() {
        val repo = FakeDualPaneFileRepository()
        val viewModel = DualPaneViewModel(repo)

        viewModel.selectTab(0)
        viewModel.syncPanes()

        assertEquals(
            viewModel.uiState.value.tabs[0].currentPath,
            viewModel.uiState.value.tabs[1].currentPath
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
        assertEquals(1, viewModel.uiState.value.activeTabIndex)
        assertEquals("/storage/emulated/0/Music", viewModel.uiState.value.tabs[0].currentPath)
        assertEquals("/storage/emulated/0/Pictures", viewModel.uiState.value.tabs[1].currentPath)
    }
}
