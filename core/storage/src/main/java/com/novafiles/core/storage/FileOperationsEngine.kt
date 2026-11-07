package com.novafiles.core.storage

import com.novafiles.core.model.BatchRenameMode
import com.novafiles.core.model.ConflictResolution
import com.novafiles.core.model.FileOperationProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileOperationsEngine {

    /**
     * Copies a list of files or directories into targetDir with progress reporting and conflict resolution.
     */
    fun copyFiles(
        sources: List<File>,
        targetDir: File,
        conflictResolution: ConflictResolution = ConflictResolution.RENAME_NEW
    ): Flow<FileOperationProgress> = flow {
        if (!targetDir.exists()) targetDir.mkdirs()

        val allItems = mutableListOf<Pair<File, File>>() // source to target
        var totalBytes = 0L

        sources.forEach { src ->
            if (src.isDirectory) {
                src.walkTopDown().forEach { subFile ->
                    val relPath = subFile.relativeTo(src).path
                    val destFile = File(File(targetDir, src.name), relPath)
                    allItems.add(subFile to destFile)
                    if (!subFile.isDirectory) totalBytes += subFile.length()
                }
            } else {
                allItems.add(src to File(targetDir, src.name))
                totalBytes += src.length()
            }
        }

        var processedCount = 0
        var processedBytes = 0L

        for ((src, initialDest) in allItems) {
            processedCount++
            var dest = initialDest

            if (dest.exists()) {
                when (conflictResolution) {
                    ConflictResolution.SKIP -> {
                        emit(
                            FileOperationProgress(
                                currentFileName = src.name,
                                processedCount = processedCount,
                                totalCount = allItems.size,
                                processedBytes = processedBytes
                            )
                        )
                        continue
                    }
                    ConflictResolution.RENAME_NEW -> {
                        dest = getNonCollidingFile(dest)
                    }
                    ConflictResolution.OVERWRITE -> {
                        if (dest.isDirectory) dest.deleteRecursively() else dest.delete()
                    }
                }
            }

            if (src.isDirectory) {
                dest.mkdirs()
            } else {
                dest.parentFile?.mkdirs()
                FileInputStream(src).use { fis ->
                    FileOutputStream(dest).use { fos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                            processedBytes += read
                        }
                    }
                }
            }

            emit(
                FileOperationProgress(
                    currentFileName = src.name,
                    processedCount = processedCount,
                    totalCount = allItems.size,
                    processedBytes = processedBytes,
                    totalBytes = totalBytes,
                    isComplete = processedCount == allItems.size
                )
            )
        }

        emit(
            FileOperationProgress(
                currentFileName = "Done",
                processedCount = allItems.size,
                totalCount = allItems.size,
                processedBytes = totalBytes,
                totalBytes = totalBytes,
                isComplete = true
            )
        )
    }

    /**
     * Moves files into targetDir using atomic rename when possible, or copy-then-delete across filesystems.
     */
    fun moveFiles(
        sources: List<File>,
        targetDir: File,
        conflictResolution: ConflictResolution = ConflictResolution.RENAME_NEW
    ): Flow<FileOperationProgress> = flow {
        if (!targetDir.exists()) targetDir.mkdirs()

        val totalCount = sources.size
        var processedCount = 0

        for (src in sources) {
            processedCount++
            var dest = File(targetDir, src.name)

            if (dest.exists()) {
                when (conflictResolution) {
                    ConflictResolution.SKIP -> continue
                    ConflictResolution.RENAME_NEW -> dest = getNonCollidingFile(dest)
                    ConflictResolution.OVERWRITE -> {
                        if (dest.isDirectory) dest.deleteRecursively() else dest.delete()
                    }
                }
            }

            // Attempt atomic filesystem rename
            val moved = src.renameTo(dest)
            if (!moved) {
                // Fallback: Copy and delete
                if (src.isDirectory) {
                    src.copyRecursively(dest, overwrite = true)
                    src.deleteRecursively()
                } else {
                    src.copyTo(dest, overwrite = true)
                    src.delete()
                }
            }

            emit(
                FileOperationProgress(
                    currentFileName = src.name,
                    processedCount = processedCount,
                    totalCount = totalCount,
                    isComplete = processedCount == totalCount
                )
            )
        }
    }

    /**
     * Deletes files or directories recursively.
     */
    fun deleteFiles(targets: List<File>): Flow<FileOperationProgress> = flow {
        val totalCount = targets.size
        var processedCount = 0

        for (target in targets) {
            processedCount++
            if (target.exists()) {
                if (target.isDirectory) {
                    target.deleteRecursively()
                } else {
                    target.delete()
                }
            }

            emit(
                FileOperationProgress(
                    currentFileName = target.name,
                    processedCount = processedCount,
                    totalCount = totalCount,
                    isComplete = processedCount == totalCount
                )
            )
        }
    }

    /**
     * Renames a single file.
     */
    suspend fun renameFile(target: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        if (!target.exists()) return@withContext Result.failure(IllegalArgumentException("File does not exist"))
        val sanitized = newName.trim().replace("/", "").replace("\\", "")
        if (sanitized.isBlank()) return@withContext Result.failure(IllegalArgumentException("Filename cannot be empty"))

        val newFile = File(target.parentFile, sanitized)
        if (newFile.exists()) return@withContext Result.failure(IllegalStateException("File with name already exists"))

        val success = target.renameTo(newFile)
        if (success) Result.success(newFile) else Result.failure(IllegalStateException("Failed to rename file"))
    }

    /**
     * Batch renames files based on the specified mode.
     */
    suspend fun batchRename(
        targets: List<File>,
        mode: BatchRenameMode
    ): List<File> = withContext(Dispatchers.IO) {
        val renamedList = mutableListOf<File>()

        targets.forEachIndexed { index, file ->
            val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
            val baseNameWithoutExt = file.nameWithoutExtension

            val newName = when (mode) {
                is BatchRenameMode.Prefix -> "${mode.prefix}${file.name}"
                is BatchRenameMode.Suffix -> "${baseNameWithoutExt}${mode.suffix}$ext"
                is BatchRenameMode.Enumeration -> {
                    val number = mode.startNumber + index
                    val formattedNum = String.format("%0${mode.digits}d", number)
                    "${mode.baseName}_$formattedNum$ext"
                }
                is BatchRenameMode.Replace -> {
                    val replaced = file.name.replace(mode.find, mode.replace)
                    replaced
                }
            }

            val targetFile = File(file.parentFile, newName)
            if (file.renameTo(targetFile)) {
                renamedList.add(targetFile)
            } else {
                renamedList.add(file)
            }
        }

        renamedList
    }

    /**
     * Creates a new directory.
     */
    suspend fun createDirectory(parentDir: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        val sanitized = name.trim().replace("/", "").replace("\\", "")
        val newDir = File(parentDir, sanitized)
        if (newDir.exists()) return@withContext Result.failure(IllegalStateException("Directory already exists"))

        val created = newDir.mkdirs()
        if (created) Result.success(newDir) else Result.failure(IllegalStateException("Could not create directory"))
    }

    private fun getNonCollidingFile(file: File): File {
        if (!file.exists()) return file
        val parent = file.parentFile ?: File("/")
        val baseName = file.nameWithoutExtension
        val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
        var count = 1
        var candidate = File(parent, "$baseName ($count)$ext")
        while (candidate.exists()) {
            count++
            candidate = File(parent, "$baseName ($count)$ext")
        }
        return candidate
    }
}
