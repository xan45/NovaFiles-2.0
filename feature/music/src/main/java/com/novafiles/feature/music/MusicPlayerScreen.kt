@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.music

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.media.MusicState
import com.novafiles.core.model.FileItem
import com.novafiles.core.storage.StoragePermissionHelper
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaEmptyState
import com.novafiles.core.ui.components.NovaErrorState
import com.novafiles.core.ui.components.NovaLoadingState
import java.io.File

@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    MusicPlayerScreen(
        uiState = uiState,
        onPlayTrack = { viewModel.playTrack(it) },
        onPlayTracks = { tracks, idx -> viewModel.playTracks(tracks, idx) },
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onSeekTo = { viewModel.seekTo(it) },
        onSkipNext = { viewModel.skipToNext() },
        onSkipPrevious = { viewModel.skipToPrevious() },
        onToggleRepeat = { viewModel.toggleRepeat() },
        onToggleShuffle = { viewModel.toggleShuffle() },
        onToggleFavorite = { viewModel.toggleFavorite() },
        onSelectTab = { viewModel.setLibraryTab(it) },
        onToggleQueue = { viewModel.toggleQueueSheet(it) },
        onRefresh = { viewModel.loadMusicLibrary(forceRefresh = true) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onSelectSubItem = { title, tracks -> viewModel.selectSubItem(title, tracks) },
        onDismissSubItem = { viewModel.dismissSubItem() },
        onExpandNowPlaying = { viewModel.expandNowPlaying() },
        onCollapseNowPlaying = { viewModel.collapseNowPlaying() },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
fun MusicPlayerScreen(
    uiState: MusicPlayerUiState,
    onPlayTrack: (FileItem) -> Unit,
    onPlayTracks: (List<MusicTrack>, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSelectTab: (MusicLibraryTab) -> Unit,
    onToggleQueue: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectSubItem: (String, List<MusicTrack>) -> Unit,
    onDismissSubItem: () -> Unit,
    onExpandNowPlaying: () -> Unit,
    onCollapseNowPlaying: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNowPlaying = uiState.isNowPlayingExpanded && uiState.playback.currentTrack != null

    BackHandler {
        when {
            uiState.selectedSubItemTitle != null -> onDismissSubItem()
            uiState.isQueueSheetVisible -> onToggleQueue(false)
            isNowPlaying -> onCollapseNowPlaying()
            uiState.searchQuery.isNotBlank() -> onSearchQueryChange("")
            else -> onNavigateBack()
        }
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
            // Header: "Now Playing" or "Music Library"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = if (isNowPlaying) onCollapseNowPlaying else onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NovaTheme.colors.textPrimary
                    )
                }

                Text(
                    text = if (isNowPlaying) "Now Playing" else "Music Library",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary
                )

                Row {
                    if (!isNowPlaying) {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = NovaTheme.colors.textPrimary
                            )
                        }
                    }
                    IconButton(onClick = { onToggleQueue(true) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Playback Queue",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }
                }
            }

            if (isNowPlaying) {
                // Screen: Full Music Player UI
                FullMusicPlayerContent(
                    playback = uiState.playback,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onToggleRepeat = onToggleRepeat,
                    onToggleShuffle = onToggleShuffle,
                    onToggleFavorite = onToggleFavorite,
                    onOpenQueue = { onToggleQueue(true) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Screen: Music Library UI
                MusicLibraryContent(
                    uiState = uiState,
                    onPlayTrack = onPlayTrack,
                    onPlayTracks = onPlayTracks,
                    onSelectTab = onSelectTab,
                    onRefresh = onRefresh,
                    onSearchQueryChange = onSearchQueryChange,
                    onSelectSubItem = onSelectSubItem,
                    modifier = Modifier.weight(1f)
                )

                // Mini-player at the bottom of Music Library if track is active
                uiState.playback.currentTrack?.let { currentTrack ->
                    MusicMiniPlayerBar(
                        track = currentTrack,
                        isPlaying = uiState.playback.isPlaying,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onClick = onExpandNowPlaying
                    )
                }
            }
        }

        // Sub-item (Album/Artist/Folder) drill-down sheet
        if (uiState.selectedSubItemTitle != null && uiState.selectedSubItemTracks != null) {
            val subTracks = uiState.selectedSubItemTracks
            NovaBottomSheet(onDismissRequest = onDismissSubItem) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.selectedSubItemTitle,
                                style = NovaTheme.typography.title,
                                color = NovaTheme.colors.textPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${subTracks.size} ${if (subTracks.size == 1) "track" else "tracks"}",
                                style = NovaTheme.typography.caption,
                                color = NovaTheme.colors.textSecondary
                            )
                        }
                        NovaButton(
                            text = "Play All",
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                onPlayTracks(subTracks, 0)
                                onDismissSubItem()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(modifier = Modifier.height(340.dp)) {
                        itemsIndexed(subTracks) { idx, track ->
                            val isCurrent = uiState.playback.currentFilePath == track.fileItem.path
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPlayTracks(subTracks, idx)
                                        onDismissSubItem()
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    style = NovaTheme.typography.captionMedium,
                                    color = if (isCurrent) NovaTheme.colors.primary else NovaTheme.colors.textSecondary,
                                    modifier = Modifier.width(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = NovaTheme.typography.subtitle,
                                        color = if (isCurrent) NovaTheme.colors.primary else NovaTheme.colors.textPrimary,
                                        maxLines = 1
                                    )
                                    val durationText = if (track.durationMs > 0) FileFormatters.formatDuration(track.durationMs) else FileFormatters.formatBytes(track.fileItem.sizeBytes)
                                    Text(
                                        text = "${track.artist} • ${track.format} • $durationText",
                                        style = NovaTheme.typography.caption,
                                        color = NovaTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Queue Bottom Sheet
        if (uiState.isQueueSheetVisible) {
            NovaBottomSheet(onDismissRequest = { onToggleQueue(false) }) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Current Queue (${uiState.playback.queue.size} tracks)",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        itemsIndexed(uiState.playback.queue) { index, file ->
                            val isCurrent = index == uiState.playback.currentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPlayTrack(
                                            FileItem(
                                                id = file.absolutePath,
                                                name = file.name,
                                                path = file.absolutePath
                                            )
                                        )
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = NovaTheme.typography.captionMedium,
                                    color = if (isCurrent) NovaTheme.colors.primary else NovaTheme.colors.textSecondary,
                                    modifier = Modifier.width(32.dp)
                                )
                                Text(
                                    text = file.nameWithoutExtension,
                                    style = NovaTheme.typography.subtitle,
                                    color = if (isCurrent) NovaTheme.colors.primary else NovaTheme.colors.textPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullMusicPlayerContent(
    playback: MusicState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playback.currentTrack

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideOrLandscape = maxWidth > 600.dp || maxWidth > maxHeight
        if (isWideOrLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .aspectRatio(1f)
                        .shadow(NovaTheme.elevation.level4, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(NovaTheme.colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    val artFile = track?.albumArtFile
                    if (artFile != null && artFile.exists()) {
                        AsyncImage(
                            model = artFile,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = track?.avatarLetter ?: "♪",
                            style = NovaTheme.typography.display,
                            color = Color.White
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track?.title ?: "No Track Playing",
                                style = NovaTheme.typography.title,
                                color = NovaTheme.colors.textPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = track?.artist ?: "Unknown Artist",
                                style = NovaTheme.typography.subtitle,
                                color = NovaTheme.colors.textSecondary,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (playback.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (playback.isFavorite) NovaTheme.colors.categoryVideos else NovaTheme.colors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = playback.progressFraction,
                        onValueChange = { frac -> onSeekTo((frac * playback.durationMs).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = NovaTheme.colors.primary,
                            activeTrackColor = NovaTheme.colors.primary,
                            inactiveTrackColor = NovaTheme.colors.cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (playback.isShuffleEnabled) NovaTheme.colors.primary else NovaTheme.colors.textSecondary)
                        }
                        IconButton(onClick = onSkipPrevious) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = NovaTheme.colors.textPrimary)
                        }
                        IconButton(onClick = onTogglePlayPause) {
                            Icon(if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = NovaTheme.colors.primary, modifier = Modifier.size(40.dp))
                        }
                        IconButton(onClick = onSkipNext) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = NovaTheme.colors.textPrimary)
                        }
                        IconButton(onClick = onToggleRepeat) {
                            Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = if (playback.repeatMode != Player.REPEAT_MODE_OFF) NovaTheme.colors.primary else NovaTheme.colors.textSecondary)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .shadow(NovaTheme.elevation.level4, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(NovaTheme.colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    val artFile = track?.albumArtFile
                    if (artFile != null && artFile.exists()) {
                        AsyncImage(
                            model = artFile,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = track?.avatarLetter ?: "♪",
                            style = NovaTheme.typography.display,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: "No Track Playing",
                            style = NovaTheme.typography.title,
                            color = NovaTheme.colors.textPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = track?.artist ?: "Unknown Artist",
                            style = NovaTheme.typography.subtitle,
                            color = NovaTheme.colors.textSecondary,
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (playback.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (playback.isFavorite) NovaTheme.colors.categoryVideos else NovaTheme.colors.textSecondary
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = playback.progressFraction,
                        onValueChange = { frac ->
                            val targetMs = (frac * playback.durationMs).toLong()
                            onSeekTo(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = NovaTheme.colors.primary,
                            activeTrackColor = NovaTheme.colors.primary,
                            inactiveTrackColor = NovaTheme.colors.cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = FileFormatters.formatDuration(playback.currentPositionMs),
                            style = NovaTheme.typography.captionMedium,
                            color = NovaTheme.colors.textSecondary
                        )
                        Text(
                            text = FileFormatters.formatDuration(playback.durationMs),
                            style = NovaTheme.typography.captionMedium,
                            color = NovaTheme.colors.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playback.isShuffleEnabled) NovaTheme.colors.primary else NovaTheme.colors.textSecondary
                        )
                    }

                    IconButton(onClick = onSkipPrevious) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = NovaTheme.colors.textPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NovaTheme.colors.primary, NovaTheme.colors.secondary)
                                )
                            )
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playback.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = onSkipNext) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = NovaTheme.colors.textPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = if (playback.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (playback.repeatMode != Player.REPEAT_MODE_OFF) NovaTheme.colors.primary else NovaTheme.colors.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    IconButton(onClick = onOpenQueue) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue", tint = NovaTheme.colors.textSecondary)
                    }
                    IconButton(onClick = {
                        try {
                            val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                            }
                            context.startActivity(eqIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "System Equalizer not available", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Equalizer, contentDescription = "Equalizer", tint = NovaTheme.colors.textSecondary)
                    }
                    IconButton(onClick = {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        audioManager?.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_SAME,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Volume", tint = NovaTheme.colors.textSecondary)
                    }
                    IconButton(onClick = {
                        val info = "${track?.title ?: "Track"} • ${track?.artist ?: "Unknown"} (${track?.bitrate ?: "Audio"})"
                        Toast.makeText(context, info, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "Track Info", tint = NovaTheme.colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicLibraryContent(
    uiState: MusicPlayerUiState,
    onPlayTrack: (FileItem) -> Unit,
    onPlayTracks: (List<MusicTrack>, Int) -> Unit,
    onSelectTab: (MusicLibraryTab) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectSubItem: (String, List<MusicTrack>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Tab Row: Tracks, Albums, Artists, Folders
        TabRow(
            selectedTabIndex = uiState.currentTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = NovaTheme.colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.currentTab.ordinal]),
                    color = NovaTheme.colors.primary
                )
            }
        ) {
            MusicLibraryTab.entries.forEach { tab ->
                val isSelected = uiState.currentTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { onSelectTab(tab) },
                    text = {
                        Text(
                            text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = NovaTheme.typography.button,
                            color = if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.textSecondary
                        )
                    }
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search in ${uiState.currentTab.name.lowercase()}...",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = NovaTheme.colors.textSecondary
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = NovaTheme.colors.textSecondary
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NovaTheme.colors.primary,
                unfocusedBorderColor = NovaTheme.colors.cardBorder,
                focusedTextColor = NovaTheme.colors.textPrimary,
                unfocusedTextColor = NovaTheme.colors.textPrimary,
                cursorColor = NovaTheme.colors.primary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // Content Area with State Management
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NovaLoadingState(text = "Scanning music library...")
                }
            }

            uiState.isPermissionRequired -> {
                val context = LocalContext.current
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = NovaTheme.colors.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Storage Permission Required",
                            style = NovaTheme.typography.title,
                            color = NovaTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NovaFiles requires storage or audio permission to discover music on your device.",
                            style = NovaTheme.typography.body,
                            color = NovaTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        NovaButton(
                            text = "Grant Access",
                            onClick = {
                                try {
                                    context.startActivity(StoragePermissionHelper.createManageStorageIntent(context))
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NovaErrorState(
                        message = uiState.errorMessage,
                        onRetry = onRefresh
                    )
                }
            }

            else -> {
                when (uiState.currentTab) {
                    MusicLibraryTab.TRACKS -> {
                        val tracks = uiState.filteredTracks
                        if (tracks.isEmpty()) {
                            NovaEmptyState(
                                title = "No tracks found",
                                subtitle = if (uiState.searchQuery.isNotEmpty()) "No tracks matched '${uiState.searchQuery}'" else "Add audio files (.mp3, .flac, .wav, .m4a) to your device"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                            ) {
                                itemsIndexed(tracks, key = { _, track -> track.fileItem.id }) { index, track ->
                                    val isCurrentTrack = uiState.playback.currentFilePath == track.fileItem.path
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(NovaTheme.shapes.medium)
                                            .clickable { onPlayTracks(tracks, index) }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(NovaTheme.colors.categoryAudio.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = NovaTheme.colors.categoryAudio,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                style = NovaTheme.typography.subtitle,
                                                color = if (isCurrentTrack) NovaTheme.colors.primary else NovaTheme.colors.textPrimary,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val durationText = if (track.durationMs > 0) {
                                                FileFormatters.formatDuration(track.durationMs)
                                            } else {
                                                FileFormatters.formatBytes(track.fileItem.sizeBytes)
                                            }
                                            Text(
                                                text = "${track.artist} • ${track.format} • $durationText",
                                                style = NovaTheme.typography.caption,
                                                color = NovaTheme.colors.textSecondary,
                                                maxLines = 1
                                            )
                                        }

                                        if (isCurrentTrack && uiState.playback.isPlaying) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(NovaTheme.colors.primary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MusicLibraryTab.ALBUMS -> {
                        val albums = uiState.filteredAlbums
                        if (albums.isEmpty()) {
                            NovaEmptyState(
                                title = "No albums found",
                                subtitle = if (uiState.searchQuery.isNotEmpty()) "No albums matched '${uiState.searchQuery}'" else "Music files with album tags will appear here"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                            ) {
                                itemsIndexed(albums, key = { _, album -> album.name + album.artist }) { _, album ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(NovaTheme.shapes.medium)
                                            .clickable { onSelectSubItem(album.name, album.tracks) }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(NovaTheme.colors.surfaceElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = album.name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "A",
                                                style = NovaTheme.typography.title,
                                                color = NovaTheme.colors.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = album.name,
                                                style = NovaTheme.typography.subtitle,
                                                color = NovaTheme.colors.textPrimary,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${album.artist} • ${album.trackCount} ${if (album.trackCount == 1) "track" else "tracks"}",
                                                style = NovaTheme.typography.caption,
                                                color = NovaTheme.colors.textSecondary,
                                                maxLines = 1
                                            )
                                        }

                                        IconButton(onClick = { onPlayTracks(album.tracks, 0) }) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play Album",
                                                tint = NovaTheme.colors.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MusicLibraryTab.ARTISTS -> {
                        val artists = uiState.filteredArtists
                        if (artists.isEmpty()) {
                            NovaEmptyState(
                                title = "No artists found",
                                subtitle = if (uiState.searchQuery.isNotEmpty()) "No artists matched '${uiState.searchQuery}'" else "Music files with artist tags will appear here"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                            ) {
                                itemsIndexed(artists, key = { _, artist -> artist.name }) { _, artist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(NovaTheme.shapes.medium)
                                            .clickable { onSelectSubItem(artist.name, artist.tracks) }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(NovaTheme.colors.primary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = artist.name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "♪",
                                                style = NovaTheme.typography.title,
                                                color = NovaTheme.colors.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = artist.name,
                                                style = NovaTheme.typography.subtitle,
                                                color = NovaTheme.colors.textPrimary,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${artist.trackCount} ${if (artist.trackCount == 1) "track" else "tracks"} • ${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"}",
                                                style = NovaTheme.typography.caption,
                                                color = NovaTheme.colors.textSecondary,
                                                maxLines = 1
                                            )
                                        }

                                        IconButton(onClick = { onPlayTracks(artist.tracks, 0) }) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play Artist",
                                                tint = NovaTheme.colors.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MusicLibraryTab.FOLDERS -> {
                        val folders = uiState.filteredFolders
                        if (folders.isEmpty()) {
                            NovaEmptyState(
                                title = "No folders found",
                                subtitle = if (uiState.searchQuery.isNotEmpty()) "No folders matched '${uiState.searchQuery}'" else "Directories containing audio files will appear here"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                            ) {
                                itemsIndexed(folders, key = { _, folder -> folder.path }) { _, folder ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(NovaTheme.shapes.medium)
                                            .clickable { onSelectSubItem(folder.name, folder.tracks) }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(NovaTheme.colors.categoryFolders.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = NovaTheme.colors.categoryFolders,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.name,
                                                style = NovaTheme.typography.subtitle,
                                                color = NovaTheme.colors.textPrimary,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${folder.trackCount} ${if (folder.trackCount == 1) "track" else "tracks"} • ${FileFormatters.formatBytes(folder.totalSizeBytes)}",
                                                style = NovaTheme.typography.caption,
                                                color = NovaTheme.colors.textSecondary,
                                                maxLines = 1
                                            )
                                        }

                                        IconButton(onClick = { onPlayTracks(folder.tracks, 0) }) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play Folder",
                                                tint = NovaTheme.colors.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicMiniPlayerBar(
    track: com.novafiles.core.media.AudioTrackMetadata,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = track.title.ifBlank { "Unknown Track" }
    val artist = track.artist.ifBlank { "Unknown Artist" }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = NovaTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    style = NovaTheme.typography.subtitle,
                    color = NovaTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$artist • ${if (isPlaying) "Playing" else "Paused"}",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary,
                    maxLines = 1
                )
            }

            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = onSkipNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = NovaTheme.colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
