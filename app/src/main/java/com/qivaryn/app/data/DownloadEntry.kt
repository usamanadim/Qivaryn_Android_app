package com.qivaryn.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DownloadStatus {
    DOWNLOADING,
    COMPLETED,
    FAILED,
    EXPIRED,
    FILE_REMOVED
}

/**
 * How a download request should be executed by the engine.
 */
enum class DownloadScope {
    SINGLE,
    ALL_REELS,
    ALL_POSTS
}

data class DownloadEntry(
    val id: String,
    val title: String,
    val status: DownloadStatus,
    val sourceUrl: String? = null,
    val platform: String = "Instagram",
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val localPath: String? = null,
    val contentUri: String? = null,
    val mimeType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    val isActive: Boolean get() = status == DownloadStatus.DOWNLOADING
    val isFinished: Boolean get() = !isActive
    val hasFile: Boolean get() = !localPath.isNullOrBlank() || !contentUri.isNullOrBlank()
}

enum class DownloadFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

fun DownloadEntry.matches(filter: DownloadFilter): Boolean = when (filter) {
    DownloadFilter.ALL -> true
    DownloadFilter.ACTIVE -> status == DownloadStatus.DOWNLOADING
    DownloadFilter.COMPLETED -> status == DownloadStatus.COMPLETED
    DownloadFilter.FAILED -> status == DownloadStatus.FAILED ||
        status == DownloadStatus.EXPIRED ||
        status == DownloadStatus.FILE_REMOVED
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "—"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.1f GB", mb / 1024.0)
}

fun formatEntryDate(timestamp: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(timestamp))

fun DownloadEntry.subtitleLine(): String {
    val size = formatBytes(totalBytes ?: downloadedBytes)
    return "$platform  •  " + formatEntryDate(timestamp) + "  •  " + size
}
