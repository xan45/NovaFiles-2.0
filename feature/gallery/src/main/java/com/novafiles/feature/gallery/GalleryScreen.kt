@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.SortDirection
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaCard
import com.novafiles.core.ui.components.NovaConfirmDialog
import com.novafiles.core.ui.components.NovaInputDialog
import com.novafiles.feature.files.MultiSelectionBottomDock
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showSortSheet by remember { mutableStateOf(false) }

    // Predictive / In-App Back Navigation
    BackHandler {
        when {
            uiState.activeViewerIndex != null -> viewModel.closeViewer()
            uiState.isSelectionMode -> viewModel.clearSelection()
            uiState.isSearchOpen -> viewModel.toggleSearch(false)
            uiState.selectedFolder != null -> viewModel.selectFolder(null)
            uiState.selectedUserAlbum != null -> viewModel.selectUserAlbum(null)
            uiState.currentSection != GallerySection.ALL_IMAGES -> viewModel.selectSection(GallerySection.ALL_IMAGES)
            else -> onNavigateBack()
        }
    }

    // Full-Screen Image Viewer
    if (uiState.activeViewerIndex != null) {
        val activeList = uiState.activeImageList
        ImageViewerScreen(
            images = activeList,
            initialIndex = uiState.activeViewerIndex ?: 0,
            isFavorite = { viewModel.isFavorite(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDeleteImage = { item ->
                viewModel.deleteSingleImage(item) {
                    if (activeList.size <= 1) {
                        viewModel.closeViewer()
                    }
                }
            },
            onRenameImage = { item, newName ->
                viewModel.renameImage(item, newName)
            },
            onNavigateBack = { viewModel.closeViewer() }
        )
        return
    }

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
            // Top Bar
            GalleryTopBar(
                uiState = uiState,
                onNavigateBack = {
                    when {
                        uiState.isSelectionMode -> viewModel.clearSelection()
                        uiState.isSearchOpen -> viewModel.toggleSearch(false)
                        uiState.selectedFolder != null -> viewModel.selectFolder(null)
                        uiState.selectedUserAlbum != null -> viewModel.selectUserAlbum(null)
                        else -> onNavigateBack()
                    }
                },
                onToggleSearch = { viewModel.toggleSearch(!uiState.isSearchOpen) },
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onToggleFavorites = { viewModel.toggleOnlyFavorites() },
                onOpenSort = { showSortSheet = true },
                onOpenSettings = { viewModel.toggleSettingsSheet(true) },
                onSelectAll = { viewModel.selectAll() }
            )

            // Primary Navigation Tabs (All Images | Folders | Albums)
            if (uiState.selectedFolder == null && uiState.selectedUserAlbum == null) {
                GallerySectionTabs(
                    currentSection = uiState.currentSection,
                    onSectionSelect = { viewModel.selectSection(it) }
                )
            } else {
                // Breadcrumb Bar for active folder / album
                val breadcrumbTitle = uiState.selectedFolder?.name ?: uiState.selectedUserAlbum?.name ?: ""
                val countText = "${uiState.activeImageList.size} items"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = breadcrumbTitle,
                        style = NovaTheme.typography.subtitle,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = countText,
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Content Area based on selected section / drilldown
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.selectedFolder != null || uiState.selectedUserAlbum != null || uiState.currentSection == GallerySection.ALL_IMAGES -> {
                        AllImagesContentView(
                            uiState = uiState,
                            onImageClick = { index, item ->
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleItemSelection(item.id)
                                } else {
                                    viewModel.openViewer(index)
                                }
                            },
                            onImageLongClick = { item ->
                                viewModel.toggleItemSelection(item.id)
                            }
                        )
                    }
                    uiState.currentSection == GallerySection.FOLDERS -> {
                        FoldersContentView(
                            folders = uiState.folders,
                            onFolderClick = { viewModel.selectFolder(it) },
                            onExcludeFolder = { viewModel.showExcludeFolderDialog(it) }
                        )
                    }
                    uiState.currentSection == GallerySection.ALBUMS -> {
                        AlbumsContentView(
                            albums = uiState.userAlbums,
                            allItems = uiState.allItems,
                            onCreateAlbum = { viewModel.toggleCreateAlbumDialog(true) },
                            onAlbumClick = { viewModel.selectUserAlbum(it) },
                            onRenameAlbum = { viewModel.showRenameAlbumDialog(it) },
                            onDeleteAlbum = { viewModel.showDeleteAlbumDialog(it) }
                        )
                    }
                }
            }
        }

        // Floating Multi-Selection Bottom Dock
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            MultiSelectionBottomDock(
                onCopy = { viewModel.showCopyPicker(true) },
                onMove = { viewModel.showMovePicker(true) },
                onDelete = { viewModel.showDeleteConfirm(true) },
                onShare = { viewModel.shareSelected(context) },
                onCompress = { viewModel.toggleAddToAlbumDialog(true) }, // Used as "Add to Album" action
                onMore = {
                    if (uiState.selectedUserAlbum != null) {
                        viewModel.removeSelectedFromCurrentAlbum()
                    } else {
                        viewModel.selectAll()
                    }
                }
            )
        }

        // ==========================================
        // Dialogs & Sheets
        // ==========================================

        // Dedicated Gallery Settings Sheet
        if (uiState.showSettingsSheet) {
            GallerySettingsSheet(
                preferences = uiState.preferences,
                onThumbnailSizeChange = { viewModel.setThumbnailSize(it) },
                onGridSpacingChange = { viewModel.setGridSpacing(it) },
                onRoundedThumbnailsChange = { viewModel.setRoundedThumbnails(it) },
                onSortConfigChange = { viewModel.setSortConfig(it) },
                onGroupByDateChange = { viewModel.setGroupByDate(it) },
                onShowHiddenChange = { viewModel.setShowHiddenFolders(it) },
                onRestoreFolder = { viewModel.restoreFolder(it) },
                onRescan = { viewModel.loadMedia() },
                onDismiss = { viewModel.toggleSettingsSheet(false) }
            )
        }

        // Quick Sort & View Sheet
        if (showSortSheet) {
            GallerySortSheet(
                currentConfig = uiState.preferences.sortConfig,
                groupByDate = uiState.preferences.groupByDate,
                onSortChange = { viewModel.setSortConfig(it) },
                onToggleGroupByDate = { viewModel.setGroupByDate(it) },
                onDismiss = { showSortSheet = false }
            )
        }

        // Create Album Dialog
        if (uiState.showCreateAlbumDialog) {
            NovaInputDialog(
                title = "New Album",
                initialValue = "Album 1",
                placeholder = "Album name",
                confirmText = "Create",
                cancelText = "Cancel",
                onConfirm = { name -> viewModel.createAlbum(name) },
                onDismiss = { viewModel.toggleCreateAlbumDialog(false) }
            )
        }

        // Rename Album Dialog
        if (uiState.showRenameAlbumDialog != null) {
            val album = uiState.showRenameAlbumDialog!!
            NovaInputDialog(
                title = "Rename Album",
                initialValue = album.name,
                placeholder = "New album name",
                confirmText = "Rename",
                cancelText = "Cancel",
                onConfirm = { newName -> viewModel.renameAlbum(album.id, newName) },
                onDismiss = { viewModel.showRenameAlbumDialog(null) }
            )
        }

        // Delete Album Dialog
        if (uiState.showDeleteAlbumDialog != null) {
            val album = uiState.showDeleteAlbumDialog!!
            NovaConfirmDialog(
                title = "Delete Album?",
                message = "Are you sure you want to delete album \"${album.name}\"? Photos in this album will NOT be deleted from disk.",
                confirmText = "Delete",
                cancelText = "Cancel",
                isDestructive = true,
                onConfirm = { viewModel.deleteAlbum(album.id) },
                onDismiss = { viewModel.showDeleteAlbumDialog(null) }
            )
        }

        // Exclude Folder Dialog
        if (uiState.showExcludeFolderDialog != null) {
            val folder = uiState.showExcludeFolderDialog!!
            NovaConfirmDialog(
                title = "Exclude Folder?",
                message = "Exclude \"${folder.name}\" from the gallery? You can restore it anytime in Gallery Settings.",
                confirmText = "Exclude",
                cancelText = "Cancel",
                isDestructive = false,
                onConfirm = { viewModel.excludeFolder(folder.path) },
                onDismiss = { viewModel.showExcludeFolderDialog(null) }
            )
        }

        // Add to Album Sheet
        if (uiState.showAddToAlbumDialog) {
            AddToAlbumSheet(
                albums = uiState.userAlbums,
                selectedCount = uiState.selectedCount,
                onSelectAlbum = { viewModel.addSelectedToAlbum(it.id) },
                onCreateNewAlbum = {
                    viewModel.toggleAddToAlbumDialog(false)
                    viewModel.toggleCreateAlbumDialog(true)
                },
                onDismiss = { viewModel.toggleAddToAlbumDialog(false) }
            )
        }

        // Delete Multiple Images Confirmation Dialog
        if (uiState.showDeleteConfirmDialog) {
            NovaConfirmDialog(
                title = "Delete ${uiState.selectedCount} Images?",
                message = "Are you sure you want to permanently delete the selected images from device storage?",
                confirmText = "Delete",
                cancelText = "Cancel",
                isDestructive = true,
                onConfirm = { viewModel.deleteSelected() },
                onDismiss = { viewModel.showDeleteConfirm(false) }
            )
        }
    }
}

