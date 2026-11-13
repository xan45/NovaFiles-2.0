package com.novafiles.feature.music

import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.media.MusicPlaybackManager
import com.novafiles.core.media.MusicState
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import com.novafiles.data.files.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class MusicLibraryTab {
    TRACKS,
    ALBUMS,
    ARTISTS,
    FOLDERS
}

data class MusicPlayerUiState(
    val playback: MusicState = MusicState(),
    val libraryTracks: List<FileItem> = emptyList(),
    val currentTab: MusicLibraryTab = MusicLibraryTab.TRACKS,
    val isQueueSheetVisible: Boolean = false,
    val searchQuery: String = "",
    val sleepTimerMinutesRemaining: Int? = null
)

class MusicPlayerViewModel(
    private val fileRepository: FileRepository,
    private val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    companion object {
        private const val KEY_MUSIC_TAB = "key_music_current_tab"
    }

    private val restoredTab = savedStateHandle?.get<String>(KEY_MUSIC_TAB)?.let {
        runCatching { MusicLibraryTab.valueOf(it) }.getOrNull()
    } ?: MusicLibraryTab.TRACKS

    private val _uiState = MutableStateFlow(MusicPlayerUiState(currentTab = restoredTab))
    val uiState: StateFlow<MusicPlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            MusicPlaybackManager.state.collect { playback ->
                _uiState.update { it.copy(playback = playback) }
            }
        }
        loadMusicLibrary()
    }

    fun loadMusicLibrary() {
        viewModelScope.launch {
            val musicDir = File(Environment.getExternalStorageDirectory(), "Music")
            val targetDir = if (musicDir.exists()) musicDir.absolutePath else Environment.getExternalStorageDirectory().absolutePath
            fileRepository.getFiles(targetDir).collect { result ->
                if (result is com.novafiles.core.common.NovaResult.Success) {
                    val tracks = result.data.filter { it.fileType == FileType.AUDIO }
                    _uiState.update { it.copy(libraryTracks = tracks) }
                }
            }
        }
    }

    fun playTrack(track: FileItem) {
        val tracks = _uiState.value.libraryTracks.map { File(it.path) }
        val index = tracks.indexOfFirst { it.absolutePath == track.path }.coerceAtLeast(0)
        MusicPlaybackManager.playQueue(tracks, index)
    }

    fun togglePlayPause() = MusicPlaybackManager.togglePlayPause()
    fun seekTo(positionMs: Long) = MusicPlaybackManager.seekTo(positionMs)
    fun skipToNext() = MusicPlaybackManager.skipToNext()
    fun skipToPrevious() = MusicPlaybackManager.skipToPrevious()
    fun toggleRepeat() = MusicPlaybackManager.toggleRepeat()
    fun toggleShuffle() = MusicPlaybackManager.toggleShuffle()
    fun toggleFavorite() = MusicPlaybackManager.toggleFavorite()

    fun setLibraryTab(tab: MusicLibraryTab) {
        savedStateHandle?.set(KEY_MUSIC_TAB, tab.name)
        _uiState.update { it.copy(currentTab = tab) }
    }

    private var sleepTimerJob: Job? = null

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.update { it.copy(sleepTimerMinutesRemaining = null) }
            return
        }
        _uiState.update { it.copy(sleepTimerMinutesRemaining = minutes) }
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (isActive && remaining > 0) {
                delay(60_000L)
                remaining--
                _uiState.update { it.copy(sleepTimerMinutesRemaining = if (remaining > 0) remaining else null) }
            }
            if (isActive && MusicPlaybackManager.state.value.isPlaying) {
                MusicPlaybackManager.togglePlayPause()
            }
        }
    }

    fun toggleQueueSheet(visible: Boolean) {
        _uiState.update { it.copy(isQueueSheetVisible = visible) }
    }
}
