package com.example.data.browser

import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import com.example.data.model.DetectedMedia
import com.example.data.model.QualityOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class VideoSniffer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _detectedVideos = MutableStateFlow<List<DetectedMedia>>(emptyList())
    val detectedVideos: StateFlow<List<DetectedMedia>> = _detectedVideos.asStateFlow()

    private val detectedUrls = ConcurrentHashMap.newKeySet<String>()

    companion object {
        private const val TAG = "VideoSniffer"

        val VIDEO_EXTENSIONS = listOf(
            ".mp4", ".webm", ".m3u8", ".m4v", ".mov", ".mkv", ".3gp", ".ts", ".mpd"
        )

        val VIDEO_MIME_TYPES = listOf(
            "video/mp4",
            "video/webm",
            "video/x-matroska",
            "video/quicktime",
            "video/3gpp",
            "application/x-mpegurl",
            "application/vnd.apple.mpegurl",
            "application/dash+xml"
        )

        // JavaScript injection to sniff HTML5 video elements and media events
        val JS_SNIFFER = """
            (function() {
                if (window.__videoSnifferInjected) return;
                window.__videoSnifferInjected = true;

                function reportVideo(videoEl) {
                    try {
                        var src = videoEl.currentSrc || videoEl.src;
                        if (!src) {
                            var source = videoEl.querySelector('source');
                            if (source) src = source.src;
                        }
                        if (src && src.startsWith('http')) {
                            var title = document.title || 'Web Video';
                            var width = videoEl.videoWidth || 0;
                            var height = videoEl.videoHeight || 0;
                            var duration = videoEl.duration || 0;
                            if (window.AndroidBridge && window.AndroidBridge.onVideoFound) {
                                window.AndroidBridge.onVideoFound(src, title, width, height, duration);
                            }
                        }
                    } catch(e) {}
                }

                // Check existing videos
                var videos = document.getElementsByTagName('video');
                for (var i = 0; i < videos.length; i++) {
                    reportVideo(videos[i]);
                    videos[i].addEventListener('play', function() { reportVideo(this); });
                    videos[i].addEventListener('loadeddata', function() { reportVideo(this); });
                }

                // Observe dynamically added video tags
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        for (var i = 0; i < mutation.addedNodes.length; i++) {
                            var node = mutation.addedNodes[i];
                            if (node.tagName === 'VIDEO') {
                                reportVideo(node);
                                node.addEventListener('play', function() { reportVideo(this); });
                            } else if (node.querySelectorAll) {
                                var innerVideos = node.querySelectorAll('video');
                                for (var j = 0; j < innerVideos.length; j++) {
                                    reportVideo(innerVideos[j]);
                                }
                            }
                        }
                    });
                });
                observer.observe(document.body || document.documentElement, { childList: true, subtree: true });
            })();
        """.trimIndent()
    }

    fun clearForNewPage() {
        detectedUrls.clear()
        _detectedVideos.value = emptyList()
    }

    fun inspectUrl(url: String, pageTitle: String = "Video", pageUrl: String = "") {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || cleanUrl.startsWith("blob:") || cleanUrl.startsWith("data:")) return

        // Fast pattern check
        val lower = cleanUrl.lowercase()
        val isVideoUrl = VIDEO_EXTENSIONS.any { lower.contains(it) } ||
                lower.contains("videoplayback") ||
                lower.contains("/video/") ||
                lower.contains("mime=video") ||
                lower.contains(".m3u8")

        if (isVideoUrl && detectedUrls.add(cleanUrl)) {
            val format = determineFormat(cleanUrl)
            val initialMedia = DetectedMedia(
                url = cleanUrl,
                title = sanitizeTitle(pageTitle, cleanUrl),
                format = format,
                detectedResolution = "Auto",
                mimeType = "video/${format.lowercase()}",
                pageUrl = pageUrl
            )
            _detectedVideos.update { current ->
                if (current.none { it.url == cleanUrl }) current + initialMedia else current
            }
            resolveMetadataAsync(cleanUrl, pageTitle, pageUrl)
        }
    }

    private fun resolveMetadataAsync(videoUrl: String, pageTitle: String, pageUrl: String) {
        scope.launch {
            try {
                val headReq = Request.Builder()
                    .url(videoUrl)
                    .head()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    try {
                        client.newCall(headReq).execute()
                    } catch (e: Exception) {
                        // Fallback to GET with Range 0-1
                        val getReq = Request.Builder()
                            .url(videoUrl)
                            .header("Range", "bytes=0-1")
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                            .build()
                        client.newCall(getReq).execute()
                    }
                }

                val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                val contentType = response.headers["Content-Type"] ?: "video/mp4"
                val format = when {
                    contentType.contains("webm") || videoUrl.contains(".webm", true) -> "WebM"
                    contentType.contains("mpegurl") || videoUrl.contains(".m3u8", true) -> "HLS (M3U8)"
                    else -> "MP4"
                }

                response.close()

                _detectedVideos.update { list ->
                    list.map { item ->
                        if (item.url == videoUrl) {
                            item.copy(
                                fileSizeBytes = if (contentLength > 0) contentLength else item.fileSizeBytes,
                                format = format,
                                mimeType = contentType
                            )
                        } else item
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "HEAD request failed for $videoUrl: ${e.message}")
            }
        }
    }

    private fun determineFormat(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".webm") -> "WebM"
            lower.contains(".m3u8") -> "HLS"
            lower.contains(".mov") -> "MOV"
            lower.contains(".mkv") -> "MKV"
            else -> "MP4"
        }
    }

    private fun sanitizeTitle(pageTitle: String, url: String): String {
        val clean = pageTitle.replace(Regex("[|–—\\-].*"), "").trim()
        if (clean.length in 3..50 && !clean.equals("Web Video", true)) {
            return clean
        }
        val uri = Uri.parse(url)
        val lastPath = uri.lastPathSegment
        if (!lastPath.isNullOrBlank() && lastPath.contains(".")) {
            return lastPath.substringBeforeLast(".")
        }
        return "Video_${System.currentTimeMillis() % 100000}"
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun onVideoFound(url: String, title: String, width: Int, height: Int, duration: Float) {
            val resolution = if (width > 0 && height > 0) {
                when {
                    height >= 2160 -> "4K"
                    height >= 1080 -> "1080p"
                    height >= 720 -> "720p"
                    height >= 480 -> "480p"
                    else -> "${height}p"
                }
            } else "HD"

            if (detectedUrls.add(url)) {
                val media = DetectedMedia(
                    url = url,
                    title = sanitizeTitle(title, url),
                    format = determineFormat(url),
                    detectedResolution = resolution,
                    mimeType = "video/mp4"
                )
                _detectedVideos.update { it + media }
                resolveMetadataAsync(url, title, "")
            }
        }
    }

    val javascriptBridge = AndroidBridge()

    fun generateQualityOptions(media: DetectedMedia): List<QualityOption> {
        val baseSize = if (media.fileSizeBytes > 0) media.fileSizeBytes else 25 * 1024 * 1024L
        return listOf(
            QualityOption(
                label = "1080p Full HD",
                resolutionTag = "1080p",
                approximateBitrateMbps = 4.5,
                estimatedSizeBytes = baseSize
            ),
            QualityOption(
                label = "720p HD",
                resolutionTag = "720p",
                approximateBitrateMbps = 2.5,
                estimatedSizeBytes = (baseSize * 0.58).toLong()
            ),
            QualityOption(
                label = "480p Standard",
                resolutionTag = "480p",
                approximateBitrateMbps = 1.2,
                estimatedSizeBytes = (baseSize * 0.32).toLong()
            ),
            QualityOption(
                label = "360p Data Saver",
                resolutionTag = "360p",
                approximateBitrateMbps = 0.7,
                estimatedSizeBytes = (baseSize * 0.18).toLong()
            )
        )
    }
}
