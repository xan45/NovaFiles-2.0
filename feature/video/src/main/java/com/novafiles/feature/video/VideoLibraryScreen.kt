@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.video

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.Surface
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.media.VideoMetadataHelper
import com.novafiles.core.media.VideoTechnicalDetails
import com.novafiles.core.model.SortDirection
import com.novafiles.feature.files.MultiSelectionBottomDock
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaConfirmDialog
import com.novafiles.core.ui.components.NovaInputDialog
import java.io.File

@Composable
fun VideoLibraryScreen(
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onPlayQueue: (List<String>, Int) -> Unit = { queue, idx -> if (queue.isNotEmpty()) onPlayVideo(queue[idx]) },
    modifier: Modifier = Modifier,
    viewModel: VideoLibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Show status message toast
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    // System Back Handler
    BackHandler {
        when {
            uiState.isSearchOpen -> viewModel.toggleSearch(false)
            uiState.isSelectionMode -> viewModel.clearSelection()
            uiState.selectedFolder != null -> viewModel.selectFolder(null)
            uiState.selectedPlaylist != null -> viewModel.selectPlaylist(null)
            else -> onNavigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NovaTheme.colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            VideoLibraryTopBar(
                uiState = uiState,
                onNavigateBack = {
                    when {
                        uiState.isSearchOpen -> viewModel.toggleSearch(false)
                        uiState.isSelectionMode -> viewModel.clearSelection()
                        uiState.selectedFolder != null -> viewModel.selectFolder(null)
                        uiState.selectedPlaylist != null -> viewModel.selectPlaylist(null)
                        else -> onNavigateBack()
                    }
                },
                onSearchToggle = { viewModel.toggleSearch(it) },
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onOpenSort = { viewModel.showSortSheet(true) },
                onOpenSettings = { viewModel.showSettingsSheet(true) },
                onSelectAll = { viewModel.selectAll() },
                onClearSelection = { viewModel.clearSelection() }
            )

            // Primary Navigation Tabs (All Videos | Folders | Playlists)
            if (uiState.selectedFolder == null && uiState.selectedPlaylist == null) {
                VideoPrimarySectionTabs(
                    currentSection = uiState.currentSection,
                    onSectionSelected = { viewModel.selectSection(it) }
                )
            } else {
                // Breadcrumb Bar for active folder / playlist
                val breadcrumbTitle = uiState.selectedFolder?.name ?: uiState.selectedPlaylist?.name ?: ""
                val countText = "${uiState.activeVideoList.size} items"
                VideoBreadcrumbBar(
                    title = breadcrumbTitle,
                    countText = countText,
                    onBack = {
                        if (uiState.selectedFolder != null) viewModel.selectFolder(null)
                        else viewModel.selectPlaylist(null)
                    }
                )
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NovaTheme.colors.primary)
                    }
                } else when {
                    uiState.selectedFolder != null || uiState.selectedPlaylist != null || uiState.currentSection == VideoSection.ALL_VIDEOS -> {
                        AllVideosContentView(
                            uiState = uiState,
                            onVideoClick = { video ->
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleSelection(video.id)
                                } else {
                                    val activeList = uiState.activeVideoList.map { it.path }
                                    val idx = activeList.indexOf(video.path).coerceAtLeast(0)
                                    onPlayQueue(activeList, idx)
                                }
                            },
                            onVideoLongClick = { video ->
                                viewModel.toggleSelection(video.id)
                            },
                            onDetailsClick = { viewModel.showVideoDetails(it) },
                            onRenameClick = { viewModel.showRenameVideoDialog(it) },
                            onDeleteClick = { viewModel.deleteSingleVideo(it) },
                            onShareClick = {
                                viewModel.toggleSelection(it.id)
                                viewModel.shareSelected(context)
                                viewModel.clearSelection()
                            },
                            onAddToPlaylistClick = {
                                viewModel.toggleSelection(it.id)
                                viewModel.showAddToPlaylistDialog(true)
                            }
                        )
                    }
                    uiState.currentSection == VideoSection.FOLDERS -> {
                        VideoFoldersContentView(
                            folders = uiState.folders,
                            onFolderClick = { viewModel.selectFolder(it) }
                        )
                    }
                    uiState.currentSection == VideoSection.PLAYLISTS -> {
                        VideoPlaylistsContentView(
                            playlists = uiState.playlists,
                            allVideos = uiState.allVideos,
                            onCreatePlaylistClick = { viewModel.showCreatePlaylistDialog(true) },
                            onPlaylistClick = { viewModel.selectPlaylist(it) },
                            onPlayPlaylist = { playlist ->
                                if (playlist.videoPaths.isNotEmpty()) {
                                    onPlayQueue(playlist.videoPaths, 0)
                                } else {
                                    Toast.makeText(context, "Playlist is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRenamePlaylist = { viewModel.showRenamePlaylistDialog(it) },
                            onDeletePlaylist = { viewModel.showDeletePlaylistDialog(it) }
                        )
                    }
                }
            }
        }

        // Multi-Selection Bottom Dock
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            MultiSelectionBottomDock(
                onCopy = {},
                onMove = {},
                onDelete = { viewModel.showDeleteConfirmDialog(true) },
                onShare = { viewModel.shareSelected(context) },
                onCompress = { viewModel.showAddToPlaylistDialog(true) },
                onMore = { viewModel.selectAll() }
            )
        }

        // Dedicated Video Settings Bottom Sheet
        if (uiState.showSettingsSheet) {
            VideoSettingsBottomSheet(
                preferences = uiState.preferences,
                onViewModeChange = { viewModel.setViewMode(it) },
                onThumbnailSizeChange = { viewModel.setThumbnailSize(it) },
                onGridSpacingChange = { viewModel.setGridSpacing(it) },
                onShowDurationOverlayChange = { viewModel.setShowDurationOverlay(it) },
                onGroupByDateChange = { viewModel.setGroupByDate(it) },
                onRememberPlaybackPositionChange = { viewModel.setRememberPlaybackPosition(it) },
                onDefaultSubtitleChange = { viewModel.setDefaultSubtitleEnabled(it) },
                onDefaultPlaybackSpeedChange = { viewModel.setDefaultPlaybackSpeed(it) },
                onOrientationPreferenceChange = { viewModel.setOrientationPreference(it) },
                onRefreshMedia = { viewModel.loadVideos() },
                onDismiss = { viewModel.showSettingsSheet(false) }
            )
        }

        // Video Sort Bottom Sheet
        if (uiState.showSortSheet) {
            VideoSortBottomSheet(
                sortConfig = uiState.preferences.sortConfig,
                groupByDate = uiState.preferences.groupByDate,
                onSortChange = { viewModel.setSortConfig(it) },
                onToggleGroupByDate = { viewModel.setGroupByDate(it) },
                onDismiss = { viewModel.showSortSheet(false) }
            )
        }

        // Video Details Bottom Sheet
        if (uiState.activeVideoDetails != null) {
            val video = uiState.activeVideoDetails!!
            VideoDetailsBottomSheet(
                video = video,
                details = uiState.activeTechnicalDetails,
                onDismiss = { viewModel.showVideoDetails(null) }
            )
        }

        // Create Playlist Dialog
        if (uiState.showCreatePlaylistDialog) {
            NovaInputDialog(
                title = "New Playlist",
                initialValue = "Playlist 1",
                placeholder = "Playlist name",
                confirmText = "Create",
                cancelText = "Cancel",
                onConfirm = { name -> viewModel.createPlaylist(name) },
                onDismiss = { viewModel.showCreatePlaylistDialog(false) }
            )
        }

        // Rename Playlist Dialog
        if (uiState.showRenamePlaylistDialog != null) {
            val playlist = uiState.showRenamePlaylistDialog!!
            NovaInputDialog(
                title = "Rename Playlist",
                initialValue = playlist.name,
                placeholder = "New playlist name",
                confirmText = "Rename",
                cancelText = "Cancel",
                onConfirm = { newName -> viewModel.renamePlaylist(playlist.id, newName) },
                onDismiss = { viewModel.showRenamePlaylistDialog(null) }
            )
        }

        // Delete Playlist Dialog
        if (uiState.showDeletePlaylistDialog != null) {
            val playlist = uiState.showDeletePlaylistDialog!!
            NovaConfirmDialog(
                title = "Delete Playlist?",
                message = "Are you sure you want to delete playlist \"${playlist.name}\"? Videos will NOT be deleted from disk.",
                confirmText = "Delete",
                cancelText = "Cancel",
                onConfirm = { viewModel.deletePlaylist(playlist.id) },
                onDismiss = { viewModel.showDeletePlaylistDialog(null) }
            )
        }

        // Add to Playlist Selection Sheet
        if (uiState.showAddToPlaylistDialog) {
            AddToPlaylistSheet(
                playlists = uiState.playlists,
                onPlaylistSelected = { viewModel.addSelectedToPlaylist(it.id) },
                onCreateNewPlaylist = {
                    viewModel.showAddToPlaylistDialog(false)
                    viewModel.showCreatePlaylistDialog(true)
                },
                onDismiss = { viewModel.showAddToPlaylistDialog(false) }
            )
        }

        // Rename Video Dialog
        if (uiState.showRenameVideoDialog != null) {
            val video = uiState.showRenameVideoDialog!!
            NovaInputDialog(
                title = "Rename Video",
                initialValue = video.name,
                placeholder = "Video name",
                confirmText = "Rename",
                cancelText = "Cancel",
                onConfirm = { newName -> viewModel.renameVideo(video, newName) },
                onDismiss = { viewModel.showRenameVideoDialog(null) }
            )
        }

        // Delete Video Confirmation Dialog
        if (uiState.showDeleteConfirmDialog) {
            NovaConfirmDialog(
                title = "Delete Selected Videos?",
                message = "Permanently delete ${uiState.selectedCount} video(s) from storage? This action cannot be undone.",
                confirmText = "Delete Permanently",
                cancelText = "Cancel",
                onConfirm = { viewModel.deleteSelected() },
                onDismiss = { viewModel.showDeleteConfirmDialog(false) }
            )
        }
    }
}

