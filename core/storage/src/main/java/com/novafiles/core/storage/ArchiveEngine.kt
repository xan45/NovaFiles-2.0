package com.novafiles.core.storage

import com.novafiles.core.model.FileOperationProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ArchiveEngine {

    companion object {
        const val MAX_ALLOWED_ENTRIES = 20_000
        const val MAX_ALLOWED_EXPANDED_BYTES = 10L * 1024 * 1024 * 1024 // 10 GB
    }

    /**
     * Extracts an archive with strict Zip Slip containment protection.
     */
    fun extractArchive(
        archiveFile: File,
        destinationDir: File
    ): Flow<FileOperationProgress> = flow {
        if (!archiveFile.exists() || !archiveFile.canRead()) {
            throw IllegalArgumentException("Archive file does not exist or cannot be read: ${archiveFile.path}")
        }
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val destCanonicalPath = destinationDir.canonicalPath
        var extractedEntries = 0
        var totalBytesWritten = 0L

        val name = archiveFile.name.lowercase()
        when {
            name.endsWith(".zip") -> {
                ZipFile(archiveFile).use { zip ->
                    val totalEntries = zip.size()
                    val entries = zip.entries()

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        extractedEntries++

                        if (extractedEntries > MAX_ALLOWED_ENTRIES) {
                            throw SecurityException("Archive contains too many entries (max $MAX_ALLOWED_ENTRIES)")
                        }

                        val targetFile = File(destinationDir, entry.name)
                        // Security check: Verify canonical containment to block ZIP Slip
                        if (!targetFile.canonicalPath.startsWith(destCanonicalPath + File.separator) &&
                            targetFile.canonicalPath != destCanonicalPath
                        ) {
                            throw SecurityException("Zip Slip path traversal blocked for entry: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            targetFile.mkdirs()
                        } else {
                            targetFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(targetFile).use { output ->
                                    val buffer = ByteArray(8192)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                        totalBytesWritten += read

                                        if (totalBytesWritten > MAX_ALLOWED_EXPANDED_BYTES) {
                                            throw SecurityException("Exceeded maximum archive expansion size limit")
                                        }
                                    }
                                }
                            }
                        }

                        emit(
                            FileOperationProgress(
                                currentFileName = entry.name,
                                processedCount = extractedEntries,
                                totalCount = totalEntries,
                                processedBytes = totalBytesWritten,
                                isComplete = extractedEntries == totalEntries
                            )
                        )
                    }
                }
            }
            name.endsWith(".tar") || name.endsWith(".tar.gz") || name.endsWith(".tgz") -> {
                val fileIn = FileInputStream(archiveFile)
                val inStream = if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
                    GZIPInputStream(fileIn)
                } else {
                    fileIn
                }
                TarArchiveInputStream(BufferedInputStream(inStream)).use { tar ->
                    var entry = tar.nextEntry
                    while (entry != null) {
                        extractedEntries++
                        val targetFile = File(destinationDir, entry.name)
                        if (!targetFile.canonicalPath.startsWith(destCanonicalPath + File.separator) &&
                            targetFile.canonicalPath != destCanonicalPath
                        ) {
                            throw SecurityException("Tar Slip path traversal blocked for entry: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            targetFile.mkdirs()
                        } else {
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { output ->
                                val buffer = ByteArray(8192)
                                var read: Int
                                while (tar.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    totalBytesWritten += read
                                }
                            }
                        }

                        emit(
                            FileOperationProgress(
                                currentFileName = entry.name,
                                processedCount = extractedEntries,
                                totalCount = extractedEntries, // streaming archive
                                processedBytes = totalBytesWritten
                            )
                        )
                        entry = tar.nextEntry
                    }
                }
            }
            else -> {
                throw UnsupportedOperationException("Unsupported archive format: ${archiveFile.name}")
            }
        }

        emit(
            FileOperationProgress(
                currentFileName = archiveFile.name,
                processedCount = extractedEntries,
                totalCount = extractedEntries,
                processedBytes = totalBytesWritten,
                isComplete = true
            )
        )
    }

    /**
     * Creates a standard ZIP archive from a list of files or folders.
     */
    fun createZipArchive(
        sources: List<File>,
        targetZipFile: File
    ): Flow<FileOperationProgress> = flow {
        targetZipFile.parentFile?.mkdirs()
        val allFiles = mutableListOf<File>()
        sources.forEach { source ->
            if (source.isDirectory) {
                source.walkTopDown().forEach { allFiles.add(it) }
            } else {
                allFiles.add(source)
            }
        }

        val totalCount = allFiles.size
        var processedCount = 0
        var totalBytesWritten = 0L

        ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZipFile))).use { zos ->
            val baseParent = sources.firstOrNull()?.parentFile ?: File("/")

            for (file in allFiles) {
                processedCount++
                val relativePath = file.relativeTo(baseParent).path.replace('\\', '/')

                if (file.isDirectory) {
                    val entryName = if (relativePath.endsWith("/")) relativePath else "$relativePath/"
                    zos.putNextEntry(ZipEntry(entryName))
                    zos.closeEntry()
                } else {
                    zos.putNextEntry(ZipEntry(relativePath))
                    FileInputStream(file).use { fis ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            zos.write(buffer, 0, read)
                            totalBytesWritten += read
                        }
                    }
                    zos.closeEntry()
                }

                emit(
                    FileOperationProgress(
                        currentFileName = file.name,
                        processedCount = processedCount,
                        totalCount = totalCount,
                        processedBytes = totalBytesWritten,
                        isComplete = processedCount == totalCount
                    )
                )
            }
        }

        emit(
            FileOperationProgress(
                currentFileName = targetZipFile.name,
                processedCount = totalCount,
                totalCount = totalCount,
                processedBytes = totalBytesWritten,
                isComplete = true
            )
        )
    }
}
