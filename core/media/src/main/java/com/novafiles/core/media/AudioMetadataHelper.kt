package com.novafiles.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class AudioTrackMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val bitrate: String = "",
    val albumArtFile: File? = null,
    val avatarLetter: String = title.firstOrNull()?.uppercase() ?: "A",
    val formattedDuration: String = "",
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0
) {
    val albumArtPath: String? get() = albumArtFile?.absolutePath
}

object AudioMetadataHelper {

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun extractMetadata(context: Context, audioFile: File): AudioTrackMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs = 0L
        var bitrate = ""

        try {
            retriever.setDataSource(audioFile.absolutePath)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durStr?.toLongOrNull() ?: 0L
            val brStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (brStr != null) {
                val kbps = brStr.toLongOrNull()?.div(1000) ?: 0
                bitrate = "$kbps kbps"
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        val resolvedTitle = if (!title.isNullOrBlank()) title else audioFile.nameWithoutExtension
        val resolvedArtist = if (!artist.isNullOrBlank()) artist else "Unknown Artist"
        val resolvedAlbum = if (!album.isNullOrBlank()) album else "Unknown Album"

        val artFile = getOrGenerateAlbumArt(context, audioFile, resolvedTitle)
        val firstChar = resolvedTitle.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "♪"

        AudioTrackMetadata(
            title = resolvedTitle,
            artist = resolvedArtist,
            album = resolvedAlbum,
            durationMs = durationMs,
            bitrate = bitrate,
            albumArtFile = artFile,
            avatarLetter = firstChar
        )
    }

    suspend fun getOrGenerateAlbumArt(
        context: Context,
        audioFile: File,
        titleFallback: String = audioFile.nameWithoutExtension
    ): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "album_art").apply { mkdirs() }
        val hash = md5(audioFile.absolutePath)
        val artFile = File(cacheDir, "$hash.jpg")

        if (artFile.exists() && artFile.length() > 0) {
            return@withContext artFile
        }

        val retriever = MediaMetadataRetriever()
        var embeddedBytes: ByteArray? = null
        try {
            retriever.setDataSource(audioFile.absolutePath)
            embeddedBytes = retriever.embeddedPicture
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        if (embeddedBytes != null && embeddedBytes.isNotEmpty()) {
            FileOutputStream(artFile).use { out ->
                out.write(embeddedBytes)
                out.flush()
            }
            return@withContext artFile
        }

        // Generate letter avatar artwork
        val firstChar = titleFallback.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "♪"
        val avatarBitmap = generateLetterAvatarBitmap(firstChar, hash.hashCode().toLong())

        FileOutputStream(artFile).use { out ->
            avatarBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }
        artFile
    }

    private fun generateLetterAvatarBitmap(letter: String, seed: Long): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val gradients = listOf(
            Pair(android.graphics.Color.parseColor("#3B82F6"), android.graphics.Color.parseColor("#1D4ED8")), // Blue
            Pair(android.graphics.Color.parseColor("#8B5CF6"), android.graphics.Color.parseColor("#6D28D9")), // Purple
            Pair(android.graphics.Color.parseColor("#EC4899"), android.graphics.Color.parseColor("#BE185D")), // Pink
            Pair(android.graphics.Color.parseColor("#10B981"), android.graphics.Color.parseColor("#047857")), // Green
            Pair(android.graphics.Color.parseColor("#F97316"), android.graphics.Color.parseColor("#C2410C")), // Orange
            Pair(android.graphics.Color.parseColor("#06B6D4"), android.graphics.Color.parseColor("#0E7490"))  // Cyan
        )
        val idx = kotlin.math.abs(seed % gradients.size).toInt()
        val (col1, col2) = gradients[idx]

        val shader = android.graphics.LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            col1, col2,
            android.graphics.Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 220f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(letter, size / 2f, yPos, textPaint)

        return bitmap
    }
}
