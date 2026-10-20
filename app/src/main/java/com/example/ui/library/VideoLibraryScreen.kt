package com.example.ui.library

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import com.example.ui.common.VideoLauncher
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.AppDatabase
import com.example.data.model.VideoItem
import com.example.ui.common.FormatUtils
import com.example.ui.player.VideoPlayerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VideoLibraryScreen(
    database: AppDatabase,
    onNavigateToBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val videoEntities by database.videoDao().getAllVideos().collectAsStateWithLifecycle(initialValue = emptyList())

    val videos = remember(videoEntities) { videoEntities.map { it.toVideoItem() } }
    var searchQuery by remember { mutableStateOf("") }
    var activePlayingVideo by remember { mutableStateOf<VideoItem?>(null) }
    var videoToRename by remember { mutableStateOf<VideoItem?>(null) }
    var videoToDelete by remember { mutableStateOf<VideoItem?>(null) }
    var videoToShowDetails by remember { mutableStateOf<VideoItem?>(null) }

    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isBlank()) videos
        else videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val totalStorageUsed = remember(videos) {
        videos.sumOf { it.fileSizeBytes }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("video_library_screen")
    ) {
        // Top Header
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
                            text = "Saved Video Library",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${videos.size} videos • ${FormatUtils.formatBytes(totalStorageUsed)} stored offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (videos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search saved videos…", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("library_search_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }

        if (videos.isEmpty()) {
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
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Your Library is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Downloaded videos will appear here for instant offline playback without needing an internet connection.",
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
                        modifier = Modifier.testTag("library_empty_browse_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Discover & Download Videos", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredVideos, key = { it.id }) { video ->
                    VideoItemRow(
                        video = video,
                        onClick = { activePlayingVideo = video },
                        onRename = { videoToRename = video },
                        onDelete = { videoToDelete = video },
                        onShowDetails = { videoToShowDetails = video },
                        onShare = { shareVideo(context, video) }
                    )
                }
            }
        }
    }

    // Video Player
    activePlayingVideo?.let { video ->
        VideoPlayerDialog(
            video = video,
            onDismiss = { activePlayingVideo = null }
        )
    }

    // Rename Dialog
    videoToRename?.let { video ->
        var newTitle by remember { mutableStateOf(video.title) }
        AlertDialog(
            onDismissRequest = { videoToRename = null },
            title = { Text("Rename Video", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newTitle.trim()
                        if (trimmed.isNotBlank()) {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    database.videoDao().updateTitle(video.id, trimmed)
                                }
                                videoToRename = null
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Video?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently delete \"${video.title}\" from your device storage.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                val file = File(video.filePath)
                                if (file.exists()) {
                                    file.delete()
                                }
                                database.videoDao().deleteVideoById(video.id)
                            }
                            videoToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Video Details Dialog
    videoToShowDetails?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToShowDetails = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Video Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Title: ${video.title}", fontWeight = FontWeight.SemiBold)
                    Text("Resolution: ${video.resolution}")
                    Text("Format: ${video.format}")
                    Text("Size: ${FormatUtils.formatBytes(video.fileSizeBytes)}")
                    if (video.durationMs > 0) {
                        Text("Duration: ${FormatUtils.formatDuration(video.durationMs)}")
                    }
                    Text("Saved: ${FormatUtils.formatDate(video.dateAdded)}")
                    Text("Path: ${video.filePath}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { videoToShowDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun VideoItemRow(
    video: VideoItem,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("library_video_card_${video.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (video.thumbnailUri != null && File(video.thumbnailUri).exists()) {
                    AsyncImage(
                        model = File(video.thumbnailUri),
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Duration badge
                if (video.durationMs > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatDuration(video.durationMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${video.resolution} • ${FormatUtils.formatBytes(video.fileSizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = FormatUtils.formatDate(video.dateAdded),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { VideoLauncher.openInExternalPlayer(context, video) },
                    modifier = Modifier.testTag("video_open_external_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open with external player",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("video_menu_button_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play in App") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open with External Player") },
                            leadingIcon = { Icon(Icons.Default.OpenInNew, null) },
                            onClick = {
                                showMenu = false
                                VideoLauncher.openInExternalPlayer(context, video)
                            }
                        )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = {
                            showMenu = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("File Details") },
                        leadingIcon = { Icon(Icons.Default.Info, null) },
                        onClick = {
                            showMenu = false
                            onShowDetails()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
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
