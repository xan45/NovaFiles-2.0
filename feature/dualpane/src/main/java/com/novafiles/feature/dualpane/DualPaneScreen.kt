package com.novafiles.feature.dualpane

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import com.novafiles.core.ui.components.NovaConfirmDialog
import com.novafiles.core.ui.components.NovaFileGridItem
import com.novafiles.core.ui.components.NovaFileRow
import com.novafiles.core.ui.components.NovaInputDialog
import com.novafiles.core.ui.components.NovaLinearProgress
import com.novafiles.core.ui.components.NovaLoadingState
import com.novafiles.feature.files.BrowsingSettingsSheet
import com.novafiles.feature.files.FileActionHelper
import com.novafiles.feature.files.FileItemContextMenuSheet
import com.novafiles.feature.files.FilePropertiesSheet
import com.novafiles.feature.files.MultiSelectionBottomDock
import com.novafiles.feature.files.MultiSelectionHeader
import com.novafiles.feature.files.SharedClipboardDock
import kotlinx.coroutines.launch
import java.io.File

/**
 * Full-screen tabbed Multi-Pane Manager.
 * Completely replaces the legacy 2-pane vertical split layout with an N-tab browsing experience.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DualPaneScreen(
    uiState: DualPaneUiState,
    onSetActivePane: (ActivePane) -> Unit = {},
    onFolderClickA: (String) -> Unit = {},
    onFolderClickB: (String) -> Unit = {},
    onToggleSelectA: (String) -> Unit = {},
    onToggleSelectB: (String) -> Unit = {},
    onCopyAcross: () -> Unit = {},
    onMoveAcross: () -> Unit = {},
    onSyncPanes: () -> Unit = {},
    onSwapPanes: () -> Unit = {},
    onFileClick: (FileItem) -> Unit = {},
    canNavigateUp: Boolean = false,
    onNavigateBack: () -> Unit = {},
    viewModel: DualPaneViewModel? = null,
    onAddTab: () -> Unit = { viewModel?.addTab() },
    onCloseTab: (Int) -> Unit = { viewModel?.closeTab(it) },
    onSelectTab: (Int) -> Unit = { viewModel?.selectTab(it) },
    onFolderClick: (Int, String) -> Unit = { index, path -> viewModel?.loadDirectory(index, path) },
    onToggleSelect: (Int, String) -> Unit = { index, id -> viewModel?.toggleSelect(index, id) },
    onSelectAll: (Int) -> Unit = { viewModel?.selectAll(it) },
    onClearSelection: (Int) -> Unit = { viewModel?.clearSelection(it) },
    onCopySelected: (Int) -> Unit = { viewModel?.copySelected(it) },
    onCutSelected: (Int) -> Unit = { viewModel?.cutSelected(it) },
    onPasteClipboard: (Int) -> Unit = { viewModel?.pasteClipboard(it) },
    onPasteIntoFolder: (Int, String) -> Unit = { index, folderPath -> viewModel?.pasteClipboard(index, folderPath) },
    onClearClipboard: () -> Unit = { viewModel?.clearClipboard() },
    onDeleteSelected: (Int) -> Unit = { viewModel?.deleteSelected(it) },
    onDeleteSingleFile: (FileItem, Int) -> Unit = { item, index -> viewModel?.deleteSingleFile(item, index) },
    onRenameItem: (FileItem, String, Int) -> Unit = { item, name, index -> viewModel?.renameItem(item, name, index) },
    onCreateFolder: (String, Int) -> Unit = { name, index -> viewModel?.createFolder(name, index) },
    onCompressItems: (List<FileItem>, String, Int) -> Unit = { items, name, index -> viewModel?.compressItems(items, name, index) },
    onExtractArchive: (FileItem, Int) -> Unit = { item, index -> viewModel?.extractArchive(item, index) },
    onCancelOperation: () -> Unit = { viewModel?.cancelOperation() },
    onToggleLayoutView: (Int) -> Unit = { viewModel?.toggleLayoutView(it) },
    onToggleShowHidden: (Int) -> Unit = { viewModel?.toggleShowHidden(it) },
    onRefreshTab: (Int) -> Unit = { viewModel?.refreshTab(it) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Handle back button: navigate up directory tree, cancel selection, or close tab
    BackHandler(enabled = canNavigateUp) {
        onNavigateBack()
    }

    val tabs = uiState.tabs.ifEmpty {
        listOf(MultiPaneTab(currentPath = "/storage/emulated/0"))
    }

    val activeIndex = uiState.activeTabIndex.coerceIn(0, tabs.size - 1)
    val activeTab = tabs[activeIndex]

    val pagerState = rememberPagerState(
        initialPage = activeIndex,
        pageCount = { tabs.size }
    )

    // Sync pager with activeTabIndex
    LaunchedEffect(activeIndex) {
        if (pagerState.currentPage != activeIndex) {
            pagerState.animateScrollToPage(activeIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != uiState.activeTabIndex) {
                onSelectTab(page)
                onSetActivePane(if (page == 0) ActivePane.PANE_A else ActivePane.PANE_B)
            }
        }
    }

    // Local dialog / sheet states
    var contextMenuItem by remember { mutableStateOf<Pair<FileItem, Int>?>(null) }
    var itemForDetails by remember { mutableStateOf<FileItem?>(null) }
    var itemToRename by remember { mutableStateOf<Pair<FileItem, Int>?>(null) }
    var itemToDelete by remember { mutableStateOf<Pair<FileItem, Int>?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var filesToCompress by remember { mutableStateOf<List<FileItem>?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NovaTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar & Multi-Pane Tab Strip
            MultiPaneHeader(
                activeTab = activeTab,
                clipboard = uiState.clipboard,
                canNavigateUp = canNavigateUp,
                onNavigateBack = onNavigateBack,
                onSyncTabs = onSyncPanes,
                onSwapTabs = onSwapPanes
            )

            // Tab Strip (Scrollable row with tab badges, close button, and add button)
            MultiPaneTabStrip(
                tabs = tabs,
                activeIndex = activeIndex,
                onSelectTab = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                    onSelectTab(index)
                    onSetActivePane(if (index == 0) ActivePane.PANE_A else ActivePane.PANE_B)
                },
                onCloseTab = { index ->
                    onCloseTab(index)
                },
                onAddTab = {
                    onAddTab()
                }
            )

            // Path Breadcrumbs & Actions Strip
            MultiPaneSubHeader(
                activeTab = activeTab,
                onFolderClick = { path -> onFolderClick(activeIndex, path) },
                onToggleLayout = { onToggleLayoutView(activeIndex) },
                onOpenSettings = { showSettingsSheet = true },
                onCreateFolder = { showCreateFolderDialog = true },
                onRefresh = { onRefreshTab(activeIndex) }
            )

            // Progress banner if background operation is running
            AnimatedVisibility(visible = uiState.isOperationInProgress) {
                val progress = uiState.operationProgress?.bytesPercentage ?: 0f
                val fileName = uiState.operationProgress?.currentFileName ?: uiState.statusMessage ?: "Processing..."
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    NovaLinearProgress(progress = progress, title = fileName)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Cancel",
                            style = NovaTheme.typography.captionMedium,
                            color = NovaTheme.colors.error,
                            modifier = Modifier
                                .clickable { onCancelOperation() }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }

            // Main Tabbed Browsing Area (Full screen HorizontalPager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val tab = tabs.getOrNull(pageIndex) ?: return@HorizontalPager
                TabContent(
                    tab = tab,
                    onFolderClick = { path ->
                        if (pageIndex == 0) onFolderClickA(path)
                        if (pageIndex == 1) onFolderClickB(path)
                        onFolderClick(pageIndex, path)
                    },
                    onFileClick = onFileClick,
                    onItemLongClick = { item ->
                        onToggleSelect(pageIndex, item.id)
                    },
                    onMenuClick = { item ->
                        contextMenuItem = item to pageIndex
                    },
                    onToggleSelect = { id ->
                        if (pageIndex == 0) onToggleSelectA(id)
                        if (pageIndex == 1) onToggleSelectB(id)
                        onToggleSelect(pageIndex, id)
                    }
                )
            }
        }

        // Selection Bottom Dock (when active tab is in selection mode)
        AnimatedVisibility(
            visible = activeTab.isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        ) {
            MultiSelectionBottomDock(
                onCopy = { onCopySelected(activeIndex) },
                onMove = { onCutSelected(activeIndex) },
                onDelete = { showDeleteConfirmDialog = true },
                onShare = {
                    val selectedFiles = (activeTab.folders + activeTab.files).filter { activeTab.selectedIds.contains(it.id) }
                    FileActionHelper.shareFiles(context, selectedFiles)
                },
                onCompress = {
                    val selectedFiles = (activeTab.folders + activeTab.files).filter { activeTab.selectedIds.contains(it.id) }
                    filesToCompress = selectedFiles
                    showCompressDialog = true
                },
                onMore = {
                    val selectedFiles = (activeTab.folders + activeTab.files).filter { activeTab.selectedIds.contains(it.id) }
                    if (selectedFiles.size == 1) {
                        itemForDetails = selectedFiles.first()
                    }
                }
            )
        }

        // Floating Shared Clipboard Dock (for Cross-Tab Paste)
        AnimatedVisibility(
            visible = uiState.clipboard != null && uiState.clipboard.files.isNotEmpty() && !activeTab.isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        ) {
            uiState.clipboard?.let { clip ->
                SharedClipboardDock(
                    clipboard = clip,
                    onPaste = { onPasteClipboard(activeIndex) },
                    onClear = { onClearClipboard() }
                )
            }
        }

        // ==========================================
        // Bottom Sheets & Dialogs
        // ==========================================

        // Context Menu Sheet (BUG-004)
        contextMenuItem?.let { (item, tabIndex) ->
            FileItemContextMenuSheet(
                item = item,
                clipboard = uiState.clipboard,
                isBookmarked = false,
                onDismiss = { contextMenuItem = null },
                onOpen = {
                    contextMenuItem = null
                    if (item.isDirectory) onFolderClick(tabIndex, item.path) else onFileClick(item)
                },
                onOpenWith = {
                    contextMenuItem = null
                    FileActionHelper.openWith(context, item)
                },
                onCut = {
                    contextMenuItem = null
                    if (viewModel != null) viewModel.cutSingleFile(item) else onCutSelected(tabIndex)
                },
                onCopy = {
                    contextMenuItem = null
                    if (viewModel != null) viewModel.copySingleFile(item) else onCopySelected(tabIndex)
                },
                onPasteInto = {
                    contextMenuItem = null
                    onPasteIntoFolder(tabIndex, item.path)
                },
                onRename = {
                    contextMenuItem = null
                    itemToRename = item to tabIndex
                },
                onDelete = {
                    contextMenuItem = null
                    itemToDelete = item to tabIndex
                    showDeleteConfirmDialog = true
                },
                onShare = {
                    contextMenuItem = null
                    FileActionHelper.shareFiles(context, listOf(item))
                },
                onProperties = {
                    contextMenuItem = null
                    itemForDetails = item
                },
                onToggleBookmark = {
                    contextMenuItem = null
                    Toast.makeText(context, "Favorited ${item.name}", Toast.LENGTH_SHORT).show()
                },
                onCompress = {
                    contextMenuItem = null
                    filesToCompress = listOf(item)
                    showCompressDialog = true
                },
                onExtract = {
                    contextMenuItem = null
                    onExtractArchive(item, tabIndex)
                },
                onSelect = {
                    contextMenuItem = null
                    onToggleSelect(tabIndex, item.id)
                }
            )
        }

        // File Properties Sheet
        itemForDetails?.let { item ->
            FilePropertiesSheet(
                item = item,
                onComputeSha256 = { path ->
                    runCatching {
                        val file = File(path)
                        val digest = java.security.MessageDigest.getInstance("SHA-256")
                        file.inputStream().use { stream ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (stream.read(buffer).also { read = it } != -1) {
                                digest.update(buffer, 0, read)
                            }
                        }
                        digest.digest().joinToString("") { "%02x".format(it) }
                    }.getOrNull()
                },
                onDismiss = { itemForDetails = null }
            )
        }

        // Browsing Settings Sheet
        if (showSettingsSheet) {
            BrowsingSettingsSheet(
                isGridView = activeTab.isGridView,
                showHiddenFiles = activeTab.showHiddenFiles,
                sortConfig = activeTab.sortConfig,
                onToggleLayout = { onToggleLayoutView(activeIndex) },
                onToggleShowHidden = { onToggleShowHidden(activeIndex) },
                onSelectSortBy = { viewModel?.setSortBy(activeIndex, it) },
                onToggleSortDirection = { viewModel?.toggleSortDirection(activeIndex) },
                onDismiss = { showSettingsSheet = false }
            )
        }

        // Create Folder Dialog
        if (showCreateFolderDialog) {
            NovaInputDialog(
                title = "New Folder",
                placeholder = "Folder name",
                confirmText = "Create",
                cancelText = "Cancel",
                onConfirm = { name ->
                    showCreateFolderDialog = false
                    onCreateFolder(name, activeIndex)
                },
                onDismiss = { showCreateFolderDialog = false }
            )
        }

        // Rename Dialog
        itemToRename?.let { (item, tabIndex) ->
            NovaInputDialog(
                title = "Rename Item",
                initialValue = item.name,
                placeholder = "New name",
                confirmText = "Rename",
                cancelText = "Cancel",
                onConfirm = { newName ->
                    itemToRename = null
                    onRenameItem(item, newName, tabIndex)
                },
                onDismiss = { itemToRename = null }
            )
        }

        // Delete Confirm Dialog
        if (showDeleteConfirmDialog) {
            val count = if (itemToDelete != null) 1 else activeTab.selectedCount
            NovaConfirmDialog(
                title = "Delete $count items?",
                message = "Are you sure you want to permanently delete $count items? This action cannot be undone.",
                confirmText = "Delete",
                cancelText = "Cancel",
                onConfirm = {
                    showDeleteConfirmDialog = false
                    if (itemToDelete != null) {
                        onDeleteSingleFile(itemToDelete!!.first, itemToDelete!!.second)
                        itemToDelete = null
                    } else {
                        onDeleteSelected(activeIndex)
                    }
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                    itemToDelete = null
                }
            )
        }

        // Compress Dialog
        if (showCompressDialog) {
            val defaultName = filesToCompress?.singleOrNull()?.let {
                "${File(it.path).nameWithoutExtension}.zip"
            } ?: "Archive_${System.currentTimeMillis()}.zip"

            NovaInputDialog(
                title = "Compress to ZIP",
                initialValue = defaultName,
                placeholder = "Archive name",
                confirmText = "Compress",
                cancelText = "Cancel",
                onConfirm = { zipName ->
                    showCompressDialog = false
                    filesToCompress?.let { items ->
                        onCompressItems(items, zipName, activeIndex)
                    }
                    filesToCompress = null
                },
                onDismiss = {
                    showCompressDialog = false
                    filesToCompress = null
                }
            )
        }
    }
}

/**
 * Top bar of the Multi-Pane Manager.
 */