// ==========================================
// Top Bar & Navigation
// ==========================================

@Composable
private fun VideoLibraryTopBar(
    uiState: VideoLibraryUiState,
    onNavigateBack: () -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSort: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    Surface(
        color = NovaTheme.colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = NovaTheme.colors.textPrimary
                    )
                }

                if (uiState.isSearchOpen) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search videos...", color = NovaTheme.colors.textTertiary) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NovaTheme.colors.textPrimary,
                            unfocusedTextColor = NovaTheme.colors.textPrimary,
                            focusedBorderColor = NovaTheme.colors.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        trailingIcon = {
                            IconButton(onClick = { onSearchToggle(false) }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search", tint = NovaTheme.colors.textSecondary)
                            }
                        }
                    )
                } else if (uiState.isSelectionMode) {
                    Text(
                        text = "${uiState.selectedCount} selected",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                    IconButton(onClick = onSelectAll) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Select All", tint = NovaTheme.colors.primary)
                    }
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = NovaTheme.colors.textSecondary)
                    }
                } else {
                    Text(
                        text = "Videos",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )

                    IconButton(onClick = { onSearchToggle(true) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = NovaTheme.colors.textSecondary)
                    }
                    IconButton(onClick = onOpenSort) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort Options", tint = NovaTheme.colors.textSecondary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Video Settings", tint = NovaTheme.colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPrimarySectionTabs(
    currentSection: VideoSection,
    onSectionSelected: (VideoSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VideoSection.values().forEach { section ->
            val isSelected = currentSection == section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(NovaTheme.shapes.medium)
                    .background(if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated)
                    .clickable { onSectionSelected(section) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = section.title,
                    style = NovaTheme.typography.body,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else NovaTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun VideoBreadcrumbBar(
    title: String,
    countText: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onBack() }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = NovaTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = countText,
            style = NovaTheme.typography.caption,
            color = NovaTheme.colors.textTertiary
        )
    }
}

// ==========================================
// All Videos Views (Grid & List)
// ==========================================

@Composable
private fun AllVideosContentView(
    uiState: VideoLibraryUiState,
    onVideoClick: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit,
    onDetailsClick: (VideoItem) -> Unit,
    onRenameClick: (VideoItem) -> Unit,
    onDeleteClick: (VideoItem) -> Unit,
    onShareClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit
) {
    val videos = uiState.activeVideoList
    if (videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = NovaTheme.colors.textTertiary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (uiState.searchQuery.isNotBlank()) "No matching videos" else "No videos found",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary
                )
            }
        }
        return
    }

    if (uiState.preferences.viewMode == VideoViewMode.LIST) {
        VideoListView(
            uiState = uiState,
            onVideoClick = onVideoClick,
            onVideoLongClick = onVideoLongClick,
            onDetailsClick = onDetailsClick,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
            onShareClick = onShareClick,
            onAddToPlaylistClick = onAddToPlaylistClick
        )
    } else {
        VideoGridView(
            uiState = uiState,
            onVideoClick = onVideoClick,
            onVideoLongClick = onVideoLongClick,
            onDetailsClick = onDetailsClick,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
            onShareClick = onShareClick,
            onAddToPlaylistClick = onAddToPlaylistClick
        )
    }
}

