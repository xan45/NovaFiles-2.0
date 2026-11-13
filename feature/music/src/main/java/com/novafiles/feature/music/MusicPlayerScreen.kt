@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.music

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
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.media.MusicState
import com.novafiles.core.model.FileItem
import com.novafiles.core.ui.components.NovaBottomSheet

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
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onSeekTo = { viewModel.seekTo(it) },
        onSkipNext = { viewModel.skipToNext() },
        onSkipPrevious = { viewModel.skipToPrevious() },
        onToggleRepeat = { viewModel.toggleRepeat() },
        onToggleShuffle = { viewModel.toggleShuffle() },
        onToggleFavorite = { viewModel.toggleFavorite() },
        onSelectTab = { viewModel.setLibraryTab(it) },
        onToggleQueue = { viewModel.toggleQueueSheet(it) },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
fun MusicPlayerScreen(
    uiState: MusicPlayerUiState,
    onPlayTrack: (FileItem) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSelectTab: (MusicLibraryTab) -> Unit,
    onToggleQueue: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NovaTheme.colors.textPrimary
                    )
                }

                Text(
                    text = if (uiState.playback.currentTrack != null) "Now Playing" else "Music Library",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary
                )

                IconButton(onClick = { onToggleQueue(true) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Playback Queue",
                        tint = NovaTheme.colors.textPrimary
                    )
                }
            }

            if (uiState.playback.currentTrack != null) {
                // Screen 3: Full Music Player UI
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
                // Screen 6: Music Library UI
                MusicLibraryContent(
                    uiState = uiState,
                    onPlayTrack = onPlayTrack,
                    onSelectTab = onSelectTab,
                    modifier = Modifier.weight(1f)
                )
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

        // Large Album Artwork Container (Reference Screen 3)
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

        // Track Title & Artist Row with Heart Favorite Toggle
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

        // Scrubbable Progress Bar
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

        // Playback Controls Row: Shuffle, Previous, Play/Pause, Next, Repeat
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

            // Big Central Play/Pause Button
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

        // Bottom Auxiliary Toolbar (Queue, Equalizer, Volume, More)
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
    onSelectTab: (MusicLibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Tab Row: Tracks, Albums, Artists, Folders (Reference Screen 6)
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

        // Track List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
        ) {
            itemsIndexed(uiState.libraryTracks) { index, track ->
                val isCurrentTrack = uiState.playback.currentFilePath == track.path
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NovaTheme.shapes.medium)
                        .clickable { onPlayTrack(track) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NovaTheme.colors.categoryAudio.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = NovaTheme.colors.categoryAudio,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.name.substringBeforeLast('.'),
                            style = NovaTheme.typography.subtitle,
                            color = if (isCurrentTrack) NovaTheme.colors.primary else NovaTheme.colors.textPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MP3 • ${FileFormatters.formatBytes(track.sizeBytes)}",
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textSecondary
                        )
                    }

                    if (isCurrentTrack && uiState.playback.isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NovaTheme.colors.primary)
                        )
                    }
                }
            }
        }
    }
}
