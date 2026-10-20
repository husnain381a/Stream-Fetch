package com.example.data.model

import java.util.UUID

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class QualityOption(
    val label: String, // e.g., "1080p FHD", "720p HD", "480p SD", "360p Low", "Original"
    val resolutionTag: String, // "1080p", "720p", etc.
    val approximateBitrateMbps: Double,
    val estimatedSizeBytes: Long
)

data class DetectedMedia(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val format: String, // "MP4", "WebM", "HLS", "Stream"
    val detectedResolution: String, // "1080p", "720p", "Auto"
    val mimeType: String,
    val fileSizeBytes: Long = -1L,
    val pageUrl: String = "",
    val detectedAt: Long = System.currentTimeMillis()
)

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val filePath: String,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = -1L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val errorMessage: String? = null,
    val resolution: String = "Original",
    val format: String = "MP4",
    val thumbnailPath: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
}

data class VideoItem(
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val durationMs: Long = 0L,
    val resolution: String = "Unknown",
    val format: String = "MP4",
    val dateAdded: Long = System.currentTimeMillis(),
    val thumbnailUri: String? = null
)

data class Bookmark(
    val id: Long = 0,
    val title: String,
    val url: String,
    val category: String = "General"
)
