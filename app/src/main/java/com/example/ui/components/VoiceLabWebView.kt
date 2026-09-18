package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayInputStream

object VoiceLabWebViewDefaults {
    const val VOICE_LAB_URL = "https://vozo-voice-lab.base44.app"
}

/**
 * Reusable WebView component that loads the Voice Lab URL and properly configures
 * essential web settings including JavaScript, DOM storage, database, audio capture
 * permissions, offline assets, and JavaScript interface persistence.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VoiceLabWebView(
    modifier: Modifier = Modifier,
    url: String = VoiceLabWebViewDefaults.VOICE_LAB_URL,
    onWebViewCreated: (WebView) -> Unit = {},
    onLoadingChanged: (isLoading: Boolean, progress: Int) -> Unit = { _, _ -> },
    onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit = { _, _ -> },
    onError: (errorCode: Int, description: String?, failingUrl: String?) -> Unit = { _, _, _ -> },
    onRequestAudioPermission: (((Boolean) -> Unit) -> Unit)? = null,
    onShowFileChooser: ((ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean)? = null,
    persistenceBridge: Any? = null,
    onPageFinishedHook: ((WebView, String?) -> Unit)? = null
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .testTag("vozo_reusable_webview"),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(0xFF0B0B10.toInt())

                // In headless cloud/container emulators lacking DRI/DRM rendernode nodes,
                // software layer mode avoids Mesa driver failures while rendering HTML/CSS/JS smoothly.
                try {
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                } catch (_: Exception) {}

                // 1. Enable Cookies & Cross-origin session persistence
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                // 2. Attach persistence bridge if supplied
                if (persistenceBridge != null) {
                    addJavascriptInterface(persistenceBridge, "AndroidPersistence")
                }

                // 3. Handle Web Settings (JavaScript, DOM storage, local storage, viewport, etc.)
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

                // 4. WebViewClient with request routing, decoupling, local assets, and hydration
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, pageUrl, favicon)
                        onLoadingChanged(true, 0)
                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)

                        // Hydrate localStorage from AndroidPersistence immediately
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

                        // Block Base44 promotional badges & telemetry
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

                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        onLoadingChanged(false, 100)
                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)

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
                                    var debounceTimer = null;
                                    var isMutating = false;
                                    var observer = new MutationObserver(function(mutations) {
                                        if (isMutating) return;
                                        if (debounceTimer) clearTimeout(debounceTimer);
                                        debounceTimer = setTimeout(function() {
                                            isMutating = true;
                                            try {
                                                purgeBase44();
                                                injectClonedVoice();
                                            } finally {
                                                isMutating = false;
                                            }
                                        }, 300);
                                    });
                                    if (document.body) {
                                        observer.observe(document.body, { childList: true, subtree: true });
                                    }
                                }
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(script, null)

                        if (view != null) {
                            onPageFinishedHook?.invoke(view, pageUrl)
                        }
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
                            onError(code, error?.description?.toString(), request.url?.toString())
                        }
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        android.util.Log.e(
                            "VoiceLabWebView",
                            "Render process crash detected (didCrash=${detail?.didCrash()}). Recovering WebView."
                        )
                        // Inform UI of temporary loading/error state
                        onError(
                            -1,
                            "O processo do navegador foi reiniciado para economizar memória. Recarregando...",
                            url
                        )
                        // Destroy old crashed instance safely and reload
                        try {
                            (view?.parent as? ViewGroup)?.removeView(view)
                            view?.destroy()
                        } catch (_: Exception) {}

                        // Returning true signals to Android that we handled the crash and the host app should not terminate
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

                // 5. WebChromeClient for loading progress, microphone permissions, and file uploads
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onLoadingChanged(newProgress < 100, newProgress)
                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
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
                                post { request.grant(requestedResources) }
                            } else if (onRequestAudioPermission != null) {
                                onRequestAudioPermission { granted ->
                                    post {
                                        if (granted) request.grant(requestedResources)
                                        else request.deny()
                                    }
                                }
                            } else {
                                post { request.deny() }
                            }
                        } else {
                            post { request.grant(requestedResources) }
                        }
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        return if (onShowFileChooser != null && filePathCallback != null && fileChooserParams != null) {
                            onShowFileChooser(filePathCallback, fileChooserParams)
                        } else {
                            super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                        }
                    }
                }

                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            onWebViewCreated(webView)
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            try {
                CookieManager.getInstance().flush()
            } catch (_: Exception) {}
        }
    }
}
