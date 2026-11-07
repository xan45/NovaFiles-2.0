package com.novafiles.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class FileFormattersTest {

    @Test
    fun `formatBytes returns correct units and precision`() {
        assertEquals("0 B", FileFormatters.formatBytes(0L))
        assertEquals("512 B", FileFormatters.formatBytes(512L))
        assertEquals("1 KB", FileFormatters.formatBytes(1024L))
        assertEquals("1.5 KB", FileFormatters.formatBytes(1536L))
        assertEquals("20.8 GB", FileFormatters.formatBytes(22333829939L))
        assertEquals("69.8 GB", FileFormatters.formatBytes(74948742348L))
        assertEquals("512 GB", FileFormatters.formatBytes(549755813888L))
    }

    @Test
    fun `formatDuration formats timestamps into standard media durations`() {
        assertEquals("00:00", FileFormatters.formatDuration(0L))
        assertEquals("00:06", FileFormatters.formatDuration(6000L))
        assertEquals("03:38", FileFormatters.formatDuration(218000L))
        assertEquals("01:59:21", FileFormatters.formatDuration(7161000L))
    }
}
