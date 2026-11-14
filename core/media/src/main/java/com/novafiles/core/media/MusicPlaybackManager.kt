package com.novafiles.core.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class MusicState(
    val currentTrack: AudioTrackMetadata? = null,
    val currentFilePath: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<File> = emptyList(),
    val currentIndex: Int = -1,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isShuffleEnabled: Boolean = false,
    val isFavorite: Boolean = false
) {
    val progressFraction: Float get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

object MusicPlaybackManager {

    const val ACTION_WIDGET_UPDATE = "com.novafiles.app.ACTION_MUSIC_WIDGET_UPDATE"

    private var player: ExoPlayer? = null
    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(MusicState())
    val state: StateFlow<MusicState> = _state.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun ensureService(context: Context) {
        if (player == null) {
            try {
                val serviceIntent = Intent(context, NovaMusicPlaybackService::class.java)
                context.startService(serviceIntent)
            } catch (_: Exception) {}
        }
    }

    fun bindPlayer(exoPlayer: ExoPlayer, context: Context) {
        player = exoPlayer
        appContext = context

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressPolling() else stopProgressPolling()
                notifyWidget()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentTrackInfo()
                notifyWidget()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.update { it.copy(durationMs = exoPlayer.duration.coerceAtLeast(0L)) }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _state.update { it.copy(repeatMode = repeatMode) }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _state.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
            }
        })
    }

    fun unbindPlayer() {
        stopProgressPolling()
        player = null
    }

    fun playQueue(files: List<File>, startIndex: Int = 0) {
        val context = appContext ?: return
        ensureService(context)
        val p = player ?: return

        _state.update { it.copy(queue = files, currentIndex = startIndex) }

        p.clearMediaItems()
        files.forEach { file ->
            val metadata = MediaMetadata.Builder()
                .setTitle(file.nameWithoutExtension)
                .setArtist("NovaFiles Audio")
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(file))
                .setMediaMetadata(metadata)
                .build()

            p.addMediaItem(mediaItem)
        }

        p.seekToDefaultPosition(startIndex)
        p.prepare()
        p.play()
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun seekTo(positionMs: Long) {
        val p = player ?: return
        p.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipToNext() {
        val p = player ?: return
        if (p.hasNextMediaItem()) {
            p.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val p = player ?: return
        if (p.currentPosition > 3000L) {
            p.seekTo(0L)
        } else if (p.hasPreviousMediaItem()) {
            p.seekToPreviousMediaItem()
        }
    }

    fun toggleRepeat() {
        val p = player ?: return
        val nextMode = when (p.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        p.repeatMode = nextMode
    }

    fun toggleShuffle() {
        val p = player ?: return
        p.shuffleModeEnabled = !p.shuffleModeEnabled
    }

    fun toggleFavorite() {
        _state.update { it.copy(isFavorite = !it.isFavorite) }
        notifyWidget()
    }

    private fun updateCurrentTrackInfo() {
        val p = player ?: return
        val context = appContext ?: return
        val idx = p.currentMediaItemIndex
        val queue = _state.value.queue

        if (idx in queue.indices) {
            val file = queue[idx]
            scope.launch {
                val metadata = AudioMetadataHelper.extractMetadata(context, file)
                _state.update {
                    it.copy(
                        currentTrack = metadata,
                        currentFilePath = file.absolutePath,
                        currentIndex = idx,
                        durationMs = p.duration.coerceAtLeast(0L)
                    )
                }
                notifyWidget()
            }
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                player?.let { p ->
                    _state.update {
                        it.copy(
                            currentPositionMs = p.currentPosition.coerceAtLeast(0L),
                            durationMs = p.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun notifyWidget() {
        appContext?.let { ctx ->
            val intent = Intent(ACTION_WIDGET_UPDATE).apply {
                setPackage(ctx.packageName)
            }
            ctx.sendBroadcast(intent)
        }
    }
}
