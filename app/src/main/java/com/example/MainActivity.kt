package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.browser.VideoSniffer
import com.example.data.download.DownloadManager
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadStatus
import com.example.ui.browser.BrowserScreen
import com.example.ui.direct.FastLinkScreen
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.library.VideoLibraryScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    BROWSER("Browser", Icons.Filled.Language, Icons.Outlined.Language, "tab_browser"),
    FAST_LINK("Fast Link", Icons.Filled.Bolt, Icons.Outlined.Bolt, "tab_fast_link"),
    DOWNLOADS("Downloads", Icons.Filled.CloudDownload, Icons.Outlined.CloudDownload, "tab_downloads"),
    LIBRARY("Saved", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "tab_library")
}

class MainActivity : ComponentActivity() {

    private lateinit var downloadManager: DownloadManager
    private lateinit var database: AppDatabase
    private val videoSniffer = VideoSniffer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(this)
        downloadManager = DownloadManager.getInstance(this)

        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    downloadManager = downloadManager,
                    database = database,
                    videoSniffer = videoSniffer
                )
            }
        }
    }
}

@Composable
fun MainAppScreen(
    downloadManager: DownloadManager,
    database: AppDatabase,
    videoSniffer: VideoSniffer
) {
    var currentTab by remember { mutableStateOf(AppTab.BROWSER) }
    val tasks by downloadManager.tasks.collectAsStateWithLifecycle()
    val activeCount = remember(tasks) {
        tasks.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val handleStartDownload: (String, String, String, String) -> Unit = { url, title, resolution, format ->
        downloadManager.startDownload(url, title, resolution, format)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Downloading: $title ($resolution)",
                actionLabel = "View",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentTab = AppTab.DOWNLOADS
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == AppTab.DOWNLOADS && activeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text("$activeCount")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            label = "tab_crossfade",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                AppTab.BROWSER -> BrowserScreen(
                    videoSniffer = videoSniffer,
                    onStartDownload = handleStartDownload
                )
                AppTab.FAST_LINK -> FastLinkScreen(
                    videoSniffer = videoSniffer,
                    onStartDownload = handleStartDownload
                )
                AppTab.DOWNLOADS -> DownloadsScreen(
                    downloadManager = downloadManager,
                    onNavigateToBrowser = { currentTab = AppTab.BROWSER }
                )
                AppTab.LIBRARY -> VideoLibraryScreen(
                    database = database,
                    onNavigateToBrowser = { currentTab = AppTab.BROWSER }
                )
            }
        }
    }
}
