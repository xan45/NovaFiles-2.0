package com.novafiles.feature.video

import com.novafiles.core.model.SortDirection
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

@Serializable
data class VideoItem(
    val id: String = UUID.randomUUID().toString(),
    val path: String,
    val name: String,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val dateModified: Long = 0L,
    val dateAdded: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "video/mp4",
    val folderPath: String = "",
    val folderName: String = ""
) {
    val file: File get() = File(path)

    val formattedDuration: String
        get() {
            val totalSec = durationMs / 1000
            val s = totalSec % 60
            val m = (totalSec / 60) % 60
            val h = totalSec / 3600
            return if (h > 0) {
                String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
            } else {
                String.format(java.util.Locale.US, "%02d:%02d", m, s)
            }
        }

    val formattedSize: String
        get() {
            val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.1f MB", mb)
            }
        }

    val resolutionLabel: String
        get() = when {
            width >= 3800 || height >= 2100 -> "4K UHD"
            width >= 1900 || height >= 1000 -> "1080p FHD"
            width >= 1200 || height >= 700 -> "720p HD"
            width > 0 && height > 0 -> "${width}x${height}"
            else -> ""
        }
}

enum class VideoSection(val title: String) {
    ALL_VIDEOS("All Videos"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists")
}

@Serializable
enum class VideoViewMode(val title: String) {
    GRID("Grid"),
    LIST("List")
}

@Serializable
enum class VideoThumbnailSize(val title: String, val columns: Int) {
    SMALL("Small", 4),
    MEDIUM("Medium", 3),
    LARGE("Large", 2),
    EXTRA_LARGE("Extra Large", 1)
}

@Serializable
enum class VideoSortBy(val label: String) {
    DATE_MODIFIED("Date Modified"),
    DATE_ADDED("Date Added"),
    NAME("Name"),
    SIZE("Size"),
    DURATION("Duration")
}

@Serializable
data class VideoSortConfig(
    val sortBy: VideoSortBy = VideoSortBy.DATE_MODIFIED,
    val direction: SortDirection = SortDirection.DESCENDING
)

@Serializable
enum class VideoOrientationPref(val label: String) {
    AUTO("Auto / System"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait")
}

@Serializable
data class VideoPreferences(
    val viewMode: VideoViewMode = VideoViewMode.GRID,
    val thumbnailSize: VideoThumbnailSize = VideoThumbnailSize.MEDIUM,
    val gridSpacingDp: Int = 4,
    val showDurationOverlay: Boolean = true,
    val sortConfig: VideoSortConfig = VideoSortConfig(),
    val groupByDate: Boolean = true,
    val rememberPlaybackPosition: Boolean = true,
    val defaultSubtitleEnabled: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val orientationPreference: VideoOrientationPref = VideoOrientationPref.AUTO
)

@Serializable
data class VideoPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val videoPaths: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class VideoFolderItem(
    val path: String,
    val name: String,
    val coverItem: VideoItem?,
    val videoCount: Int,
    val totalDurationMs: Long,
    val totalSizeBytes: Long,
    val lastModified: Long
) {
    val formattedDuration: String
        get() {
            val totalSec = totalDurationMs / 1000
            val s = totalSec % 60
            val m = (totalSec / 60) % 60
            val h = totalSec / 3600
            return if (h > 0) {
                String.format(java.util.Locale.US, "%dh %02dm", h, m)
            } else {
                String.format(java.util.Locale.US, "%dm %02ds", m, s)
            }
        }

    val formattedSize: String
        get() {
            val mb = totalSizeBytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.1f MB", mb)
            }
        }
}

data class VideoDateGroup(
    val title: String,
    val items: List<VideoItem>
)

@Serializable
data class PlaybackResumePoint(
    val positionMs: Long,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
