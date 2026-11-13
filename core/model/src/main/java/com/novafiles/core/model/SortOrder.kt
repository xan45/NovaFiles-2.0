package com.novafiles.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SortBy {
    NAME,
    DATE,
    SIZE,
    TYPE
}

@Serializable
enum class SortDirection {
    ASCENDING,
    DESCENDING
}

@Serializable
data class FileSortConfig(
    val sortBy: SortBy = SortBy.DATE,
    val direction: SortDirection = SortDirection.DESCENDING,
    val foldersFirst: Boolean = true
) : java.io.Serializable
