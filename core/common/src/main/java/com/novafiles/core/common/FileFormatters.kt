package com.novafiles.core.common

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Standard formatters for file sizes, durations, and timestamps across NovaFiles 2.0.
 */
object FileFormatters {

    private val sizeFormat = DecimalFormat("#,##0.#")

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return "${sizeFormat.format(value)} ${units[index]}"
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "00:00"
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDate(timestampMs: Long, pattern: String = "dd/MM/yy"): String {
        if (timestampMs <= 0L) return ""
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatRelativeDate(timestampMs: Long): String {
        if (timestampMs <= 0L) return ""
        val now = System.currentTimeMillis()
        val diffMs = now - timestampMs
        val oneDayMs = 24 * 60 * 60 * 1000L

        return when {
            diffMs in 0 until oneDayMs -> {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                "Today, ${timeFormat.format(Date(timestampMs))}"
            }
            diffMs in oneDayMs until (2 * oneDayMs) -> "Yesterday"
            else -> formatDate(timestampMs, "MMM d, yyyy")
        }
    }
}
