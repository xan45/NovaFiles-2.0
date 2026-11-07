package com.novafiles.core.model

import kotlinx.serialization.Serializable

/**
 * Standard classification for files and content categories in NovaFiles 2.0.
 */
@Serializable
enum class FileType {
    FOLDER,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    APK,
    OTHER;

    companion object {
        fun fromExtension(ext: String?): FileType {
            if (ext.isNullOrBlank()) return OTHER
            val clean = ext.lowercase().trim()
            return when (clean) {
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg" -> IMAGE
                "mp4", "mkv", "webm", "avi", "mov", "flv", "3gp", "ts" -> VIDEO
                "mp3", "m4a", "flac", "wav", "ogg", "aac", "opus", "wma" -> AUDIO
                "pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv", "json", "xml", "html", "md" -> DOCUMENT
                "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso" -> ARCHIVE
                "apk", "apks", "xapk" -> APK
                else -> OTHER
            }
        }
    }
}
