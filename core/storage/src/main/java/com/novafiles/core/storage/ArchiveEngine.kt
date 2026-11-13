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

data class ArchiveEntryInfo(
    val name: String,
    val sizeBytes: Long,
    val isDirectory: Boolean
)

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
                        if (extractedEntries > MAX_ALLOWED_ENTRIES) {
                            throw SecurityException("Archive contains too many entries (max $MAX_ALLOWED_ENTRIES)")
                        }
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
                                    if (totalBytesWritten > MAX_ALLOWED_EXPANDED_BYTES) {
                                        throw SecurityException("Exceeded maximum archive expansion size limit")
                                    }
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
        val allEntries = mutableListOf<Pair<File, File>>()
        sources.forEach { source ->
            val baseParent = source.parentFile ?: File("/")
            if (source.isDirectory) {
                source.walkTopDown().forEach { allEntries.add(it to baseParent) }
            } else {
                allEntries.add(source to baseParent)
            }
        }

        val totalCount = allEntries.size
        var processedCount = 0
        var totalBytesWritten = 0L

        ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZipFile))).use { zos ->
            for ((file, baseParent) in allEntries) {
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

    fun listArchiveEntries(archiveFile: File): List<ArchiveEntryInfo> {
        if (!archiveFile.exists() || !archiveFile.canRead()) return emptyList()
        val result = mutableListOf<ArchiveEntryInfo>()
        val name = archiveFile.name.lowercase()
        when {
            name.endsWith(".zip") -> {
                ZipFile(archiveFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements() && result.size < MAX_ALLOWED_ENTRIES) {
                        val e = entries.nextElement()
                        result.add(ArchiveEntryInfo(e.name, e.size.coerceAtLeast(0L), e.isDirectory))
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
                    while (entry != null && result.size < MAX_ALLOWED_ENTRIES) {
                        result.add(ArchiveEntryInfo(entry.name, entry.size.coerceAtLeast(0L), entry.isDirectory))
                        entry = tar.nextEntry
                    }
                }
            }
        }
        return result
    }

    suspend fun extractSingleEntry(
        archiveFile: File,
        entryName: String,
        destinationDir: File
    ): File = withContext(Dispatchers.IO) {
        if (!destinationDir.exists()) destinationDir.mkdirs()
        val destCanonical = destinationDir.canonicalPath
        val targetFile = File(destinationDir, entryName.substringAfterLast('/'))
        if (!targetFile.canonicalPath.startsWith(destCanonical + File.separator) &&
            targetFile.canonicalPath != destCanonical
        ) {
            throw SecurityException("Zip Slip blocked for entry: $entryName")
        }

        val lower = archiveFile.name.lowercase()
        when {
            lower.endsWith(".zip") -> {
                ZipFile(archiveFile).use { zip ->
                    val entry = zip.getEntry(entryName)
                        ?: throw IllegalArgumentException("Entry not found: $entryName")
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output, bufferSize = 8192)
                            }
                        }
                    }
                }
            }
            lower.endsWith(".tar") || lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> {
                val fileIn = FileInputStream(archiveFile)
                val inStream = if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
                    GZIPInputStream(fileIn)
                } else {
                    fileIn
                }
                TarArchiveInputStream(BufferedInputStream(inStream)).use { tar ->
                    var entry = tar.nextEntry
                    var found = false
                    while (entry != null) {
                        if (entry.name == entryName) {
                            found = true
                            if (entry.isDirectory) {
                                targetFile.mkdirs()
                            } else {
                                targetFile.parentFile?.mkdirs()
                                FileOutputStream(targetFile).use { output ->
                                    tar.copyTo(output, bufferSize = 8192)
                                }
                            }
                            break
                        }
                        entry = tar.nextEntry
                    }
                    if (!found) throw IllegalArgumentException("Entry not found: $entryName")
                }
            }
            else -> throw UnsupportedOperationException("Unsupported archive format: ${archiveFile.name}")
        }
        targetFile
    }
}
