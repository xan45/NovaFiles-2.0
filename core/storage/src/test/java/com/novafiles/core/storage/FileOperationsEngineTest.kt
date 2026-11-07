package com.novafiles.core.storage

import com.novafiles.core.model.BatchRenameMode
import com.novafiles.core.model.ConflictResolution
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileOperationsEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val engine = FileOperationsEngine()
    private val archiveEngine = ArchiveEngine()

    @Test
    fun `copyFiles copies files to destination with non-colliding names`() = runTest {
        val srcFile = tempFolder.newFile("test_document.txt")
        srcFile.writeText("NovaFiles 2.0 Test Content")

        val targetDir = tempFolder.newFolder("target_dir")

        val results = engine.copyFiles(
            sources = listOf(srcFile),
            targetDir = targetDir,
            conflictResolution = ConflictResolution.RENAME_NEW
        ).toList()

        val copiedFile = File(targetDir, "test_document.txt")
        assertTrue("Copied file should exist", copiedFile.exists())
        assertEquals("NovaFiles 2.0 Test Content", copiedFile.readText())

        // Copy a second time to trigger collision rename
        val secondResults = engine.copyFiles(
            sources = listOf(srcFile),
            targetDir = targetDir,
            conflictResolution = ConflictResolution.RENAME_NEW
        ).toList()

        val secondCopiedFile = File(targetDir, "test_document (1).txt")
        assertTrue("Non-colliding renamed copy should exist", secondCopiedFile.exists())
    }

    @Test
    fun `batchRename correctly applies enumeration to files`() = runTest {
        val file1 = tempFolder.newFile("image_a.jpg")
        val file2 = tempFolder.newFile("image_b.jpg")

        val renamed = engine.batchRename(
            targets = listOf(file1, file2),
            mode = BatchRenameMode.Enumeration(baseName = "Vacation", startNumber = 1, digits = 3)
        )

        assertEquals("Vacation_001.jpg", renamed[0].name)
        assertEquals("Vacation_002.jpg", renamed[1].name)
    }

    @Test
    fun `createZipArchive and extractArchive roundtrips successfully`() = runTest {
        val sourceDir = tempFolder.newFolder("zip_source")
        val file1 = File(sourceDir, "note1.txt").apply { writeText("Hello Note 1") }
        val file2 = File(sourceDir, "note2.txt").apply { writeText("Hello Note 2") }

        val targetZip = File(tempFolder.root, "test_archive.zip")

        // 1. Create ZIP
        archiveEngine.createZipArchive(
            sources = listOf(file1, file2),
            targetZipFile = targetZip
        ).toList()

        assertTrue("ZIP file should be created", targetZip.exists())

        // 2. Extract ZIP
        val extractDir = tempFolder.newFolder("zip_extracted")
        archiveEngine.extractArchive(
            archiveFile = targetZip,
            destinationDir = extractDir
        ).toList()

        val extracted1 = File(extractDir, "note1.txt")
        val extracted2 = File(extractDir, "note2.txt")
        assertTrue("Extracted file 1 should exist", extracted1.exists())
        assertTrue("Extracted file 2 should exist", extracted2.exists())
        assertEquals("Hello Note 1", extracted1.readText())
        assertEquals("Hello Note 2", extracted2.readText())
    }

    @Test
    fun `isPathSafe blocks path traversal outside base directory`() {
        val base = tempFolder.newFolder("safe_base")
        val safePath = File(base, "child/file.txt").absolutePath
        val maliciousPath = File(base, "../../../etc/passwd").absolutePath

        val dataSource = FileSystemDataSource()
        assertTrue(dataSource.isPathSafe(base, safePath))
        assertFalse(dataSource.isPathSafe(base, maliciousPath))
    }
}
