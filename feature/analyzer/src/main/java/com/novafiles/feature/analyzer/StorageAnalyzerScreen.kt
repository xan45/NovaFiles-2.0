@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.analyzer

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.ColorCoralRed
import com.novafiles.core.designsystem.theme.ColorIndigoPrimary
import com.novafiles.core.designsystem.theme.ColorVioletSecondary
import com.novafiles.core.designsystem.theme.ColorWarmOrange
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import com.novafiles.core.model.SortDirection
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaCard
import com.novafiles.core.ui.components.NovaConfirmDialog
import com.novafiles.core.ui.components.NovaEmptyState
import com.novafiles.core.ui.components.NovaFileGridItem
import com.novafiles.core.ui.components.NovaFileRow
import com.novafiles.core.ui.components.NovaInputDialog
import com.novafiles.core.ui.components.NovaStorageSegmentBar
import com.novafiles.core.ui.components.StorageSegment
import com.novafiles.feature.files.ApkInspectorSheet
import com.novafiles.feature.files.ArchiveBrowserSheet
import com.novafiles.feature.files.BrowsingSettingsSheet
import com.novafiles.feature.files.FileActionHelper
import com.novafiles.feature.files.FileItemContextMenuSheet
import com.novafiles.feature.files.FilePropertiesSheet
import com.novafiles.feature.files.MultiSelectionBottomDock
import com.novafiles.feature.files.SharedClipboardDock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

