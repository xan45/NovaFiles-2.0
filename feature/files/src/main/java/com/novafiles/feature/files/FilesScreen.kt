package com.novafiles.feature.files

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
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.novafiles.core.common.FileFormatters
import kotlinx.coroutines.launch
import java.io.File
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.ui.components.NovaConfirmDialog
import com.novafiles.core.ui.components.NovaInputDialog
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import androidx.compose.material3.ExperimentalMaterial3Api
import com.novafiles.core.ui.components.NovaEmptyState
import com.novafiles.core.ui.components.NovaErrorState
import com.novafiles.core.ui.components.NovaFileRow
import com.novafiles.core.ui.components.NovaFolderCard
import com.novafiles.core.ui.components.NovaLoadingState

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
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<FileItem?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var showMoreActionsSheet by remember { mutableStateOf(false) }
    var showCreateZipDialog by remember { mutableStateOf(false) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var fileForDetails by remember { mutableStateOf<FileItem?>(null) }
    var archiveToBrowse by remember { mutableStateOf<FileItem?>(null) }
    var apkToInspect by remember { mutableStateOf<FileItem?>(null) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiScope = rememberCoroutineScope()

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
            // Header: Switches between Standard Mode and Selection Mode per design reference
            if (uiState.isSelectionMode) {
                SelectionHeader(
                    selectedCount = uiState.selectedCount,
                    isAllSelected = uiState.isAllSelected,
                    onClearSelection = onClearSelection,
                    onSelectAll = onSelectAll
                )
            } else {
                StandardHeader(
                    breadcrumbs = uiState.breadcrumbs,
                    isGridView = uiState.isGridView,
                    searchQuery = uiState.searchQuery,
                    isSearching = showSearchField,
                    onToggleSearch = {
                        showSearchField = !showSearchField
                        if (!showSearchField) onSearchQueryChange("")
                    },
                    onSearchQueryChange = onSearchQueryChange,
                    onBreadcrumbClick = onFolderClick,
                    onToggleLayout = onToggleLayout,
                    isBookmarked = uiState.bookmarks.any { it.path == uiState.currentPath },
                    onToggleBookmark = {
                        val name = uiState.breadcrumbs.lastOrNull()?.name ?: File(uiState.currentPath).name
                        onToggleBookmark(uiState.currentPath, name)
                    }
                )
            }

            // Pinned Bookmarks Quick-Jump Bar
            if (uiState.bookmarks.isNotEmpty() && !uiState.isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.bookmarks.forEach { bm ->
                        val isCurrent = bm.path == uiState.currentPath
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) NovaTheme.colors.primary.copy(alpha = 0.2f) else NovaTheme.colors.surface)
                                .clickable { onFolderClick(bm.path) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isCurrent) NovaTheme.colors.primary else NovaTheme.colors.textTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = bm.name,
                                style = NovaTheme.typography.caption,
                                color = if (isCurrent) NovaTheme.colors.primary else NovaTheme.colors.textSecondary
                            )
                        }
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

            // Main Content Area
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
                        subtitle = "No files or subdirectories found here",
                        modifier = Modifier.weight(1f)
                    )
                }
                uiState.isGridView -> {
                    val combinedItems = uiState.folders + uiState.files
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 148.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(combinedItems, key = { it.id }) { item ->
                            val isSelected = uiState.selectedIds.contains(item.id)
                            if (item.isDirectory) {
                                NovaFolderCard(
                                    name = item.name,
                                    itemCount = item.itemCount ?: 0,
                                    dateText = FileFormatters.formatDate(item.lastModifiedMs),
                                    isSelected = isSelected,
                                    cardColor = NovaTheme.colors.surfaceElevated,
                                    folderIconColor = NovaTheme.colors.categoryFolders,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            onToggleSelect(item.id)
                                        } else {
                                            onFolderClick(item.path)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(NovaTheme.shapes.large)
                                        .background(NovaTheme.colors.surfaceElevated)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.cardBorder,
                                            shape = NovaTheme.shapes.large
                                        )
                                        .padding(8.dp)
                                ) {
                                    NovaFileRow(
                                        item = item,
                                        formattedSize = FileFormatters.formatBytes(item.sizeBytes),
                                        formattedDate = FileFormatters.formatDate(item.lastModifiedMs),
                                        onClick = {
                                            if (uiState.isSelectionMode) {
                                                onToggleSelect(item.id)
                                            } else if (item.fileType == FileType.ARCHIVE || item.name.endsWith(".zip", true) || item.name.endsWith(".tar", true) || item.name.endsWith(".tar.gz", true) || item.name.endsWith(".tgz", true)) {
                                                archiveToBrowse = item
                                            } else if (item.fileType == FileType.APK || item.name.endsWith(".apk", true)) {
                                                apkToInspect = item
                                            } else {
                                                onFileClick(item)
                                            }
                                        },
                                        onMenuClick = { onToggleSelect(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
                    ) {
                        // Horizontal Folders Section (if folders exist)
                        if (uiState.folders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Folders",
                                    style = NovaTheme.typography.subtitle,
                                    color = NovaTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    uiState.folders.forEach { folder ->
                                        val isSelected = uiState.selectedIds.contains(folder.id)
                                        NovaFolderCard(
                                            name = folder.name,
                                            itemCount = folder.itemCount ?: 0,
                                            dateText = FileFormatters.formatDate(folder.lastModifiedMs),
                                            isSelected = isSelected,
                                            cardColor = NovaTheme.colors.surfaceElevated,
                                            folderIconColor = NovaTheme.colors.categoryFolders,
                                            onClick = {
                                                if (uiState.isSelectionMode) {
                                                    onToggleSelect(folder.id)
                                                } else {
                                                    onFolderClick(folder.path)
                                                }
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        // Files Section Header with Sort Dropdown
                        if (uiState.files.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Files (${uiState.files.size})",
                                        style = NovaTheme.typography.subtitle,
                                        color = NovaTheme.colors.textPrimary
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = onToggleSortDirection,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.sortConfig.direction == SortDirection.ASCENDING) {
                                                    Icons.Default.ArrowUpward
                                                } else {
                                                    Icons.Default.ArrowDownward
                                                },
                                                contentDescription = "Toggle Sort Direction",
                                                tint = NovaTheme.colors.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Box {
                                            Row(
                                                modifier = Modifier
                                                    .clip(NovaTheme.shapes.small)
                                                    .clickable { sortMenuExpanded = true }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = uiState.sortConfig.sortBy.name.lowercase().replaceFirstChar { it.uppercase() },
                                                    style = NovaTheme.typography.captionMedium,
                                                    color = NovaTheme.colors.textSecondary
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "Sort Options",
                                                    tint = NovaTheme.colors.textSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                        DropdownMenu(
                                            expanded = sortMenuExpanded,
                                            onDismissRequest = { sortMenuExpanded = false }
                                        ) {
                                            SortBy.entries.forEach { sortBy ->
                                                DropdownMenuItem(
                                                    text = { Text(sortBy.name) },
                                                    onClick = {
                                                        sortMenuExpanded = false
                                                        onSortChange(sortBy)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                        }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Files List
                            items(uiState.files, key = { it.id }) { file ->
                                val isSelected = uiState.selectedIds.contains(file.id)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        NovaFileRow(
                                            item = file,
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
                                            onMenuClick = { onToggleSelect(file.id) }
                                        )
                                    }

                                    if (uiState.isSelectionMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { onToggleSelect(file.id) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = NovaTheme.colors.primary,
                                                uncheckedColor = NovaTheme.colors.cardBorder
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button (+) for Add/Create Folder
        if (!uiState.isSelectionMode && uiState.clipboard == null) {
            FloatingActionButton(
                onClick = { showCreateFolderDialog = true },
                containerColor = NovaTheme.colors.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 96.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Folder")
            }
        }

        // Clipboard Floating Paste Action Bar
        if (uiState.clipboard != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = 96.dp)
                    .clip(NovaTheme.shapes.extraLarge)
                    .background(NovaTheme.colors.surfaceElevated)
                    .border(1.dp, NovaTheme.colors.primary, NovaTheme.shapes.extraLarge)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${uiState.clipboard.files.size} ready to ${uiState.clipboard.op.name.lowercase()}",
                    style = NovaTheme.typography.captionMedium,
                    color = NovaTheme.colors.textPrimary
                )
                NovaButton(
                    text = "Paste",
                    onClick = onPaste,
                    variant = NovaButtonVariant.PRIMARY
                )
                NovaButton(
                    text = "Cancel",
                    onClick = onClearClipboard,
                    variant = NovaButtonVariant.SECONDARY
                )
            }
        }

        // Selection Bottom Action Dock (Copy, Move, Delete, Rename)
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            SelectionBottomDock(
                onCopy = onCopy,
                onMove = onMove,
                onDelete = { showDeleteConfirm = true },
                onEdit = {
                    if (uiState.selectedCount > 1) {
                        showBatchRenameDialog = true
                    } else {
                        val item = (uiState.folders + uiState.files).firstOrNull { uiState.selectedIds.contains(it.id) }
                        if (item != null) {
                            fileToRename = item
                            showRenameDialog = true
                        }
                    }
                },
                onMore = { showMoreActionsSheet = true }
            )
        }

        // Delete / Move to Trash Confirmation Sheet
        if (showDeleteConfirm) {
            NovaBottomSheet(onDismissRequest = { showDeleteConfirm = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Delete ${uiState.selectedCount} selected items?",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary
                    )
                    Text(
                        text = "Move to Trash to keep them recoverable for 30 days, or delete permanently.",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary
                    )
                    NovaButton(
                        text = "Move to Trash (Recoverable)",
                        onClick = {
                            showDeleteConfirm = false
                            onMoveToTrash(File(context.filesDir, "trash"))
                        },
                        variant = NovaButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NovaButton(
                        text = "Delete Permanently",
                        onClick = {
                            showDeleteConfirm = false
                            onDelete()
                        },
                        variant = NovaButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Create Folder Dialog Modal
        if (showCreateFolderDialog) {
            NovaInputDialog(
                title = "New Folder",
                placeholder = "Folder name",
                confirmText = "Create",
                onConfirm = { name ->
                    showCreateFolderDialog = false
                    onCreateFolder(name)
                },
                onDismiss = { showCreateFolderDialog = false }
            )
        }

        // Rename File Dialog Modal
        if (showRenameDialog && fileToRename != null) {
            NovaInputDialog(
                title = "Rename",
                initialValue = fileToRename!!.name,
                placeholder = "New name",
                confirmText = "Rename",
                onConfirm = { newName ->
                    val target = fileToRename
                    showRenameDialog = false
                    fileToRename = null
                    if (target != null) {
                        onRenameFile(target, newName)
                    }
                },
                onDismiss = {
                    showRenameDialog = false
                    fileToRename = null
                }
            )
        }

        // Compress to ZIP Dialog Modal
        if (showCreateZipDialog) {
            NovaInputDialog(
                title = "Compress ${uiState.selectedCount} items to ZIP",
                initialValue = "Archive.zip",
                placeholder = "Archive name (.zip)",
                confirmText = "Compress",
                onConfirm = { zipName ->
                    showCreateZipDialog = false
                    onCompressSelected(zipName)
                },
                onDismiss = { showCreateZipDialog = false }
            )
        }

        // More Actions Sheet (MiXplorer/ES Power Tools: Compress, Extract, Properties)
        if (showMoreActionsSheet) {
            val singleSelected = (uiState.folders + uiState.files).firstOrNull { uiState.selectedIds.contains(it.id) }
            val isSingleArchive = uiState.selectedCount == 1 && singleSelected != null && (
                singleSelected.fileType == FileType.ARCHIVE ||
                    singleSelected.name.endsWith(".zip", ignoreCase = true) ||
                    singleSelected.name.endsWith(".tar", ignoreCase = true) ||
                    singleSelected.name.endsWith(".tar.gz", ignoreCase = true) ||
                    singleSelected.name.endsWith(".tgz", ignoreCase = true)
                )
            NovaBottomSheet(onDismissRequest = { showMoreActionsSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Advanced Actions (${uiState.selectedCount} selected)",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary
                    )
                    NovaButton(
                        text = "Batch Rename (Prefix / Replace / Counter)",
                        onClick = {
                            showMoreActionsSheet = false
                            showBatchRenameDialog = true
                        },
                        variant = NovaButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NovaButton(
                        text = "Compress to ZIP Archive",
                        onClick = {
                            showMoreActionsSheet = false
                            showCreateZipDialog = true
                        },
                        variant = NovaButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isSingleArchive && singleSelected != null) {
                        NovaButton(
                            text = "Extract Archive Here",
                            onClick = {
                                showMoreActionsSheet = false
                                onExtractArchive(singleSelected)
                            },
                            variant = NovaButtonVariant.SECONDARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (singleSelected != null) {
                        NovaButton(
                            text = "Properties & Path Info",
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
                        androidx.compose.material3.OutlinedTextField(
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
            var prefix by remember { mutableStateOf("") }
            var suffix by remember { mutableStateOf("") }
            var findText by remember { mutableStateOf("") }
            var replaceText by remember { mutableStateOf("") }
            var addCounter by remember { mutableStateOf(true) }
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
                    androidx.compose.material3.OutlinedTextField(
                        value = prefix, onValueChange = { prefix = it },
                        label = { Text("Prefix") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = suffix, onValueChange = { suffix = it },
                        label = { Text("Suffix") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.OutlinedTextField(
                            value = findText, onValueChange = { findText = it },
                            label = { Text("Find") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = replaceText, onValueChange = { replaceText = it },
                            label = { Text("Replace") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = addCounter, onCheckedChange = { addCounter = it })
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
    breadcrumbs: List<BreadcrumbItem>,
    isGridView: Boolean,
    searchQuery: String,
    isSearching: Boolean,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    onToggleLayout: () -> Unit,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        if (isSearching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search files...", color = NovaTheme.colors.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
                        tint = NovaTheme.colors.textPrimary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Files",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary
                )

                Row {
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Bookmark Folder",
                            tint = if (isBookmarked) NovaTheme.colors.primary else NovaTheme.colors.textPrimary
                        )
                    }
                    IconButton(onClick = onToggleSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }
                    IconButton(onClick = onToggleLayout) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        // Breadcrumb Trail (Reference: Device > Photos > Camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            breadcrumbs.forEachIndexed { index, crumb ->
                val isLast = index == breadcrumbs.size - 1
                Text(
                    text = crumb.name,
                    style = NovaTheme.typography.captionMedium,
                    color = if (isLast) NovaTheme.colors.primary else NovaTheme.colors.textSecondary,
                    modifier = Modifier.clickable { onBreadcrumbClick(crumb.path) }
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
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

        Checkbox(
            checked = isAllSelected,
            onCheckedChange = { onSelectAll() },
            colors = CheckboxDefaults.colors(
                checkedColor = NovaTheme.colors.primary,
                uncheckedColor = NovaTheme.colors.cardBorder
            )
        )
    }
}

@Composable
private fun SelectionBottomDock(
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .shadow(NovaTheme.elevation.level4, NovaTheme.shapes.pill)
            .clip(NovaTheme.shapes.pill)
            .background(NovaTheme.colors.surface)
            .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.pill)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockActionButton(icon = Icons.Default.ContentCopy, label = "Copy", onClick = onCopy)
        DockActionButton(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = "Move", onClick = onMove)
        DockActionButton(icon = Icons.Default.Delete, label = "Delete", tint = NovaTheme.colors.error, onClick = onDelete)
        DockActionButton(icon = Icons.Default.Edit, label = "Edit", onClick = onEdit)
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
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