@Composable
private fun VideoGridView(
    uiState: VideoLibraryUiState,
    onVideoClick: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit,
    onDetailsClick: (VideoItem) -> Unit,
    onRenameClick: (VideoItem) -> Unit,
    onDeleteClick: (VideoItem) -> Unit,
    onShareClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit
) {
    val columns = uiState.preferences.thumbnailSize.columns
    val spacing = uiState.preferences.gridSpacingDp.dp

    if (uiState.preferences.groupByDate && uiState.selectedFolder == null && uiState.selectedPlaylist == null) {
        val groups = uiState.dateGroups
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxSize()
        ) {
            groups.forEach { group ->
                item(span = { GridItemSpan(columns) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.title,
                            style = NovaTheme.typography.title,
                            color = NovaTheme.colors.textPrimary
                        )
                        Text(
                            text = "${group.items.size} videos",
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textTertiary
                        )
                    }
                }
                items(group.items, key = { it.id }) { video ->
                    VideoGridCard(
                        video = video,
                        isSelected = video.id in uiState.selectedVideoIds,
                        showDuration = uiState.preferences.showDurationOverlay,
                        resumePoint = uiState.resumePoints[video.path],
                        onClick = { onVideoClick(video) },
                        onLongClick = { onVideoLongClick(video) },
                        onDetailsClick = { onDetailsClick(video) },
                        onRenameClick = { onRenameClick(video) },
                        onDeleteClick = { onDeleteClick(video) },
                        onShareClick = { onShareClick(video) },
                        onAddToPlaylistClick = { onAddToPlaylistClick(video) }
                    )
                }
            }
        }
    } else {
        val videos = uiState.activeVideoList
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxSize()
        ) {
            items(videos, key = { it.id }) { video ->
                VideoGridCard(
                    video = video,
                    isSelected = video.id in uiState.selectedVideoIds,
                    showDuration = uiState.preferences.showDurationOverlay,
                    resumePoint = uiState.resumePoints[video.path],
                    onClick = { onVideoClick(video) },
                    onLongClick = { onVideoLongClick(video) },
                    onDetailsClick = { onDetailsClick(video) },
                    onRenameClick = { onRenameClick(video) },
                    onDeleteClick = { onDeleteClick(video) },
                    onShareClick = { onShareClick(video) },
                    onAddToPlaylistClick = { onAddToPlaylistClick(video) }
                )
            }
        }
    }
}

