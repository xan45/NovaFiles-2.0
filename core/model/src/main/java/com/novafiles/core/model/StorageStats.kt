package com.novafiles.core.model

import kotlinx.serialization.Serializable

/**
 * High-level storage utilization statistics.
 */
@Serializable
data class StorageStats(
    val volumeName: String = "Internal Storage",
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val categoryBreakdown: Map<FileType, Long> = emptyMap()
) {
    val freeBytes: Long get() = (totalBytes - usedBytes).coerceAtLeast(0L)
    val usedPercentage: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f
    val usedPercentageInt: Int get() = (usedPercentage * 100).toInt().coerceIn(0, 100)
}
