package com.novafiles.data.files

import com.novafiles.core.common.NovaResult
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.SortBy
import com.novafiles.core.model.SortDirection
import com.novafiles.core.model.StorageStats
import com.novafiles.core.storage.FileSystemDataSource
import com.novafiles.core.storage.StorageVolumeManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FileRepositoryImpl(
    private val fileSystemDataSource: FileSystemDataSource,
    private val storageVolumeManager: StorageVolumeManager
) : FileRepository {

    override fun getFiles(
        directoryPath: String,
        sortConfig: FileSortConfig,
        showHidden: Boolean
    ): Flow<NovaResult<List<FileItem>>> = flow {
        emit(NovaResult.Loading)
        try {
            val rawFiles = fileSystemDataSource.listFiles(directoryPath, showHidden)
            val sorted = sortFiles(rawFiles, sortConfig)
            emit(NovaResult.Success(sorted))
        } catch (e: Throwable) {
            emit(NovaResult.Error(e))
        }
    }

    override fun searchFiles(
        directoryPath: String,
        query: String
    ): Flow<List<FileItem>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val all = fileSystemDataSource.listFiles(directoryPath, showHidden = false)
        val filtered = all.filter { it.name.contains(query, ignoreCase = true) }
        emit(filtered)
    }

    override fun getStorageStats(): Flow<StorageStats> = flow {
        val stats = storageVolumeManager.getPrimaryStorageStats()
        emit(stats)
    }

    private fun sortFiles(files: List<FileItem>, config: FileSortConfig): List<FileItem> {
        val comparator = Comparator<FileItem> { a, b ->
            if (config.foldersFirst) {
                if (a.isDirectory && !b.isDirectory) return@Comparator -1
                if (!a.isDirectory && b.isDirectory) return@Comparator 1
            }

            val comparison = when (config.sortBy) {
                SortBy.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                SortBy.DATE -> a.lastModifiedMs.compareTo(b.lastModifiedMs)
                SortBy.SIZE -> a.sizeBytes.compareTo(b.sizeBytes)
                SortBy.TYPE -> a.fileType.name.compareTo(b.fileType.name)
            }

            if (config.direction == SortDirection.DESCENDING) -comparison else comparison
        }

        return files.sortedWith(comparator)
    }
}
