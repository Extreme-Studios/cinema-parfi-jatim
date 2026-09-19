package com.cinemaparfi.jatim.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cinemaparfi.jatim.data.model.Song
import java.util.Locale
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: Song,
    isTrailer: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val sourceUrl = remember(video.id, isTrailer, video.trailerUrl, video.movieUrl, video.audioUrl) {
        if (isTrailer) video.trailerUrl else video.movieUrl.ifBlank { video.audioUrl }
    }
    var isLoading by remember(video.id, isTrailer) { mutableStateOf(true) }
    var errorMessage by remember(video.id, isTrailer) { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var fullscreenContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    val youtubeId = remember(sourceUrl) { resolveYoutubeIdFromUrl(sourceUrl) }
    val displayError = remember(youtubeId, sourceUrl, errorMessage) {
        when {
            !errorMessage.isNullOrBlank() -> errorMessage
            youtubeId.isBlank() && sourceUrl.isBlank() -> "URL video tidak valid."
            else -> null
        }
    }

    val player = remember(context, sourceUrl) {
        if (youtubeId.isBlank() && sourceUrl.isNotBlank()) {
            ExoPlayer.Builder(context).build().apply {
                val playUrl = sourceUrl.toDirectStreamUrl()
                setMediaItem(MediaItem.fromUri(playUrl))
                prepare()
                playWhenReady = true
            }
        } else {
            null
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isLoading = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                errorMessage = "Gagal memuat video: ${error.localizedMessage}"
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }

    fun showSystemBars() {
        val window = activity?.window ?: return
        val decorView = window.decorView
        WindowCompat.getInsetsController(window, decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    fun hideSystemBars() {
        val window = activity?.window ?: return
        val decorView = window.decorView
        WindowCompat.getInsetsController(window, decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun enterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        val activityWindow = activity?.window ?: return
        val decorView = activityWindow.decorView as? ViewGroup ?: return
        if (customFullscreenView != null) return

        (view.parent as? ViewGroup)?.removeView(view)
        val container = FrameLayout(activity ?: context).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        decorView.addView(
            container,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        customFullscreenView = view
        fullscreenCallback = callback
        fullscreenContainer = container
        isFullscreen = true
        hideSystemBars()
    }

    fun exitFullscreen() {
        val activityWindow = activity?.window ?: return
        val decorView = activityWindow.decorView as? ViewGroup ?: return
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        customFullscreenView = null
        fullscreenContainer?.let { decorView.removeView(it) }
        fullscreenContainer = null
        isFullscreen = false
        showSystemBars()
    }

    BackHandler {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            onBack()
        }
    }

    DisposableEffect(activity) {
        onDispose {
            runCatching { webView?.destroy() }
            if (isFullscreen) {
                exitFullscreen()
            } else {
                showSystemBars()
            }
        }
    }

    LaunchedEffect(video.id, isTrailer, sourceUrl, youtubeId) {
        isLoading = true
        errorMessage = null
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Cinema PARFI Jatim",
                                style = MaterialTheme.typography.labelSmall,
                                color = ComposeColor(0xFFD7A42E),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = ComposeColor.White,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = ComposeColor.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ComposeColor(0xE60B2852))
                )
            }
        },
        containerColor = ComposeColor.Black
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ComposeColor.Black)
                .padding(paddingValues)
        ) {
            if (youtubeId.isNotBlank()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webView = this
                            setBackgroundColor(Color.BLACK)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                allowContentAccess = true
                                allowFileAccess = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = MOBILE_CHROME_USER_AGENT
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    if (view == null || callback == null) return
                                    enterFullscreen(view, callback)
                                }

                                override fun onHideCustomView() {
                                    exitFullscreen()
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val targetUrl = request?.url?.toString().orEmpty()
                                    if (targetUrl.isBlank()) return true

                                    val targetUri = runCatching { Uri.parse(targetUrl) }.getOrNull()
                                    val host = targetUri?.host.orEmpty().lowercase(Locale.ROOT)
                                    val isYoutubeHost = host.contains("youtube.com") || host.contains("youtu.be")
                                    return if (isYoutubeHost) {
                                        view?.loadUrl(targetUrl)
                                        true
                                    } else {
                                        false
                                    }
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    isLoading = true
                                    errorMessage = null
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (youtubeId.isNotBlank()) {
                                        isLoading = false
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        errorMessage = "Video gagal dimuat."
                                        isLoading = false
                                    }
                                }
                            }

                            loadUrl("https://m.youtube.com/watch?v=$youtubeId")
                        }
                    },
                    update = { view ->
                        val expectedUrl = "https://m.youtube.com/watch?v=$youtubeId"
                        if (view.url.isNullOrBlank()) {
                            view.loadUrl(expectedUrl)
                        }
                    }
                )
            } else if (sourceUrl.isNotBlank() && player != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            setBackgroundColor(Color.BLACK)
                            useController = true
                        }
                    }
                )
            }

            if (isLoading && errorMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor(0xF007152D)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = ComposeColor(0xFFD7A42E)
                        )
                        Text(
                            text = "Memuat video",
                            color = ComposeColor.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!displayError.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor(0xF007152D)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = displayError.orEmpty(),
                            color = ComposeColor.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (sourceUrl.isNotBlank()) {
                            Button(
                                onClick = { openExternal(context, sourceUrl) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ComposeColor(0xFFD7A42E),
                                    contentColor = ComposeColor(0xFF07152D)
                                )
                            ) {
                                Text("Buka di YouTube / Browser")
                            }
                        }
                    }
                }
            }

            if (isFullscreen) {
                TextButton(
                    onClick = { exitFullscreen() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Text("Keluar fullscreen", color = ComposeColor.White)
                }
            }
        }
    }
}

