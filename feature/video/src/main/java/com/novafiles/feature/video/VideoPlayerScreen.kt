@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.video

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.ui.components.NovaBottomSheet

@Composable
fun VideoPlayerScreen(
    videoFilePath: String = "",
    playQueue: List<String> = emptyList(),
    startIndex: Int = 0,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(videoFilePath, playQueue, startIndex) {
        val queue = if (playQueue.isNotEmpty()) playQueue else if (videoFilePath.isNotBlank()) listOf(videoFilePath) else emptyList()
        if (queue.isNotEmpty()) {
            viewModel.loadQueue(queue, startIndex)
        }
    }

    // Orientation Handling
    DisposableEffect(uiState.orientationPreference) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        when (uiState.orientationPreference) {
            VideoOrientationPref.LANDSCAPE -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            VideoOrientationPref.PORTRAIT -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            VideoOrientationPref.AUTO -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        when {
            uiState.showTechnicalDetailsSheet -> viewModel.dismissTechnicalDetailsSheet()
            uiState.showSubtitlesDialog -> viewModel.toggleSubtitlesDialog(false)
            uiState.showAudioTracksDialog -> viewModel.toggleAudioTracksDialog(false)
            uiState.showSpeedDialog -> viewModel.toggleSpeedDialog(false)
            else -> onNavigateBack()
        }
    }

    // Keep screen on while playing video
    DisposableEffect(uiState.isPlaying) {
        val window = (context as? Activity)?.window
        if (uiState.isPlaying) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Update screen brightness on window
    LaunchedEffect(uiState.brightnessPercent) {
        val activity = context as? Activity
        val lp = activity?.window?.attributes
        if (lp != null) {
            lp.screenBrightness = (uiState.brightnessPercent / 100f).coerceIn(0.01f, 1.0f)
            activity.window.attributes = lp
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Surface Layer
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    player = viewModel.player
                    this.resizeMode = uiState.resizeMode
                }
            },
            update = { playerView ->
                playerView.player = viewModel.player
                playerView.resizeMode = uiState.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Detection Overlay
        var totalDragX by remember { mutableFloatStateOf(0f) }
        var totalDragY by remember { mutableFloatStateOf(0f) }
        var dragMode by remember { mutableStateOf<DragGestureMode?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            viewModel.toggleControlsVisibility()
                        },
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2f) {
                                viewModel.seekRelative(-10)
                            } else {
                                viewModel.seekRelative(10)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            totalDragX = 0f
                            totalDragY = 0f
                            val screenWidth = size.width
                            dragMode = if (offset.x < screenWidth * 0.4f) {
                                DragGestureMode.BRIGHTNESS
                            } else if (offset.x > screenWidth * 0.6f) {
                                DragGestureMode.VOLUME
                            } else {
                                DragGestureMode.SEEK
                            }
                        },
                        onDragEnd = {
                            if (dragMode == DragGestureMode.SEEK) {
                                viewModel.commitSeekPreview()
                            }
                            dragMode = null
                        },
                        onDragCancel = {
                            dragMode = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            when (dragMode) {
                                DragGestureMode.BRIGHTNESS -> {
                                    val delta = -dragAmount.y / 600f
                                    viewModel.adjustBrightnessDelta(delta)
                                }
                                DragGestureMode.VOLUME -> {
                                    val delta = -dragAmount.y / 600f
                                    viewModel.adjustVolumeDelta(delta)
                                }
                                DragGestureMode.SEEK -> {
                                    val offsetSec = (totalDragX / 15f).toInt()
                                    viewModel.showSeekPreview(offsetSec)
                                }
                                null -> {}
                            }
                        }
                    )
                }
        )

        // HUD Overlays
        if (uiState.isBrightnessOverlayVisible) {
            GestureHud(
                icon = { Icon(Icons.Rounded.BrightnessMedium, contentDescription = null, tint = Color.White) },
                label = "Brightness",
                percent = uiState.brightnessPercent,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (uiState.isVolumeOverlayVisible) {
            GestureHud(
                icon = { Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = Color.White) },
                label = "Volume",
                percent = uiState.volumePercent,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (uiState.isSeekOverlayVisible) {
            SeekHud(
                offsetSec = uiState.seekPreviewOffsetSec,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Floating Frosted Controller Layer
        AnimatedVisibility(
            visible = uiState.areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.toggleControlsVisibility()
                    }
            ) {
                // Top gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                )

                // Bottom gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )

                // Top Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Floating Back Button
                    FrostedCircularButton(
                        size = 44.dp,
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Floating Upper-Right Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Subtitles button
                        FrostedCircularButton(
                            size = 40.dp,
                            onClick = { viewModel.toggleSubtitlesDialog(true) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ClosedCaption,
                                contentDescription = "Subtitles",
                                tint = if (uiState.availableSubtitles.any { it.isSelected && it.id != "none" }) NovaTheme.colors.primary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Audio Track button
                        FrostedCircularButton(
                            size = 40.dp,
                            onClick = { viewModel.toggleAudioTracksDialog(true) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Audiotrack,
                                contentDescription = "Audio Track",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Orientation toggle
                        FrostedCircularButton(
                            size = 40.dp,
                            onClick = { viewModel.toggleOrientationPreference() }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ScreenRotation,
                                contentDescription = "Orientation: ${uiState.orientationPreference.label}",
                                tint = if (uiState.orientationPreference != VideoOrientationPref.AUTO) NovaTheme.colors.primary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Aspect ratio toggle
                        FrostedCircularButton(
                            size = 40.dp,
                            onClick = { viewModel.cycleResizeMode() }
                        ) {
                            Icon(
                                imageVector = when (uiState.resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> Icons.Rounded.Fullscreen
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> Icons.Rounded.FitScreen
                                    else -> Icons.Rounded.FitScreen
                                },
                                contentDescription = "Aspect Ratio",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Playback Speed Pill
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x55000000))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .clickable { viewModel.toggleSpeedDialog(true) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${uiState.playbackSpeed}x",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // 3-Dots Technical Info
                        FrostedCircularButton(
                            size = 40.dp,
                            onClick = { viewModel.openTechnicalDetailsSheet() }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Technical Info",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Central Floating Controls (Prev, Rewind 10s, Play/Pause, Forward 10s, Next)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Video
                    FrostedCircularButton(
                        size = 48.dp,
                        onClick = { viewModel.playPrevious() },
                        enabled = uiState.hasPrevious
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous video",
                            tint = if (uiState.hasPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Rewind 10s
                    FrostedCircularButton(
                        size = 52.dp,
                        onClick = { viewModel.seekRelative(-10) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play/Pause Main Button
                    FrostedCircularButton(
                        size = 72.dp,
                        borderAlpha = 0.45f,
                        bgAlpha = 0.55f,
                        onClick = { viewModel.togglePlayPause() }
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Forward 10s
                    FrostedCircularButton(
                        size = 52.dp,
                        onClick = { viewModel.seekRelative(10) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Next Video
                    FrostedCircularButton(
                        size = 48.dp,
                        onClick = { viewModel.playNext() },
                        enabled = uiState.hasNext
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next video",
                            tint = if (uiState.hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Content: Title/Subtitle, Time, and Continuous Progress Scrubber
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Video Title and Time row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.subtitle,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "${uiState.formattedCurrentTime} / ${uiState.formattedTotalTime}",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Slider
                    Slider(
                        value = uiState.currentPositionMs.toFloat(),
                        onValueChange = { newPos ->
                            viewModel.seekTo(newPos.toLong())
                        },
                        valueRange = 0f..uiState.durationMs.coerceAtLeast(1L).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = NovaTheme.colors.primary,
                            activeTrackColor = NovaTheme.colors.primary,
                            inactiveTrackColor = Color(0x40FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                }
            }
        }

        // Subtitles Track Dialog
        if (uiState.showSubtitlesDialog) {
            TrackSelectionDialog(
                title = "Subtitles",
                options = uiState.availableSubtitles,
                onSelect = { viewModel.selectSubtitleTrack(it) },
                onDismiss = { viewModel.toggleSubtitlesDialog(false) }
            )
        }

        // Audio Track Dialog
        if (uiState.showAudioTracksDialog) {
            TrackSelectionDialog(
                title = "Audio Tracks",
                options = uiState.availableAudioTracks,
                onSelect = { viewModel.selectAudioTrack(it) },
                onDismiss = { viewModel.toggleAudioTracksDialog(false) }
            )
        }

        // Playback Speed Dialog
        if (uiState.showSpeedDialog) {
            SpeedSelectionDialog(
                currentSpeed = uiState.playbackSpeed,
                onSelectSpeed = { viewModel.setSpeed(it) },
                onDismiss = { viewModel.toggleSpeedDialog(false) }
            )
        }

        // Technical Details Bottom Sheet
        if (uiState.showTechnicalDetailsSheet) {
            NovaBottomSheet(
                onDismissRequest = { viewModel.dismissTechnicalDetailsSheet() }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Video Technical Details",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val details = uiState.technicalDetails
                    if (details != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TechnicalDetailRow("File Name", details.fileName)
                            TechnicalDetailRow("Resolution", details.resolution)
                            TechnicalDetailRow("Container", details.containerFormat)
                            TechnicalDetailRow("Video Codec", details.videoCodec)
                            TechnicalDetailRow("Audio Codec", details.audioCodec)
                            TechnicalDetailRow("Frame Rate", details.frameRate)
                            TechnicalDetailRow("Bitrate", details.videoBitrate)
                            TechnicalDetailRow("Duration", details.formattedDuration)
                            TechnicalDetailRow("File Size", details.fileSizeFormatted)
                            details.dateModifiedFormatted?.let {
                                TechnicalDetailRow("Date Modified", it)
                            }
                        }
                    } else {
                        Text(
                            text = "Analyzing video stream...",
                            color = NovaTheme.colors.textSecondary,
                            style = NovaTheme.typography.body,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostedCircularButton(
    size: androidx.compose.ui.unit.Dp,
    borderAlpha: Float = 0.25f,
    bgAlpha: Float = 0.35f,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (enabled) bgAlpha else bgAlpha * 0.5f))
            .border(1.dp, Color.White.copy(alpha = if (enabled) borderAlpha else borderAlpha * 0.3f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun TechnicalDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = NovaTheme.colors.textSecondary,
            style = NovaTheme.typography.caption
        )
        Text(
            text = value,
            color = NovaTheme.colors.textPrimary,
            style = NovaTheme.typography.body,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrackSelectionDialog(
    title: String,
    options: List<VideoTrackOption>,
    onSelect: (VideoTrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = NovaTheme.typography.title,
                fontWeight = FontWeight.Bold,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (options.isEmpty()) {
                Text(
                    text = "No tracks available",
                    color = NovaTheme.colors.textTertiary,
                    style = NovaTheme.typography.body,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(options) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(track) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = track.label,
                                style = NovaTheme.typography.body,
                                color = if (track.isSelected) NovaTheme.colors.primary else NovaTheme.colors.textPrimary,
                                fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (track.isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = NovaTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
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
private fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Playback Speed",
                style = NovaTheme.typography.title,
                fontWeight = FontWeight.Bold,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(speeds) { speed ->
                    val isSelected = currentSpeed == speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSpeed(speed) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                            style = NovaTheme.typography.body,
                            color = if (isSelected) NovaTheme.colors.primary else NovaTheme.colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = NovaTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureHud(
    icon: @Composable () -> Unit,
    label: String,
    percent: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC111B3A))
            .border(1.dp, Color(0x335B6EF5), RoundedCornerShape(16.dp)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Text(
                text = "$label: $percent%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            LinearProgressIndicator(
                progress = { percent / 100f },
                color = Color(0xFF5B6EF5),
                trackColor = Color(0x33FFFFFF),
                modifier = Modifier
                    .width(100.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun SeekHud(
    offsetSec: Int,
    modifier: Modifier = Modifier
) {
    val sign = if (offsetSec >= 0) "+" else ""
    val text = "$sign${offsetSec}s"
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC111B3A))
            .border(1.dp, Color(0x335B6EF5), RoundedCornerShape(16.dp)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Seek preview",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
    }
}

private enum class DragGestureMode {
    BRIGHTNESS, VOLUME, SEEK
}
