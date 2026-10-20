package com.example.data.download

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadedVideoEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val database = AppDatabase.getDatabase(context)

    companion object {
        private const val TAG = "DownloadManager"

        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun getStorageDir(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: File(context.filesDir, "videos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getThumbnailsDir(): File {
        val dir = File(context.cacheDir, "video_thumbnails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun startDownload(
        url: String,
        rawTitle: String,
        resolution: String = "Original",
        format: String = "MP4"
    ): String {
        val sanitizedTitle = sanitizeFilename(rawTitle.ifBlank { "video_${System.currentTimeMillis()}" })
        val fileExtension = when (format.uppercase()) {
            "WEBM" -> "webm"
            "MKV" -> "mkv"
            else -> "mp4"
        }
        val targetFile = File(getStorageDir(), "$sanitizedTitle.$fileExtension")
        // If file already exists, disambiguate
        val finalFile = if (targetFile.exists()) {
            File(getStorageDir(), "${sanitizedTitle}_${System.currentTimeMillis() % 10000}.$fileExtension")
        } else {
            targetFile
        }

        val taskId = UUID.randomUUID().toString()
        val newTask = DownloadTask(
            id = taskId,
            url = url,
            title = sanitizedTitle,
            filePath = finalFile.absolutePath,
            resolution = resolution,
            format = format,
            status = DownloadStatus.PENDING
        )

        _tasks.update { listOf(newTask) + it }
        executeDownload(taskId)
        return taskId
    }

    fun pauseDownload(taskId: String) {
        val job = activeJobs.remove(taskId)
        job?.cancel()
        _tasks.update { current ->
            current.map {
                if (it.id == taskId && it.status == DownloadStatus.DOWNLOADING) {
                    it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L, etaSeconds = -1L)
                } else it
            }
        }
    }

    fun resumeDownload(taskId: String) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED) {
            executeDownload(taskId)
        }
    }

    fun cancelDownload(taskId: String) {
        val job = activeJobs.remove(taskId)
        job?.cancel()
        val task = _tasks.value.find { it.id == taskId }
        if (task != null) {
            val file = File(task.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        _tasks.update { current -> current.filterNot { it.id == taskId } }
    }

    fun retryDownload(taskId: String) {
        resumeDownload(taskId)
    }

    fun clearCompleted() {
        _tasks.update { current ->
            current.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PAUSED }
        }
    }

    private fun executeDownload(taskId: String) {
        val job = scope.launch {
            val currentTask = _tasks.value.find { it.id == taskId } ?: return@launch
            val file = File(currentTask.filePath)
            val existingBytes = if (file.exists()) file.length() else 0L

            _tasks.update { list ->
                list.map {
                    if (it.id == taskId) it.copy(
                        status = DownloadStatus.DOWNLOADING,
                        errorMessage = null,
                        downloadedBytes = existingBytes
                    ) else it
                }
            }

            try {
                val requestBuilder = Request.Builder()
                    .url(currentTask.url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "*/*")

                if (existingBytes > 0) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful && response.code != 206) {
                    throw IllegalStateException("Server returned HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val responseContentLength = body.contentLength()
                val isPartial = response.code == 206
                val totalBytes = if (isPartial) {
                    existingBytes + responseContentLength
                } else if (responseContentLength > 0) {
                    responseContentLength
                } else {
                    currentTask.totalBytes
                }

                _tasks.update { list ->
                    list.map {
                        if (it.id == taskId) it.copy(totalBytes = totalBytes) else it
                    }
                }

                val inputStream: InputStream = body.byteStream()
                val raf = RandomAccessFile(file, "rw")
                if (isPartial) {
                    raf.seek(existingBytes)
                } else {
                    raf.setLength(0)
                    raf.seek(0)
                }

                val buffer = ByteArray(32 * 1024)
                var bytesRead: Int
                var downloaded = if (isPartial) existingBytes else 0L
                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastCalc = 0L
                var currentSpeed = 0L

                try {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        raf.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        bytesSinceLastCalc += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastSpeedCalcTime
                        if (elapsed >= 500) {
                            currentSpeed = (bytesSinceLastCalc * 1000L) / elapsed
                            bytesSinceLastCalc = 0L
                            lastSpeedCalcTime = now

                            val remainingBytes = if (totalBytes > downloaded) totalBytes - downloaded else 0L
                            val eta = if (currentSpeed > 0) remainingBytes / currentSpeed else -1L

                            _tasks.update { list ->
                                list.map {
                                    if (it.id == taskId) {
                                        it.copy(
                                            downloadedBytes = downloaded,
                                            speedBytesPerSec = currentSpeed,
                                            etaSeconds = eta
                                        )
                                    } else it
                                }
                            }
                        }
                    }
                } finally {
                    try { raf.close() } catch (_: Exception) {}
                    try { inputStream.close() } catch (_: Exception) {}
                    try { body.close() } catch (_: Exception) {}
                }

                // Download Finished Successfully!
                val finalLength = file.length()
                if (finalLength <= 0L) {
                    throw IllegalStateException("Downloaded file is empty (0 bytes received). Please check network or URL.")
                }
                val metadata = extractVideoMetadata(file)

                _tasks.update { list ->
                    list.map {
                        if (it.id == taskId) {
                            it.copy(
                                downloadedBytes = finalLength,
                                totalBytes = finalLength,
                                status = DownloadStatus.COMPLETED,
                                speedBytesPerSec = 0L,
                                etaSeconds = 0L,
                                completedAt = System.currentTimeMillis(),
                                thumbnailPath = metadata.thumbnailPath
                            )
                        } else it
                    }
                }

                // Persist to Room Database
                val videoEntity = DownloadedVideoEntity(
                    title = currentTask.title,
                    filePath = file.absolutePath,
                    sourceUrl = currentTask.url,
                    fileSizeBytes = finalLength,
                    durationMs = metadata.durationMs,
                    resolution = if (metadata.resolution != "Unknown") metadata.resolution else currentTask.resolution,
                    format = currentTask.format,
                    downloadedAt = System.currentTimeMillis(),
                    thumbnailPath = metadata.thumbnailPath
                )
                database.videoDao().insertVideo(videoEntity)
                Log.i(TAG, "Video download completed and saved to DB: ${file.name} ($finalLength bytes)")

            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.d(TAG, "Download job for $taskId was paused/cancelled")
                } else {
                    Log.e(TAG, "Download failed for task $taskId", e)
                    _tasks.update { list ->
                        list.map {
                            if (it.id == taskId) {
                                it.copy(
                                    status = DownloadStatus.FAILED,
                                    errorMessage = e.localizedMessage ?: "Download failed",
                                    speedBytesPerSec = 0L,
                                    etaSeconds = -1L
                                )
                            } else it
                        }
                    }
                }
            } finally {
                activeJobs.remove(taskId)
            }
        }
        activeJobs[taskId] = job
    }

    private data class ExtractedMetadata(
        val durationMs: Long,
        val resolution: String,
        val thumbnailPath: String?
    )

    private fun extractVideoMetadata(file: File): ExtractedMetadata {
        var durationMs = 0L
        var resolution = "Unknown"
        var thumbPath: String? = null

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            if (widthStr != null && heightStr != null) {
                val w = widthStr.toIntOrNull() ?: 0
                val h = heightStr.toIntOrNull() ?: 0
                if (w > 0 && h > 0) {
                    resolution = when {
                        h >= 2160 || w >= 3840 -> "4K ($w x $h)"
                        h >= 1080 || w >= 1920 -> "1080p ($w x $h)"
                        h >= 720 || w >= 1280 -> "720p ($w x $h)"
                        h >= 480 || w >= 848 -> "480p ($w x $h)"
                        else -> "${w}x${h}"
                    }
                }
            }

            // Extract keyframe for thumbnail
            val frameBitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime()

            if (frameBitmap != null) {
                val thumbFile = File(getThumbnailsDir(), "thumb_${file.nameWithoutExtension}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    frameBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                thumbPath = thumbFile.absolutePath
            }
            retriever.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed extracting metadata: ${e.message}")
        }

        return ExtractedMetadata(durationMs, resolution, thumbPath)
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(60)
            .ifBlank { "video_${System.currentTimeMillis()}" }
    }
}
