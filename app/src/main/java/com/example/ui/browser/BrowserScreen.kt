package com.example.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.browser.VideoSniffer
import com.example.data.model.DetectedMedia
import com.example.ui.common.FormatUtils
import com.example.ui.common.QualitySelectionSheet

data class QuickPortal(
    val title: String,
    val url: String,
    val iconEmoji: String
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    videoSniffer: VideoSniffer,
    onStartDownload: (url: String, title: String, resolution: String, format: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var urlInput by remember { mutableStateOf("https://peach.blender.org/") }
    var currentWebUrl by remember { mutableStateOf("https://peach.blender.org/") }
    var webTitle by remember { mutableStateOf("Peach Blender Open Movie") }
    var pageProgress by remember { mutableIntStateOf(0) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    val detectedList by videoSniffer.detectedVideos.collectAsStateWithLifecycle()
    var selectedMediaForDownload by remember { mutableStateOf<DetectedMedia?>(null) }
    var showDetectedListSheet by remember { mutableStateOf(false) }

    val quickPortals = remember {
        listOf(
            QuickPortal("Big Buck Bunny", "https://peach.blender.org/", "🎬"),
            QuickPortal("Pexels Videos", "https://www.pexels.com/videos/", "📹"),
            QuickPortal("Pixabay Media", "https://pixabay.com/videos/", "🎥"),
            QuickPortal("Wikimedia", "https://commons.wikimedia.org/wiki/Category:Videos", "🌐"),
            QuickPortal("Archive Movies", "https://archive.org/details/movies", "🎞️")
        )
    }

    // Handle Android system back press to navigate webview back if possible
    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("browser_screen")
    ) {
        // Browser Top Controls
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Navigation buttons & URL bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("browser_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("browser_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("browser_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Address Bar
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("browser_url_input"),
                        singleLine = true,
                        placeholder = { Text("Search or type URL…", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (currentWebUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                                contentDescription = "Security",
                                tint = if (currentWebUrl.startsWith("https://")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (urlInput.isNotBlank()) {
                                IconButton(onClick = { urlInput = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                val target = sanitizeBrowserInput(urlInput)
                                currentWebUrl = target
                                webViewRef?.loadUrl(target)
                            }
                        ),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Desktop mode toggle
                    IconButton(
                        onClick = {
                            isDesktopMode = !isDesktopMode
                            webViewRef?.settings?.let { s ->
                                if (isDesktopMode) {
                                    s.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                } else {
                                    s.userAgentString = null
                                }
                                webViewRef?.reload()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("browser_desktop_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                            contentDescription = if (isDesktopMode) "Desktop View Active" else "Mobile View Active",
                            tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Portals Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(quickPortals) { portal ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .clickable {
                                    urlInput = portal.url
                                    currentWebUrl = portal.url
                                    webViewRef?.loadUrl(portal.url)
                                }
                                .testTag("quick_portal_${portal.title.replace(" ", "_")}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = portal.iconEmoji, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = portal.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Page Loading Progress Bar
                if (pageProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // WebView and Floating Download Detector Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            allowFileAccess = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        addJavascriptInterface(videoSniffer.javascriptBridge, "AndroidBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let {
                                    urlInput = it
                                    currentWebUrl = it
                                    videoSniffer.clearForNewPage()
                                    videoSniffer.inspectUrl(it, pageTitle = webTitle, pageUrl = it)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let {
                                    canGoBack = view?.canGoBack() ?: false
                                    canGoForward = view?.canGoForward() ?: false
                                    webTitle = view?.title ?: "Web Video"
                                    videoSniffer.inspectUrl(it, pageTitle = webTitle, pageUrl = it)
                                }
                                // Inject media sniffer JS
                                view?.evaluateJavascript(VideoSniffer.JS_SNIFFER, null)
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                request?.url?.toString()?.let { reqUrl ->
                                    videoSniffer.inspectUrl(reqUrl, pageTitle = webTitle, pageUrl = currentWebUrl)
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onLoadResource(view: WebView?, url: String?) {
                                super.onLoadResource(view, url)
                                url?.let { videoSniffer.inspectUrl(it, pageTitle = webTitle, pageUrl = currentWebUrl) }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress
                                if (newProgress > 60) {
                                    view?.evaluateJavascript(VideoSniffer.JS_SNIFFER, null)
                                }
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrBlank()) {
                                    webTitle = title
                                }
                            }
                        }

                        loadUrl(currentWebUrl)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Smart Floating Video Detector Button
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 20.dp)
            ) {
                this@Column.AnimatedVisibility(
                    visible = detectedList.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn() + scaleIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut() + scaleOut()
                ) {
                FloatingActionButton(
                    onClick = {
                        if (detectedList.size == 1) {
                            selectedMediaForDownload = detectedList.first()
                        } else {
                            showDetectedListSheet = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .scale(pulseScale)
                        .testTag("floating_download_detector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Text("${detectedList.size}")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Videos",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Download Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

    // Modal Sheet showing multiple detected videos if more than one is found
    if (showDetectedListSheet && detectedList.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showDetectedListSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Detected Videos (${detectedList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select a video stream to customize quality and download",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                detectedList.forEach { media ->
                    Card(
                        onClick = {
                            showDetectedListSheet = false
                            selectedMediaForDownload = media
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("detected_video_item_${media.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = media.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${media.format} • ${if (media.fileSizeBytes > 0) FormatUtils.formatBytes(media.fileSizeBytes) else "Online Stream"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Quality Selection Sheet
    selectedMediaForDownload?.let { media ->
        val options = remember(media) { videoSniffer.generateQualityOptions(media) }
        QualitySelectionSheet(
            media = media,
            qualityOptions = options,
            onDismiss = { selectedMediaForDownload = null },
            onStartDownload = { url, title, res, fmt ->
                onStartDownload(url, title, res, fmt)
                selectedMediaForDownload = null
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }
}

private fun sanitizeBrowserInput(input: String): String {
    val trimmed = input.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }
    // Check if it's a domain name (contains . and no space)
    if (trimmed.contains(".") && !trimmed.contains(" ")) {
        return "https://$trimmed"
    }
    // Search query via DuckDuckGo
    return "https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
}
