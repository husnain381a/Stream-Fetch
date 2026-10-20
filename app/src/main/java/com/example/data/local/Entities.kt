package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Bookmark
import com.example.data.model.VideoItem

@Entity(tableName = "downloaded_videos")
data class DownloadedVideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val sourceUrl: String,
    val fileSizeBytes: Long,
    val durationMs: Long = 0L,
    val resolution: String = "Unknown",
    val format: String = "MP4",
    val downloadedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null
) {
    fun toVideoItem(): VideoItem = VideoItem(
        id = id,
        title = title,
        filePath = filePath,
        fileSizeBytes = fileSizeBytes,
        durationMs = durationMs,
        resolution = resolution,
        format = format,
        dateAdded = downloadedAt,
        thumbnailUri = thumbnailPath
    )
}

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val category: String = "Favorites",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toBookmark(): Bookmark = Bookmark(
        id = id,
        title = title,
        url = url,
        category = category
    )
}
