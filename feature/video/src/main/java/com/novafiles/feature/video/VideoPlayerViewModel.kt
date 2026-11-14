package com.novafiles.feature.video

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
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

data class VideoTrackOption(
    val id: String,
    val label: String,
    val isSelected: Boolean,
    val groupIndex: Int = 0,
    val trackIndex: Int = 0
)

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
    val technicalDetails: VideoTechnicalDetails? = null,
    val playQueue: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val availableSubtitles: List<VideoTrackOption> = emptyList(),
    val availableAudioTracks: List<VideoTrackOption> = emptyList(),
    val showSubtitlesDialog: Boolean = false,
    val showAudioTracksDialog: Boolean = false,
    val showSpeedDialog: Boolean = false,
    val orientationPreference: VideoOrientationPref = VideoOrientationPref.AUTO
) {
    val progressFraction: Float get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val formattedCurrentTime: String get() = formatTime(currentPositionMs)
    val formattedTotalTime: String get() = formatTime(durationMs)
    val hasPrevious: Boolean get() = currentIndex > 0
    val hasNext: Boolean get() = currentIndex < playQueue.size - 1

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
class VideoPlayerViewModel @JvmOverloads constructor(
    application: Application,
    private val preferencesRepo: VideoPreferencesRepository = VideoPreferencesRepository(application)
) : AndroidViewModel(application) {

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

        // Observe initial preferences
        val prefs = preferencesRepo.preferences.value
        _uiState.update {
            it.copy(
                playbackSpeed = prefs.defaultPlaybackSpeed,
                orientationPreference = prefs.orientationPreference
            )
        }
        player.playbackParameters = PlaybackParameters(prefs.defaultPlaybackSpeed)

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracking()
                    scheduleControlsAutoHide()
                } else {
                    stopProgressTracking()
                    showControls()
                    saveCurrentPosition()
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
                    updateTracks()
                } else if (playbackState == Player.STATE_ENDED) {
                    saveCurrentPosition()
                    if (_uiState.value.hasNext) {
                        playNext()
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracks()
            }
        })
    }

    fun loadVideo(filePath: String) {
        loadQueue(listOf(filePath), startIndex = 0)
    }

    fun loadQueue(queue: List<String>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, queue.size - 1)
        val targetPath = queue[validIndex]

        if (_uiState.value.filePath == targetPath && player.playbackState != Player.STATE_IDLE) {
            // Already active and playing
            return
        }

        val file = File(targetPath)
        if (!file.exists()) return

        val title = file.nameWithoutExtension
        val subtitle = "${file.extension.uppercase()} • ${(file.length() / (1024 * 1024))} MB"

        _uiState.update {
            it.copy(
                filePath = targetPath,
                title = title,
                subtitle = subtitle,
                playQueue = queue,
                currentIndex = validIndex,
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

        // Check resume position
        val savedPos = preferencesRepo.getPlaybackPosition(targetPath)
        if (savedPos > 2000L) {
            player.seekTo(savedPos)
            _uiState.update { it.copy(currentPositionMs = savedPos) }
        }

        player.playWhenReady = true

        viewModelScope.launch {
            val details = VideoMetadataHelper.extractTechnicalDetails(file)
            _uiState.update { it.copy(technicalDetails = details) }
        }

        scheduleControlsAutoHide()
    }

    fun playNext() {
        val state = _uiState.value
        if (state.hasNext) {
            saveCurrentPosition()
            val nextIdx = state.currentIndex + 1
            loadQueue(state.playQueue, nextIdx)
        }
    }

    fun playPrevious() {
        val state = _uiState.value
        if (state.hasPrevious) {
            saveCurrentPosition()
            val prevIdx = state.currentIndex - 1
            loadQueue(state.playQueue, prevIdx)
        }
    }

    private fun saveCurrentPosition() {
        val path = _uiState.value.filePath
        val pos = player.currentPosition
        val dur = player.duration
        if (path.isNotBlank() && dur > 0) {
            viewModelScope.launch {
                preferencesRepo.savePlaybackPosition(path, pos, dur)
            }
        }
    }

    private fun updateTracks() {
        val tracks = player.currentTracks
        val subtitleOptions = mutableListOf<VideoTrackOption>()
        val audioOptions = mutableListOf<VideoTrackOption>()

        // "Off" option for subtitles
        val isSubDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        subtitleOptions.add(VideoTrackOption(id = "none", label = "Off (Subtitles Disabled)", isSelected = isSubDisabled))

        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val isSelected = !isSubDisabled && group.isTrackSelected(trackIndex)
                    val lang = format.language ?: "und"
                    val label = format.label ?: "Track ${trackIndex + 1} ($lang)"
                    subtitleOptions.add(
                        VideoTrackOption(
                            id = "$groupIndex:$trackIndex",
                            label = label,
                            isSelected = isSelected,
                            groupIndex = groupIndex,
                            trackIndex = trackIndex
                        )
                    )
                }
            } else if (group.type == C.TRACK_TYPE_AUDIO) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val isSelected = group.isTrackSelected(trackIndex)
                    val lang = format.language ?: "und"
                    val label = format.label ?: "Audio ${trackIndex + 1} ($lang)"
                    audioOptions.add(
                        VideoTrackOption(
                            id = "$groupIndex:$trackIndex",
                            label = label,
                            isSelected = isSelected,
                            groupIndex = groupIndex,
                            trackIndex = trackIndex
                        )
                    )
                }
            }
        }

        _uiState.update {
            it.copy(
                availableSubtitles = subtitleOptions,
                availableAudioTracks = audioOptions
            )
        }
    }

    fun selectSubtitleTrack(track: VideoTrackOption) {
        if (track.id == "none") {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val tracks = player.currentTracks
            if (track.groupIndex < tracks.groups.size) {
                val group = tracks.groups[track.groupIndex].mediaTrackGroup
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(group, listOf(track.trackIndex)))
                    .build()
            }
        }
        _uiState.update { it.copy(showSubtitlesDialog = false) }
    }

    fun selectAudioTrack(track: VideoTrackOption) {
        val tracks = player.currentTracks
        if (track.groupIndex < tracks.groups.size) {
            val group = tracks.groups[track.groupIndex].mediaTrackGroup
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setOverrideForType(TrackSelectionOverride(group, listOf(track.trackIndex)))
                .build()
        }
        _uiState.update { it.copy(showAudioTracksDialog = false) }
    }

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        _uiState.update { it.copy(playbackSpeed = speed, showSpeedDialog = false) }
        viewModelScope.launch {
            preferencesRepo.updateDefaultPlaybackSpeed(speed)
        }
        resetControlsTimeout()
    }

    fun toggleSubtitlesDialog(show: Boolean) {
        _uiState.update { it.copy(showSubtitlesDialog = show) }
    }

    fun toggleAudioTracksDialog(show: Boolean) {
        _uiState.update { it.copy(showAudioTracksDialog = show) }
    }

    fun toggleSpeedDialog(show: Boolean) {
        _uiState.update { it.copy(showSpeedDialog = show) }
    }

    fun toggleOrientationPreference() {
        val current = _uiState.value.orientationPreference
        val next = when (current) {
            VideoOrientationPref.AUTO -> VideoOrientationPref.LANDSCAPE
            VideoOrientationPref.LANDSCAPE -> VideoOrientationPref.PORTRAIT
            VideoOrientationPref.PORTRAIT -> VideoOrientationPref.AUTO
        }
        _uiState.update { it.copy(orientationPreference = next) }
        viewModelScope.launch {
            preferencesRepo.updateOrientationPreference(next)
        }
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
        saveCurrentPosition()
        resetControlsTimeout()
    }

    fun seekRelative(offsetSec: Int) {
        val target = (player.currentPosition + (offsetSec * 1000L)).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        _uiState.update { it.copy(currentPositionMs = target) }
        saveCurrentPosition()
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
        saveCurrentPosition()
        stopProgressTracking()
        autoHideControlsJob?.cancel()
        overlayHideJob?.cancel()
        player.release()
        super.onCleared()
    }
}
