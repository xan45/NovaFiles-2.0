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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
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
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.SortBy
import com.novafiles.core.ui.components.NovaConfirmDialog
import com.novafiles.core.ui.components.NovaInputDialog
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaEmptyState
import com.novafiles.core.ui.components.NovaErrorState
import com.novafiles.core.ui.components.NovaFileRow
import com.novafiles.core.ui.components.NovaFolderCard
import com.novafiles.core.ui.components.NovaLoadingState

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
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<FileItem?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }

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
                    onToggleLayout = onToggleLayout
                )
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
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
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
                    val item = (uiState.folders + uiState.files).firstOrNull { uiState.selectedIds.contains(it.id) }
                    if (item != null) {
                        fileToRename = item
                        showRenameDialog = true
                    }
                },
                onMore = {}
            )
        }

        // Delete Confirmation Modal
        if (showDeleteConfirm) {
            NovaConfirmDialog(
                title = "Delete ${uiState.selectedCount} items?",
                message = "These items will be permanently deleted from your storage.",
                confirmText = "Delete",
                onConfirm = {
                    showDeleteConfirm = false
                    onDelete()
                },
                onDismiss = { showDeleteConfirm = false }
            )
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
    onToggleLayout: () -> Unit
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
                        imageVector = Icons.Default.Add, // Rotated or close
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
