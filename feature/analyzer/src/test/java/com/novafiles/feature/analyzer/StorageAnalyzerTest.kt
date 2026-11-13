package com.novafiles.feature.analyzer

import androidx.compose.ui.graphics.Color
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.FileType
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StorageAnalyzerTest {

    @Test
    fun testCategoryStorageItemCalculations() {
        val category = CategoryStorageItem(
            type = FileType.IMAGE,
            name = "Photos",
            sizeBytes = 20_800_000_000L,
            fileCount = 1420,
            percentageOfUsed = 0.30f,
            color = Color(0xFF8A6CFF)
        )

        assertEquals("Photos", category.name)
        assertEquals(FileType.IMAGE, category.type)
        assertEquals(20_800_000_000L, category.sizeBytes)
        assertEquals(1420, category.fileCount)
        assertEquals(0.30f, category.percentageOfUsed, 0.001f)
    }

    @Test
    fun testLargeFileItemModel() {
        val tempFile = File.createTempFile("test_large_file", ".mp4")
        tempFile.writeBytes(ByteArray(1024))

        val largeFile = LargeFileItem(file = tempFile)

        assertEquals(tempFile.name, largeFile.name)
        assertEquals(tempFile.absolutePath, largeFile.path)
        assertEquals(1024L, largeFile.sizeBytes)
        assertEquals(FileType.VIDEO, largeFile.fileType)

        tempFile.delete()
    }

    @Test
    fun testDeletedFileItemDaysRemaining() {
        val tempFile = File.createTempFile("test_deleted", ".jpg")
        val deletedItem = DeletedFileItem(
            file = tempFile,
            name = tempFile.name,
            path = tempFile.absolutePath,
            sizeBytes = 2_900_000L,
            isDirectory = false,
            daysRemaining = 24
        )

        assertEquals(24, deletedItem.daysRemaining)
        assertEquals(2_900_000L, deletedItem.sizeBytes)
        tempFile.delete()
    }

    @Test
    fun testCategoryBrowsingSortByName() {
        val file1 = FileItem(id = "/a.jpg", name = "banana.jpg", path = "/a.jpg", sizeBytes = 100L, lastModifiedMs = 1000L, fileType = FileType.IMAGE)
        val file2 = FileItem(id = "/b.jpg", name = "apple.jpg", path = "/b.jpg", sizeBytes = 200L, lastModifiedMs = 2000L, fileType = FileType.IMAGE)
        val file3 = FileItem(id = "/c.jpg", name = "cherry.jpg", path = "/c.jpg", sizeBytes = 50L, lastModifiedMs = 500L, fileType = FileType.IMAGE)

        val stateAsc = StorageAnalyzerUiState(
            selectedCategory = FileType.IMAGE,
            selectedCategoryFiles = listOf(file1, file2, file3),
            sortConfig = FileSortConfig(sortBy = SortBy.NAME, direction = SortDirection.ASCENDING)
        )
        val sortedAsc = stateAsc.displayedCategoryFiles
        assertEquals("apple.jpg", sortedAsc[0].name)
        assertEquals("banana.jpg", sortedAsc[1].name)
        assertEquals("cherry.jpg", sortedAsc[2].name)

        val stateDesc = stateAsc.copy(
            sortConfig = FileSortConfig(sortBy = SortBy.NAME, direction = SortDirection.DESCENDING)
        )
        val sortedDesc = stateDesc.displayedCategoryFiles
        assertEquals("cherry.jpg", sortedDesc[0].name)
        assertEquals("banana.jpg", sortedDesc[1].name)
        assertEquals("apple.jpg", sortedDesc[2].name)
    }

    @Test
    fun testCategoryBrowsingSortBySize() {
        val file1 = FileItem(id = "/a.png", name = "small.png", path = "/a.png", sizeBytes = 100L, lastModifiedMs = 1000L, fileType = FileType.IMAGE)
        val file2 = FileItem(id = "/b.png", name = "large.png", path = "/b.png", sizeBytes = 5000L, lastModifiedMs = 2000L, fileType = FileType.IMAGE)
        val file3 = FileItem(id = "/c.png", name = "medium.png", path = "/c.png", sizeBytes = 1000L, lastModifiedMs = 500L, fileType = FileType.IMAGE)

        val stateDesc = StorageAnalyzerUiState(
            selectedCategory = FileType.IMAGE,
            selectedCategoryFiles = listOf(file1, file2, file3),
            sortConfig = FileSortConfig(sortBy = SortBy.SIZE, direction = SortDirection.DESCENDING)
        )
        val sorted = stateDesc.displayedCategoryFiles
        assertEquals("large.png", sorted[0].name)
        assertEquals("medium.png", sorted[1].name)
        assertEquals("small.png", sorted[2].name)
    }

    @Test
    fun testCategoryBrowsingSearchFilter() {
        val file1 = FileItem(id = "/doc1.pdf", name = "invoice_nov_2026.pdf", path = "/doc1.pdf", sizeBytes = 100L, lastModifiedMs = 1000L, fileType = FileType.DOCUMENT)
        val file2 = FileItem(id = "/doc2.pdf", name = "resume_official.pdf", path = "/doc2.pdf", sizeBytes = 200L, lastModifiedMs = 2000L, fileType = FileType.DOCUMENT)
        val file3 = FileItem(id = "/doc3.txt", name = "notes_invoice.txt", path = "/doc3.txt", sizeBytes = 50L, lastModifiedMs = 500L, fileType = FileType.DOCUMENT)

        val state = StorageAnalyzerUiState(
            selectedCategory = FileType.DOCUMENT,
            selectedCategoryFiles = listOf(file1, file2, file3),
            searchQuery = "invoice"
        )
        val filtered = state.displayedCategoryFiles
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.name == "invoice_nov_2026.pdf" })
        assertTrue(filtered.any { it.name == "notes_invoice.txt" })
        assertFalse(filtered.any { it.name == "resume_official.pdf" })
    }
}
