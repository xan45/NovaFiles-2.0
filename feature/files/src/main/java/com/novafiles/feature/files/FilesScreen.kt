package com.novafiles.feature.files

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaConfirmDialog
import com.novafiles.core.ui.components.NovaEmptyState
import com.novafiles.core.ui.components.NovaErrorState
import com.novafiles.core.ui.components.NovaFileGridItem
import com.novafiles.core.ui.components.NovaFileRow
import com.novafiles.core.ui.components.NovaInputDialog
import com.novafiles.core.ui.components.NovaLoadingState
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    uiState: FilesUiState,
    onFolderClick: (String) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onNavigateBack: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSortChange: (SortBy) -> Unit,
    onToggleLayout: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit = {},
    onToggleShowHidden: () -> Unit = {},
    onCopy: () -> Unit = {},
    onMove: () -> Unit = {},
    onPaste: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    onRenameFile: (FileItem, String) -> Unit = { _, _ -> },
    onSearchQueryChange: (String) -> Unit = {},
    onClearClipboard: () -> Unit = {},
    onFilterChange: (FileTypeFilter) -> Unit = {},
    onToggleSortDirection: () -> Unit = {},
    onCompressSelected: (String) -> Unit = {},
    onExtractArchive: (FileItem) -> Unit = {},
    onExtractArchiveEntry: (FileItem, String) -> Unit = { _, _ -> },
    onBatchRename: (prefix: String, suffix: String, find: String, replace: String, startCounter: Int?) -> Unit = { _, _, _, _, _ -> },
    onMoveToTrash: (File) -> Unit = {},
    onToggleBookmark: (String, String) -> Unit = { _, _ -> },
    onComputeSha256: suspend (String) -> String = { "" },
    onCopySingleFile: (FileItem) -> Unit = {},
    onCutSingleFile: (FileItem) -> Unit = {},
    onDeleteSingleFile: (FileItem) -> Unit = {},
    onPasteIntoFolder: (FileItem) -> Unit = {},
    onCompressSingleFile: (FileItem, String) -> Unit = { _, _ -> },
    onCancelOperation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeContextMenuItem by rememberSaveable { mutableStateOf<FileItem?>(null) }
    var fileForDetails by rememberSaveable { mutableStateOf<FileItem?>(null) }
    var archiveToBrowse by rememberSaveable { mutableStateOf<FileItem?>(null) }
    var apkToInspect by rememberSaveable { mutableStateOf<FileItem?>(null) }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showSingleDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var fileToDelete by rememberSaveable { mutableStateOf<FileItem?>(null) }

    var showCreateFolderDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var fileToRename by rememberSaveable { mutableStateOf<FileItem?>(null) }

    var showCreateZipDialog by rememberSaveable { mutableStateOf(false) }
    var fileToCompress by rememberSaveable { mutableStateOf<FileItem?>(null) }

    var showBatchRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showMoreActionsSheet by rememberSaveable { mutableStateOf(false) }
    var showBrowsingSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showBookmarksSheet by rememberSaveable { mutableStateOf(false) }

    var showSearchField by rememberSaveable { mutableStateOf(uiState.searchQuery.isNotBlank()) }
    var overflowMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiScope = rememberCoroutineScope()

    val hasModalOpen = activeContextMenuItem != null || fileForDetails != null || archiveToBrowse != null ||
            apkToInspect != null || showDeleteConfirm || showSingleDeleteConfirm || showCreateFolderDialog ||
            showRenameDialog || showCreateZipDialog || showBatchRenameDialog || showMoreActionsSheet ||
            showBrowsingSettingsSheet || showBookmarksSheet || overflowMenuExpanded

    val canGoBackInFiles = hasModalOpen || uiState.isSelectionMode || showSearchField || uiState.breadcrumbs.size > 1

    BackHandler(enabled = canGoBackInFiles) {
        when {
            activeContextMenuItem != null -> activeContextMenuItem = null
            fileForDetails != null -> fileForDetails = null
            archiveToBrowse != null -> archiveToBrowse = null
            apkToInspect != null -> apkToInspect = null
            showDeleteConfirm -> showDeleteConfirm = false
            showSingleDeleteConfirm -> {
                showSingleDeleteConfirm = false
                fileToDelete = null
            }
            showCreateFolderDialog -> showCreateFolderDialog = false
            showRenameDialog -> {
                showRenameDialog = false
                fileToRename = null
            }
            showCreateZipDialog -> {
                showCreateZipDialog = false
                fileToCompress = null
            }
            showBatchRenameDialog -> showBatchRenameDialog = false
            showMoreActionsSheet -> showMoreActionsSheet = false
            showBrowsingSettingsSheet -> showBrowsingSettingsSheet = false
            showBookmarksSheet -> showBookmarksSheet = false
            overflowMenuExpanded -> overflowMenuExpanded = false
            uiState.isSelectionMode -> onClearSelection()
            showSearchField -> {
                showSearchField = false
                onSearchQueryChange("")
            }
            uiState.breadcrumbs.size > 1 -> onNavigateBack()
        }
    }

    val currentFolder = uiState.breadcrumbs.lastOrNull()
    val isCurrentFolderBookmarked = uiState.bookmarks.any { it.path == uiState.currentPath }

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
            // Header: Selection Mode vs Normal Mode
            if (uiState.isSelectionMode) {
                SelectionHeader(
                    selectedCount = uiState.selectedCount,
                    isAllSelected = uiState.isAllSelected,
                    onClearSelection = onClearSelection,
                    onSelectAll = onSelectAll
                )
            } else {
                StandardHeader(
                    folderTitle = currentFolder?.name ?: "Files",
                    folderPath = uiState.currentPath,
                    isGridView = uiState.isGridView,
                    searchQuery = uiState.searchQuery,
                    isSearching = showSearchField,
                    canNavigateUp = uiState.breadcrumbs.size > 1,
                    isBookmarked = isCurrentFolderBookmarked,
                    onNavigateBack = onNavigateBack,
                    onToggleSearch = {
                        showSearchField = !showSearchField
                        if (!showSearchField) onSearchQueryChange("")
                    },
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenSettings = { showBrowsingSettingsSheet = true },
                    onCreateFolder = { showCreateFolderDialog = true },
                    onToggleBookmark = {
                        val name = currentFolder?.name ?: "Folder"
                        onToggleBookmark(uiState.currentPath, name)
                    },
                    overflowMenuExpanded = overflowMenuExpanded,
                    onDismissOverflow = { overflowMenuExpanded = false },
                    onOpenOverflow = { overflowMenuExpanded = true },
                    showHiddenFiles = uiState.showHiddenFiles,
                    onToggleShowHidden = onToggleShowHidden,
                    onOpenBookmarks = { showBookmarksSheet = true },
                    onRefresh = onRefresh
                )
            }

            // Breadcrumb Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.breadcrumbs.forEachIndexed { index, crumb ->
                    val isLast = index == uiState.breadcrumbs.size - 1
                    Text(
                        text = crumb.name,
                        style = NovaTheme.typography.captionMedium,
                        color = if (isLast) NovaTheme.colors.primary else NovaTheme.colors.textSecondary,
                        modifier = Modifier.clickable { onFolderClick(crumb.path) }
                    )
                    if (!isLast) {
                        Text(
                            text = " > ",
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textTertiary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileTypeFilter.entries.forEach { filter ->
                    val selected = uiState.activeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated)
                            .border(
                                width = 1.dp,
                                color = if (selected) NovaTheme.colors.primary else NovaTheme.colors.cardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter.label,
                            style = NovaTheme.typography.captionMedium,
                            color = if (selected) Color.White else NovaTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Main Content: Unified Folder and File Listing
            when {
                uiState.isLoading -> {
                    NovaLoadingState(modifier = Modifier.weight(1f))
                }
                uiState.errorMessage != null -> {
                    NovaErrorState(
                        message = uiState.errorMessage,
                        onRetry = onRetry,
                        modifier = Modifier.weight(1f)
                    )
                }
                uiState.folders.isEmpty() && uiState.files.isEmpty() -> {
                    NovaEmptyState(
                        title = "Folder is empty",
                        subtitle = if (uiState.searchQuery.isNotBlank()) "No files match '${uiState.searchQuery}'" else "No items in this directory",
                        modifier = Modifier.weight(1f)
                    )
                }
                uiState.isGridView -> {
                    // Unified Grid View
                    val allItems = uiState.folders + uiState.files
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allItems, key = { it.id }) { item ->
                            val isSelected = uiState.selectedIds.contains(item.id)
                            NovaFileGridItem(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                formattedSize = FileFormatters.formatBytes(item.sizeBytes),
                                formattedDate = FileFormatters.formatDate(item.lastModifiedMs),
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        onToggleSelect(item.id)
                                    } else if (item.isDirectory) {
                                        onFolderClick(item.path)
                                    } else if (item.fileType == FileType.ARCHIVE || item.name.endsWith(".zip", true) || item.name.endsWith(".tar", true) || item.name.endsWith(".tar.gz", true) || item.name.endsWith(".tgz", true)) {
                                        archiveToBrowse = item
                                    } else if (item.fileType == FileType.APK || item.name.endsWith(".apk", true)) {
                                        apkToInspect = item
                                    } else {
                                        onFileClick(item)
                                    }
                                },
                                onLongClick = { onToggleSelect(item.id) },
                                onMenuClick = { activeContextMenuItem = item }
                            )
                        }
                    }
                }
                else -> {
                    // Unified List View (Default)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
                    ) {
                        // Folders section
                        if (uiState.folders.isNotEmpty()) {
                            item(key = "header_folders") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = NovaTheme.colors.categoryFolders,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Folders (${uiState.folders.size})",
                                        style = NovaTheme.typography.captionMedium,
                                        color = NovaTheme.colors.textSecondary
                                    )
                                }
                            }

                            items(uiState.folders, key = { it.id }) { folder ->
                                val isSelected = uiState.selectedIds.contains(folder.id)
                                NovaFileRow(
                                    item = folder,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    formattedSize = "",
                                    formattedDate = FileFormatters.formatDate(folder.lastModifiedMs),
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            onToggleSelect(folder.id)
                                        } else {
                                            onFolderClick(folder.path)
                                        }
                                    },
                                    onLongClick = { onToggleSelect(folder.id) },
                                    onMenuClick = { activeContextMenuItem = folder }
                                )
                            }
                        }

                        // Files section immediately below folders
                        if (uiState.files.isNotEmpty()) {
                            item(key = "header_files") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = null,
                                        tint = NovaTheme.colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Files (${uiState.files.size})",
                                        style = NovaTheme.typography.captionMedium,
                                        color = NovaTheme.colors.textSecondary
                                    )
                                }
                            }

                            items(uiState.files, key = { it.id }) { file ->
                                val isSelected = uiState.selectedIds.contains(file.id)
                                NovaFileRow(
                                    item = file,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    formattedSize = FileFormatters.formatBytes(file.sizeBytes),
                                    formattedDate = FileFormatters.formatDate(file.lastModifiedMs),
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            onToggleSelect(file.id)
                                        } else if (file.fileType == FileType.ARCHIVE || file.name.endsWith(".zip", true) || file.name.endsWith(".tar", true) || file.name.endsWith(".tar.gz", true) || file.name.endsWith(".tgz", true)) {
                                            archiveToBrowse = file
                                        } else if (file.fileType == FileType.APK || file.name.endsWith(".apk", true)) {
                                            apkToInspect = file
                                        } else {
                                            onFileClick(file)
                                        }
                                    },
                                    onLongClick = { onToggleSelect(file.id) },
                                    onMenuClick = { activeContextMenuItem = file }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Clipboard Dock (Paste & Clear)
        AnimatedVisibility(
            visible = uiState.clipboard != null && !uiState.isSelectionMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        ) {
            val clip = uiState.clipboard ?: return@AnimatedVisibility
            Row(
                modifier = Modifier
                    .shadow(NovaTheme.elevation.level4, NovaTheme.shapes.pill)
                    .clip(NovaTheme.shapes.pill)
                    .background(NovaTheme.colors.surfaceElevated)
                    .border(1.dp, NovaTheme.colors.primary.copy(alpha = 0.5f), NovaTheme.shapes.pill)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (clip.op == ClipboardOp.COPY) Icons.Default.ContentCopy else Icons.AutoMirrored.Filled.DriveFileMove,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${clip.files.size} item(s) (${if (clip.op == ClipboardOp.COPY) "Copy" else "Cut"})",
                    style = NovaTheme.typography.captionMedium,
                    color = NovaTheme.colors.textPrimary
                )
                NovaButton(
                    text = "Paste",
                    onClick = onPaste,
                    variant = NovaButtonVariant.PRIMARY,
                    modifier = Modifier.height(34.dp)
                )
                IconButton(
                    onClick = onClearClipboard,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear clipboard",
                        tint = NovaTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Floating Multi-Selection Action Dock
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        ) {
            SelectionBottomDock(
                onCopy = onCopy,
                onMove = onMove,
                onDelete = { showDeleteConfirm = true },
                onShare = {
                    val selectedItems = (uiState.folders + uiState.files).filter { uiState.selectedIds.contains(it.id) }
                    FileActionHelper.shareFiles(context, selectedItems)
                },
                onCompress = {
                    fileToCompress = null
                    showCreateZipDialog = true
                },
                onMore = { showMoreActionsSheet = true }
            )
        }

        // BUG-004: Context Menu Bottom Sheet
        if (activeContextMenuItem != null) {
            val item = activeContextMenuItem!!
            FileItemContextMenuSheet(
                item = item,
                clipboard = uiState.clipboard,
                isBookmarked = uiState.bookmarks.any { it.path == item.path },
                onDismiss = { activeContextMenuItem = null },
                onOpen = {
                    activeContextMenuItem = null
                    if (item.isDirectory) {
                        onFolderClick(item.path)
                    } else if (item.fileType == FileType.ARCHIVE || item.name.endsWith(".zip", true) || item.name.endsWith(".tar", true) || item.name.endsWith(".tar.gz", true) || item.name.endsWith(".tgz", true)) {
                        archiveToBrowse = item
                    } else if (item.fileType == FileType.APK || item.name.endsWith(".apk", true)) {
                        apkToInspect = item
                    } else {
                        onFileClick(item)
                    }
                },
                onOpenWith = {
                    activeContextMenuItem = null
                    FileActionHelper.openWith(context, item)
                },
                onCut = {
                    activeContextMenuItem = null
                    onCutSingleFile(item)
                },
                onCopy = {
                    activeContextMenuItem = null
                    onCopySingleFile(item)
                },
                onPasteInto = {
                    activeContextMenuItem = null
                    onPasteIntoFolder(item)
                },
                onRename = {
                    activeContextMenuItem = null
                    fileToRename = item
                    showRenameDialog = true
                },
                onDelete = {
                    activeContextMenuItem = null
                    fileToDelete = item
                    showSingleDeleteConfirm = true
                },
                onShare = {
                    activeContextMenuItem = null
                    FileActionHelper.shareFile(context, item)
                },
                onProperties = {
                    activeContextMenuItem = null
                    fileForDetails = item
                },
                onToggleBookmark = {
                    activeContextMenuItem = null
                    onToggleBookmark(item.path, item.name)
                },
                onCompress = {
                    activeContextMenuItem = null
                    fileToCompress = item
                    showCreateZipDialog = true
                },
                onExtract = {
                    activeContextMenuItem = null
                    onExtractArchive(item)
                },
                onSelect = {
                    activeContextMenuItem = null
                    onToggleSelect(item.id)
                }
            )
        }

        // Browsing Settings Sheet (Contextual Settings)
        if (showBrowsingSettingsSheet) {
            BrowsingSettingsSheet(
                isGridView = uiState.isGridView,
                showHiddenFiles = uiState.showHiddenFiles,
                sortConfig = uiState.sortConfig,
                onToggleLayout = onToggleLayout,
                onToggleShowHidden = onToggleShowHidden,
                onSelectSortBy = onSortChange,
                onToggleSortDirection = onToggleSortDirection,
                onDismiss = { showBrowsingSettingsSheet = false }
            )
        }

        // Operation Progress Dialog with Cancellation
        if (uiState.isOperating) {
            NovaConfirmDialog(
                title = "File Operation",
                message = uiState.operationMessage ?: "Processing, please wait...",
                confirmText = "Dismiss",
                cancelText = "Cancel Operation",
                onConfirm = { /* keep running in background */ },
                onDismiss = { onCancelOperation() }
            )
        }

        // Delete Confirmations
        if (showDeleteConfirm) {
            NovaConfirmDialog(
                title = "Delete Files",
                message = "Are you sure you want to delete ${uiState.selectedCount} selected item(s)? This action cannot be undone.",
                confirmText = "Delete",
                cancelText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    showDeleteConfirm = false
                    onDelete()
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }

        if (showSingleDeleteConfirm && fileToDelete != null) {
            val item = fileToDelete!!
            NovaConfirmDialog(
                title = "Delete ${if (item.isDirectory) "Folder" else "File"}",
                message = "Are you sure you want to delete '${item.name}'? This cannot be undone.",
                confirmText = "Delete",
                cancelText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    showSingleDeleteConfirm = false
                    onDeleteSingleFile(item)
                    fileToDelete = null
                },
                onDismiss = {
                    showSingleDeleteConfirm = false
                    fileToDelete = null
                }
            )
        }

        // Create Folder Dialog
        if (showCreateFolderDialog) {
            NovaInputDialog(
                title = "New Folder",
                placeholder = "Folder name",
                confirmText = "Create",
                cancelText = "Cancel",
                onConfirm = { folderName ->
                    showCreateFolderDialog = false
                    onCreateFolder(folderName)
                },
                onDismiss = { showCreateFolderDialog = false }
            )
        }

        // Rename Dialog
        if (showRenameDialog && fileToRename != null) {
            val item = fileToRename!!
            NovaInputDialog(
                title = "Rename",
                initialValue = item.name,
                placeholder = "New name",
                confirmText = "Rename",
                cancelText = "Cancel",
                onConfirm = { newName ->
                    showRenameDialog = false
                    onRenameFile(item, newName)
                    fileToRename = null
                },
                onDismiss = {
                    showRenameDialog = false
                    fileToRename = null
                }
            )
        }

        // Create ZIP Dialog
        if (showCreateZipDialog) {
            val defaultName = if (fileToCompress != null) {
                "${File(fileToCompress!!.path).nameWithoutExtension}.zip"
            } else {
                "Archive_${System.currentTimeMillis()}.zip"
            }
            NovaInputDialog(
                title = "Compress to ZIP",
                initialValue = defaultName,
                placeholder = "Archive name",
                confirmText = "Compress",
                cancelText = "Cancel",
                onConfirm = { zipName ->
                    showCreateZipDialog = false
                    if (fileToCompress != null) {
                        onCompressSingleFile(fileToCompress!!, zipName)
                        fileToCompress = null
                    } else {
                        onCompressSelected(zipName)
                    }
                },
                onDismiss = {
                    showCreateZipDialog = false
                    fileToCompress = null
                }
            )
        }

        // Bookmarks / Favorites Sheet
        if (showBookmarksSheet) {
            NovaBottomSheet(onDismissRequest = { showBookmarksSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Favorites & Shortcuts",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (uiState.bookmarks.isEmpty()) {
                        Text(
                            text = "No saved bookmarks. Tap the star icon to bookmark any directory.",
                            style = NovaTheme.typography.body,
                            color = NovaTheme.colors.textSecondary
                        )
                    } else {
                        uiState.bookmarks.forEach { bm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NovaTheme.colors.surfaceElevated)
                                    .clickable {
                                        showBookmarksSheet = false
                                        onFolderClick(bm.path)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = NovaTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = bm.name, style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)
                                        Text(text = bm.path, style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                IconButton(
                                    onClick = { onToggleBookmark(bm.path, bm.name) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove bookmark",
                                        tint = NovaTheme.colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // More Actions Sheet
        if (showMoreActionsSheet) {
            val selectedFiles = (uiState.folders + uiState.files).filter { uiState.selectedIds.contains(it.id) }
            val singleSelected = selectedFiles.singleOrNull()
            NovaBottomSheet(onDismissRequest = { showMoreActionsSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "More Actions (${uiState.selectedCount} items)",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary
                    )
                    NovaButton(
                        text = "Batch Rename",
                        onClick = {
                            showMoreActionsSheet = false
                            showBatchRenameDialog = true
                        },
                        variant = NovaButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NovaButton(
                        text = "Move to Trash (.Trash)",
                        onClick = {
                            showMoreActionsSheet = false
                            val trashDir = File(uiState.currentPath, ".Trash")
                            onMoveToTrash(trashDir)
                        },
                        variant = NovaButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (singleSelected != null) {
                        NovaButton(
                            text = "Properties & SHA-256",
                            onClick = {
                                showMoreActionsSheet = false
                                fileForDetails = singleSelected
                            },
                            variant = NovaButtonVariant.SECONDARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // File Details & SHA-256 Checksum Verifier Sheet
        if (fileForDetails != null) {
            val detailItem = fileForDetails!!
            var sha256Hash by remember(detailItem.path) { mutableStateOf<String?>(null) }
            var expectedHash by remember(detailItem.path) { mutableStateOf("") }
            NovaBottomSheet(onDismissRequest = { fileForDetails = null }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = detailItem.name,
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary
                    )
                    Text(
                        text = "Path: ${detailItem.path}",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary
                    )
                    Text(
                        text = "Size: ${FileFormatters.formatBytes(detailItem.sizeBytes)} (${detailItem.sizeBytes} bytes)",
                        style = NovaTheme.typography.captionMedium,
                        color = NovaTheme.colors.textPrimary
                    )
                    Text(
                        text = "Modified: ${FileFormatters.formatDate(detailItem.lastModifiedMs)}",
                        style = NovaTheme.typography.captionMedium,
                        color = NovaTheme.colors.textSecondary
                    )
                    if (sha256Hash != null) {
                        Text(
                            text = "SHA-256: $sha256Hash",
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.primary
                        )
                        OutlinedTextField(
                            value = expectedHash,
                            onValueChange = { expectedHash = it },
                            placeholder = { Text("Paste expected SHA-256 to verify", color = NovaTheme.colors.textTertiary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (expectedHash.isNotBlank()) {
                            val matches = sha256Hash.equals(expectedHash.trim(), ignoreCase = true)
                            Text(
                                text = if (matches) "Checksum Match" else "Checksum Mismatch",
                                style = NovaTheme.typography.captionMedium,
                                color = if (matches) NovaTheme.colors.success else NovaTheme.colors.error
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        NovaButton(
                            text = if (sha256Hash == null) "Compute SHA-256" else "Copy Hash",
                            onClick = {
                                if (sha256Hash == null) {
                                    uiScope.launch { sha256Hash = onComputeSha256(detailItem.path) }
                                } else {
                                    clipboardManager.setText(AnnotatedString(sha256Hash!!))
                                    Toast.makeText(context, "SHA-256 copied", Toast.LENGTH_SHORT).show()
                                }
                            },
                            variant = NovaButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )
                        NovaButton(
                            text = "Close",
                            onClick = { fileForDetails = null },
                            variant = NovaButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Batch Rename Sheet
        if (showBatchRenameDialog) {
            var prefix by rememberSaveable { mutableStateOf("") }
            var suffix by rememberSaveable { mutableStateOf("") }
            var findText by rememberSaveable { mutableStateOf("") }
            var replaceText by rememberSaveable { mutableStateOf("") }
            var addCounter by rememberSaveable { mutableStateOf(true) }
            NovaBottomSheet(onDismissRequest = { showBatchRenameDialog = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Batch Rename (${uiState.selectedCount} items)",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary
                    )
                    OutlinedTextField(
                        value = prefix, onValueChange = { prefix = it },
                        label = { Text("Prefix") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = suffix, onValueChange = { suffix = it },
                        label = { Text("Suffix") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = findText, onValueChange = { findText = it },
                            label = { Text("Find") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = replaceText, onValueChange = { replaceText = it },
                            label = { Text("Replace") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(checked = addCounter, onCheckedChange = { addCounter = it })
                        Text("Append sequential counter (_001, _002...)", style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
                    }
                    NovaButton(
                        text = "Apply Batch Rename",
                        onClick = {
                            showBatchRenameDialog = false
                            onBatchRename(prefix, suffix, findText, replaceText, if (addCounter) 1 else null)
                        },
                        variant = NovaButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Archive Browser Sheet
        if (archiveToBrowse != null) {
            ArchiveBrowserSheet(
                archiveItem = archiveToBrowse!!,
                onExtractAll = { onExtractArchive(it) },
                onExtractEntry = { item, entryName -> onExtractArchiveEntry(item, entryName) },
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
private fun StandardHeader(
    folderTitle: String,
    folderPath: String,
    isGridView: Boolean,
    searchQuery: String,
    isSearching: Boolean,
    canNavigateUp: Boolean,
    isBookmarked: Boolean,
    onNavigateBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCreateFolder: () -> Unit,
    onToggleBookmark: () -> Unit,
    overflowMenuExpanded: Boolean,
    onDismissOverflow: () -> Unit,
    onOpenOverflow: () -> Unit,
    showHiddenFiles: Boolean,
    onToggleShowHidden: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        if (isSearching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search files...", color = NovaTheme.colors.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NovaTheme.colors.textPrimary,
                        unfocusedTextColor = NovaTheme.colors.textPrimary,
                        focusedBorderColor = NovaTheme.colors.primary,
                        unfocusedBorderColor = NovaTheme.colors.cardBorder
                    )
                )
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Search",
                        tint = NovaTheme.colors.textPrimary
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (canNavigateUp) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Up",
                                tint = NovaTheme.colors.textPrimary
                            )
                        }
                    }
                    Text(
                        text = folderTitle,
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Bookmark Folder",
                            tint = if (isBookmarked) NovaTheme.colors.primary else NovaTheme.colors.textSecondary
                        )
                    }
                    IconButton(onClick = onToggleSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NovaTheme.colors.textSecondary
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Browsing & Sort Options",
                            tint = NovaTheme.colors.textSecondary
                        )
                    }
                    IconButton(onClick = onCreateFolder) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = NovaTheme.colors.textSecondary
                        )
                    }
                    Box {
                        IconButton(onClick = onOpenOverflow) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = NovaTheme.colors.textSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = onDismissOverflow
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (showHiddenFiles) "Hide Hidden Files" else "Show Hidden Files") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    onDismissOverflow()
                                    onToggleShowHidden()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bookmarks & Shortcuts") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                onClick = {
                                    onDismissOverflow()
                                    onOpenBookmarks()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    onDismissOverflow()
                                    onRefresh()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionHeader(
    selectedCount: Int,
    isAllSelected: Boolean,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Selection",
                    tint = NovaTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$selectedCount Selected",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
        }

        IconButton(onClick = onSelectAll) {
            Icon(
                imageVector = Icons.Default.SelectAll,
                contentDescription = if (isAllSelected) "Deselect All" else "Select All",
                tint = if (isAllSelected) NovaTheme.colors.primary else NovaTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun SelectionBottomDock(
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onCompress: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .shadow(NovaTheme.elevation.level4, NovaTheme.shapes.pill)
            .clip(NovaTheme.shapes.pill)
            .background(NovaTheme.colors.surfaceElevated)
            .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.pill)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockActionButton(icon = Icons.Default.ContentCopy, label = "Copy", onClick = onCopy)
        DockActionButton(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = "Cut", onClick = onMove)
        DockActionButton(icon = Icons.Default.Delete, label = "Delete", tint = NovaTheme.colors.error, onClick = onDelete)
        DockActionButton(icon = Icons.Default.Share, label = "Share", onClick = onShare)
        DockActionButton(icon = Icons.Default.FolderZip, label = "Zip", onClick = onCompress)
        DockActionButton(icon = Icons.Default.MoreHoriz, label = "More", onClick = onMore)
    }
}

@Composable
private fun DockActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = NovaTheme.colors.textPrimary
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = NovaTheme.typography.caption,
            color = tint
        )
    }
}