@Composable
private fun VideoGridCard(
    video: VideoItem,
    isSelected: Boolean,
    showDuration: Boolean,
    resumePoint: PlaybackResumePoint?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var thumbFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(video.path) {
        thumbFile = VideoMetadataHelper.getVideoThumbnail(context, video.file)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NovaTheme.shapes.medium)
            .background(NovaTheme.colors.surfaceElevated)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.cardBorder,
                shape = NovaTheme.shapes.medium
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF1E293B))
            ) {
                if (thumbFile != null && thumbFile!!.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Duration Overlay badge
                if (showDuration && video.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Resolution badge
                if (video.resolutionLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(NovaTheme.colors.primary.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.resolutionLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Resume playback indicator bar
                if (resumePoint != null && resumePoint.durationMs > 0) {
                    val progress = (resumePoint.positionMs.toFloat() / resumePoint.durationMs.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(3.dp)
                                .background(NovaTheme.colors.primary)
                        )
                    }
                }

                // Selection check indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(NovaTheme.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Info row below thumbnail
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.name,
                        style = NovaTheme.typography.body,
                        fontWeight = FontWeight.SemiBold,
                        color = NovaTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${video.formattedSize} • ${FileFormatters.formatDate(video.dateModified)}",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = NovaTheme.colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Details") },
                            onClick = {
                                menuExpanded = false
                                onDetailsClick()
                            },
                            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            onClick = {
                                menuExpanded = false
                                onAddToPlaylistClick()
                            },
                            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                menuExpanded = false
                                onShareClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                menuExpanded = false
                                onRenameClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = NovaTheme.colors.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = NovaTheme.colors.error) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoListView(
    uiState: VideoLibraryUiState,
    onVideoClick: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit,
    onDetailsClick: (VideoItem) -> Unit,
    onRenameClick: (VideoItem) -> Unit,
    onDeleteClick: (VideoItem) -> Unit,
    onShareClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit
) {
    val videos = uiState.activeVideoList
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(videos, key = { it.id }) { video ->
            VideoListRow(
                video = video,
                isSelected = video.id in uiState.selectedVideoIds,
                showDuration = uiState.preferences.showDurationOverlay,
                resumePoint = uiState.resumePoints[video.path],
                onClick = { onVideoClick(video) },
                onLongClick = { onVideoLongClick(video) },
                onDetailsClick = { onDetailsClick(video) },
                onRenameClick = { onRenameClick(video) },
                onDeleteClick = { onDeleteClick(video) },
                onShareClick = { onShareClick(video) },
                onAddToPlaylistClick = { onAddToPlaylistClick(video) }
            )
        }
    }
}