@Composable
private fun MultiPaneHeader(
    activeTab: MultiPaneTab,
    clipboard: com.novafiles.feature.files.ClipboardState?,
    canNavigateUp: Boolean,
    onNavigateBack: () -> Unit,
    onSyncTabs: () -> Unit,
    onSwapTabs: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canNavigateUp) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Up",
                        tint = NovaTheme.colors.textPrimary
                    )
                }
            }
            Column {
                Text(
                    text = "Multi-Pane Manager",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary
                )
                Text(
                    text = "${activeTab.title} • ${activeTab.totalItemCount} items",
                    style = NovaTheme.typography.captionMedium,
                    color = NovaTheme.colors.primary
                )
            }
        }

        // Utility actions (Sync directories, Swap tabs)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onSyncTabs,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NovaTheme.colors.surfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Synchronize Directory",
                    tint = NovaTheme.colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onSwapTabs,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NovaTheme.colors.surfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap Panes",
                    tint = NovaTheme.colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Compact horizontal tab strip showing active tab badge, close button, and add button.
 */
@Composable
private fun MultiPaneTabStrip(
    tabs: List<MultiPaneTab>,
    activeIndex: Int,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onAddTab: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val isActive = index == activeIndex
            val borderColor = if (isActive) NovaTheme.colors.primary else NovaTheme.colors.cardBorder
            val bgColor = if (isActive) NovaTheme.colors.primary.copy(alpha = 0.12f) else NovaTheme.colors.surfaceElevated

            Row(
                modifier = Modifier
                    .clip(NovaTheme.shapes.pill)
                    .background(bgColor)
                    .border(BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor), NovaTheme.shapes.pill)
                    .clickable { onSelectTab(index) }
                    .padding(start = 12.dp, end = if (tabs.size > 1) 6.dp else 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (isActive) NovaTheme.colors.primary else NovaTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tab.title,
                    style = NovaTheme.typography.captionMedium,
                    color = if (isActive) NovaTheme.colors.primary else NovaTheme.colors.textPrimary,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Close tab button (only if > 1 tab)
                if (tabs.size > 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onCloseTab(index) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close tab ${tab.title}",
                            tint = if (isActive) NovaTheme.colors.primary else NovaTheme.colors.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Add Tab Button
        IconButton(
            onClick = onAddTab,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(NovaTheme.colors.surfaceElevated)
                .border(1.dp, NovaTheme.colors.cardBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Tab",
                tint = NovaTheme.colors.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Subheader showing breadcrumbs and toolbar buttons.
 */
@Composable
private fun MultiPaneSubHeader(
    activeTab: MultiPaneTab,
    onFolderClick: (String) -> Unit,
    onToggleLayout: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateFolder: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Horizontally scrollable breadcrumb navigation
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            activeTab.breadcrumbs.forEachIndexed { index, crumb ->
                val isLast = index == activeTab.breadcrumbs.size - 1
                Text(
                    text = crumb.name,
                    style = NovaTheme.typography.captionMedium,
                    color = if (isLast) NovaTheme.colors.textPrimary else NovaTheme.colors.textSecondary,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { onFolderClick(crumb.path) }
                )
                if (!isLast) {
                    Text(
                        text = " / ",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textTertiary
                    )
                }
            }
        }

        // Quick action icons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleLayout,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (activeTab.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle View",
                    tint = NovaTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "View Options",
                    tint = NovaTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onCreateFolder,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = "New Folder",
                    tint = NovaTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = NovaTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * File listing content for an individual tab.
 */
@Composable
private fun TabContent(
    tab: MultiPaneTab,
    onFolderClick: (String) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onMenuClick: (FileItem) -> Unit,
    onToggleSelect: (String) -> Unit
) {
    if (tab.isLoading) {
        NovaLoadingState(modifier = Modifier.fillMaxSize())
        return
    }

    if (tab.folders.isEmpty() && tab.files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = NovaTheme.colors.textTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Directory is empty",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary
                )
            }
        }
        return
    }

    if (tab.isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (tab.folders.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Folders (${tab.folders.size})",
                        style = NovaTheme.typography.subtitle,
                        color = NovaTheme.colors.textSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(tab.folders, key = { it.id }) { item ->
                    val isSelected = tab.selectedIds.contains(item.id)
                    NovaFileGridItem(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = tab.isSelectionMode,
                        onClick = {
                            if (tab.isSelectionMode) onToggleSelect(item.id) else onFolderClick(item.path)
                        },
                        onLongClick = { onItemLongClick(item) },
                        onMenuClick = { onMenuClick(item) }
                    )
                }
            }

            if (tab.files.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Files (${tab.files.size})",
                        style = NovaTheme.typography.subtitle,
                        color = NovaTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(tab.files, key = { it.id }) { item ->
                    val isSelected = tab.selectedIds.contains(item.id)
                    NovaFileGridItem(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = tab.isSelectionMode,
                        onClick = {
                            if (tab.isSelectionMode) onToggleSelect(item.id) else onFileClick(item)
                        },
                        onLongClick = { onItemLongClick(item) },
                        onMenuClick = { onMenuClick(item) }
                    )
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (tab.folders.isNotEmpty()) {
                item {
                    Text(
                        text = "Folders (${tab.folders.size})",
                        style = NovaTheme.typography.subtitle,
                        color = NovaTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
                items(tab.folders, key = { it.id }) { item ->
                    val isSelected = tab.selectedIds.contains(item.id)
                    NovaFileRow(
                        item = item,
                        formattedSize = "${item.itemCount ?: 0} items",
                        formattedDate = FileFormatters.formatDate(item.lastModifiedMs),
                        isSelected = isSelected,
                        isSelectionMode = tab.isSelectionMode,
                        onClick = {
                            if (tab.isSelectionMode) onToggleSelect(item.id) else onFolderClick(item.path)
                        },
                        onLongClick = { onItemLongClick(item) },
                        onMenuClick = { onMenuClick(item) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (tab.files.isNotEmpty()) {
                item {
                    Text(
                        text = "Files (${tab.files.size})",
                        style = NovaTheme.typography.subtitle,
                        color = NovaTheme.colors.textSecondary,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 4.dp)
                    )
                }
                items(tab.files, key = { it.id }) { item ->
                    val isSelected = tab.selectedIds.contains(item.id)
                    NovaFileRow(
                        item = item,
                        formattedSize = FileFormatters.formatBytes(item.sizeBytes),
                        formattedDate = FileFormatters.formatDate(item.lastModifiedMs),
                        isSelected = isSelected,
                        isSelectionMode = tab.isSelectionMode,
                        onClick = {
                            if (tab.isSelectionMode) onToggleSelect(item.id) else onFileClick(item)
                        },
                        onLongClick = { onItemLongClick(item) },
                        onMenuClick = { onMenuClick(item) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
