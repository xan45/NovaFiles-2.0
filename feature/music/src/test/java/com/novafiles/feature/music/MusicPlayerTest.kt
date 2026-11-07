package com.novafiles.feature.music

import com.novafiles.core.media.AudioTrackMetadata
import com.novafiles.core.media.MusicState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