@Composable
fun StorageAnalyzerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageAnalyzerViewModel = viewModel(),
    onOpenFile: (FileItem) -> Unit = {},
    onNavigateToCleaner: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var activeContextMenuItem by remember { mutableStateOf<FileItem?>(null) }
    var renameTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var singleDeleteTarget by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showCompressDialog by rememberSaveable { mutableStateOf(false) }
    var compressTargetItems by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var propertiesTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var showBrowsingSettings by rememberSaveable { mutableStateOf(false) }
    var archiveToBrowse by remember { mutableStateOf<FileItem?>(null) }
    var apkToInspect by remember { mutableStateOf<FileItem?>(null) }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        when {
            uiState.isSelectionMode -> viewModel.clearSelection()
            isSearchOpen -> {
                isSearchOpen = false
                viewModel.setSearchQuery("")
            }
            uiState.selectedCategory != null -> viewModel.clearCategorySelection()
            uiState.showDeletedSheet -> viewModel.toggleDeletedSheet(false)
            else -> onNavigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NovaTheme.colors.background)
    ) {
        if (uiState.selectedCategory != null) {
            // =================================================================
            // 1. Full-Screen Category Browser (FEATURE-005)
            // =================================================================
            val categoryType = uiState.selectedCategory!!
            val categoryName = uiState.categories.find { it.type == categoryType }?.name ?: when (categoryType) {
                FileType.IMAGE -> "Photos"
                FileType.VIDEO -> "Videos"
                FileType.DOCUMENT -> "Documents"
                FileType.AUDIO -> "Audio"
                FileType.APK -> "APKs"
                FileType.ARCHIVE -> "Archives"
                FileType.FOLDER -> "Folders"
                FileType.OTHER -> "Other"
            }
            val totalCategorySize = uiState.selectedCategoryFiles.sumOf { it.sizeBytes }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                CategoryBrowserTopBar(
                    categoryName = categoryName,
                    itemCount = uiState.displayedCategoryFiles.size,
                    totalSize = totalCategorySize,
                    isGridView = uiState.isGridView,
                    isSearchActive = isSearchOpen,
                    searchQuery = uiState.searchQuery,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedCount = uiState.selectedIds.size,
                    onNavigateBack = {
                        if (uiState.isSelectionMode) {
                            viewModel.clearSelection()
                        } else if (isSearchOpen) {
                            isSearchOpen = false
                            viewModel.setSearchQuery("")
                        } else {
                            viewModel.clearCategorySelection()
                        }
                    },
                    onToggleSearch = {
                        isSearchOpen = !isSearchOpen
                        if (!isSearchOpen) viewModel.setSearchQuery("")
                    },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onToggleLayout = { viewModel.toggleLayoutMode() },
                    onOpenSort = { showBrowsingSettings = true },
                    onSelectAll = { viewModel.selectAll() },
                    onClearSelection = { viewModel.clearSelection() }
                )

                if (uiState.isCategoryLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NovaTheme.colors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else if (uiState.displayedCategoryFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NovaEmptyState(
                            title = if (uiState.searchQuery.isNotEmpty()) "No matching files" else "No $categoryName files",
                            subtitle = if (uiState.searchQuery.isNotEmpty()) "No files found for \"${uiState.searchQuery}\"" else "No files found in this category on device"
                        )
                    }
                } else {
                    if (uiState.isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = if (uiState.isSelectionMode || uiState.clipboard != null) 120.dp else 24.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.displayedCategoryFiles, key = { it.id }) { item ->
                                val isSelected = uiState.selectedIds.contains(item.id)
                                NovaFileGridItem(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    formattedSize = FileFormatters.formatBytes(item.sizeBytes),
                                    formattedDate = FileFormatters.formatDate(item.lastModifiedMs),
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelect(item.id)
                                        } else if (item.fileType == FileType.ARCHIVE || item.name.endsWith(".zip", true) || item.name.endsWith(".tar", true) || item.name.endsWith(".tar.gz", true) || item.name.endsWith(".tgz", true)) {
                                            archiveToBrowse = item
                                        } else if (item.fileType == FileType.APK || item.name.endsWith(".apk", true)) {
                                            apkToInspect = item
                                        } else {
                                            onOpenFile(item)
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelect(item.id) },
                                    onMenuClick = { activeContextMenuItem = item }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = if (uiState.isSelectionMode || uiState.clipboard != null) 120.dp else 24.dp
                            )
                        ) {
                            items(uiState.displayedCategoryFiles, key = { it.id }) { file ->
                                val isSelected = uiState.selectedIds.contains(file.id)
                                NovaFileRow(
                                    item = file,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    formattedSize = FileFormatters.formatBytes(file.sizeBytes),
                                    formattedDate = FileFormatters.formatDate(file.lastModifiedMs),
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelect(file.id)
                                        } else if (file.fileType == FileType.ARCHIVE || file.name.endsWith(".zip", true) || file.name.endsWith(".tar", true) || file.name.endsWith(".tar.gz", true) || file.name.endsWith(".tgz", true)) {
                                            archiveToBrowse = file
                                        } else if (file.fileType == FileType.APK || file.name.endsWith(".apk", true)) {
                                            apkToInspect = file
                                        } else {
                                            onOpenFile(file)
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelect(file.id) },
                                    onMenuClick = { activeContextMenuItem = file }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // Floating Multi-Selection Dock
            AnimatedVisibility(
                visible = uiState.isSelectionMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                MultiSelectionBottomDock(
                    onCopy = { viewModel.copySelected() },
                    onMove = { viewModel.cutSelected() },
                    onDelete = { showDeleteConfirm = true },
                    onShare = {
                        val selected = uiState.displayedCategoryFiles.filter { uiState.selectedIds.contains(it.id) }
                        FileActionHelper.shareFiles(context, selected)
                    },
                    onCompress = {
                        compressTargetItems = uiState.displayedCategoryFiles.filter { uiState.selectedIds.contains(it.id) }
                        showCompressDialog = true
                    },
                    onMore = { viewModel.selectAll() }
                )
            }

            // Floating Clipboard Bar
            AnimatedVisibility(
                visible = !uiState.isSelectionMode && uiState.clipboard != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                SharedClipboardDock(
                    clipboard = uiState.clipboard!!,
                    onPaste = { viewModel.pasteClipboard() },
                    onClear = { viewModel.clearClipboard() }
                )
            }
        } else {
            // =================================================================
            // 2. Storage Analyzer Dashboard Overview
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                StorageAnalyzerTopBar(
                    onNavigateBack = onNavigateBack,
                    onSearchClick = { viewModel.scanStorage() },
                    onMenuClick = onNavigateToCleaner
                )

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NovaTheme.colors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Deep Storage Cleaner Banner
                        item {
                            UpgradePlanCard(onUpgradeClick = onNavigateToCleaner)
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Interactive Storage Treemap
                        if (uiState.categories.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Storage Treemap",
                                    style = NovaTheme.typography.title,
                                    color = NovaTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                StorageTreemapCard(
                                    categories = uiState.categories,
                                    onCategoryClick = { viewModel.selectCategory(it) }
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }

                        // Storage Overview Section
                        item {
                            Text(
                                text = "Storage",
                                style = NovaTheme.typography.title,
                                color = NovaTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            StorageUsageCard(
                                usedBytes = uiState.usedBytes,
                                totalBytes = uiState.totalBytes,
                                categories = uiState.categories
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Category Breakdown Rows
                        items(uiState.categories) { category ->
                            CategoryBreakdownRow(
                                category = category,
                                onClick = { viewModel.selectCategory(category.type) }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Recently Deleted Section
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recently Deleted",
                                    style = NovaTheme.typography.title,
                                    color = NovaTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "See All",
                                    style = NovaTheme.typography.captionMedium,
                                    color = NovaTheme.colors.primary,
                                    modifier = Modifier
                                        .clickable { viewModel.toggleDeletedSheet(true) }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(uiState.recentlyDeleted.take(3)) { deletedItem ->
                            RecentlyDeletedRow(
                                item = deletedItem,
                                onRestore = { viewModel.restoreDeletedItem(deletedItem) },
                                onDelete = { viewModel.permanentlyDeleteTrashItem(deletedItem) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Large Files Section (>50 MB)
                        if (uiState.largeFiles.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Large Files (>50 MB)",
                                    style = NovaTheme.typography.title,
                                    color = NovaTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            items(uiState.largeFiles.take(5)) { largeFile ->
                                LargeFileRow(largeFile = largeFile)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Recycle Bin Full Sheet
            if (uiState.showDeletedSheet) {
                RecentlyDeletedFullSheet(
                    deletedItems = uiState.recentlyDeleted,
                    onRestore = { viewModel.restoreDeletedItem(it) },
                    onDelete = { viewModel.permanentlyDeleteTrashItem(it) },
                    onEmptyTrash = { viewModel.emptyTrash() },
                    onDismiss = { viewModel.toggleDeletedSheet(false) }
                )
            }
        }

        // =================================================================
        // Context Menu, Dialogs & Sheets
        // =================================================================

        if (activeContextMenuItem != null) {
            val item = activeContextMenuItem!!
            FileItemContextMenuSheet(
                item = item,
                clipboard = uiState.clipboard,
                isBookmarked = false,
                onDismiss = { activeContextMenuItem = null },
                onOpen = {
                    activeContextMenuItem = null
                    if (item.fileType == FileType.ARCHIVE || item.name.endsWith(".zip", true) || item.name.endsWith(".tar", true) || item.name.endsWith(".tar.gz", true) || item.name.endsWith(".tgz", true)) {
                        archiveToBrowse = item
                    } else if (item.fileType == FileType.APK || item.name.endsWith(".apk", true)) {
                        apkToInspect = item
                    } else {
                        onOpenFile(item)
                    }
                },
                onOpenWith = {
                    activeContextMenuItem = null
                    FileActionHelper.openWith(context, item)
                },
                onCut = {
                    activeContextMenuItem = null
                    viewModel.cutSingleFile(item)
                },
                onCopy = {
                    activeContextMenuItem = null
                    viewModel.copySingleFile(item)
                },
                onPasteInto = {
                    activeContextMenuItem = null
                    viewModel.pasteClipboard(File(item.path))
                },
                onRename = {
                    activeContextMenuItem = null
                    renameTargetItem = item
                },
                onDelete = {
                    activeContextMenuItem = null
                    singleDeleteTarget = item
                },
                onShare = {
                    activeContextMenuItem = null
                    FileActionHelper.shareFile(context, item)
                },
                onProperties = {
                    activeContextMenuItem = null
                    propertiesTargetItem = item
                },
                onToggleBookmark = {
                    activeContextMenuItem = null
                },
                onCompress = {
                    activeContextMenuItem = null
                    compressTargetItems = listOf(item)
                    showCompressDialog = true
                },
                onExtract = {
                    activeContextMenuItem = null
                },
                onSelect = {
                    activeContextMenuItem = null
                    viewModel.toggleSelect(item.id)
                }
            )
        }

        // Rename Dialog
        if (renameTargetItem != null) {
            val target = renameTargetItem!!
            NovaInputDialog(
                title = "Rename",
                initialValue = target.name,
                placeholder = "New name",
                confirmText = "Rename",
                cancelText = "Cancel",
                onConfirm = { newName ->
                    renameTargetItem = null
                    viewModel.renameFile(target, newName)
                },
                onDismiss = { renameTargetItem = null }
            )
        }

        // Compress Dialog
        if (showCompressDialog) {
            val itemsToZip = if (compressTargetItems.isNotEmpty()) {
                compressTargetItems
            } else {
                uiState.displayedCategoryFiles.filter { uiState.selectedIds.contains(it.id) }
            }
            val defaultName = if (itemsToZip.size == 1) itemsToZip.first().name.substringBeforeLast(".") else "Archive"
            NovaInputDialog(
                title = "Compress to ZIP",
                initialValue = defaultName,
                placeholder = "Archive name",
                confirmText = "Compress",
                cancelText = "Cancel",
                onConfirm = { zipName ->
                    showCompressDialog = false
                    viewModel.compressItems(itemsToZip, zipName)
                    compressTargetItems = emptyList()
                },
                onDismiss = {
                    showCompressDialog = false
                    compressTargetItems = emptyList()
                }
            )
        }

        // Properties Sheet
        if (propertiesTargetItem != null) {
            FilePropertiesSheet(
                item = propertiesTargetItem!!,
                onComputeSha256 = { path ->
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val digest = MessageDigest.getInstance("SHA-256")
                            val bytes = File(path).readBytes()
                            digest.digest(bytes).joinToString("") { "%02x".format(it) }
                        }.getOrNull()
                    }
                },
                onDismiss = { propertiesTargetItem = null }
            )
        }

        // Browsing Settings Sheet (Sort & Layout)
        if (showBrowsingSettings) {
            BrowsingSettingsSheet(
                isGridView = uiState.isGridView,
                showHiddenFiles = false,
                sortConfig = uiState.sortConfig,
                onToggleLayout = { viewModel.toggleLayoutMode() },
                onToggleShowHidden = { },
                onSelectSortBy = { sortBy ->
                    viewModel.setSortConfig(uiState.sortConfig.copy(sortBy = sortBy))
                },
                onToggleSortDirection = {
                    val nextDir = if (uiState.sortConfig.direction == SortDirection.ASCENDING) {
                        SortDirection.DESCENDING
                    } else {
                        SortDirection.ASCENDING
                    }
                    viewModel.setSortConfig(uiState.sortConfig.copy(direction = nextDir))
                },
                onDismiss = { showBrowsingSettings = false }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirm || singleDeleteTarget != null) {
            val isSingle = singleDeleteTarget != null
            val count = if (isSingle) 1 else uiState.selectedIds.size
            val title = if (isSingle) "Delete File" else "Delete $count Files"
            val message = if (isSingle) {
                "Are you sure you want to permanently delete \"${singleDeleteTarget?.name}\"?"
            } else {
                "Are you sure you want to permanently delete $count selected files?"
            }

            NovaConfirmDialog(
                title = title,
                message = message,
                confirmText = "Delete",
                cancelText = "Cancel",
                onConfirm = {
                    if (isSingle) {
                        val target = singleDeleteTarget!!
                        singleDeleteTarget = null
                        viewModel.deleteSingleFile(target)
                    } else {
                        showDeleteConfirm = false
                        viewModel.deleteSelected()
                    }
                },
                onDismiss = {
                    showDeleteConfirm = false
                    singleDeleteTarget = null
                }
            )
        }

        // Archive Browser Sheet
        if (archiveToBrowse != null) {
            ArchiveBrowserSheet(
                archiveItem = archiveToBrowse!!,
                onExtractAll = { archiveToBrowse = null },
                onExtractEntry = { _, _ -> },
                onDismiss = { archiveToBrowse = null }
            )
        }

        // APK Inspector Sheet
        if (apkToInspect != null) {
            ApkInspectorSheet(
                apkItem = apkToInspect!!,
                onDismiss = { apkToInspect = null }
            )
        }
    }
}

@Composable
private fun CategoryBrowserTopBar(
    categoryName: String,
    itemCount: Int,
    totalSize: Long,
    isGridView: Boolean,
    isSearchActive: Boolean,
    searchQuery: String,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onNavigateBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleLayout: () -> Unit,
    onOpenSort: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = if (isSelectionMode) "Close selection" else "Back to Overview",
                tint = NovaTheme.colors.textPrimary
            )
        }

        if (isSearchActive && !isSelectionMode) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search in $categoryName...",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaTheme.colors.primary,
                    unfocusedBorderColor = NovaTheme.colors.cardBorder,
                    focusedTextColor = NovaTheme.colors.textPrimary,
                    unfocusedTextColor = NovaTheme.colors.textPrimary
                ),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = NovaTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )

            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit search",
                    tint = NovaTheme.colors.textPrimary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = if (isSelectionMode) "$selectedCount Selected" else categoryName,
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isSelectionMode) "Tap items to select/deselect" else "$itemCount items • ${FileFormatters.formatBytes(totalSize)}",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary
                )
            }

            if (isSelectionMode) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Select All",
                        tint = NovaTheme.colors.primary
                    )
                }
            } else {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = NovaTheme.colors.textPrimary
                    )
                }

                IconButton(onClick = onToggleLayout) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = if (isGridView) "List View" else "Grid View",
                        tint = NovaTheme.colors.textPrimary
                    )
                }

                IconButton(onClick = onOpenSort) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort and View Options",
                        tint = NovaTheme.colors.textPrimary
                    )
                }

                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select All") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = null,
                                    tint = NovaTheme.colors.textPrimary
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onSelectAll()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageAnalyzerTopBar(
    onNavigateBack: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = NovaTheme.colors.textPrimary
            )
        }

        Text(
            text = "Overview",
            style = NovaTheme.typography.title,
            color = NovaTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Row {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = NovaTheme.colors.textPrimary
                )
            }
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = NovaTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun UpgradePlanCard(
    onUpgradeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NovaTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF386BF6),
                        Color(0xFF2850C8)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Deep Storage Cleaner",
                    color = Color.White,
                    style = NovaTheme.typography.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scan & remove junk, cache,\nduplicates and obsolete APKs.",
                    color = Color.White.copy(alpha = 0.85f),
                    style = NovaTheme.typography.captionMedium,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .clip(NovaTheme.shapes.pill)
                        .background(Color.White)
                        .clickable(onClick = onUpgradeClick)
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Clean Now",
                        color = Color(0xFF2850C8),
                        fontWeight = FontWeight.Bold,
                        style = NovaTheme.typography.captionMedium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFD166).copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = Color(0xFF996B00),
                    modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}

