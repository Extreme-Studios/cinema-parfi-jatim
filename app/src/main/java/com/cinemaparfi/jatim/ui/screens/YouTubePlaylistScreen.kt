package com.cinemaparfi.jatim.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

private const val MobileChromeUserAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubePlaylistScreen(
    playlistId: String,
    title: String,
    channelName: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val playlistUrl = remember(playlistId) { "https://m.youtube.com/playlist?list=$playlistId" }
    var loading by remember(playlistId) { mutableStateOf(true) }
    var loadFailed by remember(playlistId) { mutableStateOf(false) }
    var fullscreenContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    fun leaveFullscreen(notifyWebView: Boolean = true) {
        val window = activity?.window ?: return
        val decor = window.decorView as? ViewGroup ?: return
        if (notifyWebView) fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        fullscreenContainer?.let(decor::removeView)
        fullscreenContainer = null
        isFullscreen = false
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    BackHandler {
        if (isFullscreen) leaveFullscreen() else onBack()
    }

    DisposableEffect(activity) {
        onDispose {
            if (isFullscreen) leaveFullscreen()
        }
    }

    Scaffold(
        containerColor = ComposeColor.Black,
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(channelName, color = ComposeColor(0xFFD7A42E), fontWeight = FontWeight.Bold)
                            Text(title, color = ComposeColor.White, fontWeight = FontWeight.ExtraBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = ComposeColor.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ComposeColor(0xFF0B0F12))
                )
            }
        }
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().background(ComposeColor.Black).padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(Color.BLACK)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            userAgentString = MobileChromeUserAgent
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val host = request.url.host.orEmpty()
                                return !host.endsWith("youtube.com") && host != "youtu.be"
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                loading = false
                            }

                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                if (request.isForMainFrame) {
                                    loading = false
                                    loadFailed = true
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                val window = activity?.window ?: return
                                val decor = window.decorView as? ViewGroup ?: return
                                if (fullscreenContainer != null) return
                                (view.parent as? ViewGroup)?.removeView(view)
                                val container = FrameLayout(ctx).apply {
                                    setBackgroundColor(Color.BLACK)
                                    addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                                }
                                decor.addView(container, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                                fullscreenContainer = container
                                fullscreenCallback = callback
                                isFullscreen = true
                                WindowCompat.getInsetsController(window, window.decorView).apply {
                                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                    hide(WindowInsetsCompat.Type.systemBars())
                                }
                            }

                            override fun onHideCustomView() {
                                leaveFullscreen(notifyWebView = false)
                            }
                        }
                        loadUrl(playlistUrl)
                    }
                },
                update = { view -> if (view.url.isNullOrBlank()) view.loadUrl(playlistUrl) }
            )

            if (loading && !loadFailed) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ComposeColor(0xFFE7B946))
            }
            if (loadFailed) {
                Card(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF171A1D)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Playlist tidak dapat dimuat", color = ComposeColor.White, fontWeight = FontWeight.Bold)
                        Text("Periksa koneksi internet atau buka lewat YouTube.", color = ComposeColor(0xFFC3C6C8))
                        Button(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(playlistUrl))) },
                            colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFFE7B946), contentColor = ComposeColor.Black)
                        ) { Text("Buka di YouTube") }
                    }
                }
            }
        }
    }
}
