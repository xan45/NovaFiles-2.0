package com.novafiles.core.model

import kotlinx.serialization.Serializable

/**
 * Immutable domain representation of a file or directory item.
 */
@Serializable
data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long = 0L,
    val lastModifiedMs: Long = 0L,
    val isDirectory: Boolean = false,
    val fileType: FileType = FileType.OTHER,
    val mimeType: String? = null,
    val isHidden: Boolean = false,
    val itemCount: Int? = null,
    val isSelected: Boolean = false,
    val extension: String = name.substringAfterLast('.', "")
) : java.io.Serializable
