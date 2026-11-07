package com.novafiles.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FileTypeTest {

    @Test
    fun `fromExtension accurately maps standard extensions`() {
        assertEquals(FileType.IMAGE, FileType.fromExtension("jpg"))
        assertEquals(FileType.IMAGE, FileType.fromExtension("PNG"))
        assertEquals(FileType.VIDEO, FileType.fromExtension("mp4"))
        assertEquals(FileType.AUDIO, FileType.fromExtension("mp3"))
        assertEquals(FileType.AUDIO, FileType.fromExtension("flac"))
        assertEquals(FileType.DOCUMENT, FileType.fromExtension("pdf"))
        assertEquals(FileType.ARCHIVE, FileType.fromExtension("zip"))
        assertEquals(FileType.ARCHIVE, FileType.fromExtension("7z"))
        assertEquals(FileType.APK, FileType.fromExtension("apk"))
        assertEquals(FileType.OTHER, FileType.fromExtension("xyzunknown"))
        assertEquals(FileType.OTHER, FileType.fromExtension(null))
    }

    @Test
    fun `StorageStats computes freeBytes and percentage accurately`() {
        val stats = StorageStats(
            usedBytes = 75_000_000_000L,
            totalBytes = 100_000_000_000L
        )
        assertEquals(25_000_000_000L, stats.freeBytes)
        assertEquals(0.75f, stats.usedPercentage, 0.001f)
        assertEquals(75, stats.usedPercentageInt)
    }
}
