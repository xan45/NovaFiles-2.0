package com.novafiles.core.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.novafiles.core.model.StorageStats
import java.io.File

class StorageVolumeManager(private val context: Context) {

    fun getPrimaryStorageStats(): StorageStats {
        val root = Environment.getExternalStorageDirectory()
        return try {
            val statFs = StatFs(root.absolutePath)
            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val availableBlocks = statFs.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            StorageStats(
                volumeName = "Internal Storage",
                usedBytes = usedBytes,
                totalBytes = totalBytes
            )
        } catch (e: Exception) {
            StorageStats(
                volumeName = "Internal Storage",
                usedBytes = 0L,
                totalBytes = 0L
            )
        }
    }

    fun getAvailableVolumes(): List<StorageVolumeInfo> {
        val volumes = mutableListOf<StorageVolumeInfo>()
        val primaryDir = Environment.getExternalStorageDirectory()
        volumes.add(
            StorageVolumeInfo(
                id = "primary",
                name = "Internal Storage",
                path = primaryDir.absolutePath,
                isPrimary = true,
                isRemovable = false
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            storageManager?.storageVolumes?.forEach { volume ->
                if (!volume.isPrimary) {
                    val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        volume.directory?.absolutePath
                    } else null

                    if (path != null) {
                        volumes.add(
                            StorageVolumeInfo(
                                id = volume.uuid ?: path,
                                name = volume.getDescription(context) ?: "SD Card",
                                path = path,
                                isPrimary = false,
                                isRemovable = volume.isRemovable
                            )
                        )
                    }
                }
            }
        }
        return volumes
    }
}

data class StorageVolumeInfo(
    val id: String,
    val name: String,
    val path: String,
    val isPrimary: Boolean,
    val isRemovable: Boolean
)
