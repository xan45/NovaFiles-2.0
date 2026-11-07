package com.novafiles.feature.music

import android.os.Environment
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
    val searchQuery: String = ""
)

class MusicPlayerViewModel(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicPlayerUiState())
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
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun toggleQueueSheet(visible: Boolean) {
        _uiState.update { it.copy(isQueueSheetVisible = visible) }
    }
}
