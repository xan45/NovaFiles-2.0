package com.novafiles.feature.video

import com.novafiles.core.media.VideoTechnicalDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VideoMetadataTest {

    @Test
    fun testVideoTechnicalDetailsCreation() {
        val details = VideoTechnicalDetails(
            fileName = "sample_4k.mp4",
            resolution = "3840x2160 (16:9)",
            aspectRatio = "16:9",
            codec = "H.265 (HEVC)",
            videoBitrate = "18.5 Mbps",
            frameRate = "60.00 fps",
            durationMs = 125000L,
            formattedDuration = "02:05",
            fileSizeFormatted = "284.5 MB"
        )

        assertEquals("sample_4k.mp4", details.fileName)
        assertEquals("3840x2160 (16:9)", details.resolution)
        assertEquals("H.265 (HEVC)", details.codec)
        assertEquals("18.5 Mbps", details.videoBitrate)
        assertEquals("02:05", details.formattedDuration)
    }

    @Test
    fun testVideoUiStateTimeFormatting() {
        val state = VideoPlayerUiState(
            currentPositionMs = 65000L,
            durationMs = 185000L
        )

        assertEquals("01:05", state.formattedCurrentTime)
        assertEquals("03:05", state.formattedTotalTime)
        assertNotNull(state.progressFraction)
    }
}
