package com.novafiles.feature.analyzer

import androidx.compose.ui.graphics.Color
import com.novafiles.core.model.FileType
import org.junit.Assert.assertEquals
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
}