// =========================================================================
// Top Bar Component
// =========================================================================

@Composable
private fun GalleryTopBar(
    uiState: GalleryUiState,
    onNavigateBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onOpenSort: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.isSelectionMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear selection",
                        tint = NovaTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${uiState.selectedCount} Selected",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap items to select/deselect",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary
                    )
                }
            }

            IconButton(onClick = onSelectAll) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = "Select All",
                    tint = NovaTheme.colors.primary
                )
            }
        } else if (uiState.isSearchOpen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NovaTheme.colors.textPrimary
                    )
                }

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search photos...", color = NovaTheme.colors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaTheme.colors.primary,
                        unfocusedBorderColor = NovaTheme.colors.cardBorder,
                        focusedTextColor = NovaTheme.colors.textPrimary,
                        unfocusedTextColor = NovaTheme.colors.textPrimary
                    ),
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = NovaTheme.colors.textSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                IconButton(onClick = onToggleSearch) {
                    Icon(Icons.Default.Close, contentDescription = "Exit search", tint = NovaTheme.colors.textPrimary)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NovaTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gallery",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Search toggle
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NovaTheme.colors.textPrimary
                    )
                }

                // Favorites filter toggle
                IconButton(onClick = onToggleFavorites) {
                    Icon(
                        imageVector = if (uiState.isOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Filter Favorites",
                        tint = if (uiState.isOnlyFavorites) Color(0xFFFF5252) else NovaTheme.colors.textPrimary
                    )
                }

                // Sort & View options
                IconButton(onClick = onOpenSort) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort and View",
                        tint = NovaTheme.colors.textPrimary
                    )
                }

                // Settings
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Gallery Settings",
                        tint = NovaTheme.colors.textPrimary
                    )
                }
            }
        }
    }
}

