package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppPersistenceBridge
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.theme.VozoAccent
import com.example.theme.VozoDarkBg
import com.example.theme.VozoDarkSurfaceVariant
import com.example.theme.VozoError
import com.example.theme.VozoPrimary
import com.example.theme.VozoTextSecondary
import com.example.ui.components.AudioWaveformLoader
import com.example.ui.components.ConnectionErrorView
import com.example.ui.components.VoiceQuickToolbar

private const val VOZO_STUDIO_URL = "https://vozo-voice-lab.base44.app/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VozoStudioScreen(
    modifier: Modifier = Modifier,
    hasMicPermission: Boolean = true,
    onRequestAudioPermission: (((Boolean) -> Unit) -> Unit)? = null,
    viewModel: VozoStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val voicePref by viewModel.voicePreference.collectAsStateWithLifecycle()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // File upload callback for WebChromeClient
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    // Hardware back navigation
    BackHandler(enabled = canGoBack) {
        webViewInstance?.let { webView ->
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VozoDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("vozo_studio_screen")
    ) {
        // Quick control header
        VoiceQuickToolbar(
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            onBack = { webViewInstance?.goBack() },
            onForward = { webViewInstance?.goForward() },
            onRefresh = {
                hasError = false
                webViewInstance?.reload()
            },
            onHome = {
                hasError = false
                webViewInstance?.loadUrl(VOZO_STUDIO_URL)
            },
            onOpenBrowser = {
                val currentUrl = webViewInstance?.url ?: VOZO_STUDIO_URL
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                context.startActivity(intent)
            },
            onShare = {
                val currentUrl = webViewInstance?.url ?: VOZO_STUDIO_URL
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Check out Vozo Magpie Voice Studio: $currentUrl")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share Vozo Studio")
                context.startActivity(shareIntent)
            },
            hasMicPermission = hasMicPermission,
            onRequestAudioPermission = {
                onRequestAudioPermission?.invoke { }
            },
            activeVoiceName = voicePref.customVoiceName.ifBlank { voicePref.selectedVoiceId },
            isLoggedIn = authState.isLoggedIn,
            onClearAuth = {
                viewModel.logout()
                webViewInstance?.evaluateJavascript(
                    "(function() { try { localStorage.removeItem('base44_access_token'); localStorage.removeItem('token'); } catch(e){} })();",
                    null
                )
            }
        )

        // Microphone permission notification banner if not granted
        AnimatedVisibility(visible = !hasMicPermission) {
            Surface(
                color = VozoDarkSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mic_permission_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = null,
                        tint = VozoError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.mic_permission_banner_text),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = VozoTextSecondary,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onRequestAudioPermission?.invoke { } },
                        modifier = Modifier.testTag("btn_request_mic_banner")
                    ) {
                        Text(
                            text = stringResource(R.string.mic_permission_request),
                            color = VozoAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Progress bar
        AnimatedVisibility(
            visible = isLoading && loadProgress < 100,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                color = VozoPrimary,
                trackColor = VozoDarkBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Main WebView
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vozo_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(0xFF0B0B10.toInt())

                        // Enable cookies
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        // Register persistence bridge for JS <-> Room synchronization
                        val persistenceBridge = AppPersistenceBridge(viewModel.repository, coroutineScope)
                        addJavascriptInterface(persistenceBridge, "AndroidPersistence")

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            allowContentAccess = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = false
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "${settings.userAgentString} VozoMagpieAndroid/1.0"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                hasError = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true

                                val hydrateScript = """
                                    (function() {
                                        try {
                                            if (window.AndroidPersistence) {
                                                var token = window.AndroidPersistence.getSavedToken();
                                                if (token && token.trim() !== '') {
                                                    localStorage.setItem('base44_access_token', token);
                                                    localStorage.setItem('token', token);
                                                }
                                                var voiceId = window.AndroidPersistence.getSelectedVoiceId();
                                                if (voiceId) {
                                                    localStorage.setItem('vozo_selected_voice', voiceId);
                                                    window.VOZO_SELECTED_VOICE = voiceId;
                                                }
                                                var voiceName = window.AndroidPersistence.getCustomVoiceName();
                                                if (voiceName) {
                                                    localStorage.setItem('vozo_custom_cloned_voice_name', voiceName);
                                                }
                                                var provider = window.AndroidPersistence.getVoiceProvider();
                                                if (provider) {
                                                    localStorage.setItem('vozo_voice_provider', provider);
                                                }
                                            }
                                        } catch(e) {}
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(hydrateScript, null)
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val requestUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                                // Block and decouple any Base44 badges, telemetry, and promotional links
                                if (requestUrl.contains("badge.js") ||
                                    requestUrl.contains("app.base44.com") ||
                                    requestUrl.contains("/api/app-logs/") ||
                                    requestUrl.contains("media.base44.com/images/public")
                                ) {
                                    return WebResourceResponse(
                                        "application/javascript",
                                        "UTF-8",
                                        200,
                                        "OK",
                                        mapOf(
                                            "Access-Control-Allow-Origin" to "*",
                                            "Cache-Control" to "no-cache"
                                        ),
                                        ByteArrayInputStream("// decoupled from base44".toByteArray(Charsets.UTF_8))
                                    )
                                }

                                // Serve custom clean index.html for main frame or document requests
                                val isHtmlRequest = request.isForMainFrame ||
                                        requestUrl == VOZO_STUDIO_URL ||
                                        requestUrl.trimEnd('/') == "https://vozo-voice-lab.base44.app" ||
                                        (request.requestHeaders?.get("Accept")?.contains("text/html") == true && !requestUrl.contains("/api/"))
                                if (isHtmlRequest) {
                                    try {
                                        val assetStream = context.assets.open("web/index.html")
                                        val headers = mapOf(
                                            "Content-Type" to "text/html; charset=utf-8",
                                            "Cache-Control" to "no-cache, no-store, must-revalidate",
                                            "Access-Control-Allow-Origin" to "*"
                                        )
                                        return WebResourceResponse("text/html", "UTF-8", 200, "OK", headers, assetStream)
                                    } catch (e: Exception) {
                                        android.util.Log.e("VozoStudio", "Error loading local index.html", e)
                                    }
                                }

                                // Intercept JS bundle (with Minha Voz Clone integrated)
                                if (requestUrl.contains("assets/index-") && requestUrl.endsWith(".js")) {
                                    try {
                                        val assetStream = context.assets.open("web/index-DWmFGLpi.js")
                                        val headers = mapOf(
                                            "Access-Control-Allow-Origin" to "*",
                                            "Cache-Control" to "no-cache",
                                            "Content-Type" to "application/javascript; charset=utf-8"
                                        )
                                        return WebResourceResponse(
                                            "application/javascript",
                                            "UTF-8",
                                            200,
                                            "OK",
                                            headers,
                                            assetStream
                                        )
                                    } catch (e: Exception) {
                                        android.util.Log.e("VozoStudio", "Error loading patched assets", e)
                                    }
                                }

                                // Intercept CSS stylesheet
                                if (requestUrl.contains("assets/index-") && requestUrl.endsWith(".css")) {
                                    try {
                                        val assetStream = context.assets.open("web/index-Alu75Fmf.css")
                                        val headers = mapOf(
                                            "Access-Control-Allow-Origin" to "*",
                                            "Cache-Control" to "public, max-age=31536000",
                                            "Content-Type" to "text/css; charset=utf-8"
                                        )
                                        return WebResourceResponse("text/css", "UTF-8", 200, "OK", headers, assetStream)
                                    } catch (e: Exception) {
                                        android.util.Log.e("VozoStudio", "Error loading local css", e)
                                    }
                                }

                                // Intercept manifest.json
                                if (requestUrl.endsWith("/manifest.json")) {
                                    try {
                                        val assetStream = context.assets.open("web/manifest.json")
                                        val headers = mapOf(
                                            "Access-Control-Allow-Origin" to "*",
                                            "Content-Type" to "application/json; charset=utf-8"
                                        )
                                        return WebResourceResponse("application/json", "UTF-8", 200, "OK", headers, assetStream)
                                    } catch (e: Exception) {
                                        android.util.Log.e("VozoStudio", "Error loading manifest", e)
                                    }
                                }

                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true

                                val script = """
                                    (function() {
                                        function purgeBase44() {
                                            var selectors = [
                                                '#base44-badge',
                                                '.base44-badge',
                                                '[data-app-id]',
                                                'a[href*="base44"]',
                                                'img[src*="base44"]',
                                                'iframe[src*="base44"]'
                                            ];
                                            selectors.forEach(function(sel) {
                                                document.querySelectorAll(sel).forEach(function(el) {
                                                    el.remove();
                                                });
                                            });
                                            document.querySelectorAll('div, a, span, button').forEach(function(el) {
                                                var txt = (el.textContent || '');
                                                if (txt.includes('Edit with Base 44') || txt.includes('Base 44')) {
                                                    if (el.tagName === 'A' || el.tagName === 'BUTTON' || (el.tagName === 'DIV' && el.children.length <= 4 && txt.length < 60)) {
                                                        el.remove();
                                                    }
                                                }
                                            });
                                        }

                                        function syncPersistence() {
                                            try {
                                                if (window.AndroidPersistence) {
                                                    var token = window.AndroidPersistence.getSavedToken();
                                                    if (token && token.trim() !== '') {
                                                        localStorage.setItem('base44_access_token', token);
                                                        localStorage.setItem('token', token);
                                                    }
                                                    var voiceId = window.AndroidPersistence.getSelectedVoiceId();
                                                    if (voiceId) {
                                                        localStorage.setItem('vozo_selected_voice', voiceId);
                                                        window.VOZO_SELECTED_VOICE = voiceId;
                                                    }
                                                }
                                            } catch(e) {}

                                            if (!window.__vozo_storage_hooked) {
                                                window.__vozo_storage_hooked = true;
                                                var origSetItem = localStorage.setItem.bind(localStorage);
                                                localStorage.setItem = function(k, v) {
                                                    origSetItem(k, v);
                                                    try {
                                                        if (k === 'base44_access_token' || k === 'token') {
                                                            if (window.AndroidPersistence) {
                                                                window.AndroidPersistence.saveAuthToken(v, null);
                                                            }
                                                        } else if (k === 'vozo_selected_voice') {
                                                            if (window.AndroidPersistence) {
                                                                var p = localStorage.getItem('vozo_voice_provider') || 'gemini';
                                                                var n = localStorage.getItem('vozo_custom_cloned_voice_name') || 'Minha Voz (Clone)';
                                                                window.AndroidPersistence.saveVoicePreference(v, n, p, 'auto');
                                                            }
                                                        }
                                                    } catch(err) {}
                                                };

                                                var origRemove = localStorage.removeItem.bind(localStorage);
                                                localStorage.removeItem = function(k) {
                                                    origRemove(k);
                                                    try {
                                                        if (k === 'base44_access_token' || k === 'token') {
                                                            if (window.AndroidPersistence) {
                                                                window.AndroidPersistence.clearAuth();
                                                            }
                                                        }
                                                    } catch(err) {}
                                                };
                                            }
                                        }

                                        function injectClonedVoice() {
                                            var grids = document.querySelectorAll('.grid');
                                            var targetGrid = null;
                                            for (var i = 0; i < grids.length; i++) {
                                                var text = grids[i].textContent || '';
                                                if (text.includes('River') && text.includes('Spark')) {
                                                    targetGrid = grids[i];
                                                    break;
                                                }
                                            }
                                            if (!targetGrid) return;

                                            var activeVoiceId = 'minha_voz';
                                            try {
                                                if (window.AndroidPersistence) {
                                                    var pId = window.AndroidPersistence.getSelectedVoiceId();
                                                    if (pId && pId.trim() !== '') activeVoiceId = pId;
                                                }
                                                var lId = localStorage.getItem('vozo_selected_voice');
                                                if (lId && lId.trim() !== '') activeVoiceId = lId;
                                            } catch(e) {}

                                            // Attach listener to all standard voice buttons
                                            var buttons = targetGrid.querySelectorAll('button');
                                            buttons.forEach(function(btn) {
                                                if (btn.getAttribute('data-voice-id') === 'minha_voz') return;
                                                var pFirst = btn.querySelector('p');
                                                var vName = pFirst ? (pFirst.textContent || '').trim() : '';
                                                var vKey = vName.toLowerCase();
                                                if (vKey && !btn.__vozo_click_bound) {
                                                    btn.__vozo_click_bound = true;
                                                    btn.setAttribute('data-voice-id', vKey);
                                                    btn.addEventListener('click', function() {
                                                        try {
                                                            localStorage.setItem('vozo_selected_voice', vKey);
                                                            if (window.AndroidPersistence) {
                                                                window.AndroidPersistence.saveVoicePreference(vKey, vName, 'gemini', 'auto');
                                                            }
                                                        } catch(e) {}
                                                    });
                                                }
                                                if (activeVoiceId && activeVoiceId === vKey) {
                                                    btn.className = 'rounded-xl border p-3 text-left transition-all border-violet-500 bg-violet-500/10';
                                                }
                                            });

                                            var hasCloned = Array.from(targetGrid.querySelectorAll('p')).some(function(p) {
                                                var txt = p.textContent || '';
                                                return txt.includes('Minha Voz') || txt.includes('Voz Clonada');
                                            });

                                            if (hasCloned) {
                                                var existingCloneBtn = targetGrid.querySelector('[data-voice-id="minha_voz"]');
                                                if (existingCloneBtn) {
                                                    if (activeVoiceId === 'minha_voz') {
                                                        existingCloneBtn.className = 'rounded-xl border p-3 text-left transition-all border-violet-500 bg-violet-500/10';
                                                    } else {
                                                        existingCloneBtn.className = 'rounded-xl border p-3 text-left transition-all border-white/10 bg-white/5 hover:bg-white/10';
                                                    }
                                                }
                                                return;
                                            }

                                            if (buttons.length === 0) return;

                                            var sampleBtn = buttons[buttons.length - 1];
                                            var cloneBtn = sampleBtn.cloneNode(true);
                                            cloneBtn.setAttribute('data-voice-id', 'minha_voz');
                                            var isCloneActive = (activeVoiceId === 'minha_voz');
                                            cloneBtn.className = isCloneActive ?
                                                'rounded-xl border p-3 text-left transition-all border-violet-500 bg-violet-500/10' :
                                                'rounded-xl border p-3 text-left transition-all border-white/10 bg-white/5 hover:bg-white/10';

                                            var iconBox = cloneBtn.querySelector('div');
                                            if (iconBox) {
                                                iconBox.style.background = 'rgba(217, 70, 239, 0.2)';
                                            }
                                            var iconSvg = cloneBtn.querySelector('svg');
                                            if (iconSvg) {
                                                iconSvg.style.color = '#d946ef';
                                            }

                                            var paragraphs = cloneBtn.querySelectorAll('p');
                                            var customName = 'Minha Voz (Clone)';
                                            try {
                                                if (window.AndroidPersistence) {
                                                    var pName = window.AndroidPersistence.getCustomVoiceName();
                                                    if (pName && pName.trim() !== '') customName = pName;
                                                }
                                                customName = localStorage.getItem('vozo_custom_cloned_voice_name') || customName;
                                            } catch(e) {}

                                            if (paragraphs.length > 0) paragraphs[0].textContent = customName;
                                            if (paragraphs.length > 1) paragraphs[1].textContent = 'Voz de dublagem';

                                            cloneBtn.onclick = function(e) {
                                                e.preventDefault();
                                                e.stopPropagation();
                                                targetGrid.querySelectorAll('button').forEach(function(b) {
                                                    b.className = 'rounded-xl border p-3 text-left transition-all border-white/10 bg-white/5 hover:bg-white/10';
                                                });
                                                cloneBtn.className = 'rounded-xl border p-3 text-left transition-all border-violet-500 bg-violet-500/10';
                                                try {
                                                    localStorage.setItem('vozo_selected_voice', 'minha_voz');
                                                    localStorage.setItem('vozo_cloned_voice_active', 'true');
                                                    window.VOZO_SELECTED_VOICE = 'minha_voz';
                                                    if (window.AndroidPersistence) {
                                                        window.AndroidPersistence.saveVoicePreference('minha_voz', customName, 'gemini', 'auto');
                                                    }
                                                } catch(err) {}
                                            };

                                            targetGrid.appendChild(cloneBtn);
                                        }

                                        purgeBase44();
                                        syncPersistence();
                                        injectClonedVoice();

                                        if (!window.__vozo_observer_registered) {
                                            window.__vozo_observer_registered = true;
                                            var observer = new MutationObserver(function() {
                                                purgeBase44();
                                                injectClonedVoice();
                                            });
                                            observer.observe(document.documentElement, { childList: true, subtree: true });
                                        }
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(script, null)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    hasError = true
                                    errorMessage = error?.description?.toString()
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                return if (url.startsWith("http://") || url.startsWith("https://")) {
                                    false
                                } else {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                        true
                                    } catch (_: Exception) {
                                        true
                                    }
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                loadProgress = newProgress
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                if (request == null) return
                                val requestedResources = request.resources
                                val needsAudio = requestedResources.any { it == PermissionRequest.RESOURCE_AUDIO_CAPTURE }

                                if (needsAudio) {
                                    val isGranted = ContextCompat.checkSelfPermission(
                                        this@apply.context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (isGranted) {
                                        post {
                                            request.grant(requestedResources)
                                        }
                                    } else if (onRequestAudioPermission != null) {
                                        onRequestAudioPermission { granted ->
                                            post {
                                                if (granted) {
                                                    request.grant(requestedResources)
                                                } else {
                                                    request.deny()
                                                }
                                            }
                                        }
                                    } else {
                                        post {
                                            request.deny()
                                        }
                                    }
                                } else {
                                    post {
                                        request.grant(requestedResources)
                                    }
                                }
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallbackInternal: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = filePathCallbackInternal
                                filePickerLauncher.launch("*/*")
                                return true
                            }
                        }

                        loadUrl(VOZO_STUDIO_URL)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                }
            )

            // Sleek Loading Overlay
            if (isLoading) {
                AudioWaveformLoader(
                    progress = loadProgress,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Connection Error Overlay
            if (hasError) {
                ConnectionErrorView(
                    errorMessage = errorMessage,
                    onRetry = {
                        hasError = false
                        isLoading = true
                        webViewInstance?.reload()
                    },
                    onOpenExternal = {
                        val currentUrl = webViewInstance?.url ?: VOZO_STUDIO_URL
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                CookieManager.getInstance().flush()
            } catch (_: Exception) {}
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}