@Composable
private fun VideoListRow(
    video: VideoItem,
    isSelected: Boolean,
    showDuration: Boolean,
    resumePoint: PlaybackResumePoint?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var thumbFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(video.path) {
        thumbFile = VideoMetadataHelper.getVideoThumbnail(context, video.file)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NovaTheme.shapes.medium)
            .background(NovaTheme.colors.surfaceElevated)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.cardBorder,
                shape = NovaTheme.shapes.medium
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 65.dp)
                .clip(NovaTheme.shapes.small)
                .background(Color(0xFF1E293B))
        ) {
            if (thumbFile != null && thumbFile!!.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (showDuration && video.durationMs > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Progress bar
            if (resumePoint != null && resumePoint.durationMs > 0) {
                val progress = (resumePoint.positionMs.toFloat() / resumePoint.durationMs.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(NovaTheme.colors.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.name,
                style = NovaTheme.typography.body,
                fontWeight = FontWeight.SemiBold,
                color = NovaTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (video.resolutionLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(NovaTheme.colors.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = video.resolutionLabel,
                            color = NovaTheme.colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = "${video.formattedSize} • ${FileFormatters.formatDate(video.dateModified)}",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Selection or Options
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = NovaTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = NovaTheme.colors.textTertiary
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Details") },
                        onClick = {
                            menuExpanded = false
                            onDetailsClick()
                        },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylistClick()
                        },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            menuExpanded = false
                            onShareClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuExpanded = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = NovaTheme.colors.error) },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = NovaTheme.colors.error) }
                    )
                }
            }
        }
    }
}

// ==========================================
// Folders Content View
// ==========================================

