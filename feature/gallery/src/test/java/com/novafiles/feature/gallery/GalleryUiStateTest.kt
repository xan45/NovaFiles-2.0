package com.novafiles.feature.gallery

import com.novafiles.core.model.SortDirection
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
            sizeBytes = 2_500_000L,
            lastModified = 1700000000000L,
            dateTaken = 1700000000000L,
            width = 1920,
            height = 1080,
            mimeType = "image/jpeg"
        )

        assertEquals("IMG_20261107_120000.jpg", item.name)
        assertEquals("Camera", item.album)
        assertEquals(2_500_000L, item.sizeBytes)
        assertEquals(file.absolutePath, item.id)
        assertEquals(1920, item.width)
        assertEquals(1080, item.height)
        assertEquals("image/jpeg", item.mimeType)
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
    fun testActiveImageListSortingByName() {
        val itemA = GalleryItem(file = File("/path/alpha.jpg"), name = "alpha.jpg", album = "A", sizeBytes = 100L)
        val itemB = GalleryItem(file = File("/path/beta.jpg"), name = "beta.jpg", album = "A", sizeBytes = 200L)

        val stateAsc = GalleryUiState(
            allItems = listOf(itemB, itemA),
            preferences = GalleryPreferences(
                sortConfig = GallerySortConfig(sortBy = GallerySortBy.NAME, direction = SortDirection.ASCENDING)
            )
        )
        assertEquals("alpha.jpg", stateAsc.activeImageList[0].name)
        assertEquals("beta.jpg", stateAsc.activeImageList[1].name)

        val stateDesc = GalleryUiState(
            allItems = listOf(itemA, itemB),
            preferences = GalleryPreferences(
                sortConfig = GallerySortConfig(sortBy = GallerySortBy.NAME, direction = SortDirection.DESCENDING)
            )
        )
        assertEquals("beta.jpg", stateDesc.activeImageList[0].name)
        assertEquals("alpha.jpg", stateDesc.activeImageList[1].name)
    }

    @Test
    fun testActiveImageListSortingBySize() {
        val small = GalleryItem(file = File("/path/small.jpg"), name = "small.jpg", album = "A", sizeBytes = 100L)
        val large = GalleryItem(file = File("/path/large.jpg"), name = "large.jpg", album = "A", sizeBytes = 10_000L)

        val state = GalleryUiState(
            allItems = listOf(small, large),
            preferences = GalleryPreferences(
                sortConfig = GallerySortConfig(sortBy = GallerySortBy.SIZE, direction = SortDirection.DESCENDING)
            )
        )
        assertEquals("large.jpg", state.activeImageList[0].name)
        assertEquals("small.jpg", state.activeImageList[1].name)
    }

    @Test
    fun testSearchFiltering() {
        val item1 = GalleryItem(file = File("/path/vacation_beach.jpg"), name = "vacation_beach.jpg", album = "Trip")
        val item2 = GalleryItem(file = File("/path/city_night.jpg"), name = "city_night.jpg", album = "Trip")

        val state = GalleryUiState(
            allItems = listOf(item1, item2),
            searchQuery = "beach"
        )
        assertEquals(1, state.activeImageList.size)
        assertEquals("vacation_beach.jpg", state.activeImageList[0].name)
    }

    @Test
    fun testFavoritesFiltering() {
        val item1 = GalleryItem(file = File("/path/fav.jpg"), id = "/path/fav.jpg", name = "fav.jpg", album = "A")
        val item2 = GalleryItem(file = File("/path/other.jpg"), id = "/path/other.jpg", name = "other.jpg", album = "A")

        val state = GalleryUiState(
            allItems = listOf(item1, item2),
            favorites = setOf("/path/fav.jpg"),
            isOnlyFavorites = true
        )
        assertEquals(1, state.activeImageList.size)
        assertEquals("fav.jpg", state.activeImageList[0].name)
    }

    @Test
    fun testDateGrouping() {
        val now = System.currentTimeMillis()
        val itemToday = GalleryItem(file = File("/path/today.jpg"), name = "today.jpg", album = "A", lastModified = now)
        val itemYesterday = GalleryItem(file = File("/path/yesterday.jpg"), name = "yesterday.jpg", album = "A", lastModified = now - 86400000L)

        val state = GalleryUiState(
            allItems = listOf(itemToday, itemYesterday),
            preferences = GalleryPreferences(groupByDate = true)
        )
        val groups = state.dateGroups
        assertEquals(2, groups.size)
        assertEquals("Today", groups[0].title)
        assertEquals("Yesterday", groups[1].title)
    }

    @Test
    fun testUserAlbumDrilldown() {
        val item1 = GalleryItem(file = File("/path/img1.jpg"), id = "/path/img1.jpg", name = "img1.jpg", album = "A")
        val item2 = GalleryItem(file = File("/path/img2.jpg"), id = "/path/img2.jpg", name = "img2.jpg", album = "A")
        val album = UserAlbum(name = "My Favorites", imagePaths = listOf("/path/img2.jpg"))

        val state = GalleryUiState(
            allItems = listOf(item1, item2),
            selectedUserAlbum = album
        )
        assertEquals(1, state.activeImageList.size)
        assertEquals("img2.jpg", state.activeImageList[0].name)
    }

    @Test
    fun testFolderDrilldown() {
        val item1 = GalleryItem(file = File("/storage/emulated/0/DCIM/Camera/cam1.jpg"), name = "cam1.jpg", album = "Camera")
        val item2 = GalleryItem(file = File("/storage/emulated/0/Pictures/Screenshots/sc1.png"), name = "sc1.png", album = "Screenshots")

        val folder = FolderItem(
            path = "/storage/emulated/0/DCIM/Camera",
            name = "Camera",
            coverItem = item1,
            imageCount = 1,
            totalSizeBytes = 1000L,
            lastModified = 1000L
        )

        val state = GalleryUiState(
            allItems = listOf(item1, item2),
            selectedFolder = folder
        )
        assertEquals(1, state.activeImageList.size)
        assertEquals("cam1.jpg", state.activeImageList[0].name)
    }
}
