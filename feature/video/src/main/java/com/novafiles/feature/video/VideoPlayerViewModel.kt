package com.novafiles.feature.video

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.novafiles.core.media.VideoMetadataHelper
import com.novafiles.core.media.VideoTechnicalDetails
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

data class VideoPlayerUiState(
    val filePath: String = "",
    val title: String = "",
    val subtitle: String = "Video",
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val areControlsVisible: Boolean = true,
    val resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    val playbackSpeed: Float = 1.0f,
    val brightnessPercent: Int = 50,
    val isBrightnessOverlayVisible: Boolean = false,
    val volumePercent: Int = 50,
    val isVolumeOverlayVisible: Boolean = false,
    val seekPreviewOffsetSec: Int = 0,
    val isSeekOverlayVisible: Boolean = false,
    val showTechnicalDetailsSheet: Boolean = false,
    val technicalDetails: VideoTechnicalDetails? = null
) {
    val progressFraction: Float get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val formattedCurrentTime: String get() = formatTime(currentPositionMs)
    val formattedTotalTime: String get() = formatTime(durationMs)

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val s = totalSec % 60
        val m = (totalSec / 60) % 60
        val h = totalSec / 3600
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }
}

@OptIn(UnstableApi::class)
class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private var autoHideControlsJob: Job? = null
    private var progressTrackingJob: Job? = null
    private var overlayHideJob: Job? = null

    init {
        // Read initial system volume
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volPercent = if (maxVol > 0) ((curVol.toFloat() / maxVol.toFloat()) * 100).toInt() else 50
        _uiState.update { it.copy(volumePercent = volPercent) }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracking()
                    scheduleControlsAutoHide()
                } else {
                    stopProgressTracking()
                    showControls()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.update {
                        it.copy(
                            durationMs = player.duration.coerceAtLeast(0L),
                            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                        )
                    }
                }
            }
        })
    }

    fun loadVideo(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val title = file.nameWithoutExtension
        val subtitle = "${file.extension.uppercase()} • ${(file.length() / (1024 * 1024))} MB"

        _uiState.update {
            it.copy(
                filePath = filePath,
                title = title,
                subtitle = subtitle,
                areControlsVisible = true
            )
        }

        val subConfigs = listOf("srt", "vtt").mapNotNull { ext ->
            val subFile = File(file.parentFile, "${file.nameWithoutExtension}.$ext")
            if (subFile.exists() && subFile.isFile) {
                val mime = if (ext == "vtt") MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
                MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subFile))
                    .setMimeType(mime)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            } else null
        }
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setSubtitleConfigurations(subConfigs)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        viewModelScope.launch {
            val details = VideoMetadataHelper.extractTechnicalDetails(file)
            _uiState.update { it.copy(technicalDetails = details) }
        }

        scheduleControlsAutoHide()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        resetControlsTimeout()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
        resetControlsTimeout()
    }

    fun seekRelative(offsetSec: Int) {
        val target = (player.currentPosition + (offsetSec * 1000L)).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        _uiState.update { it.copy(currentPositionMs = target) }
        resetControlsTimeout()
    }

    fun toggleControlsVisibility() {
        val newVisibility = !_uiState.value.areControlsVisible
        _uiState.update { it.copy(areControlsVisible = newVisibility) }
        if (newVisibility && player.isPlaying) {
            scheduleControlsAutoHide()
        }
    }

    fun showControls() {
        _uiState.update { it.copy(areControlsVisible = true) }
        if (player.isPlaying) {
            scheduleControlsAutoHide()
        }
    }

    fun hideControls() {
        _uiState.update { it.copy(areControlsVisible = false) }
        autoHideControlsJob?.cancel()
    }

    fun resetControlsTimeout() {
        if (player.isPlaying) {
            scheduleControlsAutoHide()
        }
    }

    private fun scheduleControlsAutoHide() {
        autoHideControlsJob?.cancel()
        autoHideControlsJob = viewModelScope.launch {
            delay(3500)
            _uiState.update { it.copy(areControlsVisible = false) }
        }
    }

    fun cycleResizeMode() {
        val current = _uiState.value.resizeMode
        val next = when (current) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        _uiState.update { it.copy(resizeMode = next) }
        resetControlsTimeout()
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
        val currentSpeed = _uiState.value.playbackSpeed
        val nextIdx = (speeds.indexOf(currentSpeed) + 1) % speeds.size
        val nextSpeed = speeds[nextIdx]
        player.playbackParameters = PlaybackParameters(nextSpeed)
        _uiState.update { it.copy(playbackSpeed = nextSpeed) }
        resetControlsTimeout()
    }

    fun adjustVolumeDelta(delta: Float) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newVol = (curVol + (delta * maxVol)).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)

        val percent = if (maxVol > 0) ((newVol.toFloat() / maxVol.toFloat()) * 100).toInt() else 0
        _uiState.update {
            it.copy(
                volumePercent = percent,
                isVolumeOverlayVisible = true,
                isBrightnessOverlayVisible = false,
                isSeekOverlayVisible = false
            )
        }
        scheduleOverlayAutoHide()
    }

    fun adjustBrightnessDelta(delta: Float) {
        val current = _uiState.value.brightnessPercent
        val updated = (current + (delta * 100).toInt()).coerceIn(0, 100)
        _uiState.update {
            it.copy(
                brightnessPercent = updated,
                isBrightnessOverlayVisible = true,
                isVolumeOverlayVisible = false,
                isSeekOverlayVisible = false
            )
        }
        scheduleOverlayAutoHide()
    }

    fun showSeekPreview(offsetSec: Int) {
        _uiState.update {
            it.copy(
                seekPreviewOffsetSec = offsetSec,
                isSeekOverlayVisible = true,
                isBrightnessOverlayVisible = false,
                isVolumeOverlayVisible = false
            )
        }
    }

    fun commitSeekPreview() {
        val offset = _uiState.value.seekPreviewOffsetSec
        if (offset != 0) {
            seekRelative(offset)
        }
        _uiState.update { it.copy(isSeekOverlayVisible = false, seekPreviewOffsetSec = 0) }
    }

    private fun scheduleOverlayAutoHide() {
        overlayHideJob?.cancel()
        overlayHideJob = viewModelScope.launch {
            delay(1200)
            _uiState.update {
                it.copy(
                    isVolumeOverlayVisible = false,
                    isBrightnessOverlayVisible = false,
                    isSeekOverlayVisible = false
                )
            }
        }
    }

    fun openTechnicalDetailsSheet() {
        _uiState.update { it.copy(showTechnicalDetailsSheet = true) }
    }

    fun dismissTechnicalDetailsSheet() {
        _uiState.update { it.copy(showTechnicalDetailsSheet = false) }
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update {
                    it.copy(
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                        bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                    )
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    override fun onCleared() {
        stopProgressTracking()
        autoHideControlsJob?.cancel()
        overlayHideJob?.cancel()
        player.release()
        super.onCleared()
    }
}
