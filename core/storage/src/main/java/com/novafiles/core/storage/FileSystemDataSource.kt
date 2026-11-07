package com.novafiles.core.storage

import android.webkit.MimeTypeMap
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

open class FileSystemDataSource {

    open suspend fun listFiles(
        directoryPath: String,
        showHidden: Boolean = false
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val files = dir.listFiles() ?: return@withContext emptyList()

        files.asSequence()
            .filter { showHidden || !it.name.startsWith(".") }
            .map { file ->
                val isDir = file.isDirectory
                val extension = file.extension
                val mimeType = if (!isDir && extension.isNotBlank()) {
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                } else null

                val itemCount = if (isDir) {
                    try {
                        file.list()?.size ?: 0
                    } catch (e: Exception) {
                        0
                    }
                } else null

                FileItem(
                    id = file.absolutePath,
                    name = file.name,
                    path = file.absolutePath,
                    sizeBytes = if (isDir) 0L else file.length(),
                    lastModifiedMs = file.lastModified(),
                    isDirectory = isDir,
                    fileType = if (isDir) FileType.FOLDER else FileType.fromExtension(extension),
                    mimeType = mimeType,
                    isHidden = file.name.startsWith("."),
                    itemCount = itemCount
                )
            }
            .toList()
    }

    suspend fun getFileDetails(path: String): FileItem? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null

        val isDir = file.isDirectory
        val extension = file.extension
        val mimeType = if (!isDir && extension.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        } else null

        FileItem(
            id = file.absolutePath,
            name = file.name,
            path = file.absolutePath,
            sizeBytes = if (isDir) 0L else file.length(),
            lastModifiedMs = file.lastModified(),
            isDirectory = isDir,
            fileType = if (isDir) FileType.FOLDER else FileType.fromExtension(extension),
            mimeType = mimeType,
            isHidden = file.name.startsWith("."),
            itemCount = if (isDir) file.list()?.size else null
        )
    }

    fun isPathSafe(baseDir: File, targetPath: String): Boolean {
        return try {
            val baseCanonical = baseDir.canonicalPath
            val targetCanonical = File(targetPath).canonicalPath
            targetCanonical.startsWith(baseCanonical)
        } catch (e: Exception) {
            false
        }
    }
}
