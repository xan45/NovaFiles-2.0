package com.novafiles.feature.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GalleryUiStateTest {

    @Test
    fun testGalleryItemCreation() {
        val file = File("/storage/emulated/0/DCIM/Camera/IMG_20261107_120000.jpg")
        val item = GalleryItem(
            file = file,
            album = "Camera",
            sizeBytes = 2_500_000L
        )

        assertEquals("IMG_20261107_120000.jpg", item.name)
        assertEquals("Camera", item.album)
        assertEquals(2_500_000L, item.sizeBytes)
        assertEquals(file.absolutePath, item.id)
    }

    @Test
    fun testGalleryUiStateSelectionCount() {
        val state = GalleryUiState(
            selectedItemIds = setOf("id1", "id2", "id3"),
            isSelectionMode = true
        )

        assertEquals(3, state.selectedCount)
        assertTrue(state.isSelectionMode)
    }

    @Test
    fun testGalleryUiStateFilteredItems() {
        val f1 = File("/storage/emulated/0/DCIM/Camera/pic1.jpg")
        val f2 = File("/storage/emulated/0/Pictures/Screenshots/sc1.png")
        val item1 = GalleryItem(file = f1, album = "Camera")
        val item2 = GalleryItem(file = f2, album = "Screenshots")

        val state = GalleryUiState(
            allItems = listOf(item1, item2),
            filteredItems = listOf(item2),
            selectedAlbum = "Screenshots"
        )

        assertEquals(1, state.filteredItems.size)
        assertEquals("Screenshots", state.filteredItems[0].album)
        assertFalse(state.isSelectionMode)
    }
}
