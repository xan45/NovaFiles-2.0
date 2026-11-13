package com.novafiles.feature.music

import com.novafiles.core.media.AudioTrackMetadata
import com.novafiles.core.media.MusicState
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MusicPlayerTest {

    @Test
    fun testAudioTrackMetadata() {
        val track = AudioTrackMetadata(
            title = "Midnight Odyssey",
            artist = "Nova Ensemble",
            album = "Cosmic Waves",
            durationMs = 210000L,
            formattedDuration = "03:30",
            bitrateKbps = 320,
            sampleRateHz = 44100
        )

        assertEquals("Midnight Odyssey", track.title)
        assertEquals("Nova Ensemble", track.artist)
        assertEquals("Cosmic Waves", track.album)
        assertEquals("03:30", track.formattedDuration)
    }

    @Test
    fun testMusicStateProgressFraction() {
        val state = MusicState(
            currentPositionMs = 60000L,
            durationMs = 120000L,
            isPlaying = true
        )

        assertEquals(0.5f, state.progressFraction, 0.001f)
        assertTrue(state.isPlaying)
        assertFalse(state.isFavorite)
    }

    @Test
    fun testMusicTrackAndGroupings() {
        val fileItem1 = FileItem(
            id = "/music/song1.mp3",
            name = "song1.mp3",
            path = "/music/song1.mp3",
            sizeBytes = 5000000L,
            fileType = FileType.AUDIO
        )
        val track1 = MusicTrack(
            fileItem = fileItem1,
            title = "Song One",
            artist = "Artist A",
            album = "Album Alpha",
            durationMs = 180000L,
            format = "MP3",
            folderPath = "/music",
            folderName = "Music"
        )

        val fileItem2 = FileItem(
            id = "/music/song2.flac",
            name = "song2.flac",
            path = "/music/song2.flac",
            sizeBytes = 25000000L,
            fileType = FileType.AUDIO
        )
        val track2 = MusicTrack(
            fileItem = fileItem2,
            title = "Song Two",
            artist = "Artist B",
            album = "Album Beta",
            durationMs = 240000L,
            format = "FLAC",
            folderPath = "/music",
            folderName = "Music"
        )

        val album = MusicAlbum(
            name = "Album Alpha",
            artist = "Artist A",
            trackCount = 1,
            tracks = listOf(track1),
            firstTrackFile = File(track1.fileItem.path)
        )
        assertEquals("Album Alpha", album.name)
        assertEquals(1, album.trackCount)

        val artist = MusicArtist(
            name = "Artist B",
            trackCount = 1,
            albumCount = 1,
            tracks = listOf(track2)
        )
        assertEquals("Artist B", artist.name)
        assertEquals(1, artist.trackCount)

        val folder = MusicFolder(
            name = "Music",
            path = "/music",
            trackCount = 2,
            totalSizeBytes = 30000000L,
            tracks = listOf(track1, track2)
        )
        assertEquals("Music", folder.name)
        assertEquals(2, folder.trackCount)
        assertEquals(30000000L, folder.totalSizeBytes)
    }

    @Test
    fun testMusicPlayerUiStateSearchFiltering() {
        val t1 = MusicTrack(
            fileItem = FileItem(id = "1", name = "acoustic.mp3", path = "/1.mp3"),
            title = "Summer Acoustic",
            artist = "Sun Ensemble",
            album = "Seasons",
            durationMs = 120000L,
            format = "MP3",
            folderPath = "/Music",
            folderName = "Music"
        )
        val t2 = MusicTrack(
            fileItem = FileItem(id = "2", name = "winter.wav", path = "/2.wav"),
            title = "Winter Chill",
            artist = "Frost Crew",
            album = "Seasons",
            durationMs = 150000L,
            format = "WAV",
            folderPath = "/Music/Winter",
            folderName = "Winter"
        )

        val state = MusicPlayerUiState(
            tracks = listOf(t1, t2),
            searchQuery = "summer"
        )

        assertEquals(1, state.filteredTracks.size)
        assertEquals("Summer Acoustic", state.filteredTracks.first().title)

        val artistFilteredState = state.copy(searchQuery = "Frost")
        assertEquals(1, artistFilteredState.filteredTracks.size)
        assertEquals("Winter Chill", artistFilteredState.filteredTracks.first().title)

        val albumFilteredState = state.copy(searchQuery = "Seasons")
        assertEquals(2, albumFilteredState.filteredTracks.size)

        assertFalse(state.isNowPlayingExpanded)
        val expandedState = state.copy(isNowPlayingExpanded = true)
        assertTrue(expandedState.isNowPlayingExpanded)
    }
}
