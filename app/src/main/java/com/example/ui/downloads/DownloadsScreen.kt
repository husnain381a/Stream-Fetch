package com.example.ui.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.common.VideoLauncher
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.download.DownloadManager
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.VideoItem
import com.example.ui.common.FormatUtils
import com.example.ui.player.VideoPlayerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloadManager: DownloadManager,
    onNavigateToBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by downloadManager.tasks.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedVideoForPlayback by remember { mutableStateOf<VideoItem?>(null) }

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            "Active" -> tasks.filter {
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING || it.status == DownloadStatus.PAUSED
            }
            "Completed" -> tasks.filter { it.status == DownloadStatus.COMPLETED }
            else -> tasks
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("downloads_screen")
    ) {
        // Header
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Download Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${tasks.count { it.status == DownloadStatus.DOWNLOADING }} downloading • ${tasks.count { it.status == DownloadStatus.COMPLETED }} completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (tasks.any { it.status == DownloadStatus.COMPLETED }) {
                        IconButton(
                            onClick = { downloadManager.clearCompleted() },
                            modifier = Modifier.testTag("clear_completed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear completed",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Active", "Completed").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_chip_$filter")
                        )
                    }
                }
            }
        }

        // List of tasks
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedFilter == "Active") "No active downloads" else "No downloads yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Browse websites with the built-in browser or paste a video link to begin downloading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToBrowser,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("empty_explore_videos_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse Videos Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    DownloadTaskCard(
                        task = task,
                        onPause = { downloadManager.pauseDownload(task.id) },
                        onResume = { downloadManager.resumeDownload(task.id) },
                        onCancel = { downloadManager.cancelDownload(task.id) },
                        onRetry = { downloadManager.retryDownload(task.id) },
                        onPlay = {
                            selectedVideoForPlayback = VideoItem(
                                title = task.title,
                                filePath = task.filePath,
                                fileSizeBytes = task.totalBytes,
                                resolution = task.resolution,
                                format = task.format
                            )
                        }
                    )
                }
            }
        }
    }

    selectedVideoForPlayback?.let { video ->
        VideoPlayerDialog(
            video = video,
            onDismiss = { selectedVideoForPlayback = null }
        )
    }
}

@Composable
fun DownloadTaskCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = task.status == DownloadStatus.COMPLETED) { onPlay() }
            .testTag("download_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (task.status) {
                DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                DownloadStatus.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.4f)
                DownloadStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (task.status) {
                                DownloadStatus.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.15f)
                                DownloadStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (task.status) {
                            DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                            DownloadStatus.FAILED -> Icons.Default.Error
                            else -> Icons.Default.Movie
                        },
                        contentDescription = null,
                        tint = when (task.status) {
                            DownloadStatus.COMPLETED -> Color(0xFF10B981)
                            DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = task.resolution,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = task.format,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Controls
                when (task.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onPlay,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("play_completed_button_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { VideoLauncher.openInExternalPlayer(context, task.filePath, task.title) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("open_external_button_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open in player",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open With", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    DownloadStatus.PENDING -> {
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    DownloadStatus.CANCELLED -> {}
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar (if active or paused)
            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.PENDING) {
                LinearProgressIndicator(
                    progress = { task.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (task.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stats row: Downloaded / Total, Percentage, Speed, ETA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (task.totalBytes > 0) {
                            "${FormatUtils.formatBytes(task.downloadedBytes)} / ${FormatUtils.formatBytes(task.totalBytes)} (${task.progressPercent}%)"
                        } else {
                            "${FormatUtils.formatBytes(task.downloadedBytes)} downloaded"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (task.status == DownloadStatus.DOWNLOADING) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = FormatUtils.formatSpeed(task.speedBytesPerSec),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (task.etaSeconds > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = FormatUtils.formatEta(task.etaSeconds),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (task.status == DownloadStatus.PAUSED) {
                        Text(
                            text = "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (task.status == DownloadStatus.COMPLETED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Completed • ${FormatUtils.formatBytes(task.totalBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = task.completedAt?.let { FormatUtils.formatDate(it) } ?: "Just now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (task.status == DownloadStatus.FAILED) {
                Text(
                    text = task.errorMessage ?: "Download interrupted or network timed out",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
