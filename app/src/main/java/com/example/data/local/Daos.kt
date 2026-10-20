package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM downloaded_videos ORDER BY downloadedAt DESC")
    fun getAllVideos(): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Long): DownloadedVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: DownloadedVideoEntity): Long

    @Query("DELETE FROM downloaded_videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)

    @Query("UPDATE downloaded_videos SET title = :newTitle WHERE id = :id")
    suspend fun updateTitle(id: Long, newTitle: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)
}