@Composable
private fun VideoFoldersContentView(
    folders: List<VideoFolderItem>,
    onFolderClick: (VideoFolderItem) -> Unit
) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = NovaTheme.colors.textTertiary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No video folders found", style = NovaTheme.typography.body, color = NovaTheme.colors.textSecondary)
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders, key = { it.path }) { folder ->
            val context = LocalContext.current
            var thumbFile by remember { mutableStateOf<File?>(null) }

            LaunchedEffect(folder.path) {
                folder.coverItem?.let {
                    thumbFile = VideoMetadataHelper.getVideoThumbnail(context, it.file)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NovaTheme.shapes.large)
                    .background(NovaTheme.colors.surfaceElevated)
                    .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.large)
                    .clickable { onFolderClick(folder) }
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .background(Color(0xFF1E293B))
                    ) {
                        if (thumbFile != null && thumbFile!!.exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(thumbFile)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = folder.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = NovaTheme.colors.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        // Badge with video count
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🎬 ${folder.videoCount}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = folder.name,
                            style = NovaTheme.typography.body,
                            fontWeight = FontWeight.Bold,
                            color = NovaTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${folder.formattedDuration} • ${folder.formattedSize}",
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// Playlists Content View
// ==========================================

@Composable
private fun VideoPlaylistsContentView(
    playlists: List<VideoPlaylist>,
    allVideos: List<VideoItem>,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (VideoPlaylist) -> Unit,
    onPlayPlaylist: (VideoPlaylist) -> Unit,
    onRenamePlaylist: (VideoPlaylist) -> Unit,
    onDeletePlaylist: (VideoPlaylist) -> Unit
) {
    val context = LocalContext.current
    val videoMap = remember(allVideos) { allVideos.associateBy { it.path } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Create Playlist Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 13f)
                    .clip(NovaTheme.shapes.large)
                    .background(NovaTheme.colors.surfaceElevated.copy(alpha = 0.6f))
                    .border(1.dp, NovaTheme.colors.primary.copy(alpha = 0.4f), NovaTheme.shapes.large)
                    .clickable { onCreatePlaylistClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NovaTheme.colors.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New Playlist",
                            tint = NovaTheme.colors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "New Playlist",
                        style = NovaTheme.typography.body,
                        fontWeight = FontWeight.Bold,
                        color = NovaTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Group videos",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textTertiary
                    )
                }
            }
        }

        items(playlists, key = { it.id }) { playlist ->
            var menuExpanded by remember { mutableStateOf(false) }
            val firstVideo = playlist.videoPaths.firstOrNull()?.let { videoMap[it] }
            var thumbFile by remember { mutableStateOf<File?>(null) }

            LaunchedEffect(firstVideo?.path) {
                firstVideo?.let {
                    thumbFile = VideoMetadataHelper.getVideoThumbnail(context, it.file)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NovaTheme.shapes.large)
                    .background(NovaTheme.colors.surfaceElevated)
                    .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.large)
                    .clickable { onPlaylistClick(playlist) }
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .background(Color(0xFF1E293B))
                    ) {
                        if (thumbFile != null && thumbFile!!.exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(thumbFile)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = playlist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = NovaTheme.colors.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        // Play All floating button on thumbnail
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NovaTheme.colors.primary)
                                .clickable { onPlayPlaylist(playlist) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play All",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Count badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${playlist.videoPaths.size} videos",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = NovaTheme.typography.body,
                                fontWeight = FontWeight.Bold,
                                color = NovaTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${playlist.videoPaths.size} items",
                                style = NovaTheme.typography.caption,
                                color = NovaTheme.colors.textSecondary
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = NovaTheme.colors.textTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Play All") },
                                    onClick = {
                                        menuExpanded = false
                                        onPlayPlaylist(playlist)
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        menuExpanded = false
                                        onRenamePlaylist(playlist)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = NovaTheme.colors.error) },
                                    onClick = {
                                        menuExpanded = false
                                        onDeletePlaylist(playlist)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = NovaTheme.colors.error) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Settings Bottom Sheet
// ==========================================

@Composable
private fun VideoSettingsBottomSheet(
    preferences: VideoPreferences,
    onViewModeChange: (VideoViewMode) -> Unit,
    onThumbnailSizeChange: (VideoThumbnailSize) -> Unit,
    onGridSpacingChange: (Int) -> Unit,
    onShowDurationOverlayChange: (Boolean) -> Unit,
    onGroupByDateChange: (Boolean) -> Unit,
    onRememberPlaybackPositionChange: (Boolean) -> Unit,
    onDefaultSubtitleChange: (Boolean) -> Unit,
    onDefaultPlaybackSpeedChange: (Float) -> Unit,
    onOrientationPreferenceChange: (VideoOrientationPref) -> Unit,
    onRefreshMedia: () -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Video Settings",
                        style = NovaTheme.typography.title,
                        fontWeight = FontWeight.Bold,
                        color = NovaTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure layout, organization, and playback preferences",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary
                    )
                }
            }

            // Appearance Section
            item {
                Text(
                    text = "APPEARANCE",
                    style = NovaTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = NovaTheme.colors.primary
                )
            }

            item {
                Column {
                    Text("Browsing Layout", style = NovaTheme.typography.body, color = NovaTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VideoViewMode.values().forEach { mode ->
                            val isSelected = preferences.viewMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(NovaTheme.shapes.medium)
                                    .background(if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated)
                                    .clickable { onViewModeChange(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.title,
                                    style = NovaTheme.typography.body,
                                    color = if (isSelected) Color.White else NovaTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            if (preferences.viewMode == VideoViewMode.GRID) {
                item {
                    Column {
                        Text("Thumbnail Density", style = NovaTheme.typography.body, color = NovaTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoThumbnailSize.values().forEach { size ->
                                val isSelected = preferences.thumbnailSize == size
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(NovaTheme.shapes.small)
                                        .background(if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated)
                                    .clickable { onThumbnailSizeChange(size) }
                                    .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = size.title,
                                        style = NovaTheme.typography.caption,
                                        color = if (isSelected) Color.White else NovaTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Grid Spacing", style = NovaTheme.typography.body, color = NovaTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2 to "Compact", 6 to "Normal", 12 to "Relaxed").forEach { (dpVal, label) ->
                                val isSelected = preferences.gridSpacingDp == dpVal
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(NovaTheme.shapes.small)
                                        .background(if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated)
                                        .clickable { onGridSpacingChange(dpVal) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = NovaTheme.typography.caption,
                                        color = if (isSelected) Color.White else NovaTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Duration Overlay", style = NovaTheme.typography.body, color = NovaTheme.colors.textPrimary)
                        Text("Show video duration badge on thumbnail", style = NovaTheme.typography.caption, color = NovaTheme.colors.textTertiary)
                    }
                    Switch(
                        checked = preferences.showDurationOverlay,
                        onCheckedChange = onShowDurationOverlayChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaTheme.colors.primary)
                    )
                }
            }

            // Organization Section
            item {
                Text(
                    text = "ORGANIZATION",
                    style = NovaTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = NovaTheme.colors.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Group Videos by Date", style = NovaTheme.typography.body, color = NovaTheme.colors.textPrimary)
                        Text("Display Today, Yesterday, and Month headers", style = NovaTheme.typography.caption, color = NovaTheme.colors.textTertiary)
                    }
                    Switch(
                        checked = preferences.groupByDate,
                        onCheckedChange = onGroupByDateChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaTheme.colors.primary)
                    )
                }
            }

            // Playback Section
            item {
                Text(
                    text = "PLAYBACK",
                    style = NovaTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = NovaTheme.colors.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Remember Playback Position", style = NovaTheme.typography.body, color = NovaTheme.colors.textPrimary)
                        Text("Resume playing videos from where you left off", style = NovaTheme.typography.caption, color = NovaTheme.colors.textTertiary)
                    }
                    Switch(
                        checked = preferences.rememberPlaybackPosition,
                        onCheckedChange = onRememberPlaybackPositionChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaTheme.colors.primary)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default Subtitles", style = NovaTheme.typography.body, color = NovaTheme.colors.textPrimary)
                        Text("Auto-enable subtitle tracks if available", style = NovaTheme.typography.caption, color = NovaTheme.colors.textTertiary)
                    }
                    Switch(
                        checked = preferences.defaultSubtitleEnabled,
                        onCheckedChange = onDefaultSubtitleChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaTheme.colors.primary)
                    )
                }
            }

            item {
                Column {
                    Text("Default Playback Speed", style = NovaTheme.typography.body, color = NovaTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            val isSelected = preferences.defaultPlaybackSpeed == speed
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(NovaTheme.shapes.small)
                                    .background(if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated)
                                    .clickable { onDefaultPlaybackSpeedChange(speed) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${speed}x",
                                    style = NovaTheme.typography.caption,
                                    color = if (isSelected) Color.White else NovaTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Orientation Preference", style = NovaTheme.typography.body, color = NovaTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoOrientationPref.values().forEach { pref ->
                            val isSelected = preferences.orientationPreference == pref
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(NovaTheme.shapes.small)
                                    .background(if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated)
                                    .clickable { onOrientationPreferenceChange(pref) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pref.label,
                                    style = NovaTheme.typography.caption,
                                    color = if (isSelected) Color.White else NovaTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            item {
                NovaButton(
                    text = "Refresh & Rescan Videos",
                    onClick = {
                        onRefreshMedia()
                        onDismiss()
                    },
                    variant = NovaButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// Sort Options Bottom Sheet
// ==========================================

@Composable
private fun VideoSortBottomSheet(
    sortConfig: VideoSortConfig,
    groupByDate: Boolean,
    onSortChange: (VideoSortConfig) -> Unit,
    onToggleGroupByDate: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Sort & View Options",
                style = NovaTheme.typography.title,
                fontWeight = FontWeight.Bold,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Sort By", style = NovaTheme.typography.body, fontWeight = FontWeight.SemiBold, color = NovaTheme.colors.textPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            VideoSortBy.values().forEach { sortBy ->
                val isSelected = sortConfig.sortBy == sortBy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSortChange(sortConfig.copy(sortBy = sortBy)) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSortChange(sortConfig.copy(sortBy = sortBy)) },
                        colors = RadioButtonDefaults.colors(selectedColor = NovaTheme.colors.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(sortBy.label, color = NovaTheme.colors.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = NovaTheme.colors.cardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Direction", style = NovaTheme.typography.body, fontWeight = FontWeight.SemiBold, color = NovaTheme.colors.textPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(NovaTheme.shapes.medium)
                        .background(if (sortConfig.direction == SortDirection.DESCENDING) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated)
                        .clickable { onSortChange(sortConfig.copy(direction = SortDirection.DESCENDING)) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Newest / Z-A (Desc)",
                        color = if (sortConfig.direction == SortDirection.DESCENDING) Color.White else NovaTheme.colors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(NovaTheme.shapes.medium)
                        .background(if (sortConfig.direction == SortDirection.ASCENDING) NovaTheme.colors.primary else NovaTheme.colors.surfaceElevated)
                        .clickable { onSortChange(sortConfig.copy(direction = SortDirection.ASCENDING)) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Oldest / A-Z (Asc)",
                        color = if (sortConfig.direction == SortDirection.ASCENDING) Color.White else NovaTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

// ==========================================
// Video Details Bottom Sheet
// ==========================================

@Composable
private fun VideoDetailsBottomSheet(
    video: VideoItem,
    details: VideoTechnicalDetails?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Video Details",
                style = NovaTheme.typography.title,
                fontWeight = FontWeight.Bold,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            MetadataItemRow(label = "Filename", value = video.name)
            MetadataItemRow(label = "Duration", value = details?.formattedDuration ?: video.formattedDuration)
            MetadataItemRow(label = "Resolution", value = details?.resolution ?: video.resolutionLabel.ifBlank { "Not available" })
            MetadataItemRow(label = "Container Format", value = details?.containerFormat ?: video.file.extension.uppercase())
            MetadataItemRow(label = "Video Codec", value = details?.videoCodec ?: details?.codec ?: "Standard Video")
            MetadataItemRow(label = "Audio Codec", value = details?.audioCodec ?: "Standard Audio")
            MetadataItemRow(label = "Frame Rate", value = details?.frameRate ?: "Not available")
            MetadataItemRow(label = "Bitrate", value = details?.videoBitrate ?: "Not available")
            MetadataItemRow(label = "File Size", value = "${video.formattedSize} (${video.sizeBytes} bytes)")
            MetadataItemRow(
                label = "Date Modified",
                value = details?.dateModifiedFormatted ?: FileFormatters.formatDate(video.dateModified)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = NovaTheme.colors.cardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Full Path", style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = video.path,
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Video Path", video.path))
                    Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy Path",
                        tint = NovaTheme.colors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = NovaTheme.typography.caption, color = NovaTheme.colors.textSecondary)
        Text(
            text = value,
            style = NovaTheme.typography.body,
            fontWeight = FontWeight.Medium,
            color = NovaTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==========================================
// Add to Playlist Selection Sheet
// ==========================================

@Composable
private fun AddToPlaylistSheet(
    playlists: List<VideoPlaylist>,
    onPlaylistSelected: (VideoPlaylist) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Add to Playlist",
                style = NovaTheme.typography.title,
                fontWeight = FontWeight.Bold,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Create New Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NovaTheme.shapes.medium)
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f))
                    .clickable { onCreateNewPlaylist() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = NovaTheme.colors.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Create New Playlist",
                    style = NovaTheme.typography.body,
                    fontWeight = FontWeight.Bold,
                    color = NovaTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (playlists.isEmpty()) {
                Text(
                    text = "No custom playlists created yet",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textTertiary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaylistSelected(playlist) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = NovaTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = playlist.name,
                                    style = NovaTheme.typography.body,
                                    color = NovaTheme.colors.textPrimary
                                )
                                Text(
                                    text = "${playlist.videoPaths.size} videos",
                                    style = NovaTheme.typography.caption,
                                    color = NovaTheme.colors.textTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
