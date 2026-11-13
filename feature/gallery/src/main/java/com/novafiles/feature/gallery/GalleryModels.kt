package com.novafiles.feature.gallery

import com.novafiles.core.model.SortDirection
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

enum class GallerySection(val title: String) {
    ALL_IMAGES("All Images"),
    FOLDERS("Folders"),
    ALBUMS("Albums")
}

enum class ThumbnailSize(val columns: Int, val label: String) {
    SMALL(5, "Small"),
    MEDIUM(4, "Medium"),
    LARGE(3, "Large"),
    EXTRA_LARGE(2, "Extra Large")
}

enum class GallerySortBy(val label: String) {
    DATE_MODIFIED("Date Modified"),
    DATE_TAKEN("Date Taken"),
    NAME("Name"),
    SIZE("Size")
}

@Serializable
data class GallerySortConfig(
    val sortBy: GallerySortBy = GallerySortBy.DATE_MODIFIED,
    val direction: SortDirection = SortDirection.DESCENDING
)

@Serializable
data class GalleryPreferences(
    val thumbnailSize: ThumbnailSize = ThumbnailSize.LARGE,
    val gridSpacingDp: Int = 4,
    val isRoundedThumbnails: Boolean = true,
    val sortConfig: GallerySortConfig = GallerySortConfig(),
    val groupByDate: Boolean = true,
    val showHiddenFolders: Boolean = false,
    val excludedFolderPaths: Set<String> = emptySet()
)

data class GalleryItem(
    val file: File,
    val id: String = file.absolutePath,
    val name: String = file.name,
    val album: String,
    val sizeBytes: Long = file.length(),
    val lastModified: Long = file.lastModified(),
    val dateTaken: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null
)

@Serializable
data class UserAlbum(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val imagePaths: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class FolderItem(
    val path: String,
    val name: String,
    val coverItem: GalleryItem?,
    val imageCount: Int,
    val totalSizeBytes: Long,
    val lastModified: Long,
    val isHidden: Boolean = false
)

data class DateGroup(
    val title: String,
    val items: List<GalleryItem>
)

data class ImageExifMetadata(
    val width: Int?,
    val height: Int?,
    val dateTaken: Long?,
    val latitude: Double?,
    val longitude: Double?,
    val cameraModel: String?,
    val iso: String?,
    val fNumber: String?,
    val exposureTime: String?,
    val orientation: Int = 0
)