internal fun resolveYoutubeId(video: Song): String {
    val candidates = listOf(video.trailerUrl, video.movieUrl, video.audioUrl)
    for (candidate in candidates) {
        val id = candidate.extractYoutubeId()
        if (id.isNotBlank()) return id
    }
    return ""
}

internal fun resolveYoutubeIdFromUrl(url: String): String = url.extractYoutubeId()

internal fun String.extractYoutubeId(): String {
    val raw = trim()
    if (raw.isBlank()) return ""

    val uri = runCatching { Uri.parse(raw) }.getOrNull()
    if (uri != null) {
        val host = uri.host.orEmpty().lowercase(Locale.ROOT)
        val path = uri.path.orEmpty().trim('/')

        if (host.contains("youtube.com")) {
            val v = uri.getQueryParameter("v").orEmpty()
            if (v.isNotBlank()) return v.substringBefore('&')

            if (path.startsWith("embed/", ignoreCase = true)) {
                val id = path.removePrefix("embed/").substringBefore('/')
                if (id.isNotBlank()) return id
            }

            if (path.startsWith("shorts/", ignoreCase = true)) {
                val id = path.removePrefix("shorts/").substringBefore('/')
                if (id.isNotBlank()) return id
            }
        }

        if (host.contains("youtu.be")) {
            val id = path.substringBefore('/')
            if (id.isNotBlank()) return id
        }
    }

    val fallbackRegex = Regex("""(?:v=|youtu\.be/|embed/|shorts/)([A-Za-z0-9_-]{6,})""", RegexOption.IGNORE_CASE)
    fallbackRegex.find(raw)?.groupValues?.getOrNull(1)?.let { return it }
    return ""
}

private fun openExternal(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

internal fun String.toDirectStreamUrl(): String {
    val raw = this.trim()
    if (raw.isBlank()) return ""

    val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return raw
    val host = uri.host.orEmpty().lowercase(Locale.ROOT)
    
    if (host.contains("drive.google.com") || host.contains("docs.google.com")) {
        val path = uri.path.orEmpty()
        if (path.contains("/file/d/")) {
            val segments = uri.pathSegments
            val fileIndex = segments.indexOf("d")
            if (fileIndex != -1 && fileIndex + 1 < segments.size) {
                val fileId = segments[fileIndex + 1]
                return "https://docs.google.com/uc?export=download&id=$fileId"
            }
        }
        
        val id = uri.getQueryParameter("id").orEmpty()
        if (id.isNotBlank()) {
            return "https://docs.google.com/uc?export=download&id=$id"
        }
    }
    
    return raw
}

private const val MOBILE_CHROME_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
