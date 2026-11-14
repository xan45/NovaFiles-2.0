package com.novafiles.feature.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLibraryTest {

    private fun createDummyVideo(
        id: String,
        name: String,
        durationMs: Long,
        sizeBytes: Long,
        dateModified: Long
    ): VideoItem {
        return VideoItem(
            id = id,
            path = "/storage/emulated/0/Movies/$name",
            name = name,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            dateModified = dateModified,
            dateAdded = dateModified,
            width = 1920,
            height = 1080,
            mimeType = "video/mp4",
            folderPath = "/storage/emulated/0/Movies",
            folderName = "Movies"
        )
    }

    @Test
    fun testSortingByName() {
        val v1 = createDummyVideo("1", "Alpha.mp4", 1000L, 100L, 1000L)
        val v2 = createDummyVideo("2", "Beta.mp4", 2000L, 200L, 2000L)
        val v3 = createDummyVideo("3", "Gamma.mp4", 3000L, 300L, 3000L)
        val list = listOf(v2, v1, v3)

        val asc = list.sortedWith(
            compareBy<VideoItem> { it.name.lowercase() }
        )
        assertEquals(listOf("Alpha.mp4", "Beta.mp4", "Gamma.mp4"), asc.map { it.name })

        val desc = list.sortedWith(
            compareByDescending<VideoItem> { it.name.lowercase() }
        )
        assertEquals(listOf("Gamma.mp4", "Beta.mp4", "Alpha.mp4"), desc.map { it.name })
    }

    @Test
    fun testSortingByDuration() {
        val vShort = createDummyVideo("1", "short.mp4", 10_000L, 100L, 1000L)
        val vMedium = createDummyVideo("2", "med.mp4", 60_000L, 200L, 2000L)
        val vLong = createDummyVideo("3", "long.mp4", 300_000L, 300L, 3000L)
        val list = listOf(vMedium, vLong, vShort)

        val asc = list.sortedBy { it.durationMs }
        assertEquals(listOf("short.mp4", "med.mp4", "long.mp4"), asc.map { it.name })

        val desc = list.sortedByDescending { it.durationMs }
        assertEquals(listOf("long.mp4", "med.mp4", "short.mp4"), desc.map { it.name })
    }

    @Test
    fun testSortingBySize() {
        val vSmall = createDummyVideo("1", "s.mp4", 1000L, 1_000L, 1000L)
        val vMed = createDummyVideo("2", "m.mp4", 1000L, 50_000L, 2000L)
        val vBig = createDummyVideo("3", "b.mp4", 1000L, 999_999L, 3000L)
        val list = listOf(vBig, vSmall, vMed)

        val asc = list.sortedBy { it.sizeBytes }
        assertEquals(listOf("s.mp4", "m.mp4", "b.mp4"), asc.map { it.name })

        val desc = list.sortedByDescending { it.sizeBytes }
        assertEquals(listOf("b.mp4", "m.mp4", "s.mp4"), desc.map { it.name })
    }

    @Test
    fun testSortingByDate() {
        val vOld = createDummyVideo("1", "old.mp4", 1000L, 1000L, 10_000L)
        val vMid = createDummyVideo("2", "mid.mp4", 1000L, 1000L, 50_000L)
        val vNew = createDummyVideo("3", "new.mp4", 1000L, 1000L, 99_000L)
        val list = listOf(vMid, vOld, vNew)

        val asc = list.sortedBy { it.dateModified }
        assertEquals(listOf("old.mp4", "mid.mp4", "new.mp4"), asc.map { it.name })

        val desc = list.sortedByDescending { it.dateModified }
        assertEquals(listOf("new.mp4", "mid.mp4", "old.mp4"), desc.map { it.name })
    }

    @Test
    fun testSearchFiltering() {
        val v1 = createDummyVideo("1", "Nature_Wildlife_4K.mp4", 1000L, 1000L, 1000L)
        val v2 = createDummyVideo("2", "Vacation_Trip.mp4", 1000L, 1000L, 2000L)
        val v3 = createDummyVideo("3", "Tech_Keynote.mkv", 1000L, 1000L, 3000L)
        val list = listOf(v1, v2, v3)

        val query = "nature"
        val filtered = list.filter { it.name.contains(query, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("Nature_Wildlife_4K.mp4", filtered.first().name)
    }

    @Test
    fun testPlaylistOperations() {
        val playlist = VideoPlaylist(
            id = "pl_1",
            name = "Favorites",
            createdAt = 1000L,
            videoPaths = listOf("/path/v1.mp4", "/path/v2.mp4")
        )

        assertEquals(2, playlist.videoPaths.size)

        // Add item
        val added = playlist.copy(videoPaths = playlist.videoPaths + "/path/v3.mp4")
        assertEquals(3, added.videoPaths.size)
        assertTrue(added.videoPaths.contains("/path/v3.mp4"))

        // Remove item
        val removed = added.copy(videoPaths = added.videoPaths.filter { it != "/path/v1.mp4" })
        assertEquals(2, removed.videoPaths.size)
        assertFalse(removed.videoPaths.contains("/path/v1.mp4"))

        // Reorder
        val reordered = removed.copy(videoPaths = listOf("/path/v3.mp4", "/path/v2.mp4"))
        assertEquals("/path/v3.mp4", reordered.videoPaths[0])
        assertEquals("/path/v2.mp4", reordered.videoPaths[1])
    }

    @Test
    fun testPlaybackResumePoint() {
        val point = PlaybackResumePoint(
            positionMs = 120_000L,
            durationMs = 240_000L
        )

        assertEquals(120_000L, point.positionMs)
        assertEquals(240_000L, point.durationMs)
    }

    @Test
    fun testDefaultVideoPreferences() {
        val prefs = VideoPreferences()
        assertEquals(VideoViewMode.GRID, prefs.viewMode)
        assertEquals(VideoThumbnailSize.MEDIUM, prefs.thumbnailSize)
        assertEquals(3, prefs.thumbnailSize.columns)
        assertTrue(prefs.showDurationOverlay)
        assertTrue(prefs.rememberPlaybackPosition)
        assertEquals(1.0f, prefs.defaultPlaybackSpeed, 0.001f)
        assertTrue(prefs.defaultSubtitleEnabled)
        assertEquals(VideoOrientationPref.AUTO, prefs.orientationPreference)
    }
}