@Composable
private fun StorageUsageCard(
    usedBytes: Long,
    totalBytes: Long,
    categories: List<CategoryStorageItem>
) {
    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        shape = NovaTheme.shapes.large,
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Used Storage",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary
                )

                val usedDisplay = if (usedBytes > 0) FileFormatters.formatBytes(usedBytes) else "69.8 GB"
                val totalDisplay = if (totalBytes > 0) FileFormatters.formatBytes(totalBytes) else "512 GB"

                Text(
                    text = "$usedDisplay / $totalDisplay",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val segments = if (categories.any { it.sizeBytes > 0 }) {
                categories.map {
                    StorageSegment(
                        color = it.color,
                        weight = it.sizeBytes.toFloat().coerceAtLeast(1f)
                    )
                }
            } else {
                listOf(
                    StorageSegment(ColorVioletSecondary, 20.8f),
                    StorageSegment(ColorCoralRed, 29.0f),
                    StorageSegment(ColorIndigoPrimary, 8.0f),
                    StorageSegment(ColorWarmOrange, 12.0f)
                )
            }

            NovaStorageSegmentBar(
                segments = segments,
                height = 10.dp
            )
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    category: CategoryStorageItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(category.color)
            )

            Text(
                text = category.name,
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val sizeDisplay = if (category.sizeBytes > 0) {
                FileFormatters.formatBytes(category.sizeBytes)
            } else {
                "${category.fileCount} items"
            }

            Text(
                text = sizeDisplay,
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textSecondary,
                fontWeight = FontWeight.Medium
            )

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = NovaTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RecentlyDeletedRow(
    item: DeletedFileItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        shape = NovaTheme.shapes.large,
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (item.isDirectory) Color(0xFFFFD166).copy(alpha = 0.2f)
                        else ColorVioletSecondary.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Image,
                    contentDescription = null,
                    tint = if (item.isDirectory) Color(0xFFFFD166) else ColorVioletSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val countOrSize = if (item.isDirectory) "${item.itemCount} items" else FileFormatters.formatBytes(item.sizeBytes)
                Text(
                    text = "$countOrSize • ${item.daysRemaining} Days Remaining",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Rounded.Restore,
                    contentDescription = "Restore",
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete Permanently",
                    tint = NovaTheme.colors.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LargeFileRow(
    largeFile: LargeFileItem
) {
    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = largeFile.name,
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${largeFile.fileType.name} • ${largeFile.path}",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(NovaTheme.shapes.pill)
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = FileFormatters.formatBytes(largeFile.sizeBytes),
                    color = NovaTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    style = NovaTheme.typography.caption
                )
            }
        }
    }
}

