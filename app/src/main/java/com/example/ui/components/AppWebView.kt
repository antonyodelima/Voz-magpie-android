package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.theme.VozoAccent
import com.example.theme.VozoBorder
import com.example.theme.VozoDarkBg
import com.example.theme.VozoDarkSurface
import com.example.theme.VozoDarkSurfaceVariant
import com.example.theme.VozoError
import com.example.theme.VozoPrimary
import com.example.theme.VozoTextMuted
import com.example.theme.VozoTextPrimary
import com.example.theme.VozoTextSecondary

/**
 * Configuration options for the reusable AppWebView.
 */
data class WebViewConfig(
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val databaseEnabled: Boolean = true,
    val supportZoom: Boolean = true,
    val allowFileAccess: Boolean = true,
    val mediaPlaybackRequiresUserGesture: Boolean = false,
    val customUserAgent: String? = null,
    val showNavigationControls: Boolean = false
)

/**
 * Reusable WebView composable that accepts a URL and handles:
 * - Basic navigation (Back, Forward, Refresh, external link handling)
 * - Configurable settings (JavaScript enablement, DOM storage, zoom, caching)
 * - Comprehensive error handling for page loads (HTTP/network failures, rendering recovery, retry UI)
 * - Progress tracking
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AppWebView(
    url: String,
    modifier: Modifier = Modifier,
    config: WebViewConfig = WebViewConfig(),
    onLoadingChanged: ((isLoading: Boolean, progress: Int) -> Unit)? = null,
    onNavigationStateChanged: ((canGoBack: Boolean, canGoForward: Boolean) -> Unit)? = null,
    onError: ((errorCode: Int, description: String?, failingUrl: String?) -> Unit)? = null,
    onWebViewCreated: ((WebView) -> Unit)? = null
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(url) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failingUrl by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_reusable_webview_container")
    ) {
        // Optional navigation header toolbar
        if (config.showNavigationControls) {
            Surface(
                color = VozoDarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VozoBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.testTag("webview_nav_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = if (canGoBack) VozoTextPrimary else VozoTextMuted
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.testTag("webview_nav_forward")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Avançar",
                            tint = if (canGoForward) VozoTextPrimary else VozoTextMuted
                        )
                    }

                    IconButton(
                        onClick = {
                            hasError = false
                            webViewInstance?.reload()
                        },
                        modifier = Modifier.testTag("webview_nav_reload")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recarregar",
                            tint = VozoAccent
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = currentUrl,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = VozoTextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Linear progress indicator
        if (isLoading && progress < 100) {
            LinearProgressIndicator(
                progress = { (progress.coerceIn(0, 100)) / 100f },
                color = VozoPrimary,
                trackColor = VozoDarkBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .testTag("webview_linear_progress")
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("app_webview_element"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(0xFF0B0B10.toInt())

                        try {
                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        } catch (_: Exception) {}

                        // Cookie Manager configuration
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this@apply, true)

                        // WebSettings applying user-specified configuration
                        settings.apply {
                            javaScriptEnabled = config.javaScriptEnabled
                            domStorageEnabled = config.domStorageEnabled
                            databaseEnabled = config.databaseEnabled
                            allowFileAccess = config.allowFileAccess
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = config.mediaPlaybackRequiresUserGesture
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(config.supportZoom)
                            builtInZoomControls = false
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            if (!config.customUserAgent.isNullOrBlank()) {
                                userAgentString = config.customUserAgent
                            }
                        }

                        // WebViewClient: URL navigation, page lifecycle, and errors
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, pageUrl, favicon)
                                if (pageUrl != null) currentUrl = pageUrl
                                isLoading = true
                                hasError = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                onLoadingChanged?.invoke(true, 0)
                                onNavigationStateChanged?.invoke(canGoBack, canGoForward)
                            }

                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                if (pageUrl != null) currentUrl = pageUrl
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                onLoadingChanged?.invoke(false, 100)
                                onNavigationStateChanged?.invoke(canGoBack, canGoForward)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        error?.errorCode ?: -1
                                    } else {
                                        -1
                                    }
                                    val desc = error?.description?.toString() ?: "Falha de conexão"
                                    val reqUrl = request.url?.toString()

                                    hasError = true
                                    errorMessage = desc
                                    failingUrl = reqUrl
                                    isLoading = false

                                    onError?.invoke(code, desc, reqUrl)
                                }
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                hasError = true
                                errorMessage = "O processo do navegador foi reiniciado para economizar memória."
                                isLoading = false
                                onError?.invoke(-1, errorMessage, currentUrl)

                                try {
                                    (view?.parent as? ViewGroup)?.removeView(view)
                                    view?.destroy()
                                } catch (_: Exception) {}

                                return true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                return if (reqUrl.startsWith("http://") || reqUrl.startsWith("https://")) {
                                    false
                                } else {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reqUrl))
                                        context.startActivity(intent)
                                        true
                                    } catch (_: Exception) {
                                        true
                                    }
                                }
                            }
                        }

                        // WebChromeClient: Progress updates
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                progress = newProgress
                                isLoading = newProgress < 100
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                onLoadingChanged?.invoke(isLoading, newProgress)
                                onNavigationStateChanged?.invoke(canGoBack, canGoForward)
                            }
                        }

                        loadUrl(url)
                        webViewInstance = this
                        onWebViewCreated?.invoke(this)
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                    onWebViewCreated?.invoke(webView)
                }
            )

            // Reusable error handling overlay for page load errors
            if (hasError) {
                Surface(
                    color = VozoDarkBg.copy(alpha = 0.96f),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("webview_error_overlay")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(VozoError.copy(alpha = 0.15f))
                                .border(1.dp, VozoError.copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Erro de carregamento",
                                tint = VozoError,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Erro ao Carregar a Página",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = VozoTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMessage ?: "Não foi possível estabelecer conexão com o servidor.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = VozoTextSecondary,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        if (!failingUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = failingUrl ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = VozoTextMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    hasError = false
                                    isLoading = true
                                    webViewInstance?.loadUrl(failingUrl ?: currentUrl)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VozoPrimary
                                ),
                                modifier = Modifier.testTag("webview_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tentar Novamente", color = Color.White)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val target = failingUrl ?: currentUrl
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.testTag("webview_open_external_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = VozoAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Navegador", color = VozoAccent)
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
                CookieManager.getInstance().flush()
            } catch (_: Exception) {}
        }
    }
}
