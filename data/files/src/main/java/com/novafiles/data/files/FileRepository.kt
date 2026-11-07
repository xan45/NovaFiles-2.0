package com.novafiles.data.files

import com.novafiles.core.common.NovaResult
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileSortConfig
import com.novafiles.core.model.StorageStats
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getFiles(
        directoryPath: String,
        sortConfig: FileSortConfig = FileSortConfig(),
        showHidden: Boolean = false
    ): Flow<NovaResult<List<FileItem>>>

    fun searchFiles(
        directoryPath: String,
        query: String
    ): Flow<List<FileItem>>

    fun getStorageStats(): Flow<StorageStats>
}