@Composable
private fun RecentlyDeletedFullSheet(
    deletedItems: List<DeletedFileItem>,
    onRestore: (DeletedFileItem) -> Unit,
    onDelete: (DeletedFileItem) -> Unit,
    onEmptyTrash: () -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recycle Bin",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (deletedItems.isNotEmpty()) {
                    NovaButton(
                        text = "Empty Trash",
                        onClick = onEmptyTrash,
                        variant = NovaButtonVariant.SECONDARY
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (deletedItems.isEmpty()) {
                Text(
                    text = "Trash is empty.",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(deletedItems) { item ->
                        RecentlyDeletedRow(
                            item = item,
                            onRestore = { onRestore(item) },
                            onDelete = { onDelete(item) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageTreemapCard(
    categories: List<CategoryStorageItem>,
    onCategoryClick: (FileType) -> Unit
) {
    val nonEmpty = categories.filter { it.sizeBytes > 0L }.sortedByDescending { it.sizeBytes }.take(5)
    if (nonEmpty.isEmpty()) return
    val total = nonEmpty.sumOf { it.sizeBytes }.coerceAtLeast(1L)

    NovaCard(
        shape = NovaTheme.shapes.large,
        backgroundColor = NovaTheme.colors.surfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp)),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                nonEmpty.forEach { cat ->
                    val weight = (cat.sizeBytes.toFloat() / total.toFloat()).coerceAtLeast(0.08f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxSize()
                            .background(cat.color.copy(alpha = 0.85f))
                            .clickable { onCategoryClick(cat.type) }
                            .padding(6.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = cat.name,
                            style = NovaTheme.typography.caption,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            Text(
                text = "Tap any block to inspect category files",
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary
            )
        }
    }
}
