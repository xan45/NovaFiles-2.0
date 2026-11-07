package com.novafiles.feature.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StorageCleanerTest {

    @Test
    fun testJunkItemCalculations() {
        val tempFile = File.createTempFile("test_cache", ".tmp")
        tempFile.writeBytes(ByteArray(4096))

        val junkItem = JunkItem(
            file = tempFile,
            category = JunkCategoryType.TEMP_LOGS,
            sizeBytes = 4096L
        )

        assertEquals(tempFile.name, junkItem.name)
        assertTrue(junkItem.name.startsWith("test_cache"))
        assertEquals(JunkCategoryType.TEMP_LOGS, junkItem.category)
        assertEquals(4096L, junkItem.sizeBytes)
        assertFalse(junkItem.isDirectory)

        tempFile.delete()
    }

    @Test
    fun testJunkCategorySummation() {
        val item1 = JunkItem(
            file = File("/tmp/1.tmp"),
            category = JunkCategoryType.TEMP_LOGS,
            sizeBytes = 1000L
        )
        val item2 = JunkItem(
            file = File("/tmp/2.tmp"),
            category = JunkCategoryType.TEMP_LOGS,
            sizeBytes = 2500L
        )

        val category = JunkCategory(
            type = JunkCategoryType.TEMP_LOGS,
            title = "Temporary Files",
            description = "Residual log and temp files",
            items = listOf(item1, item2),
            isSelected = true
        )

        assertEquals(3500L, category.totalSizeBytes)
        assertEquals(2, category.items.size)
        assertTrue(category.isSelected)
    }

    @Test
    fun testCleanerStateTransitions() {
        val state = StorageCleanerUiState(
            state = CleanerState.READY_TO_CLEAN,
            totalCleanableBytes = 50_000_000L,
            selectedCleanableBytes = 50_000_000L
        )

        assertEquals(CleanerState.READY_TO_CLEAN, state.state)
        assertEquals(50_000_000L, state.totalCleanableBytes)
        assertEquals(50_000_000L, state.selectedCleanableBytes)
    }
}
