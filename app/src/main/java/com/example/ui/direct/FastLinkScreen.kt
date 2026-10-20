package com.example.ui.direct

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.browser.VideoSniffer
import com.example.data.model.DetectedMedia
import com.example.ui.common.FormatUtils
import com.example.ui.common.QualitySelectionSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class TestVideoPreset(
    val title: String,
    val description: String,
    val url: String,
    val resolution: String,
    val format: String,
    val approxSize: String
)

@Composable
fun FastLinkScreen(
    videoSniffer: VideoSniffer,
    onStartDownload: (url: String, title: String, resolution: String, format: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var urlText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeDetectedMedia by remember { mutableStateOf<DetectedMedia?>(null) }

    val testPresets = remember {
        listOf(
            TestVideoPreset(
                title = "Big Buck Bunny (Blender)",
                description = "Open source test video • Peach Project",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                resolution = "1080p FHD",
                format = "MP4",
                approxSize = "158 MB"
            ),
            TestVideoPreset(
                title = "Elephants Dream",
                description = "World's first open computer animation movie",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                resolution = "720p HD",
                format = "MP4",
                approxSize = "140 MB"
            ),
            TestVideoPreset(
                title = "For Bigger Blazes (Nature)",
                description = "Chromecast Ultra HDR Sample",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                resolution = "1080p FHD",
                format = "MP4",
                approxSize = "15 MB"
            ),
            TestVideoPreset(
                title = "Tears of Steel (Sci-Fi VFX)",
                description = "Blender VFX open test project",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                resolution = "1080p FHD",
                format = "MP4",
                approxSize = "75 MB"
            )
        )
    }

    val analyzeLink: (String) -> Unit = { targetUrl ->
        val trimmed = targetUrl.trim()
        if (trimmed.isBlank()) {
            errorMessage = "Please enter or paste a valid video URL"
        } else {
            errorMessage = null
            isAnalyzing = true
            coroutineScope.launch {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .followRedirects(true)
                        .build()

                    val headReq = Request.Builder()
                        .url(trimmed)
                        .head()
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                        .build()

                    val (size, contentType) = withContext(Dispatchers.IO) {
                        try {
                            client.newCall(headReq).execute().use { response ->
                                val len = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                                val ct = response.headers["Content-Type"] ?: "video/mp4"
                                Pair(len, ct)
                            }
                        } catch (e: Exception) {
                            Pair(24 * 1024 * 1024L, "video/mp4")
                        }
                    }

                    val detected = DetectedMedia(
                        url = trimmed,
                        title = trimmed.substringAfterLast("/").substringBefore("?").ifBlank { "Video_${System.currentTimeMillis() % 10000}" },
                        format = if (contentType.contains("webm")) "WebM" else "MP4",
                        detectedResolution = "1080p",
                        mimeType = contentType,
                        fileSizeBytes = size
                    )
                    activeDetectedMedia = detected
                } catch (e: Exception) {
                    errorMessage = "Could not reach URL. Please verify link."
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("fast_link_screen")
    ) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Universal Fast Link",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Paste any direct video link or supported stream URL to inspect qualities and download with high-speed multi-part streaming.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Link Input Box
        Text(
            text = "Video URL",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = urlText,
            onValueChange = {
                urlText = it
                errorMessage = null
            },
            placeholder = { Text("https://example.com/video.mp4") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val pasteData = clip.getItemAt(0).text?.toString() ?: ""
                            urlText = pasteData
                            errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .height(36.dp)
                        .testTag("paste_clipboard_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste", fontSize = 12.sp)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fast_link_input"),
            shape = RoundedCornerShape(14.dp),
            isError = errorMessage != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Analyze and Download Button
        Button(
            onClick = { analyzeLink(urlText) },
            enabled = !isAnalyzing && urlText.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("analyze_and_download_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing Video Stream…", fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Inspect & Download Video", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Preset Test Videos Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Instant Test Videos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "Royalty-Free Open Media",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = "Tap any preset video to immediately test high-speed downloading and playback.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        testPresets.forEach { preset ->
            Card(
                onClick = {
                    urlText = preset.url
                    analyzeLink(preset.url)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .testTag("preset_${preset.title.replace(" ", "_")}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = preset.resolution,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = preset.approxSize,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download preset",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Notice and Policy reminder
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "StreamFetch supports saving public open-source videos, educational content, and personal media files. DRM-protected or proprietary streaming locks are not supported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Quality Selection Sheet
    activeDetectedMedia?.let { media ->
        val options = remember(media) { videoSniffer.generateQualityOptions(media) }
        QualitySelectionSheet(
            media = media,
            qualityOptions = options,
            onDismiss = { activeDetectedMedia = null },
            onStartDownload = { url, title, res, fmt ->
                onStartDownload(url, title, res, fmt)
                activeDetectedMedia = null
            }
        )
    }
}
