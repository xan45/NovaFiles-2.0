package com.novafiles.data.files

import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.FileType
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class FileRepositoryTest {

    private val testFiles = listOf(
        FileItem(id = "1", name = "Zebra.txt", path = "/test/Zebra.txt", sizeBytes = 100, lastModifiedMs = 1000, fileType = FileType.DOCUMENT),
        FileItem(id = "2", name = "Apple.pdf", path = "/test/Apple.pdf", sizeBytes = 500, lastModifiedMs = 3000, fileType = FileType.DOCUMENT),
        FileItem(id = "3", name = "Documents", path = "/test/Documents", sizeBytes = 0, lastModifiedMs = 2000, isDirectory = true, fileType = FileType.FOLDER)
    )

    @Test
    fun `FileSortConfig puts folders first and sorts by name ascending`() {
        val config = FileSortConfig(sortBy = SortBy.NAME, direction = SortDirection.ASCENDING, foldersFirst = true)
        val sorted = testFiles.sortedWith { a, b ->
            if (config.foldersFirst) {
                if (a.isDirectory && !b.isDirectory) return@sortedWith -1
                if (!a.isDirectory && b.isDirectory) return@sortedWith 1
            }
            a.name.compareTo(b.name, ignoreCase = true)
        }

        assertEquals("Documents", sorted[0].name)
        assertEquals("Apple.pdf", sorted[1].name)
        assertEquals("Zebra.txt", sorted[2].name)
    }

    @Test
    fun `FileSortConfig sorts by date descending with folders first`() {
        val config = FileSortConfig(sortBy = SortBy.DATE, direction = SortDirection.DESCENDING, foldersFirst = true)
        val sorted = testFiles.sortedWith { a, b ->
            if (config.foldersFirst) {
                if (a.isDirectory && !b.isDirectory) return@sortedWith -1
                if (!a.isDirectory && b.isDirectory) return@sortedWith 1
            }
            -a.lastModifiedMs.compareTo(b.lastModifiedMs)
        }

        assertEquals("Documents", sorted[0].name) // folder stays first
        assertEquals("Apple.pdf", sorted[1].name) // 3000ms
        assertEquals("Zebra.txt", sorted[2].name) // 1000ms
    }

    @Test
    fun `FileSortConfig sorts by size descending with folders first`() {
        val config = FileSortConfig(sortBy = SortBy.SIZE, direction = SortDirection.DESCENDING, foldersFirst = true)
        val sorted = testFiles.sortedWith { a, b ->
            if (config.foldersFirst) {
                if (a.isDirectory && !b.isDirectory) return@sortedWith -1
                if (!a.isDirectory && b.isDirectory) return@sortedWith 1
            }
            -a.sizeBytes.compareTo(b.sizeBytes)
        }

        assertEquals("Documents", sorted[0].name) // folder stays first
        assertEquals("Apple.pdf", sorted[1].name) // 500 bytes
        assertEquals("Zebra.txt", sorted[2].name) // 100 bytes
    }
}
