package com.example.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.model.VideoItem
import com.example.ui.common.FormatUtils
import com.example.ui.common.VideoLauncher
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream

private const val TAG = "VideoPlayerDialog"

@Composable
fun VideoPlayerDialog(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(video.durationMs) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

    val file = remember(video.filePath) { File(video.filePath) }
    val fileExists = remember(file) { file.exists() && file.length() > 0 }

    // Auto-hide controls
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(4000)
            isControlsVisible = false
        }
    }

    // Progress ticker
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayerRef?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        currentPosition = mp.currentPosition.toLong()
                        if (mp.duration > 0) {
                            totalDuration = mp.duration.toLong()
                        }
                    }
                } catch (_: Exception) {}
            }
            delay(400)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isControlsVisible = !isControlsVisible
                }
                .testTag("fullscreen_video_player")
        ) {
            if (!fileExists) {
                // File missing or empty state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Video File Not Found",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The downloaded file could not be read (${video.filePath}). Please try downloading again.",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            } else {
                // TextureView-based video rendering for 100% reliable Compose drawing
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )

                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                    val surface = Surface(surfaceTexture)
                                    val mp = MediaPlayer()
                                    mediaPlayerRef = mp
                                    try {
                                        mp.setSurface(surface)

                                        // Open via FileDescriptor to bypass Android 10+ mediaserver permission issues
                                        FileInputStream(file).use { fis ->
                                            mp.setDataSource(fis.fd, 0, file.length())
                                        }

                                        mp.setOnPreparedListener { preparedMp ->
                                            isBuffering = false
                                            playbackError = null
                                            totalDuration = preparedMp.duration.toLong()
                                            preparedMp.isLooping = true
                                            preparedMp.start()
                                            isPlaying = true
                                        }

                                        mp.setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                                            if (videoWidth > 0 && videoHeight > 0) {
                                                adjustAspectRatio(this@apply, videoWidth, videoHeight, width, height)
                                            }
                                        }

                                        mp.setOnErrorListener { _, what, extra ->
                                            Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                                            isBuffering = false
                                            isPlaying = false
                                            playbackError = "Decoder error ($what, $extra). Open with external player."
                                            true
                                        }

                                        mp.prepareAsync()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to initialize MediaPlayer", e)
                                        isBuffering = false
                                        playbackError = e.localizedMessage ?: "Failed to open video stream"
                                    }
                                }

                                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                    try {
                                        mediaPlayerRef?.stop()
                                        mediaPlayerRef?.release()
                                        mediaPlayerRef = null
                                    } catch (_: Exception) {}
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                            }
                            textureViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isBuffering && fileExists && playbackError == null) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Error Overlay Banner if codec/player failed
            playbackError?.let { err ->
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Playback Note",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { VideoLauncher.openInExternalPlayer(context, video) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open in External Player")
                            }
                        }
                    }
                }
            }

            // Controls Overlay
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close player",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${video.resolution} • ${video.format} • ${FormatUtils.formatBytes(video.fileSizeBytes)}",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Open in External Player
                        IconButton(
                            onClick = { VideoLauncher.openInExternalPlayer(context, video) },
                            modifier = Modifier.testTag("player_open_external_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open in External App",
                                tint = Color.White
                            )
                        }

                        // Share action
                        IconButton(
                            onClick = { shareVideo(context, video) },
                            modifier = Modifier.testTag("player_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share video",
                                tint = Color.White
                            )
                        }
                    }

                    // Center Quick Controls (Rewind 10, Play/Pause, Forward 10)
                    if (fileExists && playbackError == null) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    mediaPlayerRef?.let { mp ->
                                        try {
                                            val newPos = (mp.currentPosition - 10000).coerceAtLeast(0)
                                            mp.seekTo(newPos)
                                            currentPosition = newPos.toLong()
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .testTag("player_replay_10")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Main Play / Pause
                            IconButton(
                                onClick = {
                                    mediaPlayerRef?.let { mp ->
                                        try {
                                            if (mp.isPlaying) {
                                                mp.pause()
                                                isPlaying = false
                                            } else {
                                                mp.start()
                                                isPlaying = true
                                            }
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("player_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    mediaPlayerRef?.let { mp ->
                                        try {
                                            val newPos = (mp.currentPosition + 10000).coerceAtMost(mp.duration)
                                            mp.seekTo(newPos)
                                            currentPosition = newPos.toLong()
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .testTag("player_forward_10")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Bottom Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        if (totalDuration > 0) {
                            Slider(
                                value = currentPosition.toFloat().coerceIn(0f, totalDuration.toFloat()),
                                onValueChange = { newPos ->
                                    currentPosition = newPos.toLong()
                                    try {
                                        mediaPlayerRef?.seekTo(newPos.toInt())
                                    } catch (_: Exception) {}
                                },
                                valueRange = 0f..totalDuration.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("player_seek_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${FormatUtils.formatDuration(currentPosition)} / ${FormatUtils.formatDuration(totalDuration)}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // External Player launch button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.clickable {
                                        VideoLauncher.openInExternalPlayer(context, video)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "External App",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Speed Selector
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.clickable {
                                        val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
                                        val nextIndex = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                                        val newSpeed = speeds[nextIndex]
                                        playbackSpeed = newSpeed
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                mediaPlayerRef?.let { mp ->
                                                    mp.playbackParams = mp.playbackParams.setSpeed(newSpeed)
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "Speed",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${playbackSpeed}x",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayerRef?.stop()
                mediaPlayerRef?.release()
                mediaPlayerRef = null
            } catch (_: Exception) {}
        }
    }
}

private fun adjustAspectRatio(textureView: TextureView, videoWidth: Int, videoHeight: Int, viewWidth: Int, viewHeight: Int) {
    if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
    val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()

    var scaleX = 1f
    var scaleY = 1f

    if (videoAspect > viewAspect) {
        scaleY = viewAspect / videoAspect
    } else {
        scaleX = videoAspect / viewAspect
    }

    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}

private fun shareVideo(context: Context, video: VideoItem) {
    try {
        val file = File(video.filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, video.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
    } catch (_: Exception) {}
}
