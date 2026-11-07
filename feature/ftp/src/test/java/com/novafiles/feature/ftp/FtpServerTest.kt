package com.novafiles.feature.ftp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FtpServerTest {

    @Test
    fun testFtpServerConfigDefaults() {
        val config = FtpServerConfig()
        assertEquals(2121, config.port)
        assertEquals("novafiles", config.username)
        assertTrue(config.password.isNotBlank())
        assertEquals(8, config.password.length)
        assertTrue(config.isReadOnly)
        assertFalse(config.isAnonymousAllowed)
    }

    @Test
    fun testSandboxPathContainment() {
        val rootDir = File.createTempFile("ftp_sandbox_root", "").apply {
            delete()
            mkdirs()
        }
        val safeSubDir = File(rootDir, "documents").apply { mkdirs() }
        val safeFile = File(safeSubDir, "sample.txt").apply { writeText("Hello") }

        val outsideFile = File.createTempFile("outside_test", ".txt")

        assertTrue(FtpServerEngine.isWithinSandbox(safeFile, rootDir))
        assertTrue(FtpServerEngine.isWithinSandbox(safeSubDir, rootDir))
        assertTrue(FtpServerEngine.isWithinSandbox(rootDir, rootDir))

        assertFalse(FtpServerEngine.isWithinSandbox(outsideFile, rootDir))

        // Path traversal attempt
        val traversalFile = File(rootDir, "../traversal.txt")
        assertFalse(FtpServerEngine.isWithinSandbox(traversalFile, rootDir))

        rootDir.deleteRecursively()
        outsideFile.delete()
    }
}
