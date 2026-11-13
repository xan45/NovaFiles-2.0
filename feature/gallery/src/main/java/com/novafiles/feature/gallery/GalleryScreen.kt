@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.gallery

import androidx.activity.compose.BackHandler
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaCard
import com.novafiles.core.ui.components.NovaConfirmDialog
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

    BackHandler {
        when {
            uiState.isSelectionMode -> viewModel.clearSelection()
            uiState.selectedAlbum != "All" -> viewModel.selectAlbum("All")
            else -> onNavigateBack()
        }
    }

    // If active viewer is open, display full-screen viewer
    if (uiState.activeViewerIndex != null) {
        ImageViewerScreen(
            images = uiState.filteredItems,
            initialIndex = uiState.activeViewerIndex ?: 0,
            isFavorite = { viewModel.isFavorite(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDeleteImage = { item ->
                viewModel.toggleItemSelection(item.id)
                viewModel.deleteSelected {
                    viewModel.closeViewer()
                }
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (uiState.isSelectionMode) {
                            viewModel.clearSelection()
                        } else if (uiState.selectedAlbum != "All") {
                            viewModel.selectAlbum("All")
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (uiState.isSelectionMode) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (uiState.isSelectionMode) "Clear selection" else "Back",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (uiState.isSelectionMode) "${uiState.selectedCount} Selected" else "Gallery",
                        style = NovaTheme.typography.display,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.isSelectionMode) {
                    IconButton(onClick = { viewModel.selectAll() }) {
                        Icon(
                            imageVector = Icons.Rounded.SelectAll,
                            contentDescription = "Select All",
                            tint = NovaTheme.colors.primary
                        )
                    }
                }
            }

            // Album Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.availableAlbums) { album ->
                    val isSelected = album == uiState.selectedAlbum
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.cardBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .combinedClickable(
                                onClick = { viewModel.selectAlbum(album) }
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = album,
                            color = if (isSelected) Color.White else NovaTheme.colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid of Images (3 columns)
            if (uiState.filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isLoading) "Scanning photos..." else "No photos found",
                        color = NovaTheme.colors.textSecondary,
                        style = NovaTheme.typography.body
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.filteredItems) { index, item ->
                        val isSelected = uiState.selectedItemIds.contains(item.id)
                        GalleryGridItem(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = uiState.isSelectionMode,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleItemSelection(item.id)
                                } else {
                                    viewModel.openViewer(index)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleItemSelection(item.id)
                            }
                        )
                    }
                }
            }
        }

        // Contextual Bottom Dock (when in selection mode)
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            NovaCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = NovaTheme.colors.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockButton(
                        icon = Icons.Rounded.ContentCopy,
                        label = "Copy",
                        onClick = { viewModel.showCopyPicker(true) }
                    )
                    DockButton(
                        icon = Icons.Rounded.DriveFileMove,
                        label = "Move",
                        onClick = { viewModel.showMovePicker(true) }
                    )
                    DockButton(
                        icon = Icons.Rounded.Delete,
                        label = "Delete",
                        tint = NovaTheme.colors.error,
                        onClick = { viewModel.showDeleteConfirm(true) }
                    )
                    DockButton(
                        icon = Icons.Rounded.Share,
                        label = "Share",
                        onClick = { viewModel.shareSelected(context) }
                    )
                }
            }
        }

        // Delete Confirm Dialog
        if (uiState.showDeleteConfirmDialog) {
            NovaConfirmDialog(
                title = "Delete ${uiState.selectedCount} items?",
                message = "Are you sure you want to permanently delete the selected images?",
                confirmText = "Delete",
                isDestructive = true,
                onConfirm = { viewModel.deleteSelected() },
                onDismiss = { viewModel.showDeleteConfirm(false) }
            )
        }

        // Copy Folder Picker Bottom Sheet
        if (uiState.showCopyPickerDialog) {
            FolderPickerBottomSheet(
                title = "Copy ${uiState.selectedCount} items to",
                onSelectFolder = { targetFolder ->
                    viewModel.copySelected(targetFolder)
                },
                onDismiss = { viewModel.showCopyPicker(false) }
            )
        }

        // Move Folder Picker Bottom Sheet
        if (uiState.showMovePickerDialog) {
            FolderPickerBottomSheet(
                title = "Move ${uiState.selectedCount} items to",
                onSelectFolder = { targetFolder ->
                    viewModel.moveSelected(targetFolder)
                },
                onDismiss = { viewModel.showMovePicker(false) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryGridItem(
    item: GalleryItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(NovaTheme.colors.surfaceElevated)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) NovaTheme.colors.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.file)
                .size(360)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Checkbox Overlay
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) NovaTheme.colors.primary else Color(0x66000000))
                    .border(1.5.dp, if (isSelected) NovaTheme.colors.primary else Color.White, CircleShape)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = NovaTheme.colors.textPrimary,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FolderPickerBottomSheet(
    title: String,
    onSelectFolder: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val commonFolders = listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    ).filterNotNull()

    NovaBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            commonFolders.forEach { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NovaTheme.colors.surfaceElevated)
                        .border(1.dp, NovaTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                        .combinedClickable { onSelectFolder(folder) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = NovaTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = folder.name,
                            style = NovaTheme.typography.title,
                            color = NovaTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = folder.absolutePath,
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
