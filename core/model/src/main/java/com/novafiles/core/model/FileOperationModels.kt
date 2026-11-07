package com.novafiles.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConflictResolution {
    OVERWRITE,
    RENAME_NEW,
    SKIP
}

data class FileOperationProgress(
    val currentFileName: String = "",
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val countPercentage: Float get() = if (totalCount > 0) (processedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
    val bytesPercentage: Float get() = if (totalBytes > 0) (processedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

sealed interface BatchRenameMode {
    data class Prefix(val prefix: String) : BatchRenameMode
    data class Suffix(val suffix: String) : BatchRenameMode
    data class Enumeration(val baseName: String, val startNumber: Int = 1, val digits: Int = 3) : BatchRenameMode
    data class Replace(val find: String, val replace: String) : BatchRenameMode
}