// =========================================================================
// Primary Navigation Section Tabs (All Images | Folders | Albums)
// =========================================================================

@Composable
private fun GallerySectionTabs(
    currentSection: GallerySection,
    onSectionSelect: (GallerySection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GallerySection.values().forEach { section ->
            val isSelected = currentSection == section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(NovaTheme.shapes.pill)
                    .background(
                        if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.cardBorder,
                        shape = NovaTheme.shapes.pill
                    )
                    .clickable { onSectionSelect(section) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = section.title,
                    color = if (isSelected) Color.White else NovaTheme.colors.textPrimary,
                    style = NovaTheme.typography.captionMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// =========================================================================
// 1. All Images Section View (Flat Grid or Date Grouped)
// =========================================================================

@Composable
private fun AllImagesContentView(
    uiState: GalleryUiState,
    onImageClick: (Int, GalleryItem) -> Unit,
    onImageLongClick: (GalleryItem) -> Unit
) {
    val items = uiState.activeImageList
    val prefs = uiState.preferences

    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (uiState.isLoading) "Scanning photos..." else "No photos found",
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textSecondary
            )
        }
        return
    }

    val columns = prefs.thumbnailSize.columns
    val spacingDp = prefs.gridSpacingDp.dp
    val cornerRadius = if (prefs.isRoundedThumbnails) 10.dp else 0.dp

    if (prefs.groupByDate && uiState.selectedFolder == null && uiState.selectedUserAlbum == null) {
        // Date Grouped View
        val dateGroups = uiState.dateGroups
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            dateGroups.forEach { group ->
                item(key = "header_${group.title}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.title,
                            style = NovaTheme.typography.subtitle,
                            color = NovaTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${group.items.size} photos",
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textSecondary
                        )
                    }
                }

                // Chunk items into rows matching columns
                val chunked = group.items.chunked(columns)
                items(chunked) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacingDp / 2),
                        horizontalArrangement = Arrangement.spacedBy(spacingDp)
                    ) {
                        for (item in rowItems) {
                            val globalIndex = items.indexOf(item)
                            val isSelected = uiState.selectedItemIds.contains(item.id)
                            val isFav = uiState.favorites.contains(item.id)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            ) {
                                GalleryImageCard(
                                    item = item,
                                    isSelected = isSelected,
                                    isFavorite = isFav,
                                    isSelectionMode = uiState.isSelectionMode,
                                    cornerRadius = cornerRadius,
                                    onClick = { onImageClick(globalIndex, item) },
                                    onLongClick = { onImageLongClick(item) }
                                )
                            }
                        }
                        // Fill empty weight slots if rowItems.size < columns
                        for (i in 0 until (columns - rowItems.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    } else {
        // Flat Grid View
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(spacingDp),
            verticalArrangement = Arrangement.spacedBy(spacingDp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val isSelected = uiState.selectedItemIds.contains(item.id)
                val isFav = uiState.favorites.contains(item.id)
                GalleryImageCard(
                    item = item,
                    isSelected = isSelected,
                    isFavorite = isFav,
                    isSelectionMode = uiState.isSelectionMode,
                    cornerRadius = cornerRadius,
                    onClick = { onImageClick(index, item) },
                    onLongClick = { onImageLongClick(item) },
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
private fun GalleryImageCard(
    item: GalleryItem,
    isSelected: Boolean,
    isFavorite: Boolean,
    isSelectionMode: Boolean,
    cornerRadius: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(NovaTheme.colors.surfaceElevated)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) NovaTheme.colors.primary else Color(0x22FFFFFF),
                shape = shape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = item.file,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dim overlay when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NovaTheme.colors.primary.copy(alpha = 0.25f))
            )
        }

        // Selection Checkbox in top right
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NovaTheme.colors.primary,
                        uncheckedColor = Color.White
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (isFavorite) {
            // Favorite Heart Badge in top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorited",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// =========================================================================
// 2. Folders Section View
// =========================================================================

@Composable
private fun FoldersContentView(
    folders: List<FolderItem>,
    onFolderClick: (FolderItem) -> Unit,
    onExcludeFolder: (FolderItem) -> Unit
) {
    if (folders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No image folders found",
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textSecondary
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders, key = { it.path }) { folder ->
            FolderCard(
                folder = folder,
                onClick = { onFolderClick(folder) },
                onExclude = { onExcludeFolder(folder) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun FolderCard(
    folder: FolderItem,
    onClick: () -> Unit,
    onExclude: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NovaTheme.shapes.large)
            .background(NovaTheme.colors.surfaceElevated)
            .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        // Folder Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color(0xFF1E2230))
        ) {
            if (folder.coverItem != null) {
                AsyncImage(
                    model = folder.coverItem.file,
                    contentDescription = folder.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = NovaTheme.colors.categoryFolders,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Top-left folder badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(NovaTheme.shapes.pill)
                    .background(Color(0xAA000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${folder.imageCount}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top-right 3-dot menu button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Exclude folder") },
                        leadingIcon = { Icon(Icons.Default.FolderOff, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onExclude()
                        }
                    )
                }
            }
        }

        // Folder Title & Metadata
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = folder.name,
                style = NovaTheme.typography.subtitle,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${folder.imageCount} images • ${FileFormatters.formatBytes(folder.totalSizeBytes)}",
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =========================================================================
// 3. Albums Section View
// =========================================================================

@Composable
private fun AlbumsContentView(
    albums: List<UserAlbum>,
    allItems: List<GalleryItem>,
    onCreateAlbum: () -> Unit,
    onAlbumClick: (UserAlbum) -> Unit,
    onRenameAlbum: (UserAlbum) -> Unit,
    onDeleteAlbum: (UserAlbum) -> Unit
) {
    val pathMap = remember(allItems) { allItems.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // "+ New Album" action card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(NovaTheme.shapes.large)
                .background(NovaTheme.colors.surfaceElevated)
                .border(1.dp, NovaTheme.colors.primary.copy(alpha = 0.5f), NovaTheme.shapes.large)
                .clickable(onClick = onCreateAlbum)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NovaTheme.colors.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Create New Album",
                    style = NovaTheme.typography.subtitle,
                    color = NovaTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Group custom photos without moving files",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (albums.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No custom albums created yet",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(albums, key = { it.id }) { album ->
                    val coverFile = album.imagePaths.firstOrNull()?.let { pathMap[it]?.file }
                    UserAlbumCard(
                        album = album,
                        coverFile = coverFile,
                        onClick = { onAlbumClick(album) },
                        onRename = { onRenameAlbum(album) },
                        onDelete = { onDeleteAlbum(album) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun UserAlbumCard(
    album: UserAlbum,
    coverFile: File?,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NovaTheme.shapes.large)
            .background(NovaTheme.colors.surfaceElevated)
            .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color(0xFF1E2230))
        ) {
            if (coverFile != null && coverFile.exists()) {
                AsyncImage(
                    model = coverFile,
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = NovaTheme.colors.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Top-right 3-dot menu button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Album Menu",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = NovaTheme.colors.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = album.name,
                style = NovaTheme.typography.subtitle,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${album.imagePaths.size} photos",
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary
            )
        }
    }
}

// =========================================================================
// Dedicated Gallery Settings Sheet
// =========================================================================

@Composable
private fun GallerySettingsSheet(
    preferences: GalleryPreferences,
    onThumbnailSizeChange: (ThumbnailSize) -> Unit,
    onGridSpacingChange: (Int) -> Unit,
    onRoundedThumbnailsChange: (Boolean) -> Unit,
    onSortConfigChange: (GallerySortConfig) -> Unit,
    onGroupByDateChange: (Boolean) -> Unit,
    onShowHiddenChange: (Boolean) -> Unit,
    onRestoreFolder: (String) -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Gallery Settings",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure display, sorting, and content preferences",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary
                )
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = NovaTheme.colors.cardBorder)
            }

            // Appearance Section
            item {
                Text(
                    text = "APPEARANCE",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Thumbnail Size / Density", style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThumbnailSize.values().forEach { size ->
                        val isSelected = preferences.thumbnailSize == size
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(NovaTheme.shapes.pill)
                                .background(if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surface)
                                .border(1.dp, if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.cardBorder, NovaTheme.shapes.pill)
                            .clickable { onThumbnailSizeChange(size) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = size.label,
                                color = if (isSelected) Color.White else NovaTheme.colors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Rounded Thumbnails", style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)
                        Text("Soft rounded corners on image tiles", style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
                    }
                    Switch(
                        checked = preferences.isRoundedThumbnails,
                        onCheckedChange = onRoundedThumbnailsChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaTheme.colors.primary)
                    )
                }
            }

            item {
                Text("Grid Spacing", style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2 to "Compact", 4 to "Normal", 8 to "Relaxed").forEach { (dpVal, label) ->
                        val isSelected = preferences.gridSpacingDp == dpVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(NovaTheme.shapes.pill)
                                .background(if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surface)
                                .border(1.dp, if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.cardBorder, NovaTheme.shapes.pill)
                                .clickable { onGridSpacingChange(dpVal) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else NovaTheme.colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Browsing Section
            item {
                HorizontalDivider(color = NovaTheme.colors.cardBorder)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BROWSING & GROUPING",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Group Photos by Date", style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)
                        Text("Organize into Today, Yesterday, and Month headers", style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
                    }
                    Switch(
                        checked = preferences.groupByDate,
                        onCheckedChange = onGroupByDateChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaTheme.colors.primary)
                    )
                }
            }

            // Content & Privacy Section
            item {
                HorizontalDivider(color = NovaTheme.colors.cardBorder)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CONTENT & PRIVACY",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Show Hidden Folders", style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)
                        Text("Include dot-directories and .nomedia folders", style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
                    }
                    Switch(
                        checked = preferences.showHiddenFolders,
                        onCheckedChange = onShowHiddenChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaTheme.colors.primary)
                    )
                }
            }

            // Excluded Folders Management
            if (preferences.excludedFolderPaths.isNotEmpty()) {
                item {
                    Text("Excluded Folders (${preferences.excludedFolderPaths.size})", style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(preferences.excludedFolderPaths.toList()) { path ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(NovaTheme.shapes.medium)
                            .background(NovaTheme.colors.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(File(path).name, color = NovaTheme.colors.textPrimary, style = NovaTheme.typography.subtitle)
                            Text(path, color = NovaTheme.colors.textSecondary, style = NovaTheme.typography.caption, maxLines = 1)
                        }
                        IconButton(onClick = { onRestoreFolder(path) }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore", tint = NovaTheme.colors.primary)
                        }
                    }
                }
            }

            // Rescan Library Button
            item {
                HorizontalDivider(color = NovaTheme.colors.cardBorder)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NovaTheme.shapes.large)
                        .background(NovaTheme.colors.primary.copy(alpha = 0.15f))
                        .clickable {
                            onRescan()
                            onDismiss()
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NovaTheme.colors.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh & Rescan Media", color = NovaTheme.colors.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// =========================================================================
// Quick Sort & View Sheet
// =========================================================================

@Composable
private fun GallerySortSheet(
    currentConfig: GallerySortConfig,
    groupByDate: Boolean,
    onSortChange: (GallerySortConfig) -> Unit,
    onToggleGroupByDate: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Sort & View Options",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(color = NovaTheme.colors.cardBorder)

            Text("Sort By", style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)

            GallerySortBy.values().forEach { sortBy ->
                val isSelected = currentConfig.sortBy == sortBy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NovaTheme.shapes.medium)
                        .background(if (isSelected) NovaTheme.colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable {
                            onSortChange(currentConfig.copy(sortBy = sortBy))
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(sortBy.label, color = if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.textPrimary)
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = NovaTheme.colors.primary)
                    }
                }
            }

            HorizontalDivider(color = NovaTheme.colors.cardBorder)

            Text("Direction", style = NovaTheme.typography.subtitle, color = NovaTheme.colors.textPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    SortDirection.DESCENDING to "Newest / Z-A (Desc)",
                    SortDirection.ASCENDING to "Oldest / A-Z (Asc)"
                ).forEach { (dir, label) ->
                    val isSelected = currentConfig.direction == dir
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(NovaTheme.shapes.pill)
                            .background(if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surface)
                            .border(1.dp, if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.cardBorder, NovaTheme.shapes.pill)
                            .clickable { onSortChange(currentConfig.copy(direction = dir)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else NovaTheme.colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// =========================================================================
// Add to Album Sheet
// =========================================================================

@Composable
private fun AddToAlbumSheet(
    albums: List<UserAlbum>,
    selectedCount: Int,
    onSelectAlbum: (UserAlbum) -> Unit,
    onCreateNewAlbum: () -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Add $selectedCount Images to Album",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(color = NovaTheme.colors.cardBorder)

            // Option: New Album
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NovaTheme.shapes.medium)
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f))
                    .clickable(onClick = onCreateNewAlbum)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = NovaTheme.colors.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Create New Album", color = NovaTheme.colors.primary, fontWeight = FontWeight.Bold)
            }

            if (albums.isEmpty()) {
                Text(
                    text = "No existing albums. Create one above!",
                    color = NovaTheme.colors.textSecondary,
                    style = NovaTheme.typography.caption
                )
            } else {
                Text("Select Existing Album", style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(albums) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NovaTheme.shapes.medium)
                                .clickable { onSelectAlbum(album) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(album.name, color = NovaTheme.colors.textPrimary, style = NovaTheme.typography.subtitle)
                            Text("${album.imagePaths.size} photos", color = NovaTheme.colors.textSecondary, style = NovaTheme.typography.caption)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
